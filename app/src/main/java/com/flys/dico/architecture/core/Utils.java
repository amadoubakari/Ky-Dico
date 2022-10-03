package com.flys.dico.architecture.core;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.View;

import androidx.core.app.TaskStackBuilder;

import com.flys.dico.R;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class Utils {

    // liste de messages d'une exception - version 1
    static public List<String> getMessagesFromException(Throwable ex) {
        // on crée une liste avec les msg d'erreur de la pile d'exceptions
        List<String> messages = new ArrayList<>();
        Throwable th = ex;
        while (th != null) {
            messages.add(th.getMessage());
            th = th.getCause();
        }
        return messages;
    }

    // liste de messages d'une exception - version 2
    static public String getMessageForAlert(Throwable th) {
        // on construit le texte à afficher
        StringBuilder texte = new StringBuilder();
        List<String> messages = getMessagesFromException(th);
        int n = messages.size();
        for (String message : messages) {
            texte.append(String.format("%s : %s\n", n, message));
            n--;
        }
        // résultat
        return texte.toString();
    }

    // liste de messages d'une exception - version 3
    static public String getMessageForAlert(List<String> messages) {
        // on construit le texte à afficher
        StringBuilder texte = new StringBuilder();
        int n = messages.size();
        for (String message : messages) {
            texte.append(String.format("%s : %s\n", n, message));
            n--;
        }
        // résultat
        return texte.toString();
    }

    /**
     * @param context
     * @param parent
     * @param textColor
     * @param msg
     */
    public static void showErrorMessage(Context context, View parent, int textColor, String msg) {
        Snackbar.make(parent, msg, Snackbar.LENGTH_LONG)
                .setAction("CLOSE", v -> {

                })
                .setActionTextColor(textColor)
                .setBackgroundTint(context.getColor(R.color.grey_900))
                .setTextColor(context.getColor(R.color.white))
                .show();
    }

    /**
     * Suppression d'un fichier existant
     *
     * @param fileName
     * @param context
     */
    public static boolean fileExist(String dirName, String fileName, Context context) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir(dirName, Context.MODE_PRIVATE);
        // Create imageDir
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, fileName);
        return file.exists();
    }



    /**
     * @param context
     * @param activityClass
     * @return
     */
    public static Observable<Object> restartApplication(DApplicationContext context, Class activityClass) {
        return Observable.create(subscriber -> {
            TaskStackBuilder.create(context)
                    .addNextIntent(new Intent(context, activityClass))
                    .startActivities();
            subscriber.onCompleted();
        });
    }

}
