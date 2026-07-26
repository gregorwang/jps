package com.animejapaneselab.nativeapp.ui.fusion

/**
 * Business and feature code refers to semantic roles only. Android resource ids, Lottie files,
 * Rive artboards, and renderer details stay behind [FusionAssetResolver].
 */
enum class FusionVisualKey {
    SessionCompleteCelebration,
    TodayHeroCompanion,
    LessonPromptCompanion,
    LessonAudioSpeaker,
    LessonAnswerCorrectIcon,
    LessonAnswerWrongIcon,
    TrainingPathAmbientCompanion,
    TrainingPathLockedCompanionSmores,
    TrainingPathLockedCompanionTennis,
    TrainingPathLockedRewardChest,
    TrainingPathGuidebookIcon,
}

enum class FusionEvidenceLevel {
    Runtime,
    Static,
    Candidate,
    Rejected,
}

enum class FusionVerificationLevel {
    Stress,
    Screenshot,
    Preview,
    Unverified,
}

enum class FusionProductionStatus {
    Approved,
    FeatureFlagged,
    InternalOnly,
    Rejected,
}

enum class FusionLicenseStatus {
    Cleared,
    InternalReferenceOnly,
    Blocked,
}

data class FusionVisualMetadata(
    val key: FusionVisualKey,
    val evidence: FusionEvidenceLevel,
    val verification: FusionVerificationLevel,
    val productionStatus: FusionProductionStatus,
    val licenseStatus: FusionLicenseStatus,
    val owner: String,
    val origin: String,
    val sha256: List<String>,
    val evidenceRefs: List<String>,
    val verifiedOn: String,
    val knownIssues: List<String> = emptyList(),
)

