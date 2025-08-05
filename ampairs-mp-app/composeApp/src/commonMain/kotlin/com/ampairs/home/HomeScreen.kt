package com.ampairs.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import org.koin.compose.koinInject

@Composable
fun HomeScreen(onNavItemClick: (NavItem) -> Unit) {
    val viewModel: HomeScreenViewModel = koinInject<HomeScreenViewModel>()

    val navItems = viewModel.navItems
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(navItems) { navItem ->
            ElevatedCard(
                modifier = Modifier.padding(8.dp).height(128.dp),
                onClick = { onNavItemClick(navItem) }
            ) {
                val painter =
                    rememberImagePainter(navItem.icon)
                Image(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    alignment = Alignment.Center,
                    painter = painter,
                    contentDescription = "Translated description of what the image contains"
                )
                Text(
                    text = navItem.title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(2.dp).fillMaxWidth().heightIn(min = 40.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                )
            }

        }
    }
}