package com.sm3.audiosuite;

import android.app.*;import android.os.*;import android.content.*;import android.net.*;import android.provider.Settings;import android.speech.tts.TextToSpeech;import android.media.MediaPlayer;import android.view.*;import android.widget.*;import java.util.*;import java.io.*;

public class MainActivity extends Activity{
    static ArrayList<Uri> tracks=new ArrayList<>(); static int index=0; static MediaPlayer player; static TextToSpeech tts; TextView status;
    final int PICK=77;
    @Override public void onCreate(Bundle b){super.onCreate(b); initTts(); buildUi();}
    void initTts(){tts=new TextToSpeech(this, s->{ if(s==TextToSpeech.SUCCESS){ tts.setLanguage(Locale.KOREAN); tts.setSpeechRate(0.82f); tts.setPitch(0.86f);} });}
    void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,28,28,28);root.setBackgroundColor(0xff101820);
        TextView title=new TextView(this); title.setText("SM3 Audio Suite V1.3\n목포 TTS + 티맵 안내 + 폴더 재생");title.setTextSize(24);title.setTextColor(0xffffffff);root.addView(title);
        status=new TextView(this); status.setText("티맵은 묵음 처리 후 접근성 권한을 켜세요.");status.setTextSize(16);status.setTextColor(0xffcfd8dc);status.setPadding(0,20,0,20);root.addView(status);
        Button acc=btn("접근성 설정 열기");acc.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(acc);
        Button folder=btn("음악 파일 선택/추가");folder.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);});root.addView(folder);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);root.addView(row);
        Button prev=btn("◀ 이전");Button play=btn("▶/⏸");Button next=btn("다음 ▶");Button stop=btn("■ 정지"); row.addView(prev,new LinearLayout.LayoutParams(0,120,1));row.addView(play,new LinearLayout.LayoutParams(0,120,1));row.addView(next,new LinearLayout.LayoutParams(0,120,1));row.addView(stop,new LinearLayout.LayoutParams(0,120,1));
        prev.setOnClickListener(v->{if(tracks.size()>0){index=(index-1+tracks.size())%tracks.size();startTrack();}});
        next.setOnClickListener(v->{if(tracks.size()>0){index=(index+1)%tracks.size();startTrack();}});
        play.setOnClickListener(v->{ if(player!=null && player.isPlaying())player.pause(); else if(player!=null)player.start(); else if(tracks.size()>0)startTrack();});
        stop.setOnClickListener(v->{ if(player!=null){player.stop();player.release();player=null;}});
        Button test=btn("목포 안내 테스트"); test.setOnClickListener(v->TmapAccessibilityService.speakStatic(this,"아따아, 잠시 후 좌회전이랑께요오.")); root.addView(test);
        setContentView(root);}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextSize(18);b.setAllCaps(false);return b;}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data); if(r==PICK&&c==RESULT_OK&&data!=null){ if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)tracks.add(data.getClipData().getItemAt(i).getUri());} else if(data.getData()!=null)tracks.add(data.getData()); status.setText("음악 "+tracks.size()+"개 추가됨");}}
    static void startTrack(){try{ if(player!=null){player.release();} player=new MediaPlayer(); player.setDataSource(App.ctx,tracks.get(index)); player.prepare(); player.start(); player.setOnCompletionListener(mp->{index=(index+1)%tracks.size();startTrack();}); }catch(Exception e){}}
    @Override public void onDestroy(){super.onDestroy(); if(tts!=null)tts.shutdown();}
}
