# Git Worktree Tools

Git Worktree Tools makes Git worktrees first-class inside JetBrains IDEs. Create, open, and manage worktrees from a dedicated tool window, the **Git** menu, and the Project View — and carry your project setup (run configurations, code style, local tooling) into a new worktree automatically, before the IDE opens it for the first time.

A Git worktree checks out a second branch of the same repository into its own directory, so you can review a pull request without stashing, run a long build on one branch while you keep coding on another, or keep a release branch open beside `main`. This plugin removes the friction of driving that workflow from inside the IDE.

## Features

- **Worktrees tool window.** A Pull-Request-style list where each worktree is one tall row: branch title, a dimmed path / commit / age subtitle, an optional commit-message and creator line, and compact state badges (main, current, safe-to-delete, dirty, locked, prunable, detached, staged/unstaged/untracked counts, HEAD line delta, and divergence from `main` and the remote). Status loads asynchronously and never blocks the UI.
- **Search, filter, and sort.** Full-width search across branch, path, commit, message, creator, and state, plus quick filters and dedicated State, Creator, and Sort choosers.
- **Every worktree operation, through Git4Idea.** List, create, open, remove (with optional force and backing-branch deletion), lock and unlock, move, prune, and repair — all run on a background thread.
- **Flexible open modes.** Open a worktree in a new window, as a tab on the current frame, in place of the current project, or through the IDE's normal project-open prompt. Already-open worktrees are focused instead of opened twice.
- **Carry over project setup on first open.** Before a new worktree opens for the first time, the plugin copies `.idea/` and any files listed in a `.worktree-copy` manifest from the main worktree into the new one, so your run configurations, code style, and local tooling are present immediately. Secrets and heavy build directories are never copied (see [Carry-over](#carry-over-on-first-open)).
- **Safe-to-delete detection.** Worktrees whose work is fully merged are dimmed and badged `SAFE`, computed from a clean working tree plus standard Git cleanup checks.
- **IDE integration.** A `Git > Worktrees` menu, a Project View context group on worktree directories, a worktree marker in the frame title, and a tool-window title drawn from the repository's `owner/repo` identity.

## Compatibility

- **IntelliJ IDEA 2025.2 or later** (build 252+).
- **Android Studio 2025.2.x.**

Any IntelliJ Platform 252+ IDE that bundles the **Git4Idea** plugin can load it. Compatibility is checked on every change with the JetBrains Plugin Verifier against IntelliJ IDEA Ultimate 2025.2.6.2 and Android Studio 2025.2.3.9.

## Installation

### From the JetBrains Marketplace

In your IDE, open **Settings → Plugins → Marketplace**, search for **Git Worktree Tools**, and click **Install**.

### From a downloaded ZIP

1. Download a release ZIP from the [Releases](https://github.com/findyourexit/intellij-git-worktree-tools/releases) page, or build one (see [Building from source](#building-from-source)).
2. In your IDE, open **Settings → Plugins → ⚙ → Install Plugin from Disk…**.
3. Select the ZIP and restart the IDE.

## Usage

Open the **Git Worktree Tools** tool window from the right-hand tool-window bar, or run **Git → Worktrees → Open Worktrees Panel**.

- **Create** a worktree with the `+` title action. Choose a starting point (branch, tag, commit, or `HEAD`), optionally name a new branch, pick a target directory, and decide whether to open it afterward.
- **Open** a worktree by double-clicking it, pressing Enter, or using the **Open** title action. Right-click a row for new-window, tab, and replace-current-project variants.
- **Manage** worktrees from the row's right-click menu or the `Git > Worktrees` menu: lock, unlock, move, reapply carry-over, and remove. Removing offers force removal and, when a local branch backs the worktree, deletion of that branch.
- **Prune** and **Repair** the repository's worktree administrative data from the `Git > Worktrees` menu.

### Carry-over on first open

When a worktree is opened for the first time (detected by the absence of a `.idea/` directory in it), the plugin copies project setup from a source root — the main worktree by default — into the new worktree before the IDE opens it:

- `.idea/` is copied so run configurations and code style are available immediately.
- Every path listed in a `.worktree-copy` manifest at the source root is copied. The manifest is UTF-8 text, one path per line, relative to the source root; blank lines and lines starting with `#` are ignored.

Carry-over never overwrites a file that already exists in the target, preserves symlinks as symlinks (without following them outside the source root), and always skips a denylist of sensitive files (`.env`, `*.local`, anything matching `*secret*`, `*token*`, `*credential*`, `*private*`, and `.idea/httpRequests/`). Heavy build and dependency directories (`node_modules`, `build`, `.gradle`, `target`, and similar) are skipped unless you opt in. A result dialog reports what was copied, skipped, rejected, and failed; if any copy fails you are asked before the worktree opens. Use **Reapply Carry-over…** to run it again on an existing worktree.

### Settings

**Settings → Version Control → Git Worktree Tools** configures the global defaults and an optional per-project override:

- Default worktree directory (used to seed the create dialog's target path).
- Default open mode.
- Carry-over scope (curated, all-ignored-minus-denylist, or manifest-only) and source (main worktree or current project).
- Whether to copy `.idea/`, the manifest file name, whether to run automatic carry-over only when the target `.idea/` is missing, whether to allow heavy manifest paths, and whether to show worktree locations relative to the repository root.

## Building from source

**Prerequisites:** JDK 21 and Git on your `PATH`. The Gradle wrapper is included; no other tooling is required.

```bash
git clone https://github.com/findyourexit/intellij-git-worktree-tools.git
cd intellij-git-worktree-tools
./gradlew build
```

The plugin ZIP is written to `build/distributions/`.

```bash
./gradlew runIde          # launch a sandboxed IDE with the plugin installed
./gradlew test            # run the test suite
./gradlew ktlintCheck detekt   # run the style and static-analysis gates
./gradlew verifyPlugin    # run the JetBrains Plugin Verifier
```

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the build, test, and submission workflow, and [CHANGELOG.md](CHANGELOG.md) for release notes.

## Security

To report a vulnerability, follow the process in [SECURITY.md](SECURITY.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
