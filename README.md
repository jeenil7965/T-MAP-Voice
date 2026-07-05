# SM3 Audio Suite V1.2 Real Mokpo

실제 GitHub Actions 빌드용 Android 프로젝트입니다.

## 포함 기능
- 티맵 접근성 감지
- 화면 전체 읽기 방지용 키워드 필터
- 목포 사투리 느낌 TTS 문장 변환
- TTS 속도 0.82 / 피치 0.88 조정
- 폴더 음악 재생: MP3, M4A, OGG, WAV, FLAC
- 이전/다음/재생/일시정지/셔플
- 차량용 큰 버튼 UI
- 앱 아이콘
- GitHub Actions APK 자동 빌드

## 사용법
1. GitHub에 이 폴더 내용을 업로드합니다.
2. Actions → Android APK Build 완료 후 Artifacts에서 APK를 받습니다.
3. 기존 앱 삭제 후 새 APK를 설치합니다.
4. 앱 실행 → 접근성 설정 → SM3 Audio Suite 켜기
5. 티맵 길안내 음성은 묵음으로 설정합니다.

## 주의
TTS 억양은 안드로이드 기본 TTS 엔진의 한계가 있습니다. 실제 녹음 OGG를 넣으면 훨씬 자연스럽습니다.
