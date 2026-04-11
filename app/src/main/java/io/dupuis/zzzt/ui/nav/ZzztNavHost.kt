package io.dupuis.zzzt.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.dupuis.zzzt.ui.add.AddClipScreen
import io.dupuis.zzzt.ui.library.LibraryScreen
import io.dupuis.zzzt.ui.player.PlayerScreen
import io.dupuis.zzzt.ui.trim.TrimScreen

@Composable
fun ZzztNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                onAddClick = { navController.navigate("add") },
                onClipClick = { id -> navController.navigate("player/$id") },
            )
        }
        composable("add") {
            AddClipScreen(
                onBack = { navController.popBackStack() },
                onFetched = { clipId ->
                    navController.navigate("trim/$clipId") {
                        popUpTo("library")
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
                onSaved = { navController.popBackStack("library", inclusive = false) },
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
