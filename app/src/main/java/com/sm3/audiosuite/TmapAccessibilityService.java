package com.sm3.audiosuite;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.speech.tts.TextToSpeech;
import android.content.*;
import java.util.*;
import java.util.regex.*;

public class TmapAccessibilityService extends AccessibilityService {
    static TextToSpeech tts;
    static long lastTime = 0;
    static String last = "";
    static String voiceMode = "목포 아재";
    static final String PREF = "sm3";
    static final String KEY_VOICE = "voice";

    private static final Pattern DISTANCE = Pattern.compile(".*(\\d+\\s?m|\\d+\\s?km|미터|킬로).*", Pattern.CASE_INSENSITIVE);
    private static final String[] GUIDE_KEYS = {"좌회전","우회전","유턴","직진","단속","카메라","목적지","도착","고속도로","분기점","나들목","차로","방면","합류","터널","교차로","회전교차로"};
    private static final String[] BLOCK_KEYS = {"검색","메뉴","광고","즐겨찾기","최근목적지","설정","공유","로그인","닫기","확대","축소","음악","홈","이퀄라이저","리버브","스펙트럼","SM3"};

    @Override public void onCreate() { super.onCreate(); setupTts(this); loadVoice(this); }

    static void loadVoice(Context c) {
        voiceMode = c.getSharedPreferences(PREF, MODE_PRIVATE).getString(KEY_VOICE, "목포 아재");
        applyTone();
    }

    public static void setVoiceMode(Context c, String mode) {
        voiceMode = mode;
        c.getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(KEY_VOICE, mode).commit();
        setupTts(c); applyTone();
    }

