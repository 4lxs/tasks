@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.todo.ui.features

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.domain.CalInstance
import com.example.todo.domain.Event
import com.example.todo.domain.Item
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val CalendarStartLine = HorizontalAlignmentLine(::min)

@Composable
fun ScheduleView(
    firstDate: LocalDate,
    lastDate: LocalDate,
    events: Map<LocalDate, List<CalInstance>>,
    modifier: Modifier = Modifier,
    content: @Composable (firstHour: Int, lastHour: Int) -> Unit,
) {
    val (startHour: Int, endHour: Int) = remember(firstDate, lastDate, events) {
        var firstHour = LocalTime.MAX
        var lastHour = LocalTime.MIN
        val ndays = ChronoUnit.DAYS.between(firstDate, lastDate)
        for (d in (0..<ndays)) {
            val day = firstDate.plusDays(d)
            events[day]?.forEach { ci ->
                if (ci.multiDayItem || ci.allDay) return@forEach
                if (ci.firstDay && ci.dayStart < firstHour) {
                    firstHour = ci.dayStart
                }
                if (ci.lastDay && ci.dayEnd > lastHour) {
                    lastHour = ci.dayEnd
                }
            }
        }
        if (firstHour == LocalTime.MAX || lastHour == LocalTime.MIN) 8 to 16
        else firstHour.hour to min(lastHour.hour + 2, 24) // space for bottom sheet
    }

    Layout(
        modifier = modifier,
        contents = listOf(
            {
                repeat(endHour - startHour) {
                    Text(
                        text = (startHour + it).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            },
            {
                content(startHour, endHour)
            }
        )
    ) { (times, calendar), constraints ->
        val times = times.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val timeWidth =
            (times.maxOf { it.width } + 4.dp.roundToPx()).coerceAtMost(constraints.maxWidth)
        val calendarWidth = (constraints.maxWidth - timeWidth)
        if (calendarWidth < 0) {
            return@Layout layout(width = constraints.maxWidth, height = constraints.maxHeight) {}
        }
        val calendar = calendar.first()
            .measure(constraints.copy(maxWidth = calendarWidth, minWidth = calendarWidth))

        val scheduleTop =
            calendar[CalendarStartLine].let { if (it == AlignmentLine.Unspecified) 0 else it }
        val timeHeight = (constraints.maxHeight - scheduleTop) / (endHour - startHour)

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            times.forEachIndexed { ix, timePcbl ->
                timePcbl.placeRelative(0, scheduleTop + ix * timeHeight - timePcbl.height / 2)
            }
            calendar.placeRelative(timeWidth, 0)
        }
    }
}

@Composable
fun DayCalendar(
    focusedDate: LocalDate,
    events: Map<LocalDate, List<CalInstance>>,
    onEditItem: (Item) -> Unit,
    onFocusDate: (LocalDate) -> Unit,
    startHour: Int,
    endHour: Int,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    var startDay by remember { mutableStateOf(focusedDate) }
    val pagerState = rememberPagerState(initialPage = 5000, pageCount = { 10000 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onFocusDate(startDay.plusDays((pagerState.currentPage - 5000).toLong()))
    }
    LaunchedEffect(focusedDate) {
        val pagerDay = startDay.plusDays(pagerState.currentPage - 5000L)
        val scroll = ChronoUnit.DAYS.between(focusedDate, pagerDay)
        if (scroll != 0L) {
            Log.d(null, scroll.toString())
            coroutineScope.launch {
                pagerState.animateScrollToPage((pagerState.currentPage + scroll).toInt())
            }
        }
    }

    HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = modifier) { page ->
        val day = startDay.plusDays(page - 5000L)
        Column {
            val (allDayEvents, timedEvents) = events[day]?.partition {
                it.multiDayItem || it.allDay
            } ?: Pair(listOf(), listOf())
            AllDayGutter(
                listOf(allDayEvents),
                onEditItem = onEditItem,
                firstDay = day,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            )
            CustomDay(
                startHour..endHour,
                timedEvents,
                onCreateEvent = { start, end ->
                    onEditItem(
                        Event(
                            "",
                            LocalDateTime.of(day, start),
                            LocalDateTime.of(day, end)
                        )
                    )
                },
                onEditItem = onEditItem,
                stack = false,
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)

                        layout(
                            width = placeable.width,
                            height = placeable.height,
                            alignmentLines = mapOf(CalendarStartLine to 0)
                        ) {
                            placeable.placeRelative(0, 0)
                        }
                    },
                animatedContentScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope,
            )
        }
    }
}


