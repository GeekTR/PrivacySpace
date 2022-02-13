package cn.geektang.privacyspace.util

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

class NavHostControllerWrapper(private val navHostController: NavHostController?) {

    fun navigate(route: String) {
        navHostController?.navigate(route)
    }

    fun popBackStack(){
        navHostController?.popBackStack()
    }
}

val LocalNavHostController = compositionLocalOf { NavHostControllerWrapper(null) }