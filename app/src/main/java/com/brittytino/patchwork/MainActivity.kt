package com.brittytino.patchwork

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import com.brittytino.patchwork.domain.DIYTabs
import com.brittytino.patchwork.domain.registry.initPermissionRegistry
import com.brittytino.patchwork.ui.components.ReusableTopAppBar
import com.brittytino.patchwork.ui.components.DIYFloatingToolbar
import com.brittytino.patchwork.ui.composables.SetupFeatures
import com.brittytino.patchwork.ui.composables.DIYScreen
import com.brittytino.patchwork.ui.theme.PatchworkTheme
import com.brittytino.patchwork.utils.HapticUtil
import com.brittytino.patchwork.viewmodels.MainViewModel
import com.brittytino.patchwork.viewmodels.LocationReachedViewModel
import com.brittytino.patchwork.ui.components.sheets.UpdateBottomSheet
import com.brittytino.patchwork.ui.components.sheets.InstructionsBottomSheet
import com.brittytino.patchwork.ui.composables.configs.FreezeSettingsUI
import com.brittytino.patchwork.ui.composables.FreezeGridUI
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : FragmentActivity() {
    val viewModel: MainViewModel by viewModels()
    val locationViewModel: LocationReachedViewModel by viewModels()
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and configure the splash screen
        val splashScreen = installSplashScreen()
        
        // Force splash screen to dismiss after 2 seconds no matter what
        // to prevent getting stuck if Compose has an issue on this device/OS
        window.decorView.postDelayed({ isAppReady = true }, 2000)
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        Log.d("MainActivity", "onCreate with action: ${intent?.action}")
        handleLocationIntent(intent)

        // Initialize HapticUtil with saved preferences
        HapticUtil.initialize(this)
        // initialize permission registry
        initPermissionRegistry()
        
        // viewModel.check is also called in LaunchedEffect inside setContent.
        viewModel.check(this)
        
        setContent {
            // Confirm composition started and mark app as ready
            LaunchedEffect(Unit) {
                isAppReady = true
                Log.d("MainActivity", "Composition started")
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            PatchworkTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val context = LocalContext.current
                val view = LocalView.current
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    stringResource(R.string.label_unknown)
                }

                var searchRequested by remember { mutableStateOf(false) }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                var showUpdateSheet by remember { mutableStateOf(false) }
                var showInstructionsSheet by remember { mutableStateOf(false) }
                val isUpdateAvailable by viewModel.isUpdateAvailable
                val updateInfo by viewModel.updateInfo

                LaunchedEffect(Unit) {
                    viewModel.check(context)
                    // Request notification permission if not granted (Android 13+)
                    if (!viewModel.isPostNotificationsEnabled.value) {
                        viewModel.requestNotificationPermission(this@MainActivity)
                    }
                    viewModel.checkForUpdates(context)
                }

                val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
                
                // Dynamic tabs configuration
                val tabs = remember { DIYTabs.entries }
                
                val defaultTab by viewModel.defaultTab
                val initialPage = remember(tabs) {
                    val index = tabs.indexOf(defaultTab)
                    if (index != -1) index else 0
                }
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
                val scope = rememberCoroutineScope()

                // Gracefully handle tab removal (e.g. disabling Developer Mode)
                LaunchedEffect(tabs) {
                    if (pagerState.currentPage >= tabs.size) {
                        pagerState.scrollToPage(0)
                    }
                }
                val exitAlwaysScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

                if (showUpdateSheet) {
                    UpdateBottomSheet(
                        updateInfo = updateInfo,
                        isChecking = viewModel.isCheckingUpdate.value,
                        onDismissRequest = { showUpdateSheet = false }
                    )
                }

                if (showInstructionsSheet) {
                    InstructionsBottomSheet(
                        onDismissRequest = { showInstructionsSheet = false }
                    )
                }
                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .nestedScroll(exitAlwaysScrollBehavior),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        val currentTab = remember(tabs, pagerState.currentPage) {
                            tabs.getOrNull(pagerState.currentPage) ?: tabs.firstOrNull() ?: DIYTabs.ESSENTIALS
                        }
                        ReusableTopAppBar(
                            title = currentTab.title,
                            subtitle = currentTab.subtitle,
                            hasBack = false,
                            hasSearch = true,
                            hasSettings = true,
                            hasHelp = true,
                            onSearchClick = { searchRequested = true },
                            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                            onUpdateClick = { showUpdateSheet = true },
                            onHelpClick = { showInstructionsSheet = true },
                            hasUpdateAvailable = isUpdateAvailable,
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        DIYFloatingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset - 12.dp)
                                .zIndex(1f),
                            currentPage = pagerState.currentPage,
                            tabs = tabs,
                            onTabSelected = { index ->
                                HapticUtil.performUIHaptic(view)
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            scrollBehavior = exitAlwaysScrollBehavior
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (tabs[page]) {
                                DIYTabs.ESSENTIALS -> {
                                    SetupFeatures(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding),
                                        searchRequested = searchRequested,
                                        onSearchHandled = { searchRequested = false },
                                        onHelpClick = { showInstructionsSheet = true }
                                    )
                                }
                                DIYTabs.FREEZE -> {
                                    FreezeGridUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding),
                                        contentPadding = innerPadding
                                    )
                                }
                                DIYTabs.DIY -> {
                                    DIYScreen(
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent with action: ${intent.action}")
        handleLocationIntent(intent)
    }

    private fun handleLocationIntent(intent: Intent?) {
        intent?.let {
            if (locationViewModel.handleIntent(it)) {
                val settingsIntent = Intent(this, FeatureSettingsActivity::class.java).apply {
                    putExtra("feature", "Location reached")
                }
                startActivity(settingsIntent)
            }
        }
    }
}
