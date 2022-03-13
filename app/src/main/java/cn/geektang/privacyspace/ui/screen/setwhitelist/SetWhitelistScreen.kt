package cn.geektang.privacyspace.ui.screen.setwhitelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun SetWhitelistScreen(viewModel: SetWhitelistViewModel = viewModel()) {
    val allAllList by viewModel.allAppListFlow.collectAsState()
    val isLoading = allAllList.isEmpty()
    val appList by viewModel.appListFlow.collectAsState()
    val whitelist by viewModel.whitelistFlow.collectAsState()
    val showSystemApps by viewModel.showSystemAppsFlow.collectAsState()
    val searchText by viewModel.searchTextFlow.collectAsState()
    val showSelectAll by viewModel.showSelectAll.collectAsState()
    val actions = object : SetWhitelistActions {
        override fun addApp2Whitelist(appInfo: AppInfo) {
            viewModel.addApp2Whitelist(appInfo)
        }

        override fun removeApp2Whitelist(appInfo: AppInfo) {
            viewModel.removeApp2Whitelist(appInfo)
        }

        override fun setSystemAppsVisible(showSystemApps: Boolean) {
            viewModel.setSystemAppsVisible(showSystemApps)
        }

        override fun onSearchTextChange(searchText: String) {
            viewModel.updateSearchText(searchText)
        }

        override fun selectAllSystemApps(selectAll: Boolean) {
            viewModel.selectAllSystemApps(selectAll)
        }
    }
    SetWhiteListContent(
        appList = appList,
        whitelist = whitelist,
        searchText = searchText,
        isLoading = isLoading,
        showSystemApps = showSystemApps,
        showSelectAll = showSelectAll,
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
private fun SetWhiteListContent(
    appList: List<AppInfo>,
    whitelist: Set<String>,
    searchText: String,
    isLoading: Boolean,
    showSystemApps: Boolean,
    showSelectAll : Boolean,
    actions: SetWhitelistActions
) {
    val navHostController = LocalNavHostController.current
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    SetWhitelistPopupMenu(
        isPopupMenuShow,
        showSystemApps,
        showSelectAll,
        onSystemAppsVisibleChange = { showSystemApps ->
            isPopupMenuShow.value = false
            actions.setSystemAppsVisible(showSystemApps)
        }, selectAllCallback = { selectAll ->
            actions.selectAllSystemApps(selectAll)
        })
    Column {
        SearchTopBar(
            title = stringResource(R.string.set_white_list),
            searchText = searchText,
            onSearchTextChange = {
                actions.onSearchTextChange(it)
            }, showMorePopupState = isPopupMenuShow,
            onNavigationIconClick = {
                navHostController.popBackStack()
            })
        LoadingBox(modifier = Modifier.fillMaxSize(), showLoading = isLoading) {
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
private fun SetWhitelistPopupMenu(
    popupMenuShow: MutableState<Boolean>,
    showSystemApps: Boolean,
    showSelectAll : Boolean,
    onSystemAppsVisibleChange: (Boolean) -> Unit,
    selectAllCallback: (Boolean) -> Unit
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
            PopupCheckboxItem(
                text = stringResource(R.string.select_all_system_apps),
                checked = showSelectAll,
                onCheckedChange = selectAllCallback
            )
        }
    }
}

interface SetWhitelistActions {
    fun addApp2Whitelist(appInfo: AppInfo) {
    }

    fun removeApp2Whitelist(appInfo: AppInfo) {
    }

    fun setSystemAppsVisible(showSystemApps: Boolean) {
    }

    fun onSearchTextChange(searchText: String) {

    }

    fun selectAllSystemApps(selectAll: Boolean) {

    }
}