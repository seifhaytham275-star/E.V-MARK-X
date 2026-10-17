const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");

const server = fs.readFileSync("server/index.js", "utf8");
const adapter = fs.readFileSync("server/whatsapp.js", "utf8");
const render = fs.readFileSync("render.yaml", "utf8");
const setup = fs.readFileSync("WEB_BACKEND_SETUP.md", "utf8");
const render = fs.readFileSync("render.yaml", "utf8");

test("WhatsApp webhook is mounted before JSON parsing so signature verification sees raw bytes", () => {
  const mount = server.indexOf('app.use("/api/webhooks/whatsapp"');
  const jsonParser = server.indexOf('app.use(express.json({ limit: "32kb" }))');
  assert.ok(mount >= 0 && jsonParser > mount);
  assert.match(server, /express\.raw\(\{ type: "application\/json", limit: "1mb" \}\)/);
});

test("WhatsApp webhook validates Meta signatures and subscription challenges", () => {
  assert.match(adapter, /x-hub-signature-256/);
  assert.match(adapter, /createHmac\("sha256", APP_SECRET\)/);
  assert.match(adapter, /timingSafeEqual/);
  assert.match(adapter, /hub\.verify_token/);
  assert.match(adapter, /hub\.challenge/);
});

test("WhatsApp replies use only the configured Gemini model and avoid logging message content", () => {
  assert.match(adapter, /const MODEL_ID = "gemini-3\.5-flash-lite"/);
  assert.match(adapter, /message\.type !== "text"/);
  assert.match(adapter, /Do not log message bodies, phone numbers, access tokens/);
  assert.doesNotMatch(adapter, /console\.log\([^\n]*(message\.text|ACCESS_TOKEN|APP_SECRET)/);
});

test("WhatsApp credentials are environment-only and setup docs state initial scope", () => {
  for (const key of ["WHATSAPP_VERIFY_TOKEN", "WHATSAPP_APP_SECRET", "WHATSAPP_ACCESS_TOKEN", "WHATSAPP_PHONE_NUMBER_ID"]) {
    assert.match(render, new RegExp(key));
    assert.match(adapter, new RegExp("process\\.env\\." + key));
  }
  assert.match(setup, /incoming text messages and automated text replies only/);
  assert.match(setup, /does not implement outbound campaigns, media messages, persistence-based deduplication, or voice\/telephone calls/);
});


test("External integrations remain disabled by default while their code is retained", () => {
  assert.match(adapter, /process\.env\.INTEGRATIONS_ENABLED === "true"/);
  assert.match(adapter, /if \(!INTEGRATIONS_ENABLED\) return res\.status\(503\)/);
  assert.match(render, /key: INTEGRATIONS_ENABLED\n\s+value: "false"/);
  assert.match(setup, /code and configuration are intentionally retained/);
  assert.match(setup, /does not disable the ordinary E\.V website chat/);
});
