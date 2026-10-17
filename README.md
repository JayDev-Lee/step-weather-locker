# Step Weather Locker · 한걸음

삼성 헬스의 걸음 수와 현재 위치의 날씨를 홈 위젯과 별도 잠금화면에 표시하는 Android 앱입니다.

- 패키지: `com.jaydev.stepcounter`
- Kotlin / Jetpack Compose / Jetpack Glance
- minSdk 29, compileSdk / targetSdk 37
- 현재 버전: **1.1.5**

## 기능

- 오늘 걸음 수, 활동 칼로리, 이동 거리 표시
- 10,000보에서 가득 차는 원형 진행 표시와 목표에 제한되지 않는 걸음 수 숫자
- 4×4 / 5×5 홈 위젯, 크기 조절, 수동 새로고침
- WorkManager를 통한 15분 주기 건강 데이터 동기화
- 잠금화면 자동 표시 모드, 시계·날짜, 사용자 배경 사진
- 현재 조회 위치의 도시명, 기온, 날씨 아이콘과 낮·밤 구분
- 자동 표시 상태 진단과 알림에서 모드 종료

활동 칼로리와 이동 거리는 삼성 헬스의 `ACTIVITY_SUMMARY` 집계값으로, 걷기 이외 활동도 포함할 수 있습니다. 데이터나 해당 읽기 권한이 없으면 `—`로 표시합니다.

## 빌드

Android SDK Platform 37, JDK 17 이상의 빌드 환경이 필요합니다. Gradle Wrapper는 9.4.1, Android Gradle Plugin은 9.2.1이며, Gradle daemon/toolchain 설정은 `gradle/`을 참고하세요.

삼성의 공식 **Samsung Health Data SDK 1.1.0**을 직접 받아 다음 위치에 라이브러리를 배치합니다.

```text
app/libs/samsung-health-data-api-1.1.0.aar
```

SDK AAR은 저장소에 포함하지 않습니다. [삼성 SDK 안내](https://developer.samsung.com/health/data/overview.html)와 [로컬 라이브러리 안내](app/libs/README.md)를 참고하세요. SDK가 없으면 빌드가 실패합니다.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. Release 빌드에는 배포용 서명 설정이 별도로 필요합니다. APK, 서명 키, 로컬 SDK 경로 및 빌드 캐시는 Git에서 제외합니다.

## 삼성 헬스 연결

삼성 헬스 설치와 초기 설정을 마친 뒤 앱에서 **삼성 헬스 연결 · 읽기 권한**을 눌러 걸음 수와 활동 요약 읽기를 허용합니다. SDK가 지원하는 삼성 헬스 버전과 앱 등록·서명 조건을 충족해야 합니다.

개발 테스트는 [삼성 개발자 모드 문서](https://developer.samsung.com/health/data/guide/developer-mode.html)를 따르세요. 일반 사용자 배포에는 삼성의 파트너 등록 및 배포용 서명 등록이 필요합니다. 이 저장소에는 삼성 발급 액세스 코드나 배포용 키가 없습니다.

새로고침은 삼성 헬스에 저장된 데이터를 다시 읽습니다. 워치에서 삼성 헬스로의 동기화를 강제로 실행하지는 않습니다.

## 잠금화면 설정

1. 앱의 **잠금화면 모드 · 배경과 날씨**를 엽니다.
2. 자동 표시를 켜고 **다른 앱 위에 표시**를 허용합니다.
3. 앱으로 돌아와 자동 표시를 다시 켜고 알림을 허용합니다.
4. 필요하면 배경 사진과 현재 위치 날씨를 설정합니다.
5. 화면을 껐다 켜서 확인합니다. 문제가 있으면 **자동 표시 상태** 문구를 확인합니다.

기본 잠금화면의 위젯 목록과 별개로 `showWhenLocked` Activity를 사용하는 모드입니다. 지문·PIN 잠금을 해제하지 않으며, 화면을 깨우거나 계속 켜 두지 않습니다. 기기와 One UI 정책에 따라 자동 실행이 제한될 수 있습니다. 재부팅·강제 종료 후에는 앱에서 모드를 다시 켜주세요.

## 위치와 날씨

- 위치는 화면이 보이는 동안 Android `LocationManager`로 조회합니다. Android 12 이상에서 Fused Provider를 우선 사용하고, 사용할 수 없으면 Network Provider를 사용합니다.
- 위치 권한은 앱 사용 중에만 요청합니다. **정확한 위치**를 선택하면 도시 경계에서 다른 도시가 표시되는 현상을 줄일 수 있습니다. 대략적인 위치만 허용해도 동작합니다.
- 도시명은 반올림 전 좌표를 Android `Geocoder`로 주소 변환해 확인합니다. 위치·주소 서비스의 정확도를 보장하지는 않습니다.
- 날씨는 [Open-Meteo](https://open-meteo.com/)에서 조회하며, 해당 API에는 소수점 둘째 자리로 반올림한 좌표만 전달합니다. 데이터 출처: Open-Meteo, CC BY 4.0.
- 날씨 화면을 열 때 마지막 성공 조회 후 30분이 지났으면 갱신합니다. 수동 갱신도 가능합니다. 조회 실패 시 마지막 값과 기준 시각을 표시합니다.
- 앱에는 좌표나 상세 주소를 저장하지 않고 도시명과 날씨 결과만 보관합니다. 백그라운드 위치 권한은 요청하지 않습니다.

건강 데이터, 날씨 결과와 배경 사진은 앱 내부 저장소에 보관하며 Android 백업은 비활성화했습니다. 자체 서버나 분석 SDK는 사용하지 않습니다.

## 검증

1.1.5 기준 APK 빌드, 단위 테스트 20개, Android Lint 오류 0개를 확인했습니다. Android 16 에뮬레이터에서 권한 승격, 날씨·도시 조회 및 잠금화면 표시를 검증했습니다. Galaxy S24+ 사용 환경에서 잠금화면이 표시된다는 사용자 확인을 받았습니다. 다른 기기·OS 버전의 동작은 별도 확인이 필요합니다.
