package com.example.yugiohdeckbuilder;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.yugiohdeckbuilder.model.LanguageItem;

import java.util.List;

public class LanguageSpinnerAdapter extends ArrayAdapter<LanguageItem> {

    public LanguageSpinnerAdapter(Context context, List<LanguageItem> languageList) {
        super(context, 0, languageList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.spinner_item_language, parent, false
            );
        }

        ImageView imageViewFlag = convertView.findViewById(R.id.image_view_flag);
        TextView textViewName = convertView.findViewById(R.id.text_view_language_name);

        LanguageItem currentItem = getItem(position);

        if (currentItem != null) {
            imageViewFlag.setImageResource(currentItem.getFlagImage());
            textViewName.setText(currentItem.getLanguageName());
        }

        return convertView;
    }
}