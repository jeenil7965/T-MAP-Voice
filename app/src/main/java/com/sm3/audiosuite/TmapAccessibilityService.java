package com.sm3.audiosuite;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.speech.tts.TextToSpeech;
import android.content.*;
import java.util.*;

public class TmapAccessibilityService extends AccessibilityService {
    static TextToSpeech tts;
    static long lastTime = 0;
    static String last = "";
    static String voiceMode = "목포 아재";

    @Override public void onCreate() { super.onCreate(); setupTts(this); loadVoice(this); }

    static void loadVoice(Context c) {
        voiceMode = c.getSharedPreferences("sm3", MODE_PRIVATE).getString("voice", "목포 아재");
        applyTone();
    }

    public static void setVoiceMode(Context c, String mode) {
        voiceMode = mode;
        c.getSharedPreferences("sm3", MODE_PRIVATE).edit().putString("voice", mode).apply();
        setupTts(c); applyTone();
    }

    static void setupTts(Context c) {
        if (tts == null) tts = new TextToSpeech(c.getApplicationContext(), s -> {
            if (s == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.KOREAN); applyTone(); }
        });
    }

    static void applyTone() {
        if (tts == null) return;
        if (voiceMode.contains("아주머니")) { tts.setSpeechRate(0.78f); tts.setPitch(1.02f); }
        else if (voiceMode.contains("차분")) { tts.setSpeechRate(0.74f); tts.setPitch(0.90f); }
        else if (voiceMode.contains("표준")) { tts.setSpeechRate(0.92f); tts.setPitch(1.00f); }
        else { tts.setSpeechRate(0.78f); tts.setPitch(0.84f); }
    }

    public static void speakStatic(Context c, String msg) {
        setupTts(c); loadVoice(c);
        if (tts != null) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "sm3-test");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        CharSequence pkg = e.getPackageName();
        if (pkg == null || !pkg.toString().toLowerCase().contains("tmap")) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        ArrayList<String> texts = new ArrayList<>();
        collect(root, texts);
        String guide = findGuide(texts);
        if (guide != null) speakGuide(guide);
    }

    void collect(AccessibilityNodeInfo n, ArrayList<String> out) {
        if (n == null) return;
        CharSequence t = n.getText();
        if (t != null) addText(t.toString(), out);
        CharSequence d = n.getContentDescription();
        if (d != null) addText(d.toString(), out);
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), out);
    }

    void addText(String raw, ArrayList<String> out) {
        String s = raw.trim();
        if (s.length() < 2 || s.length() > 32) return;
        if (s.contains("검색") || s.contains("메뉴") || s.contains("광고") || s.contains("즐겨찾기")) return;
        out.add(s);
    }

    String findGuide(ArrayList<String> list) {
        for (String s : list) if (isGuide(s)) return s;
        return null;
    }

    boolean isGuide(String s) {
        String x = s.replace(" ", "");
        if (x.length() < 2 || x.length() > 30) return false;
        String[] keys = {"좌회전", "우회전", "유턴", "직진", "단속", "카메라", "목적지", "도착", "고속도로", "분기점", "나들목", "차로", "방면"};
        for (String k : keys) if (x.contains(k)) return true;
        return false;
    }

    void speakGuide(String s) {
        long now = System.currentTimeMillis();
        if (s.equals(last) && now - lastTime < 7000) return;
        last = s; lastTime = now;
        loadVoice(this);
        String msg = toVoiceText(s);
        if (tts != null) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "sm3-guide");
    }

    String toVoiceText(String s) {
        if (voiceMode.contains("표준")) return toStandard(s);
        String x = s.replace("하세요", "").replace("입니다", "").trim();
        if (x.contains("좌회전")) return voiceMode.contains("차분") ? "잠시 후 좌회전이랑께요." : "아따아, 잠시 후 좌회전이랑께요오.";
        if (x.contains("우회전")) return voiceMode.contains("아주머니") ? "오메에, 잠시 후 우회전이랑께요오." : "아따아, 잠시 후 우회전이랑께요오.";
        if (x.contains("유턴")) return "아따아, 앞에서 유턴허면 되것소잉.";
        if (x.contains("단속") || x.contains("카메라")) return "아따아, 앞에 단속 있응께 속도 쪼까 줄이소잉.";
        if (x.contains("목적지") || x.contains("도착")) return "고생 많았소잉, 목적지에 도착했당께라.";
        if (x.contains("고속도로")) return "이제 고속도로로 올라간당께요오.";
        if (x.contains("차로")) return "차로 잘 맞춰서 천천히 가소잉.";
        return "아따아, " + x + " 이랑께요오.";
    }

    String toStandard(String s) {
        if (s.contains("좌회전")) return "잠시 후 좌회전입니다.";
        if (s.contains("우회전")) return "잠시 후 우회전입니다.";
        if (s.contains("유턴")) return "앞에서 유턴입니다.";
        if (s.contains("단속") || s.contains("카메라")) return "앞에 단속 카메라가 있습니다. 속도를 줄여 주세요.";
        if (s.contains("목적지") || s.contains("도착")) return "목적지에 도착했습니다.";
        return s;
    }

    @Override public void onInterrupt() { }
    @Override public void onDestroy() { super.onDestroy(); if (tts != null) { tts.shutdown(); tts = null; } }
}
