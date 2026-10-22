// E.V-MARK-X GitHub Agent + Gemini API for Cloudflare Workers.
// Secrets belong in Cloudflare Dashboard, never in this repository.
let googleJwksCache = { expiresAt: 0, keys: [] };
let modelCache = { expiresAt: 0, models: [] };

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...headers }
  });
}

function withHeaders(response, baseHeaders) {
  const headers = new Headers(response.headers);
  for (const [key, value] of Object.entries(baseHeaders)) {
    if (value !== undefined && value !== null) headers.set(key, value);
  }
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers
  });
}

function allowedOrigins(env) {
  return new Set([
    env.FRONTEND_ORIGIN || "https://seifhaytham275-star.github.io",
    ...(env.EXTRA_ALLOWED_ORIGINS || "").split(",").map((x) => x.trim()).filter(Boolean)
  ]);
}

function b64urlBytes(value) {
  let s = value.replace(/-/g, "+").replace(/_/g, "/");
  s += "=".repeat((4 - (s.length % 4)) % 4);
  const binary = atob(s);
  const output = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) output[i] = binary.charCodeAt(i);
  return output;
}

function decodeB64UrlJson(value) {
  return JSON.parse(decoder.decode(b64urlBytes(value)));
}

async function fetchGoogleJwks(force = false) {
  if (!force && googleJwksCache.expiresAt > Date.now() && googleJwksCache.keys.length) {
    return googleJwksCache.keys;
  }
  const response = await fetch("https://www.googleapis.com/oauth2/v3/certs", {
    headers: { Accept: "application/json" }
  });
  if (!response.ok) throw new Error("Google signing keys could not be retrieved.");
  const data = await response.json();
  const maxAge = Number((response.headers.get("cache-control") || "").match(/max-age=(\d+)/i)?.[1] || 300);
  googleJwksCache = {
    keys: Array.isArray(data.keys) ? data.keys : [],
    expiresAt: Date.now() + Math.max(60, Math.min(maxAge, 21600)) * 1000
  };
  return googleJwksCache.keys;
}

async function verifyGoogleIdToken(token, env) {
  const clientId = (env.GOOGLE_CLIENT_ID || "").trim();
  if (!clientId) return { error: "Google sign-in is not configured on the Worker.", status: 503 };

  const pieces = token.split(".");
  if (pieces.length !== 3) return { error: "Invalid Google credential.", status: 401 };

  try {
    const header = decodeB64UrlJson(pieces[0]);
    const payload = decodeB64UrlJson(pieces[1]);
    if (header.alg !== "RS256" || !header.kid) return { error: "Unsupported Google credential.", status: 401 };

    let keys = await fetchGoogleJwks();
    let jwk = keys.find((key) => key.kid === header.kid && key.kty === "RSA");
    if (!jwk) {
      keys = await fetchGoogleJwks(true);
      jwk = keys.find((key) => key.kid === header.kid && key.kty === "RSA");
    }
    if (!jwk) return { error: "Google credential signing key was not found.", status: 401 };

    const publicKey = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"]
    );
    const signatureValid = await crypto.subtle.verify(
      { name: "RSASSA-PKCS1-v1_5" },
      publicKey,
      b64urlBytes(pieces[2]),
      encoder.encode(pieces[0] + "." + pieces[1])
    );
    if (!signatureValid) return { error: "Google credential signature is invalid.", status: 401 };

    const now = Math.floor(Date.now() / 1000);
    const audienceOk = Array.isArray(payload.aud) ? payload.aud.includes(clientId) : payload.aud === clientId;
    const issuerOk = payload.iss === "accounts.google.com" || payload.iss === "https://accounts.google.com";
    if (!audienceOk || !issuerOk || !payload.sub || !payload.exp || payload.exp <= now ||
        (payload.iat && payload.iat > now + 120) ||
        !(payload.email_verified === true || payload.email_verified === "true")) {
      return { error: "Google credential is expired or could not be verified.", status: 401 };
    }
    return {
      user: {
        id: String(payload.sub),
        email: String(payload.email || "").toLowerCase(),
        name: String(payload.name || "Google user"),
        picture: String(payload.picture || "")
      }
    };
  } catch (_error) {
    return { error: "Google sign-in verification failed. Check the OAuth client ID and sign in again.", status: 401 };
  }
}

async function requireUser(request, env) {
  const match = (request.headers.get("authorization") || "").match(/^Bearer\s+(.+)$/i);
  if (!match) return { error: "Please sign in with Google to continue.", status: 401 };
  return verifyGoogleIdToken(match[1], env);
}

