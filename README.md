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
- `business.html` — E.V Business Desk preview for customer contacts, phone numbers, sales pipeline, and follow-up tasks (browser-local storage only).
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


## E.V Business Desk preview

Open [Business Desk](https://seifhaytham275-star.github.io/E.V-MARK-X/business.html) to track customer leads, phone numbers, opportunity values, stages, and follow-up tasks. The current preview stores data only in the current browser/device; it has no account sync, shared team database, billing, or automatic outbound messaging. Call and WhatsApp links are launched only after a user action. Do not store sensitive customer information in this local-only preview.


## E.V Business Manager — Roadmap

E.V is intended to grow beyond a chat interface into a business workspace that
helps customers organize daily operations. The items below distinguish the
current preview from planned production capabilities.

### Available in the current Business Desk preview

Open the [E.V Business Desk](https://seifhaytham275-star.github.io/E.V-MARK-X/business.html).

- Customer and lead records: name, company, phone, email, stage, deal value,
  next step, follow-up date, and notes.
- Sales pipeline stages: New, Contacted, Qualified, Won, and Lost.
- Search and stage filtering.
- Dashboard totals for open leads, pipeline value, won revenue, and follow-ups
  due.
- Follow-up task list with due dates and completion status.
- Click-to-call and user-triggered WhatsApp links.
- Responsive layout for desktop and mobile browsers.

**Current limitations:** this preview stores records in the browser on the
current device only. It does not yet provide cloud sync, shared team access,
secure customer accounts, automated messages, billing, or a production CRM
database. Avoid entering sensitive customer information.

### Planned: E.V Business Agent

- **Business insights:** summarize sales activity and highlight overdue
  follow-ups, stalled opportunities, and trends from data the customer provides.
- **Next-best-action suggestions:** recommend practical next steps and draft
  customer replies, quotations, and follow-up messages for review.
- **E.V Studio:** help create and update simple business websites, landing pages,
  and reusable business content.
- **Data Intelligence:** analyze customer-approved CSV and spreadsheet data,
  create summaries and charts, and explain business metrics.
- **Customer operations:** organize tasks, notes, leads, and follow-up schedules.
- **Human approval:** ask the customer before sending messages, contacting
  people, publishing changes, or performing other external actions.
- **Privacy controls:** provide clear data export and deletion, access controls,
  and a way to disconnect integrations.

These capabilities are planned; they should not be presented as available until
implemented and tested.

### Planned: accounts, cloud data, and team workspace

Before offering Business Desk as a real multi-user service, implement:

1. Secure sign-in and server-side session handling.
2. A production database with each business's records isolated from other
   customers.
3. Server-side authorization checks on every read and write.
4. Data export, account deletion, retention rules, and audit logs.
5. Backups, error monitoring, rate limits, and recovery procedures.
6. Explicit customer consent before importing contacts or connecting external
   services.

The browser-local preview is not a substitute for this production setup.

### Planned: subscriptions and automatic model selection

The intended subscription flow is:

1. Customer chooses a plan and completes payment with a supported payment
   provider.
2. The server verifies the payment using a trusted provider webhook.
3. The server activates the plan and its usage limits for that customer.
4. E.V selects an eligible AI model on the server according to the active plan,
   task requirements, usage budget, and provider availability.
5. Usage is metered and the customer can view plan status and remaining limits.
6. Failed payments, refunds, cancellations, and expired subscriptions update
   access according to the published billing policy.

**Security requirements:**

- Never send provider API keys to the browser or mobile app.
- Keep secrets in server-side environment variables or a secret manager.
- Never trust a success page or client-side payment status as proof of payment.
- Enforce plan entitlements, rate limits, and spending limits on the server.
- Do not promise a particular model or unlimited usage until provider access,
  costs, and availability have been verified.
- Choose an AI provider whose current terms allow the intended audience and
  use case. Review age requirements, privacy terms, and commercial-use terms
  before launch.

Subscription prices and included quotas remain undecided until model costs,
payment fees, and expected usage have been measured.

### Production readiness checklist

- [ ] Secure accounts and isolated cloud database.
- [ ] Authorization and privacy tests for all customer records.
- [ ] Verified payment webhooks and subscription lifecycle handling.
- [ ] Server-side model routing, quotas, usage tracking, and cost controls.
- [ ] Approved AI provider and documented provider fallback behavior.
- [ ] Customer-approved integrations and external actions.
- [ ] Backup, export, deletion, monitoring, and recovery tests.
- [ ] End-to-end tests on mobile and desktop.
- [ ] Clear terms, privacy notice, pricing, and customer support contact.
