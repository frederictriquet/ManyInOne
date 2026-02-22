package fr.triquet.manyinone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.triquet.manyinone.loyalty.AddCardScreen
import fr.triquet.manyinone.loyalty.CardDetailScreen
import fr.triquet.manyinone.loyalty.LoyaltyCardsScreen
import fr.triquet.manyinone.navigation.Routes
import fr.triquet.manyinone.navigation.Screen
import fr.triquet.manyinone.scanner.ScannerScreen
import fr.triquet.manyinone.ui.theme.ManyInOneTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManyInOneTheme {
                MainApp()
            }
        }
    }
}

private const val ROUTE_MAIN = "main"

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == ROUTE_MAIN

    val tabs = listOf(Screen.Scanner, Screen.LoyaltyCards)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_MAIN,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_MAIN) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> CameraPermissionScreen(
                            onSaveAsCard = { value, format ->
                                navController.navigate(
                                    "${Routes.ADD_CARD}?value=$value&format=$format"
                                )
                            },
                        )
                        1 -> LoyaltyCardsScreen(
                            onAddCard = { navController.navigate(Routes.ADD_CARD) },
                            onCardClick = { id ->
                                navController.navigate("${Routes.CARD_DETAIL}/$id")
                            },
                        )
                    }
                }
            }

            composable(
                route = "${Routes.ADD_CARD}?value={value}&format={format}",
                arguments = listOf(
                    navArgument("value") { type = NavType.StringType; defaultValue = "" },
                    navArgument("format") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { backStackEntry ->
                val value = backStackEntry.arguments?.getString("value")?.takeIf { it.isNotEmpty() }
                val format = backStackEntry.arguments?.getString("format")?.takeIf { it.isNotEmpty() }
                AddCardScreen(
                    initialValue = value,
                    initialFormat = format,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.EDIT_CARD}/{cardId}",
                arguments = listOf(
                    navArgument("cardId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
                AddCardScreen(
                    editCardId = cardId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "${Routes.CARD_DETAIL}/{cardId}",
                arguments = listOf(
                    navArgument("cardId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
                CardDetailScreen(
                    cardId = cardId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigate("${Routes.EDIT_CARD}/$id")
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    onSaveAsCard: (value: String, format: String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (hasCameraPermission) {
        ScannerScreen(onSaveAsCard = onSaveAsCard)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app needs access to your camera to scan barcodes and QR codes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                launcher.launch(Manifest.permission.CAMERA)
            }) {
                Text("Allow camera")
            }
        }
    }
}
