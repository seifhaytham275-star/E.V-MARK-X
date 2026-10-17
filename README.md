# E.V-MARK-X

**Public website:** https://seifhaytham275-star.github.io/E.V-MARK-X/  
**AI workspace:** https://seifhaytham275-star.github.io/E.V-MARK-X/app.html  
**About & privacy:** https://seifhaytham275-star.github.io/E.V-MARK-X/about.html

E.V-MARK-X is an independent AI workspace with a responsive website, Gemini model discovery, and direct browser-to-Gemini chat. Google sign-in is not required for the GitHub Pages workspace.

## Web project

- `index.html` — public landing page.
- `app.html` — authenticated AI workspace.
- `about.html` — project, privacy, and security notes.
- `config.js` — optional public configuration; never place API secrets here.
- `server/` — backend for Google credential verification and server-side Gemini requests.
- `render.yaml` — Render deployment blueprint.
- `WEB_BACKEND_SETUP.md` — deployment and OAuth setup guide.

## GitHub Pages usage (no sign-in)

Open the website and choose **Open Workspace**. In **Settings**, enter your own Gemini API key to load supported models and chat. The key is saved in local browser storage on that device and sent directly from your browser to Google Gemini; it is not committed to this repository or sent to the included backend. Use this only on a trusted personal device and remove the key before sharing the device. Browser storage is not a secure vault.

GitHub Pages serves static files only. This browser-based setup does not need Google OAuth or the Node.js backend. The optional `server/` backend and [WEB_BACKEND_SETUP.md](WEB_BACKEND_SETUP.md) remain available for a future server-backed configuration. Never commit a Gemini API key or other secret.

## Android project

The repository also contains Android source and APK build workflows. See the existing Android setup documentation in [SETUP.md](SETUP.md). The Android build and web backend are separate parts of this repository.
