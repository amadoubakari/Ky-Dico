/*
 * @copyright reserved Kyossi Ltd.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * */
package com.flys.dico.fragments.behavior;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.Utils;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.dico.utils.Constants;
import com.flys.dico.utils.RxSearchObservable;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @email amadoubakari1992@gmail.com
 * @since 08/08/20
 */
@EFragment(R.layout.fragment_home_layout)
@OptionsMenu(R.menu.menu_home)
public class HomeFragment extends AbstractFragment {

    private final String TAG = "HomeFragment";

    private static WordAdapter wordAdapter;
    private static List<Word> words;
    private static SearchView searchView;
    private static final int size = 10;
    private static int index = 0;
    private static boolean wasInPause = false;
    private static FirebaseDatabase database;
    private int itemsPerDisplay = 6;
    // Creates instance of the update app manager.
    private AppUpdateManager appUpdateManager;

    @ViewById(R.id.recyclerview)
    protected RecyclerView recyclerView;

    @OptionsMenuItem(R.id.search)
    protected MenuItem menuItem;

    @ViewById(R.id.ll_search_block_id)
    protected LinearLayout llSearchBlock;

    @Override
    public CoreState saveFragment() {
        return new CoreState();
    }

    @Override
    protected int getNumView() {
        return mainActivity.HOME_FRAGMENT;
    }

    @Override
    protected void initFragment(CoreState previousState) {
        Log.e(TAG, "initFragment");
        ((AppCompatActivity) mainActivity).getSupportActionBar().show();
        if (previousState == null) {
            reloadData();
        }
    }

    @Override
    protected void initView(CoreState previousState) {
        if (Constants.isNetworkConnected) {
            Observable<Boolean> observable = Observable.create(subscriber -> {
                subscriber.onNext(Boolean.TRUE);
                subscriber.onCompleted();
            });
            observable
                    .delay(15000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(o -> {
                        mainActivity.checkUpdatesAvailable();
                    });
        }
    }

    @Override
    protected void updateOnSubmit(CoreState previousState) {
        Log.e(TAG, "updateOnSubmit");
        reloadData();
    }

    @Override
    protected void updateOnRestore(CoreState previousState) {
        Log.e(TAG, "updateOnRestore");
    }

    @Override
    protected void notifyEndOfUpdates() {
        Log.e(TAG, "notifyEndOfUpdates");

    }

    @Override
    protected void notifyEndOfTasks(boolean runningTasksHaveBeenCanceled) {

    }

    @Override
    protected boolean hideNavigationBottomView() {
        return false;
    }

    @Override
    public void onFragmentResume() {
        super.onFragmentResume();
        if (wasInPause) {
            reloadData();
        }
    }

    @OptionsItem(R.id.search)
    protected void doSearch() {
        searchView = (SearchView) menuItem.getActionView();
        Utils.changeSearchTextColor(activity, searchView);
        initSearchFeatureNew();
    }


    @Override
    public void onPause() {
        super.onPause();
        wasInPause = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wasInPause = false;

    }

    @Click(R.id.tv_change_language_id)
    public void tvChangeLanguage() {
        changeLanguage();
    }

    /**
     * Base search function
     */
    private void initSearchFeatureNew() {
        AtomicReference<String> queryWords = new AtomicReference<>();
        //Launch the loader
        beginRunningTasks(1);
        RxSearchObservable.fromSearchView(searchView)
                .debounce(1500, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                //Search all words containing query word
                .switchMap((Func1<String, Observable<List<Word>>>) query -> {
                    queryWords.set(query);
                    return mainActivity.loadWords(activity, query);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordList -> {
                    //No item found
                    if (wordList.isEmpty()) {
                        llSearchBlock.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        sendQueryWordToTheServer(queryWords);
                    } else {
                        llSearchBlock.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    wordAdapter = new WordAdapter(activity, wordList, queryWords.get(), (v, position) -> {
                    });
                    recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    recyclerView.setAdapter(wordAdapter);
                    //cancel the loading
                    cancelWaitingTasks();
                });
    }


    /**
     * Reload data from the dictionary json file
     */
    private void reloadData() {
        llSearchBlock.setVisibility(View.GONE);
        index = 0;
        words = new ArrayList<>();
        wordAdapter = new WordAdapter(activity, words, itemsPerDisplay);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setHasFixedSize(true);
        beginRunningTasks(1);
        executeInBackground(mainActivity.loadSequenceWords(activity, index, size).delay(1000, TimeUnit.MILLISECONDS), wordList -> {
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(wordAdapter);
            wordAdapter.addWords(wordList);
            applyLoadMoreOnScrollListener(wordAdapter);
        });

    }

    /**
     * Applying on scroll listener to our adapter
     *
     * @param wordAdapter
     */
    public void applyLoadMoreOnScrollListener(WordAdapter wordAdapter) {
        wordAdapter.setOnLoadMoreListener(currentPage -> {
            loadNextData(wordAdapter);
        });
    }

    /**
     * load data after scroll and update our adapter
     *
     * @param wordAdapter
     */
    private void loadNextData(WordAdapter wordAdapter) {
        wordAdapter.setLoading();
        index = index + size;
        executeInBackground(mainActivity.loadSequenceWords(activity, index, size).delay(500, TimeUnit.MILLISECONDS), wordList -> {
            wordAdapter.insertData(wordList);
        });
    }

    /**
     * Send searched word to the server if not found
     *
     * @param queryWords
     */
    private void sendQueryWordToTheServer(AtomicReference<String> queryWords) {
        //Inform me
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
        }
        //Send query word to the server
        DatabaseReference myRef = database.getReference(activity.getString(R.string.fragment_home_database_root_ref)).child(activity.getString(R.string.fragment_home_database_reference));
        myRef.push().setValue(queryWords.get());
    }
}
