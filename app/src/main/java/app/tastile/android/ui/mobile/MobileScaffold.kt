package app.tastile.android.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: m3-component
import androidx.compose.material3.DatePicker as MaterialDatePicker
// m2-allow: state-holder
import androidx.compose.material3.DrawerValue
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: m3-component
import androidx.compose.material3.ModalNavigationDrawer
// m2-allow: m3-component
import androidx.compose.material3.Scaffold
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
// m2-allow: state-holder
import androidx.compose.material3.rememberDrawerState
// m2-allow: state-holder
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaDatePickerDialog
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.tabs.ExecuteScreen
import app.tastile.android.ui.mobile.tabs.IntegrationsScreen
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import app.tastile.android.ui.mobile.tabs.TilesScreen
import app.tastile.android.ui.mobile.tabs.TimelineScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val START = "timeline"
private const val MONTH_PICKER_YEAR_RADIUS = 50

private enum class TimelinePickerKind { Day, Month }

/** Convert a [LocalDate] to UTC midnight epoch-millis, matching Material3's
 *  [androidx.compose.material3.DatePickerState.selectedDateMillis] contract.
 *  Material3 interprets the millis value as UTC start-of-day, so converting
 *  to ZonedDateTime at ZoneOffset.UTC and toInstant preserves the date. */
private fun LocalDate.toUtcMillis(): Long =
    this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Inverse of [toUtcMillis]; reconstructs the [LocalDate] at ZoneOffset.UTC,
 *  which Material3 always emits as UTC midnight. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

internal fun formatWeekTitle(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    locale: Locale = Locale.getDefault(),
): AnnotatedString {
    val dateFormatter = DateTimeFormatter.ofPattern("M/d", locale)
    val weekdayFormatter = DateTimeFormatter.ofPattern("E", locale)
    val weekdayStyle = SpanStyle(fontSize = 11.sp)
    return buildAnnotatedString {
        append(weekStart.format(dateFormatter))
        withStyle(weekdayStyle) { append("(${weekStart.format(weekdayFormatter)})") }
        append("–")
        append(weekEnd.format(dateFormatter))
        withStyle(weekdayStyle) { append("(${weekEnd.format(weekdayFormatter)})") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileScaffold(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: START
    val locale = LocalLocale.current.platformLocale
    val selectedDay by dashboardViewModel.selectedDay.collectAsStateWithLifecycle()
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val avatarUrl by dashboardViewModel.avatarUrl.collectAsStateWithLifecycle()
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()
    val scale by dashboardViewModel.scale.collectAsStateWithLifecycle()

    // Range-aware header titles: Day=single date with weekday, Week=Mon–Sun range
    // with weekday on both ends, Month=long month name.
    val weekStart = remember(selectedDay) {
        selectedDay.minusDays((selectedDay.dayOfWeek.value - 1).toLong())
    }
    val weekEnd = remember(weekStart) { weekStart.plusDays(6) }
    val monthStart = remember(selectedDay) { selectedDay.withDayOfMonth(1) }
    val dayFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("M月d日 (EEE)", locale)
    }
    val monthFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }
    // Week keeps the date at titleMedium and shrinks the parenthesized weekday
    // so the range stays clear of the scale pill.
    val title: CharSequence = when (currentRoute) {
        "execute" -> "Tasks"
        "tiles" -> "Projects"
        "integrations" -> "References"
        "settings" -> "Preferences"
        else -> when (scale) {
            TimelineScale.Day -> selectedDay.format(dayFormatter)
            TimelineScale.Week -> remember(weekStart, weekEnd) {
                formatWeekTitle(weekStart, weekEnd)
            }
            TimelineScale.Month -> monthStart.format(monthFormatter)
            TimelineScale.List -> "All events"
        }
    }

    // Day and Week select one date; Month selects from a horizontally scrolling
    // sequence of year labels and month chips.
    var pickerKind by remember { mutableStateOf<TimelinePickerKind?>(null) }
    val openPicker: (() -> Unit)? = if (currentRoute == "timeline") {
        when (scale) {
            TimelineScale.Day, TimelineScale.Week -> ({ pickerKind = TimelinePickerKind.Day })
            TimelineScale.Month -> ({ pickerKind = TimelinePickerKind.Month })
            TimelineScale.List -> null
        }
    } else {
        null
    }

    // Phase 1: ModalNavigationDrawer replaces the bottom-anchored SidePanelSheet.
    // Drawer state is fully local to this composable (rememberDrawerState) — the
    // hamburger in the top-bar calls drawerState.open() via the onMenu callback.
    // Phase 1 also gates the drawer off full-screen routes (settings today;
    // Phase 3 promotes settings to its own Scaffold so it owns its own top-bar).
    val showDrawerForRoute = currentRoute != "settings"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                // Phase 3: Settings is a full-screen drill-down with its own
                // CenterAlignedTopAppBar, so the shell top bar is suppressed
                // on that route to avoid two stacked top bars.
                if (currentRoute != "settings") {
                    MobileTopBar(
                        title = title,
                        scale = scale,
                        onScaleChange = { dashboardViewModel.setScale(it) },
                        onMenu = { coroutineScope.launch { drawerState.open() } },
                        onNotifications = { overlayViewModel.show(Overlay.Notifications) },
                        avatarUrl = avatarUrl,
                        avatarFallback = profile?.displayName?.firstOrNull()?.toString()
                            ?: email.firstOrNull()?.toString()
                            ?: "U",
                        showScale = currentRoute == "timeline",
                        onTitleClick = openPicker,
                    )
                }
            },
            // Edge-to-edge: main content fills the whole screen so the transparent
            // top-bar gradient can show the timeline peeking through.
            contentWindowInsets = WindowInsets(0),
        ) { innerPadding ->
            // Non-timeline tabs pad themselves with the scaffold's innerPadding so their
            // first row sits below the top bar. TimelineScreen already pads internally
            // (status bar + MobileTokens.topBarHeight); applying innerPadding here too
            // would push it off-screen, so we leave the timeline route alone.
            val topPad = innerPadding.calculateTopPadding()
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = START,
                ) {
                    composable("timeline") { TimelineScreen(viewModel = dashboardViewModel, overlay = overlayViewModel) }
                    composable("execute") {
                        Box(modifier = Modifier.padding(top = topPad)) {
                            ExecuteScreen(viewModel = dashboardViewModel)
                        }
                    }
                    composable("tiles") {
                        Box(modifier = Modifier.padding(top = topPad)) {
                            TilesScreen(viewModel = dashboardViewModel)
                        }
                    }
                    composable("integrations") {
                        Box(modifier = Modifier.padding(top = topPad)) {
                            IntegrationsScreen(viewModel = dashboardViewModel)
                        }
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = dashboardViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                OverlayLayer(
                    overlay = overlayViewModel,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                )

                // Task 21: prioritize overlay dismissal over nav pop on system back press.
                val overlayCurrent by overlayViewModel.current.collectAsStateWithLifecycle()
                // Phase 1: drawer has its own back handling — if open, swallow the
                // back press to close it before letting the overlay handler fire.
                BackHandler(enabled = drawerState.isOpen) {
                    coroutineScope.launch { drawerState.close() }
                }
                BackHandler(enabled = overlayCurrent !is Overlay.Hidden) {
                    dashboardViewModel.clearSelectedTile()
                    overlayViewModel.dismiss()
                }

                when (pickerKind) {
                    TimelinePickerKind.Day -> {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedDay.toUtcMillis(),
                        )
                        NiaDatePickerDialog(
                            onDismissRequest = { pickerKind = null },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            dashboardViewModel.setSelectedDay(millis.toLocalDate())
                                        }
                                        pickerKind = null
                                    },
                                    enabled = datePickerState.selectedDateMillis != null,
                                ) { Text(stringResource(R.string.date_picker_confirm)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { pickerKind = null }) {
                                    Text(stringResource(R.string.date_picker_cancel))
                                }
                            },
                        ) {
                            MaterialDatePicker(state = datePickerState)
                        }
                    }
                    TimelinePickerKind.Month -> {
                        MonthPickerDialog(
                            initialMonth = YearMonth.from(selectedDay),
                            onDismissRequest = { pickerKind = null },
                            onMonthSelected = { month ->
                                dashboardViewModel.setSelectedDay(month.atDay(1))
                                pickerKind = null
                            },
                        )
                    }
                    null -> Unit
                }
            }
        }
    }

    if (showDrawerForRoute) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SidePanelDrawerContent(
                    navController = navController,
                    drawerState = drawerState,
                )
            },
            content = scaffoldContent,
        )
    } else {
        scaffoldContent()
    }
}

