package com.flys.dico.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractActivity;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.ISession;
import com.flys.dico.architecture.core.MyPager;
import com.flys.dico.architecture.core.Utils;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.flys.dico.architecture.custom.Session;
import com.flys.dico.dao.db.NotificationDao;
import com.flys.dico.dao.db.NotificationDaoImpl;
import com.flys.dico.dao.db.UserDao;
import com.flys.dico.dao.db.UserDaoImpl;
import com.flys.dico.dao.entities.User;
import com.flys.dico.dao.entities.WordToShare;
import com.flys.dico.dao.service.Dao;
import com.flys.dico.dao.service.IDao;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.dico.fragments.behavior.AboutFragment_;
import com.flys.dico.fragments.behavior.HomeFragment_;
import com.flys.dico.fragments.behavior.NotificationFragment_;
import com.flys.dico.fragments.behavior.SettingsFragment_;
import com.flys.dico.fragments.behavior.SplashScreenFragment_;
import com.flys.dico.utils.CheckNetwork;
import com.flys.dico.utils.Constants;
import com.flys.dico.utils.FacebookProfile;
import com.flys.dico.utils.FacebookUrl;
import com.flys.generictools.dao.daoException.DaoException;
import com.flys.notification.domain.Notification;
import com.flys.tools.dialog.MaterialNotificationDialog;
import com.flys.tools.domain.NotificationData;
import com.flys.tools.utils.FileUtils;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.messaging.FirebaseMessaging;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@EActivity
@OptionsMenu(R.menu.menu_main)
public class MainActivity extends AbstractActivity implements MaterialNotificationDialog.NotificationButtonOnclickListeneer {

    /*===============================================================================
     * Static variables
     ===============================================================================*/
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 123;

    @OptionsMenuItem(R.id.connexion)
    protected MenuItem connexion;

    @ViewById(R.id.container)
    protected MyPager myPager;

    // couche [DAO]
    @Bean(Dao.class)
    protected IDao dao;

    @Bean(UserDaoImpl.class)
    protected UserDao userDao;

    @Bean(NotificationDaoImpl.class)
    protected NotificationDao notificationDao;
    // session
    private Session session;
    //Notification
    private MaterialNotificationDialog dialog;

    private ObjectMapper objectMapper;
    // Register Callback - Call this in your app start!
    private CheckNetwork network;

    //Action mode
    private static ActionMode actionMode;

    // méthodes classe parent -----------------------
    @Override
    protected void onCreateActivity() {
        // log
        if (IS_DEBUG_ENABLED) {
            Log.d(className, "onCreateActivity");
        }
        // session
        this.session = (Session) super.session;
        getSupportActionBar().hide();
        bottomNavigationView.setVisibility(View.GONE);
        if (!session.isSubscribed()) {
            firebaseSubscription();
        }
        //Initializations
        objectMapper = new ObjectMapper();
        //Check network
        network = new CheckNetwork(getApplicationContext());
        network.registerNetworkCallback();

        //If we have fcm pushed notification in course
        //Subscription on firebase to receive notifications
        handleNotifications(getIntent());

        initializeAds();

    }

    /**
     * AdMob initialization
     */
    private void initializeAds() {
        MobileAds.initialize(this, initializationStatus -> {
        });
    }


    @Override
    protected void onResumeActivity() {
        //Update view if user has been connected
        User user = updateProfile();
        if (user != null && user.getType() != null) {
            updateUserConnectedProfile(user);
        }

    }


