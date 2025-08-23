package com.lumi.android.bicyclemap.ui.setting;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.ApiClient;
import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseReviewDto;
import com.lumi.android.bicyclemap.api.dto.CourseReviewListResponse;
import com.lumi.android.bicyclemap.data.local.entity.CompletedCourseEntity;
import com.lumi.android.bicyclemap.data.local.repository.CompletedCourseRepository;
import com.lumi.android.bicyclemap.repository.AuthRepository;
import com.lumi.android.bicyclemap.ui.course.CourseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 설정 탭
 * - 프로필/로그인
 * - 내가 완주한 코스: CourseAdapter(item_course) (최신순)
 * - 내가 작성한 리뷰: /api/review/course/{courseId} 를 코스별로 호출 → user_id 로 필터
 * - 토큰 만료(401/403) 시 재로그인 다이얼로그 → 성공 후 자동 재시도
 */
public class SettingFragment extends Fragment {

    private SettingViewModel viewModel;
    private MainViewModel mainViewModel;

    // ===== 프로필/로그인 UI =====
    private ConstraintLayout sectionProfile;
    private ShapeableImageView imgAvatar;
    private TextView txtEmail, txtName;
    private MaterialButton btnEditProfile, btnAccount, btnLogin;

    private static final String PREFS = "my_page_prefs";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

