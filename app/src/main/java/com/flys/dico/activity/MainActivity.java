package com.flys.dico.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractActivity;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.ISession;
import com.flys.dico.architecture.core.Utils;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.flys.dico.architecture.custom.Session;
import com.flys.dico.dao.db.NotificationDao;
import com.flys.dico.dao.db.NotificationDaoImpl;
import com.flys.dico.dao.db.UserDao;
import com.flys.dico.dao.db.UserDaoImpl;
import com.flys.dico.dao.entities.User;
import com.flys.dico.dao.service.Dao;
import com.flys.dico.dao.service.IDao;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.dico.fragments.behavior.AboutFragment_;
import com.flys.dico.fragments.behavior.HomeFragment_;
import com.flys.dico.fragments.behavior.NotificationFragment_;
import com.flys.dico.fragments.behavior.SettingsFragment_;
import com.flys.dico.fragments.behavior.SplashScreenFragment_;
import com.flys.dico.utils.Constants;
import com.flys.generictools.dao.daoException.DaoException;
import com.flys.notification.domain.Notification;
import com.flys.tools.dialog.MaterialNotificationDialog;
import com.flys.tools.domain.NotificationData;
import com.flys.tools.utils.FileUtils;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.messaging.FirebaseMessaging;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@EActivity
@OptionsMenu(R.menu.menu_main)
public class MainActivity extends AbstractActivity implements MaterialNotificationDialog.NotificationButtonOnclickListeneer {

    /*===============================================================================
     * Static variables
     ===============================================================================*/
    public static final String TAG = GraphRequest.class.getSimpleName();

    private static final int RC_SIGN_IN = 123;

