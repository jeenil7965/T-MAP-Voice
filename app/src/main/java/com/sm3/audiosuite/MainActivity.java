package com.sm3.audiosuite;

import android.app.*;import android.os.*;import android.content.*;import android.net.*;import android.provider.Settings;import android.speech.tts.TextToSpeech;import android.media.MediaPlayer;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;import java.io.*;

public class MainActivity extends Activity{
    static ArrayList<Uri> tracks=new ArrayList<>(); static int index=0; static MediaPlayer player; static TextToSpeech tts;
    TextView status, trackInfo; final int PICK=77;
    final int BG=Color.rgb(8,18,23), CARD=Color.rgb(15,28,35), CARD2=Color.rgb(20,35,43), GOLD=Color.rgb(224,183,108), TEXT=Color.rgb(245,241,230), SUB=Color.rgb(176,184,184);
    @Override public void onCreate(Bundle b){super.onCreate(b); initTts(); buildUi();}
    void initTts(){tts=new TextToSpeech(this, s->{ if(s==TextToSpeech.SUCCESS){ tts.setLanguage(Locale.KOREAN); tts.setSpeechRate(0.80f); tts.setPitch(0.86f);} });}
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}    
    GradientDrawable bg(int color,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius)); if(stroke>0)g.setStroke(dp(1),GOLD); return g;}
    TextView tv(String s,int sp,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,style);t.setIncludeFontPadding(true);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(TEXT);b.setAllCaps(false);b.setBackground(bg(CARD2,1,14));b.setPadding(dp(8),0,dp(8),0);return b;}
    TextView chip(String s){TextView t=tv(s,14,TEXT,Typeface.NORMAL);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setBackground(bg(CARD2,1,18));return t;}
    void addSpace(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    void buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(14),dp(16),dp(16));scroll.addView(root);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);top.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=tv("SM3 Audio Suite V1.4",28,GOLD,Typeface.BOLD);left.addView(title);
        left.addView(tv("목포 TTS · 티맵 안내 · 폴더 재생",18,TEXT,Typeface.NORMAL));
        status=tv("티맵 묵음 + 접근성 권한을 켜면 안내만 감지합니다.",13,SUB,Typeface.NORMAL);status.setPadding(0,dp(8),0,0);left.addView(status);
        TextView nav=tv("↰ 144m\n좌회전\n\n↰ 209m  다음 좌회전",20,TEXT,Typeface.BOLD);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(18),dp(12),dp(18),dp(12));nav.setBackground(bg(Color.rgb(13,60,39),1,16));top.addView(nav,new LinearLayout.LayoutParams(dp(210),dp(150)));
        addSpace(root,14);
        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);tabs.setPadding(0,dp(8),0,dp(8));tabs.setBackground(bg(Color.rgb(12,24,30),1,28));root.addView(tabs,new LinearLayout.LayoutParams(-1,-2));
        tabs.addView(chip("홈"),new LinearLayout.LayoutParams(0,dp(58),1));tabs.addView(chip("음악"),new LinearLayout.LayoutParams(0,dp(58),1));tabs.addView(chip("음성"),new LinearLayout.LayoutParams(0,dp(58),1));tabs.addView(chip("설정"),new LinearLayout.LayoutParams(0,dp(58),1));
        addSpace(root,12);
        LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.HORIZONTAL);root.addView(main,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout cardL=new LinearLayout(this);cardL.setOrientation(LinearLayout.VERTICAL);cardL.setPadding(dp(14),dp(14),dp(14),dp(14));cardL.setBackground(bg(CARD,1,18));main.addView(cardL,new LinearLayout.LayoutParams(0,-2,1));
        Button acc=btn("♿  접근성 설정 열기   ›");acc.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));cardL.addView(acc,new LinearLayout.LayoutParams(-1,dp(62)));
        addSpace(cardL,8);
        Button folder=btn("📁  음악 파일 선택 / 추가   ›");folder.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);});cardL.addView(folder,new LinearLayout.LayoutParams(-1,dp(62)));
        addSpace(cardL,10);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);cardL.addView(row);
        Button prev=btn("⏮\n이전");Button play=btn("▶/Ⅱ\n재생");Button next=btn("⏭\n다음");Button stop=btn("■\n정지");row.addView(prev,new LinearLayout.LayoutParams(0,dp(76),1));row.addView(play,new LinearLayout.LayoutParams(0,dp(76),1));row.addView(next,new LinearLayout.LayoutParams(0,dp(76),1));row.addView(stop,new LinearLayout.LayoutParams(0,dp(76),1));
        prev.setOnClickListener(v->{if(tracks.size()>0){index=(index-1+tracks.size())%tracks.size();startTrack();updateTrack();}});
        next.setOnClickListener(v->{if(tracks.size()>0){index=(index+1)%tracks.size();startTrack();updateTrack();}});
        play.setOnClickListener(v->{ if(player!=null && player.isPlaying())player.pause(); else if(player!=null)player.start(); else if(tracks.size()>0){startTrack();updateTrack();}});
        stop.setOnClickListener(v->{ if(player!=null){player.stop();player.release();player=null;} });
        addSpace(cardL,10);
        Button test=btn("🎙  목포 안내 테스트");test.setOnClickListener(v->TmapAccessibilityService.speakStatic(this,"아따아, 잠시 후 좌회전이랑께요오."));cardL.addView(test,new LinearLayout.LayoutParams(-1,dp(62)));
        Space gap=new Space(this);main.addView(gap,new LinearLayout.LayoutParams(dp(12),1));
        LinearLayout cardR=new LinearLayout(this);cardR.setOrientation(LinearLayout.VERTICAL);cardR.setPadding(dp(14),dp(14),dp(14),dp(14));cardR.setBackground(bg(CARD,1,18));main.addView(cardR,new LinearLayout.LayoutParams(0,-2,1));
        cardR.addView(tv("음성 선택 (TTS)",18,TEXT,Typeface.BOLD));addSpace(cardR,8);
        String[] voices={"목포 아재  추천","목포 아주머니","목포 차분한","표준어 기본"};
        for(String v:voices){TextView c=chip("◉  "+v+"\n   안내 억양을 부드럽게 적용");c.setGravity(Gravity.CENTER_VERTICAL);cardR.addView(c,new LinearLayout.LayoutParams(-1,dp(66)));addSpace(cardR,8);}        
        addSpace(root,14);
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(16),dp(12),dp(16),dp(12));bottom.setBackground(bg(Color.rgb(11,24,31),1,18));root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(116)));
        TextView album=tv("♪",42,GOLD,Typeface.BOLD);album.setGravity(Gravity.CENTER);album.setBackground(bg(Color.rgb(19,38,48),1,16));bottom.addView(album,new LinearLayout.LayoutParams(dp(84),dp(84)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(16),0,0,0);bottom.addView(info,new LinearLayout.LayoutParams(0,-1,1));
        trackInfo=tv("음악 대기 중",20,TEXT,Typeface.BOLD);info.addView(trackInfo);info.addView(tv("SM3 MUSIC · 폴더 재생",13,SUB,Typeface.NORMAL));
        TextView eq=tv("▁▂▃▄▅▆▇▅▄▃▂▁   Spectrum Ready",18,GOLD,Typeface.NORMAL);eq.setGravity(Gravity.CENTER);bottom.addView(eq,new LinearLayout.LayoutParams(dp(280),-1));
        setContentView(scroll);
    }
    void updateTrack(){trackInfo.setText("재생 중  " + (index+1) + " / " + tracks.size()); status.setText("음악 "+tracks.size()+"개 추가됨");}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data); if(r==PICK&&c==RESULT_OK&&data!=null){ if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)tracks.add(data.getClipData().getItemAt(i).getUri());} else if(data.getData()!=null)tracks.add(data.getData()); updateTrack();}}
    static void startTrack(){try{ if(player!=null){player.release();} player=new MediaPlayer(); player.setDataSource(App.ctx,tracks.get(index)); player.prepare(); player.start(); player.setOnCompletionListener(mp->{index=(index+1)%tracks.size();startTrack();}); }catch(Exception e){} }
    @Override public void onDestroy(){super.onDestroy(); if(tts!=null)tts.shutdown();}
}
