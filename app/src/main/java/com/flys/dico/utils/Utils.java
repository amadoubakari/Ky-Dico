package com.flys.dico.utils;

import android.text.Spannable;
import android.text.style.URLSpan;

import com.flys.dico.fragments.adapters.WordAdapter;

import java.util.Arrays;

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
}

