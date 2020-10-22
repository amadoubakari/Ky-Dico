package com.flys.dico.dao.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flys.dico.R;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.flys.dico.dao.db.NotificationDao;
import com.flys.dico.dao.db.NotificationDaoImpl;
import com.flys.dico.dao.entities.Dictionnaire;
import com.flys.dico.fragments.adapters.Word;
import com.flys.dico.fragments.adapters.WordAdapter;
import com.flys.generictools.dao.daoException.DaoException;
import com.flys.notification.domain.Notification;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscriber;

@EBean(scope = EBean.Scope.Singleton)
public class Dao extends AbstractDao implements IDao {

    // client du service web
    @RestService
    protected WebClient webClient;
    // sécurité
    @Bean
    protected MyAuthInterceptor authInterceptor;
    // le RestTemplate
    private RestTemplate restTemplate;
    // factory du RestTemplate
    private SimpleClientHttpRequestFactory factory;
    // mappeur jSON
    protected ObjectMapper jsonMapper;
    //
    @Bean(NotificationDaoImpl.class)
    protected NotificationDao notificationDao;

    @AfterInject
    public void afterInject() {
        // log
        Log.d(className, "afterInject");
        // on construit le restTemplate
        factory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(factory);
        // on fixe le convertisseur jSON
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        // on fixe le restTemplate du client web
        webClient.setRestTemplate(restTemplate);
        // jsonMapper
        jsonMapper = new ObjectMapper();
    }

    @Override
    public void setUrlServiceWebJson(String url) {
        // on fixe l'URL du service web
        webClient.setRootUrl(url);
    }

    @Override
    public void setUser(String user, String mdp) {
        // on enregistre l'utilisateur dans l'intercepteur
        authInterceptor.setUser(user, mdp);
    }

    @Override
    public void setTimeout(int timeout) {
        if (isDebugEnabled) {
            Log.d(className, String.format("setTimeout thread=%s, timeout=%s", Thread.currentThread().getName(), timeout));
        }
        // configuration factory
        factory.setReadTimeout(timeout);
        factory.setConnectTimeout(timeout);
    }

    @Override
    public void setBasicAuthentification(boolean isBasicAuthenticationNeeded) {
        if (isDebugEnabled) {
            Log.d(className, String.format("setBasicAuthentication thread=%s, isBasicAuthenticationNeeded=%s", Thread.currentThread().getName(), isBasicAuthenticationNeeded));
        }
        // intercepteur d'authentification ?
        if (isBasicAuthenticationNeeded) {
            // on ajoute l'intercepteur d'authentification
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
            interceptors.add(authInterceptor);
            restTemplate.setInterceptors(interceptors);
        }
    }

    @Override
    public Observable<byte[]> downloadUrl(String url) {
        return getResponse(() -> webClient.downloadUrl(url));
    }

    @Override
    public Observable<byte[]> downloadFacebookImage(String url, String type) {
        return getResponse(() -> webClient.downloadFacebookImage(url, type));
    }

    @Override
    public Observable<List<Word>> loadDictionaryDataFromAssets(Context context) {
        //Load dictionary data from assets dictionary
        return Observable.create(subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                try {
                    List<Word> words = jsonMapper.readValue(context.getAssets().open(context.getString(R.string.dictionary_data_source), AssetManager.ACCESS_STREAMING), Dictionnaire.class).getWords();
                    //and observable that are going to emit data
                    emitData(subscriber, words, 50);
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }


    @Override
    public Observable<Void> reloadData(List<Word> words, WordAdapter adapter, RecyclerView recyclerView) {
        return Observable.create(subscriber -> {
            adapter.addWords(words);
            recyclerView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public Observable<List<Notification>> loadNotificationsFromDatabase() {
        return Observable.create(subscriber -> {
            try {
                List<Notification> notifications = notificationDao.getAll();
                if (notifications != null) {
                    subscriber.onNext(notifications.stream()
                            .distinct()
                            .sorted(Comparator.comparing(Notification::getDate).reversed())
                            .collect(Collectors.toList()));
                } else {
                    subscriber.onNext(new ArrayList<>());
                }
                subscriber.onCompleted();
            } catch (DaoException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Observable<byte[]> downloadFacebookImage(String url) {
        return getResponse(() -> webClient.downloadFacebookImage(url));
    }

    @Override
    public Observable<byte[]> downloadFacebookProfileImage(final String baseUrl, final String params) {
        return getResponse(() -> webClient.downloadFacebookProfileImage(baseUrl, params));
    }

    @Override
    public Observable<byte[]> downloadFacebookProfileImage(String baseUrl) {
        return getResponse(() -> webClient.downloadFacebookProfileImage(baseUrl));
    }

    @Override
    public Observable<List<Word>> loadSequenceWords(Context context, int index, int size) {
        return Observable.create(subscriber -> {
            try {
                List<Word> words = jsonMapper.readValue(context.getAssets().open(context.getString(R.string.dictionary_data_source), AssetManager.ACCESS_STREAMING), Dictionnaire.class).getWords();
                subscriber.onNext(words.stream()
                        .skip(index)
                        .limit(size)
                        .collect(Collectors.toList()));
                subscriber.onCompleted();
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<List<Word>> loadWords(Context context, final String query) {
        return Observable.create(subscriber -> {
            try {
                List<Word> words = jsonMapper.readValue(context.getAssets().open(context.getString(R.string.dictionary_data_source), AssetManager.ACCESS_STREAMING), Dictionnaire.class).getWords();
                subscriber.onNext(
                        words.parallelStream()
                                .filter(word -> word.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                                        word.getDescription().toLowerCase().contains(query.toLowerCase()))
                                .collect(Collectors.toList()));
                subscriber.onCompleted();
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        });
    }

    /**
     * It emit data from define limit element on words
     *
     * @param subscriber
     * @param words
     * @param limit
     */
    private void emitData(Subscriber<? super List<Word>> subscriber, List<Word> words, long limit) {
        //Is empty we don't have treatment to do
        if (words.isEmpty()) {
            //If there not element in our list we notify that task is finish
            subscriber.onCompleted();
        } else {
            //Emission of limit element
            subscriber.onNext(words.stream().limit(limit).collect(Collectors.toList()));
            //skip limit element
            emitData(subscriber, words.stream().skip(limit).collect(Collectors.toList()), limit);
        }
    }

}