function allowedForGitHub(user, env) {
  if (!env.GITHUB_TOKEN) return { error: "GitHub Agent is not configured: add GITHUB_TOKEN as a Worker secret.", status: 503 };
  const allowlist = new Set((env.GITHUB_ALLOWED_EMAILS || "").split(",").map((x) => x.trim().toLowerCase()).filter(Boolean));
  if (!allowlist.size) return { error: "GitHub Agent is not enabled: configure GITHUB_ALLOWED_EMAILS as a Worker secret.", status: 503 };
  if (!user?.email || !allowlist.has(user.email.toLowerCase())) {
    return { error: "This Google account is not allowed to use the GitHub Agent.", status: 403 };
  }
  return null;
}

function getRepository(env) {
  const value = (env.GITHUB_REPOSITORY || "seifhaytham275-star/E.V-MARK-X").trim();
  const parts = value.split("/");
  if (parts.length !== 2 || parts.some((part) => !/^[A-Za-z0-9_.-]+$/.test(part))) {
    throw new Error("GITHUB_REPOSITORY must use owner/repository format.");
  }
  return parts.map(encodeURIComponent).join("/");
}

async function githubRequest(path, env, options = {}) {
  if (!env.GITHUB_TOKEN) {
    const error = new Error("GitHub Agent is not configured: add GITHUB_TOKEN as a Worker secret.");
    error.status = 503;
    throw error;
  }
  const response = await fetch("https://api.github.com" + path, {
    ...options,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: "Bearer " + env.GITHUB_TOKEN,
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

function mapGitHubError(error) {
  const status = [400, 401, 403, 404, 409, 422, 429, 503].includes(error.status) ? error.status : 502;
  return json({ error: error.message || "GitHub operation failed." }, status);
}

function validGitHubPath(value) {
  const path = typeof value === "string" ? value.trim().replace(/^\/+/, "") : "";
  if (!path || path.length > 400 ||
      path.split("/").some((part) => !part || part === "." || part === "..") ||
      path.includes("\\") || path.includes("\0")) return null;
  return path;
}

function protectedWritePath(path) {
  return [".github", "server", "cloudflare-worker"].some((root) => path === root || path.startsWith(root + "/"));
}

function encodeBase64Utf8(value) {
  const bytes = encoder.encode(value);
  let binary = "";
  const chunkSize = 0x8000;
  for (let offset = 0; offset < bytes.length; offset += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + chunkSize));
  }
  return btoa(binary);
}

function decodeBase64Utf8(value) {
  const binary = atob(String(value || "").replace(/\s/g, ""));
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return decoder.decode(bytes);
}

async function getModels(apiKey, env) {
  if (apiKey === (env.GEMINI_API_KEY || "") && modelCache.expiresAt > Date.now() && modelCache.models.length) {
    return modelCache.models;
  }
  const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100", {
    headers: { "x-goog-api-key": apiKey }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error?.message || "Could not retrieve Gemini models.");
  const models = (data.models || [])
    .filter((model) => Array.isArray(model.supportedGenerationMethods) && model.supportedGenerationMethods.includes("generateContent"))
    .map((model) => ({
      name: String(model.name || "").replace(/^models\//, ""),
      displayName: model.displayName || model.name || "Gemini model",
      description: model.description || ""
    }))
    .filter((model) => /^gemini-/i.test(model.name));
  models.sort((a, b) => {
    const rank = (name) => {
      const lower = name.toLowerCase();
      if (lower.includes("flash-lite")) return 2;
      if (lower.includes("flash")) return 0;
      if (lower.includes("pro")) return 1;
      return 3;
    };
    return rank(a.name) - rank(b.name) || a.name.localeCompare(b.name);
  });
  if (apiKey === (env.GEMINI_API_KEY || "")) {
    const cacheSeconds = Math.max(30, Number(env.MODELS_CACHE_SECONDS || 300));
    modelCache = { expiresAt: Date.now() + cacheSeconds * 1000, models };
  }
  return models;
}

async function readJson(request, maxBytes = 1100000) {
  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength > maxBytes) throw Object.assign(new Error("Request body is too large."), { status: 413 });
  const text = await request.text();
  if (encoder.encode(text).byteLength > maxBytes) throw Object.assign(new Error("Request body is too large."), { status: 413 });
  try { return JSON.parse(text); } catch { throw Object.assign(new Error("Request body must be valid JSON."), { status: 400 }); }
}

