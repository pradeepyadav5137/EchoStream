package com.echostream.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.echostream.app.ui.navigation.Screen
import com.echostream.app.ui.theme.CardBackground
import com.echostream.app.ui.theme.PrimaryViolet
import com.echostream.app.ui.theme.SurfaceDark
import com.echostream.app.ui.theme.TextMuted
import com.echostream.app.ui.theme.TextPrimary

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Search : BottomNavItem(Screen.Search.route, "Search", Icons.Default.Search)
    object Library : BottomNavItem(Screen.Library.route, "Library", Icons.Default.LibraryMusic)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Library,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = selected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryViolet,
                    selectedTextColor = PrimaryViolet,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = CardBackground
                )
            )
        }
    }
}
