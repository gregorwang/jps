package com.animejapaneselab.nativeapp.ui.screens

import com.animejapaneselab.nativeapp.data.EpisodePlan
import com.animejapaneselab.nativeapp.data.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPathModelsTest {
    @Test
    fun kOnEpisodeOneBuildsDuolingoLikePathNodes() {
        val plan = buildTrainingPathPlan(
            baseInput(
                episodePlan = EpisodePlan(
                    id = "k-on-ep01-plan",
                    workSlug = "k-on",
                    episode = 1,
                    vocabCount = 20,
                    handwritingCount = 10,
                    shadowingCount = 5,
                    grammarCount = 5,
                    exerciseCount = 17,
                    vocabItemIds = emptyList(),
                    handwritingVocabIds = emptyList(),
                    notes = "第1集学习计划：高频词20、手写10、跟读5、语法5",
                ),
            ),
        )

        assertEquals("K-ON! EP01", plan.title)
        assertEquals("mixed-1", plan.nodes.first().key)
        assertEquals(1, plan.nodes.count { it.action == TrainingPathNodeAction.Vocab })
        assertEquals(3, plan.nodes.count { it.action == TrainingPathNodeAction.Grammar })
        assertEquals(5, plan.nodes.count { it.action == TrainingPathNodeAction.Shadowing })
        assertEquals(2, plan.nodes.count { it.action == TrainingPathNodeAction.ReadAir })
        assertEquals(2, plan.nodes.count { it.action == TrainingPathNodeAction.Mixed })
        assertEquals("0/20", plan.nodes.first { it.key == "vocab-1" }.countLabel)
        assertEquals("0/6", plan.nodes.first { it.key == "grammar-1" }.countLabel)
        assertEquals("0/6", plan.nodes.first { it.key == "shadowing-1" }.countLabel)
        assertEquals("0/7", plan.nodes.first { it.key == "read-air-1" }.countLabel)
        assertEquals("0/4", plan.nodes.first { it.key == "read-air-2" }.countLabel)
        assertTrue(plan.planNote.contains("完整词库 20"))
    }

    @Test
    fun reZeroEpisodeOneSplitsLargePoolsIntoMultipleUnits() {
        val plan = buildTrainingPathPlan(
            baseInput(
                workTitle = "Re:Zero",
                episodeLabel = "Re:Zero EP01",
                vocabCount = 120,
                grammarCount = 25,
                shadowingCount = 25,
                readAirCount = 14,
                episodePlan = EpisodePlan(
                    id = "re-zero-ep01-plan",
                    workSlug = "re-zero",
                    episode = 1,
                    vocabCount = 40,
                    handwritingCount = 26,
                    shadowingCount = 25,
                    grammarCount = 25,
                    exerciseCount = 40,
                    vocabItemIds = emptyList(),
                    handwritingVocabIds = emptyList(),
                    notes = "第1集学习计划：词汇40、语法25、跟读25、练习40",
                ),
            ),
        )

        assertEquals(
            listOf("vocab-1", "vocab-2", "vocab-3", "vocab-4", "vocab-5", "vocab-6"),
            plan.nodes.filter { it.action == TrainingPathNodeAction.Vocab }.map { it.key },
        )
        assertEquals((1..6).toList(), plan.nodes.filter { it.action == TrainingPathNodeAction.Vocab }.map { it.batch })
        assertEquals(120, plan.fullVocabCount)
        assertEquals(40, plan.coreVocabCount)
        assertEquals("核心 1–20", plan.nodes.first { it.key == "vocab-1" }.scopeLabel)
        assertEquals("扩展 41–60", plan.nodes.first { it.key == "vocab-3" }.scopeLabel)
        assertEquals(5, plan.nodes.count { it.action == TrainingPathNodeAction.Grammar })
        assertEquals(5, plan.nodes.count { it.action == TrainingPathNodeAction.Shadowing })
        assertEquals(2, plan.nodes.count { it.action == TrainingPathNodeAction.ReadAir })
        assertTrue(plan.nodes.any { it.state == TrainingPathNodeState.Reward })
    }

    @Test
    fun reZeroEpisodeTwentySixUsesMergedPlanCounts() {
        val plan = buildTrainingPathPlan(
            baseInput(
                workTitle = "Re:Zero",
                episodeLabel = "Re:Zero EP26",
                vocabCount = 0,
                grammarCount = 26,
                shadowingCount = 33,
                readAirCount = 1,
                episodePlan = EpisodePlan(
                    id = "re-zero-ep26-plan-merged",
                    workSlug = "re-zero",
                    episode = 26,
                    vocabCount = 54,
                    handwritingCount = 0,
                    shadowingCount = 15,
                    grammarCount = 26,
                    exerciseCount = 149,
                    vocabItemIds = (1..54).map { "vocab-$it" },
                    handwritingVocabIds = emptyList(),
                    shadowingSentenceIds = (1..15).map { "sentence-$it" },
                    grammarPointIds = (1..26).map { "grammar-$it" },
                    exerciseIds = (1..149).map { "exercise-$it" },
                    notes = "复习学习：覆盖本集全部词汇、语法和推荐跟读理解题。",
                ),
            ),
        )

        assertEquals(3, plan.nodes.count { it.action == TrainingPathNodeAction.Vocab })
        assertEquals("0/14", plan.nodes.first { it.key == "vocab-3" }.countLabel)
        assertEquals(5, plan.nodes.count { it.action == TrainingPathNodeAction.Grammar })
        assertEquals(6, plan.nodes.count { it.action == TrainingPathNodeAction.Shadowing })
        assertEquals(1, plan.nodes.count { it.action == TrainingPathNodeAction.ReadAir })
        assertEquals(15, plan.nodes.count { it.action == TrainingPathNodeAction.Mixed })
        assertEquals(4, plan.nodes.first { it.key == "mixed-15" }.totalCount)
    }

    @Test
    fun reviewNodeUsesDueItemsAndLocalMistakes() {
        val plan = buildTrainingPathPlan(
            baseInput(reviewDueCount = 2, localMistakeCount = 1),
        )

        val reviewNode = plan.nodes.first { it.key == "review-due" }
        assertEquals(TrainingPathNodeState.ReviewDue, reviewNode.state)
        assertEquals(TrainingPathNodeAction.Review, reviewNode.action)
        assertEquals("3 项", reviewNode.countLabel)
    }

    @Test
    fun weakProgressAlsoUnlocksReviewNode() {
        val plan = buildTrainingPathPlan(
            baseInput(
                progressItems = listOf(
                    TrainingPathProgressItem("vocab", ReviewState.Bad),
                    TrainingPathProgressItem("grammar", ReviewState.Fuzzy),
                    TrainingPathProgressItem("sentence", ReviewState.Unknown),
                    TrainingPathProgressItem("exercise", ReviewState.Ok),
                ),
            ),
        )

        val reviewNode = plan.nodes.first { it.key == "review-due" }
        assertEquals(TrainingPathNodeState.ReviewDue, reviewNode.state)
        assertEquals(TrainingPathNodeAction.Review, reviewNode.action)
    }

    @Test
    fun unstartedPathOnlyHighlightsFirstIncompleteNode() {
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = 2,
                grammarCount = 1,
                shadowingCount = 1,
                readAirCount = 1,
                lessonNodeCount = 1,
            ),
        )

        assertEquals(TrainingPathNodeState.Current, plan.nodes.first { it.key == "mixed-1" }.state)
        assertEquals(TrainingPathNodeState.Locked, plan.nodes.first { it.key == "vocab-1" }.state)
        assertEquals(TrainingPathNodeState.Locked, plan.nodes.first { it.key == "grammar-1" }.state)
    }

    @Test
    fun completedEarlierNodesUnlockNextIncompleteNode() {
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = 2,
                grammarCount = 1,
                shadowingCount = 1,
                readAirCount = 1,
                lessonNodeCount = 1,
                lessonAnswered = 1,
                progressItems = listOf(
                    TrainingPathProgressItem("vocab", ReviewState.Good, itemId = "vocab-1"),
                    TrainingPathProgressItem("vocab", ReviewState.Good, itemId = "vocab-2"),
                ),
            ),
        )

        assertEquals(TrainingPathNodeState.Current, plan.nodes.first { it.key == "mixed-1" }.state)
        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "vocab-1" }.state)
        assertEquals(TrainingPathNodeState.Locked, plan.nodes.first { it.key == "grammar-1" }.state)
        assertEquals(TrainingPathNodeState.Locked, plan.nodes.first { it.key == "shadowing-1" }.state)
    }

    @Test
    fun goodAndKnownProgressMarkNodesCompleted() {
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = 2,
                grammarCount = 1,
                shadowingCount = 1,
                readAirCount = 1,
                lessonNodeCount = 1,
                lessonAnswered = 1,
                progressItems = listOf(
                    TrainingPathProgressItem("vocab", ReviewState.Good, itemId = "vocab-1"),
                    TrainingPathProgressItem("vocab", ReviewState.Known, itemId = "vocab-2"),
                    TrainingPathProgressItem("grammar", ReviewState.Good, itemId = "grammar-1"),
                    TrainingPathProgressItem("sentence", ReviewState.Known, itemId = "sentence-1"),
                    TrainingPathProgressItem("exercise", ReviewState.Good, itemId = "exercise-1"),
                ),
            ),
        )

        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "mixed-1" }.state)
        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "vocab-1" }.state)
        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "grammar-1" }.state)
        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "shadowing-1" }.state)
        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "read-air-1" }.state)
    }

    @Test
    fun rewardNodesRemainNonActionableWhenVisualsChange() {
        val plan = buildTrainingPathPlan(baseInput())

        val rewardNodes = plan.nodes.filter { it.state == TrainingPathNodeState.Reward }

        assertTrue(rewardNodes.isNotEmpty())
        assertTrue(rewardNodes.all { it.action == TrainingPathNodeAction.None })
    }

    @Test
    fun pathNeverExposesMoreThanOneCurrentNode() {
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = 80,
                grammarCount = 30,
                shadowingCount = 30,
                readAirCount = 21,
                lessonNodeCount = 40,
            ),
        )

        assertTrue(plan.nodes.count { it.state == TrainingPathNodeState.Current } <= 1)
    }

    @Test
    fun vocabUnitsUseTheirOwnMaterialIdsForCompletion() {
        val vocabIds = (1..45).map { "vocab-$it" }
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = vocabIds.size,
                vocabIds = vocabIds,
                progressItems = (1..20).map { index ->
                    TrainingPathProgressItem(
                        itemType = "vocab",
                        state = ReviewState.Good,
                        itemId = "vocab-$index-study",
                        payload = mapOf("sourceId" to "vocab-$index"),
                        lastReviewedAt = "2026-07-11T00:00:${index.toString().padStart(2, '0')}Z",
                    )
                },
            ),
        )

        val vocabOne = plan.nodes.first { it.key == "vocab-1" }
        val vocabTwo = plan.nodes.first { it.key == "vocab-2" }
        val vocabThree = plan.nodes.first { it.key == "vocab-3" }
        assertEquals(TrainingPathNodeState.Completed, vocabOne.state)
        assertEquals(20, vocabOne.completedCount)
        assertEquals(0, vocabTwo.completedCount)
        assertEquals(0, vocabThree.completedCount)
    }

    @Test
    fun commaSeparatedPairProgressCountsEveryMaterialInThePair() {
        val plan = buildTrainingPathPlan(
            baseInput(
                vocabCount = 2,
                vocabIds = listOf("vocab-1", "vocab-2"),
                progressItems = listOf(
                    TrainingPathProgressItem(
                        itemType = "vocab",
                        state = ReviewState.Good,
                        itemId = "pair-node",
                        payload = mapOf("sourceId" to "vocab-1,vocab-2"),
                    ),
                ),
            ),
        )

        assertEquals(2, plan.nodes.first { it.key == "vocab-1" }.completedCount)
    }

    @Test
    fun pathMarkerCompletesOnlyItsOwnMixedNode() {
        val plan = buildTrainingPathPlan(
            baseInput(
                lessonNodeCount = 20,
                progressItems = listOf(
                    TrainingPathProgressItem(
                        itemType = "path_node",
                        state = ReviewState.Good,
                        itemId = "path-node:re-zero:1:mixed-1",
                        payload = mapOf("pathNodeKey" to "mixed-1"),
                    ),
                ),
            ),
        )

        assertEquals(TrainingPathNodeState.Completed, plan.nodes.first { it.key == "mixed-1" }.state)
        assertTrue(plan.nodes.first { it.key == "mixed-2" }.state != TrainingPathNodeState.Completed)
    }

    private fun baseInput(
        workTitle: String = "K-ON!",
        episodeLabel: String = "K-ON! EP01",
        vocabCount: Int = 20,
        grammarCount: Int = 15,
        shadowingCount: Int = 30,
        readAirCount: Int = 11,
        reviewDueCount: Int = 0,
        localMistakeCount: Int = 0,
        lessonNodeCount: Int = 10,
        lessonAnswered: Int = 0,
        progressItems: List<TrainingPathProgressItem> = emptyList(),
        episodePlan: EpisodePlan? = null,
        vocabIds: List<String> = emptyList(),
    ): TrainingPathInput {
        return TrainingPathInput(
            workTitle = workTitle,
            episodeLabel = episodeLabel,
            lessonTitle = "线上综合训练 · $episodeLabel",
            energy = 25,
            streakDays = 0,
            sessionXp = 0,
            lessonNodeCount = lessonNodeCount,
            lessonAnswered = lessonAnswered,
            lessonCorrect = 0,
            vocabCount = vocabCount,
            vocabIds = vocabIds,
            grammarCount = grammarCount,
            shadowingCount = shadowingCount,
            readAirCount = readAirCount,
            reviewDueCount = reviewDueCount,
            localMistakeCount = localMistakeCount,
            progressItems = progressItems,
            episodePlan = episodePlan,
        )
    }
}
