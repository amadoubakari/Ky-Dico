package com.flys.dico.architecture.custom;

import android.content.SharedPreferences;

import com.flys.dico.architecture.core.ISession;
import com.flys.dico.dao.entities.User;
import com.flys.dico.dao.entities.WordToShare;
import com.flys.dico.dao.service.IDao;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.google.android.play.core.appupdate.AppUpdateManager;

import java.util.Set;

public interface IMainActivity extends IDao {

    // accès à la session
    ISession getSession();

    // changement de vue
    void navigateToView(int position, ISession.Action action);

    // gestion de l'attente
    void beginWaiting();

    void cancelWaiting();

    //update profile
    User updateProfile();

    // constantes de l'application (à modifier) -------------------------------------

    // mode debug
    boolean IS_DEBUG_ENABLED = false;

    // délai maximal d'attente de la réponse du serveur
    int TIMEOUT = 10000;

    // délai d'attente avant exécution de la requête client
    int DELAY = 0;

    // authentification basique
    boolean IS_BASIC_AUTHENTIFICATION_NEEDED = false;

    // adjacence des fragments
    int OFF_SCREEN_PAGE_LIMIT = 1;

    //barre d'onglets
    boolean ARE_TABS_NEEDED = false;

    //image d'attente
    boolean IS_WAITING_ICON_NEEDED = true;

    //nombre de fragments de l'application
    int FRAGMENTS_COUNT = 6;
    //Fragment number
    int SPLASHSCREEN_FRAGMENT = 0;
    int HOME_FRAGMENT = 1;
    int NOTIFICATION_FRAGMENT = 2;
    int ABOUT_FRAGMENT = 3;
    int AUTH_FRAGMENT = 4;
    int SETTINGS_FRAGMENT = 5;

    //todo ajoutez ici vos constantes et autres méthodes
    //hide or show navigation bottom view
    void hideNavigationView(boolean hide);

    //Select the default bottomviem item
    void activateMainButtonMenu(int itemId);

    //update notifications
    void updateNotificationNumber(int number);

    //add new notification on number of notifications
    void clearNotification();

    //
    void setLocale(String language);

    void loadLocale();

    void recreateActivity();

    void hideBottomNavigation(int visibility);

    void popupSnackbarForCompleteUpdate(AppUpdateManager appUpdateManager);

    //When user scroll up the view
    void scrollUp();

    //When user scroll down the view
    void scrollDown();

    //Night mode
    void setNightMode(int mode);

    //
    void getNightMode();

    //share words
    void shareWords(Word word);

    //Share words from action mode
    void shareWords(Set<WordToShare> wordsToShare);

    //Close action mode share words
    void closeActionModeShareWords();

    //Share words from action mode
    void shareWords(WordAdapter wordAdapter, Set<WordToShare> wordsToShare);

    //get shared preference
    SharedPreferences getSharedPreferences();

    //set a language
    void setLanguage(String languageCode);
}
