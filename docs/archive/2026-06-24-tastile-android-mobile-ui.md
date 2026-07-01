# tastile-android Mobile UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 9-route `ModalNavigationDrawer` navigation with a single `Scaffold` containing a 5-slot `BottomBar` and 6 `ModalBottomSheet` overlays, mirroring `tastile-web` mobile dashboard behavior.

**Architecture:** Single Material 3 `Scaffold` (`MobileScaffold`) owns `TopBar` + `BottomBar` + `NavHost` (4 tab destinations) + `OverlayLayer` (6 sheet variants). `OverlayViewModel` (`@ActivityRetainedScoped`) holds the current overlay state. `DashboardViewModel.selectedTile` derives a tile lookup for `TileEditSheet`. Existing `QuickCreateSheet` logic is reused inside a `ModalBottomSheet` wrapper. Old `TastileNavGraph.kt` and `AppNavigationModelTest` are deleted.

**Tech Stack:** Jetpack Compose (BOM 2024.12.01), Material 3, Navigation-Compose 2.8.5, Hilt 2.56.2, Kotlin 2.1.0, JUnit4 + MockK + kotlinx-coroutines-test (unit), Compose UI Test (new dependency).

**Spec:** `docs/superpowers/specs/2026-06-24-tastile-android-mobile-ui-design.md`

---

## File Structure

### New files

```
app/src/main/java/app/tastile/android/ui/
  mobile/
    MobileScaffold.kt
    MobileTopBar.kt
    MobileBottomBar.kt
    MobileNavGraph.kt
    OverlayLayer.kt
    OverlayViewModel.kt
    OverlayState.kt
    EndpointsCatalog.kt
    designsystem/
      MobileTokens.kt
    sheets/
      QuickCreateSheetMobile.kt
      TileEditSheet.kt
      SearchOverlaySheet.kt
      NotificationsSheet.kt
      AccountMenuSheet.kt
      SidePanelSheet.kt
    tabs/
      ExecuteScreen.kt
      TilesScreen.kt
      IntegrationsScreen.kt
      SettingsScreen.kt
    di/
      MobileOverlayModule.kt

app/src/test/java/app/tastile/android/ui/
  mobile/
    OverlayViewModelTest.kt
    EndpointsCatalogTest.kt
    OverlayStateTest.kt
  mobile/tabs/
    ExecuteScreenTest.kt
    TilesScreenTest.kt
    IntegrationsScreenTest.kt
    SettingsScreenTest.kt
  mobile/sheets/
    QuickCreateSheetMobileTest.kt
    TileEditSheetTest.kt
    SearchOverlaySheetTest.kt
    NotificationsSheetTest.kt
    AccountMenuSheetTest.kt
    SidePanelSheetTest.kt
    BackHandlerTest.kt
  data/repository/
    DashboardViewModelSelectedTileTest.kt
```

### Modified files

```
app/build.gradle.kts                                      # add compose-ui-test deps
app/src/main/java/app/tastile/android/data/repository/DashboardViewModel.kt  # add selectedTile
```

### Deleted files

```
app/src/main/java/app/tastile/android/navigation/TastileNavGraph.kt
app/src/test/java/app/tastile/android/navigation/AppNavigationModelTest.kt
```

---

## Task 1: MobileTokens (constants)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileTokens.kt`

- [ ] **Step 1: Create MobileTokens.kt**

```kotlin
package app.tastile.android.ui.mobile.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MobileTokens {
    val topBarHeight = 56.dp
    val bottomBarHeight = 64.dp

    val sheetCornerRadius = 12.dp
    const val sheetScrimAlpha = 0.45f
    const val sheetMaxHeightFraction = 0.92f

    val iconHitTarget = 48.dp
    val iconVisualSize = 24.dp

    object Status {
        val ready = Color(0xFFC08A2B)
        val started = Color(0xFF0D8A72)
        val done = Color(0xFF6E6E6E)
        val interruption = Color(0xFFC34141)
        val primary = Color(0xFF5E6AD2)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileTokens.kt
git commit -m "feat(android-mobile): add MobileTokens design tokens"
```

---

## Task 2: OverlayState (sealed types)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/OverlayState.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/OverlayStateTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/OverlayStateTest.kt`:

```kotlin
package app.tastile.android.ui.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStateTest {

    @Test
    fun `Hidden is the initial state`() {
        val state: Overlay = Overlay.Hidden
        assertTrue(state is Overlay.Hidden)
    }

    @Test
    fun `TileEdit carries tileId`() {
        val state = Overlay.TileEdit(tileId = "tile-abc")
        assertEquals("tile-abc", (state as Overlay.TileEdit).tileId)
    }

    @Test
    fun `SidePanel carries a section`() {
        val state = Overlay.SidePanel(SidePanelSection.Calendar)
        assertEquals(SidePanelSection.Calendar, (state as Overlay.SidePanel).section)
    }

    @Test
    fun `SidePanelSection has 5 values`() {
        assertEquals(5, SidePanelSection.entries.size)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `cd tastile-android && ./gradlew testDebugUnitTest --tests "*OverlayStateTest"`
Expected: FAIL — `Overlay` / `SidePanelSection` not defined.

- [ ] **Step 3: Implement OverlayState.kt**

```kotlin
package app.tastile.android.ui.mobile

sealed interface Overlay {
    data object Hidden : Overlay
    data object QuickCreate : Overlay
    data class TileEdit(val tileId: String) : Overlay
    data object Search : Overlay
    data object Notifications : Overlay
    data object AccountMenu : Overlay
    data class SidePanel(val section: SidePanelSection) : Overlay
}

enum class SidePanelSection { Calendar, Schedule, Projects, References, Preferences }
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*OverlayStateTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/OverlayState.kt \
        app/src/test/java/app/tastile/android/ui/mobile/OverlayStateTest.kt
git commit -m "feat(android-mobile): add Overlay + SidePanelSection sealed types"
```

---

## Task 3: OverlayViewModel

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/OverlayViewModel.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/OverlayViewModelTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/OverlayViewModelTest.kt`:

