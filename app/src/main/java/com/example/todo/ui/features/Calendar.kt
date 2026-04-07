package com.example.todo.ui.features

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo.domain.CalInstance
import com.example.todo.domain.Event
import com.example.todo.domain.Item
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class DragAnchors { Day, Week }

@Composable
fun Calendar(
    focusedDate: LocalDate,
    events: Map<LocalDate, List<CalInstance>>,
    onEditItem: (Item) -> Unit,
    onFocusDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeGutterWidth = 60.dp
    val dayHeight = 120.dp
    val coroutineScope = rememberCoroutineScope()

    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Week,
        )
    }

    val seekableTransitionState =
        remember { SeekableTransitionState(initialState = DragAnchors.Week) }
    val transition = rememberTransition(seekableTransitionState)

    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.offset to anchoredDraggableState.isAnimationRunning }.collect { (offset, isRunning) ->
            val anchors = anchoredDraggableState.anchors

            if (anchors.hasPositionFor(DragAnchors.Day) && anchors.hasPositionFor(DragAnchors.Week)) {
                val dayOffset = anchors.positionOf(DragAnchors.Day)
                val weekOffset = anchors.positionOf(DragAnchors.Week)
                val distance = weekOffset - dayOffset

                if (!offset.isNaN()) {
                    val fraction = (offset / distance).coerceIn(0f, 1f)
                    seekableTransitionState.seekTo(fraction, targetState = DragAnchors.Week)

                    if (!isRunning)
                        seekableTransitionState.snapTo(anchoredDraggableState.settledValue)
                }
            }
        }
    }

    SharedTransitionLayout(
        modifier
            .onSizeChanged { layoutSize ->
                val dragDistance = layoutSize.height * 0.4f
                anchoredDraggableState.updateAnchors(
                    DraggableAnchors {
                        DragAnchors.Day at 0f
                        DragAnchors.Week at dragDistance
                    }
                )
            }
        .anchoredDraggable(
            state = anchoredDraggableState,
            orientation = Orientation.Vertical
        )
    ) {
        transition.AnimatedContent {
            val scheduleState = rememberScheduleState(
                firstDay = focusedDate,
                events = events,
                daysPerPage = if (it == DragAnchors.Week) 7 else 1,
            )

            var skipForFirst by remember { mutableStateOf(true) }
            LaunchedEffect(focusedDate) {
                if (skipForFirst) {
                    skipForFirst = false; return@LaunchedEffect
                }

                scheduleState.focusDate(focusedDate)
            }

            Row {
                Column(Modifier.fillMaxHeight()) {
                    TimeGutter(
                        dayHeight,
                        modifier = Modifier
                            .width(timeGutterWidth)
                            .fillMaxHeight()
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    CalendarGrid(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        state = scheduleState,
                        onEditItem = onEditItem,
                        openDayView = { day ->
                            onFocusDate(day)
                            coroutineScope.launch {
                                seekableTransitionState.animateTo(DragAnchors.Day)
                                anchoredDraggableState.snapTo(DragAnchors.Day)
                            }
                        },
                        animatedContentScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout,
                    )
                }
            }
        }
    }
}

@Stable
class ScheduleGridState(
    initialTopTime: Int, // sec of day that should be matched to top of the view
    initialHourHeight: Float, // height of hour in px
) {
    var topTime by mutableIntStateOf(initialTopTime)
    var hourHeight by mutableFloatStateOf(initialHourHeight)
}


@Composable
fun rememberScheduleGridState(
    initialHourHeight: Float,
): ScheduleGridState {
    return remember {
        ScheduleGridState(
            initialTopTime = LocalTime.of(9, 0).toSecondOfDay(),
            initialHourHeight = initialHourHeight,
        )
    }
}

