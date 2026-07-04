package com.sm3.tmapvoice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("SM3 Tmap Voice Overlay\n\n티맵 음성은 묵음으로 두고, 접근성 권한을 켜면 티맵 안내 문구를 감지해 이 앱이 TTS로 안내합니다.");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        Button btn = new Button(this);
        btn.setText("접근성 설정 열기");
        btn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(btn);

        TextView note = new TextView(this);
        note.setText("권한에서 'SM3 Tmap Voice Overlay'를 켜세요.\nOGG 파일은 assets/voice 폴더에 넣어 확장할 수 있습니다.");
        note.setTextSize(14);
        note.setPadding(0, 30, 0, 0);
        root.addView(note);

        setContentView(root);
    }
}