    private final ActivityResultLauncher<String[]> openImageDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    try {
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception ignore) {
                        try {
                            requireContext().getContentResolver()
                                    .takePersistableUriPermission(uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignoredAgain) {}
                    }
                    Glide.with(this).load(uri).into(imgAvatar);
                    saveProfileImage(requireContext(), uri.toString());
                }
            });

    // ===== 리스트 UI =====
    private RecyclerView rvCompleted, rvMyReviews;
    private TextView txtCompletedTitle, txtReviewTitle;

    private CourseAdapter     completedAdapter; // item_course
    private ReviewCardAdapter reviewAdapter;    // item_review

    // 코스 캐시 (courseId → CourseDto)
    private final HashMap<Integer, CourseDto> courseCache = new HashMap<>();

    // 리뷰 로딩 상태
    private boolean loadingMyReviews = false;

    // 재로그인 다이얼로그 제어
    private boolean isAuthDialogShowing = false;
    private Runnable pendingAfterLogin = null;

    public SettingFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ===== VM =====
        viewModel     = new ViewModelProvider(requireActivity()).get(SettingViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // ===== 프로필 바인딩 =====
        sectionProfile = view.findViewById(R.id.sectionProfile);
        imgAvatar      = view.findViewById(R.id.imgAvatar);
        txtEmail       = view.findViewById(R.id.txtEmail);
        txtName        = view.findViewById(R.id.txtName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnAccount     = view.findViewById(R.id.btnAccount);
        btnLogin       = view.findViewById(R.id.btnLogin);

        boolean loggedIn = AuthRepository.getInstance(requireContext()).isLoggedIn();
        toggleLoginStateUI(loggedIn);

        String saved = getSavedProfileImage(requireContext());
        if (!TextUtils.isEmpty(saved)) Glide.with(this).load(Uri.parse(saved)).into(imgAvatar);

        btnEditProfile.setOnClickListener(v -> openImageDocument.launch(new String[]{"image/*"}));
        btnAccount.setOnClickListener(v -> {
            // TODO: 계정 관리 화면 이동
        });
        btnLogin.setOnClickListener(v -> showAuthDialog(null));

        // ===== 리스트 바인딩 =====
        rvCompleted      = view.findViewById(R.id.rvCompleted);
        rvMyReviews      = view.findViewById(R.id.rvReviews);
        txtCompletedTitle= view.findViewById(R.id.txtCompletedTitle);
        txtReviewTitle   = view.findViewById(R.id.txtReviewTitle);

        rvCompleted.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMyReviews.setLayoutManager(new LinearLayoutManager(requireContext()));

        completedAdapter = new CourseAdapter(mainViewModel);
        reviewAdapter    = new ReviewCardAdapter();
        rvCompleted.setAdapter(completedAdapter);
        rvMyReviews.setAdapter(reviewAdapter);

        // 코스 목록 들어오면 캐시에 담고 섹션 갱신
        mainViewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            courseCache.clear();
            if (routes != null) for (CourseDto c : routes) courseCache.put(c.getCourse_id(), c);
            loadCompletedList();
            loadMyReviewsAcrossCourses(); // 로그인 상태라면 내 리뷰 로드
        });

        // 초기도 로드
        loadCompletedList();
        loadMyReviewsAcrossCourses();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 완주 목록 (로컬 DB → 최신순)  → CourseAdapter(item_course)
    // ─────────────────────────────────────────────────────────────────────
    private void loadCompletedList() {
        mainViewModel.fetchCompleted(new CompletedCourseRepository.Callback<List<CompletedCourseEntity>>() {
            @Override public void onResult(List<CompletedCourseEntity> data) {
                List<CourseDto> mapped = new ArrayList<>();
                if (data != null) {
                    for (CompletedCourseEntity e : data) {
                        CourseDto c = courseCache.get(e.courseId);
                        if (c != null) mapped.add(c);
                    }
                }
                completedAdapter.submitList(mapped);
                toggleCompletedSection(!mapped.isEmpty());
            }
        });
    }

    private void toggleCompletedSection(boolean show) {
        if (txtCompletedTitle != null) txtCompletedTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvCompleted != null)       rvCompleted.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 내 리뷰 로드:
    // - 모든 코스에 대해 /api/review/course/{courseId} 호출
    // - user_id == currentUserId 만 수집
    // - 401/403 → 재로그인 다이얼로그 → 성공 시 전체 재시도
    // ─────────────────────────────────────────────────────────────────────
    private void loadMyReviewsAcrossCourses() {
        AuthRepository auth = AuthRepository.getInstance(requireContext());
        if (!auth.isLoggedIn()) {
            reviewAdapter.submit(new ArrayList<>());
            toggleMyReviewsSection(false);
            return;
        }
        List<CourseDto> routes = mainViewModel.getAllRoutes().getValue();
        if (routes == null || routes.isEmpty()) {
            reviewAdapter.submit(new ArrayList<>());
            toggleMyReviewsSection(false);
            return;
        }
        if (loadingMyReviews) return; // 중복 방지
        loadingMyReviews = true;

        int userId = auth.getCurrentUserId();
        ApiService api = ApiClient.getInstance(requireContext()).getApiService();
        List<ReviewCardAdapter.Row> myRows = new ArrayList<>();

        fetchNextCourseReviewSequential(api, routes, 0, userId, myRows, new Runnable() {
            @Override public void run() {
                loadingMyReviews = false;
                reviewAdapter.submit(myRows);
                toggleMyReviewsSection(!myRows.isEmpty());
            }
        });
    }

    private void fetchNextCourseReviewSequential(ApiService api,
                                                 List<CourseDto> routes,
                                                 int index,
                                                 int userId,
                                                 List<ReviewCardAdapter.Row> acc,
                                                 Runnable done) {
        if (index >= routes.size()) {
            done.run();
            return;
        }
        CourseDto course = routes.get(index);
        int courseId = course.getCourse_id();

        Call<ApiResponse<CourseReviewListResponse>> call = api.getCourseReviews(courseId);
        call.enqueue(new Callback<ApiResponse<CourseReviewListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseReviewListResponse>> c,
                                   Response<ApiResponse<CourseReviewListResponse>> res) {
                // 인증 실패 처리
                if (res.code() == 401 || res.code() == 403) {
                    loadingMyReviews = false; // 재시도 가능하게 해제
                    handleAuthFailureRetry(() -> loadMyReviewsAcrossCourses());
                    return;
                }

                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()
                        && res.body().getData() != null && res.body().getData().reviews != null) {
                    for (CourseReviewDto r : res.body().getData().reviews) {
                        if (r.user_id == userId) {
                            String title = safe(course.getTitle());
                            String diffBadge = diffToBadge(course.getDiff());
                            String detail = buildCourseDetail(course);
                            String thumbUrl = course.getImage();
                            String content = safe(r.content);
                            acc.add(new ReviewCardAdapter.Row(title, diffBadge, detail, content, thumbUrl));
                        }
                    }
                }
                // 다음 코스
                fetchNextCourseReviewSequential(api, routes, index + 1, userId, acc, done);
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseReviewListResponse>> c, Throwable t) {
                // 네트워크 실패 → 그냥 다음 코스로 진행
                fetchNextCourseReviewSequential(api, routes, index + 1, userId, acc, done);
            }
        });
    }

    /** 토큰 만료 시 재로그인 다이얼로그를 띄우고, 성공하면 retryAction 실행 */
    private void handleAuthFailureRetry(Runnable retryAction) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (isAuthDialogShowing) {
                // 이미 열려있다면, 로그인 성공 후 실행할 작업만 갱신
                pendingAfterLogin = retryAction;
                return;
            }
            showAuthDialog(retryAction);
        });
    }

    private void toggleMyReviewsSection(boolean show) {
        if (txtReviewTitle != null) txtReviewTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvMyReviews != null)    rvMyReviews.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String diffToBadge(Integer diff) {
        if (diff == null) return "";
        if (diff == 1) return " 상";
        if (diff == 2) return " 중";
        if (diff == 3) return " 하";
        return "";
    }

    private String buildCourseDetail(CourseDto c) {
        if (c == null) return "";
        String dist = String.format(Locale.getDefault(), "경로 %.1fkm", c.getDist_km());
        String time = (c.getTime() + "분");
        String tag1 = (c.getTags() != null && !c.getTags().isEmpty()) ? (" · #" + c.getTags().get(0)) : "";
        String mid = (!TextUtils.isEmpty(dist) && !TextUtils.isEmpty(time)) ? " · " : "";
        return (dist + mid + time + tag1).trim();
    }

    private String safe(String s){ return s == null ? "" : s; }

    // ─────────────────────────────────────────────────────────────────────
    // 로그인/회원가입 다이얼로그
    //  - afterLogin != null 이면 로그인 성공 후 해당 Runnable 실행
    // ─────────────────────────────────────────────────────────────────────
    private void showAuthDialog(@Nullable Runnable afterLogin) {
        if (isAuthDialogShowing) { // 이미 떠 있으면 콜백만 바꿔두고 종료
            pendingAfterLogin = afterLogin;
            return;
        }
        isAuthDialogShowing = true;
        pendingAfterLogin = afterLogin;

        ContextThemeWrapper dialogCtx = new ContextThemeWrapper(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);

        View dialogView = LayoutInflater.from(dialogCtx).inflate(R.layout.dialog_auth, null, false);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilName);
        TextInputLayout tilEmail = dialogView.findViewById(R.id.tilEmail);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilPassword);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        MaterialButton btnSwitch = dialogView.findViewById(R.id.btnSwitch);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        ProgressBar progress = dialogView.findViewById(R.id.progress);

        final boolean[] isRegisterMode = { false };
        final Dialog dialog = new MaterialAlertDialogBuilder(dialogCtx)
                .setView(dialogView).setCancelable(false).create();

        Runnable applyMode = () -> {
            if (isRegisterMode[0]) {
                tilName.setVisibility(View.VISIBLE);
                btnSubmit.setText("회원가입");
                btnSwitch.setText("로그인");
            } else {
                tilName.setVisibility(View.GONE);
                btnSubmit.setText("로그인");
                btnSwitch.setText("회원가입");
            }
        };
        applyMode.run();

        btnSwitch.setOnClickListener(v -> {
            isRegisterMode[0] = !isRegisterMode[0];
            applyMode.run();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            isAuthDialogShowing = false;
            // 취소 시 대기중이던 작업은 버림
            pendingAfterLogin = null;
        });

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pwd   = etPassword.getText() == null ? "" : etPassword.getText().toString();

            tilEmail.setError(null);
            tilPassword.setError(null);
            tilName.setError(null);

            if (email.isEmpty()) { tilEmail.setError("이메일을 입력하세요."); return; }
            if (pwd.length() < 8) { tilPassword.setError("비밀번호는 8자 이상이어야 합니다."); return; }

            if (isRegisterMode[0]) {
                String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                if (name.isEmpty()) { tilName.setError("이름을 입력하세요."); return; }

                setLoading(true, progress, btnSubmit, btnSwitch, btnCancel);
                AuthRepository.getInstance(requireContext())
                        .register(name, pwd, email, new AuthRepository.RepositoryCallback<ApiResponse>() {
                            @Override public void onSuccess(ApiResponse response) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), response.getMessage() != null ? response.getMessage() : "회원가입 성공", Toast.LENGTH_SHORT).show();
                                isRegisterMode[0] = false;
                                applyMode.run();
                            }
                            @Override public void onError(String errorMessage) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });

            } else {
                setLoading(true, progress, btnSubmit, btnSwitch, btnCancel);
                AuthRepository.getInstance(requireContext())
                        .login(email, pwd, new AuthRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.AuthResponse>() {
                            @Override public void onSuccess(com.lumi.android.bicyclemap.api.dto.AuthResponse response) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), response.getMessage() != null ? response.getMessage() : "로그인 성공", Toast.LENGTH_SHORT).show();
                                toggleLoginStateUI(true);
                                bindUserInfo();

                                // 로그인 성공 → 대기 중 작업 실행 또는 기본 동작
                                Runnable task = pendingAfterLogin;
                                pendingAfterLogin = null;
                                dialog.dismiss();
                                isAuthDialogShowing = false;

                                if (task != null) {
                                    task.run();
                                } else {
                                    // 기본: 내 리뷰/완주 갱신
                                    loadMyReviewsAcrossCourses();
                                    loadCompletedList();
                                }
                            }
                            @Override public void onError(String errorMessage) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }

    private void setLoading(boolean loading, ProgressBar progress,
                            MaterialButton btnSubmit, MaterialButton btnSwitch, MaterialButton btnCancel) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnSwitch.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
    }

    private void toggleLoginStateUI(boolean loggedIn) {
        if (loggedIn) {
            sectionProfile.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            bindUserInfo();
        } else {
            sectionProfile.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            toggleMyReviewsSection(false);
        }
    }

    private void bindUserInfo() {
        String email = AuthRepository.getInstance(requireContext()).getUserEmail();
        String name  = AuthRepository.getInstance(requireContext()).getUserName();
        if (!TextUtils.isEmpty(email)) txtEmail.setText(email);
        if (!TextUtils.isEmpty(name))  txtName.setText(name);
    }

    // ===== 저장/로드 유틸 =====
    private void saveProfileImage(Context c, String uri) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PROFILE_IMAGE_URI, uri).apply();
    }
    private String getSavedProfileImage(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_PROFILE_IMAGE_URI, "");
    }
}
