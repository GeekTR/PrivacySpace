package cn.geektang.privacyspace.ui.screen.managehiddenapps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.ui.widget.*
import cn.geektang.privacyspace.util.*
import com.google.accompanist.insets.navigationBarsPadding
import kotlin.system.exitProcess

@Composable
fun AddHiddenAppsScreen(viewModel: AddHiddenAppsViewModel = viewModel()) {
    val allAppInfoList by viewModel.allAppInfoListFlow.collectAsState()
    val isLoading = allAppInfoList.isEmpty()
    val appInfoList by viewModel.appInfoListFlow.collectAsState()
    val hiddenAppList by viewModel.hiddenAppListFlow.collectAsState()
    val showSystemApps by viewModel.isShowSystemAppsFlow.collectAsState()
    val searchText by viewModel.searchTextFlow.collectAsState()
    val actions = object : AddHiddenAppsActions {
        override fun addApp2HiddenList(appInfo: AppInfo) {
            viewModel.addApp2HiddenList(appInfo)
        }

        override fun removeApp2HiddenList(appInfo: AppInfo) {
            viewModel.removeApp2HiddenList(appInfo)
        }

        override fun setSystemAppsVisible(showSystemApps: Boolean) {
            viewModel.setShowSystemApps(showSystemApps)
        }

        override fun onSearchTextChange(searchText: String) {
            viewModel.updateSearchText(searchText)
        }
    }

    AddHiddenAppsContent(
        appInfoList = appInfoList,
        hiddenAppList = hiddenAppList,
        searchText = searchText,
        isLoading = isLoading,
        showSystemApps = showSystemApps,
        actions = actions
    )

    val context = LocalContext.current
    NoticeDialogLocal(context)

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
private fun NoticeDialogLocal(context: Context) {
    var isShowAlterDialog by remember {
        mutableStateOf(!context.sp.hasReadNotice2)
    }
    if (isShowAlterDialog) {
        NoticeDialog(
            text = stringResource(R.string.tips_whitelist_magisk),
            onPositiveButtonClick = {
                isShowAlterDialog = false
                context.sp.hasReadNotice2 = true
            },
            onDismissRequest = {
                isShowAlterDialog = false
            })
    }
}

@Composable
fun AddHiddenAppsContent(
    appInfoList: List<AppInfo>,
    hiddenAppList: Set<String>,
    searchText: String,
    isLoading: Boolean,
    showSystemApps: Boolean,
    actions: AddHiddenAppsActions
) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    AddHiddenPopupMenu(
        isPopupMenuShow,
        showSystemApps,
        onSystemAppsVisibleChange = { showSystemApps ->
            isPopupMenuShow.value = false
            actions.setSystemAppsVisible(showSystemApps)
        })
    Column {
        val navController = LocalNavHostController.current
        SearchTopBar(
            title = stringResource(R.string.add_hidden_apps),
            searchText = searchText,
            onSearchTextChange = {
                actions.onSearchTextChange(it)
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
                    val isChecked = hiddenAppList.contains(appInfo.packageName)
                    AppInfoColumnItem(appInfo, isChecked, onClick = {
                        if (!hiddenAppList.contains(appInfo.packageName)) {
                            actions.addApp2HiddenList(appInfo)
                        } else {
                            actions.removeApp2HiddenList(appInfo)
                        }
                    })
                }
                item {
                    Box(modifier = Modifier.navigationBarsPadding())
                }
            })
        }
    }
}

@Composable
private fun AddHiddenPopupMenu(
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

@Preview(showSystemUi = true)
@Composable
fun AddHiddenAppsScreenPreview() {
    val context = LocalContext.current
    val data = AppInfo(
        appIcon = ColorDrawable(),
        packageName = BuildConfig.APPLICATION_ID,
        appName = context.getString(R.string.app_name),
        sharedUserId = null,
        isXposedModule = true,
        isSystemApp = false,
        applicationInfo = ApplicationInfo()
    )
    val actions = object : AddHiddenAppsActions {
    }
    AddHiddenAppsContent(
        listOf(data, data, data, data),
        emptySet(),
        searchText = "",
        showSystemApps = false,
        isLoading = false,
        actions = actions
    )
}

interface AddHiddenAppsActions {
    fun addApp2HiddenList(appInfo: AppInfo) {
    }

    fun removeApp2HiddenList(appInfo: AppInfo) {
    }

    fun setSystemAppsVisible(showSystemApps: Boolean) {
    }

    fun onSearchTextChange(searchText: String) {

    }
}