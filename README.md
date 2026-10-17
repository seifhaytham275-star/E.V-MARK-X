# E.V-MARK-X

**Public website:** https://seifhaytham275-star.github.io/E.V-MARK-X/  
**AI workspace:** https://seifhaytham275-star.github.io/E.V-MARK-X/app.html  
**About & privacy:** https://seifhaytham275-star.github.io/E.V-MARK-X/about.html

E.V-MARK-X is an independent AI workspace with a responsive website and direct browser-to-Gemini chat. The web workspace is fixed to Gemini 3.5 Flash-Lite; Google sign-in is not required.

## Web project

- `index.html` — public landing page.
- `app.html` — AI workspace, fixed to Gemini 3.5 Flash-Lite.
- `about.html` — project, privacy, and security notes.
- `config.js` — optional public configuration; never place API secrets here.
- `server/` — backend for Google credential verification and server-side Gemini requests.
- `render.yaml` — Render deployment blueprint.
- `WEB_BACKEND_SETUP.md` — deployment and OAuth setup guide.

## GitHub Pages usage (no sign-in)

Open the website and choose **Open Workspace**. In **Settings**, enter your own Gemini API key to chat with Gemini 3.5 Flash-Lite. The key is saved in local browser storage on that device and sent directly from your browser to Google Gemini; it is not committed to this repository or sent to the included backend. Use this only on a trusted personal device and remove the key before sharing the device. Browser storage is not a secure vault.

GitHub Pages serves static files only. This browser-based setup does not need Google OAuth or the Node.js backend. The optional `server/` backend and [WEB_BACKEND_SETUP.md](WEB_BACKEND_SETUP.md) remain available for a future server-backed configuration. Never commit a Gemini API key or other secret.

## Android project

The repository also contains Android source and APK build workflows. See the existing Android setup documentation in [SETUP.md](SETUP.md). The Android build and web backend are separate parts of this repository.

## Workspace feature status

The current web workspace includes opt-in Google Search grounding with returned source links, attachments for PDF/images/text and code files, editable browser-local memory, and save/load for text conversation projects. These features use the fixed Gemini 3.5 Flash-Lite model. PDF/image understanding and search depend on the model/API key's current access and should be tested with a real key before public launch.

- Attachments are limited to 10 MB total per message. DOCX/XLSX parsing is not implemented yet.
- Saved memory, projects, and the Gemini API key remain in browser storage on that device. Browser storage is not a secure vault; use a trusted device and do not save secrets in memory.
- Voice, image generation, E.V Studio, sandboxed code execution, and Agent Mode are planned phases, not currently shipped features.
- Roadmap: [FEATURE_ROADMAP.md](FEATURE_ROADMAP.md).
- Static regression tests: `node --test tests/web-smoke.test.cjs` (also wired into GitHub Actions). These tests do not replace a live Gemini API test or a real-device APK test.
