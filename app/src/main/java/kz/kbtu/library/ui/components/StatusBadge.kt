package kz.kbtu.library.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.LoanStatus

enum class BadgeTone { Available, Busy, Warning, Danger }

// Small pill with a colored dot
@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val container = when (tone) {
        BadgeTone.Available -> Color.Transparent
        BadgeTone.Busy -> colors.secondaryContainer
        BadgeTone.Warning -> colors.tertiaryContainer
        BadgeTone.Danger -> colors.errorContainer
    }
    val content = when (tone) {
        BadgeTone.Available -> colors.onSurface
        BadgeTone.Busy -> colors.onSecondaryContainer
        BadgeTone.Warning -> colors.onTertiaryContainer
        BadgeTone.Danger -> colors.onErrorContainer
    }
    val dot = when (tone) {
        BadgeTone.Available -> colors.primary
        BadgeTone.Busy -> colors.secondary
        BadgeTone.Warning -> colors.tertiary
        BadgeTone.Danger -> colors.error
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (tone == BadgeTone.Available) BorderStroke(1.dp, colors.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(dot, CircleShape))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Available, Borrowed by someone, Overdue or Kept for someone
@Composable
fun AvailabilityBadge(availability: Availability, modifier: Modifier = Modifier) {
    when (availability) {
        Availability.Available -> StatusBadge("Available", BadgeTone.Available, modifier)
        is Availability.OnLoan -> StatusBadge(
            text = if (availability.status == LoanStatus.OVERDUE) "Overdue · ${availability.borrower?.name}"
            else "Borrowed by ${availability.borrower?.name}",
            tone = if (availability.status == LoanStatus.OVERDUE) BadgeTone.Danger else BadgeTone.Busy,
            modifier = modifier
        )
        is Availability.Held -> StatusBadge("Kept for ${availability.member?.name}", BadgeTone.Warning, modifier)
    }
}
