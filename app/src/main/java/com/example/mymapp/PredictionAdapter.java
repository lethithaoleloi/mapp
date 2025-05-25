package com.example.mymapp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class PredictionAdapter extends ArrayAdapter<AutocompletePrediction> {

    private final List<AutocompletePrediction> predictions;
    private final String keyword;

    public PredictionAdapter(Context context, List<AutocompletePrediction> predictions, String keyword) {
        super(context, android.R.layout.simple_dropdown_item_1line, predictions);
        this.predictions = predictions;
        this.keyword = keyword.toLowerCase();
    }

    @Override
    public int getCount() {
        return predictions.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return predictions.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        String description = predictions.get(position).getFullText(null).toString();

        Spannable spannable = new SpannableString(description);
        int start = description.toLowerCase().indexOf(keyword);
        if (start >= 0) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, start + keyword.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannable);
        return view;
    }
}
