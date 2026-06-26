# Changelog

All notable changes to Git Arborist are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Settings changed just before opening a worktree now carry over reliably. Carry-over reads the source project's `.idea/` from disk, but the IDE debounces `PersistentStateComponent` writes and otherwise flushes them only on frame deactivation or project close — so settings edited moments earlier, most visibly **Settings | Tools** entries from third-party plugins (Detekt, Develocity, KtLint, …) persisted to `.idea/` XML files, could be copied stale or missing. The open source project's settings are now flushed to disk before the copy runs.

## [0.1.1] - 2026-06-23

### Removed

- The **Open in New Window**, **Open as Tab**, and **Replace Current Project with Worktree** open modes, their Project View actions, and the "ask each time" prompt. Opening a worktree now always hands off to the IDE's standard project-open flow (open in this window, a new window, or cancel), which already honors your IDE's window-vs-tab preference. The "default open mode" setting is replaced by an "open new worktrees after creation" toggle.

### Fixed

- Opening a worktree no longer risks a `NoSuchMethodError` on IntelliJ 2025.3, 2026.1, and 2026.2 EAP. The former "open as tab" mode relied on `OpenProjectTask.copy(...)`, whose compiler-generated `copy$default` signature changes between platform builds; delegating to the IDE's open flow removes that dependency — and the internal-API workaround it would otherwise require — entirely.
- Replaced the searchable-chooser popup's use of the scheduled-for-removal `SimpleListCellRenderer.create(...)` (flagged on 2026.2) with a non-deprecated `SimpleListCellRenderer` subclass; the popup looks and behaves the same.

### Changed

- The Plugin Verifier release gate now verifies the latest released **and EAP** IntelliJ IDEA builds (auto-resolved, alongside the build-252 floor) and fails on internal, scheduled-for-removal, non-extendable, and override-only API usage, so forward-compatibility problems surface before upload.

## [0.1.0] - 2026-06-22

### Added

- Worktrees tool window presenting each worktree as a list row with branch title, path/commit/age subtitle, commit-message and creator details, and state badges for main, current, safe-to-delete, dirty, locked, prunable, detached, staged/unstaged/untracked counts, HEAD line delta, and divergence from `main` and the remote.
- Search, quick filters, and State, Creator, and Sort choosers over the worktree list, with contextual tooltips and a right-click context menu.
- Git4Idea-backed worktree operations: list, create, open, remove (with optional force and backing-branch deletion), lock, unlock, move, prune, repair, and status loading, all run off the UI thread.
- Open modes for new window, tab on the current frame, replace current project, and the IDE-default project-open prompt, focusing an already-open worktree instead of opening a duplicate.
- Carry-over on first open that copies `.idea/` and `.worktree-copy` manifest entries (optionally all git-ignored files) from the configured source into a new worktree before it opens, enforcing sensitive and heavy-directory denylists, never overwriting existing files, and preserving symlinks without following them outside the source root.
- Carry-over result dialog with copied, skipped, rejected, and failed counts, open gating on copy failures, and explicit reapply actions.
- Safe-to-delete detection that dims and badges worktrees whose work is fully merged into the default branch or its upstream.
- `Git > Worktrees` main-menu group, Project View context group on worktree directories, and a tool-window title drawn from the repository's `owner/repo` identity.
- Settings under Version Control > Git Arborist for the default worktree directory, open mode, carry-over scope and source, `.idea/` copying, manifest file name, automatic-carry-over guard, heavy-path opt-in, and relative locations, with an optional per-project override.

[Unreleased]: https://github.com/findyourexit/intellij-git-arborist/compare/0.1.0...HEAD
[0.1.1]: https://github.com/findyourexit/intellij-git-arborist/releases/tag/0.1.1
[0.1.0]: https://github.com/findyourexit/intellij-git-arborist/releases/tag/0.1.0
