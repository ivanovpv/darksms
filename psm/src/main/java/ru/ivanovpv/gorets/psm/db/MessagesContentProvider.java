package ru.ivanovpv.gorets.psm.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.view.ConversationsCursor;

public class MessagesContentProvider extends ContentProvider {
    private final static int GET_CONVERSATIONS_LIST = 0;
    private final static int GET_ONE_CONVERSATION = 1;
    private final static int GET_MESSAGES_LIST = 2;
    private static final String TAG = MessagesContentProvider.class.getName();
    private static final String CONVERSATION_PATH = "conversations";
    private static final String MESSAGES_PATH = "messages";
    private static final String AUTHORITY = "ru.ivanovpv.gorets.psm.provider";
    public static final Uri CONVERSATION_URI = Uri.parse("content://" + AUTHORITY + "/" + CONVERSATION_PATH);
    public static final Uri MESSAGES_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH);
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, CONVERSATION_PATH, GET_CONVERSATIONS_LIST);
        sUriMatcher.addURI(AUTHORITY, CONVERSATION_PATH + "/#", GET_ONE_CONVERSATION);
        sUriMatcher.addURI(AUTHORITY, MESSAGES_PATH, GET_MESSAGES_LIST);
    }

    public MessagesContentProvider() {
        super();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        Log.i(TAG, "Creating messages content provider");
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor=null;
        switch (sUriMatcher.match(uri)) {
            case GET_CONVERSATIONS_LIST:
                cursor=new ConversationsCursor(this.getContext(), projection, selection, selectionArgs, sortOrder);
                break;
            case GET_ONE_CONVERSATION:
                if(Me.DEBUG && (selection!=null || selectionArgs!=null || projection!=null || sortOrder!=null))
                    Log.w(TAG, "Ignored selection/projection and sortOrder parameters");
                cursor = Me.getMe().getMessageDAO().getMmsSmsConversationCursor(this.getContext(), uri.getLastPathSegment());
                break;
            case GET_MESSAGES_LIST:
                if(Me.DEBUG && (projection!=null || sortOrder!=null))
                    Log.w(TAG, "Ignored projection and sortOrder parameters");
                cursor = Me.getMe().getMessageDAO().getMmsSmsCursor(this.getContext(), selection, selectionArgs);
            }
        if(cursor!=null)
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
