# E.V-MARK-X — EV AI System

**Live web demo:** https://seifhaytham275-star.github.io/E.V-MARK-X/  
**AI workspace:** https://seifhaytham275-star.github.io/E.V-MARK-X/app.html  
**About & privacy:** https://seifhaytham275-star.github.io/E.V-MARK-X/about.html

**An Android AI assistant project with tools for chat, everyday planning, and simple personal tracking.**

E.V-MARK-X brings these areas together in one Android app:

### Available features

- **Gemini AI chat** — send questions to a model available to your own API key.
- **Model discovery** — refresh the model list from Google's API and choose a model available to your key.
- **Remember my API key** — save the Gemini key locally using Android Keystore-backed AES-GCM encryption; the app reloads it on next launch and lets you remove it. This protects local storage but does not make a client-side key safe to distribute publicly.
- **Spider-tech interface** — custom red/blue gradients and original web-inspired artwork, rather than an unlicensed movie still.
- **Saved chat history** — revisit recent conversations stored on the device; cloud sync depends on Firebase setup.
- **Quick Prompt Library** — one-tap starters for planning a day, explaining a topic simply, and improving a message.
- **Share Conversation** — share the current chat as plain text through Android's standard share sheet.
- **New Chat shortcut** — start a clean conversation without manually clearing the previous transcript.
- **EV Wallet** — local income and expense totals in EGP.
- **EV Calories / Wellness** — a basic food log and estimated calorie totals with wellness-planning UI.

### Planned improvements (not yet implemented)

- Voice input and spoken responses.
- Image and document understanding.
- Reminders and calendar integration.
- More accessible onboarding and language options.
- A secure server-side API proxy with rate limits and cost controls.

Planned improvements above are ideas for future versions and are **not included in the current APK**.

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

## Public web demo and API-key safety

The GitHub Pages website has a public landing page (`index.html`), AI workspace (`app.html`), and about/privacy page (`about.html`). The workspace discovers Gemini models available to the visitor's own API key and sends the key in the `x-goog-api-key` request header rather than placing it in the request URL. The key is held in page memory only and is not stored in local storage. **This does not make a browser-side key secret**: the device owner can inspect browser requests. Visitors must use their own keys and should never paste a shared/private service key into the public page. A shared-key service requires a separate backend with server-side secrets, per-user rate limits, abuse controls, and cost monitoring; GitHub Pages alone cannot provide that backend.

## Gemini setup and API costs

1. Create your own Gemini API key in Google AI Studio.
2. Enter the key in EV AI Core and tap **Save API Key**. It will be remembered on this device until you remove it or uninstall the app.
3. Use **Refresh Available Models** to discover models available to your key.

Model access, quotas, pricing, and free-tier eligibility depend on Google's current policies and your account. Do not assume every model is free.

**Security note:** the saved key is encrypted locally with an Android Keystore-managed key. A mobile client still cannot fully protect a secret while using it. Do not put a private API key in source code or commit it to this repository. Before broad public distribution, the app needs a carefully secured server-side proxy, rate limits, cost controls, and a clear privacy notice. Never share your API key with other users.

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