//@Composable
//fun WeekCalendarGrid(
//    state: WeekScheduleState,
//    events: Map<LocalDate, List<CalInstance>>,
//    onEditItem: (Item) -> Unit,
//    onFocusDay: () -> Unit,
//    modifier: Modifier = Modifier,
//    animatedVisibilityScope: AnimatedContentScope,
//    sharedTransitionScope: SharedTransitionScope,
//) {
//    HorizontalPager(state = state.pagerState, beyondViewportPageCount = 1, modifier = modifier) { page ->
//        val week = state.startWeek.plus(page - 5000)
//        Column {
//            DayGutter(
//                week = week,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//            )
//            val (allDayEvents, timedEvents) = Array(7) { day ->
//                events[week.firstDay.plusDays(day.toLong())]?.partition {
//                    it.multiDayItem || it.allDay
//                } ?: Pair(listOf(), listOf())
//            }.unzip()
//            AllDayGutter(
//                allDayEvents,
//                onEditItem = onEditItem,
//                firstDay = week.firstDay,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(30.dp)
//            )
//            WeekGrid(
//                timedEvents,
//                onCreateEvent = { onEditItem(it) },
//                onEditItem = onEditItem,
//                week.firstDay,
//                startHour..endHour,
//                Modifier
//                    .fillMaxSize()
//                    .layout { measurable, constraints ->
//                        val placeable = measurable.measure(constraints)
//
//                        layout(
//                            width = placeable.width,
//                            height = placeable.height,
//                            alignmentLines = mapOf(CalendarStartLine to 0)
//                        ) {
//                            placeable.placeRelative(0, 0)
//                        }
//                    },
//                animatedVisibilityScope = animatedVisibilityScope,
//                sharedTransitionScope = sharedTransitionScope,
//            )
//        }
//    }
//}

@Composable
fun AllDayGutter(
    instances: List<List<CalInstance>>,
    onEditItem: (Item) -> Unit,
    firstDay: LocalDate,
    modifier: Modifier
) {
    val ndays = instances.size
    val eventHeight = 30

    data class EvPlace(val placeable: Placeable, val x: Int, val depth: Int)

    val instances = instances.flatten().distinctBy { it.item.id }
    val depths = getDayDepths(instances)
    val maxDepth = depths.maxOrNull()?.plus(1) ?: 0

    Layout(
        modifier = modifier,
        content = {
            instances.forEachIndexed { i, instance ->
                if (depths[i] != -1) AllDayEventBox(
                    instance,
                    onClick = { onEditItem(instance.item) })
            }
        }) { measurables, constraints ->
        val width = constraints.maxWidth
        val dayWidth = width / ndays

        val evPlaces = measurables.mapIndexed { i, measurable ->
            val event = instances[i]
            val depth = depths[i]
            val startD = max(ChronoUnit.DAYS.between(firstDay, event.itemStart), 0)
            val endD = min(ChronoUnit.DAYS.between(firstDay, event.itemEnd), ndays - 1L) + 1
            val startX = startD * dayWidth
            val endX = endD * dayWidth
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = (endX - startX).toInt(),
                        minHeight = 0,
                        maxHeight = eventHeight,
                    )
                )

            EvPlace(placeable, startX.toInt(), depth)
        }

        layout(width = constraints.maxWidth, height = maxDepth * eventHeight) {
            evPlaces.forEach {
                it.placeable.place(
                    it.x,
                    it.depth * constraints.maxHeight / 3,
                    0f
                )
            }
        }
    }
}

@Composable
fun AllDayEventBox(
    item: CalInstance,
    onClick: () -> Unit,
) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(Dp.Hairline, Color.LightGray, RoundedCornerShape(5))
            .clip(RoundedCornerShape(5))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
            .clickable(onClick = onClick)
    ) {
        Row {
            if (!item.allDay) {
                Text(
                    fmt.format(item.itemStart.toLocalTime()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 8.sp
                    )
                )
            }
            Text(
                item.item.title,
                style = MaterialTheme.typography.labelSmallEmphasized.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 8.sp
                ),
                modifier = Modifier.padding(2.dp, 0.dp)
            )
            if (!item.allDay) {
                Spacer(Modifier.weight(1f))
                Text(
                    fmt.format(item.itemEnd.toLocalTime()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 8.sp
                    )
                )
            }
        }
    }
}

@Composable
fun DayGutter(
    days: List<LocalDate>,
    openDayView: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayNumberStyle =
        MaterialTheme.typography.bodyMedium.copy(
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    val currentDayNumberStyle = dayNumberStyle.copy(color = MaterialTheme.colorScheme.primary)
    val dayTextStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
    val currentDayTextStyle = dayTextStyle.copy(color = MaterialTheme.colorScheme.primary)

    Row(modifier) {
        days.forEach { day ->
            val isCurrent = day == LocalDate.now()
            val dayText = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            val dayNumberText = day.dayOfMonth.toString()

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = { openDayView(day) })
                        .minimumInteractiveComponentSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayText,
                            style = if (isCurrent) currentDayTextStyle else dayTextStyle
                        )
                        Text(
                            style = if (isCurrent) currentDayNumberStyle else dayNumberStyle,
                            text = dayNumberText,
                        )
                    }
                }
            }
        }
    }
}


