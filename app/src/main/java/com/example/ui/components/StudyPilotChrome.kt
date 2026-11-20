package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.StreakState
import com.example.data.StudyTab
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TrapAmberBg
import com.example.ui.theme.TrapAmberBorder
import com.example.ui.theme.TrapAmberBright

@Composable
fun StudyPilotTopHeader(
    streakState: StreakState = StreakState(),
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ObsidianBg,
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = CyanContainer,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("top_header_title"),
                    )
                }

                val streakDesc = stringResource(R.string.streak_badge_desc, streakState.currentStreak)
                Surface(
                    modifier = Modifier
                        .semantics { contentDescription = streakDesc }
                        .testTag("streak_badge"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (streakState.isActiveToday) TrapAmberBg else SlateSurfaceElevated,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (streakState.isActiveToday) TrapAmberBorder else SlateBorder,
                    ),
                ) {
                    Text(
                        text = "🔥 ${streakState.currentStreak} Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (streakState.isActiveToday) TrapAmberBright else IceWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = SlateBorder.copy(alpha = 0.65f),
            )
        }
    }
}

private data class TabItemMeta(
    val tab: StudyTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val studyTabs = listOf(
    TabItemMeta(
        tab = StudyTab.Solve,
        labelRes = R.string.tab_solve,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_solve",
    ),
    TabItemMeta(
        tab = StudyTab.Recall,
        labelRes = R.string.tab_recall,
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style,
        testTag = "nav_tab_recall",
    ),
    TabItemMeta(
        tab = StudyTab.Plan,
        labelRes = R.string.tab_plan,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_tab_plan",
    ),
    TabItemMeta(
        tab = StudyTab.Focus,
        labelRes = R.string.tab_focus,
        selectedIcon = Icons.Filled.CenterFocusStrong,
        unselectedIcon = Icons.Outlined.CenterFocusStrong,
        testTag = "nav_tab_focus",
    ),
)

@Composable
fun StudyPilotBottomBar(
    selectedTab: StudyTab,
    onSelectTab: (StudyTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SlateBorder.copy(alpha = 0.25f),
                            ElectricCyan.copy(alpha = 0.35f),
                            SlateBorder.copy(alpha = 0.25f),
                        )
                    )
                )
        )
        NavigationBar(
            containerColor = MidnightSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars,
        ) {
            studyTabs.forEach { item ->
                val isSelected = selectedTab == item.tab
                val label = stringResource(item.labelRes)
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectTab(item.tab) },
                    modifier = Modifier.testTag(item.testTag),
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = label,
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = SlateSurfaceElevated,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                    ),
                )
            }
        }
    }
}
