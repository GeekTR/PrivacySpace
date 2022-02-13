package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun PopupMenu(isShow: MutableState<Boolean>, content: @Composable () -> Unit) {
    Popup(
        alignment = Alignment.TopEnd,
        properties = PopupProperties(focusable = isShow.value),
        onDismissRequest = { isShow.value = false }) {
        if (isShow.value) {
            Surface(
                modifier = Modifier
                    .padding(end = 5.dp, top = 10.dp),
                elevation = 3.dp,
                shape = RoundedCornerShape(5.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PopupItem(text: String, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
            .fillMaxWidth(),
        maxLines = 1,
        text = text
    )
}

@Composable
fun PopupCheckboxItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clickable {
                onCheckedChange(!checked)
            }
            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked, onCheckedChange = { checked ->
            onCheckedChange(checked)
        })
        Text(
            maxLines = 1,
            text = text
        )
        Spacer(modifier = Modifier.width(15.dp))
    }
}
