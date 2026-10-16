package com.ev.aisystem;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
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
import java.util.HashMap;
import java.util.Map;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private TextView authStatus;
    private static final int RC_GOOGLE_SIGN_IN = 701;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ev_system", MODE_PRIVATE);
        initializeFirebase();
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
        if (firebaseAuth != null && firebaseAuth.getCurrentUser() != null) loadCloudHistory();
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
        base("E.V INTELLIGENCE  /  GEMINI STUDIO", "Your AI command center.", "Free-tier Gemini models, saved chats, one workspace.");
        LinearLayout loginCard = cardBox();
        loginCard.addView(label("ACCOUNT & SYNC", 11, cyan, true));
        authStatus = label(getAuthStatusText(), 12, muted, false);
        authStatus.setPadding(0, dp(7), 0, dp(9));
        loginCard.addView(authStatus);
        Button googleButton = button("G  CONTINUE WITH GOOGLE", panel2);
        googleButton.setAllCaps(false);
        loginCard.addView(googleButton, params());
        googleButton.setOnClickListener(v -> beginGoogleSignIn());
        body.addView(loginCard, params());
        space(12);

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
        modelIds.add("gemini-2.5-flash");
        modelIds.add("gemini-2.5-flash-lite");
        modelIds.add("gemini-2.5-pro");
        modelIds.add("gemini-2.0-flash");
        modelIds.add("gemini-2.0-flash-lite");
        refreshModelPicker(prefs.getString("selected_model", "gemini-2.5-flash"));
        LinearLayout.LayoutParams sp = params();
        sp.topMargin = dp(8);
        modelCard.addView(modelPicker, sp);
        modelStatus = label("Gemini models supported by your API key. API access does not guarantee free-tier access; check current Google quotas.", 11, muted, false);
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
                        modelStatus.setText("Could not load models. Check your API key and connection; common model options remain.");
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
        Button historyToggle = button("☰ HISTORY", panel2);
        historyToggle.setAllCaps(false);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(-2, dp(40));
        htp.rightMargin = dp(6);
        historyHeader.addView(historyToggle, htp);
        Button newChat = button("＋ NEW CHAT", panel2);
        newChat.setAllCaps(false);
        historyHeader.addView(newChat, new LinearLayout.LayoutParams(-2, dp(40)));
        historyCard.addView(historyHeader);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        historyList.setVisibility(View.GONE);
        historyToggle.setOnClickListener(v -> historyList.setVisibility(historyList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
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



    private void initializeFirebase() {
        try {
            String apiKey = getString(R.string.firebase_api_key);
            String appId = getString(R.string.firebase_app_id);
            String projectId = getString(R.string.firebase_project_id);
            String webClientId = getString(R.string.default_web_client_id);
            if (apiKey.startsWith("REPLACE_") || appId.startsWith("REPLACE_") ||
                projectId.startsWith("REPLACE_") || webClientId.startsWith("REPLACE_")) return;
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey(apiKey).setApplicationId(appId).setProjectId(projectId).build();
                FirebaseApp.initializeApp(this, options);
            }
            firebaseAuth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception ignored) {
            firebaseAuth = null;
            firestore = null;
        }
    }

    private String getAuthStatusText() {
        if (firebaseAuth == null) return "Google sign-in needs the Firebase project values configured. See SETUP.md.";
        if (firebaseAuth.getCurrentUser() == null) return "Sign in once; Google keeps your session on this device.";
        String name = firebaseAuth.getCurrentUser().getDisplayName();
        return "Signed in" + (name == null || name.isEmpty() ? "" : " as " + name) + " · chat sync enabled";
    }

    private void beginGoogleSignIn() {
        if (firebaseAuth == null) {
            toast("Google sign-in setup is required. Open SETUP.md in the repository.");
            return;
        }
        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(getString(R.string.default_web_client_id)).build();
            GoogleSignInClient client = GoogleSignIn.getClient(this, options);
            startActivityForResult(client.getSignInIntent(), RC_GOOGLE_SIGN_IN);
        } catch (Exception e) {
            toast("Could not start Google sign-in. Check Firebase and OAuth setup.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_GOOGLE_SIGN_IN) return;
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account == null || account.getIdToken() == null || firebaseAuth == null) {
                toast("Google sign-in returned no token. Check the Web client ID.");
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    if (authStatus != null) authStatus.setText(getAuthStatusText());
                    toast("Signed in with Google.");
                    loadCloudHistory();
                } else {
                    toast("Sign-in failed. Verify Firebase Authentication and SHA-1/OAuth settings.");
                }
            });
        } catch (ApiException e) {
            toast("Google sign-in cancelled or not configured (code " + e.getStatusCode() + ").");
        }
    }

    private void loadCloudHistory() {
        if (firestore == null || firebaseAuth == null || firebaseAuth.getCurrentUser() == null) return;
        firestore.collection("users").document(firebaseAuth.getCurrentUser().getUid())
            .collection("chats").orderBy("updated", Query.Direction.DESCENDING).limit(50).get()
            .addOnSuccessListener(snapshot -> {
                JSONArray local = readChatSessions();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    try {
                        String id = doc.getString("id");
                        if (id == null || id.isEmpty()) continue;
                        JSONObject obj = new JSONObject();
                        obj.put("id", id);
                        obj.put("title", doc.getString("title") == null ? "Saved chat" : doc.getString("title"));
                        obj.put("transcript", doc.getString("transcript") == null ? "" : doc.getString("transcript"));
                        obj.put("updated", doc.getLong("updated") == null ? 0 : doc.getLong("updated"));
                        JSONArray merged = new JSONArray();
                        boolean exists = false;
                        for (int i = 0; i < local.length(); i++) {
                            JSONObject old = local.optJSONObject(i);
                            if (old == null) continue;
                            if (id.equals(old.optString("id"))) {
                                if (!exists) merged.put(obj);
                                exists = true;
                            } else merged.put(old);
                        }
                        if (!exists) merged.put(obj);
                        local = merged;
                    } catch (Exception ignored) { }
                }
                prefs.edit().putString("chat_sessions", local.toString()).apply();
                if (authStatus != null) authStatus.setText(getAuthStatusText());
                renderHistory();
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
        if (firestore != null && firebaseAuth != null && firebaseAuth.getCurrentUser() != null) {
            Map<String, Object> cloudChat = new HashMap<>();
            cloudChat.put("id", currentChatId);
            cloudChat.put("title", currentChatTitle);
            cloudChat.put("transcript", currentTranscript);
            cloudChat.put("updated", System.currentTimeMillis());
            firestore.collection("users").document(firebaseAuth.getCurrentUser().getUid())
                .collection("chats").document(currentChatId).set(cloudChat);
        }
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
                if (supports && name.startsWith("models/")) {
                    String id = name.substring("models/".length());
                    found.add(id);
                }
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
            part.put("text", "You are EV AI Core, a helpful assistant. Answer clearly and safely. The user may be a minor. For minors, do not prescribe calorie deficits, weight-loss calorie targets, restrictive diets, fasting, supplements, or weight-loss programs. Encourage a parent/guardian and pediatric healthcare professional for weight or growth concerns. You may offer balanced meal ideas, hydration, sleep, enjoyable age-appropriate activity, and non-extreme strength training with supervision. User: " + prompt);
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
        base("EV WELLNESS  /  NUTRITION + MOVEMENT", "Your daily wellness", "Balanced meals, movement, hydration and recovery — all in one place.");

        LinearLayout profile = cardBox();
        profile.addView(label("YOUR PROFILE", 11, cyan, true));
        TextView note = label("For teens, calorie needs depend on growth and individual health. This app will not set a weight-loss calorie target or restrictive diet. If you are worried about weight or growth, talk with a parent/guardian and a pediatric healthcare professional.", 12, muted, false);
        note.setPadding(0, dp(8), 0, dp(12));
        profile.addView(note);
        EditText age = field("Age in years (e.g. 15)", false);
        age.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText weight = field("Weight in kg (optional)", false);
        weight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText height = field("Height in cm (optional)", false);
        height.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        age.setText(prefs.getString("wellness_age", "15"));
        weight.setText(prefs.getString("wellness_weight", ""));
        height.setText(prefs.getString("wellness_height", ""));
        for (EditText e : new EditText[]{age, weight, height}) {
            LinearLayout.LayoutParams p = params();
            p.topMargin = dp(8);
            profile.addView(e, p);
        }
        body.addView(profile, params());
        space(12);

        LinearLayout coach = cardBox();
        coach.addView(label("GEMINI WELLNESS COACH", 11, cyan, true));
        coach.addView(label("Build a balanced meal routine and beginner-friendly weekly movement plan with Gemini 2.5 Flash. No calorie-deficit targets or restrictive dieting.", 12, muted, false));
        EditText api = field("Gemini API key (not saved)", false);
        api.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams ak = params();
        ak.topMargin = dp(10);
        coach.addView(api, ak);
        EditText preferences = field("Food preferences, allergies, schedule, gym access (optional)", true);
        LinearLayout.LayoutParams pp = params();
        pp.topMargin = dp(8);
        coach.addView(preferences, pp);
        Button build = button("✦  BUILD MY WELLNESS PLAN", accent);
        build.setAllCaps(false);
        LinearLayout.LayoutParams bp = params();
        bp.topMargin = dp(12);
        coach.addView(build, bp);
        TextView plan = label(prefs.getString("wellness_plan", "Your plan will appear here. Add preferences and tap Build My Wellness Plan."), 13, Color.WHITE, false);
        plan.setPadding(dp(14), dp(14), dp(14), dp(14));
        plan.setBackground(background(panel2, dp(14), Color.TRANSPARENT));
        LinearLayout.LayoutParams planP = params();
        planP.topMargin = dp(10);
        coach.addView(plan, planP);
        build.setOnClickListener(v -> {
            String key = api.getText().toString().trim();
            String ageText = age.getText().toString().trim();
            if (key.isEmpty()) { toast("Enter your Gemini API key to generate a plan."); return; }
            int years;
            try {
                years = Integer.parseInt(ageText);
                if (years < 1 || years > 100) throw new NumberFormatException();
            } catch (Exception ex) { toast("Enter a valid age."); return; }
            String w = weight.getText().toString().trim();
            String h = height.getText().toString().trim();
            prefs.edit().putString("wellness_age", ageText).putString("wellness_weight", w)
                .putString("wellness_height", h).apply();
            String context = "Create a practical, non-restrictive wellbeing routine for a " + years + "-year-old user. ";
            if (!w.isEmpty()) context += "User provided weight " + w + " kg. ";
            if (!h.isEmpty()) context += "User provided height " + h + " cm. ";
            context += "Food preferences and schedule: " + preferences.getText().toString().trim() + ". ";
            context += "Give balanced sample meals and snacks without calorie counts, no calorie deficit, no target weight, no fasting, and no supplements. Include hydration, sleep, and a beginner-friendly weekly movement schedule. For a teen, suggest enjoyable daily activity and strength/bone-strengthening activity about three days a week when appropriate; start gradually, use good form and adult/coach supervision in a gym. Explain that food needs vary during growth and a parent/guardian plus pediatric healthcare professional should help with weight or growth concerns. Do not diagnose or claim medical precision. Use short headings and bullets.";
            build.setEnabled(false);
            build.setText("BUILDING YOUR PLAN…");
            new Thread(() -> {
                String answer = askGemini(key, context, "gemini-2.5-flash");
                runOnUiThread(() -> {
                    build.setEnabled(true);
                    build.setText("✦  BUILD MY WELLNESS PLAN");
                    plan.setText(answer);
                    prefs.edit().putString("wellness_plan", answer).apply();
                });
            }).start();
        });
        body.addView(coach, params());
        space(12);

        int total = prefs.getInt("calorie_total", 0);
        LinearLayout tracker = cardBox();
        tracker.addView(label("OPTIONAL FOOD LOG", 11, cyan, true));
        TextView totalText = label(total + " kcal logged", 22, Color.WHITE, true);
        totalText.setPadding(0, dp(5), 0, dp(4));
        tracker.addView(totalText);
        tracker.addView(label("This is a record of foods you choose to log, not a daily target or a judgement of how much you should eat.", 11, muted, false));
        EditText food = field("Food name", false);
        EditText calories = field("Calories on food label (optional)", false);
        calories.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText servings = field("Servings (defaults to 1)", false);
        servings.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        for (EditText e : new EditText[]{food, calories, servings}) {
            LinearLayout.LayoutParams p = params();
            p.topMargin = dp(8);
            tracker.addView(e, p);
        }
        Button add = button("＋  LOG FOOD", panel2);
        add.setAllCaps(false);
        LinearLayout.LayoutParams addP = params();
        addP.topMargin = dp(10);
        tracker.addView(add, addP);
        add.setOnClickListener(v -> {
            try {
                String name = food.getText().toString().trim();
                String calorieText = calories.getText().toString().trim();
                String servingText = servings.getText().toString().trim();
                if (name.isEmpty()) throw new NumberFormatException();
                int added = 0;
                if (!calorieText.isEmpty()) {
                    double per = Double.parseDouble(calorieText);
                    double count = servingText.isEmpty() ? 1 : Double.parseDouble(servingText);
                    if (per < 0 || count <= 0) throw new NumberFormatException();
                    added = (int)Math.round(per * count);
                }
                prefs.edit().putInt("calorie_total", prefs.getInt("calorie_total", 0) + added)
                    .putString("last_food", name).putInt("last_food_calories", added).apply();
                showCalories();
                toast("Food logged");
            } catch (Exception ex) { toast("Enter a food name and valid optional label values."); }
        });
        body.addView(tracker, params());
        space(8);
        Button reset = button("RESET FOOD LOG", panel2);
        body.addView(reset, params());
        reset.setOnClickListener(v -> {
            prefs.edit().putInt("calorie_total", 0).remove("last_food").remove("last_food_calories").apply();
            showCalories();
        });
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
