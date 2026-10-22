const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const { OAuth2Client } = require("google-auth-library");

const app = express();
const PORT = Number(process.env.PORT || 8080);
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || "";
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || "";
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || "";
const GITHUB_REPOSITORY = process.env.GITHUB_REPOSITORY || "seifhaytham275-star/E.V-MARK-X";
const GITHUB_ALLOWED_EMAILS = new Set((process.env.GITHUB_ALLOWED_EMAILS || "").split(",").map(x => x.trim().toLowerCase()).filter(Boolean));
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
    .filter(m => /^gemini-/i.test(m.name));
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
  const model = typeof req.body?.model === "string" ? req.body.model.trim() : "";
  const prompt = typeof req.body?.prompt === "string" ? req.body.prompt.trim() : "";
  if (!model) return res.status(400).json({ error: "Choose a model before starting the chat." });
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


// GitHub Agent: fixed-repository access, owner-email allowlist, and branch-first writes.
const githubLimiter = rateLimit({ windowMs: 60 * 1000, limit: 30, standardHeaders: "draft-7", legacyHeaders: false });
function requireGitHubAgent(req, res, next) {
  if (!GITHUB_TOKEN) return res.status(503).json({ error: "GitHub Agent is not configured: add GITHUB_TOKEN to backend secrets." });
  if (!GITHUB_ALLOWED_EMAILS.size) return res.status(503).json({ error: "GitHub Agent is not enabled: configure GITHUB_ALLOWED_EMAILS on the backend." });
  if (!req.user?.email || !GITHUB_ALLOWED_EMAILS.has(req.user.email.toLowerCase())) {
    return res.status(403).json({ error: "This Google account is not allowed to use the GitHub Agent." });
  }
  next();
}
async function githubRequest(path, options = {}) {
  const response = await fetch("https://api.github.com" + path, {
    ...options,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: "Bearer " + GITHUB_TOKEN,
      "X-GitHub-Api-Version": "2022-11-28",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {})
    }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.message || "GitHub API request failed.");
    error.status = response.status;
    throw error;
  }
  return data;
}
function githubPath(value) {
  const path = typeof value === "string" ? value.trim().replace(/^\/+/, "") : "";
  if (!path || path.length > 400 || path.split("/").some(part => !part || part === "." || part === "..") || path.includes("\\") || path.includes("\0")) return null;
  return path;
}
function protectedWritePath(path) {
  return path === ".github" || path.startsWith(".github/") || path === "server" || path.startsWith("server/");
}
function githubError(res, error) {
  const status = [400, 401, 403, 404, 409, 422, 429].includes(error.status) ? error.status : 502;
  return res.status(status).json({ error: error.message || "GitHub operation failed." });
}
const encodedRepo = GITHUB_REPOSITORY.split("/").map(encodeURIComponent).join("/");
app.get("/api/github/status", githubLimiter, verifyGoogleCredential, requireGitHubAgent, async (_req, res) => {
  try {
    const repo = await githubRequest("/repos/" + encodedRepo);
    res.setHeader("Cache-Control", "private, no-store");
    return res.json({ ok: true, repository: repo.full_name, defaultBranch: repo.default_branch, permissions: { contents: repo.permissions?.push ? "write" : "read", actions: "read" } });
  } catch (error) { return githubError(res, error); }
});
app.get("/api/github/contents", githubLimiter, verifyGoogleCredential, requireGitHubAgent, async (req, res) => {
  const path = githubPath(req.query.path);
  if (!path) return res.status(400).json({ error: "Provide a valid repository file path." });
  try {
    const branch = typeof req.query.branch === "string" ? req.query.branch : "";
    const suffix = branch ? "?ref=" + encodeURIComponent(branch) : "";
    const data = await githubRequest("/repos/" + encodedRepo + "/contents/" + path.split("/").map(encodeURIComponent).join("/") + suffix);
    if (Array.isArray(data) || data.type !== "file") return res.status(400).json({ error: "The requested path is not a single file." });
    const content = Buffer.from(data.content || "", "base64").toString("utf8");
    res.setHeader("Cache-Control", "private, no-store");
    return res.json({ path: data.path, name: data.name, sha: data.sha, size: data.size, branch: branch || undefined, content });
  } catch (error) { return githubError(res, error); }
});
app.post("/api/github/branches", githubLimiter, verifyGoogleCredential, requireGitHubAgent, async (req, res) => {
  const label = typeof req.body?.name === "string" ? req.body.name.trim() : "ev-agent";
  const safeLabel = label.toLowerCase().replace(/[^a-z0-9._/-]+/g, "-").replace(/^[-/]+|[-/]+$/g, "").slice(0, 60) || "ev-agent";
  if (safeLabel.split("/").some(part => !part || part === "..")) return res.status(400).json({ error: "Invalid branch name." });
  try {
    const repo = await githubRequest("/repos/" + encodedRepo);
    const base = typeof req.body?.base === "string" && /^[A-Za-z0-9._/-]{1,120}$/.test(req.body.base) ? req.body.base : repo.default_branch;
    const ref = await githubRequest("/repos/" + encodedRepo + "/git/ref/heads/" + base.split("/").map(encodeURIComponent).join("/"));
    const branch = "ev-agent/" + safeLabel + "-" + Date.now().toString(36);
    const created = await githubRequest("/repos/" + encodedRepo + "/git/refs", { method: "POST", body: JSON.stringify({ ref: "refs/heads/" + branch, sha: ref.object.sha }) });
    return res.status(201).json({ ok: true, branch, base, commit: created.object.sha });
  } catch (error) { return githubError(res, error); }
});
app.put("/api/github/contents", githubLimiter, verifyGoogleCredential, requireGitHubAgent, async (req, res) => {
  const path = githubPath(req.body?.path);
  const content = typeof req.body?.content === "string" ? req.body.content : null;
  const branch = typeof req.body?.branch === "string" ? req.body.branch.trim() : "";
  const message = typeof req.body?.message === "string" ? req.body.message.trim().slice(0, 180) : "";
  if (!path || content === null || Buffer.byteLength(content, "utf8") > 900000 || !branch || !branch.startsWith("ev-agent/") || !message) {
    return res.status(400).json({ error: "A valid path, content under 900 KB, ev-agent branch, and commit message are required." });
  }
  if (protectedWritePath(path)) return res.status(403).json({ error: "Writes to backend and GitHub workflow files are blocked by the GitHub Agent safety policy." });
  try {
    const encodedPath = path.split("/").map(encodeURIComponent).join("/");
    let sha;
    try {
      const existing = await githubRequest("/repos/" + encodedRepo + "/contents/" + encodedPath + "?ref=" + encodeURIComponent(branch));
      if (!Array.isArray(existing) && existing.type === "file") sha = existing.sha;
    } catch (error) { if (error.status !== 404) throw error; }
    const payload = { message, content: Buffer.from(content, "utf8").toString("base64"), branch };
    if (sha) payload.sha = sha;
    const result = await githubRequest("/repos/" + encodedRepo + "/contents/" + encodedPath, { method: "PUT", body: JSON.stringify(payload) });
    return res.status(201).json({ ok: true, path, branch, commit: result.commit?.sha, url: result.commit?.html_url });
  } catch (error) { return githubError(res, error); }
});
app.get("/api/github/actions", githubLimiter, verifyGoogleCredential, requireGitHubAgent, async (req, res) => {
  const perPage = Math.max(1, Math.min(20, Number.parseInt(req.query.per_page, 10) || 10));
  try {
    const data = await githubRequest("/repos/" + encodedRepo + "/actions/runs?per_page=" + perPage);
    res.setHeader("Cache-Control", "private, no-store");
    return res.json({ total: data.total_count || 0, runs: (data.workflow_runs || []).map(run => ({
      id: run.id, name: run.name, workflow: run.path, branch: run.head_branch, event: run.event,
      status: run.status, conclusion: run.conclusion, createdAt: run.created_at, updatedAt: run.updated_at,
      url: run.html_url, runNumber: run.run_number
    })) });
  } catch (error) { return githubError(res, error); }
});

app.use((_req, res) => res.status(404).json({ error: "Not found." }));
app.listen(PORT, () => console.log("E.V-MARK-X API listening on port " + PORT));