package com.flys.dico.fragments.behavior;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.flys.dico.R;
import com.flys.dico.architecture.core.AbstractFragment;
import com.flys.dico.architecture.core.Utils;
import com.flys.dico.architecture.custom.CoreState;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @email: amadoubakari1992@gmail.com
 * @since 03/06/2020 15:34
 */
@EFragment(R.layout.fragment_about)
@OptionsMenu(R.menu.menu_vide)
public class AboutFragment extends AbstractFragment {

    @ViewById(R.id.source)
    protected TextView source;

    @Override
    public CoreState saveFragment() {
        return new CoreState();
    }

    @Override
    protected int getNumView() {
        return mainActivity.ABOUT_FRAGMENT;
    }

    @Override
    protected void initFragment(CoreState previousState) {

    }

    @Override
    protected void initView(CoreState previousState) {
        source.setText(HtmlCompat.fromHtml(activity.getString(R.string.about_fragment_github_source,String.format("#%06x",  Utils.getColorFromAttr(activity, R.attr.color_secondary)& 0xffffff),activity.getString(R.string.fragment_about_github_source_here)), HtmlCompat.FROM_HTML_MODE_LEGACY));
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
    @Click(R.id.site)
    void openSite() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.kyossi_website_url)));
        startActivity(browserIntent);
    }

    @Click(R.id.source)
    void openSources() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.source_repository_git_url)));
        startActivity(browserIntent);
    }
}
