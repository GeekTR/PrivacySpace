package cn.geektang.privacyspace.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.geektang.privacyspace.RouteConstant
import cn.geektang.privacyspace.ui.screen.launcher.LauncherScreen
import cn.geektang.privacyspace.ui.screen.managehiddenapps.AddHiddenAppsScreen
import cn.geektang.privacyspace.ui.screen.setwhitelist.SetWhitelistScreen
import cn.geektang.privacyspace.ui.theme.PrivacySpaceTheme
import cn.geektang.privacyspace.util.LocalNavHostController
import cn.geektang.privacyspace.util.NavHostControllerWrapper
import com.google.accompanist.insets.ProvideWindowInsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrivacySpaceTheme {
                // A surface container using the 'background' color from the theme
                ProvideWindowInsets {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun Content() {
    val navHostController = rememberNavController()

    val navHostControllerWrapper = remember {
        NavHostControllerWrapper(navHostController)
    }
    CompositionLocalProvider(LocalNavHostController provides navHostControllerWrapper) {
        NavHost(
            navController = navHostController,
            startDestination = RouteConstant.LAUNCHER
        ) {
            composable(RouteConstant.LAUNCHER) {
                LauncherScreen()
            }
            composable(RouteConstant.ADD_HIDDEN_APPS) {
                AddHiddenAppsScreen()
            }
            composable(RouteConstant.SET_WHITE_LIST) {
                SetWhitelistScreen()
            }
        }
    }
}