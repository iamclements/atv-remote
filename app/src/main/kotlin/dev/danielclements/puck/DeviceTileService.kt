package dev.danielclements.puck

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * A Quick Settings tile bound to "the Nth most recently used Apple TV,"
 * mirroring the two long-press launcher shortcuts ([AppShortcuts]).
 *
 * Android doesn't let an app spawn one tile per paired device dynamically —
 * each tile is a separately declared component the user adds by hand from
 * the Quick Settings editor — so this follows the same fixed-slot design as
 * the shortcuts rather than offering per-tile device selection like the
 * home-screen widget does.
 */
abstract class DeviceTileServiceBase(private val slot: Int) : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val device = CredentialStore(this).recentDevices(slot + 1).getOrNull(slot)

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (device != null) putExtra(MainActivity.EXTRA_DEVICE_KEY, device.credentialKey)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val device = CredentialStore(this).recentDevices(slot + 1).getOrNull(slot)
        tile.label = device?.name ?: getString(R.string.tile_unconfigured_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_remote)
        tile.state = if (device != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (device != null) getString(R.string.tile_subtitle) else null
        }
        tile.updateTile()
    }
}

/** Slot 0: the single most recently used Apple TV. */
class DeviceTileOne : DeviceTileServiceBase(slot = 0)

/** Slot 1: the second most recently used Apple TV. */
class DeviceTileTwo : DeviceTileServiceBase(slot = 1)
