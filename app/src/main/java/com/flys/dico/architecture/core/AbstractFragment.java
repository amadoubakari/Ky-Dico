package com.flys.dico.architecture.core;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.core.content.IntentCompat;
import androidx.fragment.app.Fragment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flys.dico.R;
import com.flys.dico.activity.MainActivity_;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.flys.dico.architecture.custom.IMainActivity;
import com.flys.dico.architecture.custom.Session;
import com.flys.dico.utils.Constants;
import com.flys.tools.dialog.MaterialNotificationDialog;
import com.flys.tools.domain.NotificationData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public abstract class AbstractFragment extends Fragment {

    // données privées ------------------------------------------------------------
    // les abonnements aux observables
    private List<Subscription> abonnements = new ArrayList<>();
    // menu du fragment
    private Menu menu;
    private MenuItemState[] menuOptionsStates = new MenuItemState[0];
    // cycle de vie du fragment
    private boolean isVisibleToUser = false;
    private boolean saveFragmentDone = false;
    // états du fragment
    private CoreState previousState;
    // mappeur jSON
    private ObjectMapper jsonMapper = new ObjectMapper();
    // cycle de vie du fragment
    private boolean fragmentHasToBeInitialized = false;
    private boolean viewHasToBeInitialized = false;
    // tâches asynchrones
    private boolean runningTasksHaveBeenCanceled;

    // données  accessibles aux classes filles ---------------------------------------
    // mode debug
    final protected boolean isDebugEnabled = IMainActivity.IS_DEBUG_ENABLED;
    // nom de la classe
    protected String className;
    // tâches asynchrones
    protected int numberOfRunningTasks;
    // activité
    protected IMainActivity mainActivity;
    protected Activity activity;
    // session
    protected Session session;

    // constructeur ----------------------
    public AbstractFragment() {
        // init
        className = getClass().getSimpleName();
        fragmentHasToBeInitialized = true;
        // log
        if (isDebugEnabled) {
            Log.d(className, "constructeur");
        }
    }

    // update Fragment ----------------------------------------------------------------------------------
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // log
        if (isDebugEnabled) {
            Log.d(className, "onCreateOptionsMenu");
        }
        // mémoire
        this.menu = menu;
        // on récupère les # options du menu si cela n'a pas déjà été fait
        if (fragmentHasToBeInitialized) {
            // on récupère les # options du menu
            getMenuOptionsStates(menu);
            // activité
            this.activity = getActivity();
            this.mainActivity = (IMainActivity) activity;
            this.session = (Session) this.mainActivity.getSession();
        }
        mainActivity.hideNavigationView(hideNavigationBottomView());
        // on récupère l'état précédent du fragment (la toute 1ère fois, seul le booléen hasBeenVisited représente quelque chose)
        previousState = session.getCoreState(getNumView());
        // mise à jour du fragment fille en plusieurs étapes
        // étape 1 - est-ce la 1ère visite ?
        if (!previousState.getHasBeenVisited()) {
            if (isDebugEnabled) {
                Log.d(className, "initFragment initView updateForFirstVisit");
            }
            // initialisation fragment et vue
            initFragment(null);
            initView(null);
            // raz previousState pour la suite
            previousState = null;
        } else {
            // ce n'est pas la 1ère visite
            // étape 2 : le fragment doit-il être initialisé ?
            if (fragmentHasToBeInitialized) {
                if (isDebugEnabled) {
                    Log.d(className, "initialisation fragment");
                }
                // fragment fille
                initFragment(previousState);
            }
            // étape 3 : la vue doit elle être initialisée ?
            if (viewHasToBeInitialized) {
                if (isDebugEnabled) {
                    Log.d(className, "initialisation vue");
                }
                // fragment fille
                initView(previousState);
            }
        }
        // étape 4 : un submit, une navigation, un restore ?
        // log
        if (isDebugEnabled) {
            try {
                Log.d(className, String.format("session=%s", jsonMapper.writeValueAsString(session)));
                Log.d(className, String.format("état précédent=%s", jsonMapper.writeValueAsString(previousState)));
            } catch (JsonProcessingException e) {
                Log.e(getClass().getSimpleName(), "restore view Processing Exception", e);
            }
        }
        // action en cours
        ISession.Action action = session.getAction();
        switch (action) {
            case SUBMIT:
                if (isDebugEnabled) {
                    Log.d(className, "updateOnSubmit");
                }
                // fragment fille
                updateOnSubmit(previousState);
                break;
            case NAVIGATION:
                if (isDebugEnabled) {
                    Log.d(className, "updateForNavigation");
                }
                if (previousState != null) {
                    // restauration menu
                    setMenuOptionsStates(previousState.getMenuOptionsState());
                    // fragment fille
                    updateOnRestore(previousState);
                } else {
                    // il s'agit d'une 1ère visite - rien à faire
                }
                break;
            case RESTORE:
                // restauration
                if (isDebugEnabled) {
                    Log.d(className, "updateOnRestore");
                }
                // restauration menu (previousState ne peut être null)
                if (previousState != null) {
                    setMenuOptionsStates(previousState.getMenuOptionsState());
                    // fragment fille
                    updateOnRestore(previousState);
                }
                break;
        }

        // étape 5 : mise à jour terminales ----------------------
        // on a changé de vue
        session.setPreviousView(getNumView());
        // plus d'action en cours
        session.setAction(ISession.Action.NONE);
        // lorsqu'on quittera ce fragment, il devra être sauvegardé
        saveFragmentDone = false;
        // tant que le fragment n'est pas reconstruit, il n'a pas à être initialisé
        fragmentHasToBeInitialized = false;
        // tant que la vue n'est pas reconstruite, elle n'a pas à être initialisée
        viewHasToBeInitialized = false;
        // on revient à un fonctionnement normal de la sélection d'onglets
        session.setNavigationOnTabSelectionNeeded(true);

        // on signale au fragment que la vue est prête
        if (isDebugEnabled) {
            Log.d(className, "notifyEndOfUpdates");
        }
        notifyEndOfUpdates();
    }

    // gestion du menu ------------------------------------------
    private void getMenuOptions(Menu menu, List<Integer> menuOptionsIds) {
        // on parcourt tous les items du menu
        for (int i = 0; i < menu.size(); i++) {
            // item n° i
            MenuItem menuItem = menu.getItem(i);
            menuOptionsIds.add(menuItem.getItemId());
            // si item n° i est un sous-menu, alors on recommence
            if (menuItem.hasSubMenu()) {
                // récursivité
                getMenuOptions(menuItem.getSubMenu(), menuOptionsIds);
            }
        }
    }

    private void getMenuOptionsStates(Menu menu) {
        // résultat
        if (isDebugEnabled) {
            Log.d(className, "getMenuOptionsStates(Menu)");
        }
        // on récupère les identifiants des options du menu
        List<Integer> menuOptionsIds = new ArrayList<>();
        getMenuOptions(menu, menuOptionsIds);
        // on transfère les options de menu dans un tableau
        menuOptionsStates = new MenuItemState[menuOptionsIds.size()];
        for (int i = 0; i < menuOptionsStates.length; i++) {
            // identifiant option
            int id = menuOptionsIds.get(i);
            // état option
            menuOptionsStates[i] = new MenuItemState(id, menu.findItem(id).isVisible());
        }
        // résultat
        if (isDebugEnabled) {
            Log.d(className, String.format("Nombre d'options de menu=%s", menuOptionsStates.length));
        }
    }

    // états des options de menu
    private MenuItemState[] getMenuOptionsStates() {
        MenuItemState[] menuOptionsStates = new MenuItemState[this.menuOptionsStates.length];
        for (int i = 0; i < menuOptionsStates.length; i++) {
            // état
            MenuItemState state = this.menuOptionsStates[i];
            // id du menu
            int id = state.getMenuItemId();
            // initialisation état
            if (menu.findItem(id) != null) {
                menuOptionsStates[i] = new MenuItemState(id, menu.findItem(id).isVisible());
            }
        }
        // résultat
        return menuOptionsStates;
    }

    // affichage options de menu -----------------------------------
    protected void setAllMenuOptionsStates(boolean isVisible) {
        // on met à jour toutes les options du menu
        for (MenuItemState menuItemState : menuOptionsStates) {
            menu.findItem(menuItemState.getMenuItemId()).setVisible(isVisible);
        }
    }

    protected void setMenuOptionsStates(MenuItemState[] menuItemStates) {
        // on met à jour certaines options du menu
        for (MenuItemState menuItemState : menuItemStates) {
            if (menuItemState != null) {
                menu.findItem(menuItemState.getMenuItemId()).setVisible(menuItemState.isVisible());
            }

        }
    }

    // gestion de l'attente de la fin d'une opération asynchrone -------------------------------------
    protected void beginRunningTasks(int numberOfRunningTasks) {
        // on note le nombre de tâches qui vont s'exécuter
        this.numberOfRunningTasks = numberOfRunningTasks;
        // on met l'image d'attente
        mainActivity.beginWaiting();
        // on vide la liste des abonnements
        abonnements.clear();
        // pas encore d'annulation
        runningTasksHaveBeenCanceled = false;
    }

    protected void cancelWaitingTasks() {
        // on cache l'image d'attente
        mainActivity.cancelWaiting();
    }


    /**
     * exécution d'une tâche asynchrone avec RxAndroid
     *
     * @param process
     * @param consumeResult
     * @param <T>
     */
    protected <T> void executeInBackground(Observable<T> process, Action1<T> consumeResult) {
        // process : l'observable à exécuter / observer
        // consumeResult : la méthode qui exploite la réponse obtenue
        // endOfTask : la méthode à appeler lorsque le processus observé émet la marque de fin d'émission
        // on ne crée de nouveaux abonnements que s'il n'y a pas eu annulation
        if (!runningTasksHaveBeenCanceled) {
            // exécution sur thread d'E/S et observation sur thread de l'Ui
            process = process.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
            // on exécute l'observable
            try {
                abonnements.add(process.subscribe(
                        // consommation résultat
                        consumeResult,
                        // consommation exception
                        th -> consumeThrowable(th),
                        // fin de tâche
                        () -> endOfTask()));
            } catch (Throwable th) {
                consumeThrowable(th);
            }
        }
    }

    private void endOfTask() {
        // une tâche en moins à attendre
        numberOfRunningTasks--;
        // fini ?
        if (numberOfRunningTasks == 0) {
            // fin attente
            cancelWaitingTasks();
            // on signale la fin des tâches à la classe fille
            notifyEndOfTasks(false);
        }
    }

    // une opération asynchrone a émis une exception
    // ou une exception s'est produite pendant l'exécution d'une opération asynchrone
    private void consumeThrowable(Throwable th) {
        // th : l'exception à traiter
        // cancelWaiting : la méthode à appeler pour signaler qu'il faut arrêter d'attendre
        // log
        if (isDebugEnabled) {
            Log.d(className, "Exception reçue");
        }
        // on annule les tâches déjà lancées
        cancelRunningTasks();
        // on affiche les messages d'erreur
        showAlert(th);
    }

    // annulation des tâches
    protected void cancelRunningTasks() {
        // log
        if (isDebugEnabled) {
            Log.d(className, "Annulation des tâches lancées");
        }
        // on annule toutes les tâches asynchrones enregistrées
        for (Subscription abonnement : abonnements) {
            abonnement.unsubscribe();
        }
        // on note l'annulation
        runningTasksHaveBeenCanceled = true;
        numberOfRunningTasks = 0;
        // fin de l'attente
        cancelWaitingTasks();
        // on signale l'annulation des tâches au fragment fille
        notifyEndOfTasks(true);
    }


    // gestion exception -------------------------------------------------------------------

    // affichage alerte sur exception
    protected void showAlert(Throwable th) {
        // on affiche les messages de la pile d'exceptions du Throwable th
        new android.app.AlertDialog.Builder(activity).setTitle("Des erreurs se sont produites").setMessage(Utils.getMessageForAlert(th)).setNeutralButton("Fermer", null).show();
    }

    // affichage liste de messages
    protected void showAlert(List<String> messages) {
        // on affiche la liste des messages
        new android.app.AlertDialog.Builder(activity).setTitle("Des erreurs se sont produites").setMessage(Utils.getMessageForAlert(messages)).setNeutralButton("Fermer", null).show();
    }

    // cycle de vie --------------------------------------------------------
    @Override
    public void onDestroyView() {
        // parent
        super.onDestroyView();
        // log
        if (isDebugEnabled) {
            Log.d(className, "onDestroyView");
        }
    }

    @Override
    public void onDestroy() {
        // parent
        super.onDestroy();
        // log
        if (isDebugEnabled) {
            Log.d(className, "onDestroy");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // sauvegarde ?
        if (this.isVisibleToUser) {
            // le fragment va être caché - on le sauvegarde
            if (!saveFragmentDone) {
                saveState();
            }
        }
        // mémoire
        this.isVisibleToUser = true;
        onFragmentResume();
    }

    public void onFragmentResume() {
        if (mainActivity == null) {
            this.activity = getActivity();
            this.mainActivity = (IMainActivity) activity;
            this.session = (Session) this.mainActivity.getSession();
        }
    }

    private void saveState() {
        // tâches à annuler ?
        if (numberOfRunningTasks != 0) {
            // on annule les tâches
            cancelRunningTasks();
        }
        // on sauvegarde l'état du fragment
        CoreState currentState = saveFragment();
        // le frgamen a été visité
        currentState.setHasBeenVisited(true);
        // sauvegarde état du menu
        currentState.setMenuOptionsState(getMenuOptionsStates());
        if (session != null) {
            // mise en session
            session.setCoreState(getNumView(), currentState);
            // sauvegarde faite
            saveFragmentDone = true;
        }

        // log
        if (isDebugEnabled) {
            try {
                Log.d(className, String.format("saveFragment state=%s", jsonMapper.writeValueAsString(currentState)));
            } catch (JsonProcessingException e) {
                Log.e(getClass().getSimpleName(), "saving view Processing Exception", e);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // parent
        super.onActivityCreated(savedInstanceState);
        // log
        if (isDebugEnabled) {
            Log.d(className, "onActivityCreated");
        }
        // la vue doit être restaurée
        viewHasToBeInitialized = true;
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        // log
        if (isDebugEnabled) {
            Log.d(className, String.format("onSaveInstanceState isVisibleToUser=%s, saveFragmentDone=%s", isVisibleToUser, saveFragmentDone));
        }
        // parent
        super.onSaveInstanceState(outState);
        // sauvegarde du fragment seulement s'il est visible
        if (isVisibleToUser) {
            // peut-être que la sauvegarde a déjà été faite ?
            if (!saveFragmentDone) {
                saveState();
            }
            // restauration à faire dans tous les cas
            session.setAction(ISession.Action.RESTORE);
        }
    }

    /**
     * Change application language
     */
    protected void changeLanguage() {
        String[] language = {getString(R.string.settings_fragment_language_french), getString(R.string.settings_fragment_language_english)};
        int checkedItem = isEnglish();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.customMaterialAlertEditDialog);
        builder.setTitle(getString(R.string.settingsFragment_select_language))
                .setSingleChoiceItems(language, checkedItem, (dialog, which) -> {
                    //if user select preferred language as English then
                    if (language[which].equals(getString(R.string.settings_fragment_language_english))) {
                        mainActivity.setLanguage(Constants.EN);
                        dialog.dismiss();
                        restartApp();
                    }
                    //if user select preferred language as Hindi then
                    if (language[which].equals(getString(R.string.settings_fragment_language_french))) {
                        mainActivity.setLanguage(Constants.FR);
                        dialog.dismiss();
                        restartApp();
                    }
                })
                .setPositiveButton(getString(R.string.activity_main_button_cancel), (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    public void restartApp(){
        getActivity().finish();
        final Intent intent = getActivity().getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(intent);
    }
    public void restartApp2() {
        MaterialNotificationDialog notificationDialog = new MaterialNotificationDialog(activity, new NotificationData(getString(R.string.app_name), getString(R.string.abstract_fragment_restart_app), getString(R.string.activity_main_button_yes_msg), getString(R.string.activity_main_button_no_msg), activity.getDrawable(R.drawable.logo), R.style.customMaterialAlertEditDialog), new MaterialNotificationDialog.NotificationButtonOnclickListeneer() {
            @Override
            public void okButtonAction(DialogInterface dialogInterface, int i) {
                beginRunningTasks(1);
                Utils.restartApplication(DApplicationContext.getInstance(),MainActivity_.class);
            }

            @Override
            public void noButtonAction(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        notificationDialog.show(getParentFragmentManager(), "material_notification_alert_dialog");
    }

    /**
     * System language
     *
     * @return 0 for english and 1 for french
     */
    protected int isEnglish() {
        String localeCode = mainActivity.getSharedPreferences().getString(Constants.MY_LAND, getResources().getConfiguration().getLocales().get(0).getLanguage());
        int checkedItem;
        if (localeCode.equals(Constants.FR)) {
            checkedItem = Language.FRENCH.getOrder();
        } else {
            checkedItem = Language.ENGLISH.getOrder();
        }
        return checkedItem;
    }


    protected void setCustomTheme(int theme) {
        SharedPreferences.Editor editor = mainActivity.getSharedPreferences().edit();
        editor.putInt(Constants.THEME, theme);
        editor.apply();
        getActivity().finish();
        final Intent intent = getActivity().getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(intent);
    }

    protected enum Language {
        FRENCH(0),
        ENGLISH(1);

        private int order;

        Language(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    // classes filles -----------------------------------------------------
    public abstract CoreState saveFragment();

    protected abstract int getNumView();

    protected abstract void initFragment(CoreState previousState);

    protected abstract void initView(CoreState previousState);

    protected abstract void updateOnSubmit(CoreState previousState);

    protected abstract void updateOnRestore(CoreState previousState);

    protected abstract void notifyEndOfUpdates();

    protected abstract void notifyEndOfTasks(boolean runningTasksHaveBeenCanceled);

    protected abstract boolean hideNavigationBottomView();


}
