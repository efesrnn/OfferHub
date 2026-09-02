package com.example.offerhub.screens.admin

import android.content.ClipData
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.R
import com.example.offerhub.data.model.admin.AdminStaff
import kotlinx.coroutines.launch

@Composable
fun CreateStaffScreen(
    onBackClick: () -> Unit,
    onCreateStaff: (String, String, String, String, List<String>, List<String>) -> Unit,
    onClearClick: () -> Unit,
    isSubmitting: Boolean = false,
    successMessage: String? = null,
    createdStaffId: String? = null,
    createdStaffTempPassword: String? = null,
    errorMessage: String? = null
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("EXPERT") }
    var selectedSpecialties by remember { mutableStateOf(emptySet<String>()) }
    var selectedRegions by remember { mutableStateOf(emptySet<String>()) }
    var submitAttempted by remember { mutableStateOf(false) }

    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank() &&
        Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
        selectedSpecialties.isNotEmpty() && selectedRegions.isNotEmpty()

    fun clearForm() {
        firstName = ""
        lastName = ""
        email = ""
        role = "EXPERT"
        selectedSpecialties = emptySet()
        selectedRegions = emptySet()
        submitAttempted = false
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) clearForm()
    }

    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.admin_create_staff),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clearForm()
                            onClearClick()
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.admin_clear)) }
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            submitAttempted = true
                            if (isFormValid) {
                                onCreateStaff(firstName, lastName, email, role, selectedSpecialties.toList(), selectedRegions.toList())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isSubmitting) stringResource(R.string.admin_creating)
                            else stringResource(R.string.admin_create_account)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(R.string.admin_first_name)) },
                isError = submitAttempted && firstName.isBlank(),
                supportingText = {
                    if (submitAttempted && firstName.isBlank()) Text(stringResource(R.string.admin_error_first_name))
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.admin_last_name)) },
                isError = submitAttempted && lastName.isBlank(),
                supportingText = {
                    if (submitAttempted && lastName.isBlank()) Text(stringResource(R.string.admin_error_last_name))
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.admin_email)) },
                isError = submitAttempted && !Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                supportingText = {
                    if (submitAttempted && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Text(stringResource(R.string.admin_error_email))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.admin_role), fontWeight = FontWeight.SemiBold)
            ChoiceRow(listOf("EXPERT", "SUPERVISOR"), role) { role = it }

            Text(stringResource(R.string.admin_specialties), fontWeight = FontWeight.SemiBold)
            MultiChoiceRow(listOf("CHURN_ONLEME", "YUKSEK_DEGER"), selectedSpecialties) {
                selectedSpecialties = selectedSpecialties.toggle(it)
            }
            if (submitAttempted && selectedSpecialties.isEmpty()) {
                Text(stringResource(R.string.admin_error_specialty), color = MaterialTheme.colorScheme.error)
            }

            Text(stringResource(R.string.admin_regions), fontWeight = FontWeight.SemiBold)
            MultiChoiceRow(listOf("ISTANBUL", "ANKARA", "IZMIR"), selectedRegions) {
                selectedRegions = selectedRegions.toggle(it)
            }
            if (submitAttempted && selectedRegions.isEmpty()) {
                Text(stringResource(R.string.admin_error_region), color = MaterialTheme.colorScheme.error)
            }

            successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            createdStaffId?.let { id ->
                CopyableStaffId(id = id)
            }
            createdStaffTempPassword?.let { password ->
                CopyableValue(
                    label = stringResource(R.string.admin_created_staff_temp_password),
                    value = password,
                    contentDescription = stringResource(R.string.admin_copy_password)
                )
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStaffRoleScreen(
    query: String,
    searchResults: List<AdminStaff>,
    selectedStaff: AdminStaff?,
    isSearchingStaff: Boolean,
    staffSearchError: String?,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onStaffSelected: (AdminStaff) -> Unit,
    onDismissStaff: () -> Unit,
    onUpdateRole: (String, String) -> Unit,
    onClearClick: () -> Unit,
    isSubmitting: Boolean = false,
    successMessage: String? = null,
    errorMessage: String? = null
) {
    var role by remember { mutableStateOf("SUPERVISOR") }
    val availableRoles = listOf("EXPERT", "SUPERVISOR")

    LaunchedEffect(selectedStaff?.id, selectedStaff?.role) {
        role = availableRoles.firstOrNull { it != selectedStaff?.role } ?: ""
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onDismissStaff()
        }
    }

    Scaffold(
        topBar = { OfferHubDetailTopBar(stringResource(R.string.admin_update_role), onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.admin_search_staff_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearClick) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.admin_clear_search)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            when {
                isSearchingStaff -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                staffSearchError != null -> item {
                    Text(staffSearchError, color = MaterialTheme.colorScheme.error)
                }
                searchResults.isEmpty() -> item {
                    Text(
                        stringResource(R.string.admin_staff_search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(searchResults, key = { it.id }) { staff ->
                    Card(
                        onClick = { onStaffSelected(staff) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${staff.firstName} ${staff.lastName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(staff.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${stringResource(R.string.admin_current_role)}: ${staff.role}",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    selectedStaff?.let { staff ->
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSubmitting) onDismissStaff()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.admin_staff_information), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                CopyableStaffId(id = staff.id)
                StaffDetailValue(stringResource(R.string.admin_first_name), staff.firstName)
                StaffDetailValue(stringResource(R.string.admin_last_name), staff.lastName)
                StaffDetailValue(stringResource(R.string.admin_email), staff.email)
                StaffDetailValue(stringResource(R.string.admin_current_role), staff.role)
                StaffDetailValue(
                    stringResource(R.string.admin_specialties),
                    staff.specialties.joinToString().ifBlank { stringResource(R.string.common_not_available) }
                )
                StaffDetailValue(
                    stringResource(R.string.admin_regions),
                    staff.regions.joinToString().ifBlank { stringResource(R.string.common_not_available) }
                )
                Text(stringResource(R.string.admin_new_role), fontWeight = FontWeight.SemiBold)
                ChoiceRow(
                    options = availableRoles,
                    selected = role,
                    isOptionEnabled = { it != staff.role },
                    onSelect = { role = it }
                )
                successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !isSubmitting && role.isNotBlank() && role != staff.role,
                    onClick = { onUpdateRole(staff.id, role) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isSubmitting) stringResource(R.string.admin_updating)
                        else stringResource(R.string.admin_update_role)
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffDetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun ChoiceRow(
    options: List<String>,
    selected: String,
    isOptionEnabled: (String) -> Boolean = { true },
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = isOptionEnabled(option),
                label = { Text(option) }
            )
        }
    }
}

@Composable
private fun MultiChoiceRow(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option in selected, onClick = { onToggle(option) }, label = { Text(option) })
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

@Composable
private fun CopyableStaffId(id: String) {
    CopyableValue(
        label = stringResource(R.string.admin_created_staff_id),
        value = id,
        contentDescription = stringResource(R.string.admin_copy_id)
    )
}

@Composable
private fun CopyableValue(label: String, value: String, contentDescription: String) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(label, value))
                    )
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
