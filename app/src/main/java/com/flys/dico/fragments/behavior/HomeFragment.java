package com.flys.dico.fragments.behavior;

import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

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
import com.flys.dico.utils.RxSearchObservable;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


@EFragment(R.layout.fragment_home_layout)
@OptionsMenu(R.menu.menu_home)
public class HomeFragment extends AbstractFragment {

    private final String TAG = "HomeFragment";

    private static WordAdapter wordAdapter;
    private static List<Word> words;
    private static SearchView searchView;

    @ViewById(R.id.recyclerview)
    protected RecyclerView recyclerView;
    @OptionsMenuItem(R.id.search)
    protected MenuItem menuItem;
    private int itemsPerDisplay = 6;
    private static final int size = 10;
    private static int index = 0;

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
        Log.e(TAG, "initView");
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
    public void onDestroy() {
        super.onDestroy();
    }

    @OptionsItem(R.id.search)
    protected void doSearch() {
        searchView = (SearchView) menuItem.getActionView();
        Utils.changeSearchTextColor(activity, searchView);
        initSearchFeatureNew();
    }

    /**
     *
     */
    private void initSearchFeatureNew() {
        beginRunningTasks(1);
        RxSearchObservable.fromSearchView(searchView)
                .debounce(1500, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .switchMap((Func1<String, Observable<List<Word>>>) query -> mainActivity.loadWords(activity, query))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordList -> {
                    wordAdapter = new WordAdapter(activity, wordList, (v, position) -> {
                    });
                    recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    recyclerView.setAdapter(wordAdapter);
                    //cancel the loading
                    cancelWaitingTasks();
                });
    }

    /**
     * @param wordList base list of search
     * @param query    typed text to search to the dictionary
     * @return already searched word list
     */
    private Observable<List<Word>> filterWithObservable(List<Word> wordList, String query) {
        return Observable.create(subscriber -> {
            subscriber.onNext(wordList.stream().filter(notification -> notification.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    notification.getDescription().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList()));
            subscriber.onCompleted();
        });
    }

    /**
     * Reload data from the dictionary json file
     */
    private void reloadData() {
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

    public void applyLoadMoreOnScrollListener(WordAdapter wordAdapter) {
        wordAdapter.setOnLoadMoreListener(currentPage -> {
            loadNextData(wordAdapter);
        });
    }

    private void loadNextData(WordAdapter wordAdapter) {
        wordAdapter.setLoading();
        index = index + size;
        executeInBackground(mainActivity.loadSequenceWords(activity, index, size).delay(500, TimeUnit.MILLISECONDS), wordList -> {
            wordAdapter.insertData(wordList);
        });
    }


}
