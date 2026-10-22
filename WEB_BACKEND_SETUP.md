# E.V-MARK-X Web Backend Setup

The static website can be hosted on GitHub Pages, but GitHub Pages cannot keep a Gemini API key secret or run an API server. The included Node.js backend verifies Google sign-in and proxies Gemini model/chat requests.

## A. Create Google OAuth credentials

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create/select a project and configure the Google Auth Platform / OAuth consent screen.
3. Create an **OAuth client ID** with application type **Web application**.
4. Add this exact **Authorized JavaScript origin**: `https://seifhaytham275-star.github.io` (no repository path).
5. Copy the client ID. Do not put a client secret in the website. Google Identity Services uses the web client ID in the browser; the backend verifies the returned ID token.
6. If the OAuth app is in Testing mode, add the Google accounts allowed to test it. For a public launch, review and publish the consent configuration as required by Google.

## B. Deploy the backend on Render

1. Sign in to [Render](https://render.com/) and choose **New → Blueprint**.
2. Select this GitHub repository. Render should detect `render.yaml` and create the `ev-mark-x-api` web service.
3. In the service's Environment settings, add:
   - `GOOGLE_CLIENT_ID`: your Google OAuth **Web client ID**.
   - `GEMINI_API_KEY`: the Gemini API key for the project.
4. Deploy the service and open its `/health` endpoint. Confirm `authConfigured` and `geminiConfigured` are both `true`.
5. Copy the service URL, e.g. `https://your-service.onrender.com`.

Keep `GEMINI_API_KEY` only in the backend host's secret environment variables. Never commit it, paste it into GitHub Pages, or add it to `config.js`. Configure usage limits and billing alerts with Google.

## C. Connect GitHub Pages to the backend

Edit the root `config.js` file in GitHub and replace the placeholders:

```js
window.EV_CONFIG = Object.freeze({
  API_BASE_URL: "https://YOUR-ACTUAL-SERVICE.onrender.com",
  GOOGLE_CLIENT_ID: "YOUR_ACTUAL_WEB_CLIENT_ID.apps.googleusercontent.com"
});
```

Commit the changes. Wait for the GitHub Pages deployment to finish, then open the public site and choose **Continue with Google**.

## D. Test before sharing

- `/health` returns `ok: true` and both configuration flags are true.
- Google sign-in redirects to `app.html` after the backend verifies the credential.
- The workspace loads the model list and returns a Gemini answer.
- An unsigned request to `/api/models` returns HTTP 401.
- A wrong Google client ID or missing Gemini key returns a setup error, not a key value.

## Current security boundaries

The backend verifies Google ID tokens, uses a server-side Gemini key, applies rate limits, validates input sizes and model names, and restricts browser CORS origins. This is a starter backend, not a complete multi-tenant SaaS: it does not include a database, persistent user profiles, per-account quotas, payments, or abuse analytics. Add those before scaling to a large public audience. Rate limiting is not a guarantee against all abuse.


## E. Enable the E.V GitHub Agent (optional)

The backend includes restricted GitHub Agent endpoints for this repository. GitHub credentials are read only from backend environment variables; they are never returned to the browser.

1. Create a **fine-grained personal access token** for only `seifhaytham275-star/E.V-MARK-X`.
2. Grant **Contents: Read and write**, **Metadata: Read-only**, and **Actions: Read-only**. The Agent does not need Admin permissions. Keep the token's expiry short enough to review and rotate it regularly.
3. In Render → your `ev-mark-x-api` service → **Environment**, set:
   - `GITHUB_TOKEN`: the token value (secret; never commit it).
   - `GITHUB_ALLOWED_EMAILS`: the exact Google sign-in email(s) permitted to use this GitHub Agent, comma-separated. Keep this to your own verified account.
   - `GITHUB_REPOSITORY`: `seifhaytham275-star/E.V-MARK-X` (already set by the Blueprint).
4. Save changes and redeploy the service.
5. Test `/api/github/status` after signing into E.V with an allowlisted Google account. This endpoint requires a valid Google ID token; opening it directly in a browser without the Authorization header is expected to fail.

### Available backend endpoints

- `GET /api/github/status` — checks access and reports the configured repository.
- `GET /api/github/contents?path=app.html&branch=main` — reads one file.
- `POST /api/github/branches` with JSON `{"name":"feature-name"}` — creates an `ev-agent/*` branch from the default branch or requested base.
- `PUT /api/github/contents` with JSON `{"path":"app.html","content":"...","message":"Explain change","branch":"ev-agent/..." }` — creates/updates a file and commits it to an `ev-agent/*` branch.
- `GET /api/github/actions?per_page=10` — lists recent workflow runs and status.

All endpoints require a valid Google sign-in and an email listed in `GITHUB_ALLOWED_EMAILS`. The repository is fixed by backend configuration. Writes are blocked for `.github/**` and `server/**` paths; the Agent must not modify workflow/security/backend files through this API. Changes go to a dedicated `ev-agent/*` branch, not directly to the default branch. Actions access is read-only: this Agent can monitor workflows but cannot dispatch or rerun them.

**Important status:** These are backend API endpoints. The current website UI does not yet contain a complete GitHub Agent panel to call them, and the Render environment values must be configured before they can work. Do not expose the token in `config.js`, client-side JavaScript, chat messages, or commits.
