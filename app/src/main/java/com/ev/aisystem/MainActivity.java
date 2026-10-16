package com.ev.aisystem;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
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

public class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 13, 27);
    private final int panel = Color.rgb(18, 27, 48);
    private final int red = Color.rgb(239, 54, 75);
    private final int blue = Color.rgb(65, 154, 255);
    private final int muted = Color.rgb(166, 181, 207);
    private LinearLayout root, body;
    private EditText apiKey, question;
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ev_system", MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);
        TextView brand = label("EV // AI SYSTEM", 23, Color.WHITE, true);
        brand.setPadding(20, 22, 20, 6);
        root.addView(brand);
        TextView subtitle = label("ONE APP · THREE PROTOCOLS", 11, blue, true);
        subtitle.setPadding(20, 0, 20, 14);
        root.addView(subtitle);
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(10, 0, 10, 10);
        root.addView(nav);
        addNav(nav, "AI CORE", 0);
        addNav(nav, "WALLET", 1);
        addNav(nav, "CALORIES", 2);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body = new LinearLayout(this);
        body.setPadding(16, 8, 16, 24);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        showAI();
    }

    private void addNav(LinearLayout nav, String title, int page) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(10);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        nav.addView(b, new LinearLayout.LayoutParams(0, dp(46), 1));
        b.setOnClickListener(v -> {
            if (page == 0) showAI();
            else if (page == 1) showWallet();
            else showCalories();
        });
    }

    private void base(String protocol, String title, String desc) {
        body.removeAllViews();
        card(protocol, title, desc);
    }

    private void showAI() {
        base("PROTOCOL 01 / INTELLIGENCE", "EV AI Core", "Live Gemini API · API key is entered for this session");
        TextView warning = label("For a public release, use a backend proxy to protect your API key.", 12, muted, false);
        warning.setPadding(4, 6, 4, 14);
        body.addView(warning);
        apiKey = field("Gemini API key", false);
        body.addView(apiKey, params());
        space(8);
        question = field("Ask EV anything…", true);
        body.addView(question, params());
        space(8);
        Button ask = button("ASK GEMINI", red);
        body.addView(ask, params());
        TextView answer = label("Ready to connect.", 14, Color.WHITE, false);
        answer.setPadding(12, 18, 12, 18);
        body.addView(answer, params());
        ask.setOnClickListener(v -> {
            String key = apiKey.getText().toString().trim();
            String q = question.getText().toString().trim();
            if (key.isEmpty() || q.isEmpty()) { toast("Enter an API key and a question."); return; }
            ask.setEnabled(false);
            answer.setText("Connecting to Gemini…");
            new Thread(() -> {
                String result = askGemini(key, q);
                runOnUiThread(() -> { answer.setText(result); ask.setEnabled(true); });
            }).start();
        });
    }

    private String askGemini(String key, String prompt) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
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
            if (candidates == null || candidates.length() == 0) return "Gemini returned no answer. Try again.";
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
            return "Could not connect. Check internet and API key. " + e.getMessage();
        } finally { if (c != null) c.disconnect(); }
    }

    private void showWallet() {
        base("PROTOCOL 02 / FINANCE", "EV Wallet", "Simple income and expense tracker · EGP");
        int income = prefs.getInt("income", 0), expense = prefs.getInt("expense", 0);
        card("BALANCE", (income - expense) + " EGP", "Income: " + income + " EGP · Expenses: " + expense + " EGP");
        EditText amount = field("Amount in EGP", false);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(amount, params()); space(8);
        LinearLayout row = new LinearLayout(this);
        Button inc = button("ADD INCOME", blue), exp = button("ADD EXPENSE", red);
        row.addView(inc, new LinearLayout.LayoutParams(0, dp(48), 1));
        row.addView(exp, new LinearLayout.LayoutParams(0, dp(48), 1));
        body.addView(row, params());
        inc.setOnClickListener(v -> saveMoney(amount, true));
        exp.setOnClickListener(v -> saveMoney(amount, false));
        space(8);
        Button reset = button("RESET TOTALS", panel); body.addView(reset, params());
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
        base("PROTOCOL 03 / NUTRITION", "EV Calories", "Simple food calorie counter · kcal");
        int total = prefs.getInt("calorie_total", 0);
        card("TODAY'S TOTAL", total + " kcal", "Calories entered on this device");
        EditText food = field("Food name (e.g. apple)", false);
        body.addView(food, params()); space(8);
        EditText calories = field("Calories per serving", false);
        calories.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(calories, params()); space(8);
        EditText servings = field("Number of servings", false);
        servings.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(servings, params()); space(8);
        Button add = button("ADD FOOD", red); body.addView(add, params());
        add.setOnClickListener(v -> {
            try {
                String name = food.getText().toString().trim();
                double per = Double.parseDouble(calories.getText().toString().trim());
                double count = Double.parseDouble(servings.getText().toString().trim());
                if (name.isEmpty() || per < 0 || count <= 0) throw new NumberFormatException();
                int added = (int)Math.round(per * count);
                prefs.edit().putInt("calorie_total", prefs.getInt("calorie_total", 0) + added)
                    .putString("last_food", name).putInt("last_food_calories", added).apply();
                showCalories();
                toast("Added " + added + " kcal");
            } catch (Exception e) { toast("Enter food, calories, and servings correctly."); }
        });
        space(8);
        String last = prefs.getString("last_food", "");
        if (!last.isEmpty()) card("LAST FOOD", last, prefs.getInt("last_food_calories", 0) + " kcal");
        Button reset = button("RESET CALORIE TOTAL", panel); body.addView(reset, params());
        reset.setOnClickListener(v -> { prefs.edit().putInt("calorie_total", 0).remove("last_food").remove("last_food_calories").apply(); showCalories(); });
        space(8);
        body.addView(label("Values are estimates. Use food labels when available.", 11, muted, false));
    }

    private void card(String eyebrow, String title, String desc) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(16, 16, 16, 16);
        c.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        c.setBackgroundColor(panel);
        TextView e = label(eyebrow, 10, blue, true);
        c.addView(e);
        TextView t = label(title, 22, Color.WHITE, true);
        t.setPadding(0, 6, 0, 6);
        c.addView(t);
        c.addView(label(desc, 12, muted, false));
        LinearLayout.LayoutParams p = params(); p.bottomMargin = dp(16);
        body.addView(c, p);
    }

    private EditText field(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(muted);
        e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
        if (multiline) { e.setMinLines(2); e.setGravity(48); }
        return e;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(11); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
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
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
