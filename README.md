# EV AI System

A single Android app with three sections:
- **EV AI Core** — connects to Google's Gemini API using a key entered for the current session.
- **EV Wallet** — simple local income and expense totals in EGP.
- **EV Calories** — simple food calorie counter with servings and a local total.

## Build with GitHub Actions
Open the **Actions** tab and run **Build EV AI System APK**, or push a commit to `main`. Download `EV-AI-System-debug-APK` from the successful workflow run's Artifacts section.

## Gemini API
Create a Gemini API key from Google AI Studio and enter it in EV AI Core. The app does not persist the key. A client-side app cannot fully protect an API key while it is being used; for public production distribution, use a server-side proxy and keep the secret on the server. Never commit API keys.

## Notes
- Wallet and calorie totals are stored locally on the device.
- Calories are estimates, not medical advice.
- This is a debug APK build configuration; verify the app on a device before release.
