# TV navigation + home screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the unbounded edge scroll, make the chrome bar reachable on remotes with no MENU key, and give the home screen a random gradient background plus two seeded favorites.

**Architecture:** Three independent changes to existing modules, following the repo's split of pure Kotlin logic (unit-tested on the JVM) from thin Android glue. New pure objects: `CursorGeometry.scrollStep`, `home/HomeBackgrounds`, `data/DefaultFavorites`. Glue changes touch `CursorController`, `CursorLayout`, `HomeView`, and `MainActivity`. Two internal booleans are added to `Settings` to make the seed and the discoverability hint fire exactly once.

**Tech Stack:** Kotlin, framework `android.app.Activity` (no AndroidX outside Media3), JUnit 4, Gradle 9.3.1 / AGP 9.1.1, JDK 17.

**Spec:** `docs/superpowers/specs/2026-07-30-tv-nav-and-home-design.md`

## Global Constraints

- No AndroidX or Compose dependencies may be added. Activities extend the framework `Activity`; UI is XML in `res/layout`.
- Pure modules are plain Kotlin `object`s/classes with **no Android imports**, placed so they can be unit-tested without a device.
- Dependency versions live in `gradle/libs.versions.toml`, never in `app/build.gradle.kts`. This plan adds no dependencies.
- Netflix-style dark theme; brand red is `#E50914`.
- All user-visible text goes through `res/values/strings.xml`. No hardcoded strings in Kotlin.
- Full test command: `./gradlew test`. Single class: `./gradlew test --tests "com.EdS.mrowserF.<pkg>.<Class>"`.
- Do **not** push. Commit locally only.
- Do not bump `versionCode` / `versionName` — that belongs to the release step.

---

### Task 1: Clamp the cursor's edge scroll

The bug: `CursorGeometry.step` clamps the cursor to `y = 0`, which keeps it permanently inside the 48dp top edge zone, so `CursorController.mover` fires `webView.scrollBy(0, -24)` every 16ms with nothing stopping it. `scrollY` goes negative and blank space opens above the page.

**Files:**
- Modify: `app/src/main/kotlin/net/mrowser/web/CursorGeometry.kt` (add `scrollStep` after `isAtBottomEdge`, line 33)
- Modify: `app/src/main/kotlin/net/mrowser/web/CursorController.kt:44-45`
- Test: `app/src/test/kotlin/net/mrowser/web/CursorGeometryTest.kt`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: `CursorGeometry.scrollStep(dirY: Int, y: Float, height: Int, zonePx: Float, stepPx: Int, canScrollUp: Boolean, canScrollDown: Boolean): Int`. No later task uses it.

- [ ] **Step 1: Write the failing tests**

Append these to `CursorGeometryTest`, inside the class, after the existing `edge detection flags top and bottom zones` test:

```kotlin
    @Test fun `no scroll up when the page is already at the top`() {
        assertEquals(0, CursorGeometry.scrollStep(-1, 0f, 1000, 48f, 24, canScrollUp = false, canScrollDown = true))
    }

    @Test fun `scrolls up at the top edge while the page can still scroll`() {
        assertEquals(-24, CursorGeometry.scrollStep(-1, 10f, 1000, 48f, 24, canScrollUp = true, canScrollDown = true))
    }

    @Test fun `no scroll down when the page is already at the bottom`() {
        assertEquals(0, CursorGeometry.scrollStep(1, 1000f, 1000, 48f, 24, canScrollUp = true, canScrollDown = false))
    }

    @Test fun `scrolls down at the bottom edge while the page can still scroll`() {
        assertEquals(24, CursorGeometry.scrollStep(1, 990f, 1000, 48f, 24, canScrollUp = true, canScrollDown = true))
    }

    @Test fun `no scroll away from either edge`() {
        assertEquals(0, CursorGeometry.scrollStep(-1, 500f, 1000, 48f, 24, canScrollUp = true, canScrollDown = true))
        assertEquals(0, CursorGeometry.scrollStep(1, 500f, 1000, 48f, 24, canScrollUp = true, canScrollDown = true))
    }

    @Test fun `no scroll for horizontal or idle movement`() {
        assertEquals(0, CursorGeometry.scrollStep(0, 0f, 1000, 48f, 24, canScrollUp = true, canScrollDown = true))
    }
```

