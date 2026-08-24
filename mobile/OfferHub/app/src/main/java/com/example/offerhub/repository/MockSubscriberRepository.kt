package com.example.offerhub.repository

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.network.ApiError
import java.time.Instant

class MockSubscriberRepository(
    initialOffers: List<Offer>
) : SubscriberRepository {
    private var offers = initialOffers

    override suspend fun getOffers(): SubscriberResult<List<Offer>> =
        SubscriberResult.Success(offers.sortedByDescending { it.score })

    override suspend fun getOfferDetail(offerId: String): SubscriberResult<Offer> =
        findOffer(offerId)

    override suspend fun acceptOffer(offerId: String): SubscriberResult<Offer> =
        updateOffer(offerId) {
            it.copy(
                status = OfferStatus.ACCEPTED,
                acceptedAt = Instant.now().toString()
            )
        }

    override suspend fun declineOffer(offerId: String): SubscriberResult<Offer> =
        updateOffer(offerId) { it.copy(status = OfferStatus.DECLINED) }

    override suspend fun rateOffer(
        offerId: String,
        rating: Int
    ): SubscriberResult<Offer> {
        if (rating !in 1..5) {
            return SubscriberResult.Failure(ApiError("INVALID_RATING"))
        }
        val offer = offers.firstOrNull { it.offerId == offerId }
            ?: return SubscriberResult.Failure(ApiError("OFFER_NOT_FOUND"))
        if (offer.status != OfferStatus.ACCEPTED) {
            return SubscriberResult.Failure(ApiError("OFFER_NOT_ACCEPTED"))
        }
        return updateOffer(offerId) { it.copy(rating = rating) }
    }

    private fun findOffer(offerId: String): SubscriberResult<Offer> {
        val offer = offers.firstOrNull { it.offerId == offerId }
            ?: return SubscriberResult.Failure(ApiError("OFFER_NOT_FOUND"))
        return SubscriberResult.Success(offer)
    }

    private fun updateOffer(
        offerId: String,
        transform: (Offer) -> Offer
    ): SubscriberResult<Offer> {
        val current = offers.firstOrNull { it.offerId == offerId }
            ?: return SubscriberResult.Failure(ApiError("OFFER_NOT_FOUND"))
        val updated = transform(current)
        offers = offers.map { if (it.offerId == offerId) updated else it }
        return SubscriberResult.Success(updated)
    }
}
