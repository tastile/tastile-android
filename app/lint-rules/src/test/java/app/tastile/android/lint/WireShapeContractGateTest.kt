package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

@Suppress("JUnitMalformedDeclaration", "FunctionName")
class WireShapeContractGateTest : LintDetectorTest() {
    override fun getDetector() = WireShapeContractGate()
    override fun getIssues() = listOf(WireShapeContractGate.ISSUE)

    fun testNoOpFileScanDoesNotReport() {
        // WireShapeContractGate is a Gradle-task gate; the lint stub is
        // a no-op to keep the rule registered in IssueRegistry. Lock the
        // no-op behaviour so any future regression trips a test.
        lint().allowCompilationErrors().files(kotlin(
            """
            package app.tastile.android.data.api
            class Foo
            """.trimIndent()
        )).run().expectWarningCount(0)
    }

    fun testContractPathsAreStable() {
        // Lock the canonical-keys file and fixture glob against accidental
        // relocation; the Gradle task reads from these constants.
        assert(WireShapeContractGate.CANONICAL_KEYS_PATH ==
            "app/src/main/assets/source_tile_canonical_keys.json")
        assert(WireShapeContractGate.FIXTURE_GLOB ==
            "app/src/test/resources/wire_fixtures/source_tile/*.json")
    }
}