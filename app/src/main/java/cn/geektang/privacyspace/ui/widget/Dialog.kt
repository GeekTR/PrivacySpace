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
fun MessageDialog(
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    positiveButtonText: String = stringResource(id = R.string.confirm),
    negativeButtonText: String = stringResource(id = R.string.cancel),
    onPositiveButtonClick: () -> Unit = onDismissRequest,
    onNegativeButtonClick: () -> Unit = onDismissRequest
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNegativeButtonClick) {
                    Text(
                        text = negativeButtonText,
                        color = MaterialTheme.colors.secondary
                    )
                }
                TextButton(onClick = onPositiveButtonClick) {
                    Text(
                        text = positiveButtonText,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        },
        text = text,
        title = title
    )
}

@Composable
fun NoticeDialog(
    text: String,
    onDismissRequest: () -> Unit,
    onPositiveButtonClick: () -> Unit = onDismissRequest
) {
    MessageDialog(
        title = {
            Text(text = stringResource(R.string.tips))
        },
        text = {
            Text(text = text)
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        negativeButtonText = stringResource(id = R.string.launcher_notice_cancel),
        positiveButtonText = stringResource(id = R.string.launcher_notice_confirm),
        onNegativeButtonClick = {
            exitProcess(0)
        },
        onPositiveButtonClick = onPositiveButtonClick,
        onDismissRequest = onDismissRequest
    )
}