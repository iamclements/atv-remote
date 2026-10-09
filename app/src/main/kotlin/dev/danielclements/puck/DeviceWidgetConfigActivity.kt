package dev.danielclements.puck

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.atvremote.protocol.discovery.AppleTvDevice

/**
 * Shown once, when a widget is first dragged onto the home screen: picks
 * which already-paired Apple TV this particular widget instance controls.
 *
 * Standard Android widget-config contract: default to RESULT_CANCELED so
 * backing out (or being killed) before a pick leaves the widget un-added,
 * and finish with RESULT_OK + the widget id only once a device is bound.
 */
class DeviceWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val store = CredentialStore(this)
        val devices = store.pairedKeys().mapNotNull { store.loadDevice(it) }

        setContent {
            PuckTheme {
                Surface(Modifier.fillMaxSize()) {
                    DevicePicker(devices) { device -> finishWithDevice(store, device) }
                }
            }
        }
    }

    private fun finishWithDevice(store: CredentialStore, device: AppleTvDevice) {
        WidgetBindings.bind(this, appWidgetId, device.credentialKey)
        DeviceWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), store, appWidgetId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@Composable
private fun DevicePicker(devices: List<AppleTvDevice>, onPick: (AppleTvDevice) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            stringResource(R.string.widget_pick_device_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (devices.isEmpty()) {
            Text(
                stringResource(R.string.widget_pick_device_empty),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(devices, key = { it.credentialKey }) { device ->
                ListItem(
                    headlineContent = { Text(device.name) },
                    supportingContent = { Text(device.model ?: "Apple TV") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(device) },
                )
            }
        }
    }
}
