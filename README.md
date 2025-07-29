# 🚲 KT 디인재 X 블루보드 - 자전거/산책 내비게이션 앱

이 프로젝트는 KT 디지털 인재 장학생 프로그램과 블루보드 연계 프로젝트로 개발된 **자전거 및 산책 네비게이션 앱**입니다. 사용자의 현재 위치 기반으로 추천 경로를 시각화하고, 경로별 주변 정보 및 리뷰 기능을 제공합니다.

---

## 🛠️ 환경설정 (Environment Setup)

### ✅ 개발 환경

| 항목 | 값 |
|------|-----|
| **Java** | Java 22 (build 22+36-2370)<br>※ Java 17 이상 필요 |
| **Android Studio** | Giraffe | 2022.3.1 Patch 3 |
| **Test Device** | Pixel 2 (API 34, Google Play 포함) |
| **BIOS 설정** | SVM (Secure Virtual Machine) 기능 BIOS에서 활성화 |
| **운영체제** | Windows |
| **지도 기능** | Google Maps API Key 필요 (아래 실행 방법 참고) |

---

## 🔐 SHA-1 인증서 지문 확인 방법

Google Maps API를 사용하려면 SHA-1 인증서 지문과 패키지명을 Google Cloud Console에 등록해야 합니다.

### ▪ macOS / Linux

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### ▪ Windows

```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## 🚀 실행 방법 (How to Run)

1. **레포지토리 클론**
   ```bash
   git clone https://github.com/lumi213/BikeNav.git
   cd BikeNav
   ```

2. **Android Studio로 프로젝트 열기**
   - Android Studio 실행
   - `Open` > `BikeNav` 프로젝트 폴더 선택

3. **Google Maps API 키 설정**
   - `app/src/main/res/values/google_maps_api.xml` 파일 생성
   - 아래 내용 입력:
     ```xml
     <resources>
         <string name="google_maps_key" translatable="false">YOUR_GOOGLE_MAPS_API_KEY</string>
     </resources>
     ```
   - [Google Cloud Console](https://console.cloud.google.com/apis/credentials)에서 API 키 생성 후
     - `Maps SDK for Android` API 활성화
     - 앱 SHA-1 + 패키지명 등록 (예: `com.lumi.android.bicyclemap`)

4. **에뮬레이터 실행**
   - Device Manager > Pixel 2 API 34 (Google Play 포함) 에뮬레이터 실행

5. **앱 실행**
   - Android Studio 상단 ▶ Run 버튼 클릭 또는 `Shift + F10`

---

## 📂 주요 폴더 구조

```
app/
├── java/com/lumi/android/bicyclemap/
│   ├── MainActivity.java       # 화면 전환 및 탭 컨트롤
│   ├── ui/map/MapFragment.java # 지도 및 경로/POI 표시
│   ├── ui/course/...           # 코스 관련 UI
│   └── ui/surrounding/...      # 주변 장소 필터링 기능
├── res/
│   ├── layout/                 # XML 레이아웃 파일
│   ├── values/                # 스타일, 색상, strings 등
│   └── drawable/              # 아이콘 및 이미지 리소스
```
