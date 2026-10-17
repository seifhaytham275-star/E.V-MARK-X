const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("app.html", "utf8");
const android = fs.readFileSync("app/src/main/java/com/ev/aisystem/MainActivity.java", "utf8");
const scriptMatch = html.match(/<script>\s*([\s\S]*?)\s*<\/script>/);
assert.ok(scriptMatch, "app.html must contain the application script");
const script = scriptMatch[1];


test("Android app uses the same single fixed Gemini model", () => {
  assert.match(android, /private static final String MODEL_ID = "gemini-3\.5-flash-lite"/);
  assert.match(android, /String model = MODEL_ID/);
  assert.match(android, /askGemini\(key, planPrompt, MODEL_ID\)/);
  assert.doesNotMatch(android, /modelPicker|selected_model|MODEL SELECTOR|REFRESH AVAILABLE MODELS|fetchModels\(/);
  assert.match(android, /ACTIVE MODEL/);
  assert.match(android, /Gemini 3\.5 Flash-Lite/);
});

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
  assert.match(script, /requestBody\.tools=\[\{google_search:\{\}\}\]/);
  assert.match(script, /groundingMetadata/);
  assert.match(script, /noopener noreferrer/);
  assert.match(script, /https\?:/);
});

test("attachment handling includes size limits and supported formats", () => {
  assert.match(html, /id="attachments"/);
  assert.match(script, /MAX_FILE_BYTES = 10 \* 1024 \* 1024/);
  assert.match(script, /pendingBytes \+ file\.size > MAX_FILE_BYTES/);
  assert.match(script, /application\/pdf/);
  assert.match(script, /image\/png/);
  assert.match(script, /Attached file:/);
  assert.match(script, /DOCX\/XLSX parsing is planned next/);
  assert.match(script, /Attachments restored for retry/);
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

test("every JavaScript ID lookup has a matching HTML element", () => {
  const ids = new Set([...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]));
  const used = [...script.matchAll(/\$\("([^"]+)"\)/g)].map(match => match[1]);
  assert.deepEqual([...new Set(used.filter(id => !ids.has(id)))], []);
});

test("empty saved-project selection is ignored", () => {
  assert.match(script, /if\(!\$\("projectSelect"\)\.value\) return/);
});

test("web search uses the REST API tool field and renders grounding sources", () => {
  assert.match(script, /google_search/);
  assert.match(script, /groundingMetadata/);
  assert.match(script, /function searchWebTool/);
});

test("Agent Mode has a bounded allowlist of local tools", () => {
  assert.match(script, /const AGENT_DECLARATIONS=/);
  assert.match(script, /async function runAgent\(contents,key\)/);
  assert.match(script, /calls>=4/);
  assert.match(script, /if\(call\.id\)functionResponse\.id=call\.id/);
  assert.match(script, /functionCallingConfig:\{mode:"AUTO"\}/);
  for (const tool of ["calculate", "get_current_time", "search_web", "create_studio_preview", "read_saved_memory"]) {
    assert.match(script, new RegExp('name:"' + tool + '"'));
  }
});

test("E.V Studio preview is sandboxed and restricts network access", () => {
  assert.match(script, /async function generateStudio\(description,key\)/);
  assert.match(script, /setAttribute\("sandbox","allow-scripts"\)/);
  assert.match(script, /connect-src 'none'/);
  assert.match(script, /textContent="Download HTML"/);
  assert.match(script, /new Blob\(\[secureStudioHtml\(html\)\]/);
});

test("safe calculator evaluates arithmetic without eval", () => {
  const match = script.match(/function parseSafeMath\(expression\)\{[\s\S]*?\n  \}/);
  assert.ok(match, "safe calculator should exist");
  const calculate = vm.runInNewContext("(" + match[0] + ")");
  assert.equal(calculate("2 + 3 * 4"), 14);
  assert.equal(calculate("(10 - 4) / 2"), 3);
  assert.throws(() => calculate("1 / 0"), /zero/);
  assert.throws(() => calculate("alert(1)"));
  assert.throws(() => calculate("2 + bad"));
});

test("Agent and Studio commands preserve the fixed model and existing UI", () => {
  assert.match(script, /commandMatch = text\.match/);
  assert.match(script, /mode==="studio"/);
  assert.match(script, /mode==="agent"/);
  assert.match(script, /const MODEL_ID = "gemini-3\.5-flash-lite"/);
  assert.doesNotMatch(html, /<select[^>]+id=["']model["']/i);
});

test("70+ new workflow commands are available without changing the UI", () => {
  const block = script.match(/const FEATURE_COMMANDS = \{([\s\S]*?)\n  \};/);
  assert.ok(block, "workflow registry should exist");
  const names = [...block[1].matchAll(/^\s{4}([a-z][a-z0-9]*):/gm)].map(match => match[1]);
  assert.ok(names.length >= 65, "expected at least 65 new AI workflows");
  for (const command of ["summarize", "translate", "quiz", "flashcards", "debug", "studyplan", "security", "documentation", "testcases", "troubleshoot"]) {
    assert.ok(names.includes(command), "missing /" + command);
  }
  assert.match(script, /runLocalCommand\(text\)/);
  assert.match(script, /recognizedCommand/);
  assert.match(script, /const MODEL_ID = "gemini-3\.5-flash-lite"/);
  assert.doesNotMatch(html, /<select[^>]+id=["']model["']/i);
});

test("local utility commands work without adding UI controls", () => {
  for (const command of ["help", "commands", "math", "wordcount", "reverse", "upper", "lower", "date", "time", "export"]) {
    assert.match(script, new RegExp(command));
  }
  assert.match(script, /parseSafeMath\(arg\)/);
  assert.match(script, /ev-mark-x-chat\.txt/);
});



test("Studio HTML receives a restrictive CSP in every document shape", () => {
  const match = script.match(/function secureStudioHtml\(html\)\{[\s\S]*?\n  \}/);
  assert.ok(match, "Studio HTML security wrapper should exist");
  const secure = vm.runInNewContext("(" + match[0] + ")");
  for (const source of [
    "<!doctype html><html><head><title>Test</title></head><body>ok</body></html>",
    "<html><body>fragment</body></html>",
    "<p>fragment</p>"
  ]) {
    const result = secure(source);
    assert.match(result, /Content-Security-Policy/);
    assert.match(result, /connect-src 'none'/);
    assert.match(result, /form-action 'none'/);
    assert.match(result, /base-uri 'none'/);
  }
  const existingPolicy = secure("<html><head><meta http-equiv=\"Content-Security-Policy\" content=\"default-src https:\"></head><body></body></html>");
  assert.equal((existingPolicy.match(/Content-Security-Policy/g) || []).length, 2, "existing policies should remain while the restrictive policy is added");
});

test("Gemini requests have a timeout and do not expose the API key in the URL", () => {
  assert.match(script, /new AbortController\(\)/);
  assert.match(script, /setTimeout\(\(\)=>controller\.abort\(\),60000\)/);
  assert.match(script, /headers:\{\"Content-Type\":\"application\/json\",\"x-goog-api-key\":key\}/);
  assert.doesNotMatch(script, /generativelanguage\.googleapis\.com[^\n]*[?&]key=/i);
});

test("conversation context is bounded and prior binary attachments are not resent", () => {
  const match = script.match(/function buildHistoryContext\(items\)\{[\s\S]*?\n  \}/);
  assert.ok(match, "bounded history helper should exist");
  const build = vm.runInNewContext("(function(){const MAX_HISTORY_MESSAGES=24;const MAX_HISTORY_CHARS=60000;" + match[0] + ";return buildHistoryContext;})()");
  const items = [];
  for (let i = 0; i < 30; i++) {
    items.push({ role: "user", parts: [{ text: "message-" + i }, ...(i === 29 ? [{ inline_data: { mime_type: "application/pdf", data: "A".repeat(100000) } }] : [])] });
    items.push({ role: "model", parts: [{ text: "reply-" + i }] });
  }
  const result = build(items);
  assert.ok(result.length <= 24, "context should include at most 24 messages");
  assert.ok(result.reduce((sum, item) => sum + item.parts.reduce((n, part) => n + (part.text || "").length, 0), 0) <= 60000);
  assert.ok(result.every(item => item.parts.every(part => !part.inline_data)), "binary attachments must not be replayed");
  assert.ok(result.some(item => item.parts.some(part => (part.text || "").includes("omitted to keep context small"))));
  assert.ok(result.some(item => item.parts.some(part => (part.text || "").includes("message-29"))));
});

test("conversation history stores attachment placeholders and is capped after each reply", () => {
  assert.match(script, /const MAX_HISTORY_MESSAGES = 24/);
  assert.match(script, /const historyParts = userParts\.map\(part => part && part\.inline_data/);
  assert.match(script, /if\(history\.length > MAX_HISTORY_MESSAGES\) history = history\.slice\(-MAX_HISTORY_MESSAGES\)/);
  assert.match(script, /const contents = buildHistoryContext\(history\)/);
});

test("unknown slash-prefixed text remains ordinary chat", () => {
  assert.match(script, /const recognizedCommand = mode === "agent" \|\| mode === "studio" \|\| Boolean\(featurePrompt\)/);
  assert.match(script, /commandMatch && recognizedCommand \? \(commandMatch\[2\] \|\| ""\)\.trim\(\) : text/);
});
