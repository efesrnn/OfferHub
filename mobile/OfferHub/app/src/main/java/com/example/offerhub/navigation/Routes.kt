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
    const val EXPERT_CASES = "expert_cases"
    const val EXPERT_CRITICAL_CASES = "expert_critical_cases"
    const val EXPERT_PROFILE = "expert_profile"
    const val EXPERT_OPERATIONS = "expert_operations"
    const val EXPERT_CAMPAIGNS = "expert_campaigns"
    const val EXPERT_CREATE_CAMPAIGN = "expert_create_campaign"
    const val EXPERT_CAMPAIGN_DETAIL = "expert_campaign_detail"
    const val EXPERT_CAMPAIGN_DETAIL_WITH_NO = "$EXPERT_CAMPAIGN_DETAIL/{campaignNo}"
    const val EXPERT_CASE_DETAIL = "expert_case_detail"
    const val EXPERT_CASE_DETAIL_WITH_ID = "$EXPERT_CASE_DETAIL/{caseId}"
    const val SUPERVISOR_HOME = "supervisor_home"
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_CREATE_STAFF = "admin_create_staff"
    const val ADMIN_UPDATE_ROLE = "admin_update_role"
    const val ADMIN_SEARCH_STAFF = "admin_search_staff"
    const val ADMIN_AUDIT_LOGS = "admin_audit_logs"
    const val ADMIN_PROFILE = "admin_profile"
    const val OFFERS = "offers"
    const val PROFILE ="subscriber_profile"
    const val OFFER_CATEGORY = "offer_category"
    const val OFFER_CATEGORY_WITH_TYPE =
        "offer_category/{offerType}"

    fun offerCategory(offerType: String): String {
        return "$OFFER_CATEGORY/$offerType"
    }

    fun expertCaseDetail(caseId: String): String = "$EXPERT_CASE_DETAIL/$caseId"
    fun expertCampaignDetail(campaignNo: String): String = "$EXPERT_CAMPAIGN_DETAIL/$campaignNo"
    const val ACCEPTED_OFFERS = "accepted_offers"
    const val RATED_OFFERS = "rated_offers"

}
