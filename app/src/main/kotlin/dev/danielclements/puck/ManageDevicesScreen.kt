package dev.danielclements.puck

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atvremote.protocol.discovery.AppleTvDevice

@Composable
fun ManageDevicesScreen(state: UiState, vm: RemoteViewModel) {
    BackHandler { vm.closeManageDevices() }

    var renaming by remember { mutableStateOf<AppleTvDevice?>(null) }
    var forgetting by remember { mutableStateOf<AppleTvDevice?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { vm.closeManageDevices() }
                    .padding(8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.manage_devices_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(20.dp))

        if (state.managedDevices.isEmpty()) {
            Text(
                stringResource(R.string.manage_devices_empty),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.managedDevices, key = { it.credentialKey }) { device ->
                    ManagedDeviceRow(
                        device = device,
                        onRename = { renaming = device },
                        onForget = { forgetting = device },
                    )
                }
            }
        }
    }

    renaming?.let { device ->
        RenameDialog(
            device = device,
            onConfirm = { name -> vm.renameDevice(device, name); renaming = null },
            onDismiss = { renaming = null },
        )
    }

    forgetting?.let { device ->
        ForgetDialog(
            device = device,
            onConfirm = { vm.forgetManagedDevice(device); forgetting = null },
            onDismiss = { forgetting = null },
        )
    }
}

@Composable
private fun ManagedDeviceRow(device: AppleTvDevice, onRename: () -> Unit, onForget: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                device.model ?: "Apple TV",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(R.string.cd_rename),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onRename)
                .padding(8.dp),
        )
        Icon(
            Icons.Default.Delete,
            contentDescription = stringResource(R.string.cd_forget),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onForget)
                .padding(8.dp),
        )
    }
}

@Composable
private fun RenameDialog(device: AppleTvDevice, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(device.credentialKey) { mutableStateOf(device.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_device_title, device.name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.device_name_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ForgetDialog(device: AppleTvDevice, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.forget_device_title, device.name)) },
        text = { Text(stringResource(R.string.forget_device_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.forget), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
