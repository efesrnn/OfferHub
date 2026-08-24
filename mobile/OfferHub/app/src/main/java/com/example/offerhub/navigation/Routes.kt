package com.example.offerhub.navigation

object Routes{
    const val SPLASH="splash"
    const val AUTH_CHOICE="auth_choice"
    const val STAFF_LOGIN="staff_login"
    const val SUBSCRIBER_LOGIN="subscriber_login"
    const val SUBSCRIBER_REGISTER="subscriber_register"
    const val OTP_VERIFICATION="otp_verification"
    const val OTP_VERIFICATION_WITH_PHONE="otp_verification/{phoneNumber}"
    const val SUBSCRIBER_HOME="subscriber_home"
    const val EXPERT_HOME = "expert_home"
    const val SUPERVISOR_HOME = "supervisor_home"
    const val ADMIN_HOME = "admin_home"
    const val OFFERS = "offers"
    const val PROFILE ="subscriber_profile"
    const val OFFER_CATEGORY = "offer_category"
    const val OFFER_CATEGORY_WITH_TYPE =
        "offer_category/{offerType}"

    fun offerCategory(offerType: String): String {
        return "$OFFER_CATEGORY/$offerType"
    }
    const val ACCEPTED_OFFERS = "accepted_offers"
    const val RATED_OFFERS = "rated_offers"

}
