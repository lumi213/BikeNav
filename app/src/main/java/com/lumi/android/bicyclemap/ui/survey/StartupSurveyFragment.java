package com.lumi.android.bicyclemap.ui.survey;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;

public class StartupSurveyFragment extends Fragment {

    private MainViewModel vm;
    private TextView tvQuestion, tvLeft, tvRight, tvStep;
    private View leftHit, rightHit, btnSkip, loadingPanel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_startup_survey, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        tvQuestion   = v.findViewById(R.id.tvQuestion);
        tvLeft       = v.findViewById(R.id.tvLeft);
        tvRight      = v.findViewById(R.id.tvRight);
        tvStep       = v.findViewById(R.id.tvStep);
        leftHit      = v.findViewById(R.id.leftHit);
        rightHit     = v.findViewById(R.id.rightHit);
        btnSkip      = v.findViewById(R.id.btnSkip);
        loadingPanel = v.findViewById(R.id.loadingPanel);

        // 설문 단계 UI
        vm.getSurveyStep().observe(getViewLifecycleOwner(), step -> {
            if (step != null) updateStep(step);
        });

        // 로딩 상태 관찰 → 패널 토글 + 클릭 가능 여부 제어
        vm.getSurveyLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            if (loading) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        // 추천 결과가 오면 첫 코스를 자동 선택하고 닫기
        vm.getRecommendedCourses().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) return;
            CourseDto first = list.get(0);
            vm.setSelectedRoute(first); // 지도에서 이 코스를 사용
            closeSelf();
        });

        // 좌/우 반 화면 터치
        leftHit.setOnClickListener(view -> onChoose(true));
        rightHit.setOnClickListener(view -> onChoose(false));

        // 스킵: 보류 중인 GPT도 취소하고 닫기
        btnSkip.setOnClickListener(view -> {
            vm.cancelSurveyAndPending(); // 로딩/보류 해제
            closeSelf();
        });
    }

    private void updateStep(int step){
        tvStep.setText("Q" + step + "/3");
        if (step == 1) {
            tvQuestion.setText("오늘은 어떤 기분이신가요?");
            tvLeft.setText("산책");
            tvRight.setText("자전거길");
        } else if (step == 2) {
            tvQuestion.setText("동행 인원은 어떻게 되나요?");
            tvLeft.setText("소규모 (4인 이하)");
            tvRight.setText("단체");
        } else { // step == 3
            tvQuestion.setText("어떤 난이도를 원하시나요?");
            tvLeft.setText("쉬움");
            tvRight.setText("보통/어려움");
        }
    }

    private void onChoose(boolean left){
        Integer step = vm.getSurveyStep().getValue();
        if (step == null) return;

        if (step == 1) {
            vm.chooseRouteType(left ? MainViewModel.RouteType.WALK : MainViewModel.RouteType.BIKE);
        } else if (step == 2) {
            vm.chooseGroupType(left ? MainViewModel.GroupType.SMALL : MainViewModel.GroupType.LARGE);
        } else { // 마지막 선택
            // 로딩은 ViewModel이 surveyLoading을 올리면 자동 표시됨
            vm.chooseDifficulty(left ? MainViewModel.Difficulty.EASY : MainViewModel.Difficulty.NORMAL);
        }
    }

    private void showLoading() {
        if (loadingPanel != null) loadingPanel.setVisibility(View.VISIBLE);
        setClickable(false);
    }

    private void hideLoading() {
        if (loadingPanel != null) loadingPanel.setVisibility(View.GONE);
        setClickable(true);
    }

    private void setClickable(boolean enabled){
        if (leftHit != null)  leftHit.setEnabled(enabled);
        if (rightHit != null) rightHit.setEnabled(enabled);
        if (btnSkip != null)  btnSkip.setEnabled(enabled);
    }

    private void closeSelf(){
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .remove(this)
                .commitAllowingStateLoss();
    }
}
