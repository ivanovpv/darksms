package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import ru.ivanovpv.gorets.psm.cipher.*;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.Purse;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 493 $
 *   $LastChangedDate: 2014-02-02 18:18:32 +0400 (Вс, 02 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/SettingsActivity.java $
 */

public class SettingsActivity extends SherlockPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener
{

    private final static String TAG = SettingsActivity.class.getName();

    public final static int LOGIN_ENABLED=1;

    SharedPreferences sharedPreferences;
    private static boolean acceptChange;
    private Me me;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        me=(Me )this.getApplication();
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getPreferenceManager().setSharedPreferencesName(Me.PREFERENCES_NAME);
        addPreferencesFromResource(R.xml.settings);
        sharedPreferences=getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Preference aboutAppPref = findPreference("about");
        aboutAppPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Dialog dialog=new AboutDialog(SettingsActivity.this);
                dialog.show();
                return false;
            }
        });

        Preference rateAppPref = findPreference("rate");
        if(Me.ENABLE_MARKET)
            rateAppPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference preference)
                {
                    new RapeMe().launchMarket(SettingsActivity.this);
                    return false;
                }
            });
        else
            rateAppPref.setEnabled(false);
        Preference readEULAPref = findPreference("eula");
        readEULAPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Eula.show(SettingsActivity.this);
                return false;
            }
        });
        //****************************
        //Ask PIN
        //****************************
        CheckBoxPreference askPIN=(CheckBoxPreference )findPreference("askPIN");
        final Hash hash=me.getHashDAO().get();
        askPIN.setChecked(hash.isEnabled());
        askPIN.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(((Boolean )newValue)) {
                        //password not set yet
                        if(me.psmSettings.passwordSalt().get()==null || me.psmSettings.passwordSalt().get().length()==0) {
                            Dialog dialog=new SetPINDialog(SettingsActivity.this);
                            dialog.setOnCancelListener(SettingsActivity.this);
                            dialog.setOnDismissListener(SettingsActivity.this);
                            dialog.show();
                        }
                        else {
                            //there's password, so either change it or use old one
                            new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(SettingsActivity.this.getString(R.string.pinExists))
                            .setMessage(SettingsActivity.this.getString(R.string.pinChange))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.change, new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ChangePinDialog dlg=new ChangePinDialog(SettingsActivity.this);
                                    dlg.setOnCancelListener(SettingsActivity.this);
                                    dlg.setOnDismissListener(SettingsActivity.this);
                                    dlg.show();
                                }
                            })
                            .setNegativeButton(R.string.useOld, new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialogInterface, int whichButton) {
                                    Dialog dialog=new CheckPINDialog(SettingsActivity.this, CheckPINDialog.MODE_SET_CHECK);
                                    dialog.setOnCancelListener(SettingsActivity.this);
                                    dialog.setOnDismissListener(SettingsActivity.this);
                                    dialog.show();
                                }
                            })
                            .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialogInterface, int whichButton) {
                                    CheckBoxPreference askPIN=(CheckBoxPreference )findPreference("askPIN");
                                    askPIN.setChecked(false);
                                    hash.disable();
                                    me.getHashDAO().save(SettingsActivity.this, hash);
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener()
                            {
                                public void onCancel(DialogInterface dialogInterface) {
                                    CheckBoxPreference askPIN=(CheckBoxPreference )findPreference("askPIN");
                                    askPIN.setChecked(false);
                                    hash.disable();
                                    me.getHashDAO().save(SettingsActivity.this, hash);
                                }
                            })
                            .show();
                        }
                    } else {
                        Dialog dialog=new CheckPINDialog(SettingsActivity.this, CheckPINDialog.MODE_CLEAR_CHECK);
                        dialog.setOnCancelListener(SettingsActivity.this);
                        dialog.setOnDismissListener(SettingsActivity.this);
                        dialog.show();
                    }

                    return true;
                }
        });
        //****************************
        //Comfort PIN
        //****************************
        final CheckBoxPreference comfortPIN=(CheckBoxPreference )findPreference("comfortPIN");
        final Purse purse=Me.getMe().getMessagePurseDAO().get(this);
        comfortPIN.setChecked(purse.isComfortPINEnabled());
        comfortPIN.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    comfortPIN.setChecked((Boolean) newValue);
                    purse.setComfortPINEnabled((Boolean) newValue);
                    Me.getMe().getMessagePurseDAO().save(SettingsActivity.this, purse);
                }
                return true;
            }
        });

        //****************************
        //Ringtone
        //****************************
        RingtonePreference smsRingTone = (RingtonePreference) findPreference("smsRingTone");
        if(smsRingTone!=null) {
            Uri ringtoneUri = Uri.parse(me.psmSettings.smsRingTone().get());
            if(ringtoneUri==null || ringtoneUri.toString().length()==0)
                ringtoneUri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if(ringtoneUri!=null)
                me.psmSettings.smsRingTone().put(ringtoneUri.toString());
            else
                me.psmSettings.smsRingTone().put(""); //silence
        }
        ListPreference protectionPrivacyLevel = (ListPreference) findPreference("protectionPrivacyLevel");
        protectionPrivacyLevel.setSummary(protectionPrivacyLevel.getEntry());

        ListPreference messagingPrivacyLevel = (ListPreference) findPreference("messagingPrivacyLevel");
