# Shared Advancements

한 명이 도전과제를 달성하면 **모두에게 해금**하고, **실제 달성자**를 기록하는 Fabric 모드입니다.

개발 알파입니다. 12개 버전 빌드 성공이 모든 버전의 실제 플레이 호환성을 뜻하지는 않습니다. [검증 기록](docs/VERIFICATION.md)을 확인하세요. Modrinth 배포는 보류되어 있습니다.

## 사용법

- L로 기존 도전과제 화면을 엽니다. 공유된 도전과제는 내 화면에서도 완료됩니다.
- 도전과제 위에 마우스를 올리면 설명, 실제 달성자와 달성 시간이 표시됩니다.
- 오른쪽 위 플레이어 얼굴의 테두리 색은 그 플레이어의 기록 색입니다. 얼굴 위에 마우스를 올리면 이름이 표시됩니다.
- 왼쪽 위 **자세히 보기**를 누르면 도전과제 아이콘과 달성 상태를 카드로 볼 수 있습니다.
- 얼굴을 누르면 해당 플레이어가 직접 달성한 기록만 표시됩니다. **전체 플레이어**로 필터를 해제합니다.
- **시간순 기록**은 달성한 도전과제를 오래된 순서로 보여줍니다. 항목에 마우스를 올려 달성자를 확인합니다.
- 긴 목록은 마우스 휠로 스크롤합니다. 플레이어가 많으면 얼굴 영역 위에서 휠로 다른 플레이어를 찾습니다.

개인전/협동전 전환은 제거했습니다. 공유는 항상 활성화되며 기존 mode.txt는 더 이상 사용하지 않습니다. 오프라인 플레이어는 재접속할 때 따라잡습니다. 공유 해금은 개인 실적으로 세지 않습니다. 모든 플레이어가 이미 해금한 도전과제를 각자 다시 달성하는 방식은 아닙니다.

전체 진행률은 현재 도전과제 중 공유된 고유 도전과제 수입니다. 사람 수를 곱하거나 같은 도전과제를 중복 계산하지 않습니다. 플레이어별 목록은 실제 달성 기록만 집계합니다.

## 설치

1. 사용할 Minecraft 버전에 맞는 Fabric Loader와 Fabric API를 설치합니다.
2. SharedAdvancements-<Minecraft>-0.1.0-alpha.4.jar를 서버와 클라이언트의 mods 폴더에 넣습니다. 이전 모드 JAR은 함께 설치하지 않습니다.
3. Minecraft 1.21.x는 Java 21, 26.x는 Java 25로 실행합니다.

싱글플레이 통합 서버도 같은 공유 규칙을 사용합니다. 모드가 없는 클라이언트도 바닐라 해금과 채팅 알림은 받지만 추가 UI는 표시되지 않습니다. 정확히 다른 Minecraft 버전의 JAR은 Fabric이 거부합니다. alpha.3은 새로운 동기화 채널을 사용하므로 서버와 클라이언트를 함께 업데이트하세요.

## 기존 기록과 저장

월드의 shared-advancements/history.json에 기록을 저장하며, 직전 저장은 history.json.bak에 백업합니다. UUID로 플레이어를 구분하고 중복 기록을 막습니다. 사라진 도전과제 ID는 보관합니다.

이전 알파는 실제 달성과 공유 해금을 구분하지 않았습니다. schema 1 파일을 읽으면 원본을 history.schema-1.json으로 별도 보존하고 schema 2로 옮깁니다. 이전 기록은 공유 해금에 계속 사용하지만 달성자는 **이전 기록 · 달성자 확인 불가**로 표시합니다. 누구의 실적인지 임의로 추측하지 않습니다. 설치 전에 이미 완료한 바닐라 도전과제도 달성자가 검증되지 않은 이전 기록으로 가져옵니다.

서버가 실제 새 달성을 감지한 기록부터 달성자 이름을 표시합니다. 공유받은 플레이어는 새 달성자로 추가하지 않으며, 재접속·재시작 후에도 이 구분을 유지합니다. 바닐라 해금과 보상을 적용합니다. 취소/재달성으로 기록을 중복 생성하지 않습니다.

config/shared-advancements.json에서 notifications와 format을 설정할 수 있습니다. 변경 후 재시작하세요. 기본 알림은 달성자와 도전과제, 공유 사실을 표시합니다. 기존 설정은 유지합니다.

## 개발 및 검증

Gradle 빌드 JVM은 Java 25입니다. IntelliJ에서 루트 Gradle 프로젝트를 열면 됩니다.

~~~powershell
.\gradlew.bat '-Pmc=1.21.1' build
.\gradlew.bat '-Pmc=26.3' build
.\gradlew.bat -PcoreOnly :common:test
.\scripts\build-matrix.ps1
.\scripts\collect-artifacts.ps1
~~~

버전별 산출물은 versions/<버전>/build/libs/, 수집된 알파 JAR과 SHA-256 목록은 artifacts/0.1.0-alpha.4/에 있습니다. 목록에 없는 버전은 빌드를 중단합니다.

GitHub Actions는 버전별 빌드, 공통 테스트, 패키징된 JAR의 전용 서버 시작·저장·종료를 검사합니다. 안정 릴리스는 [수동 체크리스트](docs/TESTING.md)를 완료한 버전만 게시합니다. [아키텍처와 공식 조사 근거](docs/ARCHITECTURE.md), [버전별 상태](docs/versions.json).

## 라이선스

[MIT](LICENSE). Minecraft 및 Fabric 자체의 라이선스는 별도로 적용됩니다.
NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
