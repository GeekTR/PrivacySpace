package cn.geektang.privacyspace.ui.screen.blind

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.constant.RouteConstant
import cn.geektang.privacyspace.ui.widget.*
import cn.geektang.privacyspace.util.LocalNavHostController
import cn.geektang.privacyspace.util.OnLifecycleEvent
import com.google.accompanist.insets.navigationBarsPadding

@Composable
fun AddBlindAppsScreen(viewModel: AddBlindAppsViewModel = viewModel()) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    val searchText = viewModel.searchTextFlow.collectAsState().value
    val isLoading = viewModel.allAppInfoListFlow.collectAsState().value.isEmpty()
    val appInfoList = viewModel.appInfoListFlow.collectAsState().value
    val blindApps = viewModel.blindAppsListFlow.collectAsState().value
    val whitelistApps = viewModel.whitelistFlow.collectAsState().value
    val showSystemApps = viewModel.isShowSystemAppsFlow.collectAsState().value
    AddBlindAppsScreen(
        isPopupMenuShow,
        showSystemApps,
        onSystemAppsVisibleChange = { showSystemApps ->
            isPopupMenuShow.value = false
            viewModel.setSystemAppsVisible(showSystemApps)
        })
    Column {
        val navController = LocalNavHostController.current
        SearchTopBar(
            title = stringResource(R.string.blind),
            searchText = searchText,
            onSearchTextChange = {
                viewModel.updateSearchText(it)
            }, showMorePopupState = isPopupMenuShow,
            onNavigationIconClick = {
                navController.popBackStack()
            })
        LoadingBox(
            modifier = Modifier.fillMaxSize(),
            showLoading = isLoading
        ) {
            LazyColumn(content = {
                items(appInfoList) { appInfo ->
                    AppItem(blindApps, appInfo, whitelistApps, viewModel)
                }
                item {
                    Box(modifier = Modifier.navigationBarsPadding())
                }
            })
        }
    }
    OnLifecycleEvent { event ->
        if (event == Lifecycle.Event.ON_PAUSE
            || event == Lifecycle.Event.ON_STOP
            || event == Lifecycle.Event.ON_DESTROY
        ) {
            viewModel.tryUpdateConfig()
        }
    }
}

@Composable
private fun AppItem(
    blindApps: Set<String>,
    appInfo: AppInfo,
    whitelistApps: Set<String>,
    actions: AddBlindAppsActions
) {
    var showConfirmDialog by remember {
        mutableStateOf(false)
    }
    val isChecked = blindApps.contains(appInfo.packageName)
    Column {
        val customButtons = remember(isChecked) {
            if (isChecked) {
                val setConnectedAppsButton: (@Composable () -> Unit) = {
                    val navController = LocalNavHostController.current
                    Chip(
                        modifier = Modifier
                            .padding(all = 2.5.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                navController.navigate("${RouteConstant.SET_CONNECTED_APPS}?targetPackageName=${appInfo.packageName}")
                            },
                        color = MaterialTheme.colors.primary,
                        text = stringResource(R.string.click_to_set_connected_apps)
                    )
                }
                listOf(setConnectedAppsButton)
            } else {
                emptyList()
            }
        }
        AppInfoColumnItem(appInfo, isChecked, customButtons, onClick = {
            if (!blindApps.contains(appInfo.packageName)) {
                if (whitelistApps.contains(appInfo.packageName)) {
                    showConfirmDialog = true
                } else {
                    actions.addApp2BlindList(appInfo)
                }
            } else {
                actions.removeApp2BlindList(appInfo)
            }
        })
    }
    if (showConfirmDialog) {
        MessageDialog(
            text = {
                Text(text = stringResource(id = R.string.tips_cancel_whitelist, appInfo.appName))
            }, onPositiveButtonClick = {
                actions.addApp2BlindList(appInfo)
                showConfirmDialog = false
            }, onDismissRequest = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun AddBlindAppsScreen(
    popupMenuShow: MutableState<Boolean>,
    showSystemApps: Boolean,
    onSystemAppsVisibleChange: (Boolean) -> Unit
) {
    PopupMenu(isShow = popupMenuShow) {
        Column(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .width(IntrinsicSize.Max)
        ) {
            PopupCheckboxItem(
                text = stringResource(R.string.display_system_apps),
                checked = showSystemApps,
                onCheckedChange = onSystemAppsVisibleChange
            )
        }
    }
}

interface AddBlindAppsActions {
    fun addApp2BlindList(appInfo: AppInfo) {
    }

    fun removeApp2BlindList(appInfo: AppInfo) {
    }

    fun setSystemAppsVisible(showSystemApps: Boolean) {
    }

    fun updateSearchText(searchText: String) {

    }
}