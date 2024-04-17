package local.hapra.rescalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import local.hapra.rescalc.ui.theme.ResCalcTheme
import java.text.DecimalFormat

fun ResColor.toColor(): Pair<Color, Color> {
    return when (this) {
        ResColor.Silver -> Color(0xFFE6E8FA) to Color.Black
        ResColor.Gold -> Color(0xFFFFC700) to Color.Black
        ResColor.Black -> Color.Black to Color.White
        ResColor.Brown -> Color(0xFF6D3F20) to Color.White
        ResColor.Red -> Color.Red to Color.White
        ResColor.Orange -> Color(0xFFF48B02) to Color.White
        ResColor.Yellow -> Color(0xFFF5ED06) to Color.Black
        ResColor.Green -> Color.Green to Color.Black
        ResColor.Blue -> Color.Blue to Color.White
        ResColor.Violet -> Color.Magenta to Color.White
        ResColor.Gray -> Color.Gray to Color.White
        ResColor.White -> Color.White to Color.Black
    }
}

fun displayResistance(r: Double): String {
    val fmt = DecimalFormat("#.##")
    return when {
        r < 1000 -> "${fmt.format(r)}Ω"
        r < 1000_000 -> "${fmt.format(r / 1000)}kΩ"
        r < 1000_000_000 -> "${fmt.format(r / 1000_000)}MΩ"
        else -> "${fmt.format(r / 1000_000_000)}GΩ"
    }
}

fun displayDigit(value: UInt) = value.toString()

fun displayMultiplier(value: Int): String {
    return when (value) {
        -2 -> "×0.01"
        -1 -> "×0.1"
        0 -> "×1"
        1 -> "×10"
        2 -> "×100"
        3 -> "×1k"
        4 -> "×10k"
        5 -> "×100k"
        6 -> "×1M"
        7 -> "×10M"
        8 -> "×100M"
        9 -> "×1G"
        else -> error("unimplemented")
    }
}

fun displayTolerance(value: Double): String {
    return DecimalFormat("#.##%").format(value)
}

fun displayTempCoefficient(value: UInt) = value.toString()