async function handleRequest(request, env) {
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method.toUpperCase();

  if (method === "GET" && path === "/health") {
    return json({
      ok: true,
      service: "E.V-MARK-X API on Cloudflare Workers",
      authConfigured: Boolean(env.GOOGLE_CLIENT_ID),
      geminiConfigured: Boolean(env.GEMINI_API_KEY)
    });
  }

  if (method === "POST" && path === "/api/auth/google") {
    if (!env.GOOGLE_CLIENT_ID) return json({ error: "Google sign-in is not configured on the Worker yet." }, 503);
    const body = await readJson(request, 20000);
    const credential = typeof body?.credential === "string" ? body.credential : "";
    if (!credential || credential.length > 10000) return json({ error: "A valid Google credential is required." }, 400);
    const verified = await verifyGoogleIdToken(credential, env);
    if (verified.error) return json({ error: verified.error }, verified.status);
    return json({ ok: true, user: verified.user });
  }

  if (method === "GET" && path === "/api/models") {
    const auth = await requireUser(request, env);
    if (auth.error) return json({ error: auth.error }, auth.status);
    const apiKey = (request.headers.get("x-ev-gemini-key") || env.GEMINI_API_KEY || "").trim();
    if (!apiKey) return json({ error: "Add your Gemini API key in Settings." }, 400);
    try { return json({ models: await getModels(apiKey, env) }); }
    catch (error) { return json({ error: error.message || "Could not load Gemini models." }, 502); }
  }

  if (method === "POST" && path === "/api/chat") {
    const auth = await requireUser(request, env);
    if (auth.error) return json({ error: auth.error }, auth.status);
    const apiKey = (request.headers.get("x-ev-gemini-key") || env.GEMINI_API_KEY || "").trim();
    if (!apiKey) return json({ error: "Add your Gemini API key in Settings." }, 400);
    const body = await readJson(request, 25000);
    const model = typeof body?.model === "string" ? body.model.trim() : "";
    const prompt = typeof body?.prompt === "string" ? body.prompt.trim() : "";
    if (!model) return json({ error: "Choose a model before starting the chat." }, 400);
    if (!prompt || prompt.length > 12000) return json({ error: "Enter a message up to 12,000 characters." }, 400);
    try {
      const models = await getModels(apiKey, env);
      if (!models.some((item) => item.name === model)) return json({ error: "That model is not available for this API key. Refresh the model list." }, 400);
      const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/" + encodeURIComponent(model) + ":generateContent", {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }] }] })
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) return json({ error: data.error?.message || "Gemini request failed." }, response.status === 429 ? 429 : 502);
      const answer = (data.candidates?.[0]?.content?.parts || []).map((part) => part.text || "").join("").trim();
      if (!answer) return json({ error: "Gemini returned no text for this request." }, 502);
      return json({ answer, model });
    } catch (error) {
      return json({ error: error.message || "The AI service is temporarily unavailable." }, 502);
    }
  }

  if (path.startsWith("/api/github/")) {
    const auth = await requireUser(request, env);
    if (auth.error) return json({ error: auth.error }, auth.status);
    const denied = allowedForGitHub(auth.user, env);
    if (denied) return json({ error: denied.error }, denied.status);

    let repo;
    try { repo = getRepository(env); }
    catch (error) { return json({ error: error.message }, 500); }

    if (method === "GET" && path === "/api/github/status") {
      try {
        const data = await githubRequest("/repos/" + repo, env);
        return json({
          ok: true,
          repository: data.full_name,
          defaultBranch: data.default_branch,
          permissions: { contents: data.permissions?.push ? "write" : "read", actions: "read" }
        });
      } catch (error) { return mapGitHubError(error); }
    }

    if (method === "GET" && path === "/api/github/contents") {
      const filePath = validGitHubPath(url.searchParams.get("path"));
      if (!filePath) return json({ error: "Provide a valid repository file path." }, 400);
      const branch = url.searchParams.get("branch") || "";
      const suffix = branch ? "?ref=" + encodeURIComponent(branch) : "";
      try {
        const data = await githubRequest("/repos/" + repo + "/contents/" + filePath.split("/").map(encodeURIComponent).join("/") + suffix, env);
        if (Array.isArray(data) || data.type !== "file") return json({ error: "The requested path is not a single file." }, 400);
        return json({ path: data.path, name: data.name, sha: data.sha, size: data.size, branch: branch || undefined, content: decodeBase64Utf8(data.content) });
      } catch (error) { return mapGitHubError(error); }
    }

    if (method === "POST" && path === "/api/github/branches") {
      const body = await readJson(request, 20000);
      const label = typeof body?.name === "string" ? body.name.trim() : "ev-agent";
      const safeLabel = label.toLowerCase().replace(/[^a-z0-9._/-]+/g, "-").replace(/^[-/]+|[-/]+$/g, "").slice(0, 60) || "ev-agent";
      if (safeLabel.split("/").some((part) => !part || part === "..")) return json({ error: "Invalid branch name." }, 400);
      try {
        const repository = await githubRequest("/repos/" + repo, env);
        const base = typeof body?.base === "string" && /^[A-Za-z0-9._/-]{1,120}$/.test(body.base) ? body.base : repository.default_branch;
        const ref = await githubRequest("/repos/" + repo + "/git/ref/heads/" + base.split("/").map(encodeURIComponent).join("/"), env);
        const branch = "ev-agent/" + safeLabel + "-" + Date.now().toString(36);
        const created = await githubRequest("/repos/" + repo + "/git/refs", env, {
          method: "POST",
          body: JSON.stringify({ ref: "refs/heads/" + branch, sha: ref.object.sha })
        });
        return json({ ok: true, branch, base, commit: created.object.sha }, 201);
      } catch (error) { return mapGitHubError(error); }
    }

    if (method === "PUT" && path === "/api/github/contents") {
      const body = await readJson(request);
      const filePath = validGitHubPath(body?.path);
      const content = typeof body?.content === "string" ? body.content : null;
      const branch = typeof body?.branch === "string" ? body.branch.trim() : "";
      const message = typeof body?.message === "string" ? body.message.trim().slice(0, 180) : "";
      if (!filePath || content === null || encoder.encode(content).byteLength > 900000 ||
          !branch || !branch.startsWith("ev-agent/") || !message) {
        return json({ error: "A valid path, content under 900 KB, ev-agent branch, and commit message are required." }, 400);
      }
      if (protectedWritePath(filePath)) return json({ error: "Writes to backend and GitHub workflow files are blocked by the GitHub Agent safety policy." }, 403);
      try {
        const encodedPath = filePath.split("/").map(encodeURIComponent).join("/");
        let sha;
        try {
          const existing = await githubRequest("/repos/" + repo + "/contents/" + encodedPath + "?ref=" + encodeURIComponent(branch), env);
          if (!Array.isArray(existing) && existing.type === "file") sha = existing.sha;
        } catch (error) { if (error.status !== 404) throw error; }
        const payload = { message, content: encodeBase64Utf8(content), branch };
        if (sha) payload.sha = sha;
        const result = await githubRequest("/repos/" + repo + "/contents/" + encodedPath, env, { method: "PUT", body: JSON.stringify(payload) });
        return json({ ok: true, path: filePath, branch, commit: result.commit?.sha, url: result.commit?.html_url }, 201);
      } catch (error) { return mapGitHubError(error); }
    }

    if (method === "GET" && path === "/api/github/actions") {
      const perPage = Math.max(1, Math.min(20, Number.parseInt(url.searchParams.get("per_page") || "10", 10) || 10));
      try {
        const data = await githubRequest("/repos/" + repo + "/actions/runs?per_page=" + perPage, env);
        return json({
          total: data.total_count || 0,
          runs: (data.workflow_runs || []).map((run) => ({
            id: run.id, name: run.name, workflow: run.path, branch: run.head_branch, event: run.event,
            status: run.status, conclusion: run.conclusion, createdAt: run.created_at, updatedAt: run.updated_at,
            url: run.html_url, runNumber: run.run_number
          }))
        });
      } catch (error) { return mapGitHubError(error); }
    }

    return json({ error: "Method not allowed or GitHub Agent endpoint not found." }, 405);
  }

  return json({ error: "Not found." }, 404);
}

export default {
  async fetch(request, env) {
    const origin = request.headers.get("origin");
    const allowed = allowedOrigins(env);
    const headers = {
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      "Cache-Control": "no-store"
    };

    if (origin) {
      if (!allowed.has(origin)) return withHeaders(json({ error: "Origin is not allowed." }, 403), headers);
      headers["Access-Control-Allow-Origin"] = origin;
      headers["Access-Control-Allow-Headers"] = "Authorization, Content-Type, X-EV-Gemini-Key";
      headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, OPTIONS";
      headers["Access-Control-Max-Age"] = "600";
      headers["Vary"] = "Origin";
    }

    if (request.method.toUpperCase() === "OPTIONS") {
      return withHeaders(new Response(null, { status: origin ? 204 : 403 }), headers);
    }

    try {
      return withHeaders(await handleRequest(request, env), headers);
    } catch (error) {
      return withHeaders(json({ error: error.message || "Unexpected Worker error." }, error.status || 500), headers);
    }
  }
};
