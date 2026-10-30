# Shared Advancements

Fabric 멀티플레이 도전과제 **달성 기록 공유** 모드입니다. 다른 플레이어의 도전과제를 강제로 해금하지 않습니다.

**개발 알파**입니다. 버전별 빌드 성공은 전체 게임플레이 호환성 보장이 아닙니다. 정확한 검증 범위는 [검증 기록](docs/VERIFICATION.md)과 [버전 표](docs/versions.json)를 확인하세요. Modrinth 배포는 보류되어 있습니다.

## 기능

- UUID·도전과제 ID·최초 달성 시간 기록, 중복 방지
- 서버 저장·백업, 오프라인 플레이어 보존, 재접속 동기화
- 기존 도전과제에 마우스를 올리면 플레이어 얼굴·달성 여부·시간·달성 인원 표시
- 기존 도전과제 화면의 **Shared** 버튼에서 순위·진척도·타임라인 조회
- **Players / Timeline**: 순위와 전체 타임라인 전환
- **Player >**: 플레이어별 달성 이력 전환, **< / >**: 페이지 이동
- 타임라인의 별표: 최초 달성자
- 전체 서버 알림 및 설정 가능한 알림 형식

## 설치

1. 사용할 Minecraft 버전에 맞는 Fabric Loader와 **Fabric API**를 설치합니다.
2. 정확히 같은 버전의 `SharedAdvancements-<Minecraft>-0.1.0-alpha.1.jar`를 서버와 클라이언트의 `mods` 폴더에 넣습니다.
3. Minecraft 1.21.x는 Java 21, 26.x는 Java 25로 실행합니다.
4. 서버에 접속하고 `L`로 기존 도전과제 화면을 엽니다.

싱글플레이에서는 클라이언트에 설치한 모드가 통합 서버에서도 동작합니다. 전용 서버에는 클라이언트 UI 클래스가 로드되지 않습니다. 모드가 없는 클라이언트도 바닐라 접속과 채팅 알림은 가능하지만 공유 UI는 표시되지 않습니다. 다른 버전 JAR은 Fabric이 거부합니다.

## 설정과 데이터

첫 실행 시 `config/shared-advancements.json`이 생성됩니다. 변경 후 재시작하세요.

```json
{
  "notifications": true,
  "format": "[Shared Advancements] {player} — {advancement} ({achieved}/{players})"
}
```

저장 위치: `<월드>/shared-advancements/history.json`, 직전 저장 백업: `history.json.bak`.
복구할 때 서버를 종료하고 손상 원본을 따로 보관한 다음 `.bak`을 `history.json`으로 복사하세요. 알 수 없는 스키마나 손상 파일은 자동 초기화하지 않습니다.

분모는 모드 설치 후 서버에 접속한 전체 플레이어이며 오프라인 플레이어도 포함합니다. 플레이어 진척도는 현재 공개된 표시 도전과제 기준입니다. 레시피는 제외하고 숨겨진 도전과제는 누군가 달성한 후 목록에 포함합니다. 사라진 ID는 보관 이력으로 유지합니다. 취소·재달성은 최초 이력을 변경하지 않습니다.

기존 바닐라 달성 내역은 해당 플레이어가 접속할 때 가져옵니다. 모드 설치 이후 한 번도 접속하지 않은 오프라인 플레이어는 자동 수집하지 않습니다. 오프라인/스킨 조회 불가 플레이어는 기본 얼굴을 표시할 수 있습니다. 작은 화면의 hover 패널은 일부 플레이어만 표시하므로 전체 기록은 Shared 화면에서 확인하세요.

## 개발

JDK 25를 `JAVA_HOME`으로 설정합니다. Gradle Wrapper가 포함되어 있습니다.

```powershell
.\gradlew.bat '-Pmc=1.21.1' build
.\gradlew.bat '-Pmc=26.3' build
.\gradlew.bat -PcoreOnly :common:test
.\scripts\build-matrix.ps1
```

산출물: `versions/<버전>/build/libs/`. Gradle 선택 옵션이 없는 경우 1.21.1을 빌드합니다. 목록에 없는 버전은 명확한 오류로 중단합니다. IntelliJ에서 루트 Gradle 프로젝트를 열고 Gradle JVM을 Java 25로 설정하세요. 26.x Mixin 지원에는 IntelliJ 2025.3 이상이 필요합니다.

GitHub Actions는 12개 버전을 각각 빌드하고, 공통 테스트 및 해당 Java 버전으로 패키징된 JAR의 전용 서버 시작·저장·종료를 검사합니다. CI JAR은 알파 테스트 산출물입니다. 안정 릴리스는 [수동 체크리스트](docs/TESTING.md)를 완료한 버전만 게시해야 합니다.

구조와 공식 조사 근거: [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 라이선스

[MIT](LICENSE). Minecraft 및 Fabric 자체의 라이선스는 별도로 적용됩니다.
NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
