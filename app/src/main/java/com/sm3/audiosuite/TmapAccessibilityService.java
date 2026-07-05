package com.sm3.audiosuite;

import android.accessibilityservice.AccessibilityService;import android.view.accessibility.*;import android.speech.tts.TextToSpeech;import android.content.*;import java.util.*;

public class TmapAccessibilityService extends AccessibilityService{
    static TextToSpeech tts; static long lastTime=0; static String last="";
    @Override public void onCreate(){super.onCreate(); setupTts(this);} 
    static void setupTts(Context c){ if(tts==null)tts=new TextToSpeech(c.getApplicationContext(), s->{ if(s==TextToSpeech.SUCCESS){tts.setLanguage(Locale.KOREAN);tts.setSpeechRate(0.80f);tts.setPitch(0.84f);} });}
    public static void speakStatic(Context c,String msg){setupTts(c); if(tts!=null)tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"sm3-test");}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){ if(e==null)return; CharSequence pkg=e.getPackageName(); if(pkg==null || !pkg.toString().toLowerCase().contains("tmap"))return; AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return; ArrayList<String> texts=new ArrayList<>(); collect(root,texts); String guide=findGuide(texts); if(guide!=null) speakGuide(guide); }
    void collect(AccessibilityNodeInfo n,ArrayList<String> out){ if(n==null)return; CharSequence t=n.getText(); if(t!=null){String s=t.toString().trim(); if(s.length()>0 && s.length()<45)out.add(s);} CharSequence d=n.getContentDescription(); if(d!=null){String s=d.toString().trim(); if(s.length()>0 && s.length()<45)out.add(s);} for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),out); }
    String findGuide(ArrayList<String> list){ for(String s:list){ if(isGuide(s)) return s; } return null; }
    boolean isGuide(String s){ String x=s.replace(" ",""); if(x.length()<2||x.length()>35)return false; String[] keys={"좌회전","우회전","유턴","직진","단속","카메라","목적지","도착","고속도로","분기점","나들목","차로","방면"}; for(String k:keys) if(x.contains(k)) return true; return false; }
    void speakGuide(String s){ long now=System.currentTimeMillis(); if(s.equals(last)&&now-lastTime<6500)return; last=s; lastTime=now; String msg=toMokpo(s); if(tts!=null)tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"sm3-guide"); }
    String toMokpo(String s){ String x=s.replace("하세요","").replace("입니다","").trim(); if(x.contains("좌회전")) return "아따아, 잠시 후 좌회전이랑께요오."; if(x.contains("우회전")) return "오메에, 잠시 후 우회전이랑께요오."; if(x.contains("유턴")) return "아따아, 앞에서 유턴허면 되것소잉."; if(x.contains("단속")||x.contains("카메라")) return "아따아, 앞에 단속 있응께 속도 쪼까 줄이소잉."; if(x.contains("목적지")||x.contains("도착")) return "고생 많았소잉, 목적지에 도착했당께라."; if(x.contains("고속도로")) return "이제 고속도로로 올라간당께요오."; if(x.contains("차로")) return "차로 잘 맞춰서 천천히 가소잉."; return "아따아, "+x+" 이랑께요오."; }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){super.onDestroy(); if(tts!=null){tts.shutdown();tts=null;}}
}
