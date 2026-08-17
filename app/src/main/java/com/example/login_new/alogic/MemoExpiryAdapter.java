package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * 메모 만료 기간 선택 스피너의 텍스트 색상을 흰색으로 설정하고
 * 드롭다운 텍스트 크기를 조정하는 커스텀 어댑터.
 */
public class MemoExpiryAdapter extends ArrayAdapter<String> {

    public MemoExpiryAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
    }

    // 스피너가 닫혀 있을 때(선택된 상태)의 뷰
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        // 🌟 항상 흰 글자 + 살짝 크게
        view.setTextColor(Color.WHITE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setPadding(0, 0, 0, 0);
        return view;
    }

    // 스피너가 열렸을 때(드롭다운 리스트)의 뷰
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        // 🌟 드롭다운도 흰 글자 + 어두운 배경
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.parseColor("#222222"));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return view;
    }
}
