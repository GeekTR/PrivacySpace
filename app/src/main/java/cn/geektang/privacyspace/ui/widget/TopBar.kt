package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable

@Composable
fun TopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    showNavigationIcon: Boolean = true,
    onNavigationIconClick: (() -> Unit)? = null
) {
    TopAppBar(title = {
        Text(text = title)
    }, actions = actions, navigationIcon = {
        if (showNavigationIcon) {
            IconButton(onClick = {
                onNavigationIconClick?.invoke()
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
    })
}