    static void setupTts(Context c) {
        if (tts == null) tts = new TextToSpeech(c.getApplicationContext(), s -> {
            if (s == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.KOREAN); applyTone(); }
        });
    }

    static void applyTone() {
        if (tts == null) return;
        if (voiceMode.contains("아주머니")) { tts.setSpeechRate(0.79f); tts.setPitch(1.13f); }
        else if (voiceMode.contains("차분")) { tts.setSpeechRate(0.68f); tts.setPitch(0.90f); }
        else if (voiceMode.contains("표준")) { tts.setSpeechRate(0.95f); tts.setPitch(1.00f); }
        else { tts.setSpeechRate(0.72f); tts.setPitch(0.78f); }
    }

    public static void speakStatic(Context c, String msg) {
        setupTts(c); loadVoice(c);
        if (tts != null) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "sm3-test");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        CharSequence pkg = e.getPackageName();
        String pkgName = pkg == null ? "" : pkg.toString().toLowerCase(Locale.ROOT);
        if (!isTmapPackage(pkgName)) return;

        ArrayList<String> candidates = new ArrayList<>();
        if (e.getText() != null) for (CharSequence cs : e.getText()) if (cs != null) addText(cs.toString(), candidates);
        if (e.getContentDescription() != null) addText(e.getContentDescription().toString(), candidates);

        AccessibilityNodeInfo src = e.getSource();
        if (src != null) collect(src, candidates, 0);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) collect(root, candidates, 0);

        String guide = findGuide(candidates);
        if (guide != null) speakGuide(guide);
    }

    boolean isTmapPackage(String pkg) {
        if (pkg == null) return false;
        if (pkg.contains("tmap")) return true;
        if (pkg.contains("skt") && pkg.contains("nav")) return true;
        if (pkg.contains("mobility") && pkg.contains("map")) return true;
        return false;
    }

    void collect(AccessibilityNodeInfo n, ArrayList<String> out, int depth) {
        if (n == null || depth > 8) return;
        CharSequence t = n.getText();
        if (t != null) addText(t.toString(), out);
        CharSequence d = n.getContentDescription();
        if (d != null) addText(d.toString(), out);
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), out, depth + 1);
    }

    void addText(String raw, ArrayList<String> out) {
        if (raw == null) return;
        String s = normalize(raw);
        if (s.length() < 2 || s.length() > 48) return;
        for (String b : BLOCK_KEYS) if (s.replace(" ", "").contains(b)) return;
        if (!out.contains(s)) out.add(s);
    }

    String normalize(String raw) {
        return raw.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    String findGuide(ArrayList<String> list) {
        // 1) 방향 키워드가 있는 짧은 안내 우선
        for (String s : list) if (isStrongGuide(s)) return s;
        // 2) 거리와 방향이 나뉘어 잡히는 경우 합치기
        String dist = null, dir = null;
        for (String s : list) {
            String x = s.replace(" ", "");
            if (dist == null && DISTANCE.matcher(x).matches() && x.length() <= 12) dist = s;
            if (dir == null && containsAny(x, GUIDE_KEYS) && x.length() <= 20) dir = s;
        }
        if (dir != null) return dist == null ? dir : dist + " " + dir;
        return null;
    }

    boolean isStrongGuide(String s) {
        String x = s.replace(" ", "");
        if (x.length() < 2 || x.length() > 42) return false;
        if (!containsAny(x, GUIDE_KEYS)) return false;
        // 티맵 지도 화면의 잡문구 방지: 길안내는 보통 거리/방향/목적지/단속 중 하나가 명확함
        if (x.contains("좌회전") || x.contains("우회전") || x.contains("유턴") || x.contains("직진")) return true;
        if (x.contains("단속") || x.contains("카메라") || x.contains("목적지") || x.contains("도착")) return true;
        return DISTANCE.matcher(x).matches();
    }

    boolean containsAny(String x, String[] arr) { for (String k : arr) if (x.contains(k)) return true; return false; }

    void speakGuide(String s) {
        long now = System.currentTimeMillis();
        String key = simplifyKey(s);
        if (key.equals(last) && now - lastTime < 9000) return;
        last = key; lastTime = now;
        loadVoice(this);
        String msg = toVoiceText(s);
        if (tts != null) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "sm3-guide");
    }

    String simplifyKey(String s) {
        return s.replaceAll("\\d+", "#").replace(" ", "");
    }

    String toVoiceText(String s) {
        loadVoice(this);
        if (voiceMode.contains("표준")) return toStandard(s);
        String x = s.replace("하세요", "").replace("입니다", "").replace("안내", "").trim();
        if (voiceMode.contains("아주머니")) return toAjumma(x);
        if (voiceMode.contains("차분")) return toCalm(x);
        return toAjae(x);
    }

    String toAjae(String x) {
        if (x.contains("좌회전")) return "아따아, " + distancePrefix(x) + " 좌회전이랑께요오.";
        if (x.contains("우회전")) return "아따아, " + distancePrefix(x) + " 우회전이랑께요오.";
        if (x.contains("유턴")) return "워메, 앞에서 유턴허면 되것소잉.";
        if (x.contains("직진")) return "쭈욱, 직진허면 된당께요.";
        if (x.contains("단속") || x.contains("카메라")) return "아따아, 앞에 단속 있응께, 속도 쪼까 줄이소잉.";
        if (x.contains("목적지") || x.contains("도착")) return "고생 많았소잉, 목적지에 도착했당께라.";
        if (x.contains("고속도로")) return "자아, 이제 고속도로로 올라간당께요오.";
        if (x.contains("차로")) return "차로 잘 맞춰서, 천천히 가소잉.";
        return "아따아, " + x + " 이랑께요오.";
    }

    String toAjumma(String x) {
        if (x.contains("좌회전")) return "오메에, " + distancePrefix(x) + " 왼쪽으로 돌으랑께요오.";
        if (x.contains("우회전")) return "오메에, " + distancePrefix(x) + " 오른쪽으로 돌으랑께요오.";
        if (x.contains("유턴")) return "아이고야, 앞에서 유턴허면 된당께요오.";
        if (x.contains("직진")) return "그대로 쭈욱 가면 된당께요오.";
        if (x.contains("단속") || x.contains("카메라")) return "오메, 단속 카메라 있응께, 속도 좀 줄이소잉.";
        if (x.contains("목적지") || x.contains("도착")) return "아이고 고생했소잉, 목적지 도착했당께요오.";
        return "오메에, " + x + " 이랑께요오.";
    }

    String toCalm(String x) {
        if (x.contains("좌회전")) return distancePrefix(x) + " 좌회전이랑께요.";
        if (x.contains("우회전")) return distancePrefix(x) + " 우회전이랑께요.";
        if (x.contains("유턴")) return "앞에서, 유턴허면 되것습니다.";
        if (x.contains("단속") || x.contains("카메라")) return "앞에 단속 있응께, 속도 조금 줄여주세요.";
        if (x.contains("목적지") || x.contains("도착")) return "목적지에 도착했당께요. 고생하셨습니다.";
        return x + " 입니다잉.";
    }

    String distancePrefix(String x) {
        Matcher m = Pattern.compile("(\\d+\\s?m|\\d+\\s?km|\\d+미터|\\d+킬로)").matcher(x);
        if (m.find()) return m.group(1) + " 앞에서,";
        return "쪼까 있다가,";
    }

    String toStandard(String s) {
        if (s.contains("좌회전")) return distancePrefix(s).replace("쪼까 있다가,", "잠시 후,") + " 좌회전입니다.";
        if (s.contains("우회전")) return distancePrefix(s).replace("쪼까 있다가,", "잠시 후,") + " 우회전입니다.";
        if (s.contains("유턴")) return "앞에서 유턴입니다.";
        if (s.contains("단속") || s.contains("카메라")) return "앞에 단속 카메라가 있습니다. 속도를 줄여 주세요.";
        if (s.contains("목적지") || s.contains("도착")) return "목적지에 도착했습니다.";
        return s;
    }

    @Override public void onInterrupt() { }
    @Override public void onDestroy() { super.onDestroy(); if (tts != null) { tts.shutdown(); tts = null; } }
}
