package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Segment
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    createdCampaignNo: String?,
    onBackClick: () -> Unit,
    onCreate: (String, CampaignType, Segment, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<CampaignType?>(null) }
    var segment by remember { mutableStateOf<Segment?>(null) }
    var discountText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    val normalizedTitle = title.trim()
    val discount = discountText.toIntOrNull()
    val titleValid = normalizedTitle.isNotEmpty() && normalizedTitle.length <= 200 && '<' !in title && '>' !in title
    val dateValid = selectedDate?.isAfter(LocalDate.now()) == true
    val formValid = titleValid && type != null && segment != null && discount in 0..100 && dateValid

    Scaffold(topBar = { OfferHubDetailTopBar(stringResource(R.string.expert_create_campaign), onBackClick) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 200) title = it },
                label = { Text(stringResource(R.string.expert_campaign_title)) },
                isError = submitAttempted && !titleValid,
                supportingText = {
                    if (submitAttempted && !titleValid) Text(stringResource(R.string.error_campaign_title))
                    else Text("${title.length}/200")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.expert_campaign_type))
            CampaignType.entries.filterNot { it == CampaignType.UNKNOWN }.chunked(2).forEach { values ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.forEach { value ->
                        FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value.name.displayName()) })
                    }
                }
            }
            if (submitAttempted && type == null) Text(stringResource(R.string.error_campaign_type), color = MaterialTheme.colorScheme.error)

            Text(stringResource(R.string.expert_target_segment))
            Segment.entries.filterNot { it == Segment.UNKNOWN }.chunked(3).forEach { values ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.forEach { value ->
                        FilterChip(selected = segment == value, onClick = { segment = value }, label = { Text(value.name.displayName()) })
                    }
                }
            }
            if (submitAttempted && segment == null) Text(stringResource(R.string.error_campaign_segment), color = MaterialTheme.colorScheme.error)

            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.expert_discount_rate)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = submitAttempted && discount !in 0..100,
                supportingText = { if (submitAttempted && discount !in 0..100) Text(stringResource(R.string.error_campaign_discount)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault()))
                        ?: stringResource(R.string.expert_select_valid_until)
                )
            }
            if (submitAttempted && !dateValid) {
                Text(stringResource(R.string.error_campaign_date), color = MaterialTheme.colorScheme.error)
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            createdCampaignNo?.let {
                Text(stringResource(R.string.expert_campaign_created, it), color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = {
                    submitAttempted = true
                    if (formValid) {
                        onCreate(normalizedTitle, type!!, segment!!, discount!!, "${selectedDate}T23:59:59Z")
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (isSubmitting) R.string.expert_creating_campaign else R.string.expert_create_campaign))
            }
        }
    }

    if (showDatePicker) {
        val todayStartUtc = remember {
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > todayStartUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text(stringResource(R.string.admin_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.admin_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun String.displayName(): String = lowercase().split('_').joinToString(" ") {
    it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
}
