package com.lumi.android.bicyclemap.ui.setting;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.repository.AuthRepository;

public class SettingFragment extends Fragment {

    private SettingViewModel viewModel;
    private MainViewModel mainViewModel;

    // UI
    private ConstraintLayout sectionProfile;
    private ShapeableImageView imgAvatar;
    private TextView txtEmail, txtName;
    private MaterialButton btnEditProfile, btnAccount, btnLogin;

    // 프로필 이미지 로컬 저장 키
    private static final String PREFS = "my_page_prefs";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

    // ✅ OpenDocument: 영구 접근 가능 (persistable)
    // MIME 타입 배열을 받는다. 이미지만 고를 거면 "image/*" 하나만 주면 됨.
    private final ActivityResultLauncher<String[]> openImageDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    // 영구 접근 플래그 요청: READ(필수) + (가능하면) WRITE
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    try {
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception ignore) {
                        // 일부 프로바이더는 WRITE를 안 줄 수 있음. READ만으로도 충분.
                        try {
                            requireContext().getContentResolver()
                                    .takePersistableUriPermission(uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignoredAgain) {}
                    }

                    // UI 반영
                    Glide.with(this).load(uri).into(imgAvatar);
                    // 문자열로 저장 (재부팅/재실행 후에도 사용 가능)
                    saveProfileImage(requireContext(), uri.toString());
                }
            });

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

        viewModel = new ViewModelProvider(requireActivity()).get(SettingViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 뷰 바인딩
        sectionProfile = view.findViewById(R.id.sectionProfile);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtName = view.findViewById(R.id.txtName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnAccount = view.findViewById(R.id.btnAccount);
        btnLogin = view.findViewById(R.id.btnLogin);

        // 로그인 상태에 따른 UI 토글
        boolean loggedIn = AuthRepository.getInstance(requireContext()).isLoggedIn();
        toggleLoginStateUI(loggedIn);

        // 저장된 아바타 로드 (persistable Uri)
        String saved = getSavedProfileImage(requireContext());
        if (saved != null && !saved.isEmpty()) {
            Glide.with(this).load(Uri.parse(saved)).into(imgAvatar);
        }

        // 프로필 수정 → OpenDocument 실행
        btnEditProfile.setOnClickListener(v -> {
            // SAF 문서 선택기 실행 (이미지 전용)
            // 최초 1회 선택 후 앱 재실행/재부팅해도 접근 가능(위의 takePersistableUriPermission 덕분)
            openImageDocument.launch(new String[]{"image/*"});
        });

        btnAccount.setOnClickListener(v -> {
            // TODO: 계정 관리 화면으로 이동 (프로젝트 라우팅에 맞춰 연결)
            // 예) startActivity(new Intent(requireContext(), AccountActivity.class));
            // 또는 NavController 사용 시: findNavController().navigate(R.id.action_setting_to_account);
        });

        btnLogin.setOnClickListener(v -> {
            // TODO: 로그인 화면으로 이동
            // 로그인 성공 시 아래 두 줄을 콜백에서 실행해주면 UI 갱신됨
            // toggleLoginStateUI(true);
            // bindUserInfo();
        });

        btnLogin.setOnClickListener(v -> {
            showAuthDialog();
        });
    }

    /** 로그인/회원가입 팝업 */
    private void showAuthDialog() {
        ContextThemeWrapper dialogCtx = new ContextThemeWrapper(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);

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

        final boolean[] isRegisterMode = { false }; // false=로그인, true=회원가입

        Dialog dialog = new MaterialAlertDialogBuilder(dialogCtx).setView(dialogView).setCancelable(false).create();
        // 모드 전환 UI 반영
        Runnable applyMode = () -> {
            if (isRegisterMode[0]) {
                //tvTitle.setText("회원가입");
                tilName.setVisibility(View.VISIBLE);
                btnSubmit.setText("회원가입");
                btnSwitch.setText("로그인");
            } else {
                //tvTitle.setText("로그인");
                tilName.setVisibility(View.GONE);
                btnSubmit.setText("로그인");
                btnSwitch.setText("회원가입");
            }
        };
        applyMode.run();

        // 전환 버튼
        btnSwitch.setOnClickListener(v -> {
            isRegisterMode[0] = !isRegisterMode[0];
            applyMode.run();
        });

        // 취소
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 전송(로그인/회원가입)
        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pwd   = etPassword.getText() == null ? "" : etPassword.getText().toString();

            // 간단한 검증
            tilEmail.setError(null);
            tilPassword.setError(null);
            tilName.setError(null);

            if (email.isEmpty()) {
                tilEmail.setError("이메일을 입력하세요.");
                return;
            }
            if (pwd.length() < 8) {
                tilPassword.setError("비밀번호는 8자 이상이어야 합니다.");
                return;
            }

            if (isRegisterMode[0]) {
                String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                if (name.isEmpty()) {
                    tilName.setError("이름을 입력하세요.");
                    return;
                }
                // 회원가입 호출
                setLoading(true, progress, btnSubmit, btnSwitch, btnCancel);
                AuthRepository.getInstance(requireContext())
                        .register(name, pwd, email, new AuthRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.ApiResponse>() {
                            @Override
                            public void onSuccess(com.lumi.android.bicyclemap.api.dto.ApiResponse response) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), response.getMessage() != null ? response.getMessage() : "회원가입 성공", Toast.LENGTH_SHORT).show();
                                // 회원가입 후 로그인 모드로 전환
                                isRegisterMode[0] = false;
                                applyMode.run();
                            }
                            @Override
                            public void onError(String errorMessage) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });

            } else {
                // 로그인 호출
                setLoading(true, progress, btnSubmit, btnSwitch, btnCancel);
                AuthRepository.getInstance(requireContext())
                        .login(email, pwd, new AuthRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.AuthResponse>() {
                            @Override
                            public void onSuccess(com.lumi.android.bicyclemap.api.dto.AuthResponse response) {
                                setLoading(false, progress, btnSubmit, btnSwitch, btnCancel);
                                Toast.makeText(requireContext(), response.getMessage() != null ? response.getMessage() : "로그인 성공", Toast.LENGTH_SHORT).show();

                                // 프로필 섹션 갱신
                                toggleLoginStateUI(true);
                                // 바인딩(이메일/이름)
                                bindUserInfo();
                                dialog.dismiss();
                            }
                            @Override
                            public void onError(String errorMessage) {
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
        }
    }

    private void bindUserInfo() {
        String email = AuthRepository.getInstance(requireContext()).getUserEmail();
        String name = AuthRepository.getInstance(requireContext()).getUserName();
        if (email != null && !email.isEmpty()) txtEmail.setText(email);
        if (name != null && !name.isEmpty())   txtName.setText(name);
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
