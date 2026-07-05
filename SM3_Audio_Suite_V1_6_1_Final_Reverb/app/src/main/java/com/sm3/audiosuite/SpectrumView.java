package com.sm3.audiosuite;

import android.content.Context;
import android.graphics.*;
import android.media.audiofx.Visualizer;
import android.os.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;

public class SpectrumView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] bars = new float[32];
    private Visualizer visualizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean fallback = true;
    private long t = 0;

    public SpectrumView(Context c) { super(c); init(); }
    public SpectrumView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        barPaint.setColor(Color.rgb(227,184,103));
        gridPaint.setColor(Color.rgb(38,45,55));
        gridPaint.setStrokeWidth(1f);
        textPaint.setColor(Color.rgb(155,165,175));
        textPaint.setTextSize(24f);
        for (int i=0;i<bars.length;i++) bars[i]=0.15f;
        startFallback();
    }

    public void attachToSession(int sessionId) {
        releaseVisualizer();
        if (sessionId == 0) { fallback = true; return; }
        try {
            visualizer = new Visualizer(sessionId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int sr) { }
                @Override public void onFftDataCapture(Visualizer v, byte[] fft, int sr) {
                    fallback = false;
                    updateFromFft(fft);
                }
            }, Visualizer.getMaxCaptureRate()/2, false, true);
            visualizer.setEnabled(true);
        } catch (Throwable e) { fallback = true; }
    }

    private void updateFromFft(byte[] fft) {
        if (fft == null || fft.length < 4) return;
        Arrays.fill(bars, 0.03f);
        int n = fft.length / 2;
        for (int i=2;i<n;i++) {
            int re = fft[2*i];
            int im = fft[2*i+1];
            float mag = (float)Math.sqrt(re*re + im*im) / 128f;
            int b = (int)((Math.log(i+1) / Math.log(n+1)) * (bars.length-1));
            if (b >= 0 && b < bars.length) bars[b] = Math.max(bars[b], Math.min(1f, mag));
        }
        invalidate();
    }

    private void startFallback() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (fallback) {
                    t++;
                    for (int i=0;i<bars.length;i++) {
                        double v = Math.sin((t*0.13) + i*0.55) * 0.35 + Math.sin((t*0.07)+i*0.21)*0.18 + 0.45;
                        bars[i] = Math.max(0.06f, Math.min(1f, (float)v));
                    }
                    invalidate();
                }
                handler.postDelayed(this, 70);
            }
        }, 70);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(); int h = getHeight();
        c.drawColor(Color.rgb(9,12,18));
        for (int i=1;i<4;i++) c.drawLine(0, h*i/4f, w, h*i/4f, gridPaint);
        float gap = 4f;
        float bw = (w - gap*(bars.length+1)) / bars.length;
        for (int i=0;i<bars.length;i++) {
            float bh = Math.max(8, bars[i] * (h-36));
            float left = gap + i*(bw+gap);
            RectF r = new RectF(left, h-bh-10, left+bw, h-10);
            barPaint.setColor(i%8==0 ? Color.rgb(245,210,136) : Color.rgb(227,184,103));
            c.drawRoundRect(r, 7, 7, barPaint);
        }
        c.drawText(fallback ? "SPECTRUM 32  ·  SIM" : "SPECTRUM 32  ·  LIVE", 18, 30, textPaint);
    }

    public void releaseVisualizer() {
        try { if (visualizer != null) { visualizer.setEnabled(false); visualizer.release(); } } catch (Throwable ignored) {}
        visualizer = null;
    }
}
