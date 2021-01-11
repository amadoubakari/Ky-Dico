package com.flys.dico.architecture.custom;

import com.flys.dico.architecture.core.AbstractSession;
import com.flys.dico.dao.entities.User;
import com.flys.dico.dao.entities.WordToShare;
import com.flys.dico.fragments.adapters.Word;
import com.flys.notification.domain.Notification;

import java.util.ArrayList;
import java.util.List;

public class Session extends AbstractSession {
    // données à partager entre fragments eux-mêmes et entre fragments et activité
    // les éléments qui ne peuvent être sérialisés en jSON doivent avoir l'annotation @JsonIgnore
    // ne pas oublier les getters et setters nécessaires pour la sérialisation / désérialisation jSON
    private User user;
    //Notification coming from firebase notification
    private Notification notification;
    //saved notificatiosns
    private static List<Notification> notifications = new ArrayList<>();
    //check if the application is subscribe to receive notification from channel
    private boolean subscribed;
    //Dictionnary data save in the session
    private List<Word> words;

    //Words to share
    private List<WordToShare> wordToShares;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Notification getNotification() {
        return this.notification;
    }

    public static List<Notification> getNotifications() {
        return notifications;
    }

    public static void setNotifications(List<Notification> notifs) {
        notifications = notifs;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public List<WordToShare> getWordToShares() {
        return wordToShares;
    }

    public void setWordToShares(List<WordToShare> wordToShares) {
        this.wordToShares = wordToShares;
    }

    public void addWordToShares(WordToShare wordToShare) {
        if (this.wordToShares == null) {
            this.wordToShares = new ArrayList<>();
        }
        this.wordToShares.add(wordToShare);
    }
}
