# Settings Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Android settings destination its own visible App bar and separate it from the main navigation drawer without changing the main-screen shell or settings state behavior.

**Architecture:** Keep `MobileScaffold` as the shell for Timeline, Tasks, Projects, and References. The existing `settings` route continues to suppress the shell App bar and drawer; `SettingsScreen` owns a Material 3 `Scaffold` with a back App bar. The drawer renders four primary destinations followed by a divider, a `Setting` section label, and the `Setting` destination.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Android instrumented Compose UI tests, Gradle.

---

## File Map

- **Modify:** `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
  - Make the existing settings body the content of a route-owned `Scaffold`.
  - Add a `CenterAlignedTopAppBar` with back navigation, the `Setting` title, and a test tag.
  - Apply Scaffold `innerPadding` before the existing scroll and section spacing.
- **Modify:** `app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt`
  - Remove settings from the flat primary route list.
  - Add a divider, a `Setting` section label, and a separately grouped settings row.
- **Modify:** `app/src/main/res/values/strings.xml`
  - Change the settings screen title to `Setting` and add the drawer label resource.
- **Modify:** `app/src/main/res/values-ja/strings.xml`
  - Keep the requested `Setting` label and title identical in the Japanese resource set.
- **Create:** `app/src/androidTest/java/app/tastile/android/ui/navigation/SettingsNavigationTest.kt`
  - Verify the settings App bar/back action and the separate drawer group.

The Android child repository already has unrelated uncommitted QuickCreate, Timeline, screenshot, and XML changes. Do not reset, stash, clean, or modify those files.

### Task 1: Add failing Compose UI tests

**Files:**
- Create: `app/src/androidTest/java/app/tastile/android/ui/navigation/SettingsNavigationTest.kt`

- [ ] **Step 1: Add the focused settings and drawer tests**

Create the test file with the existing repository mocking pattern so the real `SettingsScreen` can be mounted without network or datastore access:

```kotlin
package app.tastile.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.SidePanelDrawerContent
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class SettingsNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreenOwnsAppBarAndBackAction() {
        val viewModel = stubDashboardViewModel()
        var didGoBack = false

        rule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { didGoBack = true },
            )
        }

        rule.onNodeWithTag("settings-app-bar").assertIsDisplayed()
        rule.onNodeWithText("Setting").assertIsDisplayed()
        rule.onNodeWithText("Theme").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { check(didGoBack) }
    }

    @Test
    fun drawerRendersSettingAsSeparateGroup() {
        rule.setContent {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            SidePanelDrawerContent(
                navController = navController,
                drawerState = drawerState,
            )
        }

        rule.onNodeWithTag("side-panel-section-settings").assertIsDisplayed()
        rule.onNodeWithTag("side-panel-row-settings").assertIsDisplayed()
    }

    private fun stubDashboardViewModel(): DashboardViewModel {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.SYSTEM
        every { userSettingsRepository.getLocale() } returns AppLocale.EN
        every { userSettingsRepository.getSecurityLockEnabled() } returns false
        every { userSettingsRepository.getSecurityLockTimeoutMinutes() } returns 5
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { tileRepository.getTimeline(any(), any()) } returns emptyList()
        coEvery { profileRepository.getProfile(any()) } returns null
        return DashboardViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        )
    }
}
```

- [ ] **Step 2: Run the new tests before implementation**

Run from `C:\Users\rebui\Desktop\tastile\tastile-android`:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.navigation.SettingsNavigationTest
```

Expected: the tests fail because `settings-app-bar` and `side-panel-section-settings` do not exist yet. Do not change the assertions to make the pre-implementation run pass.

### Task 2: Give `SettingsScreen` ownership of the App bar

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt:17-160`

- [ ] **Step 1: Add the App bar test tag import**

Add the existing Compose test-tag import alongside the other UI imports:

```kotlin
import androidx.compose.ui.platform.testTag
```

The file already imports `CenterAlignedTopAppBar`, `Scaffold`, `Icons.AutoMirrored.Outlined.ArrowBack`, `IconButton`, and `fillMaxSize`; use those existing imports instead of introducing another top-bar abstraction.

- [ ] **Step 2: Wrap the current settings body in a route-owned Scaffold**

Replace the current top-level `Column` beginning at line 105 with this structure. Keep the existing section calls and callbacks unchanged inside the body:

```kotlin
Scaffold(
    topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            },
            modifier = Modifier.testTag("settings-app-bar"),
        )
    },
) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ThemeSection(
            current = theme,
            onPick = { viewModel.setThemeMode(it) },
        )
        LanguageSection(
            current = locale,
            onPick = { viewModel.setLocale(it) },
        )
        SecurityLockSection(
            enabled = securityLockEnabled,
            timeoutMinutes = timeoutMin,
            onToggle = { viewModel.setSecurityLockEnabled(it) },
            onDecrement = {
                viewModel.setSecurityLockTimeoutMinutes(timeoutMin - TIMEOUT_STEP)
            },
            onIncrement = {
                viewModel.setSecurityLockTimeoutMinutes(timeoutMin + TIMEOUT_STEP)
            },
        )
        NotificationsSection(
            granted = notificationGranted,
            status = notificationStatus,
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationGranted = true
                    notificationStatus = context.getString(
                        R.string.settings_notifications_status_allowed,
                    )
                }
            },
            onTest = {
                val grantedNow = canPostNotifications(context)
                notificationGranted = grantedNow
                if (grantedNow) {
                    postTestNotification(context)
                    notificationStatus = context.getString(
                        R.string.settings_notifications_test,
                    )
                } else {
                    notificationStatus = context.getString(
                        R.string.settings_notifications_status_denied,
                    )
                }
            },
        )
    }
}
```

This uses the standard Scaffold content padding so the first section is laid out below the measured App bar. Do not add padding in `MobileScaffold`; the settings route must remain isolated from the main shell.

- [ ] **Step 3: Run the focused settings test**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.navigation.SettingsNavigationTest
```

