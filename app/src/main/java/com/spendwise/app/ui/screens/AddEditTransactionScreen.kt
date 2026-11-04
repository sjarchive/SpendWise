package com.spendwise.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.ui.components.ExpressiveTypeToggle
import com.spendwise.app.ui.components.MorphingSaveButton
import com.spendwise.app.ui.theme.CardShape
import com.spendwise.app.ui.theme.PillShape
import com.spendwise.app.ui.util.CategoryIconHelper
import com.spendwise.app.ui.util.DateUtils
import com.spendwise.app.ui.util.Haptics
import com.spendwise.app.ui.viewmodel.AddEditTransactionViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

private fun hideKeyboardImmediately(
    view: View,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?
) {
    focusManager.clearFocus(force = true)
    keyboardController?.hide()
    view.clearFocus()
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
    var ctx = view.context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            WindowInsetsControllerCompat(ctx.window, view).hide(WindowInsetsCompat.Type.ime())
            break
        }
        ctx = ctx.baseContext
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }
    val view = LocalView.current

    val handleBack = {
        hideKeyboardImmediately(view, focusManager, keyboardController)
        onNavigateBack()
    }

    BackHandler {
        handleBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            hideKeyboardImmediately(view, focusManager, keyboardController)
        }
    }

    // Show a brief "Saved!" confirmation on the button before popping back,
    // instead of closing the screen the instant the row is written.
    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        when {
            uiState.isSaved -> {
                hideKeyboardImmediately(view, focusManager, keyboardController)
                delay(750)
                onNavigateBack()
            }
            uiState.isDeleted -> {
                hideKeyboardImmediately(view, focusManager, keyboardController)
                delay(350)
                onNavigateBack()
            }
        }
    }

    // Auto-focus the amount field when adding a NEW transaction so users can
    // start typing immediately (skipped in edit mode to avoid surprise scrolls).
    LaunchedEffect(uiState.isEditMode) {
        if (!uiState.isEditMode) {
            delay(300)
            runCatching {
                amountFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_edit_transaction_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = if (uiState.isEditMode) "Edit Transaction" else "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                if (uiState.isEditMode) {
                    IconButton(
                        onClick = { viewModel.setShowDeleteConfirmDialog(true) },
                        modifier = Modifier.testTag("btn_delete_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete transaction",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Expense / Income Toggle Button Group
            item {
                ExpressiveTypeToggle(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )
            }

            // 2. Amount Input Card with Large Display
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_amount_input"),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedTextField(
                                value = uiState.amountText,
                                onValueChange = viewModel::onAmountChange,
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .width(220.dp)
                                    .focusRequester(amountFocusRequester)
                                    .testTag("input_amount")
                            )
                        }

                        // Quick amount suggestions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            listOf(100, 500, 1000, 2000).forEach { addVal ->
                                Surface(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .clickable {
                                            Haptics.tick(view)
                                            val current = uiState.amountText.toDoubleOrNull() ?: 0.0
                                            val newTotal = current + addVal
                                            viewModel.onAmountChange(
                                                // Always US locale: the amount field only accepts '.' as
                                                // the decimal separator, so a comma-separator locale
                                                // (e.g. "150,75") would be mangled into "15075" by the
                                                // input sanitizer.
                                                if (newTotal % 1.0 == 0.0) newTotal.toLong().toString()
                                                else String.format(Locale.US, "%.2f", newTotal)
                                            )
                                        }
                                        .testTag("chip_add_$addVal"),
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "+$currencySymbol$addVal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Validation message for amount
                        AnimatedVisibility(visible = uiState.amountError != null) {
                            Text(
                                text = uiState.amountError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Category Picker
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedVisibility(visible = uiState.categoryError != null) {
                        Text(
                            text = uiState.categoryError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.availableCategories.forEach { category ->
                            val isSelected = uiState.selectedCategory?.id == category.id
                            val categoryColor = Color(category.color)

                            Surface(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .clickable(
                                        onClick = { viewModel.onCategorySelected(category) },
                                        onClickLabel = "Select ${category.name} category"
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = PillShape
                                    )
                                    .testTag("category_chip_${category.name}"),
                                shape = PillShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(category.icon),
                                            contentDescription = category.name,
                                            tint = categoryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Date and Time Picker Row
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Date & Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Button
                        Surface(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setShowDatePicker(true) }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("btn_select_date"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = DateUtils.formatDate(uiState.selectedDateTime),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Time Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setShowTimePicker(true) }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("btn_select_time"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Select time",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = DateUtils.formatTime(uiState.selectedDateTime),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5. Payment Method Selector (Cash, UPI, Card, Bank)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            val isSelected = uiState.paymentMethod == method
                            val weight = when (method) {
                                PaymentMethod.BANK -> 1.35f
                                PaymentMethod.UPI -> 0.85f
                                else -> 0.9f
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.onPaymentMethodSelected(method) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("payment_method_${method.name}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getPaymentMethodIcon(method),
                                        contentDescription = method.displayName,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = method.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = if (method == PaymentMethod.BANK) 10.5.sp else 12.sp,
                                            lineHeight = 12.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Note Input Field
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Note / Memo (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChange,
                        placeholder = { Text("What was this transaction for?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_note"),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // 7. Shape-Morphing Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                MorphingSaveButton(
                    text = if (uiState.isEditMode) "Update Transaction" else "Save Transaction",
                    isLoading = uiState.isSaving,
                    isSuccess = uiState.isSaved,
                    onClick = viewModel::saveTransaction,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        // Material3 DatePicker interprets the initial selection in UTC; convert
        // the locally-zoned selected date so the right day is pre-selected.
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Instant.ofEpochMilli(uiState.selectedDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.setShowDatePicker(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            val originalInstant = Instant.ofEpochMilli(uiState.selectedDateTime).atZone(ZoneId.systemDefault())
                            val newDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                            val combined = originalInstant.toLocalTime().atDate(newDate).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            viewModel.onDateTimeSelected(combined)
                        }
                        viewModel.setShowDatePicker(false)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDatePicker(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (uiState.showTimePicker) {
        val originalDateTime = Instant.ofEpochMilli(uiState.selectedDateTime).atZone(ZoneId.systemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = originalDateTime.hour,
            initialMinute = originalDateTime.minute
        )

        AlertDialog(
            onDismissRequest = { viewModel.setShowTimePicker(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val combined = originalDateTime.toLocalDate().atTime(newTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        viewModel.onDateTimeSelected(combined)
                        viewModel.setShowTimePicker(false)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowTimePicker(false) }) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteConfirmDialog(false) },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteTransaction,
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDeleteConfirmDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
