package com.example.offerhub.repository

import com.example.offerhub.data.model.gamification.ExpertLevel
import com.example.offerhub.data.model.gamification.RankingPeriod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGamificationRepositoryTest {
    @Test
    fun `profile contains progress metrics and badges`() = runBlocking {
        val repository = MockGamificationRepository()

        val result = repository.getProfile("debug-expert") as GamificationResult.Success

        assertEquals(ExpertLevel.GOLD, result.value.level)
        assertTrue(result.value.totalPoints > 0)
        assertTrue(result.value.badges.isNotEmpty())
    }

    @Test
    fun `daily ranking contains and identifies current expert`() = runBlocking {
        val repository = MockGamificationRepository()

        val result = repository.getRanking(
            RankingPeriod.DAILY,
            "debug-expert"
        ) as GamificationResult.Success

        assertTrue(result.value.any { it.expertId == "debug-expert" })
        assertEquals(10, result.value.size)
        assertEquals((1..result.value.size).toList(), result.value.map { it.rank })
    }
}
