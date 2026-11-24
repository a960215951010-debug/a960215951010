package tw.edu.pu.csim.timmy.digitalink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import tw.edu.pu.csim.timmy.digitalink.ui.theme.DigitalInkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalInkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Doodle(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private data class StrokeLine(
    val points: List<Offset>
)

@Composable
fun Doodle(modifier: Modifier = Modifier) {
    val defaultPrompt = "請在畫布區手寫，再按「開始辨識」按鈕"
    var recognizedText by remember { mutableStateOf(defaultPrompt) }

    // 建立一個路徑物件（使用 apply 簡化）
    var path by remember {
        mutableStateOf(
            Path().apply {
                moveTo(50f, 100f) // 移動到起始點 (50f, 100f)
                lineTo(200f, 500f) // 畫一條線到 (200f, 500f)
                lineTo(450f, 400f) // 畫一條線到 (450f, 400f)
                close() // 閉合路徑
            }
        )
    }

    val strokes = remember { mutableStateListOf<StrokeLine>() }
    var strokeVersion by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, Color.Gray)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            strokes.add(StrokeLine(mutableStateListOf(offset)))
                            strokeVersion++
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val currentStroke = strokes.lastOrNull()
                            if (currentStroke != null && currentStroke.points is MutableList) {
                                (currentStroke.points as MutableList<Offset>).add(change.position)
                                strokeVersion++
                            }
                        }
                    )
                }
        ) {
            @Suppress("UNUSED_EXPRESSION")
            strokeVersion

            // 繪製黑色路徑，透明度 1（不透明），線條粗細 10
            drawPath(path, Color.Black, 1f, Stroke(10f))

            // 保留原本的手寫軌跡繪製
            strokes.forEach { stroke ->
                if (stroke.points.size == 1) {
                    val p = stroke.points.first()
                    drawCircle(
                        color = Color.Black,
                        radius = 4.dp.toPx(),
                        center = p
                    )
                } else if (stroke.points.size > 1) {
                    val dragPath = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(
                        path = dragPath,
                        color = Color.Black,
                        style = Stroke(
                            width = 8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    recognizedText = if (strokes.isEmpty()) {
                        "尚未偵測到筆畫，請先在畫布區手寫"
                    } else {
                        "已送出 ${strokes.size} 筆畫進行辨識（待串接辨識模型）"
                    }
                }
            ) {
                Text("開始辨識")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 清除畫布
            Button(
                onClick = {
                    path = Path()
                    strokes.clear()
                    strokeVersion++
                    recognizedText = defaultPrompt
                }
            ) {
                Text("清除")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = recognizedText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}