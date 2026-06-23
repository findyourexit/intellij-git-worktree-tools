import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.tomlarcher.gitarborist"
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
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea(libs.versions.intellijIdea.get())
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "dev.tomlarcher.gitarborist"
        name = "Git Arborist"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "252"
        }

        vendor {
            name = "Tom Larcher"
            url = "https://github.com/findyourexit/intellij-git-arborist"
        }

        description =
            """
            Git Arborist makes Git worktrees first-class inside JetBrains IDEs: create, list, open, remove, lock, prune, repair, move, and carry over project setup before first open.
            """.trimIndent()

        changeNotes =
            """
            Initial release: a Worktrees tool window, Git menu and Project View actions for create, open, remove, lock, move, prune, and repair, multiple open modes, and carry-over of project setup into a new worktree before its first open.
            """.trimIndent()
    }

    pluginVerification {
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, libs.versions.intellijIdea.get())
            create(IntelliJPlatformType.AndroidStudio, libs.versions.androidStudio.get())
        }
        failureLevel.set(
            listOf(
                VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            ),
        )
    }

    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
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
