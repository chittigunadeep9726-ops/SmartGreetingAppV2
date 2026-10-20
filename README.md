# Smart Greeting App V2

## Features
- Imports contacts after you grant Android Contacts permission.
- Select contacts individually, select all, or deselect all.
- Custom message template supports `{name}`.
- Choose WhatsApp or SMS.
- Opens one recipient/message at a time so you can review and send manually.
- Does not upload contacts to a server or silently send bulk messages.

## Setup for beginners
1. Install Android Studio on your computer.
2. Extract `SmartGreetingAppV2.zip`.
3. In Android Studio, choose **Open** and select the extracted `SmartGreetingAppV2` folder.
4. Let Gradle sync. If asked, use JDK 17.
5. Connect an Android phone with Developer options and USB debugging enabled, or start an emulator.
6. Click **Run** and grant Contacts permission.
7. Select contacts, choose WhatsApp or SMS, edit the message, and tap **Prepare next**.
8. Review the recipient and text in the chosen app, then tap Send yourself. Tap Prepare next for the next selected contact.

## WhatsApp notes
- The phone number must be saved in international format for best results (India: +91 followed by the number).
- WhatsApp must be installed and configured for the WhatsApp option.
- This opens the chat and pre-fills the text; it does not press Send.
- For business-scale messaging, use WhatsApp's official business tools and follow its consent and messaging policies.

## SMS notes
The phone opens the SMS composer for one selected contact. Carrier charges may apply.

## Build requirements
- Android Studio
- Android SDK Platform 35
- JDK 17
- Internet access for Gradle dependencies on first build