val EventPalette = listOf(
    Color(0xFFD0BCFF),
    Color(0xFFE91E63),
    Color(0xFFFFD966),
    Color(0xFFA8EDEA),
)

@Composable
fun CustomDay(
    hours: IntRange,
    events: List<CalInstance>,
    onCreateEvent: (start: LocalTime, end: LocalTime) -> Unit,
    onEditItem: (Item) -> Unit,
    animatedContentScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    stack: Boolean = true,
) {
    data class EvPlace(val placeable: Placeable, val y: Int, val depth: Int)

    val depths = remember(events) { getDepths(events) }

    data class DragForEvent(
        val startYPerc: Float,
        val currYPerc: Float,
        val startY: Float,
        val currY: Float
    )

    data class NewEvent(val startTime: LocalTime, val endTime: LocalTime)

    var drag: DragForEvent? by remember { mutableStateOf(null) }
    val createdEvent = drag?.let {
        val start = (hours.last - hours.first) * min(it.startYPerc, it.currYPerc)
        val end = (hours.last - hours.first) * max(it.startYPerc, it.currYPerc)
        NewEvent(
            startTime = LocalTime.of(hours.first + start.toInt(), 0),
            endTime = LocalTime.of(hours.first + end.toInt(), 0)
        )
    }

    // for the pointerinput with unit key
    val currentCreatedEvent by rememberUpdatedState(createdEvent)
    val currentOnCreateEvent by rememberUpdatedState(onCreateEvent)

    with(sharedTransitionScope) {
        Layout(
            modifier = modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        drag = DragForEvent(it.y / size.height, it.y / size.height, it.y, it.y)
                    },
                    onDrag = { change, offset ->
                        drag?.let {
                            val y = (it.currY + offset.y).coerceIn(0f, size.height.toFloat())
                            drag = it.copy(
                                currYPerc = y / size.height,
                                currY = y
                            )
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        currentCreatedEvent?.let { currentOnCreateEvent(it.startTime, it.endTime) }
                        drag = null
                    },
                    onDragCancel = { drag = null })
            },
            content = {
                events.forEachIndexed { i, event ->
                    if (depths[i] != -1) EventBox(
                        event,
                        depth = depths[i],
                        onClick = { onEditItem(event.item) },
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = event.id),
                            animatedVisibilityScope = animatedContentScope
                        )
                    )
                }
                createdEvent?.let { CreatedEventBox(it.startTime, it.endTime) }
            }) { measurables, constraints ->
            val eventWidth =
                if (stack) constraints.maxWidth else constraints.maxWidth /
                        (depths.maxOrNull()?.plus(1) ?: 1)
            val height = constraints.maxHeight
            val hourHeight = height / (hours.last - hours.first)
            var minNextTop = 0f

            val evPlaces = measurables.take(measurables.size - (createdEvent?.let { 1 } ?: 0))
                .mapIndexed { i, measurable ->
                    val event = events[i]
                    val depth = depths[i]
                    val expectedY =
                        (event.dayStart.hour - hours.first + event.dayStart.minute / 60f) * hourHeight
                    val startY = max(expectedY, minNextTop)
                    val endY =
                        (event.dayEnd.hour - hours.first + event.dayEnd.minute / 60f) * hourHeight
                    val placeable =
                        measurable.measure(
                            constraints.copy(
                                minHeight = 0,
                                maxHeight = (endY - startY).toInt(),
                                maxWidth = eventWidth,
                                minWidth = 0,
                            )
                        )

                    minNextTop = startY + placeable[FirstBaseline] + 10

                    EvPlace(placeable, startY.toInt(), depth)
                }

            val newEvent = drag?.let {
                measurables.lastOrNull()?.measure(
                    constraints.copy(
                        minHeight = abs(it.currY - it.startY).toInt(),
                    )
                )
            }

            layout(width = constraints.maxWidth, height = constraints.maxHeight) {
                evPlaces.forEach {
                    it.placeable.place(
                        if (stack) it.depth * 8 else it.depth * eventWidth, it.y, 0f
                    )
                }
                newEvent?.let { plc ->
                    drag?.let { plc.place(0, min(it.startY, it.currY).toInt(), 1f) }
                }
            }
        }
    }
}

@Composable
fun CreatedEventBox(startTime: LocalTime, endTime: LocalTime) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(Dp.Hairline, Color.LightGray, RoundedCornerShape(5))
            .clip(RoundedCornerShape(5))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            fmt.format(startTime) + "-" + endTime,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        )
    }
}

