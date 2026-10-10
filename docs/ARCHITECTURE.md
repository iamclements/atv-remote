# Architecture

A deep dive into how Puck talks to an Apple TV, for anyone extending the
protocol layer or just curious how it works. The [README](../README.md)
covers everything else — features, install, compatibility.

## Why this is non-trivial

Apple publishes no API for controlling an Apple TV. Since tvOS 15 the legacy
DMAP/DAAP protocol is gone entirely, and the only route in is Companion Link:

1. **Discovery** — mDNS on `_companion-link._tcp`.
2. **Pair-setup** — HomeKit-style SRP-6a (3072-bit, SHA-512) keyed by the
   4-digit PIN the TV displays, ending in an exchange of Ed25519 long-term keys.
3. **Pair-verify** — Curve25519 ECDH plus Ed25519 signatures on every connect,
   deriving per-session ChaCha20-Poly1305 keys.
4. **Commands** — OPACK-serialized frames over the encrypted channel.

Now-playing needs more still: a second pairing on port 7000 and an
MRP-over-AirPlay tunnel, described below.

## Layout

| Module | What it is |
|---|---|
| `protocol/` | Pure Kotlin/JVM. No Android dependencies. All crypto and wire format. |
| `cli/` | Desktop harness for pairing and testing against a real device. |
| `app/` | Android app (Jetpack Compose, Material 3). |

The protocol lives in a plain JVM module on purpose: it makes the risky part —
crypto and byte framing — testable from a terminal against real hardware,
instead of only through an emulator.

```
opack/Opack.kt          Apple's OPACK binary serialization
hap/Tlv8.kt             TLV8 records used by the pairing handshakes
hap/HapPairing.kt       Transport-agnostic pair-setup and pair-verify
crypto/Srp.kt           SRP-6a client (3072-bit group, SHA-512)
crypto/Primitives.kt    HKDF, Ed25519, X25519, ChaCha20-Poly1305
plist/BinaryPlist.kt    bplist00 reader/writer with UID support
companion/              Framing, session, commands, RTI text input
airplay/                HAP channels, AP2 session, MRP data stream
mrp/                    Protobuf wire codec and now-playing model
```

The Android module has grown its own moving parts:

```
RemoteScreen.kt         D-pad, trackpad, transport, app drawer, now-playing card
RemoteViewModel.kt      Connection ownership, reconnection, UI state
CredentialStore.kt      Per-device registry: credentials plus connection metadata
NowPlayingService.kt    Foreground service holding the MediaSession
NotificationBridge.kt   Handoff between the view model and the notifications
KeyboardNotification.kt Direct-reply prompt when the TV asks for text
AppIcons.kt             App Store artwork lookup and disk cache
SystemAppIcons.kt       Bundled artwork for Apple's own apps
AppShortcuts.kt         Launcher shortcuts for the two most recent devices
DeviceWidgetProvider.kt Home-screen widget, one instance per pinned device
DeviceTileService.kt    The two Quick Settings tiles
PuckTheme.kt            Material You dynamic color, with a static fallback
```

## Correctness