No new imports needed — `assertEquals` is already imported.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.EdS.mrowserF.web.CursorGeometryTest"`
Expected: compilation failure, `Unresolved reference: scrollStep`.

- [ ] **Step 3: Add the pure function**

In `CursorGeometry.kt`, after `isAtBottomEdge`:

```kotlin
    /**
     * Vertical page-scroll delta for one frame, or 0 when the page must not move.
     * [canScrollUp] / [canScrollDown] are the clamp: the cursor pins to y=0, which
     * keeps it inside the top edge zone forever, so without the gate the caller
     * scrolls the page up without bound and blank space opens above it.
     */
    fun scrollStep(
        dirY: Int,
        y: Float,
        height: Int,
        zonePx: Float,
        stepPx: Int,
        canScrollUp: Boolean,
        canScrollDown: Boolean
    ): Int = when {
        dirY < 0 && canScrollUp && isAtTopEdge(y, zonePx) -> -stepPx
        dirY > 0 && canScrollDown && isAtBottomEdge(y, height, zonePx) -> stepPx
        else -> 0
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.EdS.mrowserF.web.CursorGeometryTest"`
Expected: PASS, all tests including the pre-existing ones.

- [ ] **Step 5: Wire it into the controller**

In `CursorController.kt`, replace these two lines inside `mover.run` (currently lines 44-45):

```kotlin
            if (dirY < 0 && CursorGeometry.isAtTopEdge(y, edgeZonePx)) webView.scrollBy(0, -SCROLL_STEP_PX)
            if (dirY > 0 && CursorGeometry.isAtBottomEdge(y, webView.height, edgeZonePx)) webView.scrollBy(0, SCROLL_STEP_PX)
```

with:

```kotlin
            val scroll = CursorGeometry.scrollStep(
                dirY, y, webView.height, edgeZonePx, SCROLL_STEP_PX,
                canScrollUp = webView.canScrollVertically(-1),
                canScrollDown = webView.canScrollVertically(1)
            )
            if (scroll != 0) {
                webView.scrollBy(0, scroll)
                // The boolean gate can't stop the final step overshooting the top by
                // up to SCROLL_STEP_PX; pin it so no blank strip opens above the page.
                if (webView.scrollY < 0) webView.scrollTo(0, 0)
            }
```

The bottom needs no equivalent correction: one overshoot flips `canScrollDown` false and the loop stops.

- [ ] **Step 6: Verify the whole suite and the build**

Run: `./gradlew test assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/net/mrowser/web/CursorGeometry.kt \
        app/src/main/kotlin/net/mrowser/web/CursorController.kt \
        app/src/test/kotlin/net/mrowser/web/CursorGeometryTest.kt
git commit -m "fix: stop the cursor scrolling the page past its ends"
```

---

### Task 2: Add the two internal Settings flags

Both flags are internal bookkeeping, not user preferences — they get no row in `SettingsView`. Task 3 uses `navHintShown`; Task 4 uses `seeded`.

**Files:**
- Modify: `app/src/main/kotlin/net/mrowser/data/Settings.kt`
- Modify: `app/src/main/kotlin/net/mrowser/data/SettingsJson.kt`
- Test: `app/src/test/kotlin/net/mrowser/data/SettingsJsonTest.kt`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: `Settings.seeded: Boolean` (default `false`) and `Settings.navHintShown: Boolean` (default `false`), both round-tripped by `SettingsJson`. Read via `SettingsRepository.get()`, written via `SettingsRepository.update(settings.copy(...))`.

- [ ] **Step 1: Write the failing tests**

Append to `SettingsJsonTest`, inside the class:

```kotlin
    @Test fun `round trips the internal flags`() {
        val s = Settings(seeded = true, navHintShown = true)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `internal flags default to false for a pre-upgrade file`() {
        val s = SettingsJson.fromJson("""{"autoOpenPlayer":true,"cursorSpeed":"NORMAL"}""")
        assertFalse(s.seeded)
        assertFalse(s.navHintShown)
    }
```

Add the import at the top of the file, after the existing `assertEquals` import:

```kotlin
import org.junit.Assert.assertFalse
```

The second test is the upgrade path: an existing v1.0 install has a `settings.json` with no such keys, so it must read as `false` and receive the seed once.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.EdS.mrowserF.data.SettingsJsonTest"`
Expected: compilation failure, `Cannot find a parameter with this name: seeded`.

