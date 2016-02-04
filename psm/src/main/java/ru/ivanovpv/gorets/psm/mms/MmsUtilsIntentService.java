/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.mms;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.mms.pdu.*;
import ru.ivanovpv.gorets.psm.mms.transaction.HttpUtils;
import ru.ivanovpv.gorets.psm.mms.transaction.TransactionSettings;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessage;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessagePart;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Pavel on 14.06.2014.
 */
public class MmsUtilsIntentService extends IntentService
{
    private static final String TAG=MmsUtilsIntentService.class.getName();

    static final String APN_TYPE_HIPRI = "hipri";
    // "Features" accessible through the connectivity manager
    static final String FEATURE_ENABLE_MMS = "enableMMS";
    static final String FEATURE_ENABLE_SUPL = "enableSUPL";
    static final String FEATURE_ENABLE_DUN = "enableDUN";
    static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";
    static final int APN_ALREADY_ACTIVE     = 0;
    static final int APN_REQUEST_STARTED    = 1;
    static final int APN_TYPE_NOT_AVAILABLE = 2;
    static final int APN_REQUEST_FAILED     = 3;

    static final String REASON_ROAMING_ON = "roamingOn";
    static final String REASON_ROAMING_OFF = "roamingOff";
    static final String REASON_DATA_DISABLED = "dataDisabled";
    static final String REASON_DATA_ENABLED = "dataEnabled";
    static final String REASON_GPRS_ATTACHED = "gprsAttached";
    static final String REASON_GPRS_DETACHED = "gprsDetached";
    static final String REASON_CDMA_DATA_ATTACHED = "cdmaDataAttached";
    static final String REASON_CDMA_DATA_DETACHED = "cdmaDataDetached";
    static final String REASON_APN_CHANGED = "apnChanged";
    static final String REASON_APN_SWITCHED = "apnSwitched";
    static final String REASON_APN_FAILED = "apnFailed";
    static final String REASON_RESTORE_DEFAULT_APN = "restoreDefaultApn";
    static final String REASON_RADIO_TURNED_OFF = "radioTurnedOff";
    static final String REASON_PDP_RESET = "pdpReset";
    static final String REASON_VOICE_CALL_ENDED = "2GVoiceCallEnded";
    static final String REASON_VOICE_CALL_STARTED = "2GVoiceCallStarted";
    static final String REASON_PS_RESTRICT_ENABLED = "psRestrictEnabled";
    static final String REASON_PS_RESTRICT_DISABLED = "psRestrictDisabled";
    static final String REASON_SIM_LOADED = "simLoaded";

    private ConnectivityManager connectivityManager;
    private Context context;
    private TransactionSettings transactionSettings;
    private PowerManager.WakeLock wakeLock;
    private SharedPreferences sharedPreferences;


    public static void startActionReceiveMms(Context context, MultimediaMessage mmsMessage) {
        Intent intent = new Intent(context, MmsUtilsIntentService.class);
        intent.putExtra(MultimediaMessage.TAG, mmsMessage);
        intent.setAction(Constants.ACTION_MESSAGE_RECEIVED);
        context.startService(intent);
    }

    public static void startActionSendMms(Context context, MultimediaMessage mmsMessage) {
        Intent intent = new Intent(context, MmsUtilsIntentService.class);
        intent.putExtra(MultimediaMessage.TAG, mmsMessage);
        intent.setAction(Constants.ACTION_SEND);
        context.startService(intent);
    }

    public MmsUtilsIntentService() {
        super(TAG);
    }

    public MmsUtilsIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context=this.getApplicationContext();
        connectivityManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        sharedPreferences=context.getSharedPreferences(Me.PREFERENCES_NAME, MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //ensure that wakelock released anyway
        releaseWakeLock();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MultimediaMessage multimediaMessage=(MultimediaMessage )intent.getSerializableExtra(MultimediaMessage.TAG);
        final String action = intent.getAction();
        if (Constants.ACTION_MESSAGE_RECEIVED.equals(action)) {
            this.receiveMms(multimediaMessage);
        }
        else if(Constants.ACTION_SEND.equals(action)) {
            this.sendMms(multimediaMessage);
        }
    }

