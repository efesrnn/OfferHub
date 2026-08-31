package com.example.offerhub.viewModel

import com.example.offerhub.data.model.gamification.RankingPeriod
import com.example.offerhub.repository.MockGamificationRepository
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationViewModelTest {
    private fun createViewModel() = GamificationViewModel(
        repository = MockGamificationRepository(),
        dispatcher = Dispatchers.Unconfined
    )

    @Test
    fun `load publishes profile and daily top ten`() {
        val viewModel = createViewModel()

        viewModel.load("debug-expert")

        val state = viewModel.uiState.value
        assertEquals("debug-expert", state.expertId)
        assertNotNull(state.profile)
        assertEquals(RankingPeriod.DAILY, state.selectedPeriod)
        assertEquals(10, state.ranking.size)
        assertEquals(3, state.ranking.first { it.expertId == "debug-expert" }.rank)
        assertFalse(state.isLoading)
        assertFalse(state.isLoadingRanking)
    }

    @Test
    fun `selecting weekly replaces ranking and keeps current expert visible`() {
        val viewModel = createViewModel()
        viewModel.load("debug-expert")

        viewModel.selectPeriod(RankingPeriod.WEEKLY)

        val state = viewModel.uiState.value
        assertEquals(RankingPeriod.WEEKLY, state.selectedPeriod)
        assertEquals(10, state.ranking.size)
        assertEquals(7, state.ranking.first { it.expertId == "debug-expert" }.rank)
    }

    @Test
    fun `loading another expert replaces previous identity`() {
        val viewModel = createViewModel()
        viewModel.load("first-expert")

        viewModel.load("second-expert")

        val state = viewModel.uiState.value
        assertEquals("second-expert", state.expertId)
        assertEquals("second-expert", state.profile?.expertId)
        assertTrue(state.ranking.any { it.expertId == "second-expert" })
        assertFalse(state.ranking.any { it.expertId == "first-expert" })
    }
}
