# T-MAP Voice Overlay

티맵 음성을 묵음으로 두고, 접근성 서비스로 화면 안내 문구를 감지해 백그라운드에서 TTS 안내를 재생하는 Android 앱입니다.

## GitHub Actions APK 빌드

1. 이 프로젝트 파일 전체를 GitHub 저장소 루트에 업로드합니다.
2. Actions 탭으로 이동합니다.
3. `Android APK Build`가 완료되면 Artifacts에서 `T-MAP-Voice-debug-apk`를 다운로드합니다.
4. 압축을 풀어 `app-debug.apk`를 설치합니다.

## 사용법

1. 앱 설치 후 실행
2. `접근성 설정 열기`
3. `SM3 Tmap Voice Overlay` 활성화
4. 티맵 자체 음성은 묵음 처리
5. 티맵 길안내 화면에서 테스트

## 주의

- 티맵 APK를 수정하지 않습니다.
- 실제 음성 오디오를 가로채지 않습니다.
- 접근성으로 보이는 안내 문구를 읽어 TTS로 재생합니다.
- Android Auto 화면에서는 티맵 UI 구조에 따라 감지율이 달라질 수 있습니다.