    private void sendMms(final MultimediaMessage multimediaMessage) {
        ConnectivityManager mConnMgr =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        final int result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");

        if (result != 0)
        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        return;
                    }

                    @SuppressWarnings("deprecation")
                    NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                    if ((mNetworkInfo == null) || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                        return;
                    }

                    if (!mNetworkInfo.isConnected()) {
                        return;
                    } else
                    {
                        sendMmsData(multimediaMessage);
                        unregisterReceiver(this);
                    }
                }
            };

            registerReceiver(receiver, filter);
        } else {
            sendMmsData(multimediaMessage);
        }
    }

    //@TODO test mms sender
    private void sendMmsData(final MultimediaMessage multimediaMessage) {
        final Context context = this;

        new Thread(new Runnable() {

            @Override
            public void run() {
                final SendReq sendRequest = new SendReq();
                final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(multimediaMessage.getAddress());
                if (phoneNumbers != null && phoneNumbers.length > 0) {
                    sendRequest.addTo(phoneNumbers);
                }
                final PduBody pduBody = new PduBody();
                Vector<MultimediaMessagePart> parts=multimediaMessage.getParts();
                if (parts != null) {
                    for (MultimediaMessagePart part : parts)
                    {
                        if (part != null)
                        {
                            try
                            {
                                final PduPart partPdu = new PduPart();
                                partPdu.setName(part.getName().getBytes());
                                partPdu.setContentType(part.getContentType().getBytes());
                                partPdu.setData(part.getData());
                                pduBody.addPart(partPdu);
                            } catch (Exception e) {
                                Log.w(TAG, "Ignoring MMS parts pdu creation error", e);
                            }
                        }
                    }
                }

                sendRequest.setBody(pduBody);
                final PduComposer composer = new PduComposer(context, sendRequest);
                final byte[] bytesToSend = composer.make();
                List<APN> apns = new ArrayList<APN>();
                try
                {
                    APNHelper helper = new APNHelper(context);
                    apns = helper.getMMSApns();
                } catch (Exception e)
                {
                    APN apn = new APN(sharedPreferences.getString("mmsc_url", ""), sharedPreferences.getString("mms_port", ""), sharedPreferences.getString("mms_proxy", ""));
                    apns.add(apn);
                    Log.w(TAG, "Error trying to get MMS APNs, using default as of preferences", e);
                }

                try {
                    HttpUtils.httpConnection(context, 4444L, apns.get(0).MMSCenterUrl, bytesToSend, HttpUtils.HTTP_POST_METHOD, !TextUtils.isEmpty(apns.get(0).MMSProxy), apns.get(0).MMSProxy, Integer.parseInt(apns.get(0).MMSPort));
                    ConnectivityManager mConnMgr =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                    mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE_MMS, "enableMMS");
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Cursor query = context.getContentResolver().query(Uri.parse("content://mms"), new String[] {"_id"}, null, null, "date desc");
                            query.moveToFirst();
                            String id = query.getString(query.getColumnIndex("_id"));
                            query.close();

                            ContentValues values = new ContentValues();
                            values.put(MessageDAO.MMS_MESSAGE_BOX, Message.MESSAGE_BOX_SENT);  //message sent?
                            String where = "_id" + " = '" + id + "'";
                            context.getContentResolver().update(Uri.parse("content://mms"), values, where, null);

                            context.unregisterReceiver(this);
                        }

                    };

                    registerReceiver(receiver, filter);
                } catch (Exception e) {
                    Cursor query = context.getContentResolver().query(Uri.parse("content://mms"), new String[] {"_id"}, null, null, "date desc");
                    query.moveToFirst();
                    String id = query.getString(query.getColumnIndex("_id"));
                    query.close();

                    ContentValues values = new ContentValues();
                    values.put("msg_box", Message.MESSAGE_BOX_FAILED);  //failed
                    String where = "_id" + " = '" + id + "'";
                    context.getContentResolver().update(Uri.parse("content://mms"), values, where, null);
                    Log.e(TAG, "MMS send error", e);
                }

            }

        }).start();
    }

    private void receiveMms(MultimediaMessage multimediaMessage) {
        createWakeLock(context);
        try {
            //getting current network environment
            transactionSettings = new TransactionSettings(context, connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).getExtraInfo());
            //establishing connection - either using MMS connectivity or default
            this.startConnectivity();
            //ensuring route to MMS declared url (2 ways, standard and fancy)
            this.routeToHost(multimediaMessage.getContentLocation());
            byte[] rawPdu = HttpUtils.httpConnection(context,
                    //using message id as operation token (message should already by saved)
                    multimediaMessage.hashCode(),
                    multimediaMessage.getContentLocation(),
                    null,
                    HttpUtils.HTTP_GET_METHOD,
                    transactionSettings.isProxySet(),
                    transactionSettings.getProxyAddress(),
                    transactionSettings.getProxyPort());
            PduParser parser = new PduParser(rawPdu);
            GenericPdu pdu = parser.parse();
            //processPduAttachments(multimediaMessage, pdu);
            PduPersister pduPersister=PduPersister.getPduPersister(context);

            Uri uri=pduPersister.persist(pdu, Uri.withAppendedPath(MessageDAO.MMS_INBOX_URI, multimediaMessage.getId()), true, false, null);
            MultimediaMessage mms=Me.getMe().getMessageDAO().getMmsMessage(context, uri);
            mms.setDownloaded(true);
            if (Me.DEBUG)
                Log.i(TAG, "MMS #" + multimediaMessage.getId() + " extracted");
        } catch (Exception ex) {
            Log.e(TAG, "Error trying to get MMS url=" + multimediaMessage.getContentLocation(), ex);
            multimediaMessage.setDownloaded(false);
        } finally {
            releaseWakeLock();
        }
        //if (multimediaMessage.isDownloaded())
            //Me.getMe().getMessageDAO().saveMmsParts(context, multimediaMessage);

    }

    private void processPduAttachments(MultimediaMessage multimediaMessage, GenericPdu pdu) {
        if (pdu instanceof MultimediaMessagePdu) {
            PduBody body = ((MultimediaMessagePdu) pdu).getBody();
            if (body != null) {
                int partsNum = body.getPartsNum();
                for (int i = 0; i < partsNum; i++) {
                    try {
                        PduPart part = body.getPart(i);
                        if (part == null || part.getData() == null || part.getContentType() == null || part.getName() == null)
                            continue;
                        multimediaMessage.addPart(new MultimediaMessagePart(part));
                        String partType = new String(part.getContentType());
                        String partName = new String(part.getName());
                        Log.d(TAG, "Part Name: " + partName);
                        Log.d(TAG, "Part Type: " + partType);
                        if (ContentType.isTextType(partType)) {
                            //text
                            Log.i(TAG, "Text=" + ByteUtils.byteArrayToString(part.getData(), 0));
                        }
                        else if (ContentType.isImageType(partType)) {
                            //image
                            Log.i(TAG, "Image size =" + part.getData().length);
                        }
                        else if (ContentType.isVideoType(partType)) {
                            //video
                            Log.i(TAG, "Video size =" + part.getData().length);
                        }
                        else if (ContentType.isAudioType(partType)) {
                            //audio
                            Log.i(TAG, "Audio size =" + part.getData().length);
                        }
                        else {
                            //unknown
                            Log.i(TAG, "Unknown size =" + part.getData().length);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error processing part #"+i+" of MMS uri"+multimediaMessage.getContentLocation(), e);
                        // Bad part shouldn't ruin the party for the other parts
                    }
                }
            }
        } else {
            Log.w(TAG, "Not a MultimediaMessagePdu PDU");
        }
    }


    private void startConnectivity() throws Exception {
        if (!connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).isAvailable()) {
            throw new Exception("Not available yet");
        }
        int count = 0;
        int result = beginMmsConnectivity();
        if (result != APN_ALREADY_ACTIVE) {
            //try to connect thru MMS Access point
            NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            while (!info.isConnected()) {
                Thread.sleep(1500);
                info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                Log.d(TAG, "Waiting for MMS CONNECTED: state=" + info.getState());
                if (count++ > 50)
                    break;
            }
            count=0;
            //try to connect thru default one
            while (!info.isConnected()) {
                Thread.sleep(1500);
                info = connectivityManager.getNetworkInfo(connectivityManager.getNetworkPreference());
                Log.d(TAG, "Waiting for default CONNECTED: state=" + info.getState());
                if (count++ > 5)
                    throw new Exception("Failed to connect");
            }
        }
        Thread.sleep(1500);
    }

    private int beginMmsConnectivity() throws IOException {
        int result = connectivityManager.startUsingNetworkFeature(
                ConnectivityManager.TYPE_MOBILE, FEATURE_ENABLE_MMS);
        switch (result) {
            case APN_ALREADY_ACTIVE:
            case APN_REQUEST_STARTED:
                acquireWakeLock();
                return result;
        }
        throw new IOException("Cannot establish MMS connectivity");
        //TransactionSettings transactionSettings = new TransactionSettings(context, connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).getExtraInfo());
    }

    private synchronized void createWakeLock(Context context) {
        // Create a new wake lock if we haven't made one yet.
        if (wakeLock == null) {
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            wakeLock= pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            wakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
         // Don't release the wake lock if it hasn't been created and acquired.
        if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
        }
    }

    private int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16) | ((addrBytes[1] & 0xff) << 8) | (addrBytes[0] & 0xff);
        return addr;
    }

    private void routeToHost(String url) throws Exception {
        try {
            ensureRouteToHost(url);
        }
        catch(Exception ex) {
            Log.w(TAG, "Failed to standard way route to host", ex);
            try {
                ensureRouteToHostFancy(url);
            }
            catch(Exception e) {
                Log.w(TAG, "Failed to fancy way route to host", e);
                throw e;
            }
        }
    }

    private void ensureRouteToHost(String url) throws IOException {
        int inetAddr;
        if (transactionSettings.isProxySet()) {
            String proxyAddr = transactionSettings.getProxyAddress();
            inetAddr = lookupHost(proxyAddr);
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr))
                    throw new IOException("Cannot establish route to proxy " + inetAddr);
            }
        } else {
            Uri uri = Uri.parse(url);
            inetAddr = lookupHost(uri.getHost());
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr))
                    throw new IOException("Cannot establish route to " + inetAddr + " for " + url);
            }
        }
    }

    private void ensureRouteToHostFancy(String url) throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method m = connectivityManager.getClass().getMethod("requestRouteToHostAddress", new Class[] { int.class, InetAddress.class });
        InetAddress inetAddr;
        if (transactionSettings.isProxySet()) {
            String proxyAddr = transactionSettings.getProxyAddress();
            try {
                inetAddr = InetAddress.getByName(proxyAddr);
            } catch (UnknownHostException e) {
                throw new IOException("Cannot establish route for " + url + ": Unknown proxy " + proxyAddr);
            }
            if (!(Boolean) m.invoke(connectivityManager, new Object[] { ConnectivityManager.TYPE_MOBILE_MMS, inetAddr }))
                throw new IOException("Cannot establish route to proxy " + inetAddr);
        } else {
            Uri uri = Uri.parse(url);
            try {
                inetAddr = InetAddress.getByName(uri.getHost());
            } catch (UnknownHostException e) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            }
            if (!(Boolean) m.invoke(connectivityManager, new Object[] { ConnectivityManager.TYPE_MOBILE_MMS, inetAddr }))
                throw new IOException("Cannot establish route to " + inetAddr + " for " + url);
        }
    }
}
