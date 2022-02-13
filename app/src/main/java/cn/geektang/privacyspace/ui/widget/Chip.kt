package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Chip(text: String) {
    Text(
        modifier = Modifier
            .background(
                color = Color(0xffebebeb),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        text = text,
        style = MaterialTheme.typography.caption
    )
}

@Preview
@Composable
fun ChipPreview(){
    Chip(text = "XposedModule")
}