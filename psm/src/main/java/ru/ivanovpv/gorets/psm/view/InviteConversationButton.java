package ru.ivanovpv.gorets.psm.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EView;
import org.androidannotations.annotations.sharedpreferences.Pref;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.SMSActivity;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.persistent.PsmSettings_;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Gorets
 * Date: 13.07.13
 * Time: 14:30
 * To change this template use File | Settings | File Templates.
 */
@EView
public class InviteConversationButton extends Button implements View.OnClickListener {

    @App
    Me me;

    @Pref
    PsmSettings_ settings;

    private static enum STATES {
        INVITE,
        ENCRYPT,
        PLAIN,
        DISABLED;
    }

    private STATES def = STATES.INVITE;
    private Activity activity;
    private ArrayList<PhoneNumber> phoneNumbers;

    public InviteConversationButton(Context context) {
        super(context);
        phoneNumbers=new ArrayList<PhoneNumber>();
        setOnClickListener(this);
    }

    public InviteConversationButton (Context context, AttributeSet attrs) {
        super(context, attrs);
        phoneNumbers=new ArrayList<PhoneNumber>();
        setOnClickListener(this);
    }

    public InviteConversationButton (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        phoneNumbers=new ArrayList<PhoneNumber>();
        setOnClickListener(this);
    }

    public void add(PhoneNumber phoneNumber) {
        if(phoneNumber!=null)
            phoneNumbers.add(phoneNumber);
        if(!this.isEnabled()) {
            setBackgroundResource(R.drawable.sms_disabled);
            def=STATES.DISABLED;
        }
        else if(!areAllInvited()) {
            setBackgroundResource(R.drawable.send_invitation);
            def=STATES.INVITE;
        }
        else if(this.areAllAlwaysEncrypt()) { //all invited - now check are all always encrypt?
            setBackgroundResource(R.drawable.sms_encrypted);
            def = STATES.ENCRYPT;
        }
        else {
            setBackgroundResource(R.drawable.sms_plain);
            def = STATES.PLAIN;
        }
    }

    public void init(final Activity activity) {
        this.activity = activity;
        phoneNumbers.clear();
    }

    public void init(final Activity activity, ArrayList<PhoneNumber> phoneNumbers, boolean reset) {
        this.activity = activity;
        this.phoneNumbers.clear();
        for(PhoneNumber ph:phoneNumbers)
            this.phoneNumbers.add(ph);
        if(!reset)
            return;
        if(!this.isEnabled()) {
            setBackgroundResource(R.drawable.sms_disabled);
            def=STATES.DISABLED;
        }
        else if(!areAllInvited()) {
            setBackgroundResource(R.drawable.send_invitation);
            def=STATES.INVITE;
        }
        else if(this.areAllAlwaysEncrypt()) { //all invited - now check are all always encrypt?
            setBackgroundResource(R.drawable.sms_encrypted);
            def = STATES.ENCRYPT;
        }
        else {
            setBackgroundResource(R.drawable.sms_plain);
            def = STATES.PLAIN;
        }
    }

    public void init(final Activity activity, final PhoneNumber phoneNumber) {
        this.activity = activity;
        phoneNumbers.clear();
        this.add(phoneNumber);
    }

    /**
     *  onInviteButtonClick - doing when contact haven't invite
     **/
    private void onInviteButtonAction() {
        if (!settings.inviteWarning().get()) {
            new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.inviteWarningTitle))
                .setMessage(activity.getString(R.string.inviteWarning1)+"\n"+activity.getString(R.string.inviteWarning2))
                .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton) {
                    for(PhoneNumber phoneNumber:phoneNumbers)
                        Protocol.sendInvitation(activity, phoneNumber);
                    if(activity instanceof SMSActivity)
                        activity.finish();
                }
            })
            .setNegativeButton(R.string.no, null).show();
        } else {
            for(PhoneNumber phoneNumber:phoneNumbers)
                Protocol.sendInvitation(activity, phoneNumber);
            if(activity instanceof SMSActivity)
                activity.finish();
        }
    }

    public boolean isInviteState() {
        return (def==STATES.INVITE ? true : false);
    }

    public boolean isEncryptState() {
        return (def==STATES.ENCRYPT ? true : false);
    }

    public boolean isPlainState() {
        return ((def==STATES.PLAIN || def==STATES.INVITE) ? true : false);
    }

    private boolean areAllInvited() {
        boolean areInvited=true;
        if(phoneNumbers.size()==0)
            return false;
        for(PhoneNumber phoneNumber:phoneNumbers)
            if(!phoneNumber.isPresentKey()) {
                areInvited=false;
                break;
            }
        return areInvited;
    }

    private boolean areAllAlwaysEncrypt() {
        if(phoneNumbers.size()==0)
            return false;
        boolean areEncrypt=true;
        for(PhoneNumber phoneNumber:phoneNumbers)
            if(!phoneNumber.isAlwaysEncrypt()) {
                areEncrypt=false;
                break;
            }
        return areEncrypt;
    }

    @Override
    public void onClick(View v) {
        if(phoneNumbers.size()==0)
            return;
        if (!this.areAllInvited()) {
            def=STATES.INVITE;
            setBackgroundResource(R.drawable.send_invitation);
            onInviteButtonAction();
        } else {
            switch (def) {
                case INVITE:
                    def=STATES.PLAIN;
                    setBackgroundResource(R.drawable.sms_plain);
                    for(PhoneNumber phoneNumber:phoneNumbers) {
                        phoneNumber.setAlwaysEncrypt(false);
                        Me.getMe().getContactDAO().save(activity, phoneNumber);
                    }
                    break;
                case ENCRYPT:
                    def = STATES.PLAIN;
                    setBackgroundResource(R.drawable.sms_plain);
                    for(PhoneNumber phoneNumber:phoneNumbers) {
                        phoneNumber.setAlwaysEncrypt(false);
                        Me.getMe().getContactDAO().save(activity, phoneNumber);
                    }
                    break;
                case PLAIN:
                    def = STATES.ENCRYPT;
                    setBackgroundResource(R.drawable.sms_encrypted);
                    for(PhoneNumber phoneNumber:phoneNumbers) {
                        phoneNumber.setAlwaysEncrypt(true);
                        Me.getMe().getContactDAO().save(activity, phoneNumber);
                    }
                    break;
            }
        }
    }
}
