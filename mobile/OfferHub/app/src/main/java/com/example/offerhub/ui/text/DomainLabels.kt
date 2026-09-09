package com.example.offerhub.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.offerhub.R
import com.example.offerhub.data.model.campaign.*

@Composable fun Priority.localizedLabel() = stringResource(when (this) {
    Priority.KRITIK -> R.string.expert_priority_critical
    Priority.YUKSEK -> R.string.expert_priority_high
    Priority.ORTA -> R.string.expert_priority_medium
    Priority.DUSUK -> R.string.expert_priority_low
    Priority.UNKNOWN -> R.string.common_not_available
})

@Composable fun Segment.localizedLabel() = stringResource(when (this) {
    Segment.YUKSEK_DEGER -> R.string.supervisor_segment_high_value
    Segment.RISKLI_KAYIP -> R.string.supervisor_segment_churn_risk
    Segment.YENI_ABONE -> R.string.supervisor_segment_new_subscriber
    Segment.PASIF -> R.string.supervisor_segment_inactive
    Segment.BELIRSIZ -> R.string.supervisor_segment_uncertain
    Segment.UNKNOWN -> R.string.common_not_available
})

@Composable fun CaseStatus.localizedLabel() = stringResource(when (this) {
    CaseStatus.YENI -> R.string.supervisor_status_new
    CaseStatus.ATANDI -> R.string.supervisor_status_assigned
    CaseStatus.OPTIMIZE_EDILIYOR -> R.string.supervisor_status_optimizing
    CaseStatus.TEST_EDILIYOR -> R.string.supervisor_status_testing
    CaseStatus.TAMAMLANDI -> R.string.supervisor_status_completed
    CaseStatus.YAYINDA -> R.string.supervisor_status_published
    CaseStatus.ARSIVLENDI -> R.string.supervisor_status_archived
    CaseStatus.UNKNOWN -> R.string.common_not_available
})

@Composable fun CampaignStatus.localizedLabel() = stringResource(when (this) {
    CampaignStatus.YENI -> R.string.supervisor_status_new
    CampaignStatus.YAYINDA -> R.string.supervisor_status_published
    CampaignStatus.ARSIVLENDI -> R.string.supervisor_status_archived
    CampaignStatus.UNKNOWN -> R.string.common_not_available
})

@Composable fun CampaignType.localizedLabel() = stringResource(when (this) {
    CampaignType.EK_PAKET -> R.string.offer_type_add_on
    CampaignType.TARIFE_YUKSELTME -> R.string.offer_type_tariff
    CampaignType.CIHAZ_FIRSATI -> R.string.offer_type_device
    CampaignType.SADAKAT -> R.string.offer_type_loyalty
    CampaignType.UNKNOWN -> R.string.common_not_available
})

@Composable fun adminCodeLabel(code: String): String = when (code) {
    "ADMIN" -> stringResource(R.string.admin_role_admin)
    "EXPERT" -> stringResource(R.string.admin_role_expert)
    "SUPERVISOR" -> stringResource(R.string.admin_role_supervisor)
    "CHURN_ONLEME" -> stringResource(R.string.admin_specialty_churn_prevention)
    "YUKSEK_DEGER" -> stringResource(R.string.admin_specialty_high_value)
    "ISTANBUL" -> stringResource(R.string.admin_region_istanbul)
    "ANKARA" -> stringResource(R.string.admin_region_ankara)
    "IZMIR" -> stringResource(R.string.admin_region_izmir)
    else -> code
}
