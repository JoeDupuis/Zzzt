package io.dupuis.zzzt.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.dupuis.zzzt.ui.add.AddClipScreen
import io.dupuis.zzzt.ui.alarms.EditAlarmScreen
import io.dupuis.zzzt.ui.bedtime.BedtimeScreen
import io.dupuis.zzzt.ui.library.LibraryScreen
import io.dupuis.zzzt.ui.manage.ManageAlarmsScreen
import io.dupuis.zzzt.ui.player.PlayerScreen
import io.dupuis.zzzt.ui.settings.SettingsScreen
import io.dupuis.zzzt.ui.trim.TrimScreen

@Composable
fun ZzztNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "bedtime") {
        composable("bedtime") {
            BedtimeScreen(
                onNavigateSelectClip = { navController.navigate("library?select=true") },
                onNavigateSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("manageAlarms") {
            ManageAlarmsScreen(
                onBack = { navController.popBackStack() },
                onNavigateEdit = { id ->
                    if (id == null) navController.navigate("editAlarm")
                    else navController.navigate("editAlarm?id=$id")
                },
            )
        }
        composable(
            route = "editAlarm?id={id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            EditAlarmScreen(alarmId = id, onBack = { navController.popBackStack() })
        }
        composable(
            route = "library?select={select}",
            arguments = listOf(
                navArgument("select") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { backStackEntry ->
            val selectMode = backStackEntry.arguments?.getBoolean("select") ?: false
            LibraryScreen(
                onAddClick = { navController.navigate("add") },
                onClipClick = { id -> navController.navigate("player/$id") },
                selectMode = selectMode,
                onSelectClip = { navController.popBackStack() },
            )
        }
        composable("add") {
            AddClipScreen(
                onBack = { navController.popBackStack() },
                onFetched = { clipId ->
                    navController.navigate("trim/$clipId") {
                        popUpTo("bedtime")
                    }
                },
            )
        }
        composable(
            route = "trim/{clipId}",
            arguments = listOf(navArgument("clipId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getString("clipId").orEmpty()
            TrimScreen(
                clipId = clipId,
                onSaved = { navController.popBackStack("bedtime", inclusive = false) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "player/{clipId}",
            arguments = listOf(navArgument("clipId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getString("clipId").orEmpty()
            PlayerScreen(
                clipId = clipId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
