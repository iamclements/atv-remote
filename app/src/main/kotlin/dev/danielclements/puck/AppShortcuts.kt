package dev.danielclements.puck

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Home-screen quick actions (long-press the launcher icon) for the two most
 * recently used Apple TVs.
 *
 * These are dynamic rather than static shortcuts: which two TVs they point at
 * changes as the user pairs with and connects to devices, so they are rebuilt
 * from [CredentialStore.recentDevices] rather than fixed at build time.
 */
object AppShortcuts {

    private const val MAX_SHORTCUTS = 2

    fun sync(context: Context, store: CredentialStore) {
        val devices = store.recentDevices(MAX_SHORTCUTS)
        val shortcuts = devices.map { device ->
            ShortcutInfoCompat.Builder(context, device.credentialKey)
                .setShortLabel(device.name.take(10))
                .setLongLabel(context.getString(R.string.shortcut_long_label, device.name))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_app_tv))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(MainActivity.EXTRA_DEVICE_KEY, device.credentialKey)
                    }
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