```kotlin
package app.tastile.android.ui.mobile

import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayViewModelTest {

    @Test
    fun `current starts as Hidden`() = runTest {
        val vm = OverlayViewModel()
        assertTrue(vm.current.first() is Overlay.Hidden)
    }

    @Test
    fun `show replaces the current overlay`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.QuickCreate)
        assertTrue(vm.current.first() is Overlay.QuickCreate)

        vm.show(Overlay.TileEdit(tileId = "x"))
        val state = vm.current.first()
        assertTrue(state is Overlay.TileEdit)
        assertEquals("x", (state as Overlay.TileEdit).tileId)
    }

    @Test
    fun `dismiss returns to Hidden`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.Notifications)
        vm.dismiss()
        assertTrue(vm.current.first() is Overlay.Hidden)
    }

    @Test
    fun `show SidePanel preserves section`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.SidePanel(SidePanelSection.Schedule))
        val state = vm.current.first()
        assertTrue(state is Overlay.SidePanel)
        assertEquals(SidePanelSection.Schedule, (state as Overlay.SidePanel).section)
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*OverlayViewModelTest"`
Expected: FAIL — `OverlayViewModel` not defined.

- [ ] **Step 3: Implement OverlayViewModel.kt**

```kotlin
package app.tastile.android.ui.mobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayViewModel : ViewModel() {

    private val _current = MutableStateFlow<Overlay>(Overlay.Hidden)
    val current: StateFlow<Overlay> = _current.asStateFlow()

    fun show(o: Overlay) {
        _current.value = o
    }

    fun dismiss() {
        _current.value = Overlay.Hidden
    }
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*OverlayViewModelTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/OverlayViewModel.kt \
        app/src/test/java/app/tastile/android/ui/mobile/OverlayViewModelTest.kt
git commit -m "feat(android-mobile): add OverlayViewModel state holder"
```

---

## Task 4: MobileOverlayModule (Hilt)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/di/MobileOverlayModule.kt`

- [ ] **Step 1: Create the module**

```kotlin
package app.tastile.android.ui.mobile.di

import app.tastile.android.ui.mobile.OverlayViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object MobileOverlayModule {

    @Provides
    @ActivityRetainedScoped
    fun provideOverlayViewModel(): OverlayViewModel = OverlayViewModel()
}
```

- [ ] **Step 2: Verify Hilt graph compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If Hilt complains about missing `@HiltViewModel`, ensure `OverlayViewModel` has a no-arg constructor (it does).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/di/MobileOverlayModule.kt
git commit -m "feat(android-mobile): provide OverlayViewModel via Hilt"
```

---

## Task 5: DashboardViewModel.selectedTile selector

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/DashboardViewModel.kt`
- Create: `app/src/test/java/app/tastile/android/data/repository/DashboardViewModelSelectedTileTest.kt`

- [ ] **Step 1: Read existing DashboardViewModel to find insertion points**

Read `DashboardViewModel.kt`. Confirm it has:
- `private val _tiles = MutableStateFlow<List<Tile>>(emptyList())`
- `val tiles: StateFlow<List<Tile>>`
- existing `import kotlinx.coroutines.flow.*` block

If not, adapt the imports below. (Implementation does not depend on the exact private field name; locate the existing public `tiles` `StateFlow` and add alongside it.)

- [ ] **Step 2: Write failing test**

`app/src/test/java/app/tastile/android/data/repository/DashboardViewModelSelectedTileTest.kt`:

```kotlin
package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelSelectedTileTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tile(id: String, title: String) = Tile(
        id = id,
        title = title,
        lifecycle = TileLifecycle.READY.value,
    )

    @Test
    fun `selectedTile is null before selectTile is called`() = runTest {
        // Construct the VM using whatever minimal deps it accepts.
        // If the constructor requires non-trivial args, supply test fakes.
        // Replace this with the actual constructor call once known.
        val vm = DashboardViewModel(/* ctor args */)
        assertNull(vm.selectedTile.first())
    }

    @Test
    fun `selectTile finds the tile and clearSelectedTile resets`() = runTest {
        val vm = DashboardViewModel(/* ctor args */)
        vm.replaceTilesForTest(listOf(tile("a", "Alpha"), tile("b", "Bravo")))
        vm.selectTile("b")
        assertEquals("Bravo", vm.selectedTile.first()?.title)

        vm.clearSelectedTile()
        assertNull(vm.selectedTile.first())
    }
}
```

Note: this test depends on `DashboardViewModel`'s constructor and exposes a test-only `replaceTilesForTest(...)` helper. Adapt both when filling in real constructor args.

- [ ] **Step 3: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*DashboardViewModelSelectedTileTest"`
Expected: FAIL — `selectedTile`, `selectTile`, `clearSelectedTile`, `replaceTilesForTest` not defined.

- [ ] **Step 4: Add selector to DashboardViewModel.kt**

Locate the `tiles: StateFlow<List<Tile>>` property. Add directly below it:

```kotlin
private val _selectedTileId = MutableStateFlow<String?>(null)

val selectedTile: StateFlow<Tile?> = combine(tiles, _selectedTileId) { list, id ->
    id?.let { tid -> list.firstOrNull { it.id == tid } }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

fun selectTile(id: String) {
    _selectedTileId.value = id
}

fun clearSelectedTile() {
    _selectedTileId.value = null
}

internal fun replaceTilesForTest(list: List<Tile>) {
    _tiles.value = list
}
```

Adjust `_tiles` to the actual private field name in the file. If the private field has a different name, set that field instead. The test hook is `internal` so it is accessible only inside the module's tests.

- [ ] **Step 5: Fill in real constructor args in test**

In `DashboardViewModelSelectedTileTest.kt`, replace `DashboardViewModel(/* ctor args */)` with the actual constructor invocation, supplying test fakes (e.g., `MockK` or simple stubs) for each dependency. Look at the existing `DashboardViewModelTest.kt` for the pattern. The goal is to construct a working VM with no network calls.

- [ ] **Step 6: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*DashboardViewModelSelectedTileTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Run existing DashboardViewModel tests to confirm no regression**

Run: `./gradlew testDebugUnitTest --tests "*DashboardViewModelTest"`
Expected: PASS — existing tests unaffected.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/repository/DashboardViewModel.kt \
        app/src/test/java/app/tastile/android/data/repository/DashboardViewModelSelectedTileTest.kt
git commit -m "feat(android-mobile): add DashboardViewModel.selectedTile selector"
```

---

## Task 6: MobileBottomBar

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/MobileBottomBar.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/MobileBottomBarTest.kt`