- [ ] **Step 3: Add the fields**

Replace the whole body of `Settings.kt`:

```kotlin
package com.EdS.mrowserF.data

/** App-wide settings. Defaults are the shipped values. Immutable — update via copy(). */
data class Settings(
    val autoOpenPlayer: Boolean = true,
    val cursorSpeed: CursorSpeed = CursorSpeed.NORMAL,
    /** Internal bookkeeping, not a user preference: default favorites written once. */
    val seeded: Boolean = false,
    /** Internal bookkeeping, not a user preference: hold-BACK hint shown once. */
    val navHintShown: Boolean = false
)
```

In `SettingsJson.kt`, extend `toJson`:

```kotlin
    fun toJson(settings: Settings): String =
        JSONObject()
            .put("autoOpenPlayer", settings.autoOpenPlayer)
            .put("cursorSpeed", settings.cursorSpeed.name)
            .put("seeded", settings.seeded)
            .put("navHintShown", settings.navHintShown)
            .toString()
```

and extend the `Settings(...)` construction inside `fromJson`:

```kotlin
            Settings(
                autoOpenPlayer = o.optBoolean("autoOpenPlayer", defaults.autoOpenPlayer),
                cursorSpeed = enumOrDefault(o.optString("cursorSpeed"), defaults.cursorSpeed),
                seeded = o.optBoolean("seeded", defaults.seeded),
                navHintShown = o.optBoolean("navHintShown", defaults.navHintShown)
            )
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.EdS.mrowserF.data.SettingsJsonTest"`
Expected: PASS, including the four pre-existing tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/net/mrowser/data/Settings.kt \
        app/src/main/kotlin/net/mrowser/data/SettingsJson.kt \
        app/src/test/kotlin/net/mrowser/data/SettingsJsonTest.kt
git commit -m "feat: persist seeded and navHintShown flags in settings"
```

---

### Task 3: Long-press BACK opens the chrome bar

The chrome bar is currently summoned only by `KEYCODE_MENU`, which most TV remotes (Mi Box 4K included) do not have. Short BACK keeps every meaning it has today.

**Files:**
- Modify: `app/src/main/kotlin/net/mrowser/web/CursorLayout.kt`
- Modify: `app/src/main/kotlin/net/mrowser/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Settings.navHintShown` from Task 2. `ChromeController.isVisible` and `ChromeController.requestReveal(atTop: Boolean, focusInput: Boolean = true)` already exist. `BrowserWebChromeClient.isFullscreen: Boolean` already exists (`web/BrowserWebChromeClient.kt:25`).
- Produces: `CursorLayout.onLongBack: () -> Unit` and `CursorLayout.LONG_PRESS_MS: Long`. No later task uses them.

This task has no unit test — it is Android input glue with no pure logic to extract. Verification is a clean build plus the manual check in Step 5.

- [ ] **Step 1: Hoist the long-press duration to a constant**

In `CursorLayout.kt`, the OK long-press currently hardcodes `500`. The class has no `companion object` yet — it ends with `dispatchDraw` — so create one just before the class's closing brace:

```kotlin
    companion object { const val LONG_PRESS_MS = 500L }
```

Then in `handleOk`, replace:

```kotlin
                longPressHandler.postDelayed(longPress, 500)
```

with:

```kotlin
                longPressHandler.postDelayed(longPress, LONG_PRESS_MS)
```

- [ ] **Step 2: Add the BACK long-press state and callback**

In `CursorLayout.kt`, add the callback next to the existing `onExitPage` declaration:

```kotlin
    /** Set by the Activity; fired when BACK is held. Summons the chrome bar. */
    var onLongBack: () -> Unit = {}
```

and add the state fields next to the existing `okDown` / `longPressed` / `longPress` group:

```kotlin
    private var backDown = false
    private var backLongPressed = false
    private val backLongPress = Runnable { backLongPressed = true; onLongBack() }
```

- [ ] **Step 3: Route the BACK key through a long-press handler**

In `dispatchKeyEvent`, replace this block:

```kotlin
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) return handleBack()
            return true
        }
