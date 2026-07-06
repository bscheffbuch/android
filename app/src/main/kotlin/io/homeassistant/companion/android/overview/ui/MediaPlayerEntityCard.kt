package io.homeassistant.companion.android.overview.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.theme.HAThemeForPreview
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.getVolumeLevel
import io.homeassistant.companion.android.common.data.integration.isActive
import io.homeassistant.companion.android.common.data.integration.supportsMediaNextTrack
import io.homeassistant.companion.android.common.data.integration.supportsMediaPreviousTrack
import io.homeassistant.companion.android.common.data.integration.supportsVolumeSet
import java.time.LocalDateTime
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A card for media player entities.
 *
 * Gestures:
 * - Tap: toggle playback (play/pause)
 * - Horizontal drag: adjust volume live while sliding (only when the entity supports
 *   [io.homeassistant.companion.android.common.data.integration.supportsVolumeSet])
 * - Long press: open detail sheet
 * - Tap the trailing skip-previous/skip-next icon buttons: shown only when the entity supports
 *   [io.homeassistant.companion.android.common.data.integration.supportsMediaPreviousTrack]/
 *   [io.homeassistant.companion.android.common.data.integration.supportsMediaNextTrack]
 */
@Composable
fun MediaPlayerEntityCard(
    entity: Entity,
    onTogglePlayback: () -> Unit,
    onSetVolume: (volume: Float, immediate: Boolean) -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHAColorScheme.current
    val haptic = LocalHapticFeedback.current
    val isOn = entity.isActive()
    val supportsVolume = entity.supportsVolumeSet()
    val entityVolume = entity.getVolumeLevel()?.value ?: 0f
    var displayVolume by remember(entity.entityId) { mutableFloatStateOf(entityVolume) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(entityVolume) {
        if (!isDragging) displayVolume = entityVolume
    }

    val friendlyName = entity.attributes["friendly_name"]?.toString() ?: entity.entityId
    val mediaTitle = entity.attributes["media_title"]?.toString()
    val mediaArtist = entity.attributes["media_artist"]?.toString()
        ?: entity.attributes["media_album_artist"]?.toString()

    val subtitle = when {
        isDragging && supportsVolume ->
            stringResource(commonR.string.overview_media_player_volume, displayVolume.toInt())
        mediaTitle != null && mediaArtist != null -> "$mediaTitle · $mediaArtist"
        mediaTitle != null -> mediaTitle
        else -> entity.state.replaceFirstChar { it.uppercaseChar() }
    }

    val cardBg = if (isOn) colors.colorFillPrimaryQuietResting else colors.colorSurfaceLow
    val iconTint = if (isOn) colors.colorOnPrimaryNormal else colors.colorTextDisabled
    val textColor = if (isOn) colors.colorTextPrimary else colors.colorTextSecondary
    // requireUnconsumed = true so the trailing skip-track IconButtons (real Composable click
    // targets) can claim the down event first; otherwise tapping them would also toggle playback.
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(entity.entityId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                var dragStarted = false
                val startX = down.position.x
                val volumeAtGestureStart = displayVolume

                val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    var active = true
                    while (active) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: return@withTimeoutOrNull null
                        if (!change.pressed) {
                            active = false
                        } else {
                            val dx = change.position.x - startX
                            if (!dragStarted && abs(dx) > viewConfiguration.touchSlop) {
                                dragStarted = true
                                isDragging = true
                                return@withTimeoutOrNull true
                            }
                        }
                    }
                    false
                }

                when {
                    result == null -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDetail()
                        var waiting = true
                        while (waiting) {
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            if (e.changes.all { !it.pressed }) waiting = false
                        }
                    }

                    result == false -> {
                        onTogglePlayback()
                    }

                    else -> {
                        isDragging = true
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                active = false
                                isDragging = false
                                if (supportsVolume) {
                                    onSetVolume(displayVolume, true)
                                }
                            } else {
                                val dx = change.position.x - startX
                                if (supportsVolume) {
                                    change.consume()
                                    displayVolume = (volumeAtGestureStart + dx / size.width.toFloat() * 100f)
                                        .coerceIn(0f, 100f)
                                    onSetVolume(displayVolume, false)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(gestureModifier),
        shape = OverviewCardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = EntityIconProvider.iconForDomain(entity.domain, isOn),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friendlyName,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = colors.colorTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entity.supportsMediaPreviousTrack()) {
                    IconButton(onClick = onSkipToPreviousTrack, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(commonR.string.overview_media_player_previous_track),
                            tint = iconTint,
                        )
                    }
                }
                if (entity.supportsMediaNextTrack()) {
                    IconButton(onClick = onSkipToNextTrack, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(commonR.string.overview_media_player_next_track),
                            tint = iconTint,
                        )
                    }
                }
            }
        }
    }
}

private fun previewEntity(state: String, withTrackInfo: Boolean, volumeLevel: Double? = null) = Entity(
    entityId = "media_player.living_room",
    state = state,
    attributes = if (withTrackInfo) {
        mapOf(
            "friendly_name" to "Living room speaker",
            "supported_features" to (4 or 16 or 32),
            "media_title" to "Bohemian Rhapsody",
            "media_artist" to "Queen",
            "volume_level" to (volumeLevel ?: 0.5),
        )
    } else {
        mapOf("friendly_name" to "Living room speaker")
    },
    lastChanged = LocalDateTime.now(),
    lastUpdated = LocalDateTime.now(),
)

@PreviewLightDark
@Composable
private fun MediaPlayerEntityCardPlayingPreview() {
    HAThemeForPreview {
        MediaPlayerEntityCard(
            entity = previewEntity(state = "playing", withTrackInfo = true),
            onTogglePlayback = {},
            onSetVolume = { _, _ -> },
            onSkipToPreviousTrack = {},
            onSkipToNextTrack = {},
            onOpenDetail = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MediaPlayerEntityCardOffPreview() {
    HAThemeForPreview {
        MediaPlayerEntityCard(
            entity = previewEntity(state = "off", withTrackInfo = false),
            onTogglePlayback = {},
            onSetVolume = { _, _ -> },
            onSkipToPreviousTrack = {},
            onSkipToNextTrack = {},
            onOpenDetail = {},
        )
    }
}
