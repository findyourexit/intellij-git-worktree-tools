package dev.gitworktreetools.carry

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorktreeCarryOverServiceTest {
    @Test
    fun curatedCarryOverCopiesIdeaAndManifestButRejectsSecrets() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve(".idea/runConfigurations").createDirectories()
        source.resolve(".idea/codeStyles").createDirectories()
        source.resolve(".idea/runConfigurations/App.xml").writeText("<configuration name=\"App\" />")
        source.resolve(".idea/codeStyles/Project.xml").writeText("<code_scheme />")
        source.resolve(".env").writeText("SECRET=1")
        source.resolve("settings.local.json").writeText("{}")
        source.resolve("justfile").writeText("test:\n")
        source.resolve(".worktree-copy").writeText("justfile\n.env\nsettings.local.json\n")

        val result = execute(source, target)

        assertTrue(target.resolve(".idea/runConfigurations/App.xml").exists())
        assertTrue(target.resolve(".idea/codeStyles/Project.xml").exists())
        assertTrue(target.resolve("justfile").exists())
        assertFalse(target.resolve(".env").exists())
        assertFalse(target.resolve("settings.local.json").exists())
        assertTrue(result.messages.any { it.kind == CarryOverMessageKind.Rejected && it.relativePath == ".env" })
    }

    @Test
    fun existingIdeaSkipsAutomaticCarryOver() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve(".idea/runConfigurations").createDirectories()
        source.resolve(".idea/runConfigurations/App.xml").writeText("source")
        target.resolve(".idea/runConfigurations").createDirectories()
        target.resolve(".idea/runConfigurations/App.xml").writeText("target")

        val settings = EffectiveCarryOverSettings()
        val result =
            if (settings.runCarryOverOnlyWhenIdeaMissing && target.resolve(".idea").exists()) {
                CarryOverResult.skippedIdeaExists()
            } else {
                execute(source, target, settings)
            }

        assertTrue(result.skippedBecauseIdeaExists)
        assertEquals("target", target.resolve(".idea/runConfigurations/App.xml").readText())
    }

    @Test
    fun manifestRejectsEscapingAndAbsolutePaths() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve(".worktree-copy").writeText("../outside.txt\n/absolute/path\n")

        val result = execute(source, target)

        val rejected =
            result.messages
                .filter { it.kind == CarryOverMessageKind.Rejected }
                .map { it.relativePath }
                .toSet()
        assertTrue("../outside.txt" in rejected)
        assertTrue("/absolute/path" in rejected)
    }

    @Test
    fun symlinkEscapingSourceRootIsNotFollowed() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        val outside = createTempDirectory("gwt-outside")
        source.resolve("tooling").createDirectories()
        Files.createSymbolicLink(source.resolve("tooling/current"), outside)
        source.resolve(".worktree-copy").writeText("tooling/current\n")

        val result = execute(source, target)

        assertFalse(target.resolve("tooling/current").exists())
        assertTrue(result.messages.any { it.kind == CarryOverMessageKind.Skipped && it.relativePath == "tooling/current" })
    }

    @Test
    fun existingTargetFileIsNeverOverwritten() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve("justfile").writeText("source")
        source.resolve(".worktree-copy").writeText("justfile\n")
        target.resolve("justfile").writeText("target")

        val result = execute(source, target)

        assertEquals("target", target.resolve("justfile").readText())
        assertTrue(result.messages.any { it.message == "Skipped existing: justfile" })
    }

    @Test
    fun ideaHttpRequestsAreDeniedEvenDuringIdeaCopy() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve(".idea/httpRequests").createDirectories()
        source.resolve(".idea/httpRequests/private.http").writeText("GET http://example.test\nAuthorization: Bearer token")
        source.resolve(".idea/runConfigurations").createDirectories()
        source.resolve(".idea/runConfigurations/App.xml").writeText("<configuration />")

        val result = execute(source, target)

        assertTrue(target.resolve(".idea/runConfigurations/App.xml").exists())
        assertFalse(target.resolve(".idea/httpRequests/private.http").exists())
        assertTrue(result.messages.any { it.kind == CarryOverMessageKind.Rejected && it.relativePath == ".idea/httpRequests" })
    }

    @Test
    fun heavyManifestPathsRequireExplicitOptIn() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve("node_modules/tool").createDirectories()
        source.resolve("node_modules/tool/index.js").writeText("module.exports = 1")
        source.resolve(".worktree-copy").writeText("node_modules/tool/index.js\n")

        val denied = execute(source, target)
        assertFalse(target.resolve("node_modules/tool/index.js").exists())
        assertTrue(denied.messages.any { it.kind == CarryOverMessageKind.Rejected && it.relativePath == "node_modules/tool/index.js" })

        val allowedTarget = createTempDirectory("gwt-target-allowed")
        val allowed = execute(source, allowedTarget, EffectiveCarryOverSettings(allowHeavyManifestPaths = true))
        assertTrue(allowedTarget.resolve("node_modules/tool/index.js").exists())
        assertTrue(allowed.messages.any { it.kind == CarryOverMessageKind.Copied && it.relativePath == "node_modules/tool/index.js" })
    }

    private fun execute(
        source: java.nio.file.Path,
        target: java.nio.file.Path,
        settings: EffectiveCarryOverSettings = EffectiveCarryOverSettings(),
    ): CarryOverResult {
        val plan = CarryOverPlanner(settings).buildPlan(source, target)
        return CarryOverExecutor(settings.allowHeavyManifestPaths).execute(plan)
    }

    @Test
    fun allIgnoredScopeCopiesIgnoredFilesMinusDenylist() {
        val source = createTempDirectory("gwt-source")
        val target = createTempDirectory("gwt-target")
        source.resolve("dev-tools.json").writeText("{}")
        source.resolve(".env").writeText("SECRET=1")
        source.resolve("node_modules/pkg").createDirectories()
        source.resolve("node_modules/pkg/index.js").writeText("1")

        val settings =
            EffectiveCarryOverSettings(
                carryOverScope = CarryOverScope.AllIgnoredMinusDenylist,
                copyIdeaDirectory = false,
            )
        val ignored = listOf(Path.of("dev-tools.json"), Path.of(".env"), Path.of("node_modules"))
        val plan = CarryOverPlanner(settings).buildPlan(source, target, ignored)
        val result = CarryOverExecutor(settings.allowHeavyManifestPaths).execute(plan)

        assertTrue(target.resolve("dev-tools.json").exists())
        assertFalse(target.resolve(".env").exists())
        assertFalse(target.resolve("node_modules").exists())
        assertTrue(result.messages.any { it.kind == CarryOverMessageKind.Rejected && it.relativePath == ".env" })
        assertTrue(result.messages.any { it.kind == CarryOverMessageKind.Rejected && it.relativePath == "node_modules" })
    }
}