```

with:

```kotlin
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return handleBackKey(event)
```

and add the new method just above `handleBack`:

```kotlin
    /**
     * BACK tap keeps every meaning it has today (see handleBack); BACK held summons
     * the chrome bar. Most TV remotes have no MENU key, which was the only other way
     * to open it. Repeated ACTION_DOWNs arrive while the key is held — the backDown
     * flag arms the timer once, the same way okDown does.
     */
    private fun handleBackKey(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (!backDown) {
                backDown = true
                backLongPressed = false
                // A bar that's already up keeps BACK as "close me" — don't arm the reveal.
                if (!chrome.isVisible) longPressHandler.postDelayed(backLongPress, LONG_PRESS_MS)
            }
            KeyEvent.ACTION_UP -> {
                backDown = false
                longPressHandler.removeCallbacks(backLongPress)
                if (!backLongPressed) return handleBack()
            }
        }
        return true
    }
```

- [ ] **Step 4: Wire the Activity and add the one-time hint**

Add the string to `res/values/strings.xml`, after the `close_tab` entry:

```xml
    <string name="nav_hint">Hold BACK for the address bar</string>
```

In `MainActivity.onCreate`, add this line directly after `layout.onExitPage = { confirmCloseTab() }`:

```kotlin
        layout.onLongBack = { if (!chromeClient.isFullscreen) chrome.requestReveal(atTop = true) }
```

The fullscreen check lives here, not in `CursorLayout`, so the layout stays ignorant of the chrome client — matching how `onBack` and `onExitPage` are already wired. Without it, holding BACK during an HTML5 fullscreen video would stack the address bar over the picture.

Then add the hint call as the last line of `openUrl`:

```kotlin
    private fun openUrl(url: String) {
        hideAllOverlays()
        layout.requestFocus()
        clearHistoryOnLoad = true
        webView.loadUrl(url)
        chrome.onPageInteracted()
        showNavHintOnce()
    }

    /** One-time nudge: the chrome bar has no MENU key to summon it on most TV remotes. */
    private fun showNavHintOnce() {
        if (settings.get().navHintShown) return
        settings.update(settings.get().copy(navHintShown = true))
        Toast.makeText(this, R.string.nav_hint, Toast.LENGTH_LONG).show()
    }
```

`Toast` is already imported in `MainActivity.kt` (line 18).

- [ ] **Step 5: Verify**

Run: `./gradlew test assembleDebug`
Expected: BUILD SUCCESSFUL, no test regressions.

Then, if a device or emulator is available, install and check by hand:
1. Open a site from home — the hint Toast appears once, and never again on later launches.
2. Hold BACK on the page — the address bar slides in with focus in the URL field, and the page does **not** go back.
3. Tap BACK on the page — the page goes back (or the close-tab dialog appears at the root).
4. With the bar open, tap BACK — the bar closes; holding BACK does not reopen it.
5. Enter an HTML5 fullscreen video and hold BACK — no bar appears over the video.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/net/mrowser/web/CursorLayout.kt \
        app/src/main/kotlin/net/mrowser/MainActivity.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat: hold BACK to open the address bar"
```

---

### Task 4: Home gradient background and seeded favorites

**Files:**
- Create: `app/src/main/kotlin/net/mrowser/home/HomeBackgrounds.kt`
- Create: `app/src/main/kotlin/net/mrowser/data/DefaultFavorites.kt`
- Create: `app/src/test/kotlin/net/mrowser/home/HomeBackgroundsTest.kt`
- Create: `app/src/test/kotlin/net/mrowser/data/DefaultFavoritesTest.kt`
- Modify: `app/src/main/kotlin/net/mrowser/home/HomeView.kt`
- Modify: `app/src/main/kotlin/net/mrowser/MainActivity.kt`
- Modify: `app/src/main/res/layout/home_view.xml:6`

