package com.flys.dico.fragments.behavior;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.dao.entities.Dictionnaire;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.generictools.tools.Utils;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


@EFragment(R.layout.fragment_home_layout)
@OptionsMenu(R.menu.menu_home)
public class HomeFragment extends AbstractFragment {

    @ViewById(R.id.recyclerview)
    protected RecyclerView recyclerView;
    @OptionsMenuItem(R.id.search)
    protected MenuItem menuItem;
    private WordAdapter wordAdapter;
    private List<Word> words;
    protected SearchView searchView;
    //Dictionnary data
    private Dictionnaire dictionnaire;
    //Asynchronic job
    private Observable observable;
    //Json mapper to object
    ObjectMapper mapper;

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
        mapper = new ObjectMapper();
        //words = new ArrayList<>();
        try {
            //Recuperation à partir du la session
            words = session.getWords();
            //if session is empty
            if (words == null || words.isEmpty()) {
                //Read data from json file
                words = mapper.readValue(activity.getAssets().open("dictionnaire.json"), Dictionnaire.class).getWords();
                session.setWords(words);
            }
            //words.addAll(dictionnaire.getWords());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //


    }

    @Override
    protected void initView(CoreState previousState) {
        wordAdapter = new WordAdapter(activity, words, (v, position) -> {
            Toast.makeText(activity, "Clicked " + position, Toast.LENGTH_SHORT).show();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(wordAdapter);

    }

    @Override
    protected void updateOnSubmit(CoreState previousState) {

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

    @OptionsItem(R.id.search)
    protected void doSearch() {
        // on récupère le client choisi
        searchView = (SearchView) menuItem.getActionView();
        changeSearchTextColor(searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                menuItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //whether we have one caracter at least
                if (searchView.getQuery().length() > 2) {
                    wordAdapter.setFilter(filter(words, newText));
                } else {
                    wordAdapter.setFilter(words);
                }
                return true;
            }
        });
    }

    /**
     * @param view
     */
    private void changeSearchTextColor(View view) {
        if (view != null) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(ContextCompat.getColor(activity, R.color.black));
                ((TextView) view).setTextSize(14);
                view.setBackgroundColor(ContextCompat.getColor(activity, R.color.white));
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    changeSearchTextColor(viewGroup.getChildAt(i));
                }
            }
        }
    }

    /**
     * @param words
     * @param query
     * @return
     */
    private List<Word> filter(List<Word> words, String query) {
        return words.stream().filter(notification -> notification.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                notification.getDescription().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());

    }

    private Observable<Void> loadData() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    //Recuperation à partir du la session
                    words = session.getWords();
                    //if session is empty
                    if (words == null || words.isEmpty()) {
                        //Read data from json file
                        words = mapper.readValue(activity.getAssets().open("dictionnaire.json"), Dictionnaire.class).getWords();
                        session.setWords(words);
                    }
                    //words.addAll(dictionnaire.getWords());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
