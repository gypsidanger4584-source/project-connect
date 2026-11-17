# 연성 커넥트 (project-connect)

캠퍼스 생활용 Android 앱입니다. 게시판, 시간표, 캠퍼스 지도, 스터디방, 학식 랜덤 매칭, AI 채팅 화면으로 구성되어 있습니다. 앱 이름은 `연성 커넥트`(`strings.xml`의 `app_name`)입니다.

## 주요 기능

아래 내용은 소스 코드에서 확인되는 기능입니다.

| 기능 | 구현 내용 | 관련 클래스 |
|------|-----------|-------------|
| 로그인 / 회원가입 | 학번으로 이메일을 조회한 뒤 Firebase Auth(이메일·비밀번호)로 로그인. 저장된 계정으로 `BiometricPrompt` 생체 인증 로그인을 지원하고, 학번으로 비밀번호 재설정 메일을 보내는 계정 찾기 화면이 있음 | `LoginActivity`, `RegisterActivity`, `FindAccountActivity` |
| 게시판 | 글 작성·상세·수정, 댓글, 카테고리 설정, 검색. Firestore `posts` 컬렉션 사용 | `WriteActivity`, `PostDetailActivity`, `EditPostActivity`, `CategorySettingsActivity`, `SearchDialog` |
| 시간표 | Room DB에 강의를 저장하고, WorkManager `PeriodicWorkRequest`로 다음 수업 알림을 띄움 | `TimetableActivity`, `TimetableDatabase`, `ReminderWorker` |
| 메모 | 시간표 화면의 메모는 Room DB(`MemoEntity`, `LectureRepository`)에 저장. 만료일은 기준일로부터 N일 뒤 23:59:59.999로 계산. `MemoRepository`는 Firestore `memos` 컬렉션을 쓰는 별도 구현 | `MemoEntity`, `LectureRepository`, `MemoRepository`, `MemoExpireHelper` |
| 관리자 | 사용자 문서의 `role` 값이 `admin`이면 공지사항 게시판 글 작성, 공지 수정·삭제, 닉네임 옆 관리자 표시와 프로필 테두리가 적용됨. 설정 화면에서 관리자 코드를 입력해 전환 | `CategorySettingsActivity`, `WriteActivity`, `NoticeAdapter`, `MainActivity` |
| 캠퍼스 지도 | 지도 화면, 확대·축소 레이아웃, 건물 상세 화면 | `CampusMap`, `ZoomableFrameLayout`, `BuildingDetail` |
| 스터디방 | 방 생성, 방 채팅, 문제 등록과 풀이. Firestore `studyRooms/{roomId}` 아래에 `participants`, `messages`, `problems/solvedUsers`를 둠 | `StudyRoomCreateActivity`, `StudyChatActivity`, `StudyProblemActivity`, `StudyProblemSolveActivity` |
| 점수와 칭호 | 문제를 맞히면 점수를 올리고 5점마다 칭호가 바뀜 (칭호 50개) | `TitleManager`, `TitleChangeActivity` |
| 학식 랜덤 매칭 | Firestore 대기열(`waiting`)과 트랜잭션으로 짝을 맺고, 1분 타이머 안에 매칭되지 않으면 실패 화면으로 이동. 매칭 뒤 채팅방에서 퇴장·신고 메뉴 제공 | `RandomMatchManager`, `RandomMatchActivity`, `MatchFailActivity`, `ChatRoomActivity` |
| AI 채팅 | Retrofit으로 OpenAI API(`gpt-4o-mini`)를 호출 | `ChatActivity`, `RetrofitInstance`, `ChatGptApi` |

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 빌드 | Java 17, Gradle(Kotlin DSL), Android Gradle Plugin 8.13.1 |
| Android | minSdk 31, targetSdk / compileSdk 35 |
| 인증 / 서버 | Firebase Auth 22.3.0, Cloud Firestore 25.1.4, Realtime Database 20.3.0 |
| 로컬 저장 | Room 2.6.1 (KSP) |
| 백그라운드 작업 | WorkManager 2.9.0 |
| 네트워크 | Retrofit 2.11.0 (Gson converter), OkHttp logging-interceptor 4.12.0 |
| UI / 기타 | Material 1.12.0, ConstraintLayout 2.2.1, Glide 4.16.0, Biometric 1.1.0, Guava 32.1.3-android |

## 코드 구조

```
app/src/main/java/com/example/login_new/
  ├─ alogic/   Manifest에 등록된 대부분의 화면과 Firestore·Room 로직
  └─ login/    MyPage, EditPostActivity (alogic과 같은 이름의 클래스가 일부 중복되어 있음)
app/src/main/assets/profanity_filter.tflite   욕설 필터용 모델 파일 (필터 제거 후 파일만 남음)
release/       app-release.apk 등 릴리스 산출물
```

## 실행 방법

1. Android Studio에서 프로젝트를 열고 Gradle 동기화를 합니다.
2. 에뮬레이터 또는 기기(Android 12, API 31 이상)에서 `app`을 실행합니다.
3. AI 채팅을 쓰려면 프로젝트 루트의 `local.properties`(git에 올라가지 않음)에 `OPENAI_API_KEY=키값`을 추가하거나 환경 변수 `OPENAI_API_KEY`를 설정합니다. 값은 빌드 시 `BuildConfig`로 주입되며, 없으면 빈 문자열입니다.
4. 자동 로그인용 비밀번호는 `EncryptedSharedPreferences`(`SecureCredentialStore`)에 저장하고, 로그아웃 시 삭제합니다. 로그인 관련 SharedPreferences는 백업 대상에서 제외했습니다.

## Firebase 설정

콘솔에서 확인한 내용이며, 보안 규칙과 색인은 이 저장소에 파일로 들어 있지 않습니다.

- Firestore 보안 규칙: 게시글은 작성자 또는 `role == "admin"`만 수정·삭제, 랜덤 채팅방과 메시지는 방의 `members`에 포함된 사용자만 접근, 학번 확인용 `student_numbers`는 단건 조회만 허용, `users` 문서의 `role`은 본인이 바꿀 수 없음(관리자 지정은 콘솔에서만)
- Firestore 복합 색인 3개: `posts`(border, timestamp 내림차순), `studyRooms`(isOpen, createdAt 내림차순), `waiting`(status, createdAt)
- Authentication: 이메일/비밀번호, Google 로그인 사용 설정

## 개발 정보

| 항목 | 내용 |
|---|---|
| 개발 기간 | 2025.04 ~ 2025.11 (이력서 기준) |
| 개발 인원 | 4인 팀 (이력서 기준) |
| 본인 담당 | 시간표, 로그인, 학식 랜덤 매칭 채팅, 관리자 기능, 스터디 채팅방 아이디어(기획) |
| 실행 화면 캡처 | [작성 필요] |
| 회고 | [작성 필요] |

## 비속어 필터

`TensorFlow Lite` 모델로 채팅 비속어 필터를 구현했다가 문제가 생겨 제거했습니다. 현재는 `app/src/main/assets/profanity_filter.tflite` 파일만 남아 있고, `ChatRoomActivity`는 필터 없이 바로 전송합니다(`app/build.gradle.kts`에도 TFLite 의존성이 없음).

- 제거한 사유: [작성 필요]
