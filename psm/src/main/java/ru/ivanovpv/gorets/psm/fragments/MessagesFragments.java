package ru.ivanovpv.gorets.psm.fragments;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 490 $
 *   $LastChangedDate: 2014-01-29 18:26:27 +0400 (Ср, 29 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/fragments/MessagesFragments.java $
 *   RIP Gorshok 19.07.2013
 */

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.actionbarsherlock.app.SherlockListFragment;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.view.ConversationsCursorRowAdapter;
import ru.ivanovpv.gorets.psm.view.MessageSearchCursorRowAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gorets
 * Date: 10.04.13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */

@EFragment(R.layout.messages_layout)
public class MessagesFragments extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor>  {
    private static final String TAG = MessagesFragments.class.getName();

    public static final int INIT_MESSAGES_LOADER = 0;
    public static final int ALL_MESSAGES_LOADER = 1;
    public static final int FILTER_MESSAGES_LOADER = 2;
    public static final int SEARCH_MESSAGES_LOADER = 3;

    private ConversationsCursorRowAdapter conversationsCursorRowAdapter;
    private MessageSearchCursorRowAdapter messageSearchCursorRowAdapter;

    @AfterViews
    public void init() {
        setHasOptionsMenu(true);
        setRetainInstance(true);
        getLoaderManager().initLoader(INIT_MESSAGES_LOADER, null, this);
        this.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Me.getMe().getMessageDAO(); //to be sure that app started
    }


    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        //currentLoaderId=loaderId;
        switch (loaderId) {
            case INIT_MESSAGES_LOADER:
                conversationsCursorRowAdapter = new ConversationsCursorRowAdapter(getSherlockActivity(), R.layout.message_row_layout, null);
                messageSearchCursorRowAdapter = new MessageSearchCursorRowAdapter(getSherlockActivity(), R.layout.found_message_row_layout);
                setListAdapter(conversationsCursorRowAdapter);
                return Me.getMe().getMessageDAO().getConversationsCursorLoader(this.getSherlockActivity());
            case ALL_MESSAGES_LOADER:
                setListAdapter(conversationsCursorRowAdapter);
                return Me.getMe().getMessageDAO().getConversationsCursorLoader(this.getSherlockActivity());
            case FILTER_MESSAGES_LOADER:
                setListAdapter(conversationsCursorRowAdapter);
                return Me.getMe().getMessageDAO().getConversationsCursorLoader(this.getSherlockActivity(), bundle.getString(Constants.EXTRA_FILTER));
            case SEARCH_MESSAGES_LOADER:
                if(Me.DEBUG)
                    Log.i(TAG, "Loading search results cursor");
                setListAdapter(messageSearchCursorRowAdapter);
                return Me.getMe().getMessageDAO().getSearchCursorLoader(this.getSherlockActivity(), bundle.getString(Constants.EXTRA_FILTER));

            default:
                return null;
        }
    }

/*    public void restart() {
        LoaderManager loaderManager=this.getLoaderManager();
        if(loaderManager!=null) {
            loaderManager.restartLoader(currentLoaderId, currentBundle, this);
        }
    }*/

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch(cursorLoader.getId()) {
            case INIT_MESSAGES_LOADER:
            case ALL_MESSAGES_LOADER:
            case FILTER_MESSAGES_LOADER:
                conversationsCursorRowAdapter.swapCursor(cursor);
                break;
            case SEARCH_MESSAGES_LOADER:
                messageSearchCursorRowAdapter.swapCursor(cursor);
                break;
        }
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        switch(cursorLoader.getId()) {
            case INIT_MESSAGES_LOADER:
            case ALL_MESSAGES_LOADER:
            case FILTER_MESSAGES_LOADER:
                conversationsCursorRowAdapter.changeCursor(null);
                break;
            case SEARCH_MESSAGES_LOADER:
                messageSearchCursorRowAdapter.changeCursor(null);
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (!isAdded()) {
            return;
        }
        super.onDestroy();
    }
}
