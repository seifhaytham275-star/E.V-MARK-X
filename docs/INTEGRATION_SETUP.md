# E.V-MARK-X Client Integrations Setup

This guide covers Gmail replies, Facebook/Instagram messaging, and an optional feminine browser voice. Creating credentials does not itself activate an integration; the backend routes and OAuth callbacks must also be implemented and deployed.

## 1. Create a local environment file

Copy `.env.example` to `.env` for local backend development. Never commit `.env` or place server secrets in browser JavaScript. For deployment, configure the same variables in your hosting provider's server-side Environment Variables panel.

## 2. Google sign-in and Gmail

1. Open https://console.cloud.google.com/ and create/select the E.V project.
2. Configure Google Auth Platform branding, audience, and contact details.
3. Enable Gmail API in APIs & Services → Library.
4. Create a Web OAuth client and add the deployed site's origin under Authorized JavaScript origins. Add the exact backend callback URL under Authorized redirect URIs.
5. During development, add your own Google account as a test user if the app is in testing mode.
6. Request only the scopes needed. Start with read-only access; add send permission only if E.V will send replies.
7. Implement the OAuth authorization-code flow on the backend, validate the callback/state, securely store refresh tokens, and expose authenticated endpoints to the UI.
8. Test listing a message and drafting a reply before enabling actual sending.

Official docs:
- https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid
- https://developers.google.com/workspace/gmail/api/auth/web-server
- https://developers.google.com/workspace/gmail/api/auth/scopes

## 3. Facebook and Instagram messaging

1. Open https://developers.facebook.com/ and create a Meta app.
2. Add the relevant Instagram/Facebook products and follow their setup prompts.
3. Use an Instagram Professional account and link the required Facebook Page when required by the chosen API flow.
4. Configure OAuth redirect URLs to match the deployed backend exactly.
5. Request only the permissions for the supported messaging and account-management features. Some permissions require App Review and/or business verification.
6. Configure webhooks and verify webhook signatures before processing inbound events.
7. Store tokens on the server; never put app secrets or long-lived tokens in public frontend code.
8. Test with an authorized test account first.

Official docs:
- https://developers.facebook.com/docs/instagram-platform/
- https://developers.facebook.com/docs/graph-api/
- https://developers.facebook.com/docs/messenger-platform/

## 4. Recommended implementation order

- Phase A: Google Sign-In, backend session, and logout.
- Phase B: Gmail message list, compose/draft, and explicit-send confirmation.
- Phase C: Meta OAuth, inbound webhooks, and Instagram/Facebook messaging for accounts that have granted access.
- Phase D: client inbox, conversation history, status labels, and audit trail.

## 5. Voice assistant

The companion file `voice-assistant.js` is a browser-only voice helper using SpeechSynthesis and SpeechRecognition when available. Browser voice options vary by device and installed voices; it attempts to select a feminine-sounding voice but cannot guarantee that every device provides one. It does not send microphone audio to an AI service or create an AI conversation by itself.

## 6. Safety and production checklist

- Keep `.env` out of Git; `.env.example` must contain placeholders only.
- Use HTTPS, OAuth state/CSRF protection, secure server-side sessions, and least-privilege scopes.
- Validate webhook signatures and rate-limit API routes.
- Require a confirmation before sending messages, deleting data, or publishing content.
- Provide disconnect/revoke controls and a way to delete stored customer data.
- Do not claim an integration is live until an end-to-end test succeeds.
