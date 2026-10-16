# EV AI System

A single Android app with three sections:
- **EV AI Core** — Gemini API chat, live model discovery, saved conversations, Google sign-in and per-account Firestore sync (after Firebase setup).
- **EV Wallet** — simple local income and expense totals in EGP.
- **EV Calories** — simple food calorie counter with servings and a local total.

## Build with GitHub Actions
Open the [Actions tab](https://github.com/seifhaytham275-star/E.V-MARK-X/actions), open the latest **Build EV AI System APK** run, and download `EV-AI-System-debug-APK` from Artifacts after the run succeeds.

## First-time setup: Google sign-in and cloud chat sync
Follow [SETUP.md](SETUP.md). The repository contains safe placeholder configuration values; Google sign-in and cloud sync will not work until your own Firebase project and OAuth client are configured. Once signed in, Firebase Auth keeps the session on the device so you generally won't need to sign in every time.

## Gemini API
Create a Gemini API key in Google AI Studio and enter it in EV AI Core. The app uses it for the current session and does not save it. Tap **Refresh Available Models** to retrieve the models your key can access. The API model list does not guarantee that every model is on the free tier; free access and quotas depend on Google's current model, account, region and billing settings. A client-side app cannot fully protect an API key while it is being used; for public production distribution, use a server-side proxy and keep the secret on the server. Never commit API keys.

## Chat history and privacy
Without sign-in, conversations are saved locally on the device. With Firebase configured and Google sign-in active, chat sessions are also synced to that signed-in user's Firestore account. Do not use shared devices for private conversations unless you sign out.

## Notes
- Wallet and calorie totals are stored locally on the device.
- Calories are estimates, not medical advice.
- This is a debug APK build configuration; verify the app on a device before release.