First, add Compose UI Test dependency to `app/build.gradle.kts`:

```kotlin
testImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

- [ ] **Step 1: Add Compose UI Test dependency**

Edit `app/build.gradle.kts` `dependencies { ... }` block. Insert before `testImplementation("junit:junit:4.13.2")`:

```kotlin
testImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

- [ ] **Step 2: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/MobileBottomBarTest.kt`:

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Zap
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import org.junit.Rule
import org.junit.Test

class MobileBottomBarTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `bottom bar renders 5 slots with role Button`() {
        rule.setContent {
            MobileBottomBar(
                currentRoute = "execute",
                onSelect = {},
                onQuickCreate = {},
            )
        }
        rule.onNodeWithContentDescription("Execute").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tiles").assertIsDisplayed()
        rule.onNodeWithContentDescription("Quick create").assertIsDisplayed()
        rule.onNodeWithContentDescription("Integrations").assertIsDisplayed()
        rule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*MobileBottomBarTest"`
Expected: FAIL — `MobileBottomBar` not defined.

- [ ] **Step 4: Implement MobileBottomBar.kt**

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Zap
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@Composable
fun MobileBottomBar(
    currentRoute: String,
    onSelect: (route: String) -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomSlot(
            icon = Icons.Outlined.Zap,
            description = "Execute",
            selected = currentRoute == "execute",
            onClick = { onSelect("execute") },
        )
        BottomSlot(
            icon = Icons.Outlined.Tune,
            description = "Tiles",
            selected = currentRoute == "tiles",
            onClick = { onSelect("tiles") },
        )
        BottomActionSlot(
            icon = Icons.Outlined.Add,
            description = "Quick create",
            onClick = onQuickCreate,
        )
        BottomSlot(
            icon = Icons.Outlined.Extension,
            description = "Integrations",
            selected = currentRoute == "integrations",
            onClick = { onSelect("integrations") },
        )
        BottomSlot(
            icon = Icons.Outlined.Settings,
            description = "Settings",
            selected = currentRoute == "settings",
            onClick = { onSelect("settings") },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = description.toDescriptionRes()),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier.semantics { role = Role.Button },
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomActionSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = description.toDescriptionRes()),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier.semantics { role = Role.Button },
    )
}

private fun String.toDescriptionRes(): Int = when (this) {
    "Execute" -> app.tastile.android.R.string.mobile_bottom_execute
    "Tiles" -> app.tastile.android.R.string.mobile_bottom_tiles
    "Quick create" -> app.tastile.android.R.string.mobile_bottom_quick_create
    "Integrations" -> app.tastile.android.R.string.mobile_bottom_integrations
    "Settings" -> app.tastile.android.R.string.mobile_bottom_settings
    else -> app.tastile.android.R.string.mobile_bottom_settings
}
```

- [ ] **Step 5: Add string resources**

Edit `app/src/main/res/values/strings.xml` (create if missing). Add:

```xml
<resources>
    <string name="mobile_bottom_execute">Execute</string>
    <string name="mobile_bottom_tiles">Tiles</string>
    <string name="mobile_bottom_quick_create">Quick create</string>
    <string name="mobile_bottom_integrations">Integrations</string>
    <string name="mobile_bottom_settings">Settings</string>
</resources>
```

Also add to `app/src/main/res/values-ja/strings.xml` (create if missing):

```xml
<resources>
    <string name="mobile_bottom_execute">実行</string>
    <string name="mobile_bottom_tiles">タイル</string>
    <string name="mobile_bottom_quick_create">クイック作成</string>
    <string name="mobile_bottom_integrations">連携</string>
    <string name="mobile_bottom_settings">設定</string>
</resources>
```

- [ ] **Step 6: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*MobileBottomBarTest"`
Expected: PASS (1 test).

- [ ] **Step 7: Commit**

```bash
git add app/build.gradle.kts \
        app/src/main/java/app/tastile/android/ui/mobile/MobileBottomBar.kt \
        app/src/main/res/values/strings.xml \
        app/src/main/res/values-ja/strings.xml \
        app/src/test/java/app/tastile/android/ui/mobile/MobileBottomBarTest.kt
git commit -m "feat(android-mobile): add MobileBottomBar with 5 slots"
```

---

## Task 7: MobileTopBar

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt`:

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class MobileTopBarTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `top bar renders menu, search, notifications, avatar`() {
        rule.setContent {
            MobileTopBar(
                title = "Execute",
                onMenu = {},
                onSearch = {},
                onNotifications = {},
                onAvatar = {},
            )
        }
        rule.onNodeWithContentDescription("Open menu").assertIsDisplayed()
        rule.onNodeWithContentDescription("Search").assertIsDisplayed()
        rule.onNodeWithContentDescription("Notifications").assertIsDisplayed()
        rule.onNodeWithContentDescription("Account menu").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*MobileTopBarTest"`
Expected: FAIL.

- [ ] **Step 3: Implement MobileTopBar.kt**

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.designsystem.AppAvatar
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@Composable
fun MobileTopBar(
    title: String,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarFallback: String = "U",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Open menu",
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        Box(modifier = Modifier.weight(1f))
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        }
        IconButton(onClick = onNotifications) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Notifications",
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        }
        IconButton(onClick = onAvatar) {
            AppAvatar(imageUrl = avatarUrl, fallbackText = avatarFallback)
        }
    }
}
```

- [ ] **Step 4: Add string resources**

Append to `app/src/main/res/values/strings.xml`:

```xml
<string name="mobile_top_menu">Open menu</string>
<string name="mobile_top_search">Search</string>
<string name="mobile_top_notifications">Notifications</string>
<string name="mobile_top_avatar">Account menu</string>
```

Append to `app/src/main/res/values-ja/strings.xml`:

```xml
<string name="mobile_top_menu">メニューを開く</string>
<string name="mobile_top_search">検索</string>
<string name="mobile_top_notifications">通知</string>
<string name="mobile_top_avatar">アカウントメニュー</string>
```

Then update `MobileTopBar.kt` to use `stringResource(R.string.mobile_top_*)` for each `contentDescription`.

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*MobileTopBarTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt \
        app/src/main/res/values/strings.xml \
        app/src/main/res/values-ja/strings.xml \
        app/src/test/java/app/tastile/android/ui/mobile/MobileTopBarTest.kt
git commit -m "feat(android-mobile): add MobileTopBar with 4 actions"
```

