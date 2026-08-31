package com.example.offerhub.repository

import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.data.model.OfferStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSubscriberRepositoryTest {
    @Test
    fun `offers are sorted by recommendation score`() = runBlocking {
        val repository = MockSubscriberRepository(MockOfferData.offers)

        val result = repository.getOffers() as SubscriberResult.Success

        assertEquals(
            result.value.sortedByDescending { it.score },
            result.value
        )
    }

    @Test
    fun `accepting an offer stores accepted status and timestamp`() = runBlocking {
        val repository = MockSubscriberRepository(MockOfferData.offers)

        val result = repository.acceptOffer("f1a2") as SubscriberResult.Success

        assertEquals(OfferStatus.ACCEPTED, result.value.status)
        assertNotNull(result.value.acceptedAt)
    }

    @Test
    fun `pending offer cannot be rated`() = runBlocking {
        val repository = MockSubscriberRepository(MockOfferData.offers)

        val result = repository.rateOffer("f1a2", 5)

        assertTrue(result is SubscriberResult.Failure)
        assertEquals(
            "OFFER_NOT_ACCEPTED",
            (result as SubscriberResult.Failure).error.code
        )
    }

    @Test
    fun `accepted offer can be rated from one to five`() = runBlocking {
        val repository = MockSubscriberRepository(MockOfferData.offers)

        val result = repository.rateOffer("f1a5", 5) as SubscriberResult.Success

        assertEquals(5, result.value.rating)
    }
}
