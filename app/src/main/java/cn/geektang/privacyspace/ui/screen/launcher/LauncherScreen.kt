package cn.geektang.privacyspace.ui.screen.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.RouteConstant
import cn.geektang.privacyspace.bean.AppInfo
import cn.geektang.privacyspace.util.LocalNavHostController
import cn.geektang.privacyspace.ui.widget.PopupItem
import cn.geektang.privacyspace.ui.widget.PopupMenu
import cn.geektang.privacyspace.util.AppHelper
import cn.geektang.privacyspace.util.AppHelper.getLauncherPackageName
import cn.geektang.privacyspace.util.OnLifecycleEvent
import coil.compose.rememberImagePainter

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val appList by viewModel.appListFlow.collectAsState()
    OnLifecycleEvent(onEvent = {
        if (it == Lifecycle.Event.ON_RESUME) {
            viewModel.refreshAppList()
        }
    })
    LauncherScreenContent(appList)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun LauncherScreenContent(appList: List<AppInfo>) {
    val isPopupMenuShow = remember {
        mutableStateOf(false)
    }
    LauncherPopupMenu(isPopupMenuShow)

    Column {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            actions = {
                IconButton(onClick = { isPopupMenuShow.value = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
            }
        )
        LazyVerticalGrid(
            cells = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(appList) {
                AppItem(it)
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
    PopupMenuContent(isShow)
}

@Composable
private fun LauncherPopupMenu(isPopupMenuShow: MutableState<Boolean>) {
    PopupMenu(isShow = isPopupMenuShow) {
        PopupMenuContent(isPopupMenuShow)
    }
}

@Composable
private fun PopupMenuContent(isPopupMenuShow: MutableState<Boolean>) {
    val navController = LocalNavHostController.current
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .width(IntrinsicSize.Max)
    ) {
        val context = LocalContext.current
        PopupItem(text = stringResource(id = R.string.set_white_list)) {
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.SET_WHITE_LIST)
        }
        PopupItem(text = stringResource(id = R.string.add_hidden_apps)) {
            isPopupMenuShow.value = false
            navController.navigate(RouteConstant.ADD_HIDDEN_APPS)
        }
        PopupItem(text = stringResource(R.string.reboot_desktop)) {
            isPopupMenuShow.value = false
            Runtime
                .getRuntime()
                .exec("su -c am force-stop ${context.getLauncherPackageName() ?: "com.miui.home"}")
        }
        PopupItem(text = stringResource(R.string.reboot_system)) {
            isPopupMenuShow.value = false
            Runtime
                .getRuntime()
                .exec("su -c reboot")
        }
    }
}

@Composable
private fun AppItem(appInfo: AppInfo) {
    val context = LocalContext.current
    Box(modifier = Modifier
        .clickable {
            var intent: Intent? = null
            if (appInfo.isXposedModule) {
                intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory("de.robv.android.xposed.category.MODULE_SETTINGS")
                    setPackage(appInfo.packageName)
                }
                val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    PackageManager.MATCH_UNINSTALLED_PACKAGES
                } else {
                    PackageManager.GET_UNINSTALLED_PACKAGES
                }
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
    LauncherScreenContent(appList = listOf(appInfo, appInfo, appInfo, appInfo, appInfo, appInfo))
}

@Throws(PackageManager.NameNotFoundException::class)
fun getPackageInfo(context: Context, packageName: String, flag: Int = 0): PackageInfo {
    val packageInfo = context.packageManager.getPackageInfo(
        packageName,
        flag
    )
    return packageInfo
}

fun ApplicationInfo.isXposedModule(): Boolean {
    return metaData?.getBoolean("xposedmodule") == true ||
            metaData?.containsKey("xposedminversion") == true
}


