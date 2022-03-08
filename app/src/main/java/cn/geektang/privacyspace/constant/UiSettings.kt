package cn.geektang.privacyspace.constant

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import cn.geektang.privacyspace.util.*

object UiSettings {
    @Composable
    fun Context.obtainCellCount(): Int {
        return when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                sp.iconCellCountLandscape
            }
            else -> {
                sp.iconCellCountPortrait
            }
        }
    }

    @Composable
    fun Context.obtainCellContentPaddingRatio(): Float {
        return when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                sp.iconPaddingLandscape
            }
            else -> {
                sp.iconPaddingPortrait
            }
        }
    }

    @Composable
    fun WatchingAndSaveConfig(
        cellCount: Int,
        cellContentPaddingRatio: Float,
        context: Context
    ) {
        val orientation = LocalConfiguration.current.orientation
        LaunchedEffect(key1 = cellCount, key2 = cellContentPaddingRatio, block = {
            when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    context.sp.iconCellCountLandscape = cellCount
                    context.sp.iconPaddingLandscape = cellContentPaddingRatio
                }
                else -> {
                    context.sp.iconCellCountPortrait = cellCount
                    context.sp.iconPaddingPortrait = cellContentPaddingRatio
                }
            }
        })
    }

    fun getCellCountChoices(orientation: Int): Array<Int> {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            arrayOf(6, 8)
        } else {
            arrayOf(4, 5)
        }
    }
}