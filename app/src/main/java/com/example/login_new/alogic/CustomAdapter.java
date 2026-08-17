package com.example.login_new.alogic;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends ArrayAdapter<String> {

    private final List<String> originalList;
    private List<String> filteredList;

    public CustomAdapter(@NonNull Context context, int resource, @NonNull List<String> items) {
        super(context, resource, items);
        originalList = new ArrayList<>(items);
        filteredList = new ArrayList<>(items);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return choseongFilter;
    }

    private final Filter choseongFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<String> suggestions = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                suggestions.addAll(originalList);
            } else {
                String filterPattern = constraint.toString();

                for (String item : originalList) {
                    if (matchesChoseong(item, filterPattern)) {
                        suggestions.add(item);
                    }
                }
            }

            results.values = suggestions;
            results.count = suggestions.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            if (results != null && results.values != null) {
                //noinspection unchecked
                addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        }
    };

    // 초성 추출
    private String getChoseong(String str) {
        StringBuilder result = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int base = ch - 0xAC00;
                int choseongIndex = base / (21 * 28);
                char[] choseongList = {
                        'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
                };
                result.append(choseongList[choseongIndex]);
            } else {
                result.append(ch); // 영문, 숫자 등은 그대로
            }
        }
        return result.toString();
    }

    private boolean matchesChoseong(String word, String query) {
        return getChoseong(word).contains(query) || word.contains(query);
    }
}
