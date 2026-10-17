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


## E. WhatsApp Business Cloud API (inbound text replies)

The repository includes a server-side WhatsApp webhook adapter at `/api/webhooks/whatsapp`. It verifies Meta's `X-Hub-Signature-256` using the raw request body, validates webhook subscription challenges, ignores non-text messages, deduplicates recent message IDs in memory, and uses the fixed `gemini-3.5-flash-lite` model for text replies. It does not log message bodies, phone numbers, access tokens, or provider response bodies.

1. Create/configure a Meta app and WhatsApp Business Account in Meta for Developers.
2. Configure the WhatsApp product and webhook callback URL as `https://YOUR-BACKEND-HOST/api/webhooks/whatsapp`.
3. Subscribe to the `messages` webhook field.
4. Set these secrets in the backend host's environment settings (never in GitHub Pages or source control):
   - `WHATSAPP_VERIFY_TOKEN`: a long random value you choose and enter in Meta's webhook setup.
   - `WHATSAPP_APP_SECRET`: the Meta app secret used to verify webhook signatures.
   - `WHATSAPP_ACCESS_TOKEN`: a valid server-side WhatsApp Cloud API access token.
   - `WHATSAPP_PHONE_NUMBER_ID`: the WhatsApp phone-number ID from Meta, not the displayed phone number.
   - `GEMINI_API_KEY`: server-side Gemini API key.
   - Optional `META_GRAPH_VERSION`: the supported Graph API version for your Meta app.
5. Deploy, then open `/api/webhooks/whatsapp/status` to confirm the server reports whether required values are present. The status endpoint reveals only boolean configuration state, not secrets.
6. Test with a number allowed by the Meta test setup before inviting real users. Confirm Meta's current message window, template, opt-in, business verification, and Egypt-specific account/number rules before production use.

This first adapter supports **incoming text messages and automated text replies only**. It does not implement outbound campaigns, media messages, persistence-based deduplication, or voice/telephone calls. Voice calling and ordinary Egyptian mobile calls require a separately selected, region-eligible provider and additional consent/cost controls. The status endpoint is informational; it is not a substitute for securing the backend with authentication and provider controls.

## Integration status

- WhatsApp webhook: code scaffolded; requires Meta credentials and a deployed backend for live verification.
- GitHub: no live OAuth/App installation is configured by this change.
- Gmail and Google Drive: require a separate Google OAuth authorization-code flow with least-privilege scopes and a secure token store; Google sign-in alone does not grant mailbox/Drive API access.
- Web Search: the existing workspace's optional Gemini Google Search grounding remains separate from third-party web-search providers.
