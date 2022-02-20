package cn.geektang.privacyspace.ui.widget

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cn.geektang.privacyspace.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    })
}

@Composable
fun SearchTopBar(
    title: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    showMorePopupState: MutableState<Boolean>,
    onNavigationIconClick: (() -> Unit)? = null
) {
    var showSearchBox by remember {
        mutableStateOf(false)
    }
    val focusRequester = FocusRequester()
    TopAppBar(title = {
        if (!showSearchBox) {
            Text(text = title)
        }
    }, actions = {
        if (showSearchBox) {
            SearchBoxTextField(focusRequester, searchText, onSearchTextChange)
        }

        val scope = rememberCoroutineScope()
        if (!showSearchBox) {
            IconButton(onClick = {
                showSearchBox = true
                scope.launch {
                    delay(100)
                    focusRequester.smartRequestFocus()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.menu)
                )
            }
        }

        IconButton(onClick = {
            showMorePopupState.value = true
        }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.menu)
            )
        }
    }, navigationIcon = {
        IconButton(onClick = {
            if (showSearchBox) {
                if (searchText.isNotEmpty()) {
                    onSearchTextChange("")
                }
                showSearchBox = false
            } else {
                onNavigationIconClick?.invoke()
            }
        }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }
    })
}

@Composable
private fun RowScope.SearchBoxTextField(
    focusRequester: FocusRequester,
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    TextField(
        modifier = Modifier
            .focusRequester(focusRequester)
            .weight(1f),
        placeholder = {
            Text(text = stringResource(R.string.search))
        },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colors.secondary,
            textColor = Color.White,
            placeholderColor = Color(0xffcccccc)
        ),
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        tint = Color(0xccffffff),
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            }
        },
        value = searchText,
        onValueChange = onSearchTextChange
    )
}

private suspend fun FocusRequester.smartRequestFocus() {
    try {
        requestFocus()
    } catch (e: Throwable) {
        delay(100)
    }
}