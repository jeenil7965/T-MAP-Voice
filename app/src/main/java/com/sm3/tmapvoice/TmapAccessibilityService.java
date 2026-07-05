package com.sm3.tmapvoice;

import android.accessibilityservice.AccessibilityService;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmapAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private String lastSpoken = "";
    private long lastTime = 0;

    // Only short real guidance phrases are spoken.
    private static final Pattern GUIDE_PATTERN = Pattern.compile(
            "((?:\\d+(?:\\.\\d+)?\\s?(?:m|km|미터|킬로미터)\\s*)?(?:앞에서|앞|후|뒤|이후)?\\s*(?:잠시 후\\s*)?" +
            "(?:좌회전|우회전|유턴|직진|진출|분기점|고속도로|회전교차로|차로|단속|목적지|도착)" +
            "(?:입니다|하세요|해 주세요|합니다|합니다.)?)"
    );

    private static final String[] GUIDE_WORDS = {
            "좌회전", "우회전", "유턴", "직진", "진출", "분기점", "고속도로",
            "회전교차로", "차로", "단속", "목적지", "도착"
    };

    private static final String[] UI_NOISE_WORDS = {
            "검색", "홈", "메뉴", "설정", "즐겨찾기", "최근목적지", "주유소", "충전소",
            "음식점", "카페", "편의점", "주차장", "경찰", "CCTV", "제보", "경로", "취소",
            "닫기", "확대", "축소", "지도", "티맵", "T map", "운전점수", "대중교통"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.KOREAN);
            tts.setSpeechRate(0.90f);
            tts.setPitch(1.00f);
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

        // Tmap only. If your Tmap package name is different, add it here.
        if (!packageName.contains("tmap") && !packageName.contains("skt")) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return;

        List<String> candidates = new ArrayList<>();
        collectCandidateTexts(getRootInActiveWindow(), candidates, 0);
        String guide = pickBestGuide(candidates);
        if (guide == null) return;
        speakOnce(toMokpoDialect(guide));
    }

    private void collectCandidateTexts(AccessibilityNodeInfo node, List<String> out, int depth) {
        if (node == null || depth > 7) return;
        addIfCandidate(node.getText(), out);
        addIfCandidate(node.getContentDescription(), out);
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCandidateTexts(node.getChild(i), out, depth + 1);
        }
    }

    private void addIfCandidate(CharSequence seq, List<String> out) {
        if (seq == null) return;
        String s = seq.toString().replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (s.length() < 2 || s.length() > 45) return;
        if (!containsGuideWord(s)) return;
        if (containsNoiseWord(s)) return;
        Matcher m = GUIDE_PATTERN.matcher(s);
        if (m.find()) out.add(cleanGuide(m.group(1)));
    }

    private boolean containsGuideWord(String s) {
        for (String w : GUIDE_WORDS) if (s.contains(w)) return true;
        return false;
    }

    private boolean containsNoiseWord(String s) {
        for (String w : UI_NOISE_WORDS) if (s.contains(w)) return true;
        return false;
    }

    private String pickBestGuide(List<String> candidates) {
        if (candidates.isEmpty()) return null;
        Set<String> unique = new LinkedHashSet<>(candidates);
        String best = null;
        for (String c : unique) {
            if (c == null || c.length() < 2) continue;
            if (best == null) best = c;
            // Prefer distance-based guidance such as "300m 앞 우회전".
            if (c.matches(".*\\d+.*") && !best.matches(".*\\d+.*")) best = c;
            // Prefer shorter phrase, not full screen text.
            if (best != null && c.length() < best.length() && containsGuideWord(c)) best = c;
        }
        return best;
    }

    private String cleanGuide(String s) {
        s = s.replaceAll("\\s+", " ").trim();
        s = s.replace("m앞", "미터 앞");
        s = s.replace("km앞", "킬로미터 앞");
        return s;
    }


    private String toMokpoDialect(String guide) {
        if (guide == null) return "";
        String g = cleanGuide(guide);

        String distance = "";
        Matcher dm = Pattern.compile("(\\d+(?:\\.\\d+)?\\s?(?:미터|킬로미터|m|km))").matcher(g);
        if (dm.find()) distance = dm.group(1).replace("m", "미터").replace("km", "킬로미터").trim();

        boolean soon = g.contains("잠시") || g.contains("곧") || g.contains("후");
        boolean now = g.contains("지금");
        boolean hasDistance = distance.length() > 0;

        if (g.contains("좌회전")) {
            if (now) return "인자 왼쪽으로 도시오잉.";
            if (soon && !hasDistance) return "쪼까 있다가 왼쪽으로 도실라요.";
            if (hasDistance) return distance + " 앞에서 왼쪽으로 도실라요. 잘 보고 가랑께요.";
            return "왼쪽으로 도실라요.";
        }
        if (g.contains("우회전")) {
            if (now) return "인자 오른쪽으로 도시오잉.";
            if (soon && !hasDistance) return "쪼까 있다가 오른쪽으로 도실라요.";
            if (hasDistance) return distance + " 앞에서 오른쪽으로 도실라요. 천천히 가랑께요.";
            return "오른쪽으로 도실라요.";
        }
        if (g.contains("유턴")) {
            if (now) return "인자 유턴하시오잉.";
            if (hasDistance) return distance + " 앞에서 유턴하실라요.";
            return "가능한 데서 유턴하실라요.";
        }
        if (g.contains("직진")) {
            if (hasDistance) return distance + " 동안 쭉 가믄 되겄습니다.";
            return "그대로 쭉 가믄 되겄습니다.";
        }
        if (g.contains("차로")) {
            if (g.contains("좌") || g.contains("왼")) return "왼쪽 차로로 붙어 주실라요.";
            if (g.contains("우") || g.contains("오른")) return "오른쪽 차로로 붙어 주실라요.";
            return "차로 잘 맞춰서 가실라요.";
        }
        if (g.contains("단속")) {
            return "앞에 단속 있당께요. 속도 살살 맞춰 가시오.";
        }
        if (g.contains("고속도로")) {
            if (g.contains("진출") || g.contains("나들목")) return "쪼까 있다가 고속도로에서 빠져나갈라요.";
            return "인자 고속도로 타실라요. 안전거리 챙기랑께요.";
        }
        if (g.contains("분기점")) {
            return "앞에 분기점 나옵니다잉. 차로 잘 보고 가실라요.";
        }
        if (g.contains("회전교차로")) {
            return "회전교차로 나옵니다잉. 천천히 돌믄 되겄습니다.";
        }
        if (g.contains("목적지") || g.contains("도착")) {
            if (soon) return "쪼까만 더 가믄 목적지 도착이랑께요.";
            return "목적지 도착했소잉. 오늘도 고생 많았당께요.";
        }
        return g;
    }

    private void speakOnce(String guide) {
        long now = System.currentTimeMillis();
        if (guide.equals(lastSpoken) && now - lastTime < 12000) return;
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
