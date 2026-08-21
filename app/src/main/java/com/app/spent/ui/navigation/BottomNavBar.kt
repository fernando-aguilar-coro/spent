package com.app.spent.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
@Composable
fun BottomNavBar(navController: NavController) {
  val items = listOf(
  Screen.Dashboard,
  Screen.Analytics,
  Screen.Settings
  )

  val navBackStackEntry = navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry.value?.destination?.route

  NavigationBar(
  containerColor = MaterialTheme.colorScheme.surface,
  tonalElevation = 4.dp
  ) {
    items.forEach { screen ->
      val isSelected = currentRoute == screen.route || (screen == Screen.Dashboard && currentRoute == "dashboard_with_tutorial")
      val title = stringResource(screen.titleResId)
      NavigationBarItem(
      icon = {
        Icon(
        imageVector = screen.icon,
        contentDescription = title,
        modifier = Modifier.size(22.dp)
        )
      },
      label = {
        Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
      },
      selected = isSelected,
      colors = NavigationBarItemDefaults.colors(
      selectedIconColor = MaterialTheme.colorScheme.primary,
      selectedTextColor = MaterialTheme.colorScheme.primary,
      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      onClick = {
        if (currentRoute != screen.route) {
          navController.navigate(screen.route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
          }
        }
      }
      )
    }
  }
}
