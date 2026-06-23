# Changelog

All notable changes to Git Worktree Tools are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-06-22

### Added

- Worktrees tool window presenting each worktree as a list row with branch title, path/commit/age subtitle, commit-message and creator details, and state badges for main, current, safe-to-delete, dirty, locked, prunable, detached, staged/unstaged/untracked counts, HEAD line delta, and divergence from `main` and the remote.
- Search, quick filters, and State, Creator, and Sort choosers over the worktree list, with contextual tooltips and a right-click context menu.
- Git4Idea-backed worktree operations: list, create, open, remove (with optional force and backing-branch deletion), lock, unlock, move, prune, repair, and status loading, all run off the UI thread.
- Open modes for new window, tab on the current frame, replace current project, and the IDE-default project-open prompt, focusing an already-open worktree instead of opening a duplicate.
- Carry-over on first open that copies `.idea/` and `.worktree-copy` manifest entries (optionally all git-ignored files) from the configured source into a new worktree before it opens, enforcing sensitive and heavy-directory denylists, never overwriting existing files, and preserving symlinks without following them outside the source root.
- Carry-over result dialog with copied, skipped, rejected, and failed counts, open gating on copy failures, and explicit reapply actions.
- Safe-to-delete detection that dims and badges worktrees whose work is fully merged into the default branch or its upstream.
- `Git > Worktrees` main-menu group, Project View context group on worktree directories, a worktree marker in the IDE frame title, and a tool-window title drawn from the repository's `owner/repo` identity.
- Settings under Version Control > Git Worktree Tools for the default worktree directory, open mode, carry-over scope and source, `.idea/` copying, manifest file name, automatic-carry-over guard, heavy-path opt-in, and relative locations, with an optional per-project override.

[Unreleased]: https://github.com/findyourexit/intellij-git-worktree-tools/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/findyourexit/intellij-git-worktree-tools/releases/tag/v0.1.0
