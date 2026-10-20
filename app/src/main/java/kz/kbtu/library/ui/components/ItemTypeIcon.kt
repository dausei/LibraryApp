package kz.kbtu.library.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kz.kbtu.library.R
import kz.kbtu.library.model.ItemType

@DrawableRes
fun ItemType.iconRes(): Int = when (this) {
    ItemType.BOOK -> R.drawable.ic_book
    ItemType.MAGAZINE -> R.drawable.ic_magazine
    ItemType.DVD -> R.drawable.ic_dvd
}

// Colored square with the icon of the item type
@Composable
fun ItemTypeIcon(
    type: ItemType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (type) {
        ItemType.BOOK -> colors.primaryContainer to colors.onPrimaryContainer
        ItemType.MAGAZINE -> colors.tertiaryContainer to colors.onTertiaryContainer
        ItemType.DVD -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(type.iconRes()),
            contentDescription = type.label,
            colorFilter = ColorFilter.tint(content),
            modifier = Modifier.size(size / 2)
        )
    }
}