The crypto and serialization are validated byte-for-byte against vectors
generated from [pyatv](https://github.com/postlund/pyatv), a reference
implementation known to interoperate with real devices. Writing these layers
against a specification alone is how subtle interop bugs get shipped.

```bash
./gradlew :protocol:test
```

See [`tools/`](../tools/) for regenerating the vectors.

This approach caught several bugs that would have been painful to diagnose
against live hardware:

- **Nonce truncation.** Kotlin masks `Long` shifts to 6 bits, so building a
  12-byte counter nonce with `counter shr (8 * i)` wrapped at `i == 8` and
  leaked counter bytes into the high nonce bytes. Only the very first message
  of a session was unaffected.
- **SRP salt encoding.** The salt must be hashed as the raw bytes the device
  sent. Round-tripping it through an integer drops a leading zero byte and
  breaks the proof — for roughly 1 in 256 pairings.
- **Non-canonical plist output.** CoreFoundation deduplicates equal scalars, so
  repeated strings such as `"NSObject"` collapse to one object. Without that the
  archive is structurally valid but byte-different, and tvOS rejects it silently.
- **Unsigned OPACK integers.** Encoding `-10` wraps it to `246`, which would
  seek four minutes the wrong way. `Opack.pack` now refuses negative integers.

## Protocol notes

### Text entry

The RTI channel does not use OPACK. Its payloads are NSKeyedArchiver archives —
binary property lists with UID cross-references — so `plist/BinaryPlist.kt`
implements the `bplist00` format and `companion/RtiPayloads.kt` builds the two
archives tvOS expects.

The keyboard presents itself automatically when the Apple TV focuses a text
field, via `_tiStarted` / `_tiStopped` events. A field already focused when the
app connects produces no event, so the initial state is seeded from the
`_tiStart` response instead. Sending text itself performs a `_tiStop`/`_tiStart`
round trip, and those echoes are suppressed so the keyboard does not dismiss
itself on every send.

### Now playing

From tvOS 15 onward, now-playing metadata is only available by tunnelling the
Media Remote Protocol through AirPlay:

1. Pair-verify on the AirPlay control connection (port 7000), after which the
   connection is wrapped in HAP block encryption.
2. `SETUP` with `isRemoteControlOnly` to get an event port, then connect the
   event channel. Nothing useful arrives on it, but the receiver will not
   proceed without one that answers.
3. `RECORD`.
4. `SETUP` again for a data port, with a random 64-bit seed folded into the
   key-derivation salt.
5. MRP handshake: `DEVICE_INFO` first — the device stays silent until it
   arrives — then `SET_CONNECTION_STATE` and `CLIENT_UPDATES_CONFIG`.

The data channel nests three framings: a 32-byte big-endian header, a binary
plist, and `params.data` holding varint-length-prefixed protobufs.

`mrp/Protobuf.kt` is a generic wire-format codec rather than generated sources.
MRP defines dozens of message types and now-playing touches about six fields
across four of them, so reading the wire format directly avoids putting protoc
into an Android build and tolerates unknown fields from newer tvOS revisions.

Two things that are easy to get wrong:

- **Track metadata is not in `nowPlayingInfo`.** That field is essentially
  always absent. Real titles and artists arrive in
  `SetStateMessage.playbackQueue.contentItems`, indexed by the queue's
  `location`.
- **`PlayerPath.client` is field 2, not 1** — field 1 is `origin`. Reading
  field 1 yields the device's own name for every app, silently collapsing all
  players into one entry so one app's metadata leaks onto another.
- **The playhead rides on the content item too.** Elapsed time is
  `ContentItemMetadata.elapsedTime`, field 35, alongside `duration` at 14 —
  not the `nowPlayingInfo` pair, which is usually absent. Field 12 is
  `releaseDate` and is also a double, so guessing at it decodes cleanly and
  yields a playhead of roughly the year 2015 in seconds. Field numbers here
  are worth checking against pyatv's `.proto` definitions rather than
  inferring, precisely because a wrong one need not fail loudly.

Position updates arrive sporadically, so the UI extrapolates between them from
an anchor — the last reported playhead and the moment it landed — rather than
polling. The media notification hands the same anchor to Android, which runs
its own scrubber forward at the reported rate.

## Notifications

The now-playing notification is a platform `MediaSession` rather than a
hand-drawn layout, so the system's own media controls drive it: artwork, a
scrubber that animates between updates, and transport buttons. Volume is
published through `setPlaybackToRemote`, which turns it into a slider instead
of two buttons — but only when the Apple TV reports it can route volume.

It runs inside a foreground service, and the service is the point rather than
the notification. It is what keeps the process, and therefore the Companion and
MRP connections, alive once the app leaves the screen. That is also why it runs
for the whole connection instead of only during playback: Android 12 forbids
starting a foreground service from the background, so waiting for playback
would mean the notification could never appear unless the app happened to be
open.

**Its lifetime is the app's.** Swiping the app off the recents list clears the
view model, which closes the connections and takes the notification with it.
Surviving that would mean moving connection ownership into the service.

The keyboard prompt is a separate notification carrying an Android direct-reply
field, so text can be typed and sent from the shade without unlocking. It is
raised by the TV's own focus event — deliberately not by the in-app keyboard
toggle, which the user drives — and reposted as the field's contents change,
which is also what clears the reply spinner after a send.

## App icons

The Apple TV sends no artwork: Companion Link reports a name and a bundle id
and nothing else. The drawer fills that gap from Apple's public iTunes
endpoints, trying four queries in turn and stopping at the first that answers:

1. the tvOS listing in the phone's own store region — 512×307 artwork, exactly
   the tile's 5:3;
2. the same in the US store, which is the widest catalogue;
3. the app's iOS listing, whose icon is square and so sits inside the tile
   rather than filling it;
4. a search by name, which is the only step that can be wrong.

That last step is gated: the store's title must *equal* the app's name, give or
take a tagline after a separator. Mere containment is not enough — it put
"Fusion Smart Education" on an app called Fusion. Note that the search endpoint
ignores `entity=tvSoftware` and only answers for `software`, so a name match
never yields tvOS-shaped artwork.

Results are cached on disk, hits and misses alike, since an app that is not in
the store today will not be tomorrow. Network failures are deliberately *not*
cached, or a dropout would leave an app wearing initials until the cache
cleared. The cache directory carries a version suffix: a miss recorded by an
older, narrower set of queries says nothing about what the current ones would
find.

Apple's own apps are in no catalogue under any region or entity, so they are
never looked up. They ship with the app instead, as original artwork in one
visual language — see the generator under [`tools/`](../tools/). Original rather
than Apple's own, which this project has no licence to redistribute.

## Connection stability

An Apple TV rotates the ephemeral port it advertises for Companion Link, even
while it is awake and playing. When that happens the existing connection dies,
and reconnecting to the cached port fails outright. Three things guard against
this:

- Credentials are keyed on the device's stable `rpMRtID`, never on host:port,
  so a rotated port never orphans a pairing.
- Reconnection rediscovers the device over mDNS first rather than trusting the
  port it last saw.
- An idle connection is kept warm with periodic `NoOp` frames, and a failed
  write is treated as a lost connection — the reader blocks in `readFully` and
  will not notice a peer that vanished without a clean shutdown.

Commands transparently reconnect and retry once, so a dropped connection
surfaces as a brief pause rather than an error.
