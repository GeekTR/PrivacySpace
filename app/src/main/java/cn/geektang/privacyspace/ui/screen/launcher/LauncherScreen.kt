package cn.geektang.privacyspace.ui.screen.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.bean.ConfigData
import cn.geektang.privacyspace.bean.SystemUserInfo
import cn.geektang.privacyspace.constant.RouteConstant
import cn.geektang.privacyspace.constant.UiSettings
import cn.geektang.privacyspace.constant.UiSettings.obtainCellContentPaddingRatio
import cn.geektang.privacyspace.constant.UiSettings.obtainCellCount
import cn.geektang.privacyspace.ui.widget.PopupItem
import cn.geektang.privacyspace.ui.widget.PopupMenu
import cn.geektang.privacyspace.ui.widget.TopBar
import cn.geektang.privacyspace.util.*
import cn.geektang.privacyspace.util.AppHelper.getLauncherPackageName
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val appList = viewModel.hiddenAppList
    val systemUsers = viewModel.systemUsers
    val multiUserConfig = viewModel.multiUserConfig
    val configData by viewModel.configData
    val loadStatus by ConfigHelper.loadingStatusFlow.collectAsState()
    val context = LocalContext.current
    val isShowMultiUserConfigSettingsDialog = remember {
        mutableStateOf(false)
    }
    val focusAppInfo = remember {
        mutableStateOf<AppInfo?>(null)
    }

    val actions = object : LauncherActions {
        override fun uninstall(appInfo: AppInfo) {
            val uri = Uri.fromParts("package", appInfo.packageName, null)
            val intent = Intent(Intent.ACTION_DELETE, uri)
            context.startActivity(intent)
        }

        override fun cancelHide(appInfo: AppInfo) {
            viewModel.cancelHide(appInfo)
        }

        override fun connectTo(sourceApp: AppInfo, targetApp: String) {
            viewModel.connectTo(sourceApp, targetApp)
        }

        override fun disconnectTo(sourceApp: AppInfo, targetApp: String) {
            viewModel.disconnectTo(sourceApp, targetApp)
        }

        override fun onMultiUserConfigSettingsItemClick(appInfo: AppInfo) {
            focusAppInfo.value = appInfo
            isShowMultiUserConfigSettingsDialog.value = true
        }

        override fun changeMultiUserConfigSettingsMap(appInfo: AppInfo, checkedUsers: Set<Int>?) {
            viewModel.changeMultiUserConfig(appInfo, checkedUsers)
        }
    }

    val cellCountValue = context.obtainCellCount()
    val cellCount = remember { mutableStateOf(cellCountValue) }
    val cellContentPaddingRatioValue = context.obtainCellContentPaddingRatio()
    val cellContentPaddingRatio = remember { mutableStateOf(cellContentPaddingRatioValue) }
    UiSettings.WatchingAndSaveConfig(cellCount.value, cellContentPaddingRatio.value, context)
    val showAdjustLayoutDialog = remember {
        mutableStateOf(false)
    }
    LauncherScreenContent(
        appList = appList,
        systemUsers = systemUsers,
        configData = configData,
        cellCount = cellCount.value,
        cellContentPaddingRatio = cellContentPaddingRatio.value,
        loadStatus = loadStatus,
        showAdjustLayoutDialog = showAdjustLayoutDialog,
        actions = actions,
        syncConfig = viewModel::syncConfig
    )
    if (showAdjustLayoutDialog.value) {
        AdjustLayoutDialog(showAdjustLayoutDialog, cellCount, cellContentPaddingRatio)
    }

    if (isShowMultiUserConfigSettingsDialog.value) {
        MultiUserConfigSettingDialog(
            showState = isShowMultiUserConfigSettingsDialog,
            appInfo = focusAppInfo.value,
            systemUsers = systemUsers,
            multiUserConfigSettingsMap = multiUserConfig,
            actions = actions
        )
    }

    val isShowAlterDialog = remember {
        mutableStateOf(!context.sp.hasReadNotice)
    }
    if (isShowAlterDialog.value) {
        NoticeDialog(isShowAlterDialog, context)
    }
    val coroutineScope = rememberCoroutineScope()
    OnLifecycleEvent(onEvent = { event ->
        if (event == Lifecycle.Event.ON_PAUSE
            || event == Lifecycle.Event.ON_STOP
            || event == Lifecycle.Event.ON_DESTROY
        ) {
            coroutineScope.launch {
                viewModel.syncConfig()
            }
        }
    })
}

