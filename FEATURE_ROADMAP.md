# E.V-MARK-7 Platinum Feature Roadmap

This roadmap describes the staged work for the existing GitHub Pages workspace. The model remains fixed to `gemini-3.5-flash-lite`; this project does not add a model picker.

## Existing baseline (verified by source inspection)

- Static landing page and chat workspace.
- Direct browser-to-Gemini requests using the visitor's own API key.
- API key save/remove controls in browser storage.
- Multi-turn text conversation, New Chat, QR/share-related features elsewhere in the project.
- Separate optional Node.js backend; it is not the active path used by the current GitHub Pages chat.
- GitHub Actions / APK build status must be checked from the actual run, not inferred from source.

## Implemented in this increment

- **Web Search:** opt-in Google Search grounding uses the REST API's `google_search` tool field; source links are only shown when returned as grounding metadata.
- **Attachments:** PDF, common image formats, text/code/CSV/JSON, DOCX, and XLSX are supported, with a 10 MB total attachment limit per message. DOCX is extracted with Mammoth and XLSX sheets are converted to CSV-style text with SheetJS; each extracted document is capped at 120,000 characters. Pinned parser scripts load from jsDelivr and therefore require internet access.
- **Local memory and projects:** user-editable memory plus browser-local save/load for conversation projects.
- **E.V Studio:** send `/studio describe your app` in the existing chat box to generate a standalone HTML app. Preview runs in a sandboxed iframe with scripts allowed but same-origin access and network requests blocked; generated HTML can be downloaded.
- **Agent Mode:** send `/agent describe the task` in the existing chat box. The model can request an allowlisted calculator, current-time lookup, grounded web search, saved-memory read, or Studio preview. Tool execution is bounded to four calls per task; no arbitrary shell, file-system, account, or irreversible external actions are exposed.
- **70+ workflow commands:** added a registry of focused AI workflows for learning, writing, planning, coding, debugging, reviews, testing, documentation, accessibility, security, and product work. These are invoked in the existing composer with `/command your request`; `/help` and `/commands` list them.
- **Local utility commands:** `/math` uses the safe arithmetic parser, `/wordcount`, `/reverse`, `/upper`, `/lower`, `/date`, and `/time` run locally, and `/export` downloads the displayed conversation as plain text.
- **UI preservation:** no new visible controls or CSS changes were required; the existing message box is the command entry point. Gemini-powered workflows continue to use only the pinned Gemini 3.5 Flash-Lite model.
- **Regression tests:** JavaScript syntax, model pinning, UI IDs, attachment limits, search wiring, sandbox configuration, and actual safe-calculator arithmetic tests.

## Next implementation phases

### Phase 2 — robust documents and security
- Add content-signature validation for files in addition to extension/MIME checks.
- Add clearer per-file upload progress and more comprehensive per-request payload limits.
- Prefer a server-side API proxy for public deployments. The current browser-stored API key is not a secure vault and can be read by scripts running on the same origin.
- Add a project manager with selective delete/export actions and storage quota handling.

### Phase 3 — E.V Studio improvements
- Add edit, reset, copy, and version history actions to the generated preview workflow.
- Add richer artifact management after the current sandboxed preview/download path has been tested with a live API key.
- Keep generated code isolated; never execute it in the main application origin.

### Phase 4 — Agent Mode improvements
- Add visible per-step progress and a cancellable agent run without changing the existing visual language.
- Add chart generation for CSV/JSON data, then richer spreadsheet analysis after XLSX parsing is implemented.
- Only enable model-provided code execution if the pinned model/API explicitly supports it; do not use `eval` or execute arbitrary code in the main page.

### Phase 5 — voice and image creation
- Add speech input/output with clear microphone permission prompts and a text fallback.
- Image understanding can use supported image attachments.
- Image generation requires a supported image-generation API/model and must not be represented as available until an end-to-end request is verified.

## Acceptance criteria

- The fixed model ID is unchanged and no model picker is introduced.
- Existing chat, New Chat, QR, sharing, and message interactions still need a full manual regression pass after these code changes.
- Search sources are actual URLs returned by the API; no fabricated citations.
- The new Studio and Agent API flows still require a live test with a valid API key; static tests do not prove provider-side availability.
- Files over configured limits are rejected before the request.
- Memory/projects are user-controlled and local-only unless a clearly disclosed backend is added.
- Studio previews never execute inside the main page context.
- Every claimed feature has an automated test where feasible and a documented manual API/device test where credentials or a real device are required.
