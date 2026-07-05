package com.sm3.audiosuite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_FOLDER = 1001;
    private final ArrayList<Uri> tracks = new ArrayList<>();
    private int index = 0;
    private MediaPlayer player;
    private TextView status;
    private boolean shuffle = false;
    private final Random random = new Random();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 7);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("SM3 Audio Suite V1.2\n목포 억양 TTS + 폴더 재생"); title.setTextSize(24); title.setGravity(Gravity.CENTER); root.addView(title);
        Button acc = button("접근성 설정 열기"); acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(acc);
        Button folder = button("음악 폴더 선택"); folder.setOnClickListener(v -> openFolder()); root.addView(folder);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER); row.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = button("◀ 이전"); Button play = button("▶ 재생"); Button next = button("다음 ▶");
        prev.setOnClickListener(v -> prev()); play.setOnClickListener(v -> toggle()); next.setOnClickListener(v -> next());
        row.addView(prev); row.addView(play); row.addView(next); root.addView(row);
        Button sh = button("셔플 OFF"); sh.setOnClickListener(v -> { shuffle=!shuffle; sh.setText(shuffle?"셔플 ON":"셔플 OFF"); }); root.addView(sh);
        status = new TextView(this); status.setText("티맵 음성은 묵음 처리 후 접근성을 켜주세요.\nOGG/MP3/M4A/WAV 폴더 재생 가능."); status.setTextSize(16); status.setPadding(0,30,0,0); root.addView(status);
        setContentView(root);
    }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(18); b.setAllCaps(false); return b; }
    private void openFolder(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i, REQ_FOLDER); }
    @Override protected void onActivityResult(int r,int c,Intent data){ super.onActivityResult(r,c,data); if(r==REQ_FOLDER&&c==RESULT_OK&&data!=null){ Uri u=data.getData(); getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); tracks.clear(); scan(DocumentFile.fromTreeUri(this,u)); status.setText("곡 " + tracks.size() + "개 찾음"); index=0; } }
    private void scan(DocumentFile f){ if(f==null) return; if(f.isDirectory()){ for(DocumentFile x:f.listFiles()) scan(x); } else { String n=f.getName()==null?"":f.getName().toLowerCase(); if(n.endsWith(".mp3")||n.endsWith(".m4a")||n.endsWith(".ogg")||n.endsWith(".wav")||n.endsWith(".flac")) tracks.add(f.getUri()); } }
    private void toggle(){ if(player!=null&&player.isPlaying()){ player.pause(); status.setText("일시정지"); } else playCurrent(); }
    private void playCurrent(){ if(tracks.isEmpty()){ status.setText("먼저 음악 폴더를 선택하세요."); return; } try{ if(player!=null){player.release();} player=new MediaPlayer(); player.setDataSource(this, tracks.get(index)); player.setOnCompletionListener(mp->next()); player.prepare(); player.start(); status.setText("재생 중: " + (index+1) + "/" + tracks.size()); }catch(Exception e){ status.setText("재생 오류: "+e.getMessage()); } }
    private void next(){ if(tracks.isEmpty()) return; index = shuffle ? random.nextInt(tracks.size()) : (index+1)%tracks.size(); playCurrent(); }
    private void prev(){ if(tracks.isEmpty()) return; index = (index-1+tracks.size())%tracks.size(); playCurrent(); }
    @Override protected void onDestroy(){ if(player!=null) player.release(); super.onDestroy(); }
}
