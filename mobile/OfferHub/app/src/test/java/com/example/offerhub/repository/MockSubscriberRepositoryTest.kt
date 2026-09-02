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
        val repository = MockSubscriberRepository(acceptedUnratedOffers())

        val result = repository.rateOffer("f1a5", 5) as SubscriberResult.Success

        assertEquals(5, result.value.rating)
    }

    @Test
    fun `offer cannot be answered more than once`() = runBlocking {
        val repository = MockSubscriberRepository(MockOfferData.offers)
        repository.acceptOffer("f1a2")

        val secondResponse = repository.declineOffer("f1a2")

        assertTrue(secondResponse is SubscriberResult.Failure)
        assertEquals(
            "OFFER_ALREADY_RESPONDED",
            (secondResponse as SubscriberResult.Failure).error.code
        )
    }

    @Test
    fun `accepted offer can only be rated once`() = runBlocking {
        val repository = MockSubscriberRepository(acceptedUnratedOffers())
        repository.rateOffer("f1a5", 4)

        val secondRating = repository.rateOffer("f1a5", 5)

        assertTrue(secondRating is SubscriberResult.Failure)
        assertEquals(
            "OFFER_ALREADY_RATED",
            (secondRating as SubscriberResult.Failure).error.code
        )
    }

    private fun acceptedUnratedOffers() =
        MockOfferData.offers.map { offer ->
            if (offer.offerId == "f1a5") {
                offer.copy(rating = null)
            } else {
                offer
            }
        }
}
