package com.ev.aisystem;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.text.InputType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(7, 11, 24);
    private final int panel = Color.rgb(17, 25, 46);
    private final int panel2 = Color.rgb(23, 34, 60);
    private final int accent = Color.rgb(99, 117, 255);
    private final int cyan = Color.rgb(56, 211, 232);
    private final int red = Color.rgb(255, 91, 119);
    private final int muted = Color.rgb(164, 179, 207);
    private LinearLayout root, body;
    private EditText apiKey, question;
    private Spinner modelPicker;
    private TextView modelStatus;
    private android.content.SharedPreferences prefs;
    private ArrayList<String> modelIds = new ArrayList<>();
    private LinearLayout historyList;
    private TextView conversationView;
    private String currentChatId = "";
    private String currentTranscript = "";
    private String currentChatTitle = "New chat";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ev_system", MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(12));
        header.setBackground(background(panel, dp(18), Color.TRANSPARENT));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(dp(12), dp(10), dp(12), dp(10));
        root.addView(header, hp);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = label("EV", 25, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(background(accent, dp(14), Color.TRANSPARENT));
        brandRow.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(48)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(12), 0, 0, 0);
        brandText.addView(label("E.V MARK X", 19, Color.WHITE, true));
        brandText.addView(label("AI SYSTEM  /  YOUR WORKSPACE", 10, cyan, true));
        brandRow.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(brandRow);

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(10), dp(2), dp(10), dp(8));
        nav.setGravity(Gravity.CENTER);
        root.addView(nav);
        addNav(nav, "✦  AI CORE", 0);
        addNav(nav, "WALLET", 1);
        addNav(nav, "CALORIES", 2);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body = new LinearLayout(this);
        body.setPadding(dp(16), dp(8), dp(16), dp(28));
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        int screenDp = (int)(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int contentWidth = screenDp >= 760 ? dp(700) : -1;
        scroll.addView(body, new ScrollView.LayoutParams(contentWidth, -2));
        currentChatId = prefs.getString("current_chat_id", "");
        currentTranscript = prefs.getString("current_chat_transcript", "");
        currentChatTitle = prefs.getString("current_chat_title", "New chat");
        if (currentChatId.isEmpty()) startNewChat();
        showAI();
    }

    private void addNav(LinearLayout nav, String title, int page) {
        Button b = button(title, panel2);
        b.setTextSize(10);
        b.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        nav.addView(b, p);
        b.setOnClickListener(v -> {
            if (page == 0) showAI();
            else if (page == 1) showWallet();
            else showCalories();
        });
    }

    private void base(String protocol, String title, String desc) {
        body.removeAllViews();
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(heroBackground());
        TextView eyebrow = label(protocol, 10, cyan, true);
        hero.addView(eyebrow);
        TextView heading = label(title, 27, Color.WHITE, true);
        heading.setPadding(0, dp(8), 0, dp(5));
        hero.addView(heading);
        hero.addView(label(desc, 12, muted, false));
        LinearLayout.LayoutParams p = params();
        p.bottomMargin = dp(16);
        body.addView(hero, p);
    }

    private void showAI() {
        base("E.V INTELLIGENCE  /  GEMINI STUDIO", "Your AI command center.", "One workspace. Ten Gemini choices. Built for your next big idea.");
        LinearLayout keyCard = cardBox();
        keyCard.addView(label("GEMINI CONNECTION", 11, cyan, true));
        apiKey = field("Paste your Gemini API key", false);
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams fp = params();
        fp.topMargin = dp(10);
        keyCard.addView(apiKey, fp);
        TextView hint = label("Key is used for requests and is not saved by this app.", 11, muted, false);
        hint.setPadding(0, dp(6), 0, 0);
        keyCard.addView(hint);
        body.addView(keyCard, params());
        space(12);

        LinearLayout modelCard = cardBox();
        modelCard.addView(label("MODEL SELECTOR", 11, cyan, true));
        modelPicker = new Spinner(this);
        modelPicker.setPadding(dp(6), dp(6), dp(6), dp(6));
        modelPicker.setBackground(background(panel2, dp(12), Color.TRANSPARENT));
        modelIds.clear();
        modelIds.add("gemini-3.8-flash");
        modelIds.add("gemini-3.7-flash");
        modelIds.add("gemini-3.6-flash");
        modelIds.add("gemini-3.5-flash");
        modelIds.add("gemini-3.5-flash-lite");
        modelIds.add("gemini-3.1-flash-lite");
        modelIds.add("gemini-3.1-pro-preview");
        modelIds.add("gemini-3-flash-preview");
        modelIds.add("gemini-2.5-pro");
        modelIds.add("gemini-2.5-flash");
        refreshModelPicker(prefs.getString("selected_model", "gemini-2.5-flash"));
        LinearLayout.LayoutParams sp = params();
        sp.topMargin = dp(8);
        modelCard.addView(modelPicker, sp);
        modelStatus = label("10 curated Gemini models are listed. Add your API key and refresh to show the exact models enabled for your account.", 11, muted, false);
        modelStatus.setPadding(0, dp(8), 0, dp(8));
        modelCard.addView(modelStatus);
        Button refresh = button("↻  REFRESH AVAILABLE MODELS", panel2);
        refresh.setAllCaps(false);
        modelCard.addView(refresh, params());
        refresh.setOnClickListener(v -> {
            String key = apiKey.getText().toString().trim();
            if (key.isEmpty()) { toast("Enter your Gemini API key first."); return; }
            refresh.setEnabled(false);
            modelStatus.setText("Loading models from Google…");
            new Thread(() -> {
                ArrayList<String> available = fetchModels(key);
                runOnUiThread(() -> {
                    refresh.setEnabled(true);
                    if (available.isEmpty()) {
                        modelStatus.setText("Could not load models. The suggested models remain available; check the key and connection.");
                    } else {
                        String selected = modelPicker.getSelectedItem() == null ? "gemini-2.5-flash" : modelPicker.getSelectedItem().toString();
                        modelIds.clear();
                        modelIds.addAll(available);
                        refreshModelPicker(selected);
                        modelStatus.setText(available.size() + " models supporting text generation found for this API key.");
                    }
                });
            }).start();
        });
        body.addView(modelCard, params());
        space(12);

        LinearLayout historyCard = cardBox();
        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView historyTitle = label("CHAT HISTORY", 11, cyan, true);
        historyHeader.addView(historyTitle, new LinearLayout.LayoutParams(0, -2, 1));
        Button newChat = button("＋ NEW CHAT", panel2);
        newChat.setAllCaps(false);
        historyHeader.addView(newChat, new LinearLayout.LayoutParams(-2, dp(40)));
        historyCard.addView(historyHeader);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams hlp = params(); hlp.topMargin = dp(8);
        historyCard.addView(historyList, hlp);
        body.addView(historyCard, params());
        space(12);
        newChat.setOnClickListener(v -> { startNewChat(); showAI(); });
        renderHistory();

        LinearLayout chatCard = cardBox();
        chatCard.addView(label("MESSAGE", 11, cyan, true));
        question = field("What would you like to explore?", true);
        LinearLayout.LayoutParams qp = params();
        qp.topMargin = dp(8);
        chatCard.addView(question, qp);
        spaceIn(chatCard, 10);
        Button ask = button("✦  SEND TO GEMINI", accent);
        ask.setAllCaps(false);
        chatCard.addView(ask, params());
        conversationView = label(currentTranscript.isEmpty() ? "Your response will appear here." : currentTranscript, 14, Color.WHITE, false);
        conversationView.setPadding(dp(14), dp(14), dp(14), dp(14));
        conversationView.setBackground(background(panel2, dp(14), Color.TRANSPARENT));
        conversationView.setTextIsSelectable(true);
        LinearLayout.LayoutParams ap = params();
        ap.topMargin = dp(12);
        chatCard.addView(conversationView, ap);
        body.addView(chatCard, params());

        ask.setOnClickListener(v -> {
            String key = apiKey.getText().toString().trim();
            String q = question.getText().toString().trim();
            String model = modelPicker.getSelectedItem() == null ? "gemini-2.5-flash" : modelPicker.getSelectedItem().toString();
            if (key.isEmpty() || q.isEmpty()) { toast("Enter your API key and a message."); return; }
            prefs.edit().putString("selected_model", model).apply();
            if (currentChatId.isEmpty()) startNewChat();
            if (currentChatTitle.equals("New chat")) {
                currentChatTitle = q.length() > 34 ? q.substring(0, 34) + "…" : q;
            }
            currentTranscript = (currentTranscript.isEmpty() ? "" : currentTranscript + "\n\n") + "You: " + q + "\n\nEV: Thinking with " + model + "…";
            conversationView.setText(currentTranscript);
            question.setText("");
            ask.setEnabled(false);
            saveCurrentChat();
            new Thread(() -> {
                String result = askGemini(key, q, model);
                runOnUiThread(() -> {
                    int marker = currentTranscript.lastIndexOf("EV: Thinking with ");
                    if (marker >= 0) currentTranscript = currentTranscript.substring(0, marker) + "EV: " + result;
                    else currentTranscript += "\n\nEV: " + result;
                    conversationView.setText(currentTranscript);
                    ask.setEnabled(true);
                    saveCurrentChat();
                    renderHistory();
                });
            }).start();
        });
    }


    private void startNewChat() {
        currentChatId = String.valueOf(System.currentTimeMillis());
        currentTranscript = "";
        currentChatTitle = "New chat";
        prefs.edit().putString("current_chat_id", currentChatId)
            .putString("current_chat_transcript", "")
            .putString("current_chat_title", currentChatTitle).apply();
    }

    private JSONArray readChatSessions() {
        try { return new JSONArray(prefs.getString("chat_sessions", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void saveCurrentChat() {
        if (currentChatId == null || currentChatId.isEmpty() || currentTranscript.isEmpty()) return;
        JSONArray old = readChatSessions();
        JSONArray next = new JSONArray();
        JSONObject current = new JSONObject();
        try {
            current.put("id", currentChatId);
            current.put("title", currentChatTitle);
            current.put("transcript", currentTranscript);
            current.put("updated", System.currentTimeMillis());
            boolean replaced = false;
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null) continue;
                if (currentChatId.equals(item.optString("id"))) {
                    if (!replaced) { next.put(current); replaced = true; }
                } else next.put(item);
            }
            if (!replaced) next.put(current);
            JSONArray limited = new JSONArray();
            int start = Math.max(0, next.length() - 50);
            for (int i = next.length() - 1; i >= start; i--) limited.put(next.optJSONObject(i));
            JSONArray ordered = new JSONArray();
            for (int i = limited.length() - 1; i >= 0; i--) ordered.put(limited.optJSONObject(i));
            prefs.edit().putString("chat_sessions", ordered.toString())
                .putString("current_chat_id", currentChatId)
                .putString("current_chat_transcript", currentTranscript)
                .putString("current_chat_title", currentChatTitle).apply();
        } catch (Exception ignored) { }
    }

    private void renderHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();
        JSONArray sessions = readChatSessions();
        int count = 0;
        for (int i = sessions.length() - 1; i >= 0; i--) {
            JSONObject item = sessions.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "");
            String title = item.optString("title", "Saved chat");
            if (id.isEmpty()) continue;
            Button entry = button((id.equals(currentChatId) ? "●  " : "  ") + title, id.equals(currentChatId) ? accent : panel2);
            entry.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            entry.setPadding(dp(12), 0, dp(8), 0);
            entry.setAllCaps(false);
            LinearLayout.LayoutParams ep = params(); ep.topMargin = dp(5);
            historyList.addView(entry, ep);
            entry.setOnClickListener(v -> {
                currentChatId = id;
                currentChatTitle = title;
                currentTranscript = item.optString("transcript", "");
                prefs.edit().putString("current_chat_id", currentChatId)
                    .putString("current_chat_title", currentChatTitle)
                    .putString("current_chat_transcript", currentTranscript).apply();
                showAI();
            });
            count++;
            if (count >= 12) break;
        }
        if (count == 0) historyList.addView(label("Your saved conversations will appear here.", 12, muted, false));
    }

    private void refreshModelPicker(String preferred) {
        if (modelPicker == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modelIds) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextColor(Color.WHITE);
                t.setTextSize(14);
                return t;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getDropDownView(position, convertView, parent);
                t.setTextColor(Color.WHITE);
                t.setBackgroundColor(panel2);
                t.setPadding(dp(12), dp(12), dp(12), dp(12));
                return t;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelPicker.setAdapter(adapter);
        int index = modelIds.indexOf(preferred);
        modelPicker.setSelection(index >= 0 ? index : 0);
    }

    private ArrayList<String> fetchModels(String key) {
        ArrayList<String> found = new ArrayList<>();
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000");
            c = (HttpURLConnection)url.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("x-goog-api-key", key);
            int status = c.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
            StringBuilder raw = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) raw.append(line);
            }
            if (status < 200 || status >= 300) return found;
            JSONArray models = new JSONObject(raw.toString()).optJSONArray("models");
            if (models == null) return found;
            for (int i = 0; i < models.length(); i++) {
                JSONObject item = models.optJSONObject(i);
                if (item == null) continue;
                JSONArray methods = item.optJSONArray("supportedGenerationMethods");
                boolean supports = false;
                if (methods != null) for (int j = 0; j < methods.length(); j++)
                    if ("generateContent".equals(methods.optString(j))) supports = true;
                String name = item.optString("name", "");
                if (supports && name.startsWith("models/")) found.add(name.substring("models/".length()));
            }
            java.util.Collections.sort(found);
        } catch (Exception ignored) {
        } finally { if (c != null) c.disconnect(); }
        return found;
    }

    private String askGemini(String key, String prompt, String model) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
            c = (HttpURLConnection)url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(45000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setRequestProperty("x-goog-api-key", key);
            JSONObject part = new JSONObject();
            part.put("text", "You are EV AI Core, a helpful assistant. Answer clearly and safely. User: " + prompt);
            JSONArray parts = new JSONArray(); parts.put(part);
            JSONObject content = new JSONObject(); content.put("parts", parts);
            JSONArray contents = new JSONArray(); contents.put(content);
            JSONObject payload = new JSONObject(); payload.put("contents", contents);
            try (OutputStream out = c.getOutputStream()) { out.write(payload.toString().getBytes(StandardCharsets.UTF_8)); }
            int status = c.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
            StringBuilder raw = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) raw.append(line);
            }
            JSONObject response = new JSONObject(raw.toString());
            if (status < 200 || status >= 300) {
                JSONObject err = response.optJSONObject("error");
                return "Gemini API error: " + (err == null ? "HTTP " + status : err.optString("message", "Request failed"));
            }
            JSONArray candidates = response.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return "Gemini returned no answer. Try another model.";
            JSONObject candidate = candidates.optJSONObject(0);
            JSONObject responseContent = candidate == null ? null : candidate.optJSONObject("content");
            JSONArray responseParts = responseContent == null ? null : responseContent.optJSONArray("parts");
            if (responseParts == null) return "Gemini returned an empty answer.";
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < responseParts.length(); i++) {
                JSONObject p = responseParts.optJSONObject(i);
                if (p != null && p.has("text")) {
                    if (text.length() > 0) text.append("\n");
                    text.append(p.optString("text"));
                }
            }
            return text.length() == 0 ? "Gemini returned an empty answer." : text.toString();
        } catch (Exception e) {
            return "Could not connect. Check internet, API key, and selected model. " + e.getMessage();
        } finally { if (c != null) c.disconnect(); }
    }

    private void showWallet() {
        base("PROTOCOL 02  /  FINANCE", "EV Wallet", "A simple personal income and expense tracker · EGP");
        int income = prefs.getInt("income", 0), expense = prefs.getInt("expense", 0);
        card("CURRENT BALANCE", (income - expense) + " EGP", "Income: " + income + " EGP   •   Expenses: " + expense + " EGP");
        LinearLayout box = cardBox();
        box.addView(label("ADD A TRANSACTION", 11, cyan, true));
        EditText amount = field("Amount in EGP", false);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams ap = params(); ap.topMargin = dp(10); box.addView(amount, ap);
        LinearLayout row = new LinearLayout(this);
        Button inc = button("＋ INCOME", accent), exp = button("− EXPENSE", red);
        row.addView(inc, new LinearLayout.LayoutParams(0, dp(48), 1));
        row.addView(exp, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams rp = params(); rp.topMargin = dp(10); box.addView(row, rp);
        body.addView(box, params());
        inc.setOnClickListener(v -> saveMoney(amount, true));
        exp.setOnClickListener(v -> saveMoney(amount, false));
        space(10);
        Button reset = button("RESET TOTALS", panel2); body.addView(reset, params());
        reset.setOnClickListener(v -> { prefs.edit().putInt("income", 0).putInt("expense", 0).apply(); showWallet(); });
    }

    private void saveMoney(EditText amount, boolean isIncome) {
        try {
            double value = Double.parseDouble(amount.getText().toString().trim());
            if (value <= 0) throw new NumberFormatException();
            String key = isIncome ? "income" : "expense";
            int next = prefs.getInt(key, 0) + (int)Math.round(value);
            prefs.edit().putInt(key, next).apply();
            showWallet();
        } catch (Exception e) { toast("Enter a valid positive amount."); }
    }

    private void showCalories() {
        base("PROTOCOL 03  /  NUTRITION", "EV Calories", "A simple food calorie counter · kcal");
        int total = prefs.getInt("calorie_total", 0);
        card("TODAY'S TOTAL", total + " kcal", "Total entered on this device");
        LinearLayout box = cardBox();
        box.addView(label("ADD FOOD", 11, cyan, true));
        EditText food = field("Food name (e.g. apple)", false);
        EditText calories = field("Calories per serving", false);
        calories.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText servings = field("Number of servings", false);
        servings.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        for (EditText e : new EditText[]{food, calories, servings}) {
            LinearLayout.LayoutParams p = params(); p.topMargin = dp(8); box.addView(e, p);
        }
        Button add = button("＋  ADD FOOD", accent); add.setAllCaps(false);
        LinearLayout.LayoutParams bp = params(); bp.topMargin = dp(10); box.addView(add, bp);
        body.addView(box, params());
        add.setOnClickListener(v -> {
            try {
                String name = food.getText().toString().trim();
                double per = Double.parseDouble(calories.getText().toString().trim());
                double count = Double.parseDouble(servings.getText().toString().trim());
                if (name.isEmpty() || per < 0 || count <= 0) throw new NumberFormatException();
                int added = (int)Math.round(per * count);
                prefs.edit().putInt("calorie_total", prefs.getInt("calorie_total", 0) + added)
                    .putString("last_food", name).putInt("last_food_calories", added).apply();
                showCalories(); toast("Added " + added + " kcal");
            } catch (Exception e) { toast("Enter food, calories, and servings correctly."); }
        });
        space(10);
        String last = prefs.getString("last_food", "");
        if (!last.isEmpty()) card("LAST FOOD", last, prefs.getInt("last_food_calories", 0) + " kcal");
        Button reset = button("RESET CALORIE TOTAL", panel2); body.addView(reset, params());
        reset.setOnClickListener(v -> { prefs.edit().putInt("calorie_total", 0).remove("last_food").remove("last_food_calories").apply(); showCalories(); });
        space(8);
        body.addView(label("Values are estimates. Use food labels when available.", 11, muted, false));
    }

    private LinearLayout cardBox() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(background(panel, dp(18), Color.TRANSPARENT));
        return c;
    }

    private void card(String eyebrow, String title, String desc) {
        LinearLayout c = cardBox();
        c.addView(label(eyebrow, 10, cyan, true));
        TextView t = label(title, 23, Color.WHITE, true);
        t.setPadding(0, dp(6), 0, dp(6));
        c.addView(t);
        c.addView(label(desc, 12, muted, false));
        LinearLayout.LayoutParams p = params(); p.bottomMargin = dp(14);
        body.addView(c, p);
    }

    private GradientDrawable heroBackground() {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(28, 42, 82), Color.rgb(42, 31, 88), Color.rgb(14, 61, 83)}
        );
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), Color.rgb(64, 86, 145));
        return d;
    }

    private GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private EditText field(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(muted);
        e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(background(panel2, dp(12), Color.TRANSPARENT));
        if (multiline) { e.setMinLines(3); e.setGravity(Gravity.TOP | Gravity.START); }
        return e;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(11); b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(background(color, dp(12), Color.TRANSPARENT));
        return b;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private void space(int h) { View v = new View(this); body.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private void spaceIn(LinearLayout parent, int h) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
