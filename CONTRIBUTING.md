# Contributing to Git Worktree Tools

Thank you for taking the time to contribute. This document covers everything needed to build, test, and submit changes.

─

## Prerequisites

- **JDK 21.** The Gradle wrapper (`gradlew`) is committed to the repository; no Gradle installation is required.
- A working Git installation reachable on `$PATH`.
- IntelliJ IDEA 2025.2 or later is recommended for editing Kotlin source. The project is a standard Gradle project and opens directly.

─

## Building

```bash
./gradlew build
```

This compiles, runs tests, and packages the plugin ZIP under `build/distributions/`.

## Running tests

```bash
./gradlew test
```

## Running a sandbox IDE

```bash
./gradlew runIde
```

Launches a sandboxed IntelliJ IDEA instance with the plugin loaded. Use this to manually smoke-test UI changes before opening a PR.

## Plugin Verifier

```bash
./gradlew verifyPlugin
```

Must pass against both configured targets (IntelliJ IDEA Ultimate 2025.2.6.2 and Android Studio 2025.2.3.9) before a PR is merged.

─

## Code style and static analysis

Run the automated style and quality gates before submitting a PR:

```bash
./gradlew ktlintCheck detekt
```

Use `./gradlew ktlintFormat` to apply Kotlin formatting. detekt uses `config/detekt/detekt.yml`; tune the rule configuration in the same PR as any rule change.

─

## Manual smoke testing

Changes to `WorktreeCarryOverService`, `WorktreeOpenService`, or any action that triggers a project open **require manual smoke testing** in a sandbox IDE before the PR is considered complete. Run `./gradlew runIde`, exercise the affected create/open/carry-over flow, and describe what you tested and what you observed in the PR description.

─

## Submitting changes

1. Fork the repository and create a branch from `main` with a descriptive name (e.g. `fix/carry-over-symlink`, `feat/lock-reason-display`).
2. Make your change. Keep commits focused; one logical change per commit is preferred.
3. Run `./gradlew ktlintCheck detekt test build` and fix any failures.
4. Run `./gradlew verifyPlugin` before changes that affect plugin metadata, platform APIs, dependencies, or distribution behavior.
5. If your change touches open or carry-over flows, run the sandbox smoke test and document the result in the PR description.
6. Open a pull request against `main`. Fill in the PR template completely.

─

## Reporting bugs

Use the [Bug Report](https://github.com/findyourexit/intellij-git-worktree-tools/issues/new?template=bug_report.yml) issue template. Include the IDE version, plugin version (from **Settings > Plugins**), and the full stack trace if one appears in the IDE log (**Help > Show Log in…**).

─

## Proposing features

Open a [Feature Request](https://github.com/findyourexit/intellij-git-worktree-tools/issues/new?template=feature_request.yml) before starting significant work, so the approach can be discussed first.

─

## Commit messages

Use the imperative mood in the subject line (`Add carry-over denylist entry`, not `Added` or `Adding`). Keep the subject under 72 characters. Reference issue numbers in the body when relevant (`Closes #42`).

─

## Code of conduct

All contributors are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
