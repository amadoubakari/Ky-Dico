package com.flys.dico.fragments.behavior;

import android.os.Handler;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.ISession;
import com.flys.dico.architecture.custom.CoreState;

@EFragment(R.layout.fragment_splash_screen)
@OptionsMenu(R.menu.menu_vide)
public class SplashScreenFragment extends AbstractFragment {
    @Override
    public CoreState saveFragment() {
        return new CoreState();
    }

    @Override
    protected int getNumView() {
        return mainActivity.SPLASHSCREEN_FRAGMENT;
    }

    @Override
    protected void initFragment(CoreState previousState) {
       // ((AppCompatActivity) mainActivity).getSupportActionBar().hide();
    }

    @Override
    protected void initView(CoreState previousState) {
        new Handler().postDelayed(() -> {
            mainActivity.activateMainButtonMenu(R.id.bottom_menu_book);
            mainActivity.navigateToView(1, ISession.Action.SUBMIT);
        }, 1000);
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

    //Nous cachons le bottom navigation view

    @Override
    protected boolean hideNavigationBottomView() {
        return true;
    }
}