**Interfaces:**
- Consumes: `Settings.seeded` from Task 2. `Favorite(title: String, url: String)` and `FavoritesRepository.add(favorite: Favorite)` already exist.
- Produces: `HomeBackgrounds.Gradient(top: Int, bottom: Int)`, `HomeBackgrounds.ALL: List<Gradient>`, `HomeBackgrounds.pick(seed: Long): Gradient`, and `DefaultFavorites.ALL: List<Favorite>`. Nothing later depends on them.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/kotlin/net/mrowser/home/HomeBackgroundsTest.kt`:

```kotlin
package com.EdS.mrowserF.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBackgroundsTest {

    @Test fun `pick is deterministic for a given seed`() {
        assertEquals(HomeBackgrounds.pick(42L), HomeBackgrounds.pick(42L))
    }

    @Test fun `pick always returns a known gradient`() {
        (0L until 100L).forEach { assertTrue(HomeBackgrounds.pick(it) in HomeBackgrounds.ALL) }
    }

    @Test fun `pick survives negative seeds`() {
        assertTrue(HomeBackgrounds.pick(-7L) in HomeBackgrounds.ALL)
        assertTrue(HomeBackgrounds.pick(Long.MIN_VALUE) in HomeBackgrounds.ALL)
    }

    @Test fun `consecutive seeds walk the whole list`() {
        val seen = (0L until HomeBackgrounds.ALL.size.toLong()).map { HomeBackgrounds.pick(it) }.toSet()
        assertEquals(HomeBackgrounds.ALL.size, seen.size)
    }
}
```

Create `app/src/test/kotlin/net/mrowser/data/DefaultFavoritesTest.kt`:

```kotlin
package com.EdS.mrowserF.data

import com.EdS.mrowserF.web.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFavoritesTest {

    @Test fun `every shipped url is already canonical`() {
        DefaultFavorites.ALL.forEach { assertEquals(it.url, UrlNormalizer.normalize(it.url)) }
    }

    @Test fun `every shipped favorite has a title`() {
        DefaultFavorites.ALL.forEach { assertTrue(it.title.isNotBlank()) }
    }

    @Test fun `shipped urls are unique`() {
        assertEquals(DefaultFavorites.ALL.size, DefaultFavorites.ALL.map { it.url }.toSet().size)
    }
}
```

The first test matters because a non-canonical seed URL would be stored differently from what the star button later writes for the same page, breaking the favorite toggle's `it.url == url` match.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.EdS.mrowserF.home.HomeBackgroundsTest" --tests "com.EdS.mrowserF.data.DefaultFavoritesTest"`
Expected: compilation failure, `Unresolved reference: HomeBackgrounds`.

- [ ] **Step 3: Write the pure objects**

Create `app/src/main/kotlin/net/mrowser/home/HomeBackgrounds.kt`:

```kotlin
package com.EdS.mrowserF.home

/** Pure: the home overlay's random background gradients. No Android types. */
object HomeBackgrounds {

    /** A two-stop vertical gradient, top to bottom, as ARGB ints. */
    data class Gradient(val top: Int, val bottom: Int)

    /** All kept low-luminance so white text and the #E50914 accent stay legible. */
    val ALL: List<Gradient> = listOf(
        Gradient(0xFF0B0B0F.toInt(), 0xFF1A0E12.toInt()), // ember
        Gradient(0xFF080F14.toInt(), 0xFF0E1A22.toInt()), // deep sea
        Gradient(0xFF100A18.toInt(), 0xFF1C1024.toInt()), // violet
        Gradient(0xFF0A0F0C.toInt(), 0xFF12201A.toInt()), // pine
        Gradient(0xFF120C0A.toInt(), 0xFF231310.toInt()), // rust
        Gradient(0xFF0C0C0C.toInt(), 0xFF1B1B20.toInt())  // graphite
    )

    /** Deterministic for a given [seed]; safe for negative seeds. */
    fun pick(seed: Long): Gradient = ALL[(((seed % ALL.size) + ALL.size) % ALL.size).toInt()]
}
```

Create `app/src/main/kotlin/net/mrowser/data/DefaultFavorites.kt`:

```kotlin
package com.EdS.mrowserF.data

/** Pure: the starter favorites written once, on first launch. */
object DefaultFavorites {
    val ALL: List<Favorite> = listOf(
        Favorite("YouTube", "https://www.youtube.com/tv"),
        Favorite("Google", "https://www.google.com")
    )
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.EdS.mrowserF.home.HomeBackgroundsTest" --tests "com.EdS.mrowserF.data.DefaultFavoritesTest"`
Expected: PASS.

- [ ] **Step 5: Paint the background**

In `res/layout/home_view.xml`, delete line 6 from the root `LinearLayout` so the drawable set in code is not painted over:

```xml
    android:background="@color/surface"
```

In `HomeView.kt`, add the import next to the existing `android.graphics.Color` import:

```kotlin
import android.graphics.drawable.GradientDrawable
```

