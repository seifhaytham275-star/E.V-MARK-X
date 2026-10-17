const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("app.html", "utf8");
const scriptMatch = html.match(/<script>\s*([\s\S]*?)\s*<\/script>/);
assert.ok(scriptMatch, "app.html must contain the application script");
const script = scriptMatch[1];

test("workspace JavaScript parses", () => {
  assert.doesNotThrow(() => new vm.Script(script));
});

test("model remains fixed to Gemini 3.5 Flash-Lite", () => {
  assert.match(script, /const MODEL_ID = "gemini-3\.5-flash-lite"/);
  assert.match(html, /id="model" value="gemini-3\.5-flash-lite"/);
  assert.doesNotMatch(html, /<select[^>]+id=["']model["']/i);
});

test("API key has save and remove controls and is not hard-coded", () => {
  assert.match(script, /localStorage\.getItem\(KEY_STORAGE\)/);
  assert.match(script, /localStorage\.setItem\(KEY_STORAGE, key\)/);
  assert.match(script, /localStorage\.removeItem\(KEY_STORAGE\)/);
  assert.doesNotMatch(html, /AIza[0-9A-Za-z_-]{30,}/);
});

test("web search is opt-in and source links are constrained to HTTP(S)", () => {
  assert.match(html, /id="webSearchToggle"/);
  assert.match(script, /requestBody\.tools = \[\{googleSearch:\{\}\}\]/);
  assert.match(script, /groundingMetadata/);
  assert.match(script, /noopener noreferrer/);
  assert.match(script, /\^https\?:\\\/\\\//);
});

test("attachment handling includes size limits and supported formats", () => {
  assert.match(html, /id="attachments"/);
  assert.match(script, /MAX_FILE_BYTES = 10 \* 1024 \* 1024/);
  assert.match(script, /application\/pdf/);
  assert.match(script, /image\/png/);
  assert.match(script, /Attached file:/);
  assert.match(script, /DOCX\/XLSX parsing is planned next/);
});

test("memory and local projects are user-controllable", () => {
  assert.match(html, /id="userMemory"/);
  assert.match(script, /localStorage\.setItem\(MEMORY_STORAGE/);
  assert.match(script, /localStorage\.removeItem\(MEMORY_STORAGE\)/);
  assert.match(html, /id="saveProjectBtn"/);
  assert.match(script, /localStorage\.setItem\(PROJECTS_STORAGE/);
  assert.match(script, /localStorage\.getItem\(PROJECTS_STORAGE/);
});

test("all HTML IDs are unique", () => {
  const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
  const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
  assert.deepEqual([...new Set(duplicates)], []);
});