@Composable
fun EventBox(event: CalInstance, depth: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(Dp.Hairline, Color.LightGray, RoundedCornerShape(5))
            .clip(RoundedCornerShape(5))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
            .drawBehind {
                drawLine(
                    color = EventPalette[depth % EventPalette.size],
                    start = Offset(3f, 0f),
                    end = Offset(3f, size.height),
                    strokeWidth = 6f
                )
            }
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(PaddingValues(start = 2.dp))) {
            Text(
                event.item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            )
            Text(
                fmt.format(event.dayStart),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 8.sp
                )
            )
        }
    }
}

fun getDepths(events: List<CalInstance>): List<Int> {
    val columnEndTimes = mutableListOf<LocalTime>()

    return events.map {
        var depth: Int? = null
        check(!it.allDay) // allDays should be handled before days

        for (i in columnEndTimes.indices) {
            if (!columnEndTimes[i].isAfter(it.dayStart)) {
                columnEndTimes[i] = it.dayEnd
                depth = i
                break
            }
        }
        if (depth == null) {
            depth = columnEndTimes.size
            columnEndTimes.add(it.dayEnd)
        }

        depth
    }
}

fun getDayDepths(events: List<CalInstance>): List<Int> {
    val columnEndTimes = mutableListOf<LocalDate>()

    return events.map {
        var depth: Int? = null
        val (start, end) = it.itemStart.toLocalDate() to it.itemEnd.toLocalDate()

        for (i in columnEndTimes.indices) {
            if (!columnEndTimes[i].isAfter(start)) {
                columnEndTimes[i] = end
                depth = i
                break
            }
        }
        if (depth == null) {
            depth = columnEndTimes.size
            columnEndTimes.add(end)
        }

        depth
    }
}

fun DrawScope.drawGrid(ncols: Int) {
    val dayWidth = size.width / ncols
    for (i in 1..<ncols) {
        val x = i * dayWidth
        drawLine(
            color = Color.DarkGray,
            start = Offset(x + 1, 0f),
            end = Offset(x + 1, size.height),
            strokeWidth = 1f
        )
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedContentLambdaTargetStateParameter")
@Preview
@Composable
fun WeekSchedulePreviewGrid() {
    Scaffold {
        val m = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        data class T(
            val n: String,
            val d: LocalDate,
            val s: String,
            val e: String,
            val ed: LocalDate = d
        )

        val events = listOf(
            T("a", m.plusDays(0), "10:00:00", "15:00:00"),
            T("a", m.plusDays(0), "11:00:00", "15:00:00"),
            T("a", m.plusDays(0), "11:30:00", "16:00:00"),
            T("a", m.plusDays(0), "12:00:00", "15:00:00"),
            T("a", m.plusDays(0), "13:00:00", "15:00:00"),
            T("a", m.plusDays(1), "12:00:00", "15:00:00"),
            T("a", m.plusDays(1), "13:30:00", "16:00:00"),
            T("a", m.plusDays(1), "14:00:00", "15:00:00"),
            T("a", m.plusDays(1), "14:00:00", "16:00:00"),
            T("a", m.plusDays(1), "14:00:00", "16:00:00"),
            T("a", m.plusDays(3), "14:00:00", "16:00:00", ed = m.plusDays(5)),
        ).mapIndexed { ix, it ->
            val start = LocalTime.parse(it.s)
            val end = LocalTime.parse(it.e)
            CalInstance(
                date = it.d,
                item = Event(
                    title = it.n,
                    start = LocalDateTime.now(),
                    end = LocalDateTime.now()
                ),
                allDay = false,
                itemStart = start.atDate(it.d),
                itemEnd = end.atDate(it.ed),
                dayStart = start,
                dayEnd = end,
                multiDayItem = ChronoUnit.DAYS.between(it.d, it.ed) > 1,
                firstDay = true,
                lastDay = true,
                id = "$ix"
            )
        }.groupBy { it.date }
        SharedTransitionLayout {
            AnimatedContent(targetState = true) { _ ->
                ScheduleView(
                    firstDate = m,
                    lastDate = m.plusDays(6),
                    events = events,
                ) { firstHour, lastHour ->
//                    WeekCalendarGrid(
//                        focusedDate = LocalDate.now(),
//                        events = events,
//                        onEditItem = { },
//                        onFocusDate = {},
//                        onFocusDay = {},
//                        startHour = firstHour,
//                        endHour = lastHour,
//                        animatedVisibilityScope = this@AnimatedContent,
//                        sharedTransitionScope = this@SharedTransitionLayout,
//                    )
                }
            }
        }
    }
}