@Stable
class ScheduleState(
    val daysPerPage: Int,
    private val firstDay: LocalDate,
    val pagerState: PagerState,
    initialEvents: Map<LocalDate, List<CalInstance>>,
) {
    var events by mutableStateOf(initialEvents)

    val focusedWeekFirstDay: LocalDate
        get() = firstDayOfPage(pagerState.currentPage)

    fun firstDayOfPage(page: Int): LocalDate =
        firstDay.plusDays((page - 5000L) * daysPerPage)

    suspend fun focusDate(date: LocalDate) {
        val scroll = date.until(focusedWeekFirstDay, ChronoUnit.DAYS) / daysPerPage
        if (scroll != 0L) {
            pagerState.animateScrollToPage((pagerState.currentPage + scroll).toInt())
        }
    }

    private fun getEvents(date: LocalDate, allDay: Boolean): List<CalInstance> =
        events[date]?.filter { allDay == (it.multiDayItem || it.allDay) } ?: listOf()

    fun getEventsForPage(page: Int, allDay: Boolean): List<List<CalInstance>> {
        val firstDayOfPage = firstDayOfPage(page)
        return List(daysPerPage) { getEvents(firstDayOfPage.plusDays(it.toLong()), allDay) }
    }

    fun getDaysForPage(page: Int): List<LocalDate> {
        val firstDayOfPage = firstDayOfPage(page)
        return List(daysPerPage) { firstDayOfPage.plusDays(it.toLong()) }
    }
}

@Composable
fun rememberScheduleState(
    firstDay: LocalDate,
    daysPerPage: Int,
    events: Map<LocalDate, List<CalInstance>>,
): ScheduleState {
    val pagerState = rememberPagerState(initialPage = 5000, pageCount = { 10000 })

    val state = remember {
        ScheduleState(
            firstDay = firstDay,
            pagerState = pagerState,
            initialEvents = events,
            daysPerPage = daysPerPage,
        )
    }

    state.events = events

    return state
}

@Composable
fun CalendarGrid(
    modifier: Modifier,
    state: ScheduleState,
    onEditItem: (Item) -> Unit,
    openDayView: (LocalDate) -> Unit,
    animatedContentScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope
) {
    HorizontalPager(
        state = state.pagerState,
        beyondViewportPageCount = 1,
        modifier = modifier
    ) { page ->
        Column {
            DayGutter(
                state.getDaysForPage(page),
                openDayView = openDayView,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
            Column(modifier = Modifier.drawBehind(onDraw = { drawGrid(state.daysPerPage) })) {
                AllDayGutter(
                    state.getEventsForPage(page, allDay = true),
                    onEditItem = onEditItem,
                    firstDay = state.firstDayOfPage(page),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.weight(1f)) {
                    state.getEventsForPage(page, allDay = false).zip(state.getDaysForPage(page))
                        .forEach { (events, day) ->
                            CustomDay(
                                0..24,
                                events,
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
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 2.dp),
                                stack = state.daysPerPage > 3,
                                animatedContentScope = animatedContentScope,
                                sharedTransitionScope = sharedTransitionScope,
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun TimeGutter(dayHeight: Dp, modifier: Modifier) {
    Column(modifier) {
        repeat(24) { h ->
            Box(
                Modifier
                    .height(dayHeight)
                    .fillMaxWidth()
            ) {
                Text(
                    "$h:00", modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, -(placeable.height / 2))
                        }
                    }
                )
            }
        }
    }
}

//ScheduleView(
//firstDate = firstDate,
//lastDate = lastDate,
//events = events,
//modifier = modifier,
//) { startHour, endHour ->
//    Box(Modifier.fillMaxSize().background(Color.Red)) {
//        SharedTransitionLayout {
//            AnimatedContent(targetState = focusedDay) {
//                if (it) DayCalendar(
//                    focusedDate = focusedDate,
//                    events = events,
//                    onEditItem = onEditItem,
//                    onFocusDate = onFocusDate,
//                    startHour = startHour,
//                    endHour = endHour,
//                    animatedVisibilityScope = this@AnimatedContent,
//                    sharedTransitionScope = this@SharedTransitionLayout,
//                )
//                else WeekCalendar(
//                    focusedDate = focusedDate,
//                    events = events,
//                    onEditItem = onEditItem,
//                    onFocusDate = onFocusDate,
//                    onFocusDay = { focusedDay = true },
//                    startHour = startHour,
//                    endHour = endHour,
//                    animatedVisibilityScope = this@AnimatedContent,
//                    sharedTransitionScope = this@SharedTransitionLayout,
//                )
//            }
//        }
//    }
//}
