# SM3 Audio Suite V2.1 RealCore

이 버전은 **최종 완성판이 아니라 실제 동작 코어를 우선한 다음 단계 버전**입니다.

## 실제 구현 범위
- 폴더 선택 후 오디오 파일 탐색 및 자동 재생
- 여러 음악 파일 선택 재생
- 이전 / 다음 / 재생 / 일시정지 / 정지
- 셔플
- 32밴드 EQ UI와 Android Equalizer 매핑
- Android EnvironmentalReverb 기반 리버브 조절
- Visualizer 기반 스펙트럼 표시
- 티맵 접근성 감지 기반 안내 음성 보조
- 목포 스타일 / 표준어 음성 선택

## 아직 제한이 있는 부분
- 32밴드 EQ는 기기 Equalizer 밴드 수에 맞춰 매핑됩니다. 완전한 Poweramp급 DSP 엔진은 아닙니다.
- 리버브는 기기 내장 EnvironmentalReverb 지원 여부에 따라 다릅니다.
- 티맵 안내 감지는 티맵 화면/Android Auto 모드/기기 접근성 출력에 따라 실패할 수 있습니다.
- Poweramp급 완성 UI는 목표이며, 이 버전은 실사용 코어 점검용입니다.

## GitHub Actions
저장소에 업로드하면 Actions에서 debug APK를 생성합니다.
