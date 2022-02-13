package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.geektang.privacyspace.R

@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    showLoading: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (showLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    modifier = Modifier.padding(top = 5.dp),
                    text = stringResource(R.string.loading)
                )
            }
        }
    }
}