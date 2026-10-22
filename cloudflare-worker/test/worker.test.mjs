import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

const origin = "https://seifhaytham275-star.github.io";
const env = {
  FRONTEND_ORIGIN: origin,
  GITHUB_REPOSITORY: "seifhaytham275-star/E.V-MARK-X"
};

test("health endpoint returns safe service status", async () => {
  const response = await worker.fetch(new Request("https://api.example.workers.dev/health", {
    headers: { Origin: origin }
  }), env);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("access-control-allow-origin"), origin);
  const body = await response.json();
  assert.equal(body.ok, true);
  assert.equal(body.service, "E.V-MARK-X API on Cloudflare Workers");
  assert.equal(body.authConfigured, false);
  assert.equal(body.geminiConfigured, false);
});

test("CORS rejects origins that are not allowlisted", async () => {
  const response = await worker.fetch(new Request("https://api.example.workers.dev/health", {
    headers: { Origin: "https://untrusted.example" }
  }), env);
  assert.equal(response.status, 403);
  assert.equal(response.headers.get("access-control-allow-origin"), null);
});

test("GitHub Agent endpoints require Google sign-in", async () => {
  const response = await worker.fetch(new Request("https://api.example.workers.dev/api/github/status", {
    headers: { Origin: origin }
  }), env);
  assert.equal(response.status, 401);
  const body = await response.json();
  assert.match(body.error, /sign in with Google/i);
});

test("unknown paths return JSON 404", async () => {
  const response = await worker.fetch(new Request("https://api.example.workers.dev/not-a-route", {
    headers: { Origin: origin }
  }), env);
  assert.equal(response.status, 404);
  assert.match(response.headers.get("content-type"), /application\/json/i);
});
