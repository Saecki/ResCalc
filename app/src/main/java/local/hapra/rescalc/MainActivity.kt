package local.hapra.rescalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import local.hapra.rescalc.ui.theme.ResCalcTheme

fun ResColor.toColor(): Pair<Color, Color> {
    return when (this) {
        ResColor.Silver -> Color(0xFFE6E8FA) to Color.Black
        ResColor.Gold -> Color(0xFFff9700) to Color.White
        ResColor.Black -> Color.Black to Color.White
        ResColor.Brown -> Color(0xFF6D3F20) to Color.White
        ResColor.Red -> Color.Red to Color.White
        ResColor.Orange -> Color(0xFFF49B02) to Color.White
        ResColor.Yellow -> Color.Yellow to Color.Black
        ResColor.Green -> Color.Green to Color.Black
        ResColor.Blue -> Color.Blue to Color.White
        ResColor.Violet -> Color.Magenta to Color.White
        ResColor.Gray -> Color.Gray to Color.White
        ResColor.White -> Color.White to Color.Black
    }
}

fun Int.multiplier(): String {
    return when (this) {
        -2 -> "x0.01"
        -1 -> "x0.1"
        0 -> "x1"
        1 -> "x10"
        2 -> "x100"
        3 -> "x1k"
        4 -> "x10k"
        5 -> "x100k"
        6 -> "x1M"
        7 -> "x10M"
        8 -> "x100M"
        9 -> "x1G"
        else -> error("unimplemented")
    }
}

fun Double.tolerance(): String {
    return "${this * 100}%"
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
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Text(text = resistor?.formatResistance().orEmpty())
                    Row(
                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DigitColumn(
                            selectedState = model.digitColors[0],
                            colors = ResColor.FOR_FIRST_DIGIT
                        )
                        DigitColumn(
                            selectedState = model.digitColors[1],
                            colors = ResColor.FOR_OTHER_DIGIT
                        )
                        DigitColumn(
                            selectedState = model.digitColors[2],
                            colors = ResColor.FOR_OTHER_DIGIT
                        )

                        MultiplierColumn(
                            selectedState = model.multiplierColor,
                            colors = ResColor.FOR_MULTIPLIER,
                        )

                        ToleranceColumn(
                            selectedState = model.toleranceColor,
                            colors = ResColor.FOR_TOLERANCE,
                        )

                        TempCoefficientColumn(
                            selectedState = model.tempCoefficientColor,
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
    selectedState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
) {
    ColorColumn(
        selectedState = selectedState,
        colors = colors,
        display = { it.toString() },
    )
}

@Composable
fun MultiplierColumn(
    selectedState: MutableState<Pair<ResColor, Int>?>,
    colors: List<Pair<ResColor, Int>>,
) {
    ColorColumn(
        selectedState = selectedState,
        colors = colors,
        display = { it.multiplier() }
    )
}

@Composable
fun ToleranceColumn(
    selectedState: MutableState<Pair<ResColor, Double>?>,
    colors: List<Pair<ResColor, Double?>>,
) {
    ColorColumn(
        selectedState = selectedState,
        colors = colors,
        display = { it.tolerance() }
    )
}

@Composable
fun TempCoefficientColumn(
    selectedState: MutableState<Pair<ResColor, UInt>?>,
    colors: List<Pair<ResColor, UInt?>>,
) {
    ColorColumn(
        selectedState = selectedState,
        colors = colors,
        display = { it.toString() }
    )
}

@Composable
fun <T> ColorColumn(
    selectedState: MutableState<Pair<ResColor, T>?>,
    colors: List<Pair<ResColor, T?>>,
    display: (T) -> String,
) {
    var selected by selectedState
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val disabledColor = (Color(0XFFEFEFEF) to Color.Black)

        selected?.let { (col, value) ->
            ColorLine(color = col.toColor(), text = display(value))
        } ?: run {
            ColorLine(color = disabledColor, text = "")
        }

        Spacer(modifier = Modifier.height(24.dp))

        colors.forEach { (col, value) ->
            val isSelected = selected?.let { (curCol, _) -> curCol == col }
            val text = value?.let(display).orEmpty()
            val onClick = value?.let {
                { selected = col to value }
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
fun ColorLine(color: Pair<Color, Color>, text: String) {
    val (bg, _) = color
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = bg,
        modifier = Modifier
            .width(16.dp)
            .height(48.dp)
    ) {}
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = Color.Black),
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
        color = if (enabled) bg else Color(0XFFEFEFEF),
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
        Text(
            text = text,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(color = fg),
            modifier = Modifier.fillMaxSize(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ResCalcTheme {
        DigitColumn(
            selectedState = remember { mutableStateOf(null) },
            colors = ResColor.FOR_FIRST_DIGIT
        )
    }
}