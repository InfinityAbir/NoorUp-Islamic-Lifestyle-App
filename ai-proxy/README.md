# NoorUp AI proxy (Cloudflare Worker)

Powers the Islamic Assistant tab with Google Gemini. The Gemini API key lives only here (as a Worker
secret), never in the APK. The app sends a structured chat request (`tool` = `chat`, plus `question`,
`lang` and a short `history`); the Worker owns the system prompt, rate-limits per device and per IP,
caps input size, and falls back across Gemini models. The app cannot send its own prompt or system
instruction.

If the Worker is unreachable, not configured, or rate limited, the app falls back to its built-in
offline answers, so the Assistant never goes dead.

## Deploy (once)

Needs a free Cloudflare account (https://dash.cloudflare.com/sign-up) and Node 18+.

```bash
cd ai-proxy
npm install
npx wrangler login                       # opens the browser, approve access
npx wrangler secret put GEMINI_API_KEY   # paste a dedicated key from https://aistudio.google.com/apikey
npx wrangler deploy                      # prints https://noorup-ai.<your-subdomain>.workers.dev
```

Then put that URL in the repo-root `gradle.properties` and rebuild the app:

```
aiProxyUrl=https://noorup-ai.<your-subdomain>.workers.dev
```

Check it: open `<url>/health` in a browser, it returns `{"ok":true}`.

## Notes

- Use a dedicated Gemini key for this Worker. Rotate it from AI Studio if abused.
- Free Gemini quota is shared by all users. When it runs out the app shows a short notice and uses
  its offline answers.
- Cloudflare free plan: 100,000 Worker requests/day. Rate limits are per Cloudflare location and best-effort.
- User questions are forwarded to Google Gemini. Nothing is logged by this code. Do not enable
  request-body logging in the Cloudflare dashboard. Declare this in the Play Data safety form.
- Hardening later: add Play Integrity attestation so only genuine installs of the app can call the Worker.
- Tests: `npm test`
