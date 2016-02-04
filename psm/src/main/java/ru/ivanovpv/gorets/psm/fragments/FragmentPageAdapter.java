package ru.ivanovpv.gorets.psm.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.PSMActivity;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.fragments.ContactsFragments_;

import java.util.List;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 490 $
 *   $LastChangedDate: 2014-01-29 18:26:27 +0400 (Ср, 29 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/fragments/FragmentPageAdapter.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: Gorets
 * Date: 31.03.13
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */
public class FragmentPageAdapter extends FragmentPagerAdapter {

    FragmentManager fm;
    private SherlockFragmentActivity sherlockFragmentActivity;
    private Fragment fragmentMessage;
    private Fragment fragmentContact;

    public FragmentPageAdapter(FragmentManager fm, SherlockFragmentActivity sherlockFragmentActivity) {
        super(fm);
        this.fm=fm;
        this.sherlockFragmentActivity=sherlockFragmentActivity;
        fragmentMessage = new MessagesFragments_();
        fragmentContact = new ContactsFragments_();
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case PSMActivity.PAGE_MESSAGES:
                return fragmentMessage;
            case PSMActivity.PAGE_CONTACTS:
                return fragmentContact;
        }
        return null;
    }

    @Override
    public int getCount() {
        return PSMActivity.PAGES_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title;
        switch(position) {
            case PSMActivity.PAGE_MESSAGES:
                title=Me.getMe().getString(R.string.messages);
                break;
            case PSMActivity.PAGE_CONTACTS:
                title=Me.getMe().getString(R.string.contacts);
                break;
            default:
                title="";
        }
        return title;
    }
}
