package com.flys.dico.utils;


import android.util.Log;

import androidx.appcompat.widget.SearchView;

import java.util.logging.Logger;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;


/**
 *
 */

public class RxSearchObservable {

    private static final  String TAG= "RxSearchObservable";
    public static Observable<String> fromSearchView(SearchView searchView) {
        final PublishSubject<String> publisher = PublishSubject.create();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.e(TAG, "onQueryTextSubmit(String s) : s value : " +s);
                publisher.onCompleted();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.e(TAG, "onQueryTextChange(String s) : s value : " +s);
                publisher.onNext(s);
                return true;
            }
        });
        return publisher;
    }
}
