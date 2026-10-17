# E.V-MARK-X

**Public website:** https://seifhaytham275-star.github.io/E.V-MARK-X/  
**Google sign-in page:** https://seifhaytham275-star.github.io/E.V-MARK-X/login.html  
**AI workspace:** https://seifhaytham275-star.github.io/E.V-MARK-X/app.html  
**About & privacy:** https://seifhaytham275-star.github.io/E.V-MARK-X/about.html

E.V-MARK-X is an independent AI workspace project with a responsive English website, Google sign-in UI, Gemini model discovery, and a Node.js backend starter.

## Web project

- `index.html` — public landing page.
- `login.html` — Google Identity Services sign-in UI.
- `app.html` — authenticated AI workspace.
- `about.html` — project, privacy, and security notes.
- `config.js` — public configuration placeholders only; never place secrets here.
- `server/` — backend for Google credential verification and server-side Gemini requests.
- `render.yaml` — Render deployment blueprint.
- `WEB_BACKEND_SETUP.md` — deployment and OAuth setup guide.

## Important: sign-in and chat activation

The frontend and backend code are committed, but Google sign-in and server-backed chat are **not live until the project owner completes setup**. GitHub Pages hosts only static files; it cannot run the Node.js backend or safely store the Gemini API key.

Follow [WEB_BACKEND_SETUP.md](WEB_BACKEND_SETUP.md) to:
1. Create a Google OAuth Web client ID and add `https://seifhaytham275-star.github.io` as an authorized JavaScript origin.
2. Deploy the `server/` service using the included Render Blueprint.
3. Set `GOOGLE_CLIENT_ID` and `GEMINI_API_KEY` as backend environment variables.
4. Update `config.js` with the real backend URL and public Google Web client ID.
5. Wait for GitHub Pages to deploy and test sign-in, model loading, and chat.

**Never put `GEMINI_API_KEY` in `config.js`, HTML, frontend JavaScript, or a committed file.** Use only a server-side secret environment variable. The backend includes Google ID-token verification, request limits, input validation, model allow-list checking, and an origin allow-list. These are starter protections, not a complete multi-tenant service; configure provider quotas and billing alerts before public launch.

## Android project

The repository also contains Android source and APK build workflows. See the existing Android setup documentation in [SETUP.md](SETUP.md). The Android build and web backend are separate parts of this repository.