class MainViewModel(
    val digitColors: Array<MutableState<Pair<ResColor, UInt>?>> = Array(3) {
        mutableStateOf(null)
    },
    val multiplierColor: MutableState<Pair<ResColor, Int>?> = mutableStateOf(null),
    val toleranceColor: MutableState<Pair<ResColor, Double>?> = mutableStateOf(null),
    val tempCoefficientColor: MutableState<Pair<ResColor, UInt>?> = mutableStateOf(null),
) : ViewModel() {

    val resistor = derivedStateOf {
        val digit0 by digitColors[0]
        val digit1 by digitColors[1]
        val digit2 by digitColors[2]
        val prefix by multiplierColor
        val tolerance by toleranceColor
        val tempCoefficient by tempCoefficientColor

        val digits = sequenceOf(digit0?.second, digit1?.second, digit2?.second)
            .filterNotNull()
            .toList()
            .toTypedArray()
        if (digits.size < 2) return@derivedStateOf null

        Resistor.from(
            digits,
            prefix?.second ?: return@derivedStateOf null,
            tolerance?.second,
            tempCoefficient?.second
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model by viewModels<MainViewModel>()
            val resistor by model.resistor

            ResCalcTheme {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Resistance(
                            label = "min",
                            value = resistor?.minResistance(),
                            modifier = Modifier.weight(1f),
                        )
                        Resistance(
                            label = "resistance", value = resistor?.resistance(),
                            modifier = Modifier.weight(1f),
                        )
                        Resistance(
                            label = "max",
                            value = resistor?.maxResistance(),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorLine(
                            selectionState = model.digitColors[0],
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f)
                        )
                        ColorLine(
                            selectionState = model.digitColors[1],
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f)
                        )
                        ColorLine(
                            selectionState = model.digitColors[2],
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f)
                        )

                        ColorLine(
                            selectionState = model.multiplierColor,
                            display = ::displayMultiplier,
                            modifier = Modifier.weight(1f)
                        )
                        ColorLine(
                            selectionState = model.toleranceColor,
                            display = ::displayTolerance,
                            modifier = Modifier.weight(1f)
                        )
                        ColorLine(
                            selectionState = model.tempCoefficientColor,
                            display = ::displayTempCoefficient,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ColorColumn(
                            selectionState = model.digitColors[0],
                            colors = ResColor.FOR_FIRST_DIGIT,
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f),
                        )
                        ColorColumn(
                            selectionState = model.digitColors[1],
                            colors = ResColor.FOR_OTHER_DIGIT,
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f),
                        )
                        ColorColumn(
                            selectionState = model.digitColors[2],
                            colors = ResColor.FOR_OTHER_DIGIT,
                            display = ::displayDigit,
                            modifier = Modifier.weight(1f),
                        )

                        ColorColumn(
                            selectionState = model.multiplierColor,
                            colors = ResColor.FOR_MULTIPLIER,
                            display = ::displayMultiplier,
                            modifier = Modifier.weight(1f),
                        )
                        ColorColumn(
                            selectionState = model.toleranceColor,
                            colors = ResColor.FOR_TOLERANCE,
                            display = ::displayTolerance,
                            modifier = Modifier.weight(1f),
                        )
                        ColorColumn(
                            selectionState = model.tempCoefficientColor,
                            colors = ResColor.FOR_TEMP_COEFFICIENT,
                            display = ::displayTempCoefficient,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Resistance(label: String, value: Double?, modifier: Modifier) {
    Column(
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        val textColor = when {
            value != null -> MaterialTheme.colorScheme.onBackground
            else -> MaterialTheme.colorScheme.surface
        }
        Text(
            text = value?.let(::displayResistance) ?: "●",
            textAlign = TextAlign.Center,
            color = textColor,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        val labelColor = lerp(
            MaterialTheme.colorScheme.onBackground,
            MaterialTheme.colorScheme.background,
            0.5f,
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = labelColor,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun <T> ColorLine(
    selectionState: MutableState<Pair<ResColor, T>?>,
    display: (T) -> String,
    modifier: Modifier,
) {
    var selection by selectionState

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = selection != null) { selection = null }
            .padding(4.dp)
    ) {
        val bg = selection?.first?.toColor()?.first
            ?: MaterialTheme.colorScheme.surface
        val elevation = when {
            selection != null -> 2.dp
            else -> 0.dp
        }
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = bg,
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier
                .padding(4.dp)
                .width(12.dp)
                .height(48.dp)
        ) {}

        val text = selection?.second?.let(display) ?: "●"
        val textColor = when {
            (selection != null) -> MaterialTheme.colorScheme.onBackground
            else -> MaterialTheme.colorScheme.surface
        }
        Text(
            text = text,
            maxLines = 1,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun <T> ColorColumn(
    selectionState: MutableState<Pair<ResColor, T>?>,
    colors: List<Pair<ResColor, T?>>,
    display: (T) -> String,
    modifier: Modifier,
) {
    var selection by selectionState
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        colors.forEach { (col, value) ->
            val selected = selection?.first == col
            val text = value?.let(display).orEmpty()
            val onClick = value?.let {
                { selection = col to value }
            }
            ColorRect(
                color = col.toColor(),
                text = text,
                selected = selected,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun ColorRect(
    color: Pair<Color, Color>,
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val (bg, fg) = color
    val enabled = onClick != null

    val padding = 2.dp
    val size = 48.dp

    val elevation = when {
        selected -> 4.dp
        enabled -> 2.dp
        else -> 0.dp
    }
    var pressed by remember { mutableStateOf(false) }
    val selectionOutlineWidth by animateDpAsState(
        targetValue = if (selected) padding else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "selectionAnimation"
    )
    val rounding by animateDpAsState(
        targetValue = when {
            selected -> size / 2
            pressed -> 8.dp
            else -> 16.dp
        },
        animationSpec = spring(
            stiffness = lerp(
                Spring.StiffnessMediumLow,
                Spring.StiffnessMedium,
                0.5f
            )
        ),
        label = "RoundingAnimation",
    )
    val outerSize = size + (padding * 2)
    val innerShape = RoundedCornerShape(rounding)
    val outerShape = RoundedCornerShape(rounding + padding)
    Surface(
        color = Color.Transparent,
        shape = outerShape,
        modifier = Modifier
            .size(outerSize)
            .clickable(
                enabled = enabled,
                onClick = { onClick?.invoke() },
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .pointerInput(pressed) {
                if (enabled)
                    awaitPointerEventScope {
                        pressed = if (pressed) {
                            waitForUpOrCancellation()
                            false
                        } else {
                            awaitFirstDown(requireUnconsumed = false)
                            true
                        }
                    }
            }
            .run {
                if (selected) {
                    border(
                        width = selectionOutlineWidth,
                        shape = outerShape,
                        brush = Brush.sweepGradient(
                            0.0f to Color(0xFFD15CFC),
                            0.3f to Color(0xFF6A82FB),
                            0.7f to Color(0xFF1FF189),
                            1.0f to Color(0xFFD15CFC),
                        )
                    )
                } else {
                    this
                }
            }
    ) {
        Surface(
            color = if (enabled) bg else MaterialTheme.colorScheme.surface,
            shape = innerShape,
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = text,
                    style = TextStyle(color = fg),
                )
            }
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (start * (1 - fraction) + stop * fraction)
}
