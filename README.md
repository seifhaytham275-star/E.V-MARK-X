# E.V-MARK-X — EV AI System

**An Android AI assistant project with tools for chat, everyday planning, and simple personal tracking.**

E.V-MARK-X brings these areas together in one Android app:

- **EV AI Core** — Gemini-powered chat, model discovery for the API key you provide, and saved conversations.
- **EV Wallet** — local income and expense totals in EGP.
- **EV Calories** — a basic food log and estimated calorie totals.

> **Project status: beta / under active development.** Test the app before relying on it. Features that require Firebase will not work until the project owner configures their own Firebase and OAuth settings.

## Get the app

- **Latest release:** [Open Releases](https://github.com/seifhaytham275-star/E.V-MARK-X/releases/latest)
- **Build history / beta artifacts:** [Open GitHub Actions](https://github.com/seifhaytham275-star/E.V-MARK-X/actions)

If there is no published release yet, the Releases page may be empty. Open the latest successful build in Actions, then download **EV-AI-System-debug-APK** from the Artifacts section. GitHub Actions artifacts are temporary and may expire, so a tagged release is the preferred download route.

### Install the beta APK

1. Download the APK from a release or successful workflow artifact.
2. Open it on your Android device and follow Android's installation prompts.
3. Install only if you trust the source and understand that this is a beta/debug build.
4. If installation fails or the app crashes, report the Android version and the steps that caused the issue—never include passwords or API keys in a report.

## Gemini setup and API costs

1. Create your own Gemini API key in Google AI Studio.
2. Enter the key in EV AI Core.
3. Use **Refresh Available Models** to discover models available to your key.

Model access, quotas, pricing, and free-tier eligibility depend on Google's current policies and your account. Do not assume every model is free.

**Security note:** a mobile client cannot fully protect a secret while using it. Do not put a private API key in source code or commit it to this repository. Before broad public distribution, the app needs a carefully secured server-side proxy, rate limits, cost controls, and a clear privacy notice. Never share your API key with other users.

## Firebase features

Google sign-in and cloud conversation sync require a Firebase project and OAuth configuration. Follow [SETUP.md](SETUP.md). Placeholder values in the repository are not a working production configuration.

## Privacy and limitations

- Without sign-in, conversations are stored locally on the device.
- With Firebase configured and sign-in enabled, conversations may sync to the signed-in user's Firestore account.
- Wallet and food-log totals are stored locally on the device.
- Calorie values are estimates and are not medical advice.
- Do not use a shared device for private conversations unless you sign out and understand local-data behavior.

## Feedback and support

Please open a [GitHub issue](https://github.com/seifhaytham275-star/E.V-MARK-X/issues) with a clear description, device/Android version, and reproduction steps. Remove personal information, passwords, API keys, and other secrets before posting.

## For contributors

See [SETUP.md](SETUP.md) for project setup. Pull requests and constructive feedback are welcome.

## Roadmap

- [ ] Test the app on multiple Android devices
- [ ] Publish a tagged beta release with a stable APK link
- [ ] Improve first-run guidance and error messages
- [ ] Review API-key handling, privacy, and cost controls before wider distribution
- [ ] Collect feedback and prioritize the most-used features

---

Made as an independent project. Gemini and Firebase are Google services; E.V-MARK-X is not an official Google product.
