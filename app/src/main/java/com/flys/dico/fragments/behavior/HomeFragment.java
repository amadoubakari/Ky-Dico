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

import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.Utils;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.dico.utils.Constants;
import com.flys.dico.utils.FacebookUrl;
import com.flys.dico.utils.RxSearchObservable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.ActivityResult;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.listener.StateUpdatedListener;
import com.google.android.play.core.tasks.Task;
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
public class HomeFragment extends AbstractFragment implements StateUpdatedListener<InstallState> {

    private static final String TAG = "HomeFragment";
    private static final int PLAY_STORE_UPDATE_REQUEST_CODE = 124;
    private static final int size = 10;

    private static WordAdapter wordAdapter;
    private static List<Word> words;
    private static SearchView searchView;
    private static int index = 0;
    private static boolean wasInPause = false;
    private static boolean askedForUpdate = false;
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

    @ViewById(R.id.home_layout_container_id)
    protected RelativeLayout llHomeContainer;


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
        appUpdateManager = AppUpdateManagerFactory.create(activity);
        if (previousState == null) {
            reloadData();
        }
    }

    @Override
    protected void initView(CoreState previousState) {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity);
        }
    }

    @Override
    protected void updateOnSubmit(CoreState previousState) {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity);
        }
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
        //Check if there is updates already downloaded
        if (appUpdateManager != null) {
            checkIfUpdatesDownloaded();
        }

        //network available and never ask update before?
        if (Constants.isNetworkConnected && !askedForUpdate) {
            checkForUpdates();
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
        askedForUpdate = false;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Call for updating application state
        if (requestCode == PLAY_STORE_UPDATE_REQUEST_CODE) {
            if (resultCode != activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: " + resultCode);
                // If the update is cancelled or fails,
                // you can request to start the update again.
                cancelRunningTasks();
            } else if (resultCode == activity.RESULT_CANCELED) {
                cancelRunningTasks();
            } else if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                cancelRunningTasks();
                Log.e(TAG, "Some other error prevented either the user from providing consent or the update to proceed. " + resultCode);
            }
        }
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
                        llHomeContainer.setBackgroundColor(activity.getColor(R.color.green_200));
                        llSearchBlock.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        sendQueryWordToTheServer(queryWords,activity.getString(R.string.fragment_home_database_root_ref),activity.getString(R.string.fragment_home_database_reference));
                    } else {
                        llHomeContainer.setBackgroundColor(activity.getColor(R.color.white));
                        llSearchBlock.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        if(!queryWords.get().isEmpty()){
                            sendQueryWordToTheServer(queryWords,activity.getString(R.string.fragment_home_database_tops),activity.getString(R.string.fragment_home_database_reference));
                        }
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


    /*=================================================================================
    ================     Treatment ====================================================
    ================================================================================= */

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
        DatabaseReference databaseReference = database.getReference(activity.getString(R.string.fragment_home_database_root_ref)).child(activity.getString(R.string.fragment_home_database_reference));
        databaseReference.push().setValue(queryWords.get());
    }

    private void sendQueryWordToTheServer(AtomicReference<String> queryWords, String reference, String child) {
        //Inform me
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
        }
        //Send query word to the server
        DatabaseReference databaseReference = database.getReference(reference).child(child);
        databaseReference.push().setValue(queryWords.get());
    }


    /**
     * Callback triggered whenever the state has changed.
     *
     * @param state
     */
    @Override
    public void onStateUpdate(InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            mainActivity.popupSnackbarForCompleteUpdate(appUpdateManager);
        }
    }

    /**
     *
     */
    public void checkUpdatesAvailable() {
        // Create a listener to track request state updates.
        InstallStateUpdatedListener listener = state -> {
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                long bytesDownloaded = state.bytesDownloaded();
                long totalBytesToDownload = state.totalBytesToDownload();
                // Implement progress bar.
                //launch the waiting loader
                beginRunningTasks(1);
            }
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                //cancel loading waiting
                cancelRunningTasks();
                // When status updates are no longer needed, unregister the listener.
                appUpdateManager.unregisterListener(this::onStateUpdate);
                //Launch the installation
                mainActivity.popupSnackbarForCompleteUpdate(appUpdateManager);
            }
        };

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                try {
                    // Before starting an update, register a listener for updates.
                    appUpdateManager.registerListener(listener);
                    //Start download updates
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.FLEXIBLE,
                            // The current activity making the update request.
                            activity,
                            // Include a request code to later monitor this update request.
                            PLAY_STORE_UPDATE_REQUEST_CODE);
                    //request for updating is done
                    askedForUpdate = true;
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "No Update available");
            }
        });
    }

    /**
     *
     */
    private void checkIfUpdatesDownloaded() {
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        mainActivity.popupSnackbarForCompleteUpdate(appUpdateManager);
                    }
                });
    }

    /**
     *
     */
    private void checkForUpdates() {
        Observable<Boolean> observable = Observable.create(subscriber -> {
            subscriber.onNext(Boolean.TRUE);
            subscriber.onCompleted();
        });
        observable
                .delay(10, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    checkUpdatesAvailable();
                });
    }
}
