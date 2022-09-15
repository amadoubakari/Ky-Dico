package com.flys.dico.dao.service;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.notification.domain.Notification;

import java.util.List;

import rx.Observable;

public interface IDao {
    // Url du service web
    void setUrlServiceWebJson(String url);

    // utilisateur
    void setUser(String user, String mdp);

    // timeout du client
    void setTimeout(int timeout);

    // authentification basique
    void setBasicAuthentification(boolean isBasicAuthentificationNeeded);

    // mode debug
    void setDebugMode(boolean isDebugEnabled);

    // délai d'attente en millisecondes du client avant requête
    void setDelay(int delay);

    //Download user avatar from google
    Observable<byte[]> downloadUrl(String url);

    //Download user avatar image from facebook
    Observable<byte[]> downloadFacebookImage(String url, String type);

    //Load dictionary data from assets
    Observable<List<Word>> loadDictionaryDataFromAssets(Context context);

    //Reload data
    Observable<Void> reloadData(List<Word> words, WordAdapter adapter, RecyclerView recyclerView);

    //Load notifications from the data base
    Observable<List<Notification>> loadNotificationsFromDatabase();

    //Download user avatar image from facebook
    Observable<byte[]> downloadFacebookImage(String url);

    Observable<byte[]> downloadFacebookProfileImage(final String baseUrl, final String params);

    Observable<byte[]> downloadFacebookProfileImage(final String baseUrl);

    Observable<List<Word>> loadSequenceWords(Context context, int index, int size);

    Observable<List<Word>> loadWords(Context context, final String query);

    Observable<byte[]> downloadFacebookProfileImage(final String baseUrl, final String ext, final String params);

    //Load notifications from the data base
    Observable<List<Notification>> loadNotificationsFromDatabase(String property, Object value);

    //Load notifications from the data base
    Observable<List<Notification>> loadNotificationsFromDatabase(boolean withAds);

}
