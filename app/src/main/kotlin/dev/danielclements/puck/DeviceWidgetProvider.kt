package dev.danielclements.puck

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * A 1x1 home-screen widget pinned to one specific, already-paired Apple TV.
 *
 * Unlike the long-press launcher shortcuts (which always track the two most
 * recently used devices), each widget instance is bound once, at add time,
 * to whichever device the user picked in [DeviceWidgetConfigActivity] — it
 * stays pointed at that TV regardless of what else gets connected to later.
 */
class DeviceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val store = CredentialStore(context)
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, store, id) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetBindings.unbind(context, it) }
    }

    companion object {
        fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            store: CredentialStore,
            appWidgetId: Int,
        ) {
            val credentialKey = WidgetBindings.credentialKey(context, appWidgetId)
            val device = credentialKey?.let { store.loadDevice(it) }

            val views = RemoteViews(context.packageName, R.layout.widget_device)
            views.setTextViewText(
                R.id.widget_label,
                device?.name ?: context.getString(R.string.widget_unconfigured),
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (device != null) putExtra(MainActivity.EXTRA_DEVICE_KEY, device.credentialKey)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
