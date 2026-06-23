package dev.gitworktreetools.carry

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DenylistTest {
    @Test
    fun sensitivePatternsAlwaysDenySecrets() {
        val denied =
            listOf(
                ".env",
                ".env.production",
                "settings.local.json",
                "config/app.local",
                "secrets/api.txt",
                "credentials/store.json",
                "tokens/cache.json",
                "private/key.pem",
                ".idea/httpRequests/request.http",
            )

        denied.forEach { relativePath ->
            assertTrue(Denylist.isSensitive(Path.of(relativePath)), relativePath)
            assertFalse(Denylist.shouldCopy(Path.of(relativePath), allowHeavy = true), relativePath)
        }
    }

    @Test
    fun heavyPathsNeedOptInButAreNotSensitive() {
        val heavy = Path.of("node_modules/tool/index.js")

        assertFalse(Denylist.isSensitive(heavy))
        assertTrue(Denylist.isHeavy(heavy))
        assertFalse(Denylist.shouldCopy(heavy, allowHeavy = false))
        assertTrue(Denylist.shouldCopy(heavy, allowHeavy = true))
    }

    @Test
    fun normalManifestPathsAreAllowed() {
        val path = Path.of("justfile")

        assertFalse(Denylist.isSensitive(path))
        assertFalse(Denylist.isHeavy(path))
        assertTrue(Denylist.shouldCopy(path, allowHeavy = false))
    }

    @Test
    fun heavyDirectoriesAreDetectedAtAnyDepth() {
        assertTrue(Denylist.isHeavy(Path.of("frontend/node_modules")))
        assertTrue(Denylist.isHeavy(Path.of("frontend/node_modules/pkg/index.js")))
        assertTrue(Denylist.isHeavy(Path.of("a/b/target")))
        assertFalse(Denylist.isHeavy(Path.of("frontend/src/index.js")))
        assertFalse(Denylist.shouldCopy(Path.of("services/api/node_modules/x"), allowHeavy = false))
        assertTrue(Denylist.shouldCopy(Path.of("services/api/node_modules/x"), allowHeavy = true))
    }
}
