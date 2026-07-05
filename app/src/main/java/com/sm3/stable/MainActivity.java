package com.sm3.stable;

import android.app.Activity;
import android.os.Bundle;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Intent;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_FOLDER = 1001;

    private final ArrayList<Uri> tracks = new ArrayList<>();
    private int index = 0;
    private MediaPlayer player;

    private TextView title;
    private TextView status;
    private Button playPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 36, 32, 32);
        root.setBackgroundColor(Color.rgb(16, 16, 20));

        TextView appTitle = new TextView(this);
        appTitle.setText("SM3 Stable");
        appTitle.setTextSize(28);
        appTitle.setTextColor(Color.rgb(215, 181, 109));
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(appTitle);

        title = new TextView(this);
        title.setText("폴더를 선택하세요");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 30, 0, 10);
        root.addView(title);

        status = new TextView(this);
        status.setText("대기 중");
        status.setTextSize(14);
        status.setTextColor(Color.LTGRAY);
        root.addView(status);

        Button folder = new Button(this);
        folder.setText("📂 폴더 선택");
        folder.setTextSize(18);
        folder.setOnClickListener(v -> openFolder());
        root.addView(folder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 24, 0, 0);

        Button prev = new Button(this);
        prev.setText("◀ 이전");
        prev.setOnClickListener(v -> previous());

        playPause = new Button(this);
        playPause.setText("▶/Ⅱ");
        playPause.setOnClickListener(v -> toggle());

        Button next = new Button(this);
        next.setText("다음 ▶");
        next.setOnClickListener(v -> next());

        row.addView(prev, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(playPause, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(next, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(row);

        TextView note = new TextView(this);
        note.setText("\n오늘 마무리 안정판: 폴더 선택 → 음악 목록 생성 → 첫 곡 자동 재생 → 이전/다음/일시정지");
        note.setTextSize(13);
        note.setTextColor(Color.GRAY);
        root.addView(note);

        setContentView(root);
    }

    private void openFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri treeUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            loadFolder(treeUri);
        }
    }

    private void loadFolder(Uri treeUri) {
        tracks.clear();

        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);

        Cursor c = getContentResolver().query(children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        }, null, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                String childId = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);
                if (name == null) name = "";
                if (mime == null) mime = "";

                String lower = name.toLowerCase(Locale.ROOT);
                boolean audio = mime.startsWith("audio/")
                        || lower.endsWith(".mp3")
                        || lower.endsWith(".wav")
                        || lower.endsWith(".flac")
                        || lower.endsWith(".m4a")
                        || lower.endsWith(".ogg");

                if (audio) {
                    tracks.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId));
                }
            }
            c.close();
        }

        Collections.sort(tracks, Comparator.comparing(Uri::toString));
        index = 0;
        status.setText("불러온 곡: " + tracks.size() + "개");

        if (tracks.isEmpty()) {
            Toast.makeText(this, "이 폴더에 음악 파일이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        playCurrent();
    }

    private void playCurrent() {
        if (tracks.isEmpty()) return;

        releasePlayer();

        try {
            player = new MediaPlayer();
            player.setDataSource(this, tracks.get(index));
            player.setOnPreparedListener(mp -> {
                mp.start();
                title.setText("재생 중: " + (index + 1) + " / " + tracks.size());
                status.setText(tracks.get(index).getLastPathSegment());
            });
            player.setOnCompletionListener(mp -> next());
            player.prepareAsync();
        } catch (Exception e) {
            status.setText("재생 오류: " + e.getMessage());
            Toast.makeText(this, "재생할 수 없는 파일입니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void next() {
        if (tracks.isEmpty()) return;
        index = (index + 1) % tracks.size();
        playCurrent();
    }

    private void previous() {
        if (tracks.isEmpty()) return;
        index = (index == 0) ? tracks.size() - 1 : index - 1;
        playCurrent();
    }

    private void toggle() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
            status.setText("일시정지");
        } else {
            player.start();
            status.setText("재생 중");
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
