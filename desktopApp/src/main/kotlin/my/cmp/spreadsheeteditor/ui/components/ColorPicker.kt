package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.FluentTheme
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorPicker(
    isExpanded: Boolean,
    expandTrigger: () -> Unit,
    suggestedColors: List<Color>,
    onColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .height(if (isExpanded) 300.dp else 100.dp)
            .background(
                color = SpreadsheetTheme.colors.colSurface
            )
            .clip(shape = FluentTheme.shapes.overlay)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            suggestedColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color = color)
                        .border(1.dp, Color.Black.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onColorSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!isExpanded) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(150, 150, 150))
                        .border(1.dp, Color.Black.copy(alpha = 0.2f))
                        .clickable { expandTrigger() }
                        .pointerInput(Unit) {
                            detectTapGestures { expandTrigger() }
                        }
                        .alpha(0.5f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        } else {
            var color by remember { mutableStateOf(Color.Black) }
            Row(
                modifier = Modifier.height(110.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularColorCanvas(currentColor = color, onColorSelected = { color = it }, modifier = Modifier.size(100.dp))
            }
            Row(
                modifier = Modifier.height(50.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrayscaleBar(
                    currentColor = color,
                    onColorSelected = { color = it },
                    modifier = Modifier.height(30.dp).fillMaxWidth(0.9f)
                )
            }
            Row(
                modifier = Modifier.height(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hex: ${color.toHex()}, ${color.toRgb()}",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(18.dp).fillMaxWidth(0.9f)
                )
            }
            Row(
                modifier = Modifier.height(50.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Button(
                    modifier = Modifier.height(40.dp).fillMaxWidth(0.9f),
                    onClick = { onColorSelected(color) }
                ){
                    Text(text = "Apply")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hue / saturation wheel
// ---------------------------------------------------------------------------

@Composable
fun CircularColorCanvas(
    modifier: Modifier = Modifier,
    currentColor: Color = Color.Red,
    onColorSelected: (Color) -> Unit = {}
) {
    var interactionOffset by remember { mutableStateOf<Offset?>(null) }
    var isTapping by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { interactionOffset = it },
                    onDrag = { change, _ -> interactionOffset = change.position },
                    onDragEnd = { interactionOffset = null },
                    onDragCancel = { interactionOffset = null }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { 
                    interactionOffset = it
                    isTapping = true
                }
            }
    ) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Hue sweep
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                center = center
            ),
            radius = radius,
            center = center
        )
        // Saturation (white radial overlay)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center, radius = radius
            ),
            radius = radius, center = center
        )

        // Calculate offset from currentColor if not interacting
        val activeOffset = interactionOffset ?: run {
            val r = currentColor.red
            val g = currentColor.green
            val b = currentColor.blue
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }.let { if (it < 0) it + 360f else it }
            val sat = if (max == 0f) 0f else delta / max
            
            val angleRad = hue * PI / 180f
            val dist = sat * radius
            Offset(
                (center.x + cos(angleRad) * dist).toFloat(),
                (center.y + sin(angleRad) * dist).toFloat()
            )
        }

        val dx = activeOffset.x - center.x
        val dy = activeOffset.y - center.y
        val dist = hypot(dx, dy)
        val clamped = dist.coerceIn(0f, radius)
        val angleDeg = ((atan2(dy, dx) * 180.0 / PI) + 360) % 360
        val hue = angleDeg.toFloat()
        val sat = (clamped / radius).coerceIn(0f, 1f)
        val color = Color.hsv(hue, sat, 1f)
        
        // Only emit if we are interacting
        if (interactionOffset != null) {
            onColorSelected(color)
            if (isTapping) {
                interactionOffset = null
                isTapping = false
            }
        }

        val sx = (center.x + cos(angleDeg * PI / 180) * clamped).toFloat()
        val sy = (center.y + sin(angleDeg * PI / 180) * clamped).toFloat()
        drawCircle(Color.White, 16f, Offset(sx, sy))
        drawCircle(color, 12f, Offset(sx, sy))
    }
}