@Composable
private fun NoticeDialog(
    isShowAlterDialog: MutableState<Boolean>,
    context: Context
) {
    AlertDialog(onDismissRequest = { isShowAlterDialog.value = false },
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
                    isShowAlterDialog.value = false
                    context.sp.hasReadNotice = true
                }) {
                    Text(
                        text = stringResource(R.string.launcher_notice_confirm),
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }, text = {
            Text(text = stringResource(id = R.string.launcher_notice))
        }, title = {
            Text(text = stringResource(R.string.tips))
        })
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun LauncherScreenContent(
    appList: List<AppInfo>,
    systemUsers: List<SystemUserInfo>,
    configData: ConfigData,
    cellCount: Int,
    cellContentPaddingRatio: Float,
    loadStatus: Int,
    showAdjustLayoutDialog: MutableState<Boolean>,
    actions: LauncherActions,
    syncConfig: suspend () -> Unit,
) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    LauncherPopupMenu(isPopupMenuShow, showAdjustLayoutDialog, syncConfig, loadStatus)
    var popupMenuOffset: Offset? by remember {
        mutableStateOf(null)
    }
    var popupMenuAppInfo: AppInfo? by remember {
        mutableStateOf(null)
    }
    val isShowPopupMenu = remember {
        mutableStateOf(false)
    }
    val actionsWrapper = object : LauncherActions {
        override fun showAppItemMenu(appInfo: AppInfo, offset: Offset) {
            popupMenuAppInfo = appInfo
            popupMenuOffset = offset
            isShowPopupMenu.value = true
        }

        override fun uninstall(appInfo: AppInfo) {
            actions.uninstall(appInfo)
            isShowPopupMenu.value = false
        }

        override fun cancelHide(appInfo: AppInfo) {
            actions.cancelHide(appInfo)
            isShowPopupMenu.value = false
        }

        override fun connectTo(sourceApp: AppInfo, targetApp: String) {
            actions.connectTo(sourceApp, targetApp)
            isShowPopupMenu.value = false
        }

        override fun disconnectTo(sourceApp: AppInfo, targetApp: String) {
            actions.disconnectTo(sourceApp, targetApp)
            isShowPopupMenu.value = false
        }

        override fun onMultiUserConfigSettingsItemClick(appInfo: AppInfo) {
            actions.onMultiUserConfigSettingsItemClick(appInfo)
            isShowPopupMenu.value = false
        }
    }

    AppItemPopupMenu(
        configData = configData,
        systemUsers = systemUsers,
        popupMenuAppInfo = popupMenuAppInfo,
        popupMenuOffset = popupMenuOffset,
        isShowPopupMenu = isShowPopupMenu,
        actions = actionsWrapper
    )

    Column {
        TopBar(
            title = stringResource(id = R.string.app_name),
            showNavigationIcon = false,
            actions = {
                val context = LocalContext.current
                val navController = LocalNavHostController.current
                Row {
                    IconButton(onClick = {
                        if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                            context.showToast(context.getString(R.string.tips_go_active))
                            return@IconButton
                        }
                        isPopupMenuShow.value = false
                        navController.navigate(RouteConstant.ADD_HIDDEN_APPS)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add_hidden_apps)
                        )
                    }
                    IconButton(onClick = { isPopupMenuShow.value = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu)
                        )
                    }
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                cells = GridCells.Fixed(cellCount),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                items(appList) {
                    AppItem(it, cellContentPaddingRatio, actionsWrapper)
                }
            }
            if (appList.isEmpty() && loadStatus == ConfigHelper.LOADING_STATUS_SUCCESSFUL) {
                val navHostController = LocalNavHostController.current
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = { navHostController.navigate(RouteConstant.ADD_HIDDEN_APPS) }) {
                    Text(text = stringResource(id = R.string.add_hidden_apps))
                }
            } else if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 15.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tips_restart_to_active),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemPopupMenu(
    configData: ConfigData,
    systemUsers: List<SystemUserInfo>,
    popupMenuAppInfo: AppInfo?,
    popupMenuOffset: Offset?,
    isShowPopupMenu: MutableState<Boolean>,
    actions: LauncherActions
) {
    val isShow = isShowPopupMenu.value
    Popup(
        properties = PopupProperties(focusable = isShow),
        offset = IntOffset(popupMenuOffset?.x?.toInt() ?: 0, popupMenuOffset?.y?.toInt() ?: 0),
        onDismissRequest = { isShowPopupMenu.value = false }) {
        if (isShow) {
            Surface(
                modifier = Modifier
                    .padding(end = 5.dp, top = 10.dp),
                elevation = 3.dp,
                shape = RoundedCornerShape(5.dp)
            ) {
                val navController = LocalNavHostController.current
                val context = LocalContext.current
                val launcherPackageName = remember {
                    context.getLauncherPackageName() ?: ""
                }
                val isShowDesktopMenu = !configData.whitelist.contains(launcherPackageName)
                val isHideToDesktop =
                    configData.connectedApps[popupMenuAppInfo?.packageName ?: ""]?.contains(
                        launcherPackageName
                    ) == true
                Column(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    PopupItem(text = stringResource(R.string.uninstall), onClick = {
                        actions.uninstall(popupMenuAppInfo ?: return@PopupItem)
                    })
                    PopupItem(text = stringResource(R.string.unhide), onClick = {
                        actions.cancelHide(popupMenuAppInfo ?: return@PopupItem)
                    })
                    if (isShowDesktopMenu) {
                        val text =
                            if (isHideToDesktop) stringResource(R.string.disconnect_with_desktop) else stringResource(
                                R.string.connect_with_desktop
                            )
                        PopupItem(text = text, onClick = {
                            if (isHideToDesktop) {
                                actions.disconnectTo(popupMenuAppInfo!!, launcherPackageName)
                            } else {
                                actions.connectTo(popupMenuAppInfo!!, launcherPackageName)
                            }
                        })
                    }
                    PopupItem(text = stringResource(id = R.string.set_connected_apps), onClick = {
                        navController.navigate("${RouteConstant.SET_CONNECTED_APPS}?targetPackageName=${popupMenuAppInfo?.packageName ?: ""}")
                    })
                    if (systemUsers.isNotEmpty()) {
                        PopupItem(text = stringResource(R.string.multi_users_settings), onClick = {
                            actions.onMultiUserConfigSettingsItemClick(popupMenuAppInfo!!)
                        })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LauncherPopupMenuPreview() {
    val isShow = remember {
        mutableStateOf(true)
    }
    PopupMenuContent(isShow, remember {
        mutableStateOf(false)
    }, {}, ConfigHelper.LOADING_STATUS_SUCCESSFUL)
}

@Composable
private fun LauncherPopupMenu(
    isPopupMenuShow: MutableState<Boolean>,
    showAdjustLayoutDialog: MutableState<Boolean>,
    syncConfig: suspend () -> Unit,
    loadStatus: Int
) {
    PopupMenu(isShow = isPopupMenuShow) {
        PopupMenuContent(isPopupMenuShow, showAdjustLayoutDialog, syncConfig, loadStatus)
    }
}

@Composable
private fun PopupMenuContent(
    isPopupMenuShow: MutableState<Boolean>,
    showAdjustLayoutDialog: MutableState<Boolean>,
    syncConfig: suspend () -> Unit,
    loadStatus: Int
) {
    val navController = LocalNavHostController.current
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .width(IntrinsicSize.Max)
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        PopupItem(text = stringResource(id = R.string.set_white_list)) {
            if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                context.showToast(context.getString(R.string.tips_go_active))
                return@PopupItem
            }
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.SET_WHITE_LIST)
        }
        PopupItem(text = stringResource(id = R.string.add_hidden_apps)) {
            if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                context.showToast(context.getString(R.string.tips_go_active))
                return@PopupItem
            }
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.ADD_HIDDEN_APPS)
        }
        PopupItem(text = stringResource(R.string.reboot_desktop)) {
            isPopupMenuShow.value = false
            scope.launch {
                syncConfig()
                val isSucceed =
                    Su.exec("am force-stop ${context.getLauncherPackageName() ?: "com.miui.home"}")
                if (isSucceed) {
                    context.showToast(context.getString(R.string.desktop_restart_successfully))
                } else {
                    context.showToast(context.getString(R.string.desktop_restart_failed))
                }
            }
        }
        RestartSystemPopupItem(loadStatus, context, isPopupMenuShow)
        PopupItem(text = stringResource(R.string.screen_layout)) {
            if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                context.showToast(context.getString(R.string.tips_go_active))
                return@PopupItem
            }
            isPopupMenuShow.value = false
            showAdjustLayoutDialog.value = true
        }
        PopupItem(text = stringResource(R.string.current_app_version), onClick = {
            isPopupMenuShow.value = false
            context.showToast(
                String.format(
                    context.getString(R.string.current_app_version_is),
                    BuildConfig.VERSION_NAME
                )
            )
        })
        PopupItem(text = stringResource(R.string.view_update_info_coolapk), onClick = {
            isPopupMenuShow.value = false
            try {
                context.openUrl("coolmarket://u/18765870")
            } catch (e: Throwable) {
                context.showToast(context.getString(R.string.coolapk_not_found))
            }
        })
        PopupItem(text = stringResource(R.string.view_update_info_github), onClick = {
            isPopupMenuShow.value = false
            context.openUrl("https://github.com/GeekTR/PrivacySpace")
        })
    }
}

