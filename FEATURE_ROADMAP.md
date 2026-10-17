# E.V-MARK-X Feature Roadmap

This roadmap describes the staged work for the existing GitHub Pages workspace. The model remains fixed to `gemini-3.5-flash-lite`; this project does not add a model picker.

## Existing baseline (verified by source inspection)

- Static landing page and chat workspace.
- Direct browser-to-Gemini requests using the visitor's own API key.
- API key save/remove controls in browser storage.
- Multi-turn text conversation, New Chat, QR/share-related features elsewhere in the project.
- Separate optional Node.js backend; it is not the active path used by the current GitHub Pages chat.
- GitHub Actions / APK build status must be checked from the actual run, not inferred from source.

## Implemented in this increment

- **Web Search toggle:** opt-in Google Search grounding on Gemini requests; source links are displayed only when returned in grounding metadata.
- **Attachments:** PDF, PNG, JPG/JPEG, WEBP, GIF, and text/code/CSV/JSON files can be attached. Files are limited to 10 MB each; text/code content is limited to 120,000 characters per file.
- **Local memory:** editable, clearable user-approved context stored in this browser and sent as a system instruction. Do not store secrets, passwords, or sensitive data.
- **Local projects:** save and reload text conversation history in this browser (up to 30 saved entries).
- **Static smoke tests:** JavaScript syntax, key UI elements, fixed model, and feature wiring checks.

## Next implementation phases

### Phase 2 — robust documents and security
- Add DOCX and XLSX parsing with pinned, integrity-checked dependencies or a tested backend parser.
- Add file type validation based on both extension and content, clearer upload progress, and per-request payload limits.
- Prefer a server-side API proxy for public deployments. The current browser-stored API key is not a secure vault and can be read by scripts running on the same origin.
- Add explicit privacy controls for saved projects, export/delete actions, and storage quota handling.

### Phase 3 — E.V Studio
- Generate HTML/CSS/JS artifacts into a separate preview pane.
- Run previews only in a sandboxed iframe with restrictive permissions; never execute generated code in the main application origin.
- Add edit, reset, copy, download, and version history actions.

### Phase 4 — analysis and Agent Mode
- Enable model-provided code execution only where the selected API/model explicitly supports it; do not use `eval` or execute generated code in the browser page.
- Add bounded tool/function calling with allowlisted tools, per-step status, timeouts, and user confirmation before external or irreversible actions.
- Add CSV/JSON charting first, then richer spreadsheet analysis after XLSX parsing is implemented.

### Phase 5 — voice and image creation
- Add speech input/output with clear microphone permission prompts and a text fallback.
- Image understanding can use supported image attachments.
- Image generation requires a supported image-generation API/model and must not be represented as available until an end-to-end request is verified.

## Acceptance criteria

- The fixed model ID is unchanged and no model picker is introduced.
- Existing chat, New Chat, QR, sharing, and message interactions are regression-tested.
- Search sources are actual URLs returned by the API; no fabricated citations.
- Files over configured limits are rejected before the request.
- Memory/projects are user-controlled and local-only unless a clearly disclosed backend is added.
- Studio previews never execute inside the main page context.
- Every claimed feature has an automated test where feasible and a documented manual API/device test where credentials or a real device are required.
