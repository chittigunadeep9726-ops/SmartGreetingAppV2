package com.example.smartgreeting

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class PhoneContact(val key: String, val name: String, val phone: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartGreetingScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGreetingScreen() {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var template by remember {
        mutableStateOf("Happy New Year, {name}! 🎉 Wishing you happiness and success.")
    }
    var channel by remember { mutableStateOf("SMS") }
    var status by remember { mutableStateOf("Load contacts to begin. Your contacts stay on this device.") }
    var queueIndex by remember { mutableIntStateOf(0) }

    fun loadContacts() {
        val loaded = mutableListOf<PhoneContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )
        cursor?.use {
            val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = it.getString(idCol) ?: continue
                val name = it.getString(nameCol)?.trim().orEmpty()
                val phone = it.getString(phoneCol)?.trim().orEmpty()
                if (name.isNotBlank() && phone.isNotBlank()) {
                    loaded.add(PhoneContact("$id:$phone", name, phone))
                }
            }
        }
        contacts = loaded.distinctBy { it.key }
        selectedKeys = selectedKeys.intersect(contacts.map { it.key }.toSet())
        status = "Loaded ${contacts.size} phone numbers."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) loadContacts()
        else status = "Permission denied. Enable Contacts permission in Android Settings."
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) loadContacts()
    }

    val selected = contacts.filter { selectedKeys.contains(it.key) }

    fun openNextMessage() {
        if (selected.isEmpty()) {
            status = "Select at least one contact first."
            return
        }
        val contact = selected[queueIndex % selected.size]
        val message = template.replace("{name}", contact.name.ifBlank { "there" })
        try {
            val intent = if (channel == "SMS") {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${Uri.encode(contact.phone)}")
                    putExtra("sms_body", message)
                }
            } else {
                // Opens WhatsApp chat via phone number. User reviews and sends manually.
                val digits = contact.phone.filter { it.isDigit() }
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
                }
            }
            context.startActivity(intent)
            status = "Opened ${channel} for ${contact.name}. Review the recipient and message, then tap Send."
            queueIndex = (queueIndex + 1) % selected.size
        } catch (e: Exception) {
            status = if (channel == "WhatsApp")
                "Could not open WhatsApp. Check that WhatsApp is installed and the number includes its country code."
            else "Could not open an SMS app on this device."
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Smart Greeting V2") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Personalized greetings for WhatsApp and SMS", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                    Text("Load contacts")
                }
                OutlinedButton(
                    enabled = contacts.isNotEmpty(),
                    onClick = {
                        selectedKeys = if (selectedKeys.size == contacts.size) emptySet()
                        else contacts.map { it.key }.toSet()
                        queueIndex = 0
                    }
                ) {
                    Text(if (contacts.isNotEmpty() && selectedKeys.size == contacts.size) "Deselect all" else "Select all")
                }
            }

            Text("Send using", fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = channel == "WhatsApp",
                    onClick = { channel = "WhatsApp"; queueIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("WhatsApp") }
                SegmentedButton(
                    selected = channel == "SMS",
                    onClick = { channel = "SMS"; queueIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("SMS") }
            }

            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                label = { Text("Message template") },
                supportingText = { Text("Use {name} to insert each contact's name.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text("${selected.size} selected • ${contacts.size} contacts loaded", fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.bodySmall)

            if (contacts.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tap Load contacts and grant permission.")
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(contacts, key = { it.key }) { contact ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedKeys.contains(contact.key),
                                onCheckedChange = { checked ->
                                    selectedKeys = if (checked) selectedKeys + contact.key else selectedKeys - contact.key
                                    queueIndex = 0
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.Medium)
                                Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { openNextMessage() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty()
            ) {
                Text("Prepare next ${channel} message")
            }
            Text(
                "Review each message and tap Send yourself. This app does not auto-send bulk messages. " +
                    "For WhatsApp, numbers should include the country code (for example, +91).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
