# BikeNav API 구조

## 개요
API 명세서에 맞춰 구현된 완전한 API 클라이언트 구조입니다. JWT 인증을 포함하여 로컬 서버 주소(`http://10.0.2.2:8080/`)로 설정되어 있습니다. 모든 ViewModel이 API와 연결되어 있습니다.

## 구조

### 1. API 클라이언트 (`ApiClient.java`)
- Retrofit 설정
- 로컬 서버 주소: `http://10.0.2.2:8080/`
- 타임아웃: 15초
- 로깅 인터셉터 포함
- JWT 인터셉터 포함

### 2. API 서비스 (`ApiService.java`)
- 모든 API 엔드포인트 정의
- Retrofit 어노테이션 사용

### 3. DTO 클래스들
- `ApiResponse<T>`: 공통 응답 구조
- `AuthRequest/AuthResponse`: 인증 관련
- `CourseDto/CourseListResponse`: 코스 관련
- `PoiDto/PoiListResponse`: POI 관련
- `LocationRequest`: 위치 전송
- `TrackingRequest/TrackingResponse`: 트래킹 관련
- `ReviewRequest`: 후기 관련

### 4. JWT 인증
- `JwtTokenManager`: JWT 토큰 관리 (SharedPreferences 사용)
- `JwtInterceptor`: 자동으로 Authorization 헤더에 토큰 추가
- 자동 토큰 저장 및 관리

### 5. Repository 패턴
- `CourseRepository`: 코스 데이터 관리 (캐싱 포함)
- `PoiRepository`: POI 데이터 관리
- `AuthRepository`: 인증 데이터 관리

### 6. API 매니저 (`ApiManager.java`)
- API 호출을 쉽게 관리하는 싱글톤 클래스
- 콜백 기반 비동기 처리
- 에러 핸들링 포함
- JWT 토큰 자동 관리

### 7. ViewModel API 연결
- `MainViewModel`: 전체 앱 상태 + 코스 API
- `CourseViewModel`: 코스 목록/상세 API
- `MapsViewModel`: 지도용 코스 API
- `SurroundingViewModel`: POI API

## 사용법

### JWT 인증 사용법
```java
// Repository 초기화 (Context 필요)
AuthRepository authRepo = new AuthRepository(context);

// 로그인 (JWT 토큰 자동 저장)
authRepo.login("email@example.com", "password", new AuthRepository.RepositoryCallback<AuthResponse>() {
    @Override
    public void onSuccess(AuthResponse response) {
        // 로그인 성공, JWT 토큰이 자동으로 저장됨
        // 이후 모든 API 호출에 자동으로 Authorization 헤더 추가
    }

    @Override
    public void onError(String errorMessage) {
        // 에러 처리
    }
});

// 로그인 상태 확인
if (authRepo.isLoggedIn()) {
    // 로그인된 상태
    int userId = authRepo.getCurrentUserId();
}

// 로그아웃
authRepo.logout();
```

### ViewModel API 사용법
```java
// 1. CourseViewModel 사용
CourseViewModel courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);
courseViewModel.init(getContext()); // Context 초기화 필수

// 코스 목록 로드
courseViewModel.loadCourses("bike", null, true);

// 결과 관찰
courseViewModel.getCourses().observe(this, courses -> {
    // UI 업데이트
});

courseViewModel.getIsLoading().observe(this, loading -> {
    // 로딩 상태 처리
});

courseViewModel.getErrorMessage().observe(this, error -> {
    // 에러 처리
});

// 2. MapsViewModel 사용
MapsViewModel mapsViewModel = new ViewModelProvider(this).get(MapsViewModel.class);
mapsViewModel.init(getContext());

// 지도용 코스 로드
mapsViewModel.loadCoursesForMap("bike", "중(2)", true);

// 3. SurroundingViewModel 사용
SurroundingViewModel surroundingViewModel = new ViewModelProvider(this).get(SurroundingViewModel.class);
surroundingViewModel.init(getContext());

// POI 목록 로드
surroundingViewModel.loadPois(courseId, "Cafe");
```

