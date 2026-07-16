package app.tastile.android.ui.designsystem

import org.junit.Test
import java.io.File

class TokenTest {
    @Test fun `no Color(0x hex literal outside designsystem allowlist`() {
        // Walk ui/ from the test-classpath CWD (which is app/, the module dir).
        // If running from elsewhere, fall back to a relative path that still
        // resolves to app/src/main/java/app/tastile/android/ui.
        val candidates = listOf(
            File("src/main/java/app/tastile/android/ui"),
            File("../src/main/java/app/tastile/android/ui"),
            File("app/src/main/java/app/tastile/android/ui"),
        )
        val root = candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate ui/ root; tried: ${candidates.joinToString { it.path }}")

        val allowList = setOf(
            "designsystem/BrandColors.kt",
            "designsystem/Theme.kt",
        )
        val violation = root.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { it.readText().contains(Regex("""Color\(0x[0-9A-Fa-f]+""")) }
            .filter { f -> allowList.none { f.path.endsWith(it) } }
            .toList()
        assert(violation.isEmpty()) {
            "Found forbidden Color(0x literals in:\n" +
                violation.joinToString("\n") { " - " + it.path }
        }
    }
}