/*        messagingPrivacyLevel.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Spanned s=Html.fromHtml(SettingsActivity.this.getString(R.string.cryptoWarning));
                SpannableString ss=new SpannableString(s);
                Linkify.addLinks(ss, Linkify.WEB_URLS);
                final AlertDialog ad=new AlertDialog.Builder(SettingsActivity.this)
                .setMessage(ss)
                .setPositiveButton(R.string.ok, null)
                .setIcon(R.drawable.psm)
                .create();
                ad.show();
                ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            }
        });*/
        messagingPrivacyLevel.setSummary(messagingPrivacyLevel.getEntry());
        {
            Preference pref;
            KeyExchange keyExchange;
            byte[] key;
            byte[] sKey=Me.getMe().getHashDAO().get().getKey();

            pref= findPreference("fingerPrintBasic");
            keyExchange=Hash.getDefaultKeyExchange(sKey);
            key=keyExchange.getPublicKey();
            pref.setSummary(new FingerPrint(key, Me.getDefaultKeyExchangeType()).toString());

/*            if(me.getMessagingPrivacyLevel() > 0)
            {
                pref= findPreference("fingerPrintMedium");
                if(pref!=null) {
                    keyExchange = new EllipticCurve256B(sKey);
                    key = keyExchange.getPublicKey();
                    pref.setSummary(new FingerPrint(key, KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B).toString());
                }
                pref= findPreference("fingerPrintHigh");
                if(pref!=null) {
                    keyExchange = new EllipticCurve384B(sKey);
                    key = keyExchange.getPublicKey();
                    pref.setSummary(new FingerPrint(key, KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B).toString());
                }
            }
            else {
                pref= findPreference("fingerPrintMedium");
                if(pref!=null)
                    pref.setSummary(R.string.notApplicable);
                pref= findPreference("fingerPrintHigh");
                if(pref!=null)
                    pref.setSummary(R.string.notApplicable);
            }*/
        }
        Preference resetAppPref = findPreference("reset");
        resetAppPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                final AlertDialog.Builder builder_reset = new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.reset)
                    .setMessage(R.string.resetWarning)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(me.psmSettings.askPIN().get()) {
                            final CheckPINDialog checkPINDialog=new CheckPINDialog(SettingsActivity.this, CheckPINDialog.MODE_DOESNT_MATTER);
                            checkPINDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    return;
                                }
                            });
                            checkPINDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    if(!checkPINDialog.isCanceled())
                                        new ResetTask().execute();
                                }
                            });
                            checkPINDialog.show();
                        }
                        else {
                            new ResetTask().execute();
                        }
                    }
                    }).setNegativeButton(R.string.no, null);
                builder_reset.create().show();
                return true;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.compareTo("protectionPrivacyLevel")==0) {
            ListPreference privacyLevel = (ListPreference) findPreference("protectionPrivacyLevel");
            privacyLevel.setSummary(privacyLevel.getEntry());
        }
        if(key.compareTo("messagingPrivacyLevel")==0) {
            ListPreference privacyLevel = (ListPreference) findPreference("messagingPrivacyLevel");
            //privacyLevel.setValueIndex(0);//always should be zero!
            privacyLevel.setSummary(privacyLevel.getEntry());
        }
        else if(key.compareTo("smsRingTone")==0) {
            RingtonePreference smsRingTone = (RingtonePreference) findPreference("smsRingTone");
            if(smsRingTone==null)
                return;
            Uri ringtoneUri = Uri.parse(me.psmSettings.smsRingTone().get());
            Ringtone ringTone = RingtoneManager.getRingtone(this, ringtoneUri);
            smsRingTone.setSummary(ringTone==null ? "" : ringTone.getTitle(this));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCancel(DialogInterface dialog) {
        CheckBoxPreference askPIN=(CheckBoxPreference )findPreference("askPIN");
        Hash hash=me.getHashDAO().get();
        if(dialog instanceof SetPINDialog) {
            hash.disable();
            me.getHashDAO().save(this, hash);
            askPIN.setChecked(false);
        }
        else if(dialog instanceof CheckPINDialog) {
            CheckPINDialog checkPINDialog=(CheckPINDialog )dialog;
            if(checkPINDialog.getMode()==CheckPINDialog.MODE_CLEAR_CHECK) {
                askPIN.setChecked(true);
                hash.enable();
            }
            else {
                askPIN.setChecked(false);
                hash.disable();
            }
            me.getHashDAO().save(this, hash);
        }
        else if(dialog instanceof ChangePinDialog) {
            askPIN.setChecked(false);
            hash.disable();
            me.getHashDAO().save(this, hash);
        }
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean("askPIN", askPIN.isChecked());
        edit.commit();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDismiss(DialogInterface dialog) {
        CheckBoxPreference askPIN=(CheckBoxPreference )findPreference("askPIN");
        Hash hash=me.getHashDAO().get();
        if(dialog instanceof CheckPINDialog) {
            CheckPINDialog checkPINDialog=(CheckPINDialog )dialog;
            if(checkPINDialog.isCanceled())
                return;
            if(checkPINDialog.getMode()==CheckPINDialog.MODE_SET_CHECK) {
                askPIN.setChecked(true);
                hash.enable();
            }
            else {
                askPIN.setChecked(false);
                hash.disable();
            }
            me.getHashDAO().save(this, hash);
        }
        else if(dialog instanceof ChangePinDialog) {
            if(((ChangePinDialog )dialog).isCanceled())
                return;
            askPIN.setChecked(true);
            hash.enable();
            me.getHashDAO().save(this, hash);
        }
        else if(dialog instanceof SetPINDialog) {
            SetPINDialog setPINDialog=(SetPINDialog )dialog;
            if(setPINDialog.isCanceled())
                return;
            askPIN.setChecked(true);
            hash.enable();
            me.getHashDAO().save(this, hash);
        }
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean("askPIN", askPIN.isChecked());
        edit.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ResetTask extends AsyncTask<Void, Integer, Boolean>
    {
        Context context;
        ProgressDialog resetDialog;

        protected ResetTask() {
            super();
            this.context=SettingsActivity.this;
            resetDialog = new ProgressDialog(SettingsActivity.this);
            resetDialog.setOwnerActivity(SettingsActivity.this);
            resetDialog.setCancelable(false);
            resetDialog.setCanceledOnTouchOutside(false);
            resetDialog.setProgress(0);
            resetDialog.setTitle(R.string.reset);
            resetDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            int count=Me.getMe().getMessageDAO().countMessages(context)+2;
            resetDialog.setMax(count);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resetDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... arg) {
            boolean allReset=true;
            int progress=0;
            Cursor cursor=null;
            Message message;
            MessageDAO messageDAO=Me.getMe().getMessageDAO();
            try {
                cursor=messageDAO.getMessagesCursor(context);
                while(cursor!=null && cursor.moveToNext()) {
                    message=messageDAO.getSmsMessage(context, cursor);
                    if(message.isProtected()) {
                        if(!message.unprotectSilent(context))
                            allReset=false;
                    }
                    if(message.isCiphered()) {
                        if(!message.decipherSilent(context))
                            allReset=false;
                    }
                    if(Me.DEBUG)
                        Log.i(TAG, "Reset progress publish="+progress);
                    this.publishProgress(++progress);
                }
                SharedPreferences.Editor editor=SettingsActivity.this.sharedPreferences.edit();
                editor.clear();
                editor.commit();
                this.publishProgress(++progress);
                Me.getMe().getDbHelper().reset();
                this.publishProgress(++progress);
            }
            catch(Exception ex) {
                Log.w(TAG, "Error while resetting...", ex);
            }
            finally {
                if(cursor!=null)
                    cursor.close();
            }
            return allReset;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(Me.DEBUG)
                Log.i(TAG, "Reset progress update="+values[0]);
            resetDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(resetDialog!=null && resetDialog.isShowing())
                resetDialog.dismiss();
            if(!result) {
                new AlertDialog.Builder(context)
                                .setTitle(R.string.reset)
                                .setMessage(R.string.resetError)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Me.getMe().exit();
                                }}).show();
            }
            else
                Me.getMe().exit();
        }
    }
}

