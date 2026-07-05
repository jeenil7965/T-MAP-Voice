package com.sm3.audiosuite;

import android.accessibilityservice.AccessibilityService;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TmapAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private String lastSpoken = "";
    private long lastTime = 0;
    private final String[] keywords = {"좌회전","우회전","유턴","직진","단속","카메라","과속","목적지","도착","고속도로","나들목","진출","차로","회전교차로","보호구역"};

    @Override public void onServiceConnected() { tts = new TextToSpeech(this, this); }
    @Override public void onInit(int status) { if (tts != null) { tts.setLanguage(Locale.KOREAN); tts.setSpeechRate(0.82f); tts.setPitch(0.88f); } }
    @Override public void onInterrupt() {}
    @Override public void onDestroy() { if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !pkg.toString().toLowerCase().contains("tmap")) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        Set<String> texts = new HashSet<>();
        collect(root, texts);
        for (String text : texts) {
            String candidate = filter(text);
            if (candidate.length() > 0) speak(candidate);
        }
    }

    private void collect(AccessibilityNodeInfo node, Set<String> out) {
        if (node == null) return;
        CharSequence t = node.getText();
        if (t != null) out.add(t.toString());
        CharSequence d = node.getContentDescription();
        if (d != null) out.add(d.toString());
        for (int i=0;i<node.getChildCount();i++) collect(node.getChild(i), out);
    }

    private String filter(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() < 2 || s.length() > 55) return "";
        boolean has = false;
        for (String k: keywords) if (s.contains(k)) { has = true; break; }
        if (!has) return "";
        if (s.contains("검색") || s.contains("메뉴") || s.contains("즐겨찾기") || s.contains("주변") || s.contains("설정")) return "";
        return s;
    }

    private void speak(String raw) {
        String msg = MokpoSpeech.convert(raw);
        if (msg.length() == 0) return;
        long now = System.currentTimeMillis();
        if (msg.equals(lastSpoken) && now - lastTime < 9000) return;
        lastSpoken = msg; lastTime = now;
        if (tts != null) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "mokpo_guide");
    }
}
