package com.flys.dico.utils;

import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.flys.dico.fragments.adapters.WordAdapter;

class CustomURLSpan extends URLSpan {
    private WordAdapter.OnSearchActionListener onSearchActionListener;

    public CustomURLSpan(String url, WordAdapter.OnSearchActionListener actionListener) {
        super(url);
        this.onSearchActionListener = actionListener;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(View widget) {
        onSearchActionListener.search(getURL());
        super.onClick(widget);
    }
}