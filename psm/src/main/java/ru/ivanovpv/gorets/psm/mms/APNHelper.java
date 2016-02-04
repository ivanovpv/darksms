package ru.ivanovpv.gorets.psm.mms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import ru.ivanovpv.gorets.psm.Me;

/**
 * Created by pivanov on 11.02.2015.
 * List of APNs
 */
public class APNHelper {
    private static final String TAG=APNHelper.class.getName();

    //constants defined since KitKat in previous versions they're undocumented
    private static final Uri CARRIERS_URI=Uri.parse("content://telephony/carriers"); //Telephony.Carriers.CONTENT_URI
    private static final String CARRIERS_TYPE="type"; //Telephony.Carriers.TYPE
    private static final String CARRIERS_MMSC="mmsc"; //Telephony.Carriers.MMSC
    private static final String CARRIERS_MMSPROXY="mmsproxy"; //Telephony.Carriers.MMSPROXY
    private static final String CARRIERS_MMSPORT="mmsport"; //Telephony.Carriers.MMSPORT
    private Context context;

    public APNHelper(final Context context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    //@todo test with Android Gingerbread
    public List<APN> getMMSApns() {
        final Cursor apnCursor = this.context.getContentResolver().query(Uri.withAppendedPath(CARRIERS_URI, "current"), null, null, null, null);
        if ( apnCursor == null ) {
            return Collections.EMPTY_LIST;
        } else {
            final List<APN> results = new ArrayList<APN>();
            if ( apnCursor.moveToFirst() ) {
                do {
                    final String type = apnCursor.getString(apnCursor.getColumnIndex(CARRIERS_TYPE));
                    if ( !TextUtils.isEmpty(type) && ( type.equalsIgnoreCase("*") || type.equalsIgnoreCase("mms") ) ) {
                        final String mmsc = apnCursor.getString(apnCursor.getColumnIndex(CARRIERS_MMSC));
                        final String mmsProxy = apnCursor.getString(apnCursor.getColumnIndex(CARRIERS_MMSPROXY));
                        final String port = apnCursor.getString(apnCursor.getColumnIndex(CARRIERS_MMSPORT));
                        final APN apn = new APN();
                        apn.MMSCenterUrl = mmsc;
                        apn.MMSProxy = mmsProxy;
                        apn.MMSPort = port;
                        results.add(apn);

                        if(Me.DEBUG)
                            Log.i(TAG, "MMSC="+mmsc+", mmsProxy="+mmsProxy+":"+port);
                    }
                } while ( apnCursor.moveToNext() );
            }
            apnCursor.close();
            return results;
        }
    }

}