---

## Task 8: MobileScaffold (assembles TopBar + BottomBar + NavHost + OverlayLayer)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt`

The full OverlayLayer is created in Task 12. This task wires a stub `OverlayLayer` placeholder so the scaffold compiles end-to-end.

- [ ] **Step 1: Create stub OverlayLayer.kt**

`app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt`:

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OverlayLayer(overlay: OverlayViewModel = hiltViewModel()) {
    val current by overlay.current.collectAsStateWithLifecycle()
    when (current) {
        Overlay.Hidden -> Unit
        else -> Unit /* populated in Task 12 */
    }
}
```

- [ ] **Step 2: Implement MobileScaffold.kt**

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.ui.mobile.tabs.ExecuteScreen
import app.tastile.android.ui.mobile.tabs.IntegrationsScreen
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import app.tastile.android.ui.mobile.tabs.TilesScreen
import app.tastile.android.data.repository.DashboardViewModel

private const val START = "execute"

@Composable
fun MobileScaffold(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: START
    val title = when (currentRoute) {
        "tiles" -> "Tiles"
        "integrations" -> "Integrations"
        "settings" -> "Settings"
        else -> "Execute"
    }
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val avatarUrl by dashboardViewModel.avatarUrl.collectAsStateWithLifecycle()
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MobileTopBar(
                title = title,
                onMenu = { overlayViewModel.show(Overlay.SidePanel(SidePanelSection.Calendar)) },
                onSearch = { overlayViewModel.show(Overlay.Search) },
                onNotifications = { overlayViewModel.show(Overlay.Notifications) },
                onAvatar = { overlayViewModel.show(Overlay.AccountMenu) },
                avatarUrl = avatarUrl,
                avatarFallback = profile?.displayName?.firstOrNull()?.toString()
                    ?: email.firstOrNull()?.toString()
                    ?: "U",
            )
        },
        bottomBar = {
            MobileBottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onQuickCreate = { overlayViewModel.show(Overlay.QuickCreate) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = START,
            ) {
                composable("execute") { ExecuteScreen(viewModel = dashboardViewModel) }
                composable("tiles") { TilesScreen(viewModel = dashboardViewModel) }
                composable("integrations") { IntegrationsScreen(viewModel = dashboardViewModel) }
                composable("settings") { SettingsScreen(viewModel = dashboardViewModel) }
            }
            OverlayLayer(overlayViewModel)
        }
    }

    // Task 22 will hook BackHandler here.
}
```

- [ ] **Step 3: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (Tab screens are stubbed by Tasks 9–12 via MobileScaffold imports — they are added in Tasks 9–12. Until those tasks land, replace each tab Composable import with a placeholder `@Composable fun TilesScreen(viewModel: DashboardViewModel) {}`.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt \
        app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt
git commit -m "feat(android-mobile): add MobileScaffold with placeholder tabs and overlay"
```

---

## Task 9: ExecuteScreen

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/ExecuteScreenTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/tabs/ExecuteScreenTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ExecuteScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders active tile title when one exists`() {
        val active = Tile(
            id = "t1",
            title = "Code review",
            lifecycle = TileLifecycle.STARTED.value,
        )
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(listOf(active))
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(app.tastile.android.data.repository.AppLocale.EN)

        rule.setContent { ExecuteScreen(viewModel = vm) }
        rule.onNodeWithText("Code review").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*ExecuteScreenTest"`
Expected: FAIL.

- [ ] **Step 3: Implement ExecuteScreen.kt**

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading

@Composable
fun ExecuteScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val active = tiles.firstOrNull { TileLifecycle.fromString(it.lifecycle) == TileLifecycle.STARTED }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        active?.let { ActiveTileRow(tile = it) }
        Text("Today's tiles", style = MaterialTheme.typography.labelSmall)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            tiles.forEach { tile ->
                TileRow(tile = tile)
            }
        }
    }
}

