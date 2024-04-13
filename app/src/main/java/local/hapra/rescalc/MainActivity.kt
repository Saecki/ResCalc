package local.hapra.rescalc

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import local.hapra.rescalc.ui.theme.ResCalcTheme
import java.text.DecimalFormat

val disabledBg = Color(0XFFF2F2F2)

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

fun Resistor.formatResistance(): String {
    val r = resistance()
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

        Resistor.from(
            arrayOf(
                digit0?.let { (_, d) -> d } ?: return@derivedStateOf null,
                digit1?.let { (_, d) -> d } ?: return@derivedStateOf null,
                digit2?.let { (_, d) -> d } ?: return@derivedStateOf null,
            ),
            prefix?.let { (_, d) -> d } ?: return@derivedStateOf null,
            tolerance?.let { (_, d) -> d } ?: return@derivedStateOf null,
            tempCoefficient?.let { (_, d) -> d } ?: return@derivedStateOf null,
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
                Log.d("", "dark theme: ${isSystemInDarkTheme()}")
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = resistor?.formatResistance().orEmpty(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DigitColumn(
                            selectionState = model.digitColors[0],
                            colors = ResColor.FOR_FIRST_DIGIT
                        )
                        DigitColumn(
                            selectionState = model.digitColors[1],
                            colors = ResColor.FOR_OTHER_DIGIT
                        )
                        DigitColumn(
                            selectionState = model.digitColors[2],
                            colors = ResColor.FOR_OTHER_DIGIT
                        )

                        MultiplierColumn(
                            selectionState = model.multiplierColor,
                            colors = ResColor.FOR_MULTIPLIER,
                        )

                        ToleranceColumn(
                            selectionState = model.toleranceColor,
                            colors = ResColor.FOR_TOLERANCE,
                        )

                        TempCoefficientColumn(
                            selectionState = model.tempCoefficientColor,
                            colors = ResColor.FOR_TEMP_COEFFICIENT,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DigitColumn(
    selectionState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.toString() },
    )
}

@Composable
fun MultiplierColumn(
    selectionState: MutableState<Pair<ResColor, Int>?>,
    colors: List<Pair<ResColor, Int>>,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.multiplier() }
    )
}

@Composable
fun ToleranceColumn(
    selectionState: MutableState<Pair<ResColor, Double>?>,
    colors: List<Pair<ResColor, Double?>>,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.tolerance() }
    )
}

@Composable
fun TempCoefficientColumn(
    selectionState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
) {
    ColorColumn(
        selectionState = selectionState,
        colors = colors,
        display = { it.toString() }
    )
}

@Composable
fun <T> ColorColumn(
    selectionState: MutableState<Pair<ResColor, T>?>,
    colors: List<Pair<ResColor, T?>>,
    display: (T) -> String,
) {
    var selection by selectionState
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        selection?.let { (col, value) ->
            val (bg, _) = col.toColor()
            ColorLine(color = bg, text = display(value))
        } ?: run {
            ColorLine(color = disabledBg, text = null)
        }

        Spacer(modifier = Modifier.height(8.dp))

        colors.forEach { (col, value) ->
            val isSelected = selection?.let { (curCol, _) -> curCol == col }
            val text = value?.let(display).orEmpty()
            val onClick = value?.let {
                { selection = col to value }
            }
            ColorRect(
                color = col.toColor(),
                text = text,
                selected = isSelected,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun ColorLine(color: Color, text: String?) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = color,
        modifier = Modifier
            .width(12.dp)
            .height(48.dp)
    ) {}

    val textColor = if (text != null) Color.Black else Color.LightGray
    Text(
        text = text ?: "?",
        maxLines = 1,
        color = textColor,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun ColorRect(
    color: Pair<Color, Color>,
    text: String,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val enabled = onClick != null
    val elevation = if (enabled) 8.dp else 0.dp

    val (bg, fg) = color
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) bg else disabledBg,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = if (selected == true) {
            BorderStroke(
                width = 2.dp, color = fg,
            )
        } else {
            null
        },
        modifier = Modifier
            .width(48.dp)
            .aspectRatio(1.0f)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ResCalcTheme {
        DigitColumn(
            selectionState = remember { mutableStateOf(null) },
            colors = ResColor.FOR_FIRST_DIGIT
        )
    }
}