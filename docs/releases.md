# 태그로 서명 APK 배포

`v1.2.1` 같은 태그를 원격 저장소에 푸시하면 GitHub Actions가 릴리즈 APK를 빌드·서명하고 GitHub Release에 첨부합니다. AAB는 만들지 않습니다.

## 처음 한 번: 서명 키 등록

Python 3, JDK의 `keytool`, GitHub CLI `gh`가 필요합니다. `JayDev-Lee` 계정으로 `gh auth login`을 완료한 뒤 프로젝트 폴더의 대화형 터미널에서 실행합니다.

```sh
python3 scripts/configure-release-secrets.py ~/jaydev-keystore.jks
```

키스토어 비밀번호, 목록에 표시된 키 별칭(alias), 키 비밀번호를 입력합니다. 키 비밀번호가 키스토어 비밀번호와 같으면 Enter를 누릅니다. 비밀번호는 화면에 표시되지 않으며, 개인 키 사용을 확인한 뒤 다음 Repository Secrets를 등록합니다.

- `ANDROID_KEYSTORE_BASE64`: 키스토어 파일의 Base64 값
- `ANDROID_KEYSTORE_PASSWORD`: 키스토어 비밀번호
- `ANDROID_KEY_ALIAS`: 서명할 개인 키의 별칭
- `ANDROID_KEY_PASSWORD`: 개인 키 비밀번호

등록 확인: [저장소 Actions Secrets](https://github.com/JayDev-Lee/step-weather-locker/settings/secrets/actions). 값은 다시 읽을 수 없으므로 원본 키스토어와 암호를 별도로 보관하세요. 이 도구는 전역 `gh` 활성 계정을 변경하지 않습니다.

삼성 라이선스 SDK는 `.github/secure/samsung-health-data-api-1.1.0.aar.enc`에 AES-256-CBC / PBKDF2-SHA256(200,000회)로 암호화했습니다. 복호화용 `SAMSUNG_SDK_PASSPHRASE` Secret은 별도로 등록되어 있습니다. CI에서 복호화한 SDK는 고정 SHA-256으로 검증합니다. 원본 AAR과 키스토어는 Git에 올리지 않습니다. [GitHub의 대용량 Secret 안내](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets#storing-large-secrets)처럼 암호화된 파일만 보관합니다.

## 배포

배포할 코드를 커밋하고 원격 저장소에 올린 다음 태그를 푸시합니다.

```sh
git tag -a v1.2.1 -m "하루링 1.2.1"
git push origin v1.2.1
```

- 태그: 안정 버전 `vMAJOR.MINOR.PATCH`. minor와 patch는 각각 0–999입니다. 사전 출시 접미사는 지원하지 않습니다.
- `versionName`: 태그의 `v`를 제외한 문자열입니다.
- `versionCode`: `major × 1,000,000 + minor × 1,000 + patch`. 예를 들어 `v1.2.1`은 `1002001`입니다. Android 상한인 2,100,000,000 이내여야 합니다.
- 업데이트 설치가 가능하도록 배포 버전은 증가시킵니다. 기존 소스의 versionCode 9보다 첫 태그 빌드의 코드가 커집니다. 로컬 기본 버전은 `app/build.gradle.kts` 값을 사용합니다.

[Actions](https://github.com/JayDev-Lee/step-weather-locker/actions/workflows/release.yml)에서 진행 상태를 확인합니다. 서명, 단위 테스트, Lint, APK 서명 검증을 통과한 뒤 [Releases](https://github.com/JayDev-Lee/step-weather-locker/releases)에 다음 파일이 올라갑니다.

```text
haruring-1.2.1.apk
haruring-1.2.1.apk.sha256
```

이미 존재하는 Release의 바이너리는 덮어쓰지 않습니다. 실패 원인을 수정해 새 태그로 배포하거나, 아직 Release가 생성되지 않은 실패 실행을 다시 실행하세요. `v*` 태그를 푸시하는 사람이 릴리즈 코드를 결정하므로 태그 쓰기 권한은 신뢰할 수 있는 계정에만 부여합니다.

게시 없이 서명을 검증하려면 Actions의 **Run workflow**에서 버전을 입력합니다. APK는 해당 실행의 Artifact로 14일간 보관하고 GitHub Release는 만들지 않습니다.

## 설치와 삼성 헬스 등록

이 키와 다른 인증서로 서명된 기존 APK(예: debug APK)는 새 APK로 업데이트 설치할 수 없습니다. 앞으로는 같은 키스토어로 계속 서명해야 합니다. 삼성 헬스 파트너 등록에도 이번 릴리즈 서명 인증서의 SHA-256을 사용해야 합니다. CI의 **Verify signature and prepare APK** 단계에서 공개 인증서 지문을 확인할 수 있습니다.
