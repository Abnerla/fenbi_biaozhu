package com.example.annotation.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionInfoTest {
    private val versionInfo = VersionInfo(
        latestVersionCode = 10,
        latestVersionName = "1.4.0",
        updateDesc = "Pressure support",
        downloadUrl = "https://github.com/Abnerla/fenbi_biaozhu/releases/download/v1.4.0/app-release.apk",
        forceUpdate = false,
        minSupportVersion = 1
    )

    @Test
    fun newerVersionReturnsOptionalUpdate() {
        val result = compareAppVersion(9, versionInfo) as UpdateCheckResult.HasUpdate

        assertFalse(result.isForceUpdate)
    }

    @Test
    fun minimumSupportedVersionAndForceFlagRequireUpdate() {
        val belowMinimum = compareAppVersion(
            8,
            versionInfo.copy(minSupportVersion = 9)
        ) as UpdateCheckResult.HasUpdate
        val explicitlyForced = compareAppVersion(
            9,
            versionInfo.copy(forceUpdate = true)
        ) as UpdateCheckResult.HasUpdate

        assertTrue(belowMinimum.isForceUpdate)
        assertTrue(explicitlyForced.isForceUpdate)
    }

    @Test
    fun sameOrNewerInstalledVersionNeverDowngrades() {
        assertTrue(compareAppVersion(10, versionInfo) is UpdateCheckResult.NoUpdate)
        assertTrue(compareAppVersion(11, versionInfo) is UpdateCheckResult.NoUpdate)
    }

    @Test
    fun manifestValidationRejectsUnsafeOrInconsistentValues() {
        assertTrue(versionInfo.isValid())
        assertFalse(versionInfo.copy(downloadUrl = "http://example.com/app.apk").isValid())
        assertFalse(versionInfo.copy(minSupportVersion = 11).isValid())
        assertFalse(versionInfo.copy(latestVersionName = "").isValid())
    }
}
