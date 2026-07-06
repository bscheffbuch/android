package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Doubles the default M3 elevated card corner radius (12dp) for a softer, more pill-like look,
// shared by every Overview entity card so corner rounding is visually consistent across domains.
internal val OverviewCardCornerRadius = 24.dp
internal val OverviewCardShape = RoundedCornerShape(OverviewCardCornerRadius)
