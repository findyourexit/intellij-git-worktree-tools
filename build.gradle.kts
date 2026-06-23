import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform")
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "dev.gitworktreetools"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

ktlint {
    android.set(false)
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    parallel = true
    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
    )
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea("2025.2.6.2")
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "dev.gitworktreetools.intellij"
        name = "Git Worktree Tools"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "252"
        }

        vendor {
            name = "Git Worktree Tools"
            url = "https://github.com/findyourexit/intellij-git-worktree-tools"
        }

        description =
            """
            Git Worktree Tools makes Git worktrees first-class inside JetBrains IDEs: create, list, open, remove, lock, prune, repair, move, and carry over project setup before first open.
            """.trimIndent()

        changeNotes =
            """
            Initial release: a Worktrees tool window, Git menu and Project View actions for create, open, remove, lock, move, prune, and repair, multiple open modes, and carry-over of project setup into a new worktree before its first open.
            """.trimIndent()
    }

    pluginVerification {
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.2.6.2")
            create(IntelliJPlatformType.AndroidStudio, "2025.2.3.9")
        }
        failureLevel.set(
            listOf(
                VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            ),
        )
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.check {
    dependsOn("ktlintCheck", "detekt")
}
