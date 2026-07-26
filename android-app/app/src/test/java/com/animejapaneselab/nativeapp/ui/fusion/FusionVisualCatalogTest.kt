package com.animejapaneselab.nativeapp.ui.fusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionVisualCatalogTest {
    @Test
    fun everyVisualKeyHasOneCatalogEntry() {
        val entries = FusionVisualCatalog.entries

        assertEquals(FusionVisualKey.entries.toSet(), entries.map { it.key }.toSet())
        assertEquals(entries.size, entries.distinctBy { it.key }.size)
    }

    @Test
    fun firstSliceKeepsEvidenceHashAndDistributionBoundary() {
        val metadata = FusionVisualCatalog.metadataFor(FusionVisualKey.SessionCompleteCelebration)

        assertEquals(FusionEvidenceLevel.Runtime, metadata.evidence)
        assertEquals(FusionVerificationLevel.Screenshot, metadata.verification)
        assertEquals(FusionProductionStatus.InternalOnly, metadata.productionStatus)
        assertEquals(FusionLicenseStatus.InternalReferenceOnly, metadata.licenseStatus)
        assertEquals(3, metadata.sha256.size)
        assertTrue(metadata.sha256.all { it.matches(Regex("[0-9A-F]{64}")) })
        assertFalse(metadata.evidenceRefs.isEmpty())
    }

    @Test
    fun everyWhitelistedAssetKeepsHashEvidenceAndDistributionBoundary() {
        FusionVisualCatalog.entries.forEach { metadata ->
            assertEquals(FusionProductionStatus.InternalOnly, metadata.productionStatus)
            assertEquals(FusionLicenseStatus.InternalReferenceOnly, metadata.licenseStatus)
            assertTrue(metadata.sha256.isNotEmpty())
            assertTrue(metadata.sha256.all { it.matches(Regex("[0-9A-F]{64}")) })
            assertTrue(metadata.evidenceRefs.isNotEmpty())
        }
    }

    @Test
    fun trainingPathResolversExposeOnlyTypedWhitelistedAssets() {
        val motion = AnimeLabFusionAssetResolver.resolve(FusionVisualKey.TrainingPathAmbientCompanion)
        val lockedCharacter = AnimeLabFusionDrawableResolver.resolveDrawable(
            FusionVisualKey.TrainingPathLockedCompanionSmores,
        )
        val lockedChest = AnimeLabFusionDrawableResolver.resolveDrawable(
            FusionVisualKey.TrainingPathLockedRewardChest,
        )

        assertTrue(motion is FusionVisualResolution.Available)
        assertTrue(lockedCharacter is FusionDrawableResolution.Available)
        assertTrue(lockedChest is FusionDrawableResolution.Available)
    }
}
