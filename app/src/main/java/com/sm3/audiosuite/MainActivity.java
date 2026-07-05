package com.sm3.audiosuite;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static ArrayList<Uri> tracks = new ArrayList<>();
    static int index = 0;
    static MediaPlayer player;
    static TextToSpeech tts;

    final int PICK = 77;
    final int BG = Color.rgb(5, 8, 12);
    final int PANEL = Color.rgb(13, 17, 24);
    final int PANEL2 = Color.rgb(20, 25, 34);
    final int GOLD = Color.rgb(227, 184, 103);
    final int GOLD2 = Color.rgb(146, 105, 45);
    final int TEXT = Color.rgb(246, 242, 233);
    final int SUB = Color.rgb(156, 166, 175);
    final int ACCENT = Color.rgb(38, 174, 122);

    LinearLayout root, tabRow, content;
    TextView status, trackInfo, voiceInfo, nowTab;
    String selectedVoice = "목포 아재";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        selectedVoice = getSharedPreferences("sm3", MODE_PRIVATE).getString("voice", "목포 아재");
        initTts();
        buildUi();
        showHome();
    }

    void initTts() {
        tts = new TextToSpeech(this, s -> {
            if (s == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                applyVoiceTone(selectedVoice);
            }
        });
    }

    void applyVoiceTone(String voice) {
        if (tts == null) return;
        if (voice.contains("아주머니")) { tts.setSpeechRate(0.78f); tts.setPitch(1.02f); }
        else if (voice.contains("차분")) { tts.setSpeechRate(0.74f); tts.setPitch(0.90f); }
        else if (voice.contains("표준")) { tts.setSpeechRate(0.92f); tts.setPitch(1.00f); }
        else { tts.setSpeechRate(0.78f); tts.setPitch(0.84f); }
    }

    int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    GradientDrawable bg(int color, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        if (stroke > 0) g.setStroke(dp(1), stroke == 2 ? GOLD : Color.rgb(45, 54, 66));
        return g;
    }

    TextView tv(String s, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT, style);
        t.setIncludeFontPadding(true); return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(15); b.setTextColor(TEXT); b.setAllCaps(false);
        b.setBackground(bg(PANEL2, 1, 18));
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    TextView navBtn(String s) {
        TextView t = tv(s, 14, SUB, Typeface.BOLD);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(8), dp(10), dp(8), dp(10));
        t.setBackground(bg(PANEL, 1, 20));
        return t;
    }

    TextView pill(String s, int color) {
        TextView t = tv(s, 13, color, Typeface.BOLD);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(10), dp(8), dp(10), dp(8));
        t.setBackground(bg(Color.rgb(18, 23, 31), 1, 18));
        return t;
    }

    void space(LinearLayout l, int h) { Space s = new Space(this); l.addView(s, new LinearLayout.LayoutParams(1, dp(h))); }

    void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(16));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackground(bg(PANEL, 2, 24));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));
        TextView logo = tv("SM3", 28, GOLD, Typeface.BOLD);
        titleRow.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        nowTab = pill("HOME", GOLD);
        titleRow.addView(nowTab, new LinearLayout.LayoutParams(dp(110), dp(42)));

        status = tv("티맵 묵음 + SM3 음성 오버레이 대기 중", 13, SUB, Typeface.NORMAL);
        header.addView(status);
        trackInfo = tv("음악 대기 중", 20, TEXT, Typeface.BOLD);
        trackInfo.setPadding(0, dp(8), 0, 0);
        header.addView(trackInfo);

        LinearLayout miniBars = new LinearLayout(this);
        miniBars.setGravity(Gravity.BOTTOM);
        miniBars.setPadding(0, dp(10), 0, 0);
        header.addView(miniBars, new LinearLayout.LayoutParams(-1, dp(54)));
        for (int i = 0; i < 24; i++) {
            TextView bar = new TextView(this);
            int h = 10 + (int)(Math.abs(Math.sin(i * 0.65)) * 34);
            bar.setBackgroundColor(i % 5 == 0 ? GOLD : Color.rgb(58, 75, 88));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(h), 1);
            lp.setMargins(dp(2), 0, dp(2), 0);
            miniBars.addView(bar, lp);
        }

        space(root, 10);
        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER);
        tabRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabRow.setBackground(bg(Color.rgb(9, 12, 18), 1, 24));
        root.addView(tabRow, new LinearLayout.LayoutParams(-1, -2));

        TextView home = navBtn("홈"); TextView music = navBtn("음악"); TextView voice = navBtn("음성"); TextView setting = navBtn("설정");
        tabRow.addView(home, new LinearLayout.LayoutParams(0, dp(52), 1));
        tabRow.addView(music, new LinearLayout.LayoutParams(0, dp(52), 1));
        tabRow.addView(voice, new LinearLayout.LayoutParams(0, dp(52), 1));
        tabRow.addView(setting, new LinearLayout.LayoutParams(0, dp(52), 1));
        home.setOnClickListener(v -> showHome());
        music.setOnClickListener(v -> showMusic());
        voice.setOnClickListener(v -> showVoice());
        setting.setOnClickListener(v -> showSettings());

        space(root, 10);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        setContentView(scroll);
    }

    void clear(String tab) { content.removeAllViews(); nowTab.setText(tab); }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(bg(PANEL, 1, 22));
        content.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    void showHome() {
        clear("HOME");
        LinearLayout c = card();
        c.addView(tv("프리미엄 드라이브 모드", 22, GOLD, Typeface.BOLD));
        c.addView(tv("큰 버튼 · 폴드 화면 대응 · 티맵 안내만 필터링", 14, SUB, Typeface.NORMAL));
        space(c, 14);
        Button test = btn("🎙  목포 안내 테스트");
        test.setOnClickListener(v -> speakTest());
        c.addView(test, new LinearLayout.LayoutParams(-1, dp(64)));
        space(c, 10);
        Button folder = btn("📁  음악 파일 추가");
        folder.setOnClickListener(v -> pickAudio());
        c.addView(folder, new LinearLayout.LayoutParams(-1, dp(64)));
        space(c, 10);
        addTransport(c);
    }

    void showMusic() {
        clear("MUSIC");
        LinearLayout c = card();
        c.addView(tv("음악 재생기", 22, GOLD, Typeface.BOLD));
        c.addView(tv("여러 음악 파일 선택 후 이전/다음/재생/정지 사용", 14, SUB, Typeface.NORMAL));
        space(c, 14);
        Button folder = btn("📁  음악 파일 선택 / 추가");
        folder.setOnClickListener(v -> pickAudio());
        c.addView(folder, new LinearLayout.LayoutParams(-1, dp(64)));
        space(c, 12);
        addTransport(c);
        space(c, 12);
        TextView list = tv("현재 곡 수: " + tracks.size() + "개", 16, TEXT, Typeface.BOLD);
        c.addView(list);
    }

    void addTransport(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(78)));
        Button prev = btn("⏮\n이전"); Button play = btn("▶/Ⅱ\n재생"); Button next = btn("⏭\n다음"); Button stop = btn("■\n정지");
        row.addView(prev, new LinearLayout.LayoutParams(0, -1, 1));
        row.addView(play, new LinearLayout.LayoutParams(0, -1, 1));
        row.addView(next, new LinearLayout.LayoutParams(0, -1, 1));
        row.addView(stop, new LinearLayout.LayoutParams(0, -1, 1));
        prev.setOnClickListener(v -> previousTrack());
        play.setOnClickListener(v -> togglePlay());
        next.setOnClickListener(v -> nextTrack());
        stop.setOnClickListener(v -> stopTrack());
    }

    void showVoice() {
        clear("VOICE");
        LinearLayout c = card();
        c.addView(tv("음성 선택", 22, GOLD, Typeface.BOLD));
        voiceInfo = tv("현재: " + selectedVoice, 15, SUB, Typeface.NORMAL);
        c.addView(voiceInfo);
        space(c, 12);
        String[] voices = {"목포 아재", "목포 아주머니", "목포 차분한", "표준어 기본"};
        for (String name : voices) {
            Button b = btn((name.equals(selectedVoice) ? "●  " : "○  ") + name);
            b.setOnClickListener(v -> selectVoice(name));
            c.addView(b, new LinearLayout.LayoutParams(-1, dp(60)));
            space(c, 8);
        }
        Button test = btn("선택한 음성으로 테스트");
        test.setOnClickListener(v -> speakTest());
        c.addView(test, new LinearLayout.LayoutParams(-1, dp(64)));
    }

    void showSettings() {
        clear("SETTINGS");
        LinearLayout c = card();
        c.addView(tv("설정", 22, GOLD, Typeface.BOLD));
        c.addView(tv("티맵 음성은 티맵에서 묵음 처리하고, 이 앱의 접근성 서비스를 켜세요.", 14, SUB, Typeface.NORMAL));
        space(c, 14);
        Button acc = btn("♿  접근성 설정 열기");
        acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        c.addView(acc, new LinearLayout.LayoutParams(-1, dp(64)));
        space(c, 10);
        Button ttsSet = btn("🔊  Android TTS 설정 열기");
        ttsSet.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        c.addView(ttsSet, new LinearLayout.LayoutParams(-1, dp(64)));
    }

    void selectVoice(String name) {
        selectedVoice = name;
        getSharedPreferences("sm3", MODE_PRIVATE).edit().putString("voice", name).apply();
        applyVoiceTone(name);
        TmapAccessibilityService.setVoiceMode(this, name);
        status.setText("음성 선택: " + name);
        showVoice();
    }

    String testSentence() {
        if (selectedVoice.contains("표준")) return "잠시 후 좌회전입니다.";
        if (selectedVoice.contains("차분")) return "아따, 잠시 후 좌회전이랑께요.";
        if (selectedVoice.contains("아주머니")) return "오메, 잠시 후 좌회전이랑께요오.";
        return "아따아, 잠시 후 좌회전이랑께요오.";
    }

    void speakTest() { TmapAccessibilityService.speakStatic(this, testSentence()); }

    void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK);
    }

    void previousTrack() { if (tracks.size() > 0) { index = (index - 1 + tracks.size()) % tracks.size(); startTrack(); updateTrack(); } }
    void nextTrack() { if (tracks.size() > 0) { index = (index + 1) % tracks.size(); startTrack(); updateTrack(); } }
    void togglePlay() { if (player != null && player.isPlaying()) player.pause(); else if (player != null) player.start(); else if (tracks.size() > 0) { startTrack(); updateTrack(); } }
    void stopTrack() { if (player != null) { player.stop(); player.release(); player = null; status.setText("음악 정지"); } }

    void updateTrack() {
        trackInfo.setText("재생 중  " + (index + 1) + " / " + tracks.size());
        status.setText("음악 " + tracks.size() + "개 추가됨");
    }

    @Override protected void onActivityResult(int r, int c, Intent data) {
        super.onActivityResult(r, c, data);
        if (r == PICK && c == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) tracks.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) tracks.add(data.getData());
            updateTrack();
        }
    }

    static void startTrack() {
        try {
            if (player != null) player.release();
            player = new MediaPlayer();
            player.setDataSource(App.ctx, tracks.get(index));
            player.prepare(); player.start();
            player.setOnCompletionListener(mp -> { if (tracks.size() > 0) { index = (index + 1) % tracks.size(); startTrack(); } });
        } catch (Exception e) { }
    }

    @Override public void onDestroy() { super.onDestroy(); if (tts != null) tts.shutdown(); }
}
