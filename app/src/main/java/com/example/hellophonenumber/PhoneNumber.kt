package com.example.hellophonenumber

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlin.code
import kotlin.text.format

data class Country(
    val name: String,
    val code: String,
    val dialCode: String,
    val flag: String
)

fun getMaxPhoneDigitsFromLib(countryCode: String): Int {
    val phoneUtil = PhoneNumberUtil.getInstance()
    val example = phoneUtil.getExampleNumber(countryCode)
    return example?.nationalNumber?.toString()?.length ?: 15
}

@Composable
fun PhoneNumberInput(
    modifier: Modifier = Modifier,
    onPhoneNumberChange: (phoneNumber: String, isValid: Boolean, e164Format: String) -> Unit
) {
    var selectedCountry by remember { mutableStateOf(getDefaultCountry()) }
    var phoneNumber by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }
    var e164Format by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.dialCode})",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country")
        }

        Spacer(modifier = Modifier.height(8.dp))

        PhoneNumberTextField(
            phoneNumber = phoneNumber,
            countryCode = selectedCountry.code,
            onPhoneNumberChange = { newNumber, valid, e164 ->
                phoneNumber = newNumber
                isValid = valid
                e164Format = e164
                onPhoneNumberChange(newNumber, valid, e164)
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (phoneNumber.isNotEmpty()) {
            Text(
                text = if (isValid) "✓ Valid phone number" else "✗ Invalid phone number",
                color = if (isValid) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            countries = getAllCountries(),
            onCountrySelected = { country ->
                selectedCountry = country
                phoneNumber = ""
                isValid = false
                e164Format = ""
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
fun PhoneNumberTextField(
    phoneNumber: String,
    countryCode: String,
    onPhoneNumberChange: (String, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDigits = getMaxPhoneDigitsFromLib(countryCode)
    val phoneUtil = PhoneNumberUtil.getInstance()
    var isValid by remember { mutableStateOf(false) }
    var e164Format by remember { mutableStateOf("") }

    // Re-validate when phoneNumber or countryCode changes
    LaunchedEffect(phoneNumber, countryCode) {
        val digits = phoneNumber.filter { it.isDigit() }.take(maxDigits)
        try {
            val parsed = phoneUtil.parse(digits, countryCode)
            isValid = phoneUtil.isValidNumber(parsed)
            e164Format = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            isValid = false
            e164Format = ""
        }
        onPhoneNumberChange(digits, isValid, e164Format)
    }

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = { newValue ->
            val digits = newValue.filter { it.isDigit() }.take(maxDigits)
            try {
                val parsed = phoneUtil.parse(digits, countryCode)
                isValid = phoneUtil.isValidNumber(parsed)
                e164Format = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } catch (e: Exception) {
                isValid = false
                e164Format = ""
            }
            onPhoneNumberChange(digits, isValid, e164Format)
        },
        modifier = modifier,
        label = { Text("Phone Number") },
        placeholder = { Text(getPhoneNumberPlaceholder(countryCode)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        isError = phoneNumber.isNotEmpty() && !isValid,
        supportingText = {
            if (phoneNumber.isNotEmpty() && !isValid) {
                Text("Enter a valid phone number")
            } else {
                null
            }
        },
        leadingIcon = {
            Icon(Icons.Default.Phone, contentDescription = null)
        },
        trailingIcon = {
            if (phoneNumber.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier.clickable { onPhoneNumberChange("", false, "") }
                )
            }
        },
        singleLine = true,
        visualTransformation = PhoneNumberVisualTransformation(countryCode)
    )
}

// Visual transformation for formatting
class PhoneNumberVisualTransformation(
    private val countryCode: String
) : VisualTransformation {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    override fun filter(text: AnnotatedString): TransformedText {
        // Extract only digits from input
        val digitsOnly = text.text.filter { it.isDigit() }

        // Limit to reasonable phone number length to prevent OOM
        val limitedDigits = digitsOnly.take(15)

        // Format the digits
        val formatter = phoneUtil.getAsYouTypeFormatter(countryCode)
        formatter.clear()

        var formatted = ""
        for (digit in limitedDigits) {
            formatted = formatter.inputDigit(digit)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Count digits up to the offset in original text
                val digitsBefore = text.text.take(offset).count { it.isDigit() }

                // Find corresponding position in formatted text
                var count = 0
                var position = 0
                while (count < digitsBefore && position < formatted.length) {
                    if (formatted[position].isDigit()) count++
                    position++
                }
                return position
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Count digits up to the offset in formatted text
                val digitsBefore = formatted.take(offset).count { it.isDigit() }

                // Find corresponding position in original text
                var count = 0
                var position = 0
                while (count < digitsBefore && position < text.text.length) {
                    if (text.text[position].isDigit()) count++
                    position++
                }
                return position
            }
        }

        return TransformedText(
            AnnotatedString(formatted),
            offsetMapping
        )
    }
}

@Composable
fun CountryPickerDialog(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery, countries) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.dialCode.contains(searchQuery)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search country") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                // Country list
                LazyColumn {
                    items(filteredCountries) { country ->
                        CountryItem(
                            country = country,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountryItem(
    country: Country,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = country.flag,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = country.dialCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
fun getDefaultCountry(): Country {
    // You can detect user's country or default to US
    return Country("United States", "US", "+1", "🇺🇸")
}

fun getPhoneNumberPlaceholder(countryCode: String): String {
    return when (countryCode) {
        "US", "CA" -> "(555) 123-4567"
        "GB" -> "07400 123456"
        "IN" -> "98765 43210"
        "AU" -> "0412 345 678"
        else -> "Phone number"
    }
}

fun getAllCountries(): List<Country> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return phoneUtil.supportedRegions.map { regionCode ->
        val dialCode = "+${phoneUtil.getCountryCodeForRegion(regionCode)}"
        Country(
            name = regionCode, // You may want to map this to a full country name
            code = regionCode,
            dialCode = dialCode,
            flag = "" // You can add flag logic if needed
        )
    }.sortedBy { it.name }
}
