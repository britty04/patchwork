package com.brittytino.patchwork.ui.composables.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brittytino.patchwork.R
import com.brittytino.patchwork.ui.components.containers.RoundedCardContainer
import com.brittytino.patchwork.ui.components.containers.RoundedCardContainer
import com.brittytino.patchwork.viewmodels.MainViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteriesSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        RoundedCardContainer {
            // Bluetooth Devices
            val isBluetoothEnabled = viewModel.isBluetoothDevicesEnabled.value
            val isPermissionGranted = viewModel.isBluetoothPermissionGranted.value
            
            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    viewModel.isBluetoothPermissionGranted.value = true
                    viewModel.setBluetoothDevicesEnabled(true, context)
                }
            }
            
            ListItem(
                leadingContent = { 
                     androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_bluetooth_24),
                        contentDescription = null 
                    ) 
                },
                headlineContent = { Text(stringResource(R.string.show_bluetooth_devices)) },
                supportingContent = { Text(stringResource(R.string.show_bluetooth_devices_summary)) },
                trailingContent = {
                    androidx.compose.material3.Switch(
                        checked = isBluetoothEnabled,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            if (enabled) {
                                if (isPermissionGranted) {
                                    viewModel.setBluetoothDevicesEnabled(true, context)
                                } else {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        launcher.launch(
                                            arrayOf(
                                                android.Manifest.permission.BLUETOOTH_CONNECT,
                                                android.Manifest.permission.BLUETOOTH_SCAN
                                            )
                                        )
                                    } else {
                                        viewModel.setBluetoothDevicesEnabled(true, context)
                                    }
                                }
                            } else {
                                viewModel.setBluetoothDevicesEnabled(false, context)
                            }
                        }
                    )
                }
            )


            // Limit Max Devices
            ListItem(
                leadingContent = {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_devices_24),
                        contentDescription = null
                    )
                },
                headlineContent = { Text(stringResource(R.string.limit_max_devices)) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.limit_max_devices_summary))
                        androidx.compose.material3.Slider(
                            value = viewModel.batteryWidgetMaxDevices.intValue.toFloat(),
                            onValueChange = { 
                                val newInt = it.toInt()
                                if (newInt != viewModel.batteryWidgetMaxDevices.intValue) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    viewModel.setBatteryWidgetMaxDevices(newInt, context) 
                                }
                            },
                            valueRange = 1f..8f,
                            steps = 6
                        )
                    }
                },
                trailingContent = {
                    Text(
                        text = viewModel.batteryWidgetMaxDevices.intValue.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
            )

            // Widget Background Toggle
            ListItem(
                leadingContent = {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_circles_24),
                        contentDescription = null
                    )
                },
                headlineContent = { Text(stringResource(R.string.widget_background_title)) },
                supportingContent = { Text(stringResource(R.string.widget_background_summary)) },
                trailingContent = {
                    androidx.compose.material3.Switch(
                        checked = viewModel.isBatteryWidgetBackgroundEnabled.value,
                        onCheckedChange = {  
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setBatteryWidgetBackgroundEnabled(it, context) 
                        }
                    )
                }
            )
        }
    }
}
