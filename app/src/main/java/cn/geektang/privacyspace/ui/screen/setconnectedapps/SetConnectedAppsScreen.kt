package cn.geektang.privacyspace.ui.screen.setconnectedapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.ui.widget.*
import cn.geektang.privacyspace.util.LocalNavHostController
import cn.geektang.privacyspace.util.OnLifecycleEvent

@Composable
fun SetConnectedAppsScreen(viewModel: SetConnectedAppsViewModel = viewModel()) {
    val appList by viewModel.appListFlow.collectAsState()
    val whitelist by viewModel.whitelistFlow.collectAsState()
    val appName by viewModel.appNameFlow.collectAsState()
    val showSystemApps by viewModel.showSystemAppsFlow.collectAsState()
    val searchText by viewModel.searchTextFlow.collectAsState()
    val actions = object : SetConnectedAppsActions {
        override fun addApp2Whitelist(appInfo: AppInfo) {
            viewModel.addApp2HiddenList(appInfo)
        }

        override fun removeApp2Whitelist(appInfo: AppInfo) {
            viewModel.removeApp2HiddenList(appInfo)
        }

        override fun setSystemAppsVisible(showSystemApps: Boolean) {
            viewModel.setShowSystemApps(showSystemApps)
        }

        override fun onSearchTextChange(searchText: String) {
            viewModel.updateSearchText(searchText)
        }
    }
    SetConnectedAppsContent(
        appName = appName,
        showSystemApps = showSystemApps,
        searchText = searchText,
        appList = appList,
        whitelist = whitelist,
        actions = actions
    )

    OnLifecycleEvent(onEvent = { event ->
        if (event == Lifecycle.Event.ON_PAUSE
            || event == Lifecycle.Event.ON_STOP
            || event == Lifecycle.Event.ON_DESTROY
        ) {
            viewModel.tryUpdateConfig()
        }
    })
}

@Composable
private fun SetConnectedAppsContent(
    appName: String,
    showSystemApps: Boolean,
    searchText: String,
    appList: List<AppInfo>,
    whitelist: Set<String>,
    actions: SetConnectedAppsActions
) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    SetConnectedPopupMenu(
        isPopupMenuShow,
        showSystemApps,
        onSystemAppsVisibleChange = { showSystemApps ->
            isPopupMenuShow.value = false
            actions.setSystemAppsVisible(showSystemApps)
        })

    val navHostController = LocalNavHostController.current
    Column {
        SearchTopBar(
            title = stringResource(R.string.set_connected_apps),
            searchText = searchText,
            onSearchTextChange = {
                actions.onSearchTextChange(it)
            }, showMorePopupState = isPopupMenuShow,
            onNavigationIconClick = {
                navHostController.popBackStack()
            })
        if (appName.isNotBlank()) {
            Text(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.secondary)
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                text = String.format(stringResource(R.string.setting_up_for), appName, appName),
                style = MaterialTheme.typography.caption
            )
        }

        LoadingBox(modifier = Modifier.fillMaxSize(), showLoading = appList.isEmpty()) {
            LazyColumn(content = {
                items(appList) { appInfo ->
                    val isChecked = whitelist.contains(appInfo.packageName)
                    AppInfoColumnItem(appInfo = appInfo, isChecked = isChecked) {
                        if (!whitelist.contains(appInfo.packageName)) {
                            actions.addApp2Whitelist(appInfo)
                        } else {
                            actions.removeApp2Whitelist(appInfo)
                        }
                    }
                }
            })
        }
    }
}

@Composable
private fun SetConnectedPopupMenu(
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

interface SetConnectedAppsActions {
    fun addApp2Whitelist(appInfo: AppInfo) {
    }

    fun removeApp2Whitelist(appInfo: AppInfo) {
    }

    fun setSystemAppsVisible(showSystemApps: Boolean) {
    }

    fun onSearchTextChange(searchText: String) {

    }
}