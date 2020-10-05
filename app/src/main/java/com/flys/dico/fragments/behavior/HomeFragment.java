package com.flys.dico.fragments.behavior;

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

    @ViewById(R.id.recyclerview)
    protected RecyclerView recyclerView;
    @OptionsMenuItem(R.id.search)
    protected MenuItem menuItem;
    private WordAdapter wordAdapter;
    private static List<Word> words;
    private static SearchView searchView;
    private final String TAG = "HomeFragment";

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
        ((AppCompatActivity) mainActivity).getSupportActionBar().show();
        if (previousState == null) {
            reloadData();
        }
    }

    @Override
    protected void initView(CoreState previousState) {
    }

    @Override
    protected void updateOnSubmit(CoreState previousState) {
        if (recyclerView.getAdapter() == null) {
            reloadData();
        }
    }

    @Override
    protected void updateOnRestore(CoreState previousState) {
    }

    @Override
    protected void notifyEndOfUpdates() {

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
        initSearchFeatureNew(words);
    }

    /**
     * @param wordss
     */
    private void initSearchFeatureNew(final List<Word> wordss) {
        RxSearchObservable.fromSearchView(searchView)
                .debounce(1500, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .switchMap((Func1<String, Observable<List<Word>>>) s -> filterWithObservable(wordss, s))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordssss -> {
                    wordAdapter = new WordAdapter(activity, wordssss, (v, position) -> {
                    });
                    recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    recyclerView.setAdapter(wordAdapter);
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
        recyclerView.setVisibility(View.VISIBLE);
        words = new ArrayList<>();
        wordAdapter = new WordAdapter(words, activity);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(wordAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        beginRunningTasks(1);
        executeInBackground(mainActivity.loadDictionnaryDataFromAssets(activity).delay(1000, TimeUnit.MILLISECONDS), wordList -> {
            wordAdapter.addWords(wordList);
        });
    }
}
