const crypto = require("node:crypto");
const express = require("express");

const router = express.Router();
const MODEL_ID = "gemini-3.5-flash-lite";
// External integrations stay paused by default until the owner explicitly enables them.
const INTEGRATIONS_ENABLED = process.env.INTEGRATIONS_ENABLED === "true";
const META_GRAPH_VERSION = process.env.META_GRAPH_VERSION || "v23.0";
const VERIFY_TOKEN = process.env.WHATSAPP_VERIFY_TOKEN || "";
const APP_SECRET = process.env.WHATSAPP_APP_SECRET || "";
const ACCESS_TOKEN = process.env.WHATSAPP_ACCESS_TOKEN || "";
const PHONE_NUMBER_ID = process.env.WHATSAPP_PHONE_NUMBER_ID || "";
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const seenMessages = new Map();

function timingSafeSignatureMatch(rawBody, headerValue) {
  if (!APP_SECRET || !Buffer.isBuffer(rawBody) || typeof headerValue !== "string") return false;
  const match = headerValue.match(/^sha256=([a-f0-9]{64})$/i);
  if (!match) return false;
  const expected = crypto.createHmac("sha256", APP_SECRET).update(rawBody).digest();
  const actual = Buffer.from(match[1], "hex");
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
}

function rememberMessage(id) {
  const now = Date.now();
  for (const [key, timestamp] of seenMessages) if (now - timestamp > 6 * 60 * 60 * 1000) seenMessages.delete(key);
  if (seenMessages.has(id)) return false;
  seenMessages.set(id, now);
  if (seenMessages.size > 5000) seenMessages.delete(seenMessages.keys().next().value);
  return true;
}

async function createReply(message) {
  if (!GEMINI_API_KEY) throw new Error("AI backend is not configured");
  const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID + ":generateContent", {
    method: "POST",
    headers: { "content-type": "application/json", "x-goog-api-key": GEMINI_API_KEY },
    signal: AbortSignal.timeout(20000),
    body: JSON.stringify({
      system_instruction: { parts: [{ text: "You are E.V, a helpful AI assistant. Reply concisely in the language used by the user. You are replying over WhatsApp. Never claim to have performed an external action unless a connected tool confirms it. Do not request passwords, API keys, or verification codes." }] },
      contents: [{ role: "user", parts: [{ text: message.slice(0, 4000) }] }],
      generationConfig: { maxOutputTokens: 600 }
    })
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error("Gemini request failed with status " + response.status);
  const text = (data.candidates?.[0]?.content?.parts || []).map(part => part.text || "").join("").trim();
  if (!text) throw new Error("Gemini returned an empty reply");
  return text.slice(0, 4000);
}

async function sendWhatsAppText(to, text) {
  if (!ACCESS_TOKEN || !PHONE_NUMBER_ID) throw new Error("WhatsApp Cloud API is not configured");
  const response = await fetch("https://graph.facebook.com/" + META_GRAPH_VERSION + "/" + encodeURIComponent(PHONE_NUMBER_ID) + "/messages", {
    method: "POST",
    headers: { authorization: "Bearer " + ACCESS_TOKEN, "content-type": "application/json" },
    signal: AbortSignal.timeout(15000),
    body: JSON.stringify({
      messaging_product: "whatsapp",
      recipient_type: "individual",
      to,
      type: "text",
      text: { preview_url: false, body: text }
    })
  });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error("WhatsApp send failed with status " + response.status + (data.error?.code ? " (provider code " + data.error.code + ")" : ""));
  }
}

async function processWebhook(payload) {
  for (const entry of payload.entry || []) {
    for (const change of entry.changes || []) {
      const value = change.value || {};
      for (const message of value.messages || []) {
        if (!message.id || !message.from || message.type !== "text" || !message.text?.body) continue;
        if (!rememberMessage(message.id)) continue;
        try {
          const reply = await createReply(message.text.body);
          await sendWhatsAppText(message.from, reply);
        } catch (error) {
          // Do not log message bodies, phone numbers, access tokens, or provider response bodies.
          console.error("[whatsapp] message processing failed:", error.message);
        }
      }
    }
  }
}

router.get("/", (req, res) => {
  const mode = req.query["hub.mode"];
  const token = req.query["hub.verify_token"];
  const challenge = req.query["hub.challenge"];
  if (!INTEGRATIONS_ENABLED) return res.status(503).send("External integrations are temporarily paused.");
  if (!VERIFY_TOKEN) return res.status(503).send("WhatsApp webhook is not configured.");
  if (mode === "subscribe" && typeof token === "string" && token === VERIFY_TOKEN && typeof challenge === "string") {
    return res.status(200).type("text/plain").send(challenge);
  }
  return res.sendStatus(403);
});

router.post("/", (req, res) => {
  if (!INTEGRATIONS_ENABLED) return res.status(503).send("External integrations are temporarily paused.");
  const rawBody = req.body;
  if (!Buffer.isBuffer(rawBody)) return res.sendStatus(400);
  if (!timingSafeSignatureMatch(rawBody, req.get("x-hub-signature-256"))) return res.sendStatus(401);
  let payload;
  try { payload = JSON.parse(rawBody.toString("utf8")); }
  catch { return res.sendStatus(400); }
  // Acknowledge Meta promptly; message processing happens after the webhook is acknowledged.
  res.sendStatus(200);
  setImmediate(() => { processWebhook(payload).catch(error => console.error("[whatsapp] webhook processing failed:", error.message)); });
});

router.get("/status", (_req, res) => {
  res.setHeader("Cache-Control", "no-store");
  res.json({
    enabled: INTEGRATIONS_ENABLED,
    configured: Boolean(INTEGRATIONS_ENABLED && VERIFY_TOKEN && APP_SECRET && ACCESS_TOKEN && PHONE_NUMBER_ID && GEMINI_API_KEY),
    provider: "WhatsApp Cloud API",
    mode: "text-only inbound replies",
    model: MODEL_ID
  });
});

module.exports = { router };
