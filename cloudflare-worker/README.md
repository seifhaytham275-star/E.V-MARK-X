# E.V-MARK-X Cloudflare Worker API

This folder is a Cloudflare Workers-compatible version of the E.V API. The existing GitHub Pages site and the original `server/` Node/Express API are kept in place.

## Cloudflare deploy

1. In Cloudflare Dashboard, open **Workers & Pages** and select **Create application**.
2. Choose the GitHub option and connect `seifhaytham275-star/E.V-MARK-X`.
3. Configure the project root directory as `cloudflare-worker`. Keep the Wrangler config file enabled.
4. Save and deploy. The worker name must be `ev-mark-x-api`, matching `wrangler.toml`.
5. Open the Worker URL ending in `.workers.dev/health`. It should return JSON with `"ok": true`.

Cloudflare Git integration can deploy each new commit automatically. If the dashboard first creates a blank Worker instead, open its **Settings → Builds** and connect this repository, with `cloudflare-worker` as the root directory.

## Configure Worker variables and secrets

In **Workers & Pages → ev-mark-x-api → Settings → Variables and Secrets**, add:

| Name | Type | Value |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | Variable | Your existing Google OAuth Web client ID |
| `GITHUB_TOKEN` | Secret | Your GitHub fine-grained token |
| `GITHUB_ALLOWED_EMAILS` | Secret | The Google sign-in email allowed to use the GitHub Agent |
| `GEMINI_API_KEY` | Secret (optional) | Only if you want the backend to provide a shared Gemini key |

`FRONTEND_ORIGIN`, `GITHUB_REPOSITORY`, and `MODELS_CACHE_SECONDS` are defined in `wrangler.toml`. Deploy after changing variables/secrets. If you use your own Gemini key in E.V Settings, you can leave the Worker-level `GEMINI_API_KEY` unset.

Never paste a token into a browser field, `config.js`, or a committed file. Grant the GitHub token only the minimum repository permissions needed for contents read/write and Actions read.

## Connect GitHub Pages to the Worker

After the Worker deploys, copy its base URL, for example `https://ev-mark-x-api.<your-account-subdomain>.workers.dev`. Do not use the example literally; use the URL shown in your Cloudflare dashboard.

In the root-level `config.js`, set:
- `API_BASE_URL` to the actual Worker base URL, without a trailing slash.
- `GOOGLE_CLIENT_ID` to the same OAuth client ID configured on the Worker.

In Google Cloud Console, add the GitHub Pages site origin `https://seifhaytham275-star.github.io` to the OAuth client's authorized JavaScript origins if it is not already there. The OAuth client ID is public; the GitHub token is not.

## API routes

- `GET /health`
- `POST /api/auth/google`
- `GET /api/models`
- `POST /api/chat`
- `GET /api/github/status`
- `GET /api/github/contents?path=app.html&branch=main`
- `POST /api/github/branches`
- `PUT /api/github/contents`
- `GET /api/github/actions?per_page=10`

Google credentials are verified against Google's rotating RSA signing keys using Web Crypto. GitHub writes are branch-first and are limited to `ev-agent/*`; writes to `.github/`, `server/`, and `cloudflare-worker/` are blocked by the Worker safety policy. Actions are read-only.

## Free-plan note

Cloudflare Workers Free currently has daily request and CPU limits; check Cloudflare's current limits page before production use. These APIs call Google and GitHub, so their own quotas and permissions still apply.