@Composable
private fun RestartSystemPopupItem(
    loadStatus: Int,
    context: Context,
    isPopupMenuShow: MutableState<Boolean>
) {
    var isShowConfirmDialog by remember {
        mutableStateOf(false)
    }
    if (isShowConfirmDialog) {
        AlertDialog(onDismissRequest = { }, text = {
            Text(modifier = Modifier.fillMaxWidth(), text = stringResource(R.string.tips_confirm_restart_system))
        }, buttons = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { isShowConfirmDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    if (loadStatus == ConfigHelper.LOADING_STATUS_FAILED) {
                        context.showToast(context.getString(R.string.tips_go_active))
                        return@TextButton
                    }
                    isPopupMenuShow.value = false
                    isShowConfirmDialog = false
                    ConfigHelper.rebootTheSystem()
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        })
    }

    PopupItem(text = stringResource(R.string.reboot_system)) {
        isShowConfirmDialog = true
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    appInfo: AppInfo,
    cellContentPaddingRatio: Float,
    launcherActions: LauncherActions
) {
    val context = LocalContext.current
    var appItemOffset = remember {
        Offset.Zero
    }
    var appItemSize = remember {
        IntSize.Zero
    }
    Box(modifier = Modifier
        .onGloballyPositioned {
            appItemOffset = it.localToRoot(Offset.Zero)
            appItemSize = it.size
        }
        .combinedClickable(
            onLongClick = {
                onAppItemLongClick(appInfo, launcherActions, appItemSize, appItemOffset)
            },
            onClick = {
                onAppItemClick(appInfo, context)
            }
        )
        .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val paddingValue = (cellContentPaddingRatio * 20).dp
            Image(
                modifier = Modifier
                    .padding(horizontal = paddingValue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                painter = rememberImagePainter(data = appInfo.appIcon),
                contentDescription = appInfo.appName
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = appInfo.appName,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

private fun onAppItemLongClick(
    appInfo: AppInfo,
    launcherActions: LauncherActions,
    appItemSize: IntSize,
    offset: Offset
) {
    val popupOffset = Offset(
        offset.x + appItemSize.width / 2f,
        offset.y + appItemSize.height / 2f
    )
    launcherActions.showAppItemMenu(appInfo = appInfo, offset = popupOffset)
}

private fun onAppItemClick(
    appInfo: AppInfo,
    context: Context
) {
    if (appInfo.packageName == BuildConfig.APPLICATION_ID) {
        return
    }
    var intent: Intent? = null
    if (appInfo.isXposedModule) {
        intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("de.robv.android.xposed.category.MODULE_SETTINGS")
            setPackage(appInfo.packageName)
        }
        val flag =
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        val activityClassName = context.packageManager
            .queryIntentActivities(
                intent,
                flag
            )
            .firstOrNull()
            ?.activityInfo?.name
        if (activityClassName.isNullOrEmpty()) {
            intent = null
        } else {
            intent.setClassName(appInfo.packageName, activityClassName)
        }
    }
    if (null == intent) {
        intent =
            context.packageManager.getLaunchIntentForPackage(appInfo.applicationInfo.packageName)
    }
    if (null != intent) {
        try {
            context.startActivity(intent)
        } catch (ignored: Throwable) {
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun LauncherScreenPreview() {
    val context = LocalContext.current
    val appInfo = AppInfo(
        appIcon = ColorDrawable(),
        packageName = BuildConfig.APPLICATION_ID,
        appName = context.getString(R.string.app_name),
        isXposedModule = true,
        isSystemApp = true,
        applicationInfo = ApplicationInfo()
    )
    LauncherScreenContent(
        appList = listOf(appInfo, appInfo, appInfo, appInfo, appInfo, appInfo),
        systemUsers = emptyList(),
        configData = ConfigData.EMPTY,
        cellCount = 4,
        cellContentPaddingRatio = 0.5f,
        ConfigHelper.LOADING_STATUS_SUCCESSFUL,
        showAdjustLayoutDialog = remember {
            mutableStateOf(false)
        },
        actions = object : LauncherActions {

        }, syncConfig = {

        })
}


interface LauncherActions {
    fun showAppItemMenu(appInfo: AppInfo, offset: Offset) {

    }

    fun uninstall(appInfo: AppInfo) {

    }

    fun cancelHide(appInfo: AppInfo) {

    }

    fun connectTo(sourceApp: AppInfo, targetApp: String) {

    }

    fun disconnectTo(sourceApp: AppInfo, targetApp: String) {

    }

    fun onMultiUserConfigSettingsItemClick(appInfo: AppInfo) {
    }

    fun changeMultiUserConfigSettingsMap(appInfo: AppInfo, checkedUsers: Set<Int>?) {
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AdjustLayoutDialog(
    showAdjustLayoutDialog: MutableState<Boolean>,
    cellCount: MutableState<Int>,
    cellContentPaddingRatio: MutableState<Float>
) {
    Dialog(
        onDismissRequest = {
            showAdjustLayoutDialog.value = false
        }
    ) {
        Surface(shape = MaterialTheme.shapes.large) {
            val orientation = LocalConfiguration.current.orientation

            Column {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    text = stringResource(id = R.string.screen_layout)
                )
                Row(modifier = Modifier.padding(horizontal = 15.dp)) {
                    val cellCountChoices = UiSettings.getCellCountChoices(orientation)
                    for ((index, cellCountChoice) in cellCountChoices.withIndex()) {
                        if (index != 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        RadioButton(
                            cellCount = cellCountChoice,
                            cellCount.value == cellCountChoice,
                            onClick = {
                                cellCount.value = cellCountChoice
                            })
                    }
                }
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .padding(start = 15.dp), text = stringResource(R.string.icon_size)
                )

                Slider(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    value = cellContentPaddingRatio.value,
                    onValueChange = { newValue ->
                        cellContentPaddingRatio.value = newValue
                    })
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RadioButton(cellCount: Int, isChecked: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isChecked) {
        MaterialTheme.colors.primary
    } else {
        Color.LightGray
    }
    val textColor = if (isChecked) {
        Color.White
    } else {
        Color.Unspecified
    }
    val text = stringResource(R.string.icon_size_per_line, cellCount)
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 3.dp),
        color = textColor,
        text = text
    )
}

@Composable
private fun MultiUserConfigSettingDialog(
    showState: MutableState<Boolean>,
    appInfo: AppInfo?,
    systemUsers: List<SystemUserInfo>,
    multiUserConfigSettingsMap: Map<String, Set<Int>>,
    actions: LauncherActions
) {
    val targetSetting = multiUserConfigSettingsMap[appInfo?.packageName ?: ""]
    val usersCheckedStatus = remember(appInfo) {
        mutableStateOf(usersWithCheckedStatus(systemUsers, targetSetting))
    }
    Dialog(onDismissRequest = {
        showState.value = false
        val users = usersCheckedStatus.value
        when {
            users.isAllUnchecked() -> {
                actions.cancelHide(appInfo!!)
            }
            users.isAllChecked() -> {
                actions.changeMultiUserConfigSettingsMap(appInfo!!, null)
            }
            else -> {
                actions.changeMultiUserConfigSettingsMap(appInfo!!, users.toCheckedStatusSet())
            }
        }
    }) {
        MultiUserConfigSettingDialogContent(usersCheckedStatus)
    }
}

private fun List<Pair<SystemUserInfo, Boolean>>.toCheckedStatusSet(): Set<Int> {
    val checkedStatusSet = mutableSetOf<Int>()
    for (pair in this) {
        if (pair.second) {
            checkedStatusSet.add(pair.first.id)
        }
    }
    return checkedStatusSet
}

private fun List<Pair<SystemUserInfo, Boolean>>.isAllChecked(): Boolean {
    var isAllChecked = true
    for (pair in this) {
        if (!pair.second) {
            isAllChecked = false
        }
    }
    return isAllChecked
}

private fun List<Pair<SystemUserInfo, Boolean>>.isAllUnchecked(): Boolean {
    var isAllUnchecked = true
    for (pair in this) {
        if (pair.second) {
            isAllUnchecked = false
        }
    }
    return isAllUnchecked
}

private fun usersWithCheckedStatus(
    systemUsers: List<SystemUserInfo>,
    targetSetting: Set<Int>?
) = systemUsers.map { it to (null == targetSetting || targetSetting.contains(it.id)) }

@Composable
private fun MultiUserConfigSettingDialogContent(users: MutableState<List<Pair<SystemUserInfo, Boolean>>>) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 20.dp)) {
            Text(
                modifier = Modifier.padding(bottom = 10.dp),
                text = stringResource(R.string.checked_means_needs_to_be_hidden),
                style = MaterialTheme.typography.subtitle1
            )
            val usersValue = users.value
            usersValue.forEach { userWithCheckedStatus ->
                val user = userWithCheckedStatus.first
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = userWithCheckedStatus.second, onCheckedChange = {
                        val newValue = usersValue.toMutableList()
                        val index = usersValue.indexOf(userWithCheckedStatus)
                        newValue.removeAt(index)
                        newValue.add(index, user to !userWithCheckedStatus.second)
                        users.value = newValue
                    })
                    Text(text = user.name)
                }
            }
        }
    }
}