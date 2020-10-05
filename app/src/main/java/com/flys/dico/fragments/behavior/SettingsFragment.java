package com.flys.dico.fragments.behavior;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.tools.dialog.MaterialNotificationDialog;
import com.flys.tools.domain.NotificationData;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.util.Locale;

@EFragment(R.layout.fragment_settings_layout)
@OptionsMenu(R.menu.menu_home)
public class SettingsFragment extends AbstractFragment implements MaterialNotificationDialog.NotificationButtonOnclickListeneer {

    private static final int KYOSSI_SETTINGS_NOTIFICATION_REQUEST_CODE = 32;
    @ViewById(R.id.notification_switch)
    protected Switch enableNotification;

    @ViewById(R.id.dialog_change_language)
    protected TextView languageTv;

    private MaterialNotificationDialog notificationDialog;

    boolean lang_selected;

    Context context;

    Resources resources;

    @Click(R.id.notification_switch)
    public void settings() {
        String msg = "";
        if (enableNotification.isChecked()) {
            msg = getString(R.string.enable_notifications_to_received_news);
        } else {
            msg = getString(R.string.disable_notifications_to_dont_received_news);
        }
        notificationDialog = new MaterialNotificationDialog(activity, new NotificationData(getString(R.string.app_name), msg, getString(R.string.button_yes_msg), getString(R.string.button_no_msg), activity.getDrawable(R.drawable.logo), R.style.Theme_MaterialComponents_Light_Dialog_Alert), this);
        notificationDialog.show(getActivity().getSupportFragmentManager(), "settings_notification_dialog_tag");
    }

    public void changeLanguage() {
        final String[] Language = {"ENGLISH", "FRENCH"};
        final int checkedItem;
        if (lang_selected) {
            checkedItem = 0;
        } else {
            checkedItem = 1;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select a Language...")
                .setSingleChoiceItems(Language, checkedItem, (dialog, which) -> {
                    Toast.makeText(activity, "" + which, Toast.LENGTH_SHORT).show();
                    languageTv.setText(Language[which]);
                    lang_selected = Language[which].equals("ENGLISH");
                    //if user select prefered language as English then
                    if (Language[which].equals("ENGLISH")) {
                        mainActivity.setLocale("en");
                        mainActivity.recreateActivity();
                    }
                    //if user select prefered language as Hindi then
                    if (Language[which].equals("FRENCH")) {
                        //context = LocaleHelper.setLocale(activity, "fr");
                        //resources = context.getResources();
                        mainActivity.setLocale("fr");
                        mainActivity.recreateActivity();
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public CoreState saveFragment() {
        return new CoreState();
    }

    @Override
    protected int getNumView() {
        return mainActivity.SETTINGS_FRAGMENT;
    }

    @Override
    protected void initFragment(CoreState previousState) {
        enableNotification.setChecked(NotificationManagerCompat.from(activity).areNotificationsEnabled());
    }

    @Override
    protected void initView(CoreState previousState) {

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == KYOSSI_SETTINGS_NOTIFICATION_REQUEST_CODE) {
            //Checked
            if (NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
                enableNotification.setChecked(true);
            } else {
                enableNotification.setChecked(false);
            }
        }
    }

    @Override
    public void okButtonAction(DialogInterface dialogInterface, int i) {
        Intent settingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
        startActivityForResult(settingsIntent, KYOSSI_SETTINGS_NOTIFICATION_REQUEST_CODE);
    }

    @Override
    public void noButtonAction(DialogInterface dialogInterface, int i) {
        enableNotification.setChecked(false);
        dialogInterface.dismiss();
    }

    private void setLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.setLocale(locale);
        activity.getBaseContext().getResources().updateConfiguration(configuration, activity.getBaseContext().getResources().getDisplayMetrics());

        //Share
        SharedPreferences.Editor editor = activity.getSharedPreferences("Settings", Activity.MODE_PRIVATE).edit();
        editor.putString("my_land", language);
        editor.apply();
        //activity.recreate();
    }

}