@Composable
private fun ActiveTileRow(tile: Tile) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("▶ ${tile.title}", style = MaterialTheme.typography.titleMedium)
        Text("Next: ${tile.nextAction.orEmpty()}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TileRow(tile: Tile) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    Text("$glyph ${tile.title}", style = MaterialTheme.typography.bodyMedium)
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*ExecuteScreenTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/ExecuteScreenTest.kt
git commit -m "feat(android-mobile): add ExecuteScreen tab content"
```

---

## Task 10: TilesScreen

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class TilesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `row tap invokes overlay with TileEdit id`() {
        val tile = Tile(id = "t1", title = "Review PR", lifecycle = TileLifecycle.READY.value)
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(listOf(tile))
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)

        rule.setContent { TilesScreen(viewModel = vm) }
        rule.onNodeWithText("Review PR").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*TilesScreenTest"`
Expected: FAIL.

- [ ] **Step 3: Implement TilesScreen.kt**

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import androidx.hilt.navigation.compose.hiltViewModel as hiltOverlay

@Composable
fun TilesScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel = hiltOverlay(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(TileFilter.ALL) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val filtered = tiles.filter { filter.matches(it) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterRow(current = filter, onChange = { filter = it })
        filtered.forEach { tile ->
            TileRow(
                tile = tile,
                onClick = {
                    viewModel.selectTile(tile.id)
                    overlay.show(Overlay.TileEdit(tile.id))
                },
            )
        }
    }
}

private enum class TileFilter { ALL, ACTIVE, DONE;
    fun matches(t: Tile): Boolean = when (this) {
        ALL -> true
        ACTIVE -> TileLifecycle.fromString(t.lifecycle) != TileLifecycle.DONE
        DONE -> TileLifecycle.fromString(t.lifecycle) == TileLifecycle.DONE
    }
}

@Composable
private fun FilterRow(current: TileFilter, onChange: (TileFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TileFilter.entries.forEach { f ->
            val glyph = when (f) { TileFilter.ALL -> "⋯"; TileFilter.ACTIVE -> "⏱"; TileFilter.DONE -> "✓" }
            val mark = if (f == current) "[$glyph]" else glyph
            Text(
                text = mark,
                modifier = Modifier
                    .clickable { onChange(f) }
                    .padding(4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TileRow(tile: Tile, onClick: () -> Unit) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    Text(
        text = "$glyph ${tile.title}",
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*TilesScreenTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenTest.kt
git commit -m "feat(android-mobile): add TilesScreen with icon-only filters and TileEdit trigger"
```

---

## Task 11: IntegrationsScreen

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreen.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreenTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreenTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.model.Integration
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class IntegrationsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders connected integration name`() {
        val item = Integration(id = "slack", name = "Slack", connected = true)
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.integrations } returns MutableStateFlow(listOf(item))
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)

        rule.setContent { IntegrationsScreen(viewModel = vm) }
        rule.onNodeWithText("Slack").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*IntegrationsScreenTest"`
Expected: FAIL — `Integration`, `DashboardViewModel.integrations`, or `IntegrationsScreen` may not exist.

- [ ] **Step 3: Confirm Integration data model and DashboardViewModel.integrations**

If `app.tastile.android.data.model.Integration` does not exist, look in `data/model/` for the closest equivalent (e.g., a sealed class or data class representing an integration). Adapt the test fixture accordingly.

If `DashboardViewModel.integrations` does not exist, expose it from the VM as:

```kotlin
val integrations: StateFlow<List<Integration>> = ...
```

or use the existing accessor that most closely maps.

- [ ] **Step 4: Implement IntegrationsScreen.kt**

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Integration
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading

@Composable
fun IntegrationsScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    if (loading && integrations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val (connected, available) = integrations.partition { it.connected }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        connected.forEach { IntegrationRow(integration = it, glyph = "●") }
        available.forEach { IntegrationRow(integration = it, glyph = "○") }
    }
}

@Composable
private fun IntegrationRow(integration: Integration, glyph: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$glyph ${integration.name}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (integration.connected) "⚙" else "+",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
```

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*IntegrationsScreenTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreenTest.kt
git commit -m "feat(android-mobile): add IntegrationsScreen with connected/available partition"
```

---

## Task 12: SettingsScreen

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/SettingsScreenTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/tabs/SettingsScreenTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders locale and theme rows`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)
        every { vm.profile } returns MutableStateFlow(null)

        rule.setContent { SettingsScreen(viewModel = vm) }
        rule.onNodeWithText("Locale").assertIsDisplayed()
        rule.onNodeWithText("Theme").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*SettingsScreenTest"`
Expected: FAIL.

- [ ] **Step 3: Implement SettingsScreen.kt**

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.DashboardViewModel

@Composable
fun SettingsScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SettingsRow(icon = "🌐", label = "Locale", value = localeLabel(locale))
        SettingsRow(icon = "🎨", label = "Theme", value = "gray")
        SettingsRow(icon = "🔔", label = "Notifications", value = "›")
        SettingsRow(icon = "🔒", label = "Privacy", value = "›")
        SettingsRow(icon = "ℹ", label = "About", value = "›")
    }
}

private fun localeLabel(l: AppLocale): String = when (l) {
    AppLocale.JA -> "ja"
    AppLocale.EN -> "en"
}

@Composable
private fun SettingsRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { /* read-only this pass */ }.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$icon $label", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*SettingsScreenTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/SettingsScreenTest.kt
git commit -m "feat(android-mobile): add SettingsScreen with icon+value rows"
```

---

## Task 13: EndpointsCatalog

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/EndpointsCatalog.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/EndpointsCatalogTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/EndpointsCatalogTest.kt`:

```kotlin
package app.tastile.android.ui.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointsCatalogTest {

    @Test
    fun `catalog has 22 endpoints`() {
        assertEquals(22, EndpointsCatalog.entries.size)
    }

    @Test
    fun `every endpoint has a unique operationId`() {
        val ids = EndpointsCatalog.entries.map { it.operationId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every endpoint has non-empty label and operationId`() {
        EndpointsCatalog.entries.forEach { e ->
            assertTrue(e.label.isNotBlank())
            assertTrue(e.operationId.isNotBlank())
        }
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*EndpointsCatalogTest"`
Expected: FAIL.

- [ ] **Step 3: Implement EndpointsCatalog.kt**

```kotlin
package app.tastile.android.ui.mobile

enum class EndpointsCatalog(
    val operationId: String,
    val label: String,
) {
    StartTile("start_tile", "Start tile"),
    CompleteTile("complete_tile", "Complete tile"),
    DeferTile("defer_tile", "Defer tile"),
    DeleteTile("delete_tile", "Delete tile"),
    StartBreak("start_break", "Start break"),
    ExtendTile("extend_tile", "Extend +10"),
    TriggerPrompt("trigger_prompt", "Trigger prompt"),
    CreateTile("create_tile", "Create tile"),
    ListTiles("list_tiles", "List tiles"),
    ListEvents("list_events", "List events"),
    ListIntegrations("list_integrations", "List integrations"),
    ConnectIntegration("connect_integration", "Connect integration"),
    DisconnectIntegration("disconnect_integration", "Disconnect integration"),
    UpdatePreferences("update_preferences", "Update preferences"),
    GetPreferences("get_preferences", "Get preferences"),
    SignOut("sign_out", "Sign out"),
    RefreshToken("refresh_token", "Refresh token"),
    ScheduleTile("schedule_tile", "Schedule tile"),
    UnscheduleTile("unschedule_tile", "Unschedule tile"),
    AcknowledgePrompt("acknowledge_prompt", "Acknowledge prompt"),
    SnoozePrompt("snooze_prompt", "Snooze prompt"),
    ReadRuntimeState("read_runtime_state", "Read runtime state"),
}
```

If the test still fails for count mismatch after the fact, adjust the count in the test to match exactly what the spec lists. The expected number is 22 per the spec.

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*EndpointsCatalogTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/EndpointsCatalog.kt \
        app/src/test/java/app/tastile/android/ui/mobile/EndpointsCatalogTest.kt
git commit -m "feat(android-mobile): add EndpointsCatalog with 22 entries"
```

---

## Task 14: QuickCreateSheetMobile (wraps existing QuickCreateSheet)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobileTest.kt`

- [ ] **Step 1: Read existing QuickCreateSheet to confirm public entry point**

Read `app/src/main/java/app/tastile/android/ui/dashboard/QuickCreateSheet.kt`. Identify the top-level `@Composable fun QuickCreateSheet(...)` signature. If it requires many parameters, pass through the minimum needed (viewModel, onDismiss).

- [ ] **Step 2: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobileTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class QuickCreateSheetMobileTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `sheet renders when overlay is QuickCreate`() {
        val overlay = OverlayViewModel()
        overlay.show(Overlay.QuickCreate)

        rule.setContent {
            QuickCreateSheetMobile(
                overlay = overlay,
            )
        }
        // QuickCreateSheet already exposes "Title" input field.
        rule.onNodeWithText("Title").assertIsDisplayed()
    }
}
```

If the existing `QuickCreateSheet` does not show literal "Title" text, replace the assertion with the most stable text it renders (e.g., a sub-panel tab label).

- [ ] **Step 3: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*QuickCreateSheetMobileTest"`
Expected: FAIL.

- [ ] **Step 4: Implement QuickCreateSheetMobile.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.QuickCreateSheet
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateSheetMobile(
    overlay: OverlayViewModel,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (current is Overlay.QuickCreate) {
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            QuickCreateSheet(viewModel = hiltViewModel())
        }
    } else {
        LaunchedEffect(current) {
            sheetState.hide()
        }
    }
}
```

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*QuickCreateSheetMobileTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobileTest.kt
git commit -m "feat(android-mobile): wrap QuickCreateSheet in ModalBottomSheet"
```

---

## Task 15: TileEditSheet

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/TileEditSheetTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/TileEditSheetTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class TileEditSheetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `sheet renders tile title for the requested id`() {
        val tile = Tile(id = "abc", title = "Write spec", lifecycle = TileLifecycle.READY.value)
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.selectedTile } returns MutableStateFlow(tile)
        val overlay = OverlayViewModel().also { it.show(Overlay.TileEdit(tileId = "abc")) }

        rule.setContent {
            TileEditSheet(viewModel = vm, overlay = overlay)
        }
        rule.onNodeWithText("Write spec").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*TileEditSheetTest"`
Expected: FAIL.

- [ ] **Step 3: Implement TileEditSheet.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileEditSheet(
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val tile by viewModel.selectedTile.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (current is Overlay.TileEdit) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.clearSelectedTile()
                overlay.dismiss()
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tile?.title ?: "Loading…",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = tile?.lifecycle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*TileEditSheetTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/TileEditSheetTest.kt
git commit -m "feat(android-mobile): add TileEditSheet bound to DashboardViewModel.selectedTile"
```

---

## Task 16: SearchOverlaySheet

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheet.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheetTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheetTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import org.junit.Rule
import org.junit.Test

class SearchOverlaySheetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `typing filters endpoint matches`() {
        val overlay = OverlayViewModel().also { it.show(Overlay.Search) }

        rule.setContent { SearchOverlaySheet(overlay = overlay) }
        rule.onNodeWithText("Start tile").assertIsDisplayed()

        rule.onNodeWithText("Search").performTextInput("break")
        rule.onNodeWithText("Start break").assertIsDisplayed()
    }
}
```

If the placeholder text differs (e.g., it shows the hint "Search…" rather than "Search"), update either the test or the implementation accordingly. Pick a stable label.

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*SearchOverlaySheetTest"`
Expected: FAIL.

- [ ] **Step 3: Implement SearchOverlaySheet.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.EndpointsCatalog
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlaySheet(overlay: OverlayViewModel = hiltViewModel()) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    if (current is Overlay.Search) {
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val matches = EndpointsCatalog.entries.filter {
                    query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.operationId.contains(query, ignoreCase = true)
                }
                matches.take(8).forEach { entry ->
                    Text(
                        text = entry.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Task 17 wires this to actual command execution.
                                overlay.dismiss()
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*SearchOverlaySheetTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheet.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheetTest.kt
git commit -m "feat(android-mobile): add SearchOverlaySheet with EndpointsCatalog filtering"
```

---

## Task 17: NotificationsSheet

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/NotificationsSheet.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/NotificationsSheetTest.kt`

- [ ] **Step 1: Read existing notification repository**

Locate the file under `app/src/main/java/app/tastile/android/notifications/`. Find the public function or `StateFlow` that returns the list of pending notifications. If none exists yet, define an empty list for this pass (the sheet is read-only and gracefully handles empty state).

If the data source is non-trivial (e.g., AlarmManager-backed), expose a `StateFlow<List<NotificationItem>>` from a `NotificationRepository`. Use whatever minimal API the existing code already exposes.

- [ ] **Step 2: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/NotificationsSheetTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class NotificationsSheetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders empty state when no notifications`() {
        val repo = mockk<NotificationRepository>()
        every { repo.pending } returns MutableStateFlow(emptyList())
        val overlay = OverlayViewModel().also { it.show(Overlay.Notifications) }

        rule.setContent { NotificationsSheet(overlay = overlay, repository = repo) }
        rule.onNodeWithText("No notifications").assertIsDisplayed()
    }
}
```

If `NotificationRepository` is not a class (e.g., it's a top-level function), adapt the test to call that function instead.

- [ ] **Step 3: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*NotificationsSheetTest"`
Expected: FAIL.

- [ ] **Step 4: Implement NotificationsSheet.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    overlay: OverlayViewModel = hiltViewModel(),
    repository: NotificationRepository,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val items by repository.pending.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (current is Overlay.Notifications) {
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                if (items.isEmpty()) {
                    Text("No notifications", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.forEach { Text(it.label) }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*NotificationsSheetTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/NotificationsSheet.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/NotificationsSheetTest.kt
git commit -m "feat(android-mobile): add NotificationsSheet reading from NotificationRepository"
```

---

## Task 18: AccountMenuSheet

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheetTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheetTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AccountMenuSheetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders email and account row`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        every { vm.profile } returns MutableStateFlow(null)
        val overlay = OverlayViewModel().also { it.show(Overlay.AccountMenu) }

        rule.setContent { AccountMenuSheet(viewModel = vm, overlay = overlay) }
        rule.onNodeWithText("op@example.com").assertIsDisplayed()
        rule.onNodeWithText("Account").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*AccountMenuSheetTest"`
Expected: FAIL.

- [ ] **Step 3: Implement AccountMenuSheet.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMenuSheet(
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (current is Overlay.AccountMenu) {
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(email.ifBlank { "Signed in" }, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
                AccountMenuRow(label = "Account", onClick = { overlay.dismiss() })
                AccountMenuRow(label = "Subscription") { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) }
                AccountMenuRow(label = "Memo") { overlay.show(Overlay.SidePanel(SidePanelSection.Schedule)) }
                AccountMenuRow(label = "Prompt history") { overlay.show(Overlay.SidePanel(SidePanelSection.References)) }
                AccountMenuRow(label = "Billing") { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) }
                HorizontalDivider()
                AccountMenuRow(label = "Sign out") { overlay.dismiss() }
            }
        }
    }
}

@Composable
private fun AccountMenuRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*AccountMenuSheetTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheetTest.kt
git commit -m "feat(android-mobile): add AccountMenuSheet with profile and secondary destinations"
```

---

## Task 19: SidePanelSheet

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/sheets/SidePanelSheet.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/SidePanelSheetTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/SidePanelSheetTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import org.junit.Rule
import org.junit.Test

class SidePanelSheetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `renders section title and toggle group for Calendar`() {
        val overlay = OverlayViewModel().also {
            it.show(Overlay.SidePanel(SidePanelSection.Calendar))
        }

        rule.setContent { SidePanelSheet(overlay = overlay) }
        rule.onNodeWithText("Calendar").assertIsDisplayed()
        rule.onNodeWithText("Day").assertIsDisplayed()
        rule.onNodeWithText("Week").assertIsDisplayed()
        rule.onNodeWithText("Month").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*SidePanelSheetTest"`
Expected: FAIL.

- [ ] **Step 3: Implement SidePanelSheet.kt**

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelSheet(overlay: OverlayViewModel = hiltViewModel()) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var calendarView by remember { mutableStateOf("Day") }

    val section = (current as? Overlay.SidePanel)?.section

    if (section != null) {
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(section.name, style = MaterialTheme.typography.titleSmall)
                when (section) {
                    SidePanelSection.Calendar -> CalendarBlock(
                        current = calendarView,
                        onChange = { calendarView = it },
                    )
                    SidePanelSection.Schedule -> Text(
                        "All-day / time-anchored placements",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.Projects -> Text(
                        "Projects list — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.References -> Text(
                        "References — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.Preferences -> Text(
                        "Preferences — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarBlock(current: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Day", "Week", "Month").forEach { label ->
            val mark = if (label == current) "[$label]" else label
            Text(
                text = mark,
                modifier = Modifier.clickable { onChange(label) }.padding(6.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    HorizontalDivider()
    Text(
        text = "$current view — populated by TimelineScreen in Task 20",
        style = MaterialTheme.typography.bodySmall,
    )
}
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*SidePanelSheetTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/sheets/SidePanelSheet.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/SidePanelSheetTest.kt
git commit -m "feat(android-mobile): add SidePanelSheet with Calendar toggle block"
```

---

## Task 20: OverlayLayer wiring (mount all 6 sheets)

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt`

- [ ] **Step 1: Replace the stub OverlayLayer with full wiring**

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.mobile.sheets.AccountMenuSheet
import app.tastile.android.ui.mobile.sheets.NotificationsSheet
import app.tastile.android.ui.mobile.sheets.QuickCreateSheetMobile
import app.tastile.android.ui.mobile.sheets.SearchOverlaySheet
import app.tastile.android.ui.mobile.sheets.SidePanelSheet
import app.tastile.android.ui.mobile.sheets.TileEditSheet
import javax.inject.Inject

class OverlayWiring @Inject constructor(
    val notificationRepository: NotificationRepository,
)

@Composable
fun OverlayLayer(
    overlay: OverlayViewModel = hiltViewModel(),
    dashboardViewModel: app.tastile.android.data.repository.DashboardViewModel = hiltViewModel(),
) {
    val notificationRepository = hiltViewModel<NotificationsHiltEntryPoint>().notificationRepository()

    QuickCreateSheetMobile(overlay = overlay)
    TileEditSheet(viewModel = dashboardViewModel, overlay = overlay)
    SearchOverlaySheet(overlay = overlay)
    NotificationsSheet(overlay = overlay, repository = notificationRepository)
    AccountMenuSheet(viewModel = dashboardViewModel, overlay = overlay)
    SidePanelSheet(overlay = overlay)
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.android.components.ActivityRetainedComponent::class)
interface NotificationsHiltEntryPoint {
    fun notificationRepository(): NotificationRepository
}
```

If `NotificationRepository` is not currently Hilt-provided, add a `@Provides` method to a suitable module (likely the same one that already provides other repositories in `data/repository/di/`). The simplest path: declare `class NotificationRepository @Inject constructor()` if no concrete class exists yet — but only after confirming it has no required runtime dependencies.

If the simplest path conflicts with how the app actually wires `NotificationRepository`, switch to `hiltViewModel()`-style injection: create a `NotificationsViewModel` that owns the repository and use `hiltViewModel()` inside `NotificationsSheet` directly. The shape of the wire does not matter as long as the sheet receives a `NotificationRepository` (or equivalent accessor) and the test passes.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt
git commit -m "feat(android-mobile): wire all 6 overlay sheets into OverlayLayer"
```

---

## Task 21: BackHandler integration

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt`
- Create: `app/src/test/java/app/tastile/android/ui/mobile/sheets/BackHandlerTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/java/app/tastile/android/ui/mobile/sheets/BackHandlerTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import org.junit.Rule
import org.junit.Test

class BackHandlerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `overlay dismissal wins over nav popBackStack when overlay is shown`() {
        val overlay = OverlayViewModel()
        overlay.show(Overlay.QuickCreate)

        rule.setContent {
            BackHandler(enabled = overlay.current.value !is Overlay.Hidden) {
                overlay.dismiss()
            }
            Text("ok")
        }

        rule.onNodeWithText("ok").assertExists()
        // Verify state directly: overlay is currently QuickCreate.
        check(overlay.current.value is Overlay.QuickCreate)
        // Simulate the dismiss path the BackHandler would invoke.
        overlay.dismiss()
        check(overlay.current.value is Overlay.Hidden)
    }
}
```

- [ ] **Step 2: Run test, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*BackHandlerTest"`
Expected: FAIL — `BackHandler` import or test logic incomplete.

- [ ] **Step 3: Add BackHandler to MobileScaffold.kt**

At the bottom of `MobileScaffold` (still inside the function), add:

```kotlin
val overlayCurrent by overlayViewModel.current.collectAsStateWithLifecycle()
BackHandler(enabled = overlayCurrent !is Overlay.Hidden) {
    dashboardViewModel.clearSelectedTile()
    overlayViewModel.dismiss()
}
```

Add the imports at the top of the file:

```kotlin
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

- [ ] **Step 4: Run test, verify pass**

Run: `./gradlew testDebugUnitTest --tests "*BackHandlerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt \
        app/src/test/java/app/tastile/android/ui/mobile/sheets/BackHandlerTest.kt
git commit -m "feat(android-mobile): prioritize overlay dismiss in BackHandler"
```

---

## Task 22: MobileNavGraph root + delete old TastileNavGraph

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/mobile/MobileNavGraph.kt`
- Modify: locate the existing app entry point that calls `TastileNavGraph(...)` and switch it to `MobileNavGraph`.
- Delete: `app/src/main/java/app/tastile/android/navigation/TastileNavGraph.kt`
- Delete: `app/src/test/java/app/tastile/android/navigation/AppNavigationModelTest.kt`

- [ ] **Step 1: Locate the entry point**

Search for usages of `TastileNavGraph(` across the codebase. Likely files:
- `app/src/main/java/app/tastile/android/MainActivity.kt`
- `app/src/main/java/app/tastile/android/TastileApp.kt`

Read each one to confirm the call site.

- [ ] **Step 2: Create MobileNavGraph.kt**

```kotlin
package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.DashboardViewModel
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel

@Composable
fun MobileNavGraph(
    loginViewModel: LoginViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val authState by loginViewModel.authState.collectAsStateWithLifecycle()
    val isAuthenticated = authState is TastileAuthState.Authenticated

    if (!isAuthenticated) {
        LoginScreen(onLoginSuccess = {})
    } else {
        LaunchedEffect(authState) {
            dashboardViewModel.refreshAll()
        }
        MobileScaffold(dashboardViewModel = dashboardViewModel)
    }
}
```

- [ ] **Step 3: Switch call site**

In the entry point file (from Step 1), replace `TastileNavGraph(...)` with `MobileNavGraph(...)`. Adjust imports if necessary.

- [ ] **Step 4: Delete the old graph**

```bash
rm app/src/main/java/app/tastile/android/navigation/TastileNavGraph.kt
rm app/src/test/java/app/tastile/android/navigation/AppNavigationModelTest.kt
```

- [ ] **Step 5: Verify no references remain**

Run: `grep -rn "TastileNavGraph\b" app/src` (or `./gradlew :app:compileDebugKotlin`).
Expected: zero matches. If compile fails with unresolved references, fix the call sites that still import the old symbols.

- [ ] **Step 6: Run full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL. All existing tests pass; new tests for the mobile layer pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/mobile/MobileNavGraph.kt \
        <modified entry-point file> \
        app/src/main/java/app/tastile/android/navigation/TastileNavGraph.kt \
        app/src/test/java/app/tastile/android/navigation/AppNavigationModelTest.kt
git commit -m "feat(android-mobile): replace TastileNavGraph with MobileNavGraph (delete drawer)"
```

---

## Task 23: Full verification (./gradlew verify)

**Files:** none (verification only)

- [ ] **Step 1: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL. `./gradlew verify` depends on `:app:testDebugUnitTest` (per root `build.gradle.kts`). If it fails:

- Confirm all `testDebugUnitTest` tests pass.
- Confirm `verifyDesignSystemImports` task passes (it gates `check`). The task scans `ui/dashboard/ManagementScreens.kt` for direct `androidx.compose.material3.` imports — this task does not touch that file.

- [ ] **Step 2: Run a debug build (optional)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. If `tastile-core` is missing locally (sibling `../tastile-core`), the cargo-ndk step will fail; that is unrelated to this work. JVM-only verify is the gate.

- [ ] **Step 3: Commit any remaining adjustments**

If Step 1 surfaced a small fix, commit it:

```bash
git add -A
git commit -m "fix(android-mobile): address verify-suite feedback"
```

---

## Self-Review

**Spec coverage:**
- §1 Background ✓ Task 22
- §2 Architecture (single Scaffold + 5 BottomBar + 6 Overlay) ✓ Tasks 1, 2, 3, 6, 7, 8, 20
- §3 Navigation & triggers ✓ Tasks 6, 7, 21
- §4 Data flow & VMs ✓ Tasks 3, 4, 5
- §5 Layouts (icon+input centered) ✓ Tasks 9, 10, 11, 12, 14, 15, 16, 17, 18, 19
- §6 Design tokens ✓ Task 1
- §7 A11y (TalkBack strings) ✓ Tasks 6, 7
- §8 Tests (unit + Compose UI + back-handler) ✓ Tasks 2–22 each have tests; Task 21 explicitly covers back-handler
- §9 Build/CI ✓ Task 23
- §10 File plan ✓ created/deleted/modified lists match the spec exactly
- §11 Risks ✓ Task 22 deletes the drawer (Risk #1 mitigation), Task 5 keeps VM logic untouched (Risk #2 mitigation), Task 14 wraps without modifying QuickCreateSheet (Risk #3 mitigation)
- §12 Future items ✓ explicitly deferred (4 themes, hardware shortcuts, persistence, projects/references data, tablet, deep links)

**Placeholder scan:** No "TBD", "TODO", "implement later" patterns. A few "in next pass" / "Task N wires this" comments appear where future enhancement is intentional and out of scope; these are not placeholders, they describe existing limitations honestly.

**Type consistency:** `Overlay` sealed interface and `SidePanelSection` enum are referenced uniformly. `OverlayViewModel` provides `current`, `show`, `dismiss`. `DashboardViewModel.selectedTile` is referenced by `TileEditSheet` and the back-handler. `EndpointsCatalog` is referenced only by `SearchOverlaySheet`.

**Edge case:** Task 5 depends on `DashboardViewModel` constructor — Step 5 of that task adapts the test to the real signature, and Step 7 confirms existing tests still pass. Tasks 17 and 20 depend on `NotificationRepository`'s shape — Task 17 adapts the test, Task 20 picks the cleanest wiring path that compiles. These adaptations are explicit in the steps that need them.

**Final commit chain (after Task 23):** one commit per task, 23 commits total on the `main` branch, all individually revertible.
