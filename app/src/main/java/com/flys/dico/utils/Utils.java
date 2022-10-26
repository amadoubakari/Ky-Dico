package com.flys.dico.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.Spannable;
import android.text.style.URLSpan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flys.dico.R;
import com.flys.dico.fragments.adapters.WordAdapter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;

/**
 * @version 1.0.0
 * @autor AMADOU BAKARI
 * @date 18/08/2022
 * @goals application utilities
 */
public class Utils {

    public static void stripUnderlines(Spannable spannable, WordAdapter.OnSearchActionListener onSearchActionListener) {
        Arrays.stream(spannable.getSpans(0, spannable.length(), URLSpan.class)).forEach(span -> {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new CustomURLSpan(span.getURL(),onSearchActionListener);
            spannable.setSpan(span, start, end, 0);
        });
    }

    public static Observable<List<String>> loadHighLightedWords(Context context){
        return Observable.create(subscriber -> {
            try {
                ObjectMapper jsonMapper=new ObjectMapper();
                List<String> words = jsonMapper.readValue(context.getAssets().open(context.getString(R.string.words_data_source), AssetManager.ACCESS_STREAMING), ArrayList.class);
                //and observable that are going to emit data
                subscriber.onNext(words);
                subscriber.onCompleted();
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        });
    }

    public static Observable<List<String>> loadHighLightedWords(Context context, @NotNull String locale){
        return Observable.create(subscriber -> {
            try {
                ObjectMapper jsonMapper=new ObjectMapper();
                List<String> words = jsonMapper.readValue(context.getAssets().open(context.getString(R.string.words_data_source).concat("-").concat(locale).concat(".json"), AssetManager.ACCESS_STREAMING), ArrayList.class);
                //and observable that are going to emit data
                subscriber.onNext(words);
                subscriber.onCompleted();
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        });
    }

}