### Repository 직접 사용법
```java
// 코스 데이터 조회 (JWT 토큰 자동 포함)
CourseRepository courseRepo = new CourseRepository(context);
courseRepo.getCourses("bike", null, true, new CourseRepository.RepositoryCallback<CourseListResponse>() {
    @Override
    public void onSuccess(CourseListResponse response) {
        // 성공 처리
    }

    @Override
    public void onError(String errorMessage) {
        // 에러 처리
    }
});

// 인증
AuthRepository authRepo = new AuthRepository(context);
authRepo.login("email@example.com", "password", new AuthRepository.RepositoryCallback<AuthResponse>() {
    @Override
    public void onSuccess(AuthResponse response) {
        // 성공 처리
    }

    @Override
    public void onError(String errorMessage) {
        // 에러 처리
    }
});
```

### 주요 API 호출 예시

#### 1. 인증
```java
// 회원가입
authRepo.register("사용자명", "비밀번호", "이메일", callback);

// 로그인 (JWT 토큰 자동 저장)
authRepo.login("이메일", "비밀번호", callback);

// 로그아웃
authRepo.logout();

// 로그인 상태 확인
boolean isLoggedIn = authRepo.isLoggedIn();
```

#### 2. 코스
```java
// 코스 목록 조회 (JWT 토큰 자동 포함)
courseRepo.getCourses("bike", "중(2)", true, callback);

// 코스 상세 조회 (JWT 토큰 자동 포함)
courseRepo.getCourseDetail(courseId, callback);
```

#### 3. POI
```java
// POI 목록 조회 (JWT 토큰 자동 포함)
poiRepo.getPois(courseId, "Cafe", callback);

// POI 상세 조회 (JWT 토큰 자동 포함)
poiRepo.getPoiDetail(courseId, placeId, callback);
```

## JWT 인증 특징

### 1. 자동 토큰 관리
- 로그인 시 JWT 토큰 자동 저장
- 모든 API 호출에 자동으로 Authorization 헤더 추가
- 로그아웃 시 토큰 자동 삭제

### 2. 보안
- SharedPreferences에 안전하게 토큰 저장
- 토큰 만료 시 자동 로그아웃 처리 가능

### 3. 편의성
- 개발자가 직접 토큰을 관리할 필요 없음
- 모든 API 호출에 자동으로 인증 헤더 포함

## ViewModel API 연결 특징

### 1. 자동 초기화
- Context 기반 Repository 초기화
- 에러 핸들링 자동화
- 로딩 상태 자동 관리

### 2. LiveData 통합
- API 결과를 LiveData로 자동 변환
- UI 자동 업데이트
- 생명주기 관리

### 3. 에러 처리
- 모든 API 호출에 에러 핸들링
- 사용자 친화적 에러 메시지
- 로딩 상태 표시

## 에러 처리

모든 API 호출은 `RepositoryCallback` 인터페이스를 통해 결과를 받습니다:

```java
public interface RepositoryCallback<T> {
    void onSuccess(T response);
    void onError(String errorMessage);
}
```

## 서버 설정

현재 로컬 서버 주소로 설정되어 있습니다:
- `http://10.0.2.2:8080/` (Android 에뮬레이터용)
- 실제 서버로 변경 시 `ApiClient.java`의 `BASE_URL` 수정 필요

## 주의사항

1. **네트워크 권한**이 필요합니다
2. **서버가 실행 중**이어야 합니다
3. **에러 처리를 항상 구현**해야 합니다
4. **UI 스레드에서 콜백을 처리**할 때는 `runOnUiThread` 사용
5. **Repository 초기화 시 Context가 필요**합니다
6. **ViewModel 사용 시 init(Context) 호출 필수**입니다

## 현재 구현 상태

✅ **완료된 부분**
- API 인프라 (ApiClient, ApiService, ApiManager)
- JWT 인증 시스템
- Repository 패턴
- 모든 ViewModel API 연결
- 에러 핸들링
- 로딩 상태 관리

🔄 **추가 작업 필요**
- DTO ↔ 기존 모델 변환 로직
- 실제 UI에서 API 호출 테스트
- 토큰 만료 처리
- 오프라인 캐싱 강화 