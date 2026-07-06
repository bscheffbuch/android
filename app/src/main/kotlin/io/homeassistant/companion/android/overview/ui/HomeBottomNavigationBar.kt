package io.homeassistant.companion.android.overview.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme

/**
 * The content tabs hosted in-place by [HomeBottomNavigationBar]. Settings is deliberately not a
 * member of this enum: it always navigates out to
 * [io.homeassistant.companion.android.settings.SettingsActivity] rather than switching displayed
 * content in place. Passing `null` as [HomeBottomNavigationBar]'s `selectedTab` represents Settings
 * being the active screen, which is what [io.homeassistant.companion.android.settings.SettingsActivity]
 * itself does when it renders this same bar to stay navigable while on top.
 */
enum class HomeContentTab { HOME, AUTOMATIONS_AND_SCENES }

@Composable
fun HomeBottomNavigationBar(
    selectedTab: HomeContentTab?,
    onSelectHome: () -> Unit,
    onSelectAutomationsAndScenes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHAColorScheme.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.colorFillPrimaryLoudResting,
        selectedTextColor = colors.colorFillPrimaryLoudResting,
        indicatorColor = colors.colorFillPrimaryQuietResting,
        unselectedIconColor = colors.colorTextSecondary,
        unselectedTextColor = colors.colorTextSecondary,
    )

    // A tone distinct from colorSurfaceDefault (pure black in dark mode) so the bar reads as a
    // separate, slightly elevated surface instead of blending into the screen background.
    NavigationBar(modifier = modifier, containerColor = colors.colorSurfaceLow) {
        NavigationBarItem(
            selected = selectedTab == HomeContentTab.HOME,
            onClick = onSelectHome,
            icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = null) },
            label = { Text(stringResource(commonR.string.overview_home_tab)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == HomeContentTab.AUTOMATIONS_AND_SCENES,
            onClick = onSelectAutomationsAndScenes,
            icon = { Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null) },
            label = { Text(stringResource(commonR.string.automations_scenes_title)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedTab == null,
            onClick = onOpenSettings,
            icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = null) },
            label = { Text(stringResource(commonR.string.settings)) },
            colors = itemColors,
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeBottomNavigationBarPreview() {
    HAThemeForPreview {
        HomeBottomNavigationBar(
            selectedTab = HomeContentTab.HOME,
            onSelectHome = {},
            onSelectAutomationsAndScenes = {},
            onOpenSettings = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeBottomNavigationBarSettingsSelectedPreview() {
    HAThemeForPreview {
        HomeBottomNavigationBar(
            selectedTab = null,
            onSelectHome = {},
            onSelectAutomationsAndScenes = {},
            onOpenSettings = {},
        )
    }
}
