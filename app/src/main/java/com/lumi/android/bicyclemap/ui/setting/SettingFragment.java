package com.lumi.android.bicyclemap.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;

public class SettingFragment extends Fragment {

    private SettingViewModel viewModel;
    private MainViewModel mainViewModel;

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

    }
}
