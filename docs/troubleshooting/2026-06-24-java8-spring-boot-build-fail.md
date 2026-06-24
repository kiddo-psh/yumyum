# Java 8 시스템 환경에서 Spring Boot 3.5 빌드 실패
**Date:** 2026-06-24
**Time spent:** ~30분

## Symptom

`./gradlew build` 실행 시 아래 오류:

```
error: source release 21 requires target release 21
```

또는:

```
Unsupported class file major version 65
```

Spring Boot 3.5 / Java 21 타깃인데 시스템 `java -version`이 1.8.0을 가리켜 컴파일·실행 모두 실패.

## Wrong tracks

- `gradle.properties`에 `org.gradle.jvm.version=21` 추가 → Gradle이 여전히 시스템 Java 8 사용
- `org.gradle.java.installations.auto-detect=true` 추가 → 탐지되는 JDK가 없어 무효

## Root cause

Windows 시스템 `PATH`의 `java`가 Java 8 (JRE)을 가리킨다. Spring Boot 3.5는 Java 17 이상 필수.
개발 환경에 Java 21이 설치되어 있지 않아 보이지만, VS Code Java 확장이 자체 JDK 21을 번들로
포함하고 있다.

## Fix

빌드·테스트 명령에 `JAVA_HOME`을 VS Code 번들 JDK 21로 명시적으로 지정:

```bash
JAVA_HOME="C:/Users/SSAFY/.vscode/extensions/redhat.java-1.54.0-win32-x64/jre/21.0.10-win32-x86_64" \
  ./gradlew build -x test
```

테스트 단독 실행:

```bash
JAVA_HOME="C:/Users/SSAFY/.vscode/extensions/redhat.java-1.54.0-win32-x64/jre/21.0.10-win32-x86_64" \
  ./gradlew test --tests "com.ssafy.manager.home.application.HomeCommentServiceTest"
```

## Prevention

- Spring Boot 서브프로젝트 작업 전 `java -version`으로 버전 확인
- 버전이 17 미만이면 위 `JAVA_HOME` 경로로 prefix 필수
- 장기적으로 시스템 `PATH`에 Java 21을 등록하거나 `JAVA_HOME`을 `.env`에 고정하면 매번 지정 불필요
- VS Code 확장 업데이트 시 `redhat.java-1.54.0-win32-x64` 경로의 버전 번호가 바뀔 수 있으므로
  경로 변경 여부 확인