@Composable
internal fun MonthPickerDialog(
    initialMonth: YearMonth,
    onDismissRequest: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
) {
    var selectedMonth by remember(initialMonth) { mutableStateOf(initialMonth) }
    val firstYear = initialMonth.year - MONTH_PICKER_YEAR_RADIUS
    val yearCount = MONTH_PICKER_YEAR_RADIUS * 2 + 1
    val itemsPerYear = 13
    val selectedYearStart = (initialMonth.year - firstYear) * itemsPerYear
    val selectedMonthIndex = selectedYearStart + initialMonth.monthValue
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = maxOf(selectedYearStart, selectedMonthIndex - 2),
    )
    val locale = LocalLocale.current.platformLocale
    val monthLabelFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMM", locale)
    }
    val monthDescriptionFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(selectedMonth.year.toString()) },
        text = {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(
                    count = yearCount * itemsPerYear,
                    key = { index -> index },
                ) { index ->
                    val year = firstYear + index / itemsPerYear
                    val monthValue = index % itemsPerYear
                    if (monthValue == 0) {
                        Text(year.toString())
                    } else {
                        val month = YearMonth.of(year, monthValue)
                        FilterChip(
                            selected = month == selectedMonth,
                            onClick = { selectedMonth = month },
                            label = { Text(month.format(monthLabelFormatter)) },
                            modifier = Modifier.semantics {
                                contentDescription = month.format(monthDescriptionFormatter)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMonthSelected(selectedMonth) }) {
                Text(stringResource(R.string.date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.date_picker_cancel))
            }
        },
    )
}
