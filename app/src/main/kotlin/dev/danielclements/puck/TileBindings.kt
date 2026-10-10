package dev.danielclements.puck

import android.content.Context

/**
 * Which Apple TV each Quick Settings tile slot is pinned to.
 *
 * Unlike the widget, a tile has no add-time configuration step — Android
 * only offers a long-press "preferences" hook once the tile already exists
 * (see [TileConfigActivity]). Until the user long-presses to pick a device,
 * a tile has no binding and falls back to tracking the most recently used
 * Apple TV, same as the launcher shortcuts.
 */
object TileBindings {

    private const val PREFS = "tile_bindings"

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bind(context: Context, slot: Int, credentialKey: String) {
        prefs(context).edit().putString("tile-$slot", credentialKey).apply()
    }

    fun credentialKey(context: Context, slot: Int): String? =
        prefs(context).getString("tile-$slot", null)
}
