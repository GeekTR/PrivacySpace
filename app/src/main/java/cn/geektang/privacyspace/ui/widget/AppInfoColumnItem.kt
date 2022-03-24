package cn.geektang.privacyspace.ui.widget

import android.content.pm.ApplicationInfo
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.bean.AppInfo
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun AppInfoColumnItem(appInfo: AppInfo, isChecked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(48.dp),
            painter = rememberImagePainter(data = appInfo.appIcon),
            contentDescription = appInfo.appName
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 15.dp)
        ) {
            Text(text = appInfo.appName, style = MaterialTheme.typography.subtitle1)
            Text(text = appInfo.packageName, style = MaterialTheme.typography.body2)
            val chipTexts = mutableListOf<String>()
            if (appInfo.isSystemApp) {
                chipTexts.add("SystemApp")
            }
            if (appInfo.isXposedModule) {
                chipTexts.add("XposedModule")
            }
            if (!appInfo.sharedUserId.isNullOrEmpty()) {
                chipTexts.add(appInfo.sharedUserId)
            }
            if (chipTexts.isNotEmpty()) {
                FlowRow {
                    for ((index, chipText) in chipTexts.withIndex()) {
                        if (index != 0) {
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                        }
                        Chip(modifier = Modifier.padding(top = 5.dp), text = chipText)
                    }
                }
            }
        }
        Checkbox(checked = isChecked, onCheckedChange = {
            onClick()
        })
    }
}

@Preview(showBackground = true)
@Composable
fun AppInfoColumnItemPreview() {
    val context = LocalContext.current
    val appInfo = AppInfo(
        appIcon = ColorDrawable(),
        packageName = BuildConfig.APPLICATION_ID,
        appName = context.getString(R.string.app_name),
        sharedUserId = null,
        isXposedModule = true,
        isSystemApp = true,
        applicationInfo = ApplicationInfo()
    )
    AppInfoColumnItem(appInfo = appInfo, isChecked = false, onClick = {})
}