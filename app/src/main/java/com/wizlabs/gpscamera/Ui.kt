@file:OptIn(ExperimentalLayoutApi::class)

package com.wizlabs.gpscamera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Brand palette ---------------------------------------------------------------------------
val WizNavy = Color(0xFF07131C)      // app background
val WizBar = Color(0xFF050D14)       // deepest surface (camera chrome)
val WizPanel = Color(0xFF0F1F2A)     // cards
val WizPanelHi = Color(0xFF162B38)   // raised card / pressed
val WizLine = Color(0x1FFFFFFF)      // hairlines
val WizTeal = Color(0xFF5FE3EE)      // primary accent
val WizTealDeep = Color(0xFF1FB5C4)
val WizText = Color(0xFFF2F7FA)
val WizMuted = Color(0xFF8FA3B0)
val WizGood = Color(0xFF4ADE80)
val WizWarn = Color(0xFFFBBF24)
val WizBad = Color(0xFFF87171)
val WizGlass = Color(0x8C0A161F)     // translucent control background over the preview

private val WizColors = darkColorScheme(
    primary = WizTeal,
    onPrimary = WizNavy,
    primaryContainer = WizTealDeep,
    onPrimaryContainer = Color.White,
    secondary = WizTeal,
    background = WizNavy,
    onBackground = WizText,
    surface = WizPanel,
    onSurface = WizText,
    surfaceVariant = WizPanelHi,
    onSurfaceVariant = WizMuted,
    surfaceContainer = WizPanel,
    surfaceContainerHigh = WizPanelHi,
    surfaceContainerLow = WizNavy,
    outline = WizLine,
    error = WizBad
)

private val WizType = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
)

private val WizShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WizTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WizColors, typography = WizType, shapes = WizShapes, content = content)
}

/** Light "tick" used on every toggle so the app feels responsive. */
@Composable
fun rememberTick(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}

// ---- Camera chrome --------------------------------------------------------------------------

/** Round translucent icon button that sits over the camera preview. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    badge: String? = null
) {
    val tick = rememberTick()
    val bg by animateColorAsState(if (active) WizTeal else WizGlass, tween(180), label = "glassBg")
    val fg by animateColorAsState(
        when {
            !enabled -> Color.White.copy(alpha = .35f)
            active -> WizNavy
            else -> Color.White
        },
        tween(180), label = "glassFg"
    )
    Surface(
        onClick = { tick(); onClick() },
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = if (active) null else BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
            if (badge != null) {
                Text(
                    badge,
                    color = if (active) WizNavy else WizTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 4.dp)
                )
            }
        }
    }
}

/** Translucent pill with optional icon, e.g. "4:3" or "✎ Add note". */
@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    dotColor: Color? = null
) {
    val tick = rememberTick()
    Surface(
        onClick = { tick(); onClick() },
        shape = RoundedCornerShape(50),
        color = if (active) WizTeal else WizGlass,
        contentColor = if (active) WizNavy else Color.White,
        border = if (active) null else BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(7.dp))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

// ---- Settings building blocks ---------------------------------------------------------------

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.padding(start = 4.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = WizTeal, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text.uppercase(),
            color = WizTeal,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WizPanel)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { content() }
}

@Composable
fun CardDivider() {
    HorizontalDivider(color = WizLine, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
}

/** Small rounded square holding an icon, used at the start of settings rows. */
@Composable
fun IconTile(icon: ImageVector, tint: Color = WizTeal) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = .14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hint: String? = null,
    icon: ImageVector? = null
) {
    val tick = rememberTick()
    // The whole row toggles, not just the switch.
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = { tick(); onChange(it) })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(icon)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = WizText, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(hint, color = WizMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WizNavy,
                checkedTrackColor = WizTeal,
                uncheckedThumbColor = WizMuted,
                uncheckedTrackColor = WizPanelHi,
                uncheckedBorderColor = WizLine
            )
        )
    }
}

/** A labelled group of options: label (with optional icon) above a wrap of pill chips. */
@Composable
fun <T> OptionGroup(
    label: String,
    options: List<Pair<String, T>>,
    selected: T,
    icon: ImageVector? = null,
    onSelect: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                IconTile(icon)
                Spacer(Modifier.width(14.dp))
            }
            Text(label, color = WizText, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.size(10.dp))
        ChipRow(options, selected, onSelect)
    }
}

@Composable
fun <T> ChipRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            PillChip(label, value == selected) { onSelect(value) }
        }
    }
}

@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    val bg by animateColorAsState(if (selected) WizTeal else WizPanelHi, tween(160), label = "chipBg")
    val fg by animateColorAsState(if (selected) WizNavy else WizText, tween(160), label = "chipFg")
    val s by animateFloatAsState(if (selected) 1f else .97f, tween(160), label = "chipScale")
    Surface(
        onClick = { tick(); onClick() },
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
        border = if (selected) null else BorderStroke(1.dp, WizLine),
        modifier = Modifier.scale(s)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}
