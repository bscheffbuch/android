package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Doubles the default M3 elevated card corner radius (12dp) for a softer, more pill-like look,
// shared by every Overview entity card so corner rounding is visually consistent across domains.
internal val OverviewCardCornerRadius = 24.dp
internal val OverviewCardShape = RoundedCornerShape(OverviewCardCornerRadius)

// The single gap used everywhere in the Overview grid — between cards horizontally and vertically,
// and inside an expanded light group. The group highlight's bleed is derived as exactly half of it,
// so widening this here keeps the tinted frame's spacing even with the grid automatically.
internal val OverviewCardGap = 12.dp