    @OptionsMenuItem(R.id.connexion)
    MenuItem connexion;

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
        //If we have fcm pushed notification in course
        handleNotifications(getIntent());
        //Subscription on firebase to receive notifications
        if (!session.isSubscribed()) {
            FirebaseMessaging.getInstance().subscribeToTopic("dico")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            session.setSubscribed(true);
                            Log.e(TAG, "subscription to the channel for notification is successfully");
                        }
                    });
        }

        //Initializations
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void onResumeActivity() {
        //Update view if user has been connected
        if (updateProfile() != null && updateProfile().getType() != null) {
            updateUserConnectedProfile(updateProfile());
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Handle pushed notification if exist
        handleNotifications(intent);
    }

    /**
     * @param intent
     */
    private void handleNotifications(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(Constants.NOTIFICATION)) {
            Notification notification = (Notification) bundle.getSerializable(Constants.NOTIFICATION);
            if (notification != null) {
                try {
                    notification.setDate(new Date());
                    notificationDao.save(notification);
                    getSupportActionBar().show();
                    updateNotificationNumber(1);
                    activateMainButtonMenu(R.id.bottom_menu_me);
                    navigateToView(NOTIFICATION_FRAGMENT, ISession.Action.SUBMIT);
                } catch (DaoException e) {
                    Log.e(getClass().getSimpleName(), "Dao Exception!", e);
                }

            } else {
                Log.e(getClass().getSimpleName(), " onNewIntent(): notification null ");
            }

        } else {
            Log.e(getClass().getSimpleName(), " onNewIntent(): bundle null ");
        }
        this.setIntent(intent);
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
        MaterialNotificationDialog dialog = new MaterialNotificationDialog(this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_do_you_want_to_disconnect), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_no_msg), getDrawable(R.drawable.logo), R.style.Theme_MaterialComponents_DayNight_Dialog_Alert), new MaterialNotificationDialog.NotificationButtonOnclickListeneer() {
            @Override
            public void okButtonAction(DialogInterface dialogInterface, int i) {
                // Disconnection
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                MaterialNotificationDialog notificationDialog = new MaterialNotificationDialog(MainActivity.this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_thanks_msg) + (session.getUser().getNom() != null ? session.getUser().getNom() : "") + getString(R.string.activity_main_see_you_soon), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_cancel), getDrawable(R.drawable.logo), R.style.Theme_MaterialComponents_DayNight_Dialog_Alert), new MaterialNotificationDialog.NotificationButtonOnclickListeneer() {
                                    @Override
                                    public void okButtonAction(DialogInterface dialogInterface, int i) {
                                        try {
                                            //if disconnect, clear the session and delete the user from de database
                                            userDao.delete(session.getUser());
                                            session.setUser(null);
                                            dialogInterface.dismiss();
                                            onPrepareOptionsMenu(null);
                                            updateUserConnectedProfile(null);
                                        } catch (DaoException e) {
                                            Log.e(getClass().getSimpleName(), "Dao Exception!", e);
                                        }
                                    }

                                    @Override
                                    public void noButtonAction(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                                notificationDialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
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
        dialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
    }

    @Override
    public void onBackPressed() {
        drawerLayout.closeDrawers();
        if (mViewPager.getCurrentItem() == HOME_FRAGMENT) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            this.dialog = new MaterialNotificationDialog(this, new NotificationData(getString(R.string.app_name), getString(R.string.activity_main_do_you_want_to_leave_app), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_no_msg), getDrawable(R.drawable.logo), R.style.Theme_MaterialComponents_DayNight_Dialog_Alert), this);
            this.dialog.show(getSupportFragmentManager(), "material_notification_alert_dialog");
        } else {
            // Otherwise, select the previous step.
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
    }


    /**
     *
     */
    @OptionsItem(R.id.connexion)
    public void connexion() {
        createSignInIntent();
    }

    @OptionsItem(R.id.menu_profil)
    public void showProfile() {
        drawerLayout.openDrawer(Gravity.LEFT, true);
    }

    @OptionsItem(R.id.settings)
    public void showSettings() {
        navigateToView(SETTINGS_FRAGMENT, ISession.Action.SUBMIT);
    }

    /**
     * Authentication using firebase: login
     */
    public void createSignInIntent() {
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
                        .setTheme(R.style.AuthenticationTheme)      // Set theme
                        /*.setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")*/
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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


    @Override
    public Observable<byte[]> downloadUrl(String url) {
        return dao.downloadUrl(url);
    }

    @Override
    public Observable<byte[]> downloadFacebookImage(String url, String type) {
        return dao.downloadFacebookImage(url, type);
    }

    @Override
    public Observable<List<Word>> loadDictionnaryDataFromAssets() {
        //Load dictionary data from assets dictory
        return dao.loadDictionnaryDataFromAssets();
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
    public User updateProfile() {
        //Check if the session content user
        if (session.getUser() != null) {
            return session.getUser();
        } else {
            //Check in the database if the user was connected
            List<User> users = null;
            try {
                users = userDao.getAll();
                if (users != null && !users.isEmpty()) {
                    session.setUser(users.get(0));
                }
            } catch (DaoException e) {
                Log.e(getClass().getSimpleName(), "Dao Exception!", e);
            }
        }
        return session.getUser();
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
    }

    @Override
    public void clearNotification() {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.bottom_menu_me);
        badgeDrawable.setVisible(false);
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
                case "google.com":
                    //Connection using google
                    googleConnect(firebaseUser, user, profile);
                    break;
                //Connected with facebook account
                case "facebook.com":
                    //Connection using facebook
                    facebookConnect(user, profile);
                    break;
                //connected with phone number
                case "phone":
                    //Connection using phone number
                    phoneNumberConnect(user, profile);
                    break;
                //connected with an email address
                case "password":
                    //Connection using email address
                    mailConnect(user, profile);
                    break;
                default:
                    break;
            }

        }
        //Mise à jour de la base de données
        try {
            userDao.save(user);
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
     * @param profile
     */
    private void facebookConnect(User user, UserInfo profile) {
        if (Utils.isConnectedToNetwork(this)) {
            downloadProfileImage(user, profile);
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
        if (Utils.isConnectedToNetwork(this)) {
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (updateProfile() != null) {
            connexion.setVisible(false);
        } else {
            connexion.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
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

    @Override
    public void okButtonAction(DialogInterface dialogInterface, int i) {
        super.onBackPressed();
    }

    @Override
    public void noButtonAction(DialogInterface dialogInterface, int i) {
        this.dialog.dismiss();
    }

    /**
     * Update the user profile
     *
     * @param user
     */
    void updateUserConnectedProfile(User user) {
        View headerNavView = navigationView.getHeaderView(0);
        CircleImageView profile = headerNavView.findViewById(R.id.profile_image);
        TextView title = headerNavView.findViewById(R.id.profile_user_name);
        TextView mail = headerNavView.findViewById(R.id.profile_user_email_address);
        MenuItem disconnect = navigationView.getMenu().findItem(R.id.menu_deconnexion);
        //Si l'utilisateur est connecte?
        if (user != null) {
            disconnect.setVisible(true);
            switch (user.getType()) {
                case GOOGLE:
                case FACEBOOK:
                    title.setText(user.getNom());
                    mail.setText(user.getEmail());
                    profile.setImageDrawable(user.getImageUrl() != null ? FileUtils.loadImageFromStorage("glearning", user.getNom() + ".png", DApplicationContext.getContext()) : getDrawable(R.drawable.baseline_account_circle_white_48dp));
                    break;
                case MAIL:
                    title.setText(user.getNom());
                    mail.setText(user.getEmail());
                    profile.setImageDrawable(getDrawable(R.drawable.baseline_account_circle_white_48dp));
                    break;
                case PHONE:
                    title.setText(user.getNom());
                    mail.setText(user.getPhone());
                    profile.setImageDrawable(getDrawable(R.drawable.baseline_account_circle_white_48dp));
                    break;
            }

        } else {
            disconnect.setVisible(false);
            title.setText(R.string.activity_main_username);
            mail.setText(R.string.activity_main_email_address);
            profile.setImageDrawable(getDrawable(R.drawable.baseline_account_circle_white_48dp));
        }
    }


    void downloadProfileImage(User user, UserInfo profile) {
        Bundle params = new Bundle();
        params.putString("fields", "id, name, birthday,hometown,email,gender,cover,picture.width(640).height(640)");
        new GraphRequest(AccessToken.getCurrentAccessToken(), "me", params, HttpMethod.GET,
                response -> {
                    if (response != null) {
                        try {
                            FacebookProfile facebookProfile = objectMapper.readValue(response.getJSONObject().toString(), new TypeReference<FacebookProfile>() {
                            });
                            Log.e(TAG, "facebook url " + facebookProfile.getPicture().getData().getUrl());
                            user.setType(User.Type.FACEBOOK);
                            user.setNom(facebookProfile.getName());
                            user.setImageUrl(facebookProfile.getPicture().getData().getUrl());
                            user.setEmail(facebookProfile.getEmail());
                            FacebookUrl facebookUrl = facebookProfileImageUrlSplit(facebookProfile.getPicture().getData().getUrl(), "?");
                            downloadFacebookProfileImage(facebookUrl.getBaseUrl())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(bytes -> {
                                        FileUtils.saveToInternalStorage(bytes, "glearning", user.getNom() + ".png", this);
                                        //Update profile
                                        updateUserConnectedProfile(user);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FacebookProfile {
        private String id;
        private String name;
        private String email;
        private Picture picture;

        public FacebookProfile() {
        }

        public FacebookProfile(String id, String name, String email, Picture picture) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.picture = picture;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Picture getPicture() {
            return picture;
        }

        public void setPicture(Picture picture) {
            this.picture = picture;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        class Picture {

            private Data data;

            public Picture() {
            }

            public Picture(Data data) {
                this.data = data;
            }

            public Data getData() {
                return data;
            }

            public void setData(Data data) {
                this.data = data;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            class Data {
                private int height;
                private String url;
                private int width;

                public Data() {
                }

                public Data(int height, String url, int width) {
                    this.height = height;
                    this.url = url;
                    this.width = width;
                }

                public int getHeight() {
                    return height;
                }

                public void setHeight(int height) {
                    this.height = height;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public int getWidth() {
                    return width;
                }

                public void setWidth(int width) {
                    this.width = width;
                }
            }
        }

        @Override
        public String toString() {
            return "FacebookProfile{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", picture=" + picture +
                    '}';
        }
    }

    FacebookUrl facebookProfileImageUrlSplit(String url, String character) {
        String[] urlSplited = url.split("\\?");
        return new FacebookUrl(urlSplited[0], urlSplited[1]);
    }

    class FacebookUrl {
        private String baseUrl;
        private String params;

        public FacebookUrl() {
        }

        public FacebookUrl(String baseUrl, String params) {
            this.baseUrl = baseUrl;
            this.params = params;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }
    }
}
