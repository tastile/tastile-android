---
name: design-system-imports-check
description: Use when adding a new Compose screen under `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` or when reviewing such a change before commit.
---

The three top-level UI directories are M3-unified screens and must use the design system components, not direct `androidx.compose.material3.*` imports. The guard task `verifyDesignSystemImports` (app/build.gradle.kts:167-195) enforces this at build time and fails the build if a forbidden import is found without an `// m2-allow:` marker on the immediately preceding non-blank line.

Use this Skill BEFORE adding a new screen or component under the guarded roots. The flow:

1. Check the current M3 primitive you'll reach for:
   - `Icon`, `Text`, `MaterialTheme`, `Surface`, `Scaffold` are always
     available — they have `// m2-allow:` exemptions in every file
     because every screen needs them.
   - `Card`, `Button`, `OutlinedTextField`, `Tab`, `ModalBottomSheet`,
     `DatePicker`, `TimePicker`, `IconButton`, `LoadingWheel`,
     `Background`, `Tag`, `SegmentedButton`, `ViewToggle` are
     wrapped by the design system under
     `app/src/main/java/app/tastile/android/core/designsystem/component/`
     — use the design-system version, not the raw material3 import.

2. The `// m2-allow:` marker rule is exact: the marker must be on the
   line immediately before the forbidden import, with no blank line
   between. The guard only looks at the preceding non-blank line, so
   adding the comment to a `/* ... */` block above a blank line
   does not satisfy the check. Reference: the guard logic in
   `app/build.gradle.kts:178-189` walks the file with `lines.withIndex`
   and checks `lines[idx - 1].trim()` for `// m2-allow:` prefix.

3. The guard only inspects the three guarded roots. New code
   outside `ui/{dashboard,mobile,account}/` can import
   `androidx.compose.material3.*` freely. The guard files are
   declared via `designSystemGuardRoots` (app/build.gradle.kts:157-161).

4. When refactoring existing code that has a stray `material3`
   import without the marker, the fix is either:
   - Replace the import with the design-system equivalent (preferred).
   - Add the `// m2-allow:` marker if the import is unavoidable
     (must be justified in a comment on the same line or on the
     preceding `// m2-allow:` line).

5. After editing, run the guard explicitly to fail fast:

   ```
   ./gradlew :app:verifyDesignSystemImports
   ```

   The full `./gradlew :app:check` runs it as a dependency, but
   iterating on the marker placement benefits from the targeted
   invocation.

Related references:
- AGENTS.md "Build-Time Hard Requirements" section for the policy
  rationale (lint `disable +=` forbidden, design-system imports
  required).
- `app/lint-baseline-old-target-api.md` (placeholder) tracks
  unrelated Android Lint `OldTargetApi` warnings that surface when
  the API-36 SDK is installed.
