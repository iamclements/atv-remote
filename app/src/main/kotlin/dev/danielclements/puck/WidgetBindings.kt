package dev.danielclements.puck

import android.content.Context

/**
 * Which Apple TV each home-screen widget instance is pinned to.
 *
 * Separate from [CredentialStore]: a widget's binding is keyed by its
 * `appWidgetId`, which Android assigns per placed instance, not by device —
 * the same TV can be bound to more than one widget, and a widget survives a
 * device being re-paired under the same credential key.
 */
object WidgetBindings {

    private const val PREFS = "widget_bindings"

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bind(context: Context, appWidgetId: Int, credentialKey: String) {
        prefs(context).edit().putString("widget-$appWidgetId", credentialKey).apply()
    }

    fun credentialKey(context: Context, appWidgetId: Int): String? =
        prefs(context).getString("widget-$appWidgetId", null)

    fun unbind(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove("widget-$appWidgetId").apply()
    }
}
