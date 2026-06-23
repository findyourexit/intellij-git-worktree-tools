# Security Policy

## Supported versions

Security fixes are applied to the latest released version and the current `main` branch.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Use GitHub's private vulnerability reporting instead:

1. Go to the **Security** tab of this repository.
2. Click **Report a vulnerability**.
3. Fill in the details: affected component, steps to reproduce, and the potential impact.

You will receive an acknowledgement within 5 business days. We aim to triage and respond to confirmed vulnerabilities within 14 days and to publish a fix or mitigation within 90 days of confirmation.

## Scope

This plugin runs inside JetBrains IDEs and operates on local filesystem paths. Relevant concerns include:

- Path traversal during carry-over copying that could write files outside the target worktree root
- Symlink following that escapes the source worktree root
- Exposure of sensitive files (`.env`, credentials, tokens) through carry-over despite the denylist
- Command injection via worktree paths passed to `git worktree` subcommands

Issues outside the scope of this plugin (e.g. IntelliJ Platform bugs, Git vulnerabilities) should be reported to the respective upstream projects.

## Disclosure policy

Confirmed vulnerabilities are disclosed publicly in the GitHub Security Advisories section of this repository after a fix is released or a reasonable remediation period has elapsed.
