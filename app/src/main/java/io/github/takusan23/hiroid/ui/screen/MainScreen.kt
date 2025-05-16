package io.github.takusan23.hiroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.hiroid.tool.PermissionTool
import io.github.takusan23.hiroid.tool.VoskModelTool

/** ルーティング先 */
enum class Route(val path: String) {
    Home("home"),
    Setup("setup"),
    Setting("setting")
}

/** この中でルーティングする */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // 用意できていない場合は、初期設定画面へ飛ばす
    LaunchedEffect(key1 = Unit) {
        if (!PermissionTool.isAllGranted(context) || VoskModelTool.getVoskModelList(context).isEmpty()) {
            navController.navigate(route = Route.Setup.path) {
                popUpTo(route = Route.Home.path) {
                    inclusive = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(Route.Home.path) {
            HomeScreen()
        }
        composable(Route.Setup.path) {
            SetupScreen(
                onComplete = { navController.navigate(route = Route.Home.path) }
            )
        }
        composable(Route.Setting.path) {
            SettingScreen()
        }
    }
}