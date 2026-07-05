package com.sm3.audiosuite;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.EnvironmentalReverb;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static ArrayList<Uri> tracks = new ArrayList<>();
    static ArrayList<String> names = new ArrayList<>();
    static int index = 0;
    static MediaPlayer player;
    static Equalizer eq;
    static EnvironmentalReverb reverb;
    static TextToSpeech tts;
    static SpectrumView spectrum;

    final int PICK_FILES = 77;
    final int PICK_FOLDER = 78;
    final int BG = Color.rgb(4, 6, 10);
    final int PANEL = Color.rgb(12, 15, 22);
    final int PANEL2 = Color.rgb(19, 23, 32);
    final int GOLD = Color.rgb(227, 184, 103);
    final int TEXT = Color.rgb(246, 242, 233);
    final int SUB = Color.rgb(154, 164, 174);

    LinearLayout root, content;
    TextView status, trackInfo, nowTab, voiceInfo;
    String selectedVoice = "목포 아재";
    boolean shuffle = false;
    int reverbAmount = 38;
    int reverbRoom = 28;
    int reverbDecay = 34;
    TextView reverbInfo;
    Random random = new Random();
    SeekBar[] eqBars = new SeekBar[32];

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        selectedVoice = getSharedPreferences("sm3", MODE_PRIVATE).getString("voice", "목포 아재");
        reverbAmount = getSharedPreferences("sm3", MODE_PRIVATE).getInt("reverbAmount", 38);
        reverbRoom = getSharedPreferences("sm3", MODE_PRIVATE).getInt("reverbRoom", 28);
        reverbDecay = getSharedPreferences("sm3", MODE_PRIVATE).getInt("reverbDecay", 34);
        initTts();
        requestAudioPermission();
        buildUi();
        showHome();
    }

    void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList<String> ps = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) ps.add(Manifest.permission.RECORD_AUDIO);
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) ps.add(Manifest.permission.READ_MEDIA_AUDIO);
            if (!ps.isEmpty()) requestPermissions(ps.toArray(new String[0]), 9);
        }
    }

    void initTts() {
        tts = new TextToSpeech(this, s -> { if (s == TextToSpeech.SUCCESS) { tts.setLanguage(Locale.KOREAN); applyVoiceTone(selectedVoice); } });
    }

    void applyVoiceTone(String voice) {
        if (tts == null) return;
        if (voice.contains("아주머니")) { tts.setSpeechRate(0.80f); tts.setPitch(1.12f); }
        else if (voice.contains("차분")) { tts.setSpeechRate(0.70f); tts.setPitch(0.88f); }
        else if (voice.contains("표준")) { tts.setSpeechRate(0.96f); tts.setPitch(1.00f); }
        else { tts.setSpeechRate(0.74f); tts.setPitch(0.78f); }
    }

    int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    GradientDrawable bg(int color, int stroke, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); if (stroke > 0) g.setStroke(dp(1), stroke == 2 ? GOLD : Color.rgb(45, 54, 66)); return g; }
    TextView tv(String s, int sp, int color, int style) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT, style); t.setIncludeFontPadding(true); return t; }
    Button btn(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(14); b.setTextColor(TEXT); b.setAllCaps(false); b.setBackground(bg(PANEL2, 1, 18)); b.setPadding(dp(6), 0, dp(6), 0); return b; }
    TextView navBtn(String s) { TextView t = tv(s, 13, SUB, Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(dp(6), dp(10), dp(6), dp(10)); t.setBackground(bg(PANEL, 1, 18)); return t; }
    void space(LinearLayout l, int h) { Space s = new Space(this); l.addView(s, new LinearLayout.LayoutParams(1, dp(h))); }

    void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12), dp(10), dp(12), dp(16));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(16), dp(14), dp(16), dp(14)); header.setBackground(bg(PANEL, 2, 24));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout title = new LinearLayout(this); title.setGravity(Gravity.CENTER_VERTICAL); header.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView logo = tv("SM3 AUDIO", 24, GOLD, Typeface.BOLD); title.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        nowTab = tv("HOME", 13, GOLD, Typeface.BOLD); nowTab.setGravity(Gravity.CENTER); nowTab.setBackground(bg(Color.rgb(18,23,31),1,18)); title.addView(nowTab, new LinearLayout.LayoutParams(dp(100), dp(40)));
        status = tv("티맵 묵음 + SM3 안내 대기", 13, SUB, Typeface.NORMAL); header.addView(status);
        trackInfo = tv("폴더를 선택하면 자동 재생됩니다", 18, TEXT, Typeface.BOLD); trackInfo.setPadding(0, dp(8),0,0); header.addView(trackInfo);
        spectrum = new SpectrumView(this); header.addView(spectrum, new LinearLayout.LayoutParams(-1, dp(130)));

        space(root, 10);
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(dp(4), dp(4), dp(4), dp(4)); tabs.setBackground(bg(Color.rgb(9,12,18),1,22)); root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        String[] tabNames = {"홈", "음악", "EQ", "리버브", "음성", "설정"};
        for (String tn : tabNames) { TextView nb = navBtn(tn); tabs.addView(nb, new LinearLayout.LayoutParams(0, dp(52), 1));
            if (tn.equals("홈")) nb.setOnClickListener(v -> showHome());
            if (tn.equals("음악")) nb.setOnClickListener(v -> showMusic());
            if (tn.equals("EQ")) nb.setOnClickListener(v -> showEq());
            if (tn.equals("리버브")) nb.setOnClickListener(v -> showReverb());
            if (tn.equals("음성")) nb.setOnClickListener(v -> showVoice());
            if (tn.equals("설정")) nb.setOnClickListener(v -> showSettings()); }
        space(root, 10);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        setContentView(scroll);
    }

    void clear(String tab) { content.removeAllViews(); nowTab.setText(tab); }
    LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(16),dp(16),dp(16)); c.setBackground(bg(PANEL,1,22)); content.addView(c,new LinearLayout.LayoutParams(-1,-2)); return c; }

    void showHome() { clear("HOME"); LinearLayout c = card(); c.addView(tv("프리미엄 드라이브 허브",22,GOLD,Typeface.BOLD)); c.addView(tv("폴더 자동재생 · 32밴드 EQ · 리버브 · 스펙트럼 · 티맵 안내 보강",14,SUB,Typeface.NORMAL)); space(c,12); Button f=btn("📁 폴더 선택 후 자동 재생"); f.setOnClickListener(v->pickFolder()); c.addView(f,new LinearLayout.LayoutParams(-1,dp(62))); space(c,8); Button test=btn("🎙 티맵 안내 테스트"); test.setOnClickListener(v->speakTest()); c.addView(test,new LinearLayout.LayoutParams(-1,dp(62))); space(c,10); addTransport(c); }

    void showMusic() { clear("MUSIC"); LinearLayout c=card(); c.addView(tv("폴더 음악 재생",22,GOLD,Typeface.BOLD)); c.addView(tv("폴더 선택 시 내부 음악을 불러오고 첫 곡부터 자동 재생합니다.",14,SUB,Typeface.NORMAL)); space(c,12); Button folder=btn("📁 폴더 선택 / 자동 재생"); folder.setOnClickListener(v->pickFolder()); c.addView(folder,new LinearLayout.LayoutParams(-1,dp(62))); space(c,8); Button files=btn("🎵 음악 파일 여러 개 추가"); files.setOnClickListener(v->pickAudioFiles()); c.addView(files,new LinearLayout.LayoutParams(-1,dp(62))); space(c,10); addTransport(c); space(c,10); Button sh=btn(shuffle?"🔀 셔플 ON":"🔀 셔플 OFF"); sh.setOnClickListener(v->{ shuffle=!shuffle; showMusic(); }); c.addView(sh,new LinearLayout.LayoutParams(-1,dp(58))); space(c,8); c.addView(tv("현재 곡 수: "+tracks.size()+"개",16,TEXT,Typeface.BOLD)); }

    void addTransport(LinearLayout parent) { LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); parent.addView(row,new LinearLayout.LayoutParams(-1,dp(78))); Button prev=btn("⏮\n이전"), play=btn("▶/Ⅱ\n재생"), next=btn("⏭\n다음"), stop=btn("■\n정지"); row.addView(prev,new LinearLayout.LayoutParams(0,-1,1)); row.addView(play,new LinearLayout.LayoutParams(0,-1,1)); row.addView(next,new LinearLayout.LayoutParams(0,-1,1)); row.addView(stop,new LinearLayout.LayoutParams(0,-1,1)); prev.setOnClickListener(v->previousTrack()); play.setOnClickListener(v->togglePlay()); next.setOnClickListener(v->nextTrack()); stop.setOnClickListener(v->stopTrack()); }

    void showEq() { clear("EQ"); LinearLayout c=card(); c.addView(tv("32밴드 이퀄라이저",22,GOLD,Typeface.BOLD)); c.addView(tv("Android Equalizer에 32밴드 UI를 매핑합니다. 재생 중 조절하세요.",13,SUB,Typeface.NORMAL)); space(c,8); String[] labels={"31","40","50","63","80","100","125","160","200","250","315","400","500","630","800","1k","1.25k","1.6k","2k","2.5k","3.15k","4k","5k","6.3k","8k","10k","12.5k","16k","18k","20k","22k","24k"}; for(int i=0;i<32;i++){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); TextView l=tv(labels[i],12,SUB,Typeface.BOLD); r.addView(l,new LinearLayout.LayoutParams(dp(48),-2)); SeekBar sb=new SeekBar(this); sb.setMax(200); sb.setProgress(100); final int band=i; sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean f){ if(f) applyEqBand(band,p-100); } public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} }); eqBars[i]=sb; r.addView(sb,new LinearLayout.LayoutParams(0,-2,1)); c.addView(r,new LinearLayout.LayoutParams(-1,dp(34))); } }


    void showReverb() { clear("REVERB"); LinearLayout c=card();
        c.addView(tv("리버브",22,GOLD,Typeface.BOLD));
        c.addView(tv("기본값은 SM3 차량용 추천값입니다. 너무 올리면 보컬이 흐려질 수 있습니다.",13,SUB,Typeface.NORMAL));
        space(c,8);
        reverbInfo = tv(reverbText(),15,TEXT,Typeface.BOLD); c.addView(reverbInfo);
        space(c,12);
        addReverbBar(c, "공간감", "reverbAmount", reverbAmount);
        addReverbBar(c, "룸 크기", "reverbRoom", reverbRoom);
        addReverbBar(c, "잔향 길이", "reverbDecay", reverbDecay);
        space(c,10);
        Button rec=btn("추천값으로 복원"); rec.setOnClickListener(v->{ reverbAmount=38; reverbRoom=28; reverbDecay=34; saveReverb(); applyReverbSettings(); showReverb(); }); c.addView(rec,new LinearLayout.LayoutParams(-1,dp(60)));
        space(c,8);
        Button off=btn("리버브 OFF"); off.setOnClickListener(v->{ reverbAmount=0; saveReverb(); applyReverbSettings(); showReverb(); }); c.addView(off,new LinearLayout.LayoutParams(-1,dp(60)));
    }

    void addReverbBar(LinearLayout c, String label, String key, int value) {
        TextView l=tv(label+"  "+value+"%",14,SUB,Typeface.BOLD); c.addView(l);
        SeekBar sb=new SeekBar(this); sb.setMax(100); sb.setProgress(value);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean f){ if(!f) return; if(key.equals("reverbAmount")) reverbAmount=p; else if(key.equals("reverbRoom")) reverbRoom=p; else reverbDecay=p; l.setText(label+"  "+p+"%"); saveReverb(); applyReverbSettings(); if(reverbInfo!=null) reverbInfo.setText(reverbText()); }
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        c.addView(sb,new LinearLayout.LayoutParams(-1,dp(44)));
    }

    String reverbText(){ return "공간감 "+reverbAmount+"% · 룸 "+reverbRoom+"% · 잔향 "+reverbDecay+"%"; }
    void saveReverb(){ getSharedPreferences("sm3",MODE_PRIVATE).edit().putInt("reverbAmount",reverbAmount).putInt("reverbRoom",reverbRoom).putInt("reverbDecay",reverbDecay).commit(); }

    void showVoice(){ clear("VOICE"); LinearLayout c=card(); c.addView(tv("티맵 안내 음성",22,GOLD,Typeface.BOLD)); voiceInfo=tv("현재: "+selectedVoice,15,SUB,Typeface.NORMAL); c.addView(voiceInfo); space(c,12); String[] voices={"목포 아재","목포 아주머니","목포 차분한","표준어 기본"}; for(String name:voices){ Button b=btn((name.equals(selectedVoice)?"● ":"○ ")+name); b.setOnClickListener(v->selectVoice(name)); c.addView(b,new LinearLayout.LayoutParams(-1,dp(58))); space(c,8);} Button test=btn("선택한 음성 테스트"); test.setOnClickListener(v->speakTest()); c.addView(test,new LinearLayout.LayoutParams(-1,dp(62))); }

    void showSettings(){ clear("SETTINGS"); LinearLayout c=card(); c.addView(tv("설정",22,GOLD,Typeface.BOLD)); c.addView(tv("티맵 앱에서는 안내 음성을 묵음으로 두고, 이 앱의 접근성 서비스를 켜야 안내 감지가 됩니다.",14,SUB,Typeface.NORMAL)); space(c,12); Button acc=btn("♿ 접근성 설정 열기"); acc.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); c.addView(acc,new LinearLayout.LayoutParams(-1,dp(62))); space(c,8); Button perm=btn("🎧 오디오 권한 다시 요청"); perm.setOnClickListener(v->requestAudioPermission()); c.addView(perm,new LinearLayout.LayoutParams(-1,dp(62))); }

    void selectVoice(String name){ selectedVoice=name; getSharedPreferences("sm3",MODE_PRIVATE).edit().putString("voice",name).commit(); applyVoiceTone(name); TmapAccessibilityService.setVoiceMode(this,name); status.setText("음성 선택: "+name); speakTest(); showVoice(); }
    String testSentence(){ if(selectedVoice.contains("표준")) return "잠시 후 좌회전입니다."; if(selectedVoice.contains("차분")) return "잠시 후, 좌회전이랑께요."; if(selectedVoice.contains("아주머니")) return "오메에, 쪼까 있다가, 왼쪽으로 돌으랑께요오."; return "아따아, 쪼까 있다가, 좌회전이랑께요오."; }
    void speakTest(){ TmapAccessibilityService.speakStatic(this,testSentence()); }

    void pickAudioFiles(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("audio/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_FILES); }
    void pickFolder(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); startActivityForResult(i,PICK_FOLDER); }

    @Override protected void onActivityResult(int r,int c,Intent data){ super.onActivityResult(r,c,data); if(c!=RESULT_OK||data==null)return; if(r==PICK_FILES){ addFiles(data); updateTrack(); if(!tracks.isEmpty()){ index=tracks.size()==1?0:index; startTrack(); }} if(r==PICK_FOLDER){ Uri tree=data.getData(); if(tree!=null){ try{ getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){} tracks.clear(); names.clear(); loadTree(tree); index=0; updateTrack(); if(!tracks.isEmpty()) startTrack(); else Toast.makeText(this,"음악 파일을 찾지 못했습니다",Toast.LENGTH_LONG).show(); } } }
    void addFiles(Intent data){ if(data.getClipData()!=null){ for(int i=0;i<data.getClipData().getItemCount();i++) addTrack(data.getClipData().getItemAt(i).getUri()); } else if(data.getData()!=null) addTrack(data.getData()); }
    void addTrack(Uri u){ tracks.add(u); names.add(displayName(u)); }
    String displayName(Uri u){ String name="음악"; try(Cursor c=getContentResolver().query(u,null,null,null,null)){ if(c!=null&&c.moveToFirst()){ int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(idx>=0) name=c.getString(idx); }}catch(Exception ignored){} return name==null?"음악":name; }

    void loadTree(Uri treeUri){ try{ String docId=DocumentsContract.getTreeDocumentId(treeUri); Uri root=DocumentsContract.buildDocumentUriUsingTree(treeUri,docId); walkDocument(root); sortTracks(); }catch(Exception e){ Toast.makeText(this,"폴더 읽기 실패",Toast.LENGTH_LONG).show(); } }
    void walkDocument(Uri docUri){ try(Cursor c=getContentResolver().query(docUri,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){ if(c==null||!c.moveToFirst()) return; String id=c.getString(0), name=c.getString(1), mime=c.getString(2); if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){ Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(docUri,id); try(Cursor cc=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){ if(cc!=null) while(cc.moveToNext()) walkDocument(DocumentsContract.buildDocumentUriUsingTree(docUri,cc.getString(0))); } } else if(mime!=null && mime.startsWith("audio/")){ Uri u=DocumentsContract.buildDocumentUriUsingTree(docUri,id); tracks.add(u); names.add(name==null?"음악":name); } }catch(Exception ignored){} }
    void sortTracks(){ ArrayList<Integer> order=new ArrayList<>(); for(int i=0;i<tracks.size();i++)order.add(i); Collections.sort(order,(a,b)->names.get(a).compareToIgnoreCase(names.get(b))); ArrayList<Uri> nt=new ArrayList<>(); ArrayList<String> nn=new ArrayList<>(); for(int i:order){nt.add(tracks.get(i));nn.add(names.get(i));} tracks=nt; names=nn; }

    void previousTrack(){ if(tracks.size()>0){ index=(index-1+tracks.size())%tracks.size(); startTrack(); }}
    void nextTrack(){ if(tracks.size()>0){ index=shuffle?random.nextInt(tracks.size()):(index+1)%tracks.size(); startTrack(); }}
    void togglePlay(){ if(player!=null&&player.isPlaying()){ player.pause(); status.setText("일시정지"); } else if(player!=null){ player.start(); status.setText("재생 재개"); } else if(tracks.size()>0) startTrack(); }
    void stopTrack(){ if(player!=null){ player.stop(); player.release(); player=null; releaseEq(); releaseReverb(); status.setText("음악 정지"); } }
    void updateTrack(){ trackInfo.setText(tracks.size()>0?"곡 "+tracks.size()+"개 준비됨":"폴더를 선택하면 자동 재생됩니다"); status.setText("음악 "+tracks.size()+"개 불러옴"); }

    static void startTrack(){ try{ if(player!=null) player.release(); player=new MediaPlayer(); player.setDataSource(App.ctx,tracks.get(index)); player.prepare(); player.start(); if(spectrum!=null) spectrum.attachToSession(player.getAudioSessionId()); MainActivity a=App.activity; if(a!=null){ a.setupEq(player.getAudioSessionId()); a.setupReverb(player); a.trackInfo.setText("재생 중  "+(index+1)+" / "+tracks.size()+"  "+(names.size()>index?names.get(index):"")); a.status.setText("자동 재생 중"); } player.setOnCompletionListener(mp->{ MainActivity aa=App.activity; if(aa!=null) aa.nextTrack(); }); }catch(Exception e){ if(App.activity!=null) Toast.makeText(App.activity,"재생 실패: "+e.getMessage(),Toast.LENGTH_SHORT).show(); } }

    void setupEq(int session){ releaseEq(); try{ eq=new Equalizer(0,session); eq.setEnabled(true); }catch(Exception e){ eq=null; } }
    void releaseEq(){ try{ if(eq!=null){ eq.setEnabled(false); eq.release(); }}catch(Exception ignored){} eq=null; }
    void setupReverb(MediaPlayer mp){ releaseReverb(); try{ if(reverbAmount<=0) return; reverb=new EnvironmentalReverb(0,0); reverb.setEnabled(true); mp.attachAuxEffect(reverb.getId()); mp.setAuxEffectSendLevel(Math.max(0.0f, Math.min(1.0f, reverbAmount/100.0f))); applyReverbSettings(); }catch(Exception e){ releaseReverb(); if(status!=null) status.setText("리버브 사용 불가: 기기 효과 미지원"); } }
    void applyReverbSettings(){ try{ if(player!=null) player.setAuxEffectSendLevel(Math.max(0.0f, Math.min(1.0f, reverbAmount/100.0f))); if(reverb==null) return; short room=(short)(-9000 + (reverbRoom*60)); short rev=(short)(-9000 + (reverbAmount*60)); int decay=550 + reverbDecay*42; reverb.setRoomLevel(room); reverb.setRoomHFLevel((short)(room-800)); reverb.setReverbLevel(rev); reverb.setReverbDelay(35); reverb.setDecayTime(decay); reverb.setDecayHFRatio((short)650); reverb.setReflectionsLevel((short)(rev-1200)); reverb.setReflectionsDelay(18); reverb.setDensity((short)(550 + reverbRoom*4)); reverb.setDiffusion((short)(620 + reverbAmount*3)); }catch(Exception ignored){} }
    void releaseReverb(){ try{ if(reverb!=null){ reverb.setEnabled(false); reverb.release(); }}catch(Exception ignored){} reverb=null; }
    void applyEqBand(int uiBand,int value){ if(eq==null) return; try{ short bands=eq.getNumberOfBands(); short b=(short)Math.max(0,Math.min(bands-1,(int)Math.floor(uiBand*bands/32.0))); short[] range=eq.getBandLevelRange(); short level=(short)(value/100.0* (value>=0?range[1]:-range[0])); eq.setBandLevel(b,level); }catch(Exception ignored){} }

    @Override public void onDestroy(){ super.onDestroy(); App.activity=null; if(tts!=null)tts.shutdown(); if(spectrum!=null)spectrum.releaseVisualizer(); releaseEq(); releaseReverb(); }
    @Override protected void onResume(){ super.onResume(); App.activity=this; }
}
