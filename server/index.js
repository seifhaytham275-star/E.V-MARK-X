const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const { OAuth2Client } = require("google-auth-library");

const app = express();
const PORT = Number(process.env.PORT || 8080);
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || "";
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const MODEL_ID = "gemini-3.1-flash-lite";
const FRONTEND_ORIGIN = process.env.FRONTEND_ORIGIN || "https://seifhaytham275-star.github.io";
const EXTRA_ALLOWED_ORIGINS = (process.env.EXTRA_ALLOWED_ORIGINS || "").split(",").map(x => x.trim()).filter(Boolean);
const ALLOWED_ORIGINS = new Set([FRONTEND_ORIGIN, ...EXTRA_ALLOWED_ORIGINS]);
const googleClient = new OAuth2Client(GOOGLE_CLIENT_ID);
const MODEL_CACHE_MS = Math.max(30, Number(process.env.MODELS_CACHE_SECONDS || 300)) * 1000;
let modelCache = { expires: 0, models: [] };

app.disable("x-powered-by");
app.set("trust proxy", 1);
app.use(helmet({ crossOriginResourcePolicy: { policy: "cross-origin" } }));
app.use((req, res, next) => {
  const origin = req.get("origin");
  if (origin && ALLOWED_ORIGINS.has(origin)) {
    res.setHeader("Access-Control-Allow-Origin", origin);
    res.setHeader("Vary", "Origin");
    res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  }
  if (req.method === "OPTIONS") return res.sendStatus(origin && ALLOWED_ORIGINS.has(origin) ? 204 : 403);
  next();
});
app.use(express.json({ limit: "32kb" }));

const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: "draft-7", legacyHeaders: false });
const apiLimiter = rateLimit({ windowMs: 60 * 1000, limit: 25, standardHeaders: "draft-7", legacyHeaders: false, message: { error: "Too many requests. Please wait a moment and try again." } });
const chatLimiter = rateLimit({ windowMs: 60 * 1000, limit: 10, standardHeaders: "draft-7", legacyHeaders: false, message: { error: "Chat rate limit reached. Please wait before sending another message." } });

function requireConfig(res) {
  if (!GOOGLE_CLIENT_ID) { res.status(503).json({ error: "Backend setup required: configure GOOGLE_CLIENT_ID." }); return false; }
  if (!GEMINI_API_KEY) { res.status(503).json({ error: "Backend setup required: configure GEMINI_API_KEY on the backend host." }); return false; }
  return true;
}

async function verifyGoogleCredential(req, res, next) {
  try {
    if (!GOOGLE_CLIENT_ID) return res.status(503).json({ error: "Google sign-in is not configured on the backend yet." });
    const header = req.get("authorization") || "";
    const match = header.match(/^Bearer\s+(.+)$/i);
    if (!match) return res.status(401).json({ error: "Please sign in with Google to continue." });
    const ticket = await googleClient.verifyIdToken({ idToken: match[1], audience: GOOGLE_CLIENT_ID });
    const payload = ticket.getPayload();
    if (!payload || !payload.sub || !payload.email_verified) return res.status(401).json({ error: "Google account could not be verified." });
    req.user = { id: payload.sub, email: payload.email || "", name: payload.name || "Google user", picture: payload.picture || "" };
    next();
  } catch (error) {
    return res.status(401).json({ error: "Your Google sign-in has expired or is invalid. Please sign in again." });
  }
}

app.get("/health", (_req, res) => res.json({ ok: true, service: "E.V-MARK-X API", authConfigured: Boolean(GOOGLE_CLIENT_ID), geminiConfigured: Boolean(GEMINI_API_KEY) }));
app.post("/api/auth/google", authLimiter, async (req, res) => {
  if (!GOOGLE_CLIENT_ID) return res.status(503).json({ error: "Google sign-in is not configured on the backend yet." });
  const credential = typeof req.body?.credential === "string" ? req.body.credential : "";
  if (!credential || credential.length > 10000) return res.status(400).json({ error: "A valid Google credential is required." });
  try {
    const ticket = await googleClient.verifyIdToken({ idToken: credential, audience: GOOGLE_CLIENT_ID });
    const p = ticket.getPayload();
    if (!p || !p.sub || !p.email_verified) return res.status(401).json({ error: "Use a Google account with a verified email address." });
    return res.json({ ok: true, user: { id: p.sub, name: p.name || "Google user", email: p.email || "", picture: p.picture || "" } });
  } catch (_e) {
    return res.status(401).json({ error: "Google sign-in verification failed. Check the OAuth client ID and try again." });
  }
});

async function getModels(apiKey = GEMINI_API_KEY) {
  if (apiKey === GEMINI_API_KEY && modelCache.expires > Date.now() && modelCache.models.length) return modelCache.models;
  const url = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100";
  const response = await fetch(url, { headers: { "x-goog-api-key": apiKey } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error?.message || "Could not retrieve Gemini models.");
  const models = (data.models || [])
    .filter(m => Array.isArray(m.supportedGenerationMethods) && m.supportedGenerationMethods.includes("generateContent"))
    .map(m => ({ name: (m.name || "").replace(/^models\//, ""), displayName: m.displayName || m.name || "Gemini model", description: m.description || "" }))
    .filter(m => m.name === MODEL_ID);
  models.sort((a, b) => {
    const rank = n => { n = n.toLowerCase(); if (n.includes("flash-lite")) return 2; if (n.includes("flash")) return 0; if (n.includes("pro")) return 1; return 3; };
    return rank(a.name) - rank(b.name) || a.name.localeCompare(b.name);
  });
  if (apiKey === GEMINI_API_KEY) modelCache = { expires: Date.now() + MODEL_CACHE_MS, models };
  return models;
}

app.get("/api/models", apiLimiter, verifyGoogleCredential, async (req, res) => {
  const apiKey = (req.get("x-ev-gemini-key") || GEMINI_API_KEY || "").trim();
  if (!apiKey) return res.status(400).json({ error: "Add your Gemini API key in Settings." });
  try { res.setHeader("Cache-Control", "private, no-store"); res.json({ models: await getModels(apiKey) }); }
  catch (error) { res.status(502).json({ error: error.message || "Could not load models." }); }
});

app.post("/api/chat", chatLimiter, verifyGoogleCredential, async (req, res) => {
  const apiKey = (req.get("x-ev-gemini-key") || GEMINI_API_KEY || "").trim();
  if (!apiKey) return res.status(400).json({ error: "Add your Gemini API key in Settings." });
  const model = MODEL_ID;
  const prompt = typeof req.body?.prompt === "string" ? req.body.prompt.trim() : "";
  if (!prompt || prompt.length > 12000) return res.status(400).json({ error: "Enter a message up to 12,000 characters." });
  try {
    const models = await getModels(apiKey);
    if (!models.some(m => m.name === model)) return res.status(400).json({ error: "That model is not available for this API key. Refresh the model list." });
    const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/" + encodeURIComponent(model) + ":generateContent", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
      body: JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }] }] })
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) return res.status(response.status === 429 ? 429 : 502).json({ error: data.error?.message || "Gemini request failed." });
    const answer = (data.candidates?.[0]?.content?.parts || []).map(p => p.text || "").join("").trim();
    if (!answer) return res.status(502).json({ error: "Gemini returned no text for this request." });
    res.setHeader("Cache-Control", "no-store");
    return res.json({ answer, model });
  } catch (error) { return res.status(502).json({ error: error.message || "The AI service is temporarily unavailable." }); }
});

app.use((_req, res) => res.status(404).json({ error: "Not found." }));
app.listen(PORT, () => console.log("E.V-MARK-X API listening on port " + PORT));