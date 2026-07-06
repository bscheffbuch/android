package io.homeassistant.companion.android.overview.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Outlet
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

internal object EntityIconProvider {
    fun iconForDomain(domain: String, isActive: Boolean, deviceClass: String? = null): ImageVector = when {
        domain == "switch" && deviceClass == "outlet" -> Icons.Rounded.Outlet
        else -> when (domain) {
            "switch", "input_boolean" -> if (isActive) Icons.Rounded.ToggleOn else Icons.Rounded.ToggleOff
            "fan" -> Icons.Rounded.Air
            "cover" -> Icons.Rounded.DoorFront
            "lock" -> if (isActive) Icons.Rounded.Lock else Icons.Rounded.LockOpen
            "climate" -> if (isActive) Icons.Rounded.AcUnit else Icons.Rounded.AcUnit
            "media_player" -> if (isActive) Icons.Rounded.PlayArrow else Icons.Rounded.MusicNote
            "vacuum" -> Icons.Rounded.SmartToy
            "alarm_control_panel" -> Icons.Rounded.Security
            "automation" -> Icons.Rounded.Bolt
            "script" -> Icons.Rounded.Settings
            "button", "input_button" -> Icons.Rounded.PowerSettingsNew
            "humidifier" -> Icons.Rounded.WaterDrop
            else -> Icons.Rounded.Home
        }
    }
}