Expected: `settingsScreenOwnsAppBarAndBackAction` passes. The drawer test remains failing until Task 3.

### Task 3: Separate the Setting group in the drawer

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt:37-94`
- Modify: `app/src/main/res/values/strings.xml:77,194-204`
- Modify: `app/src/main/res/values-ja/strings.xml:76,193-203`

- [ ] **Step 1: Remove settings from the primary drawer route list**

Change `drawerRoutes` so it contains only the four main destinations:

```kotlin
private val drawerRoutes = listOf(
    DrawerRoute("timeline", "Timeline", Icons.Outlined.Schedule),
    DrawerRoute("execute", "Tasks", Icons.Outlined.Checklist),
    DrawerRoute("tiles", "Projects", Icons.Outlined.FolderOpen),
    DrawerRoute("integrations", "References", Icons.Outlined.Link),
)
```

- [ ] **Step 2: Add the separated Setting section after the primary routes**

Add `stringResource` and the required layout/text imports, then extend the `Column` content after `drawerRoutes.forEach`:

```kotlin
val settingLabel = stringResource(R.string.nav_setting)

ModalDrawerSheet(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        drawerRoutes.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                    }
                    scope.launch { drawerState.close() }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.testTag("side-panel-icon-${item.route}"),
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(),
                modifier = Modifier.testTag("side-panel-row-${item.route}"),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            text = settingLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 8.dp)
                .testTag("side-panel-section-settings"),
        )
        NavigationDrawerItem(
            label = { Text(settingLabel) },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    launchSingleTop = true
                }
                scope.launch { drawerState.close() }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.testTag("side-panel-icon-settings"),
                )
            },
            colors = NavigationDrawerItemDefaults.colors(),
            modifier = Modifier.testTag("side-panel-row-settings"),
        )
    }
}
```

Use `HorizontalDivider` and `MaterialTheme` imports from Material 3. Keep the existing route navigation and drawer close behavior for the four primary items unchanged.

- [ ] **Step 3: Add the requested resource labels**

In `values/strings.xml`, change the settings title and add the dedicated drawer label:

```xml
<string name="settings_title">Setting</string>
<string name="nav_setting">Setting</string>
```

In `values-ja/strings.xml`, use the same exact requested label rather than translating it:

```xml
<string name="settings_title">Setting</string>
<string name="nav_setting">Setting</string>
```

Do not change the existing `nav_preferences` resource because it is used by the existing section panel content.

- [ ] **Step 4: Run the focused tests**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.navigation.SettingsNavigationTest
```

Expected: both tests pass.

### Task 4: Run repository verification and manually verify the route boundary

**Files:**
- No additional source files.

- [ ] **Step 1: Compile the debug app and run the repository verification suite**

From `C:\Users\rebui\Desktop\tastile\tastile-android`, with JDK 17 selected:

```bash
./gradlew :app:compileDebugKotlin
./gradlew verify
```

Expected: both commands complete successfully. If the Windows host blocks native compilation, record the failure as an environment blocker rather than changing unrelated files.

- [ ] **Step 2: Exercise the navigation on an emulator/device**

1. Launch the authenticated Android app.
2. Open the main menu.
3. Confirm Timeline, Tasks, Projects, and References are grouped together.
4. Confirm a divider and `Setting` section label appear below them.
5. Tap the `Setting` row.
6. Confirm the main shell App bar is not duplicated and the settings App bar is visible with a back button and `Setting` title.
7. Confirm the Theme section begins below the App bar instead of being clipped by it.
8. Tap the App bar back button and confirm the previous main route is restored.

- [ ] **Step 3: Inspect the final diff scope**

```bash
git -C "tastile-android" status --short
git -C "tastile-android" diff -- app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt app/src/main/res/values/strings.xml app/src/main/res/values-ja/strings.xml app/src/androidTest/java/app/tastile/android/ui/navigation/SettingsNavigationTest.kt
```

Expected: the diff contains only the settings navigation implementation, its strings, and the focused test in addition to the pre-existing unrelated working-tree changes. Do not commit or clean the repository unless the user explicitly requests it.
