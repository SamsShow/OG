package com.og

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChartOutline
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.og.ui.OgViewModel
import com.og.ui.screens.BodyScreen
import com.og.ui.screens.CalendarScreen
import com.og.ui.screens.DashboardScreen
import com.og.ui.screens.FuelScreen
import com.og.ui.screens.OnboardingScreen
import com.og.ui.screens.StatsScreen
import com.og.ui.screens.TrainScreen
import com.og.ui.theme.Motion
import com.og.ui.theme.Og
import com.og.ui.theme.OgTheme
import kotlin.math.roundToInt

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.GridView),
    PLAN("Plan", Icons.Outlined.CalendarMonth),
    TRAIN("Train", Icons.Filled.Bolt),
    FUEL("Fuel", Icons.Filled.PieChartOutline),
    BODY("Body", Icons.Outlined.AccessibilityNew),
    STATS("Stats", Icons.Filled.Insights),
}

/** Backdrop blur is a RenderEffect, which only exists from Android 12. */
private val CAN_BLUR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { OgTheme { OgRoot() } }
    }
}

@Composable
private fun OgRoot() {
    val vm: OgViewModel = viewModel(factory = OgViewModel.Factory)
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(Og.Canvas))
        return
    }

    if (!state.onboarded) {
        OnboardingScreen(onDone = vm::completeOnboarding)
        return
    }

    // The screen is recorded into this layer so the floating bar can sample and blur what
    // sits behind it. The bar itself is outside the recorded subtree — were it inside, it
    // would blur its own reflection every frame.
    val backdrop = rememberGraphicsLayer()

    Box(Modifier.fillMaxSize().background(Og.Canvas)) {
        // The recording box stays anchored to the root so the bar's positionInRoot lines up
        // with the layer's coordinate space; the status-bar inset is applied inside it.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                },
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                AnimatedContent(
                    targetState = tab,
                    // Slide toward the tab you picked, so the bar's order is the mental map.
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (
                            slideInHorizontally(Motion.spring()) { (it * 0.12f * dir).toInt() } +
                                fadeIn(tween(Motion.FADE_IN))
                            ) togetherWith (
                            slideOutHorizontally(Motion.spring()) { (-it * 0.08f * dir).toInt() } +
                                fadeOut(tween(Motion.FADE_OUT))
                            )
                    },
                    label = "tab",
                ) { shown ->
                    when (shown) {
                        Tab.HOME -> DashboardScreen(
                            state = state,
                            onOpenTrain = { tab = Tab.TRAIN },
                            onOpenFuel = { tab = Tab.FUEL },
                            onOpenStats = { tab = Tab.STATS },
                        )
                        Tab.PLAN -> CalendarScreen(state, vm, onOpenTrain = { tab = Tab.TRAIN })
                        Tab.TRAIN -> TrainScreen(state, vm)
                        Tab.FUEL -> FuelScreen(state, vm)
                        Tab.BODY -> BodyScreen(state, vm)
                        Tab.STATS -> StatsScreen(state)
                    }
                }
            }
        }

        GlassNavBar(
            current = tab,
            onSelect = { tab = it },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun GlassNavBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    backdrop: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val blurred = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(CircleShape)
            .drawBehind {
                if (CAN_BLUR) {
                    // Record a margin of backdrop around the bar; without it the blur pulls
                    // in transparent pixels from beyond the edges and they wash out.
                    val pad = 40f
                    blurred.renderEffect = BlurEffect(34f, 34f, TileMode.Clamp)
                    blurred.record(
                        size = IntSize(
                            (size.width + pad * 2).roundToInt(),
                            (size.height + pad * 2).roundToInt(),
                        ),
                    ) {
                        translate(pad - origin.x, pad - origin.y) { drawLayer(backdrop) }
                    }
                    translate(-pad, -pad) { drawLayer(blurred) }
                }
                // Frosting over the blur. On pre-12 devices this alone carries the effect.
                drawRect(Og.Surface.copy(alpha = if (CAN_BLUR) 0.62f else 0.93f))
            }
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t -> NavItem(t, t == current) { onSelect(t) } }
    }
}

/** The selected tab becomes a lime pill with its label; the rest stay icon-only. */
@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) Og.Lime else Color.Transparent,
        label = "navPill",
    )
    Row(
        Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 14.dp else 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = if (selected) Og.Forest else Og.InkSecondary,
            modifier = Modifier.size(23.dp),
        )
        // The pill grows into its label rather than the text popping in.
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(Motion.spring()) + fadeIn(tween(Motion.FADE_IN)),
            exit = shrinkHorizontally(Motion.spring()) + fadeOut(tween(Motion.FADE_OUT)),
        ) {
            Row {
                Spacer(Modifier.width(7.dp))
                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = Og.Forest)
            }
        }
    }
}
