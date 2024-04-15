package local.hapra.rescalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

fun Int.multiplier(): String {
    return when (this) {
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

fun Double.tolerance(): String {
    return DecimalFormat("#.##%").format(this)
}

fun Double.formatResistance(): String {
    val r = this
    val fmt = DecimalFormat("#.##")
    return when {
        r < 1000 -> "${fmt.format(r)}Ω"
        r < 1000_000 -> "${fmt.format(r / 1000)}kΩ"
        r < 1000_000_000 -> "${fmt.format(r / 1000_000)}MΩ"
        else -> "${fmt.format(r / 1000_000_000)}GΩ"
    }
}

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
            prefix?.let { (_, d) -> d } ?: return@derivedStateOf null,
            tolerance?.let { (_, d) -> d } ?: return@derivedStateOf null,
            tempCoefficient?.let { (_, d) -> d },
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DigitColumn(
                            selectionState = model.digitColors[0],
                            colors = ResColor.FOR_FIRST_DIGIT,
                            modifier = Modifier.weight(1f),
                        )
                        DigitColumn(
                            selectionState = model.digitColors[1],
                            colors = ResColor.FOR_OTHER_DIGIT,
                            modifier = Modifier.weight(1f),
                        )
                        DigitColumn(
                            selectionState = model.digitColors[2],
                            colors = ResColor.FOR_OTHER_DIGIT,
                            modifier = Modifier.weight(1f),
                        )

                        MultiplierColumn(
                            selectionState = model.multiplierColor,
                            colors = ResColor.FOR_MULTIPLIER,
                            modifier = Modifier.weight(1f),
                        )

                        ToleranceColumn(
                            selectionState = model.toleranceColor,
                            colors = ResColor.FOR_TOLERANCE,
                            modifier = Modifier.weight(1f),
                        )

                        TempCoefficientColumn(
                            selectionState = model.tempCoefficientColor,
                            colors = ResColor.FOR_TEMP_COEFFICIENT,
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
            text = value?.formatResistance() ?: "●",
            textAlign = TextAlign.Center,
            color = textColor,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DigitColumn(
    selectionState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
    modifier: Modifier,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.toString() },
        modifier = modifier,
    )
}

@Composable
fun MultiplierColumn(
    selectionState: MutableState<Pair<ResColor, Int>?>,
    colors: List<Pair<ResColor, Int>>,
    modifier: Modifier,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.multiplier() },
        modifier = modifier,
    )
}

@Composable
fun ToleranceColumn(
    selectionState: MutableState<Pair<ResColor, Double>?>,
    colors: List<Pair<ResColor, Double?>>,
    modifier: Modifier,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.tolerance() },
        modifier = modifier,
    )
}

@Composable
fun TempCoefficientColumn(
    selectionState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
    modifier: Modifier,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.toString() },
        modifier = modifier,
    )
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
        selection?.let { (col, value) ->
            val (bg, _) = col.toColor()
            ColorLine(
                color = bg,
                text = display(value),
                onClick = { selectionState.value = null },
            )
        } ?: run {
            ColorLine(color = MaterialTheme.colorScheme.surface)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
fun ColorLine(color: Color, text: String? = null, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick?.invoke() }
            .padding(8.dp)
    )
    {
        val elevation = when {
            text != null -> 2.dp
            else -> 0.dp
        }
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = color,
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier
                .width(12.dp)
                .height(48.dp)
        ) {}

        val textColor = when {
            (text != null) -> MaterialTheme.colorScheme.onBackground
            else -> MaterialTheme.colorScheme.surface
        }
        Text(
            text = text ?: "●",
            maxLines = 1,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
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
    val rounding by animateDpAsState(
        targetValue = if (selected) size / 2 else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val outerSize = size + (padding * 2)
    val innerShape = RoundedCornerShape(rounding)
    val outerShape = RoundedCornerShape(rounding + padding)
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .size(outerSize)
            .clip(outerShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .run {
                if (selected) {
                    background(
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
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clip(innerShape)
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