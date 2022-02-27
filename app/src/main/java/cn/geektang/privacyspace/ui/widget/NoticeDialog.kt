package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cn.geektang.privacyspace.R
import kotlin.system.exitProcess

@Composable
fun NoticeDialog(text: String, onDismissRequest: () -> Unit) {
    AlertDialog(onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    exitProcess(0)
                }) {
                    Text(
                        text = stringResource(R.string.launcher_notice_cancel),
                        color = MaterialTheme.colors.secondary
                    )
                }
                TextButton(onClick = {
                    onDismissRequest()
                }) {
                    Text(
                        text = stringResource(R.string.launcher_notice_confirm),
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }, text = {
            Text(text = text)
        }, title = {
            Text(text = stringResource(R.string.tips))
        })
}