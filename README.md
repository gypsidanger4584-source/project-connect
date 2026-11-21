# 연성 커넥트 (project-connect)

캠퍼스 생활용 Android 앱입니다. 게시판, 시간표, 캠퍼스 지도, 스터디방, 학식 랜덤 매칭, AI 채팅 화면으로 구성되어 있습니다. 앱 이름은 `연성 커넥트`(`strings.xml`의 `app_name`)입니다. 서버 없이 Firebase(Auth, Firestore)와 Room만 사용합니다.

## 주요 기능

아래 내용은 소스 코드에서 확인되는 기능입니다.

| 기능 | 구현 내용 | 관련 클래스 |
|------|-----------|-------------|
| 로그인 / 회원가입 | 학번으로 이메일을 조회한 뒤 Firebase Auth(이메일·비밀번호)로 로그인. 저장된 계정으로 `BiometricPrompt` 생체 인증 로그인을 지원하고, 학번으로 비밀번호 재설정 메일을 보내는 계정 찾기 화면이 있음 | `LoginActivity`, `RegisterActivity`, `FindAccountActivity` |
| 게시판 | 글 작성·상세·수정, 댓글, 좋아요, 카테고리 설정, 검색. Firestore `posts` 컬렉션 사용 | `WriteActivity`, `PostDetailActivity`, `EditPostActivity`, `CategorySettingsActivity`, `SearchDialog` |
| 시간표 | Room DB에 강의를 저장하고, 터치 이벤트를 직접 처리하는 커스텀 View로 탭·드래그로 강의를 등록. 강의 1개 직접 입력에 필요한 UI 클릭을 유사 앱(에브리타임) 7회에서 3회로 줄임(타자 제외). WorkManager `PeriodicWorkRequest`(15분 주기)가 15분 안에 시작하는 다음 수업을 알림. [입력 시연 영상](docs/timetable-touch-compare-7to3.mp4) | `TimetableActivity`, `TimetableView`, `TimetableDatabase`, `ReminderWorker` |
| 메모 | 시간표 화면의 메모는 Room DB(`MemoEntity`, `LectureRepository`)에 저장. 만료일은 기준일로부터 N일 뒤 23:59:59.999로 계산하고, D-Day 칩을 표시하며, 만료된 메모는 목록에서 제외함(Room 행 삭제는 하지 않음). `MemoRepository`는 Firestore `memos` 컬렉션을 쓰는 별도 구현 | `MemoEntity`, `LectureRepository`, `MemoRepository`, `MemoExpireHelper`, `MemoAdapter` |
| 관리자 | 사용자 문서의 `role` 값이 `admin`이면 공지사항 게시판 글 작성, 공지 수정·삭제, 닉네임 옆 관리자 표시와 프로필 테두리가 적용됨. `role`은 앱에서 바꿀 수 없고 Firebase 콘솔에서만 지정함 | `CategorySettingsActivity`, `WriteActivity`, `NoticeAdapter`, `MainActivity` |
| 캠퍼스 지도 | 지도 화면, 확대·축소 레이아웃, 건물 상세 화면 | `CampusMap`, `ZoomableFrameLayout`, `BuildingDetail` |
| 스터디방 | 방 생성, 방 채팅, 문제 등록과 풀이. Firestore `studyRooms/{roomId}` 아래에 `participants`, `messages`, `problems`를 둠 | `StudyRoomCreateActivity`, `StudyChatActivity`, `StudyProblemActivity`, `StudyProblemSolveActivity` |
| 점수와 칭호 | 문제를 맞히면 점수를 올리고 5점마다 칭호가 바뀜 (칭호 50개) | `TitleManager`, `TitleChangeActivity` |
| 학식 랜덤 매칭 | Firestore 대기열(`waiting`)과 트랜잭션으로 짝을 맺고, 1분 타이머 안에 매칭되지 않으면 실패 화면으로 이동. 매칭 뒤 채팅방에서 퇴장·신고 메뉴 제공 (설계는 아래 참고) | `RandomMatchManager`, `RandomMatchActivity`, `MatchFailActivity`, `ChatRoomActivity` |
| AI 채팅 | Retrofit으로 OpenAI API(`gpt-4o-mini`)를 호출 | `ChatActivity`, `RetrofitInstance`, `ChatGptApi` |

## 학식 랜덤 매칭 설계

서버 없이 Firestore만으로 1:1 매칭을 구현했습니다.

1. 매칭을 시작하면 `waiting`에서 `status == "waiting"`인 문서를 1개 조회합니다.
2. 대기자가 없으면 내 문서를 `waiting/{uid}`로 만들고, 내 문서의 `status`가 `matched`로 바뀌는지 스냅샷 리스너로 기다립니다.
3. 대기자가 있으면 **트랜잭션 안에서 상대 문서의 `status`를 다시 읽어** `waiting`일 때만 `rooms/{roomId}` 문서를 만들고 상대 문서를 `matched`로 바꿉니다.
4. 트랜잭션 안에서 상태를 다시 확인하므로, 두 사람이 같은 대기자를 동시에 잡아도 한 명만 성공하고 나머지는 트랜잭션 실패로 처리됩니다.
5. 1분 안에 매칭되지 않으면 내 문서를 `cancelled`로 바꾸고 실패 화면으로 이동합니다.

알려진 한계입니다. 아직 고치지 않았습니다.

- 대기자 조회와 트랜잭션이 분리되어 있어, 두 사용자가 동시에 대기열에 들어오면 서로를 보지 못하고 둘 다 대기할 수 있습니다.
- 앱이 강제 종료되면 `waiting` 문서가 `waiting` 상태로 남아, 이후 사용자가 이미 떠난 상대와 매칭될 수 있습니다. 서버 측 정리(TTL, Cloud Functions)가 없습니다.
- 1분 타임아웃은 클라이언트 타이머입니다.
- 내 UID와 대기자 UID가 같은지 비교하는 코드가 없습니다.

