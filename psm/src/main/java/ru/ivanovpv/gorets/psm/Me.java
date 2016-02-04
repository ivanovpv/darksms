package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.L;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.sharedpreferences.Pref;
import ru.ivanovpv.gorets.psm.cipher.KeyExchange;
import ru.ivanovpv.gorets.psm.db.*;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.PsmSettings_;
import ru.ivanovpv.gorets.psm.protocol.DeliveredBroadcastReceiver;
import ru.ivanovpv.gorets.psm.protocol.SentBroadcastReceiver;
import ru.ivanovpv.gorets.psm.service.ScreenStateBroadcastReceiver;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 499 $
 *   $LastChangedDate: 2014-03-24 23:30:05 +0400 (Пн, 24 мар 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/Me.java $
 */

/**
 * Application specific globals
 */
//@ReportsCrashes( formKey = "dDkyRWxjT1F2QVo1a3BheTBlSFlnNUE6MQ", logcatArguments = { "-t", "50" } )
@ReportsCrashes
        (
                formKey = "",
                logcatArguments = {"-t", "50"},
                mailTo = "ivanovpv@gmail.com, egoretss@gmail.com, gsorron@gmail.com",
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.acraToastText,
                sharedPreferencesName = "psm"
        )
@EApplication
public class Me extends Application {
    public static final boolean DEBUG = true; //debug logs enabled?
    public static final boolean TEST = false; //for testing invitations in emulators
    public static final boolean FREE = true;
    public static final boolean ENABLE_ACRA = true;
    public static final boolean ENABLE_MARKET = false;
    public static final boolean MILITARY = false;
    public static final boolean KITKAT_SUPPORT = true;
    public static final String PREFERENCES_NAME = "psm";
    private static final String TAG = Me.class.getName();
    private Boolean writeMessagePermission = null;
    private NotificationManager nManager;
    private static Me me;
    private DbMainHelper dbHelper;
    private ContactDAO contactDAO;
    private MessageDAO messageDAO;
    private MessagePurseDAO messagePurseDAO;
    private boolean logged = false;
    private BroadcastReceiver sent = null, delivered = null, screenlocker = null;

    @Pref
    public PsmSettings_ psmSettings;

    public Me() {
        me = this;
    }

    /**
     * Necessary to have this - especially in SmsReceiver - otherwise looks like don't need it
     *
     * @return
     */
    public static Me getMe() {
        return me;
    }

    @Override
    public void onCreate() {
        //PSM real key
        super.onCreate();
        ACRA.init(this);
        me = this;

        SharedPreferences settings = psmSettings.getSharedPreferences();
        SharedPreferences.Editor editor = settings.edit();
        if (Me.ENABLE_ACRA && !Me.MILITARY) {
            ACRA.getErrorReporter().checkReportsOnApplicationStart();
            editor.putBoolean(ACRA.PREF_DISABLE_ACRA, false);
        } else {
            editor.putBoolean(ACRA.PREF_DISABLE_ACRA, true);
        }
        editor.commit();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
//                .memoryCacheExtraOptions(480 << 5, 800 << 5) // width, height
//                .threadPoolSize(5)
//                .threadPriority(Thread.MIN_PRIORITY + 2)
//                .denyCacheImageMultipleSizesInMemory()
//                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // 2 Mb
//                .discCacheFileNameGenerator(new HashCodeFileNameGenerator())
//                .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
//                .enableLogging()
                .build();
        ImageLoader.getInstance().init(config);
        if (!DEBUG)
            L.disableLogging();
        else
            L.enableLogging();
        L.disableLogging();
//        initImageLoader(getApplicationContext());

        dbHelper = new DbMainHelper(this.getApplicationContext());
        messageDAO = new MessageDAO(dbHelper);
        contactDAO = new ContactDAO(dbHelper);
        messagePurseDAO = new MessagePurseDAO(dbHelper);
        messagePurseDAO.init(this.getApplicationContext()); //setting initial ssms purse
        //this.enableReceiverComponent();
        this.getApplicationContext().registerReceiver(sent = new SentBroadcastReceiver(), new IntentFilter(Constants.ACTION_SMS_SENT));
        this.getApplicationContext().registerReceiver(delivered = new DeliveredBroadcastReceiver(), new IntentFilter(Constants.ACTION_SMS_DELIVERED));
        this.getApplicationContext().registerReceiver(screenlocker = new ScreenStateBroadcastReceiver(), new IntentFilter(Intent.ACTION_SCREEN_OFF));
        doFirstRun();
    }

    private void doFirstRun() {
        if (psmSettings.firstRun().get()) { //add shortcut to app in home screen
            final Intent shortcutIntent = new Intent(getApplicationContext(), PSMActivity_.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            final Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.getString(R.string.psm));
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.psm));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            addIntent.putExtra("duplicate", false);  //may it's already there so don't duplicate
            getApplicationContext().sendBroadcast(addIntent);
            psmSettings.firstRun().put(false);
        }
    }

    public void rateMe(Activity activity) {
        if (!Me.ENABLE_MARKET)
            return;
        int i = psmSettings.rateMe().get();
        if (Me.DEBUG)
            Log.i(TAG, "Rate launch num=" + i);
        if (psmSettings.rateApp().get()) {
            if (i == 0)
                activity.showDialog(R.string.rateApp);
        }
        psmSettings.rateMe().put(i++ >= 10 ? 0 : i++);
    }

    public void exit() {
        //this.disableReceiverComponent();
        try {
            if (sent != null)
                this.getApplicationContext().unregisterReceiver(sent);
            if (delivered != null)
                this.getApplicationContext().unregisterReceiver(delivered);
            if (screenlocker != null)
                this.getApplicationContext().unregisterReceiver(screenlocker);

            if (messageDAO != null) {
                messageDAO.closeDatabase();
            }
            messageDAO = null;
            if (contactDAO != null) {
                contactDAO.closeDatabase();
            }
            contactDAO = null;
            if (dbHelper != null) {
                dbHelper.close();
            }
            dbHelper = null;
        } catch (Throwable th) {
            Log.w(TAG, "Error during exiting application", th);
        }
        System.runFinalization();
        System.gc();
        android.os.Process.killProcess(android.os.Process.myPid());   // suicide
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        messageDAO = null;
        contactDAO = null;
        dbHelper.close();
        dbHelper = null;
    }

    public NotificationManager getNotificationManager() {
        if (nManager == null)
            nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return nManager;
    }

    public ContactDAO getContactDAO() {
        if (contactDAO == null) {
            if (dbHelper == null)
                dbHelper = new DbMainHelper(this.getApplicationContext());
            contactDAO = new ContactDAO(dbHelper);
        }
        return contactDAO;
    }

    public MessageDAO getMessageDAO() {
        if (messageDAO == null) {
            if (dbHelper == null)
                dbHelper = new DbMainHelper(this.getApplicationContext());
            messageDAO = new MessageDAO(dbHelper);
        }
        return messageDAO;
    }

    public HashDAO getHashDAO() {
        if (dbHelper == null)
            dbHelper = new DbMainHelper(this.getApplicationContext());
        return new HashDAO(dbHelper);
    }

    public MessagePurseDAO getMessagePurseDAO() {
        if (messagePurseDAO == null) {
            if (dbHelper == null)
                dbHelper = new DbMainHelper(this.getApplicationContext());
            messagePurseDAO = new MessagePurseDAO(dbHelper);
        }
        return messagePurseDAO;
    }

    public DbMainHelper getDbHelper() {
        return dbHelper;
    }

    public boolean isLoginEnabled(Context context) {
        //stub
        return false;
        //return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.loginEnabledKey), false);
    }

    public Uri getMessageSoundUri() {
        String uriString = psmSettings.smsRingTone().get();
        if (uriString == null || uriString.trim().length() == 0)
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri uri = Uri.parse(uriString);
        if (uri == null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return uri;
    }

    public int getProtectionPrivacyLevel() {
        try {
            String s = psmSettings.protectionPrivacyLevel().get();
            return Integer.parseInt(s);
        } catch (Exception ex) {
            Log.w(TAG, "Invalid protection privacy level, assuming default");
            return 0;
        }
    }

    public int getMessagingPrivacyLevel() {
        try {
            String s = psmSettings.messagingPrivacyLevel().get();
            return Integer.parseInt(s);
        } catch (Exception ex) {
            Log.w(TAG, "Invalid messaging privacy level, assuming default");
            return 0;
        }
    }

    //@todo in future version key exchange type can be different!
    public static char getDefaultKeyExchangeType() {
        return KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public String getAndroidId() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null)
            androidId = "No_Id_aT_AlL";
        return androidId;
    }

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(me.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Can't get version name", e);
            return "Unknown or hacked!";
        }
    }


    private void enableReceiverComponent() {
        String packageName = this.getApplicationContext().getPackageName();
        //enabling sms receiver
        String receiverComponent = SmsReceiver.class.getName();
        ComponentName componentName = new ComponentName(packageName, receiverComponent);
        PackageManager packageManager = this.getApplicationContext().getPackageManager();
        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void disableReceiverComponent() {
        String packageName = this.getApplicationContext().getPackageName();
        //disabling sms receiver
        String receiverComponent = SmsReceiver.class.getName();
        ComponentName componentName = new ComponentName(packageName, receiverComponent);
        PackageManager packageManager = this.getApplicationContext().getPackageManager();
        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
    }

    /**
     * Method to highlight match text
     *
     * @param text  as
     * @param match to text
     * @return SpannableStringBuilder
     * @since 21.11.2012 Gorets
     */
    public static SpannableStringBuilder highlightSelectedText(String text, String match) {
        final SpannableStringBuilder sps = new SpannableStringBuilder();
        if (text != null)
            sps.append(text);
        if (match != null && !match.equals("")) {
            int start = text.toLowerCase().indexOf(match.toLowerCase());
            if (start != -1) {
                int end = start + match.length();
                sps.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
        return sps;
    }

    public int getVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unknown version! Assuming zero version code");
        }
        return 0;
    }

    public String getPublicKey() {
        String s = this.getString(R.string.res01);
        String mask = this.getString(R.string.mas01);
        return xorString(Hash.decryptTemp(s), mask);
    }

    public String getMerchantId() {
        String s = this.getString(R.string.res02);
        String mask = this.getString(R.string.mas01);
        return xorString(Hash.decryptTemp(s), mask);
    }

    private String xorString(String original, String mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < original.length(); i++)
            sb.append((char) (original.charAt(i) ^ mask.charAt(i % mask.length())));
        return sb.toString();
    }

    public boolean isWriteMessagePermission() {
        if (writeMessagePermission == null) {
            if (android.os.Build.VERSION.SDK_INT < 19)
                writeMessagePermission = true;
            else if (!Me.KITKAT_SUPPORT && android.os.Build.VERSION.SDK_INT >= 19)
                writeMessagePermission = false;
            else if (!Telephony.Sms.getDefaultSmsPackage(this).equals(this.getPackageName()))
                writeMessagePermission = false;
            else
                writeMessagePermission = true;
        }
        return writeMessagePermission;
    }

    public void setWriteMessagePermission(boolean writeMessagePermission) {
        this.writeMessagePermission = writeMessagePermission;
    }
}

