# E.V-MARK-X Backend Setup

The GitHub Pages website is static. The GitHub Agent needs a backend so the GitHub token stays server-side. The repository now contains a Cloudflare Workers version of the API in `cloudflare-worker/`, while the original Node/Express API in `server/` remains available as an alternative.

## A. Create Google OAuth credentials

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Select or create a project and configure the Google Auth Platform / OAuth consent screen.
3. Create an **OAuth client ID** with application type **Web application**.
4. Add this exact **Authorized JavaScript origin**: `https://seifhaytham275-star.github.io` (no repository path).
5. Copy the client ID. The website uses the public client ID, not a client secret. The Worker verifies each Google ID token.
6. If the OAuth app is in Testing mode, add the Google account allowed to test it.

## B. Deploy the Cloudflare Worker (recommended for the no-card goal)

1. Sign in at [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. Open **Workers & Pages** and select **Create application**.
3. Choose **GitHub**, authorize the Cloudflare GitHub app if prompted, and select `seifhaytham275-star/E.V-MARK-X`.
4. Set the project root directory to `cloudflare-worker` so Cloudflare reads `cloudflare-worker/wrangler.toml`. Keep the Worker name `ev-mark-x-api`, which must match the `name` in `wrangler.toml`.
5. Save and deploy. Open the Worker URL plus `/health`; it should respond with JSON containing `"ok": true`. A false `authConfigured` value simply means you still need to set the Google client ID.

Cloudflare Workers Builds can deploy new commits automatically. If Cloudflare creates a blank Worker first, connect the Git repository under the Worker **Settings → Builds**, with `cloudflare-worker` as the root directory.

## C. Add Worker variables and secrets

Open **Workers & Pages → ev-mark-x-api → Settings → Variables and Secrets**. Add the items below, then deploy the changes.

| Name | Type | Value |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | Variable | Your Google OAuth Web client ID |
| `GITHUB_TOKEN` | Secret | Your fine-grained GitHub token |
| `GITHUB_ALLOWED_EMAILS` | Secret | The exact verified Google sign-in email allowed to use the GitHub Agent |
| `GEMINI_API_KEY` | Secret (optional) | Only if you want the backend to supply a shared Gemini key |

The repository and default CORS origin are already configured in `cloudflare-worker/wrangler.toml`. The E.V browser workspace can continue using a visitor's own Gemini key, so `GEMINI_API_KEY` may remain unset if that is your intended setup.

**Never paste secrets into chat, `config.js`, or a committed file.** A fine-grained GitHub token should be limited to this repository with **Contents: Read and write**, **Metadata: Read-only**, and **Actions: Read-only**. Do not grant organization or administration permissions.

## D. Connect the website to the Worker

After deployment, copy the actual `.workers.dev` base URL from Cloudflare. Do not use an example URL; use the one shown in your dashboard.

Edit the root-level `config.js` in GitHub and set its public configuration like this, using your real values:

```js
window.EV_CONFIG = Object.freeze({
  API_BASE_URL: "https://YOUR-ACTUAL-WORKER.workers.dev",
  GOOGLE_CLIENT_ID: "YOUR_ACTUAL_WEB_CLIENT_ID.apps.googleusercontent.com",
  SUPABASE_URL: "",
  SUPABASE_ANON_KEY: ""
});
```

Commit the update and wait for the GitHub Pages workflow. In Google Cloud Console, confirm the OAuth client's authorized JavaScript origin includes `https://seifhaytham275-star.github.io`. The Google client ID is public; the GitHub token is not.

## E. Test the GitHub Agent

1. Open [the GitHub Agent console](https://seifhaytham275-star.github.io/E.V-MARK-X/github-agent.html).
2. Sign in with the Google account that exactly matches `GITHUB_ALLOWED_EMAILS`.
3. Click **Check connection**. The repository and permissions should appear.
4. Create a working branch, read a file, and commit a small safe test change to that `ev-agent/*` branch.
5. Click **Refresh Actions** to see recent workflow runs.

A direct browser visit to `/api/github/status` is expected to require an Authorization header and does not by itself prove the UI is configured.

## F. API routes and security boundaries

- `GET /health`
- `POST /api/auth/google`
- `GET /api/models`
- `POST /api/chat`
- `GET /api/github/status`
- `GET /api/github/contents?path=app.html&branch=main`
- `POST /api/github/branches`
- `PUT /api/github/contents`
- `GET /api/github/actions?per_page=10`

Google ID tokens are verified against Google's rotating RSA signing keys using Web Crypto. GitHub changes are committed only to `ev-agent/*` branches. Writes to `.github/**`, `server/**`, and `cloudflare-worker/**` are blocked; Actions access is read-only. Review changes before merging them.

The Cloudflare Worker Free plan has usage limits, including daily request and CPU limits. Check [Cloudflare's current limits](https://developers.cloudflare.com/workers/platform/limits/) before depending on it for high-volume use. This starter API is not yet a full SaaS backend: it does not provide subscription entitlements, persistent per-user quotas, a database, or abuse analytics.

## Alternative: deploy the original Node backend

The original Express backend and `render.yaml` remain in the repository. That setup is documented in the Render-specific configuration, but use the Cloudflare Worker steps above for this no-card deployment attempt. Do not enter payment details just to continue this setup.