// ---------------------------------------------------------------------------
// Brightness / saturation bar  (White ← Hue → Black)
//
// Bug fixed: the original code had  lerp(1f, 1f, t)  in the white half,
// which always returned 1 (full brightness) regardless of t.  The correct
// behaviour is to keep value=1 and interpolate saturation from 0→full in the
// left half, then keep saturation=full and interpolate value from 1→0 in the
// right half.
// ---------------------------------------------------------------------------

@Composable
fun GrayscaleBar(
    modifier: Modifier = Modifier,
    currentColor: Color = Color.Red,
    onColorSelected: (Color) -> Unit = {}
) {
    // Extract stable hue + saturation from whatever colour arrived from the wheel
    data class HueSat(val hue: Float, val sat: Float)

    val hueSat = remember(currentColor) {
        val r = currentColor.red;
        val g = currentColor.green;
        val b = currentColor.blue
        val max = maxOf(r, g, b);
        val min = minOf(r, g, b);
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0) it + 360f else it }
        HueSat(hue, if (max == 0f) 0f else delta / max)
    }

    val pureHueColor = Color.hsv(hueSat.hue, hueSat.sat, 1f)

    // Calculate ratio from current color's value/saturation
    // 0 = white, 0.5 = pure hue, 1 = black
    val currentRatio = remember(currentColor) {
        val r = currentColor.red
        val g = currentColor.green
        val b = currentColor.blue
        val v = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val s = if (v == 0f) 0f else (v - min) / v

        if (v < 1f) {
            // Darkening (right half): ratio 0.5 -> 1.0 as v goes 1.0 -> 0.0
            0.5f + (1.0f - v) * 0.5f
        } else {
            // Desaturating (left half): ratio 0.5 -> 0.0 as s goes hueSat.sat -> 0.0
            // If hueSat.sat is 0, we are at white/gray, ratio is 0.
            if (hueSat.sat > 0) {
                (s / hueSat.sat) * 0.5f
            } else {
                0f
            }
        }
    }

    var interactionOffset by remember { mutableStateOf<Offset?>(null) }
    var isTapping by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { interactionOffset = it },
                    onDrag = { change, _ -> interactionOffset = change.position },
                    onDragEnd = { interactionOffset = null },
                    onDragCancel = { interactionOffset = null }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { 
                    interactionOffset = it
                    isTapping = true
                }
            }
    ) {
        val w = size.width;
        val h = size.height
        val cr = CornerRadius(h / 4)

        // Gradient: White → PureHue → Black
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, pureHueColor, Color.Black),
                startX = 0f, endX = w
            ),
            cornerRadius = cr
        )

        val activeRatio = if (interactionOffset != null) {
            interactionOffset!!.x.coerceIn(0f, w) / w
        } else {
            currentRatio
        }

        val cx = activeRatio * w
        val ratio = activeRatio

        // LEFT half  (0 → 0.5): value = 1, saturation goes 0 → full
        // RIGHT half (0.5 → 1): saturation = full, value goes 1 → 0
        val (adjSat, adjVal) = when {
            ratio < 0.5f -> {
                val t = ratio * 2f                          // 0 → 1 across left half
                lerp(0f, hueSat.sat, t) to 1f              // desaturate → pure hue
            }

            else -> {
                val t = (ratio - 0.5f) * 2f                // 0 → 1 across right half
                hueSat.sat to lerp(1f, 0f, t)              // darken → black
            }
        }

        val selectedColor = Color.hsv(
            hueSat.hue,
            adjSat.coerceIn(0f, 1f),
            adjVal.coerceIn(0f, 1f)
        )

        // Only emit if we are interacting
        if (interactionOffset != null) {
            onColorSelected(selectedColor)
            if (isTapping) {
                interactionOffset = null
                isTapping = false
            }
        }

        val cy = h / 2
        drawCircle(Color.White, h / 2 + 4f, Offset(cx, cy))
        drawCircle(selectedColor, h / 2 - 2f, Offset(cx, cy))
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

fun Color.toHex(): String {
    fun Int.hex2() = toString(16).padStart(2, '0').uppercase()
    return "${(red * 255).toInt().hex2()}" +
            "${(green * 255).toInt().hex2()}" +
            "${(blue * 255).toInt().hex2()}"
}

fun Color.toRgb(): String {
    return "R: ${(red * 255).toInt()} " +
            "G: ${(green * 255).toInt()} " +
            "B: ${(blue * 255).toInt()}"
}