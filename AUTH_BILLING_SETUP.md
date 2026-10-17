# E.V Accounts, GitHub, Email, and Billing Setup

## Current status

The public website is static GitHub Pages. The new `login.html` supports email/password, email magic-link, and GitHub OAuth **after** Supabase is configured. The pricing page is a proposal; it does not charge users or claim subscriptions are active.

## 1. Authentication backend

1. Create a Supabase project owned/managed by the project owner and a parent/guardian where required.
2. In Supabase Authentication > URL Configuration, set the Site URL to `https://seifhaytham275-star.github.io` and add the allowed redirect URL `https://seifhaytham275-star.github.io/E.V-MARK-X/app.html` plus `https://seifhaytham275-star.github.io/E.V-MARK-X/login.html`.
3. In Authentication > Providers, enable Email. Configure email confirmation and password rules.
4. Register a GitHub OAuth app. Set its authorization callback URL to the callback URL displayed by Supabase for the project, then enter the GitHub client ID and client secret in Supabase's GitHub provider settings. Never put the GitHub client secret in this repository.
5. Copy the Supabase project URL and the **public anon/publishable key** into `config.js` as `SUPABASE_URL` and `SUPABASE_ANON_KEY`. These are browser client settings; do not use a service-role key here.
6. Test signup, email confirmation, sign-in, magic links, GitHub sign-in, sign-out, and redirects on mobile and desktop.

## 2. Email delivery

Supabase can send auth emails for confirmation and magic links. For a public launch, configure a verified sending domain and an SMTP provider such as Resend through Supabase's SMTP settings. Keep SMTP/API secrets in the provider dashboard/server environment, never in `config.js` or frontend code. Test SPF/DKIM, delivery, rate limits, and abuse protection.

## 3. GitHub repository integration

GitHub sign-in proves identity; it does not by itself grant repository access. For repo browsing or changes, register a GitHub App and request the minimum permissions needed. Prefer read-only Contents and Metadata permissions for the first release. Keep the GitHub App private key/client secret server-side, use short-lived installation/user tokens, validate webhook signatures, and require explicit confirmation before creating commits or pull requests. Never request or store a user's personal access token in the browser.

## 4. Pricing and subscriptions

Suggested price experiments only:
- Free: $0/month
- Developer: $3.99/month
- Pro: $7.99/month

These are not validated market prices and are not live. Before checkout:
- Choose a payment provider available for the project's country and legal owner; a parent/guardian should handle contracts and payment accounts where required.
- Use a server-side checkout session and verify payment webhooks.
- Store plan entitlements in a database; do not trust the browser's chosen plan or a redirect success page.
- Add per-user request/token limits, per-model cost accounting, rate limits, spending caps, and a hard stop when limits are reached.
- Define refund, cancellation, privacy, and support policies.
- Test duplicate webhook delivery, failed payments, expired subscriptions, refunds, and account deletion.

## 5. AI provider and launch gate

Do not launch a paid public AI service until the selected provider's current API terms permit the service owner and the intended audience. The current workspace sends Gemini requests directly from each user's browser using their own API key; this is not a safe architecture for selling a bundled subscription. A paid plan needs a server-side AI gateway with quotas and secrets held only on the server. Confirm the provider's age and audience requirements before launch.

## 6. Launch checklist

- [ ] Supabase credentials configured without service-role secrets in the frontend
- [ ] Email and GitHub auth fully tested
- [ ] Database row-level security policies reviewed
- [ ] GitHub App permissions limited and OAuth secrets kept server-side
- [ ] Email domain verified and delivery tested
- [ ] Payment webhook verified server-side
- [ ] Usage limits and cost alerts tested
- [ ] Privacy policy, terms, refund/cancellation and support details published
- [ ] Provider terms reviewed for the intended audience
- [ ] Production build tested on Android and desktop