Add this call as the last line of `bind`, after `this.onSettings = onSettings`:

```kotlin
        applyRandomBackground()
```

and add the method just above `show()`:

```kotlin
    /** Rolled once per launch — in bind, not show — so the color doesn't change
     *  every time the user returns to the home overlay. */
    private fun applyRandomBackground() {
        val g = HomeBackgrounds.pick(System.currentTimeMillis())
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(g.top, g.bottom)
        )
    }
```

- [ ] **Step 6: Seed the favorites on first launch**

In `MainActivity.kt`, add the import next to the other `com.EdS.mrowserF.data` imports:

```kotlin
import com.EdS.mrowserF.data.DefaultFavorites
```

In `onCreate`, add this line directly after the three store constructions (`favorites` / `history` / `settings`):

```kotlin
        seedDefaultFavorites()
```

and add the method next to the other private helpers, e.g. above `recordHistory`:

```kotlin
    /** First launch only: write the shipped starter favorites. Guarded by a persisted
     *  flag rather than an is-empty check — a user who deletes them must not get them
     *  back. Existing installs have no flag in settings.json, so they seed once on upgrade. */
    private fun seedDefaultFavorites() {
        if (settings.get().seeded) return
        DefaultFavorites.ALL.forEach { favorites.add(it) }
        settings.update(settings.get().copy(seeded = true))
    }
```

Placement matters: it must run before `showHome()` at the end of `onCreate`, so the first `refresh()` already shows the tiles.

- [ ] **Step 7: Verify**

Run: `./gradlew test assembleDebug`
Expected: BUILD SUCCESSFUL.

If a device or emulator is available, install over a **fresh** install and check:
1. Home shows YouTube and Google tiles, and the background is a dark gradient rather than flat grey.
2. Delete both tiles, force-stop, relaunch — they stay deleted.
3. Relaunch a few times — the gradient varies between launches but never changes while navigating home → history → home.
4. Open the YouTube tile. If `youtube.com/tv` rejects the WebView user agent, change the seed URL in `DefaultFavorites` to `https://m.youtube.com`, re-run `./gradlew test`, and note the swap in the commit message.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/net/mrowser/home/HomeBackgrounds.kt \
        app/src/main/kotlin/net/mrowser/data/DefaultFavorites.kt \
        app/src/main/kotlin/net/mrowser/home/HomeView.kt \
        app/src/main/kotlin/net/mrowser/MainActivity.kt \
        app/src/main/res/layout/home_view.xml \
        app/src/test/kotlin/net/mrowser/home/HomeBackgroundsTest.kt \
        app/src/test/kotlin/net/mrowser/data/DefaultFavoritesTest.kt
git commit -m "feat: gradient home background and seeded starter favorites"
```

---

### Task 5: Update CLAUDE.md

`CLAUDE.md` documents the control scheme and the home/settings behaviour; three of its claims are now stale.

**Files:**
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: the finished behaviour from Tasks 1, 3, and 4.
- Produces: nothing.

- [ ] **Step 1: Correct the three stale statements**

1. In the `web/` section, the sentence describing `ChromeController` / `ChromeVisibility` says the bar is "summoned only with **MENU**". Change it to say the bar is summoned with **MENU or a BACK long-press** (500ms), and that the long-press is skipped when the bar is already visible or an HTML5 fullscreen video is playing.
2. In the same section's `CursorLayout` paragraph, note that BACK is now tap-versus-hold: the tap chain is unchanged, the hold reveals the chrome bar.
3. In the `home/` + `data/` section, note that `HomeView` paints a random `HomeBackgrounds` gradient chosen once per launch, and that `DefaultFavorites` seeds YouTube and Google on first launch, guarded by the `seeded` flag on `Settings` (alongside `navHintShown` for the one-time hold-BACK Toast) — both internal flags with no row in `SettingsView`.

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: record BACK long-press, home gradient, and seeded favorites"
```

---

## Done criteria

- `./gradlew test assembleDebug` passes.
- Scrolling up at the top of a page stops at the top; no blank strip appears.
- Holding BACK opens the address bar on a remote with no MENU key; tapping BACK behaves exactly as before.
- A fresh install shows YouTube and Google tiles over a dark gradient; deleting them is permanent.
- Nothing is pushed. All five commits stay local until the user says otherwise.