## Firestore 데이터 구조

코드에서 확인되는 컬렉션입니다.

```
users/{uid}                      학번, 학과(major), role 등 사용자 문서
posts/{postId}                   게시글 (likeCount, commentCount 보관)
  ├─ comments/{commentId}
  └─ likes/{uid}                 사용자별 좋아요 문서 (중복 좋아요 방지)
waiting/{uid}                    랜덤 매칭 대기열 (status: waiting | matched | cancelled)
rooms/{roomId}                   매칭된 채팅방 (userA, userB, members[])
  └─ messages/{autoId}
studyRooms/{roomId}              스터디방 (currentPeople, problemCount)
  ├─ participants/{uid}          점수, 칭호
  ├─ messages/{autoId}
  └─ problems/{problemId}
       └─ solves/{uid}           풀이 기록
buildings, departments_detail    지도 건물 정보, 학과 정보
restrictions                     userId로 조회하는 제한 정보 (CategorySettingsActivity)
```

- 좋아요는 트랜잭션 안에서 `likes/{uid}` 존재 여부를 확인한 뒤 `likeCount`를 바꿉니다. 같은 사용자가 두 번 눌러도 카운트가 두 번 오르지 않습니다.
- 스터디 문제 등록은 `WriteBatch`로 문제 문서 생성과 `problemCount` 증가를 함께 처리합니다. 채점은 트랜잭션으로 풀이 기록과 점수를 함께 반영합니다.
- 학번 중복 확인은 `users`에서 `studentNumber`를 조회하는 방식이며, Firestore에는 유니크 제약이 없어 조회와 가입 사이의 경합을 막지 못합니다.

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
  └─ login/    MyPage, EditPostActivity 등 (alogic과 같은 이름의 클래스가 일부 중복되어 있음)
```

`alogic`과 `login` 패키지에 같은 이름의 클래스가 있고 일부는 서로 참조하고 있어, 중복 제거는 빌드 검증과 함께 따로 진행할 예정입니다.

## 실행 방법

1. Android Studio에서 프로젝트를 열고 Gradle 동기화를 합니다.
2. Firebase 콘솔에서 앱의 `google-services.json`을 내려받아 `app/` 폴더에 넣습니다. 이 파일은 저장소에 포함하지 않습니다(`.gitignore`).
3. 에뮬레이터 또는 기기(Android 12, API 31 이상)에서 `app`을 실행합니다.
4. AI 채팅을 쓰려면 프로젝트 루트의 `local.properties`(git에 올라가지 않음)에 `OPENAI_API_KEY=키값`을 추가하거나 환경 변수 `OPENAI_API_KEY`를 설정합니다. 값은 빌드 시 `BuildConfig`로 주입되며, 없으면 빈 문자열입니다.
5. 자동 로그인용 비밀번호는 `EncryptedSharedPreferences`(`SecureCredentialStore`)에 저장하고, 로그아웃 시 삭제합니다. 로그인 관련 SharedPreferences는 백업 대상에서 제외했습니다.

APK 같은 빌드 산출물은 저장소에 포함하지 않습니다.

## Firebase 설정

콘솔에서 확인한 내용이며, 보안 규칙과 색인은 이 저장소에 파일로 들어 있지 않습니다.

- Firestore 보안 규칙: 게시글은 작성자 또는 `role == "admin"`만 수정·삭제, 랜덤 채팅방과 메시지는 방의 `members`에 포함된 사용자만 접근, 학번 확인용 `student_numbers`는 단건 조회만 허용, `users` 문서의 `role`은 본인이 바꿀 수 없음(관리자 지정은 콘솔에서만). 규칙은 AI의 도움을 받아 작성했습니다.
- Firestore 복합 색인 3개: `posts`(border, timestamp 내림차순), `studyRooms`(isOpen, createdAt 내림차순), `waiting`(status, createdAt)
- Authentication: 이메일/비밀번호, Google 로그인 사용 설정

## 보안 개선 (2026.11)

코드를 점검해 아래 문제를 확인하고 고쳤습니다.

- 자동 로그인 비밀번호를 평문 SharedPreferences에서 `EncryptedSharedPreferences`로 옮겼습니다.
- 소스에 들어 있던 OpenAI API 키를 제거하고 `BuildConfig`로 주입하도록 바꿨습니다.
- 앱에 하드코딩된 관리자 코드와 앱 안에서 관리자로 승격하는 기능을 없앴습니다.
- Firestore 보안 규칙에서 `users.role`을 본인이 바꿀 수 없도록 막았습니다.
## 링크 · 시연

| 항목 | 링크 |
|---|---|
| Repository | https://github.com/gypsidanger4584-source/project-connect |
| 시간표 입력 시연 | [에브리타임 7클릭 → 커넥트 3클릭 (타자 제외)](docs/timetable-touch-compare-7to3.mp4) |

## 개발 정보

| 항목 | 내용 |
|---|---|
| 개발 기간 | 2025.04 ~ 2025.11. GitHub 저장소는 2026-08에 올렸고, 그 이전 개발 이력은 이 저장소 커밋에 남아 있지 않습니다. |
| 개발 인원 | 4인 팀 |
| 본인 담당 | 로그인·인증, 시간표, 메모, 학식 랜덤 매칭 채팅, 관리자 기능, Figma 디자인, 스터디 채팅방 아이디어 기획 |

## 비속어 필터

`TensorFlow Lite` 모델로 채팅 비속어 필터를 구현했다가 제거했습니다. 현재 `ChatRoomActivity`는 필터 없이 바로 전송하고, 모델 파일도 저장소에서 삭제했습니다.