    private void showActionMode(@NotNull Set<WordToShare> wordToShares) {
        if (wordToShares.isEmpty()) {
            closeActionModeShareWords();
            return;
        }
        actionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_home_action_mode, menu);
                //Show selected elements
                if (wordToShares.size() == 1) {
                    mode.setTitle(wordToShares.size() + " item selected");
                } else {
                    mode.setTitle(wordToShares.size() + " items selected");
                }

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.menu_home_share_word_id) {
                    StringBuilder message = new StringBuilder();
                    wordToShares.forEach(wordToShare -> message.append(HtmlCompat.fromHtml(wordToShare.getWord().getTitle().concat(": ").concat(wordToShare.getWord().getDescription()).concat("\n"), HtmlCompat.FROM_HTML_MODE_LEGACY)));
                    com.flys.tools.utils.Utils.shareText(MainActivity.this, getString(R.string.app_name), message.toString().concat("\n").concat(getString(R.string.app_google_play_store_url)), getString(R.string.activity_abstract_recommend_app));
                    actionMode.finish();
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
            }
        });
    }

    private void showActionMode(@NotNull WordAdapter wordAdapter, @NotNull Set<WordToShare> wordToShares) {
        if (wordToShares.isEmpty()) {
            closeActionModeShareWords();
            return;
        }
        actionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_home_action_mode, menu);
                //Show selected elements
                if (wordToShares.size() == 1) {
                    mode.setTitle(wordToShares.size() + " item selected");
                } else {
                    mode.setTitle(wordToShares.size() + " items selected");
                }

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.menu_home_share_word_id) {
                    StringBuilder message = new StringBuilder();
                    wordToShares.forEach(wordToShare -> message.append(HtmlCompat.fromHtml(wordToShare.getWord().getTitle().concat(": ").concat(wordToShare.getWord().getDescription()).concat("\n"), HtmlCompat.FROM_HTML_MODE_LEGACY)));
                    com.flys.tools.utils.Utils.shareText(MainActivity.this, getString(R.string.app_name), message.toString().concat("\n").concat(getString(R.string.app_google_play_store_url)), getString(R.string.activity_abstract_recommend_app));
                    actionMode.finish();
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                wordAdapter.notifyDataSetChanged();
                actionMode = null;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Handle pushed notification if exist
        handleNotifications(intent);
    }

    @Override
    protected IDao getDao() {
        return dao;
    }

    @Override
    protected AbstractFragment[] getFragments() {
        return new AbstractFragment[]{
                new SplashScreenFragment_(), new HomeFragment_(), new NotificationFragment_(), new AboutFragment_(),
                new SettingsFragment_()
        };
    }

    @Override
    protected CharSequence getFragmentTitle(int position) {
        return null;
    }

    @Override
    protected void navigateOnTabSelected(int position) {
        //navigation par onglets - définir la vue à afficher lorsque l'onglet n° [position] est sélectionné
    }

    @Override
    protected int getFirstView() {
        //définir le n° de la première vue (fragment) à afficher
        return SPLASHSCREEN_FRAGMENT;
    }

    @Override
    protected void disconnect() {
        disconnectHandle();
    }

    @Override
    public void onBackPressed() {
        onBackPressedHandle();
    }

    /**
     *
     */
    @OptionsItem(R.id.connexion)
    public void connexion() {
        signIn();
    }

    @OptionsItem(R.id.menu_profil)
    public void showProfile() {
        drawerLayout.openDrawer(Gravity.LEFT, true);
    }

    @OptionsItem(R.id.settings)
    public void showSettings() {
        navigateToView(SETTINGS_FRAGMENT, ISession.Action.SUBMIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onResultActivityHandle(requestCode, resultCode, data);

    }


    @Override
    public Observable<byte[]> downloadUrl(String url) {
        return dao.downloadUrl(url);
    }

    @Override
    public Observable<byte[]> downloadFacebookImage(String url, String type) {
        return dao.downloadFacebookImage(url, type);
    }

    @Override
    public Observable<List<Word>> loadDictionaryDataFromAssets(Context context) {
        //Load dictionary data from assets dictory
        return dao.loadDictionaryDataFromAssets(context);
    }

    @Override
    public Observable<Void> reloadData(List<Word> words, WordAdapter adapter, RecyclerView recyclerView) {
        return dao.reloadData(words, adapter, recyclerView);
    }

    @Override
    public Observable<List<Notification>> loadNotificationsFromDatabase() {
        return dao.loadNotificationsFromDatabase();
    }

    @Override
    public Observable<byte[]> downloadFacebookImage(String url) {
        return dao.downloadFacebookImage(url);
    }

    @Override
    public Observable<byte[]> downloadFacebookProfileImage(final String baseUrl, final String params) {
        return dao.downloadFacebookProfileImage(baseUrl, params);
    }

    @Override
    public Observable<byte[]> downloadFacebookProfileImage(String baseUrl) {
        return dao.downloadFacebookProfileImage(baseUrl);
    }

    @Override
    public Observable<List<Word>> loadSequenceWords(Context context, int index, int size) {
        return dao.loadSequenceWords(context, index, size);
    }

    @Override
    public Observable<List<Word>> loadWords(Context context, String query) {
        return dao.loadWords(context, query);
    }

    @Override
    public Observable<byte[]> downloadFacebookProfileImage(String baseUrl, String ext, String params) {
        return dao.downloadFacebookProfileImage(baseUrl, ext, params);
    }

    @Override
    public Observable<List<Notification>> loadNotificationsFromDatabase(String property, Object value) {
        return dao.loadNotificationsFromDatabase(property, value);
    }

    @Override
    public User updateProfile() {
        return getConnectedUserProfile();
    }

    @Override
    public void activateMainButtonMenu(int itemMenuId) {
        bottomNavigationView.setSelectedItemId(itemMenuId);
    }

    @Override
    public void updateNotificationNumber(int number) {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.bottom_menu_me);
        badgeDrawable.setBackgroundColor(getColor(R.color.blue_500));
        badgeDrawable.setNumber(number);
        badgeDrawable.setMaxCharacterCount(2);
        badgeDrawable.setVisible(true);
    }

    @Override
    public void clearNotification() {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.bottom_menu_me);
        badgeDrawable.setVisible(false);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        loadNotificationsFromDatabase("seen", false)
                .subscribe(notifications -> {
                    if (notifications != null && !notifications.isEmpty()) {
                        notifications.forEach(notification -> {
                            notification.setSeen(true);
                            notificationDao.update(notification);
                        });
                    }
                });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (updateProfile() != null) {
            connexion.setVisible(false);
        } else {
            connexion.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void okButtonAction(DialogInterface dialogInterface, int i) {
        super.onBackPressed();
    }

    @Override
    public void noButtonAction(DialogInterface dialogInterface, int i) {
        this.dialog.dismiss();
    }

    @Override
    public void popupSnackbarForCompleteUpdate(AppUpdateManager appUpdateManager) {
        Snackbar snackbar =
                Snackbar.make(
                        myPager,
                        getString(R.string.main_activity_completed_download),
                        Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.main_activity_download_completed_restart, view -> {
            appUpdateManager.completeUpdate();
        });
        bottomNavigationView.setVisibility(View.GONE);
        snackbar.setAnchorView(bottomNavigationView);
        snackbar.setActionTextColor(getColor(R.color.blue_500));
        snackbar.show();
    }

    @Override
    public void scrollUp() {
        bottomNavigationView.setVisibility(View.GONE);
    }

    @Override
    public void scrollDown() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    @Override
    public void shareWords(Word word) {
        //showActionMode(word);
    }

    @Override
    public void shareWords(Set<WordToShare> wordsToShare) {
        showActionMode(wordsToShare);
    }

    @Override
    public void closeActionModeShareWords() {
        if (actionMode != null) {
            //Close action mode share words
            actionMode.finish();
            //clear the session
            session.setWordToShares(null);
        }
    }

    @Override
    public void shareWords(WordAdapter wordAdapter, Set<WordToShare> wordsToShare) {
        showActionMode(wordAdapter, wordsToShare);
    }



    /*------------------------------------------------------------------------------------------------
    -------------------------         BEGIN ALL HANDLES ----------------------------------------------
    ------------------------------------------------------------------------------------------------ */

    /**
     * @param intent
     */
    private void handleNotifications(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(Constants.NOTIFICATION)) {
            Notification notification = (Notification) bundle.getSerializable(Constants.NOTIFICATION);
            if (notification != null) {
                getSupportActionBar().show();
                activateMainButtonMenu(R.id.bottom_menu_me);
                navigateToView(NOTIFICATION_FRAGMENT, ISession.Action.SUBMIT);

            } else {
                Log.e(getClass().getSimpleName(), " onNewIntent(): notification null ");
            }

        } else {
            Log.e(getClass().getSimpleName(), " onNewIntent(): bundle null ");
        }
        this.setIntent(intent);
    }


    /**
     * Update the user profile
     *
     * @param user
     */
    void updateUserConnectedProfile(User user) {
        View headerNavView = navigationView.getHeaderView(0);
        ShapeableImageView profile = headerNavView.findViewById(R.id.profile_image);
        TextView title = headerNavView.findViewById(R.id.profile_user_name);
        TextView mail = headerNavView.findViewById(R.id.profile_user_email_address);
        MenuItem disconnect = navigationView.getMenu().findItem(R.id.menu_deconnexion);
        LinearLayout userInfo=headerNavView.findViewById(R.id.profile_user_info);
        //Si l'utilisateur est connecte?
        if (user != null && (user.getEmail() != null || user.getPhone() != null)) {
            disconnect.setVisible(true);
            switch (user.getType()) {
                case GOOGLE:
                case FACEBOOK:
                    title.setText(user.getNom());
                    mail.setText(user.getEmail());
                    profile.setImageDrawable(user.getImageUrl() != null ? FileUtils.loadImageFromStorage("glearning", user.getNom() + ".png", DApplicationContext.getContext()) : getDrawable(R.drawable.ic_outline_account_circle_24));
                    break;
                case MAIL:
                    title.setText(user.getNom());
                    mail.setText(user.getEmail());
                    profile.setImageDrawable(getDrawable(R.drawable.ic_outline_account_circle_24));
                    break;
                case PHONE:
                    title.setText(user.getNom());
                    mail.setText(user.getPhone());
                    profile.setImageDrawable(getDrawable(R.drawable.ic_outline_account_circle_24));
                    break;
            }
            profile.setStrokeColor(getColorStateList(R.color.color_secondary));
            profile.setStrokeWidth((float) 0.5);
            profile.setOnClickListener(null);
            userInfo.setVisibility(View.VISIBLE);
        } else {
            userInfo.setVisibility(View.GONE);
            profile.setStrokeColor(null);
            profile.setStrokeWidth(0);
            disconnect.setVisible(false);
            profile.setImageDrawable(getDrawable(R.drawable.ic_outline_account_circle_24));
            profile.setOnClickListener(v -> {
                signIn();
            });
        }
    }


    /**
     * @param user
     */
    private void downloadFacebookUserProfileImage(User user) {
        //
        user.setType(User.Type.FACEBOOK);
        Bundle params = new Bundle();
        params.putString("fields", "id, name, birthday,hometown,email,gender,cover,picture.width(640).height(640)");
        new GraphRequest(AccessToken.getCurrentAccessToken(), "me", params, HttpMethod.GET,
                response -> {
                    if (response != null) {
                        try {
                            FacebookProfile facebookProfile = objectMapper.readValue(response.getRawResponse(), new TypeReference<FacebookProfile>() {

                            });

                            //Collect user information
                            user.setNom(facebookProfile.getName());
                            user.setImageUrl(facebookProfile.getPicture().getData().getUrl());
                            user.setEmail(facebookProfile.getEmail());
                            //Updating connected user
                            session.setUser(userDao.update(user));
                            //Update profile
                            updateUserConnectedProfile(user);
                            FacebookUrl facebookUrl = facebookProfileImageUrlSplit(facebookProfile.getPicture().getData().getUrl());
                            beginWaiting();
                            downloadFacebookProfileImage(facebookUrl.getBaseUrl(), facebookUrl.getExt(), facebookUrl.getHash())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(bytes -> {
                                        //save downloaded image to the internal storage
                                        FileUtils.saveToInternalStorage(bytes, "glearning", user.getNom() + ".png", this);
                                        //Update profile
                                        updateUserConnectedProfile(user);
                                        //Cancel waiting
                                        cancelWaiting();
                                        //launch dialog fragment to show connection details
                                        showDialogImage(bytes, user);
                                    }, error -> {
                                        // on affiche les messages de la pile d'exceptions du Throwable th
                                        new AlertDialog.Builder(this).setTitle("Ooops !").setMessage(getString(R.string.activity_main_check_your_connection_and_try_again)).setNeutralButton(getString(R.string.activity_main_button_close), null).show();
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).executeAsync();
    }

    /**
     * @param bytes
     * @param user
     */
    private void showDialogImage(byte[] bytes, User user) {
        if (user != null) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
            dialog.setContentView(R.layout.dialog_contact_image);
            TextView name = dialog.findViewById(R.id.name);
            TextView email = dialog.findViewById(R.id.email_or_number);
            ImageView smallImage = dialog.findViewById(R.id.small_image);
            ImageView image = dialog.findViewById(R.id.large_image);
            switch (user.getType()) {
                case GOOGLE:
                case MAIL:
                case FACEBOOK:
                    email.setText(user.getEmail());
                    name.setText(user.getNom());
                    break;
                case PHONE:
                    email.setText(user.getPhone());
                    name.setText(user.getNom());
                    break;
            }
            if (bytes != null) {
                smallImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCancelable(true);
            (dialog.findViewById(R.id.bt_close)).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

    }

    /**
     * Return user informations switch provider type
     *
     * @param firebaseUser
     * @return
     */
    public void getProviderData(FirebaseUser firebaseUser) {
        User user = new User();
        for (UserInfo profile : firebaseUser.getProviderData()) {
            //switch provider
            switch (profile.getProviderId()) {
                //Connected with google account
                case Constants.SOCIAL_MEDIA_GOOGLE_COM:
                    //Connection using google
                    googleConnect(firebaseUser, user, profile);
                    break;
                //Connected with facebook account
                case Constants.SOCIAL_MEDIA_FACEBOOK_COM:
                    //Connection using facebook
                    facebookConnect(user);
                    break;
                //connected with phone number
                case Constants.SOCIAL_MEDIA_PHONE:
                    //Connection using phone number
                    phoneNumberConnect(user, profile);
                    break;
                //connected with an email address
                case Constants.SOCIAL_MEDIA_PASSWORD:
                    //Connection using email address
                    mailConnect(user, profile);
                    break;
                default:
                    break;
            }

        }
        try {
            Log.e(getClass().getSimpleName(), "Mainactivity user before save : " + user);
            if (user != null) {
                userDao.save(user);
            }
        } catch (DaoException e) {
            Log.e(getClass().getSimpleName(), "Dao Exception!", e);
        }
    }

    /**
     * @param user
     * @param profile
     */
    private void mailConnect(User user, UserInfo profile) {
        user.setType(User.Type.MAIL);
        user.setEmail(profile.getEmail());
        showDialogImage(null, user);
    }

    /**
     * @param user
     * @param profile
     */
    private void phoneNumberConnect(User user, UserInfo profile) {
        user.setType(User.Type.PHONE);
        user.setPhone(profile.getPhoneNumber());
        showDialogImage(null, user);
    }

    /**
     * @param user
     */
    private void facebookConnect(User user) {
        if (Constants.isNetworkConnected) {
            downloadFacebookUserProfileImage(user);
        } else {
            Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.oops_connection_issue_msg));
        }
    }

    /**
     * @param firebaseUser
     * @param user
     * @param profile
     */
    private void googleConnect(FirebaseUser firebaseUser, User user, UserInfo profile) {
        user.setType(User.Type.GOOGLE);
        user.setNom(firebaseUser.getDisplayName());
        user.setEmail(profile.getEmail());
        user.setImageUrl(profile.getPhotoUrl().toString().replace("s96-c", "s400-c"));
        if (Constants.isNetworkConnected) {
            //Launch the loader
            beginWaiting();
            downloadUrl(user.getImageUrl())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        //Save the user avator in internal storage
                        FileUtils.saveToInternalStorage(bytes, "glearning", user.getNom() + ".png", this);
                        //Update profile
                        updateUserConnectedProfile(user);
                        //cancel the loading
                        cancelWaiting();
                        //Show user dialog with user resume
                        showDialogImage(bytes, user);
                    }, error -> {
                        //on affiche les messages de la pile d'exceptions du Throwable th
                        new AlertDialog.Builder(this).setTitle("Ooops !").setMessage(R.string.activity_main_check_your_connection_and_try_again).setNeutralButton(R.string.activity_main_button_close, null).show();
                    });
        } else {
            Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.activity_main_network_issue));
        }
    }

    /**
     * Subscribe to firebase channel
     */
    private void firebaseSubscription() {
        Log.e(TAG, "subscription to the channel for notification" + getString(R.string.firebase_subscription));
        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.firebase_subscription))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        session.setSubscribed(true);
                        Log.e(TAG, "subscription to the channel for notification is successfully");
                    }
                });
    }


    /**
     *
     */
    private void Disconnection() {
        MaterialNotificationDialog notificationDialog = new MaterialNotificationDialog(MainActivity.this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_thanks_msg) + (session.getUser().getNom() != null ? session.getUser().getNom() : "") + getString(R.string.activity_main_see_you_soon), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_cancel), getDrawable(R.drawable.logo), R.style.customMaterialAlertEditDialog), new MaterialNotificationDialog.NotificationButtonOnclickListeneer() {
            @Override
            public void okButtonAction(DialogInterface dialogInterface, int i) {
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                try {
                                    //if disconnect, clear the session and delete the user from de database
                                    userDao.delete(session.getUser());
                                    session.setUser(null);
                                    onPrepareOptionsMenu(null);
                                    updateUserConnectedProfile(null);
                                    dialogInterface.dismiss();
                                } catch (DaoException e) {
                                    Log.e(getClass().getSimpleName(), "Dao Exception!", e);
                                }
                            }
                            if (task.isCanceled()) {
                                Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.activity_main_disconnect_canceled));
                            }
                        });
            }

            @Override
            public void noButtonAction(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        notificationDialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
    }

    /**
     * Authentication using firebase: login
     */
    private void signIn() {
        // Choose authentication providers
        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                .Builder(R.layout.fragment_login)
                .setGoogleButtonId(R.id.connexion_google_id)
                .setEmailButtonId(R.id.connexion_mail_id)
                .setFacebookButtonId(R.id.connexion_facebook_id)
                .setPhoneButtonId(R.id.connexion_phone_id)
                .build();

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.FacebookBuilder().build()
        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setAuthMethodPickerLayout(customLayout)
                        .setLogo(R.drawable.logo)      // Set logo drawable
                       .setTheme(R.style.AppTheme_NoActionBar)      // Set theme
                        /*.setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")*/
                        .build(),
                RC_SIGN_IN);
    }

    /**
     * @return
     */
    private User getConnectedUserProfile() {
        //Check if the session content user
        if (session.getUser() != null) {
            return session.getUser();
        } else {
            //Check in the database if the user was connected
            try {
                List<User> users = userDao.getAll();
                if (users != null && !users.isEmpty()) {
                    session.setUser(users.get(0));
                }
            } catch (DaoException e) {
                Log.e(getClass().getSimpleName(), "Dao Exception!", e);
            }
        }
        return session.getUser();
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    private void onResultActivityHandle(int requestCode, int resultCode, Intent data) {
        //Call for authentication
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                //Mise à jour des informations de l'utilisateur dans la session
                getProviderData(FirebaseAuth.getInstance().getCurrentUser());
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Log.e(getClass().getSimpleName(), "onActivityResult: sign_in_cancelled");
                    Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.activity_main_connection_canceld));
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Log.e(getClass().getSimpleName(), "onActivityResult: no_internet_connection");
                    Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.activity_main_network_issue));
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.e(getClass().getSimpleName(), "onActivityResult: unknown_error");
                    Utils.showErrorMessage(MainActivity.this, findViewById(R.id.main_content), getColor(R.color.blue_500), getString(R.string.activity_main_try_again_mdg));
                    return;
                }
            }
        }
    }

    /**
     * Handle on back pressed
     */
    private void onBackPressedHandle() {
        drawerLayout.closeDrawers();
        if (mViewPager.getCurrentItem() == HOME_FRAGMENT) {
            this.dialog = new MaterialNotificationDialog(this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_do_you_want_to_leave_app), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_no_msg), getDrawable(R.drawable.logo), R.style.customMaterialAlertEditDialog), this);
            this.dialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
        } else {
            // Otherwise, select the previous step.
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
    }

    /**
     * handle disconnection
     */
    private void disconnectHandle() {
        MaterialNotificationDialog dialog = new MaterialNotificationDialog(this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_do_you_want_to_disconnect), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_no_msg), getDrawable(R.drawable.logo), R.style.customMaterialAlertEditDialog), new MaterialNotificationDialog.NotificationButtonOnclickListeneer() {
            @Override
            public void okButtonAction(DialogInterface dialogInterface, int i) {
                // Disconnection
                Disconnection();
            }

            @Override
            public void noButtonAction(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
    }

    private FacebookUrl facebookProfileImageUrlSplit(String url) {
        FacebookUrl facebookUrl = buildFacebookProfileImageUrlFromParameters(url);
        facebookUrl.setBaseUrl(url.split("\\?")[0]);
        return facebookUrl;
    }

    /**
     * @param url
     * @return
     */
    private FacebookUrl buildFacebookProfileImageUrlFromParameters(String url) {
        Optional<Uri> uriOptional = Optional.ofNullable(Uri.parse(url));
        AtomicReference<FacebookUrl> facebookUrl = new AtomicReference<>();
        uriOptional.ifPresent(uri -> {
            FacebookUrl facebookUrlLocal = new FacebookUrl(null, uri.getQueryParameter("hash"), uri.getQueryParameter("ext"));
            facebookUrl.set(facebookUrlLocal);
        });
        return facebookUrl.get();
    }

}
