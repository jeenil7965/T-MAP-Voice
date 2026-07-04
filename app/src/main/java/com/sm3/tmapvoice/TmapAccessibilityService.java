package com.sm3.tmapvoice;

import android.accessibilityservice.AccessibilityService;
import android.media.AudioAttributes;
import android.os.Bundle;
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
    private final Set<String> keywords = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        keywords.add("좌회전"); keywords.add("우회전"); keywords.add("유턴");
        keywords.add("직진"); keywords.add("분기점"); keywords.add("진출");
        keywords.add("고속도로"); keywords.add("단속"); keywords.add("목적지");
        keywords.add("도착"); keywords.add("차로"); keywords.add("회전교차로");
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.KOREAN);
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.02f);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            tts.setAudioAttributes(attrs);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String packageName = pkg.toString().toLowerCase(Locale.ROOT);
        if (!packageName.contains("tmap") && !packageName.contains("sktelecom")) {
            // Some Tmap builds use different package names. If needed, remove this guard.
        }

        String text = collectText(getRootInActiveWindow());
        if (text == null || text.length() < 2) return;
        String guide = pickGuideText(text);
        if (guide == null) return;
        speakOnce(guide);
    }

    private String collectText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        collect(node, sb, 0);
        return sb.toString();
    }

    private void collect(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null || depth > 8) return;
        CharSequence txt = node.getText();
        if (txt != null && txt.length() > 0) sb.append(txt).append(' ');
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.length() > 0) sb.append(desc).append(' ');
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), sb, depth + 1);
    }

    private String pickGuideText(String all) {
        String normalized = all.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        boolean hit = false;
        for (String k : keywords) if (normalized.contains(k)) { hit = true; break; }
        if (!hit) return null;

        // Keep it short for navigation voice.
        String[] parts = normalized.split(" ");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (out.length() + p.length() > 80) break;
            out.append(p).append(' ');
        }
        return out.toString().trim();
    }

    private void speakOnce(String guide) {
        long now = System.currentTimeMillis();
        if (guide.equals(lastSpoken) && now - lastTime < 8000) return;
        lastSpoken = guide;
        lastTime = now;
        Bundle params = new Bundle();
        tts.speak(guide, TextToSpeech.QUEUE_FLUSH, params, "tmap_guide");
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