object FusionVisualCatalog {
    val entries: List<FusionVisualMetadata> = listOf(
        FusionVisualMetadata(
            key = FusionVisualKey.SessionCompleteCelebration,
            evidence = FusionEvidenceLevel.Runtime,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-completion",
            origin = "Mirror App session-complete visual pack",
            sha256 = listOf(
                "B87D39E381D22020D52C0E7CB7DC89CDEE631C588CBEA34515606FB0F48B469C",
                "38FCB00F36EB619267461CF6E40D199AEC6F5BD8C6D4EAE2A0B1A582C970E7F5",
                "32424BE4A68D8119A18680359C347B5BC8529912575AD75B9ECCAADC21DAD88F",
            ),
            evidenceRefs = listOf(
                "MIRROR-APP-FUSION-PLAYBOOK-20260710.zh-CN.md#phase-2",
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#session-完成",
                "verification-screenshots/emulator-5554-20260710-integrated-final/session_summary.png",
            ),
            verifiedOn = "2026-07-10 / emulator-5554 / API 36 / Lottie 6.7.1",
            knownIssues = listOf(
                "Internal personal-learning use only until distribution rights are cleared.",
                "The imported visual is a presentation layer; lesson scoring and navigation remain target-owned.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TrainingPathAmbientCompanion,
            evidence = FusionEvidenceLevel.Candidate,
            verification = FusionVerificationLevel.Preview,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-training-path",
            origin = "Mirror App Path active-character candidate pack",
            sha256 = listOf(
                "05F41C6E014341563A5311C2899D5A7524B8072019B41A463951950BB3B8DAD3",
            ),
            evidenceRefs = listOf(
                "MIRROR-APP-FUSION-PLAYBOOK-20260710.zh-CN.md#path",
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#path",
            ),
            verifiedOn = "2026-07-10 / byte-identical target copy / target screenshot pending",
            knownIssues = listOf(
                "The resource family is verified, but the original app's exact current-node assignment is not closed.",
                "Use only as a target-owned ambient companion behind the existing path state machine.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TrainingPathLockedCompanionSmores,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-training-path",
            origin = "Mirror App Path locked-character vector",
            sha256 = listOf(
                "074E74A423B21D6BA768014B3479C107BB2FAC368C25E31C140499B5A852CD71",
            ),
            evidenceRefs = listOf(
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#path",
                "verification-screenshots/emulator-5554-20260710-integrated-final/home_path.png",
            ),
            verifiedOn = "2026-07-10 / Mirror V2 screenshot / target integration",
            knownIssues = listOf(
                "Locked-state family is verified; the original runtime setter is not an exact attribution proof.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TrainingPathLockedCompanionTennis,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-training-path",
            origin = "Mirror App Path locked-character vector",
            sha256 = listOf(
                "BC22B583F6C4B88DE2ED225959C285C9F3A8E2AB5F8DD7FE156D91D55FF1F0E1",
            ),
            evidenceRefs = listOf(
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#path",
                "verification-screenshots/emulator-5554-20260710-integrated-final/home_path.png",
            ),
            verifiedOn = "2026-07-10 / Mirror V2 screenshot / target integration",
            knownIssues = listOf(
                "Locked-state family is verified; the original runtime setter is not an exact attribution proof.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TrainingPathLockedRewardChest,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-training-path",
            origin = "Mirror App common locked level chest vector",
            sha256 = listOf(
                "11831AFE79687A374D4D0F7A26127F9DCE6696A3C8BDC503745C153487FABB68",
            ),
            evidenceRefs = listOf(
                "MIRROR-APP-FUSION-PLAYBOOK-20260710.zh-CN.md#path",
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#path",
            ),
            verifiedOn = "2026-07-10 / Mirror V2 screenshot / target integration",
            knownIssues = listOf(
                "Only bind to a genuinely non-actionable locked reward node.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TrainingPathGuidebookIcon,
            evidence = FusionEvidenceLevel.Runtime,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-training-path",
            origin = "Mirror App Path guidebook vector",
            sha256 = listOf(
                "FAA1C9F98064E2BE375CFD5DDDC7083E82D949F01D97CAF3D3399A7BD591729B",
            ),
            evidenceRefs = listOf(
                "MIRROR-APP-FUSION-PLAYBOOK-20260710.zh-CN.md#path",
                "verification-screenshots/emulator-5554-20260710-integrated-final/home_path.png",
            ),
            verifiedOn = "2026-07-10 / Mirror E3-V2 / target integration",
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.TodayHeroCompanion,
            evidence = FusionEvidenceLevel.Candidate,
            verification = FusionVerificationLevel.Preview,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-today",
            origin = "Mirror App Path character reused as a target-owned daily learning companion",
            sha256 = listOf(
                "05F41C6E014341563A5311C2899D5A7524B8072019B41A463951950BB3B8DAD3",
            ),
            evidenceRefs = listOf(
                "ASSET-INTEGRATION-REPORT-20260710.zh-CN.md#path",
            ),
            verifiedOn = "2026-07-10 / local internal integration",
            knownIssues = listOf(
                "Decorative only; daily plan data and CTA behavior remain target-owned.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.LessonPromptCompanion,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Preview,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-lesson",
            origin = "Mirror App in-lesson companion animation",
            sha256 = listOf(
                "BBA13361166E9B05E0BAE07FA9330F8B0DE62B41E80BA7E55D3D77187B1A4261",
            ),
            evidenceRefs = listOf(
                "verification-screenshots/emulator-5554-20260710-listen-final/listen-correct2.png",
            ),
            verifiedOn = "2026-07-10 / local internal integration",
            knownIssues = listOf(
                "Presentation only; it does not grade answers or select feedback copy.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.LessonAudioSpeaker,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-lesson-audio",
            origin = "Mirror App normal-speed speaker Lottie",
            sha256 = listOf(
                "AE4E053944863478106FF08F5EC1F8BA147A9DF3872816A3E6C7AEF83430EFB9",
            ),
            evidenceRefs = listOf(
                "verification-screenshots/emulator-5554-20260710-listen-final/listen-correct2.png",
            ),
            verifiedOn = "2026-07-10 / Mirror E2-V2 / local internal integration",
            knownIssues = listOf(
                "Speaker animation only; audio source selection and playback remain target-owned.",
            ),
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.LessonAnswerCorrectIcon,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-lesson-feedback",
            origin = "Mirror App correct radio feedback vector",
            sha256 = listOf(
                "C3BDCD4E8B946FE8A37121F8BEC2EFDC256997EAE4D484B8E255B3EF794E69AF",
            ),
            evidenceRefs = listOf(
                "verification-screenshots/emulator-5554-20260710-select-static/correct-submitted.png",
            ),
            verifiedOn = "2026-07-10 / Mirror V2 / local internal integration",
        ),
        FusionVisualMetadata(
            key = FusionVisualKey.LessonAnswerWrongIcon,
            evidence = FusionEvidenceLevel.Static,
            verification = FusionVerificationLevel.Screenshot,
            productionStatus = FusionProductionStatus.InternalOnly,
            licenseStatus = FusionLicenseStatus.InternalReferenceOnly,
            owner = "ui-lesson-feedback",
            origin = "Mirror App incorrect radio feedback vector",
            sha256 = listOf(
                "9C1EA9BBE0BE8311415F65A8C4340993076726FE078044569F95548444E7B36C",
            ),
            evidenceRefs = listOf(
                "verification-screenshots/emulator-5554-20260710-select-static/incorrect-submitted.png",
            ),
            verifiedOn = "2026-07-10 / Mirror V2 / local internal integration",
        ),
    )

    fun metadataFor(key: FusionVisualKey): FusionVisualMetadata {
        return entries.first { it.key == key }
    }
}
