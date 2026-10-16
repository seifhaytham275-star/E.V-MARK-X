# E.V Google Sign-In + Cloud Chat Setup

The Android source is ready for Firebase Auth/Firestore, but the repository intentionally uses placeholders. Configure your own Firebase project before expecting Google sign-in or account sync to work.

## 1. Create the Firebase project
1. Open https://console.firebase.google.com/ and create a project.
2. Add an Android app with package name **com.ev.aisystem**.
3. In Project settings, register the SHA-1 signing certificate for the debug app. From the repository, run `gradle signingReport` (or use the Android Gradle task in Android Studio) and copy the debug SHA-1.
4. In **Authentication → Sign-in method**, enable **Google**.
5. Create a **Web application OAuth client** in Google Cloud Console / the Firebase Google provider setup. Its client ID is the Web client ID used for ID-token sign-in.
6. Create a Cloud Firestore database.

## 2. Set the Android config values
Edit `app/src/main/res/values/strings.xml` and replace:
- `REPLACE_WITH_FIREBASE_API_KEY` with the Firebase project's Web API key.
- `REPLACE_WITH_FIREBASE_APP_ID` with the Android app ID from Firebase settings.
- `REPLACE_WITH_FIREBASE_PROJECT_ID` with the Firebase project ID.
- `REPLACE_WITH_GOOGLE_WEB_CLIENT_ID` with the OAuth **Web client ID**, not the Android client ID.

Do not paste passwords or OAuth client secrets into the app. The Web client ID is an identifier, not a client secret. The API key for Gemini is entered separately in the app and is not saved.

## 3. Apply Firestore security rules
In Firebase Console → Firestore Database → Rules, publish these rules so every user can only read and write their own chat documents:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/chats/{chatId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Do not use open rules such as `allow read, write: if true`.

## 4. Build and test
1. Commit the updated `strings.xml` config values to your private repo (or configure them through a safer build-time secret mechanism before public distribution).
2. Open the repository Actions tab and wait for **Build EV AI System APK** to finish.
3. Install the APK, sign in with Google, send a message, then reopen the app to verify the session and history.
4. Sign in on another device with the same account to verify cloud history sync.

## How it behaves
- Google/Firebase retains the authenticated session on the device; you should not need to enter a separate app password each launch.
- Chat history is saved locally and, when signed in, synced to `users/{your UID}/chats`.
- Gemini API keys are not stored by the app.
- The current APK uses Google Sign-In APIs and Firebase; account sign-in cannot succeed with placeholder values.
