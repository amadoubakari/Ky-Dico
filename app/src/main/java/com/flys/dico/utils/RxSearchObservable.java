package com.flys.dico.utils;


import androidx.appcompat.widget.SearchView;

import rx.Observable;
import rx.subjects.PublishSubject;


/**
 *
 */

public class RxSearchObservable {

    public static Observable<String> fromSearchView(SearchView searchView) {
        final PublishSubject<String> publisher = PublishSubject.create();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                publisher.onCompleted();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                publisher.onNext(s);
                return true;
            }
        });
        return publisher;
    }
}
