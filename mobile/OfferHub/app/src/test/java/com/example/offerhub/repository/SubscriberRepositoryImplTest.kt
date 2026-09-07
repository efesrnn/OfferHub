package com.example.offerhub.repository

import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType
import com.example.offerhub.data.remote.SubscriberApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SubscriberRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SubscriberRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SubscriberApi::class.java)

        repository = SubscriberRepositoryImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get offers maps backend response and sorts by score`() = runBlocking {
        val responseJson = """
            {
              "success": true,
              "data": [
                {
                  "offerId": "offer-low",
                  "campaignNo": "CMP-LOW",
                  "title": "Low Score Offer",
                  "description": null,
                  "discountRate": 10.0,
                  "validUntil": "2026-09-30T23:59:59Z",
                  "score": 0.40,
                  "highlighted": false,
                  "status": "PENDING",
                  "type": "SADAKAT",
                  "acceptedAt": null,
                  "rating": null
                },
                {
                  "offerId": "offer-high",
                  "campaignNo": "CMP-HIGH",
                  "title": "High Score Offer",
                  "description": "20 GB internet package",
                  "discountRate": 20.0,
                  "validUntil": "2026-10-30T23:59:59Z",
                  "score": 0.90,
                  "highlighted": true,
                  "status": "PENDING",
                  "type": "EK_PAKET",
                  "acceptedAt": null,
                  "rating": null
                }
              ],
              "error": null
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(
                    "Content-Type",
                    "application/json"
                )
                .setBody(responseJson)
        )

        val result = repository.getOffers()

        assertTrue(result is SubscriberResult.Success)

        val offers =
            (result as SubscriberResult.Success).value

        assertEquals(2, offers.size)

        val firstOffer = offers.first()

        assertEquals("offer-high", firstOffer.offerId)
        assertEquals("High Score Offer", firstOffer.title)
        assertEquals("", offers[1].description)
        assertEquals(OfferStatus.PENDING, firstOffer.status)
        assertEquals(OfferType.ADD_ON, firstOffer.type)
        assertEquals(0.90, firstOffer.score, 0.001)
    }
    @Test
    fun `rate offer sends rating and maps updated offer`() = runBlocking {
        val responseJson = """
        {
          "success": true,
          "data": {
            "offer": {
              "offerId": "offer-123",
              "campaignNo": "CMP-2026-000123",
              "title": "20 GB Internet",
              "description": "Internet package",
              "discountRate": 20.0,
              "validUntil": "2026-10-30T23:59:59Z",
              "score": 0.90,
              "highlighted": true,
              "status": "ACCEPTED",
              "type": "EK_PAKET",
              "acceptedAt": "2026-09-07T12:00:00Z",
              "rating": 4
            }
          },
          "error": null
        }
    """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(
                    "Content-Type",
                    "application/json"
                )
                .setBody(responseJson)
        )

        val result = repository.rateOffer(
            offerId = "offer-123",
            rating = 4
        )

        assertTrue(result is SubscriberResult.Success)

        val ratedOffer =
            (result as SubscriberResult.Success).value

        assertEquals("offer-123", ratedOffer.offerId)
        assertEquals(OfferStatus.ACCEPTED, ratedOffer.status)
        assertEquals(4, ratedOffer.rating)

        val recordedRequest = server.takeRequest()

        assertEquals("POST", recordedRequest.method)
        assertEquals(
            "/api/v1/subscribers/me/offers/offer-123/rating",
            recordedRequest.path
        )
        assertEquals(
            """{"rating":4}""",
            recordedRequest.body.readUtf8()
        )
    }
    @Test
    fun `accept offer preserves already responded backend error`() = runBlocking {
        val responseJson = """
        {
          "success": false,
          "data": null,
          "error": {
            "code": "OFFER_ALREADY_RESPONDED",
            "message": "Offer has already been answered"
          }
        }
    """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader(
                    "Content-Type",
                    "application/json"
                )
                .setBody(responseJson)
        )

        val result = repository.acceptOffer("offer-123")

        assertTrue(result is SubscriberResult.Failure)
        assertEquals(
            "OFFER_ALREADY_RESPONDED",
            (result as SubscriberResult.Failure).error.code
        )

        val recordedRequest = server.takeRequest()

        assertEquals(
            "POST",
            recordedRequest.method
        )
        assertEquals(
            "/api/v1/subscribers/me/offers/offer-123/accept",
            recordedRequest.path
        )
    }
}