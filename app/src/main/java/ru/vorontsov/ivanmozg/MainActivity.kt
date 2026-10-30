package ru.vorontsov.ivanmozg

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMaxRefreshRate()
        val store = Store(this)
        setContent {
            var theme by remember { mutableIntStateOf(store.theme) }
            val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    var screen by rememberSaveable { mutableStateOf("home") }
                    BackHandler(screen != "home") { screen = "home" }
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        App(store, screen, { screen = it }, theme) { theme = it; store.theme = it }
                    }
                }
            }
        }
    }

    /** realme 15 5G и другие: просим самую высокую частоту экрана (144 Гц) при том же разрешении — точнее тайминг стимулов. */
    private fun requestMaxRefreshRate() {
        val d = (if (Build.VERSION.SDK_INT >= 30) display else @Suppress("DEPRECATION") windowManager.defaultDisplay) ?: return
        val cur = d.mode
        val best = d.supportedModes.filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.also { it.preferredDisplayModeId = best.modeId }
    }
}

@Composable
fun App(store: Store, screen: String, go: (String) -> Unit, theme: Int, setTheme: (Int) -> Unit) {
    val done = { key: String -> store.add(key, 1.0) }
    when (screen) {
        "home" -> HomeScreen(store, go, theme, setTheme)
        "srs" -> SrsScreen(store)
        "loci" -> LociScreen(store)
        "span" -> SpanScreen(store)
        "nback" -> NBackScreen(store)
        "rt_simple" -> ReactionScreen(store, choice = false)
        "rt_choice" -> ReactionScreen(store, choice = true)
        "stroop" -> StroopScreen(store)
        "switch" -> SwitchScreen(store)
        "move" -> MoveScreen { done("move"); go("home") }
        "sleep" -> SleepScreen { done("sleep"); go("home") }
        "progress" -> ProgressScreen(store)
        "science" -> ScienceScreen()
    }
}
