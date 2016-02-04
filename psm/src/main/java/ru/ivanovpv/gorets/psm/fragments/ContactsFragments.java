package ru.ivanovpv.gorets.psm.fragments;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 490 $
 *   $LastChangedDate: 2014-01-29 18:26:27 +0400 (Ср, 29 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/fragments/ContactsFragments.java $
 */

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.widget.AbsListView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EFragment;
import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.view.ContactCursorRowAdapter;

@EFragment(R.layout.contacts_layout)
public class ContactsFragments extends SherlockListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, ListView.OnScrollListener {

    public static final int INIT_CONTACTS_LOADER = 0;
    public static final int FILTER_CONTACTS_LOADER = 1;

    private ContactCursorRowAdapter contactCursorRowAdapter;

    @App
    Me me; //   - unworkable!

    @AfterViews
    public void initContactsPage() {

        setRetainInstance(true);
        setHasOptionsMenu(true);

        getLoaderManager().initLoader(INIT_CONTACTS_LOADER, null, this);
        contactCursorRowAdapter = new ContactCursorRowAdapter(
                getActivity(),
                R.layout.contact_row_layout,
                null, //Me.getMe().getContactDAO().getAdapterCursor(getActivity(), ""),
                null);
        setListAdapter(contactCursorRowAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Me.getMe().getContactDAO(); //to be sure that app started
    }

    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case INIT_CONTACTS_LOADER:
                return Me.getMe().getContactDAO().getContactsCursorLoader(this.getActivity(), null);
            case FILTER_CONTACTS_LOADER:
                return Me.getMe().getContactDAO().getContactsCursorLoader(this.getActivity(), bundle.getString(Constants.EXTRA_FILTER));
            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        contactCursorRowAdapter.swapCursor(cursor);
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        contactCursorRowAdapter.swapCursor(null);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case SCROLL_STATE_IDLE:
                contactCursorRowAdapter.isScrolling = false;
                break;
            case SCROLL_STATE_TOUCH_SCROLL:
            case SCROLL_STATE_FLING:
                contactCursorRowAdapter.isScrolling = true;
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ContactCursorRowAdapter getContactCursorRowAdapter() {
        return this.contactCursorRowAdapter;
    }
}
