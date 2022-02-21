package cn.geektang.privacyspace.ui.screen.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.RouteConstant
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.ui.widget.PopupItem
import cn.geektang.privacyspace.ui.widget.PopupMenu
import cn.geektang.privacyspace.util.*
import cn.geektang.privacyspace.util.AppHelper.getLauncherPackageName
import coil.compose.rememberImagePainter
import kotlin.system.exitProcess

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val appList by viewModel.appListFlow.collectAsState(initial = emptyList())
    val rootState by viewModel.rootStateFlow.collectAsState()
    val context = LocalContext.current
    val actions = object : LauncherActions {
        override fun uninstall(appInfo: AppInfo) {
            Runtime.getRuntime()
                .exec("su -c pm uninstall ${appInfo.packageName}")
            Toast.makeText(
                context,
                String.format(context.getString(R.string.app_is_uninstalled), appInfo.appName),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.onUninstallApp(appInfo)
        }

        override fun cancelHide(appInfo: AppInfo) {
            viewModel.cancelHide(appInfo)
        }

        override fun refreshRootStatus() {
            viewModel.checkRootAndInitConfig()
        }
    }
    LauncherScreenContent(appList, rootState, actions)

    var isShowAlterDialog by remember {
        mutableStateOf(!context.sp.hasReadNotice)
    }
    if (isShowAlterDialog) {
        AlertDialog(onDismissRequest = { isShowAlterDialog = false },
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
                        isShowAlterDialog = false
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

}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun LauncherScreenContent(
    appList: List<AppInfo>,
    rootState: Boolean,
    actions: LauncherActions
) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    LauncherPopupMenu(isPopupMenuShow, rootState)

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
    }
    AppItemPopupMenu(popupMenuAppInfo, popupMenuOffset, isShowPopupMenu, actionsWrapper)

    Column {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            actions = {
                IconButton(onClick = { isPopupMenuShow.value = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu)
                    )
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                cells = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                items(appList) {
                    AppItem(it, actionsWrapper)
                }
            }
            val loadStatus by ConfigHelper.loadingStatusFlow.collectAsState()
            if (rootState && appList.isEmpty() && loadStatus == ConfigHelper.LOADING_STATUS_SUCCESSFUL) {
                val navHostController = LocalNavHostController.current
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = { navHostController.navigate(RouteConstant.ADD_HIDDEN_APPS) }) {
                    Text(text = stringResource(id = R.string.add_hidden_apps))
                }
            } else if (!rootState) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 15.dp)
                ) {
                    Text(
                        text = stringResource(R.string.please_grant_root_permission),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            actions.refreshRootStatus()
                        }) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItemPopupMenu(
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
                    PopupItem(text = stringResource(id = R.string.set_connected_apps), onClick = {
                        navController.navigate("${RouteConstant.SET_CONNECTED_APPS}?targetPackageName=${popupMenuAppInfo?.packageName ?: ""}")
                    })
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
    PopupMenuContent(isShow, false)
}

@Composable
private fun LauncherPopupMenu(isPopupMenuShow: MutableState<Boolean>, rootState: Boolean) {
    PopupMenu(isShow = isPopupMenuShow) {
        PopupMenuContent(isPopupMenuShow, rootState)
    }
}

@Composable
private fun PopupMenuContent(isPopupMenuShow: MutableState<Boolean>, rootState: Boolean) {
    val navController = LocalNavHostController.current
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .width(IntrinsicSize.Max)
    ) {
        val context = LocalContext.current
        PopupItem(text = stringResource(id = R.string.set_white_list)) {
            if (!rootState) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_grant_root_permission),
                    Toast.LENGTH_SHORT
                ).show()
                return@PopupItem
            }
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.SET_WHITE_LIST)
        }
        PopupItem(text = stringResource(id = R.string.add_hidden_apps)) {
            if (!rootState) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_grant_root_permission),
                    Toast.LENGTH_SHORT
                ).show()
                return@PopupItem
            }
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.ADD_HIDDEN_APPS)
        }
        PopupItem(text = stringResource(R.string.reboot_desktop)) {
            if (!rootState) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_grant_root_permission),
                    Toast.LENGTH_SHORT
                ).show()
                return@PopupItem
            }

            isPopupMenuShow.value = false
            Runtime
                .getRuntime()
                .exec("su -c am force-stop ${context.getLauncherPackageName() ?: "com.miui.home"}")
        }
        PopupItem(text = stringResource(R.string.reboot_system)) {
            if (!rootState) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_grant_root_permission),
                    Toast.LENGTH_SHORT
                ).show()
                return@PopupItem
            }

            isPopupMenuShow.value = false
            Runtime
                .getRuntime()
                .exec("su -c reboot")
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
                context.showToast(context.getString(R.string.Coolapk_not_found))
            }

        })
        PopupItem(text = stringResource(R.string.view_update_info_github), onClick = {
            isPopupMenuShow.value = false
            context.openUrl("https://github.com/Xposed-Modules-Repo/cn.geektang.privacyspace")
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(appInfo: AppInfo, launcherActions: LauncherActions) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                painter = rememberImagePainter(data = appInfo.appIcon),
                contentDescription = appInfo.appName
            )
            Text(
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
        true,
        actions = object : LauncherActions {

        })
}

fun ApplicationInfo.isXposedModule(): Boolean {
    return metaData?.getBoolean("xposedmodule") == true ||
            metaData?.containsKey("xposedminversion") == true
}

interface LauncherActions {
    fun showAppItemMenu(appInfo: AppInfo, offset: Offset) {

    }

    fun uninstall(appInfo: AppInfo) {

    }

    fun cancelHide(appInfo: AppInfo) {

    }

    fun refreshRootStatus() {
    }
}