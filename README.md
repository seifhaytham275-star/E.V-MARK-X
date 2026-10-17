# E.V-MARK-XI Platinum

**Live website:** https://seifhaytham275-star.github.io/E.V-MARK-X/  
**Open the AI workspace:** https://seifhaytham275-star.github.io/E.V-MARK-X/app.html  
**About & privacy:** https://seifhaytham275-star.github.io/E.V-MARK-X/about.html  
**Source repository:** https://github.com/seifhaytham275-star/E.V-MARK-X

E.V-MARK-XI Platinum is the refreshed workspace name for this E.V-MARK-X project: a responsive AI workspace with chat, study and writing workflows, a browser-based Studio, and a limited tool-using Agent. The web workspace uses the configured Gemini 3.5 Flash-Lite model. Google sign-in is not required for the GitHub Pages version.

## Get the Android APK

1. Open the [latest Android build workflow](https://github.com/seifhaytham275-star/E.V-MARK-X/actions/workflows/build-apk.yml).
2. Open the newest run with a **successful** status.
3. In **Artifacts**, download `EV-AI-System-debug-APK`.
4. Extract the ZIP, then install `app-debug.apk` on your Android device. Android may ask you to allow installs from the browser or file manager you used.
5. If the latest run is still running, wait until it completes successfully before downloading.

The APK is an Android WebView wrapper around the same live `app.html` workspace. It therefore shares the website's layout and web features rather than maintaining a separate copy of the UI. It needs an internet connection to load the workspace and updates to the hosted website appear in the APK after the page is refreshed/reopened. The APK supports file selection, downloads, browser back navigation, a loading indicator, and a retry screen if the workspace cannot connect. The build produced by GitHub Actions is a **debug/test APK**, not a production-signed Play Store release.

## Web project

- `index.html` — public landing page.
- `app.html` — responsive AI workspace, fixed to Gemini 3.5 Flash-Lite.
- `about.html` — project, privacy, and security notes.
- `config.js` — optional public configuration; never place API secrets here.
- `server/` — optional backend for Google credential verification and server-side Gemini requests.
- `render.yaml` and `WEB_BACKEND_SETUP.md` — optional backend deployment setup.
- `app/` — Android project source.
- `.github/workflows/build-apk.yml` — Android debug APK build workflow.
- `.github/workflows/deploy-pages.yml` — GitHub Pages deployment workflow.

## GitHub Pages setup (no sign-in)

Open the website and choose **Open Workspace**. In **Settings**, enter your own Gemini API key to chat. In this static-browser configuration, the key is saved in local browser storage on that device and sent directly from the browser to Google Gemini; it is not committed to this repository or sent to the optional backend. Browser storage is not a secure vault: use a trusted personal device and remove the key before sharing that device. Never commit a Gemini API key or other secret.

GitHub Pages serves static files only. The browser-based setup does not need Google OAuth or the Node.js backend. The optional `server/` backend and [WEB_BACKEND_SETUP.md](WEB_BACKEND_SETUP.md) are available for a server-backed configuration.

## Workspace features

- Gemini chat with optional Google Search grounding and returned source links.
- PDF, image, text/code, DOCX, and XLSX attachments (up to 10 MB total per message). DOCX and XLSX files are converted to text in the browser before being sent to Gemini; workbook sheets are flattened to CSV-style text and capped at 120,000 characters per file.
- Browser-local editable memory and save/load for text conversation projects.
- **E.V Studio:** type `/studio build a to-do app with filters and local saving` in the message box to create a sandboxed HTML preview; use Download HTML to save it.
- **Agent Mode:** type `/agent calculate 123*456 and explain the result` or `/agent research the latest changes in a topic and summarize the sources`. The Agent has an allowlisted tool set and a limit of four tool calls per task.
- **70+ slash-command workflows:** summaries, translation, quizzes, flashcards, writing, study planning, code generation/review/debugging, requirements, test cases, security, accessibility, documentation, and more. Type `/help` or `/commands` for the command list.
- **Local utilities (no API request):** `/math 12*(3+4)`, `/wordcount paste text`, `/reverse text`, `/upper text`, `/lower text`, `/date`, and `/time`. `/export` downloads the visible conversation as text.
- Unknown slash-prefixed messages are treated as normal chat rather than discarded.

## Limitations and privacy

- Studio and Agent Mode still require a live Gemini API test with a valid key. Static tests do not prove API access, quotas, or model availability.
- Built-in voice, image generation, and a real isolated code-execution environment are not implemented yet. DOCX/XLSX parsing uses pinned browser libraries loaded from jsDelivr, so those formats need an internet connection.
- Saved memory, projects, and the Gemini API key remain in browser storage in the browser-based configuration. Do not save secrets in memory and do not use a shared device for a personal API key.
- The APK loads the hosted website and is online-first; it does not bundle the whole workspace for offline use.
- The Android source includes Firebase dependencies, but Google sign-in/cloud sync need a real Firebase project configuration. See [SETUP.md](SETUP.md).

## Tests and roadmap

Run the static regression tests locally with:

```sh
node --test tests/web-smoke.test.cjs
```

The same tests run in GitHub Actions. They do not replace testing with a real Gemini API key or testing the APK on a physical Android device.

The current workspace brand is **E.V-MARK-XI Platinum**. The existing repository URL remains unchanged so current links continue working. See [FEATURE_ROADMAP.md](FEATURE_ROADMAP.md) for planned work.

## Developer-focused product direction

E.V is focused on **programmers and developers**, not business CRM users. The core product areas are:

- **E.V Studio:** create a standalone HTML prototype from a prompt, preview it in a sandboxed frame, and download the result.
- **Data Intelligence:** inspect supported documents and structured data, summarize findings, and help reason about data. Visualizations and deeper analysis depend on the supported input and current implementation.
- **Agent Mode:** bounded tool-assisted workflows for research, calculations, and development tasks.
- **Developer workflows:** code generation, review, debugging, documentation, test planning, and accessibility/security checklists through the command system.

The current website remains an online-first static app. Studio and Agent workflows still need live API and browser/device testing; do not assume every generated artifact or tool flow is production-ready.

## Release quality and discoverability

- Keep the landing page and app focused on developer use cases.
- Preserve the existing chat, settings, file workflows, Studio, and Agent Mode while improving them incrementally.
- Run `node --test tests/web-smoke.test.cjs` before releases.
- Test the published GitHub Pages build on desktop and Android, including small screens, keyboard navigation, file selection, error states, and API failures.
- Keep screenshots, setup instructions, known limitations, and release notes current.
- Never publish secrets, API keys, or user data in source control.
- Before public distribution, confirm the selected AI provider's current terms permit the intended audience and use case.
