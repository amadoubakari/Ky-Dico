package com.flys.dico.fragments.behavior;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.utils.Constants;
import com.flys.tools.dialog.MaterialNotificationDialog;
import com.flys.tools.domain.NotificationData;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.fragment_settings_layout)
@OptionsMenu(R.menu.menu_home)
public class SettingsFragment extends AbstractFragment implements MaterialNotificationDialog.NotificationButtonOnclickListeneer {

    private static final int KYOSSI_SETTINGS_NOTIFICATION_REQUEST_CODE = 32;
    @ViewById(R.id.notification_switch)
    protected Switch enableNotification;

    @ViewById(R.id.dialog_change_language)
    protected LinearLayout languageTv;

    @ViewById(R.id.current_language)
    protected TextView tvLanguage;

    private MaterialNotificationDialog notificationDialog;

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
        if (isEnglish() == Language.ENGLISH.getOrder()) {
            tvLanguage.setText(getString(R.string.fragment_settings_language_en));
        } else {
            tvLanguage.setText(getString(R.string.fragment_settings_language_fr));
        }
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

    @Click(R.id.dialog_change_language)
    public void changeLanguageAction() {
        changeLanguage();
    }

    /**
     * Change application language
     */
    public void changeLanguage() {
        final String[] language = {getString(R.string.settings_fragment_language_french), getString(R.string.settings_fragment_language_english)};
        final int checkedItem = isEnglish();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.settingsFragment_select_language))
                .setSingleChoiceItems(language, checkedItem, (dialog, which) -> {
                    tvLanguage.setText(language[which]);
                    //if user select preferred language as English then
                    if (language[which].equals(getString(R.string.settings_fragment_language_english))) {
                        mainActivity.setLocale(Constants.EN);
                        mainActivity.recreateActivity();
                    }
                    //if user select preferred language as Hindi then
                    if (language[which].equals(getString(R.string.settings_fragment_language_french))) {
                        mainActivity.setLocale(Constants.FR);
                        mainActivity.recreateActivity();
                    }
                })
                .setPositiveButton(getString(R.string.activity_main_button_cancel), (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * System language
     *
     * @return 0 for english and 1 for french
     */
    private int isEnglish() {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
        String localeCode = sharedPreferences.getString(Constants.MY_LAND, "");
        final int checkedItem;
        if (localeCode.equals(Constants.EN)) {
            checkedItem = Language.ENGLISH.getOrder();
        } else {
            checkedItem = Language.FRENCH.getOrder();
            ;
        }
        return checkedItem;
    }

    public enum Language {

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
}
