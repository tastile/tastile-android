package app.tastile.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// m2-allow: m3-component
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.repository.CalendarProjectionBlockResponse
import app.tastile.android.data.repository.CalendarProjectionResponse
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppSecondaryButton
import app.tastile.android.ui.designsystem.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class MonthCalendarCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val titles: List<String>,
    val overflowCount: Int
)

@Composable
fun MonthCalendarScreen(
    projection: CalendarProjectionResponse?,
    modifier: Modifier = Modifier,
    fallbackTimeline: List<CoreTimelineItem> = emptyList(),
) {
    val effectiveProjection = projection ?: buildFallbackMonthProjection(fallbackTimeline)

    val cells = buildMonthCalendarCells(effectiveProjection)
    val rows = buildMonthCalendarRows(cells)
    val today = LocalDate.now(ZoneId.systemDefault())
    val monthTitle = monthTitleFromProjection(effectiveProjection)
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppScreenTitle(monthTitle)
            AppSecondaryButton(
                text = "Today",
                onClick = {}
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { weekday ->
                AppBodyText(
                    text = weekday,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )
            }
        }

        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                week.forEach { cell ->
                    MonthDayCell(
                        cell = cell,
                        isToday = cell.date == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

internal fun buildMonthCalendarCells(projection: CalendarProjectionResponse): List<MonthCalendarCell> {
    val gridStartDate = parseIsoDate(projection.gridStart) ?: return emptyList()
    val rangeStartDate = parseIsoDate(projection.rangeStart)
    val currentMonth = rangeStartDate?.month
    val currentYear = rangeStartDate?.year
    val blocksByDate = (projection.allDaySpans + projection.blocks)
        .sortedWith(compareBy<CalendarProjectionBlockResponse>({ !it.allDay }, { parseInstant(it.startAt) }))
        .groupBy { block -> parseIsoDate(block.startAt) }

    return (0 until 42).map { offset ->
        val date = gridStartDate.plusDays(offset.toLong())
        val titles = blocksByDate[date].orEmpty().map { it.title }
        MonthCalendarCell(
            date = date,
            inCurrentMonth = currentMonth == null || currentYear == null || (date.month == currentMonth && date.year == currentYear),
            titles = titles.take(3),
            overflowCount = (titles.size - 3).coerceAtLeast(0)
        )
    }
}

@Composable
private fun MonthDayCell(
    cell: MonthCalendarCell,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = AppTheme.colors.outline.copy(alpha = 0.22f),
                shape = RoundedCornerShape(0.dp)
            ),
        color = if (cell.inCurrentMonth) AppTheme.colors.surface else AppTheme.colors.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val dayTextColor = when {
                    isToday -> AppTheme.colors.onPrimary
                    cell.inCurrentMonth -> AppTheme.colors.onSurface
                    else -> AppTheme.colors.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isToday) AppTheme.colors.primary else AppTheme.colors.surface.copy(alpha = 0f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppBodyText(cell.date.dayOfMonth.toString())
                }
            }
            cell.titles.forEach { title ->
                AppBodyText(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            if (cell.overflowCount > 0) {
                AppBodyText(
                    text = "+${cell.overflowCount} more"
                )
            }
        }
    }
}

internal fun buildMonthCalendarRows(cells: List<MonthCalendarCell>): List<List<MonthCalendarCell>> {
    if (cells.size < 42) return emptyList()
    return (0 until 6).map { row -> cells.subList(row * 7, row * 7 + 7) }
}

private fun monthTitleFromProjection(projection: CalendarProjectionResponse): String {
    val date = parseIsoDate(projection.rangeStart) ?: return "Month"
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    return date.format(formatter)
}

private fun parseIsoDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate() }.getOrNull()
}

private fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun buildFallbackMonthProjection(timeline: List<CoreTimelineItem>): CalendarProjectionResponse {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val monthStart = today.withDayOfMonth(1)
    val nextMonthStart = monthStart.plusMonths(1)
    val gridStart = monthStart.minusDays((monthStart.dayOfWeek.value - 1).toLong())
    val gridEnd = gridStart.plusDays(42)
    val blocks = timeline.map {
        CalendarProjectionBlockResponse(
            tileId = it.tileId,
            title = it.title,
            startAt = it.startAt,
            endAt = it.endAt ?: it.startAt
        )
    }
    return CalendarProjectionResponse(
        view = "month",
        rangeStart = monthStart.atStartOfDay(zone).toInstant().toString(),
        rangeEnd = nextMonthStart.atStartOfDay(zone).toInstant().toString(),
        gridStart = gridStart.atStartOfDay(zone).toInstant().toString(),
        gridEnd = gridEnd.atStartOfDay(zone).toInstant().toString(),
        blocks = blocks
    )
}
