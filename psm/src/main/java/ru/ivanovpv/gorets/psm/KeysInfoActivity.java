package ru.ivanovpv.gorets.psm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;

import ru.ivanovpv.gorets.psm.cipher.FingerPrint;
import ru.ivanovpv.gorets.psm.cipher.KeyExchange;
import ru.ivanovpv.gorets.psm.persistent.Contact;
import ru.ivanovpv.gorets.psm.persistent.KeyRing;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.util.Date;
import java.util.Hashtable;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 496 $
 *   $LastChangedDate: 2014-02-05 13:42:06 +0400 (Ср, 05 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/KeysInfoActivity.java $
 */
public class KeysInfoActivity extends SherlockListActivity {

    private static final String TAG = KeysInfoActivity.class.getName();
    private PhoneNumber phoneNumber;
    private Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KeyRing keys=null;
        Bundle extras=this.getIntent().getExtras();
        if(extras!=null) {
            contact=(Contact )extras.getSerializable(Constants.EXTRA_CONTACT);
            phoneNumber=(PhoneNumber )extras.getSerializable(Constants.EXTRA_PHONE_NUMBER);
            keys=phoneNumber.getPublicKeys();
        }
        setContentView(R.layout.keys_info);
        this.setTitle(R.string.respondentKeys);
        this.setListAdapter(new KeysArrayAdapter(this, keys.getKeysFingerPrints()));
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    public void onInviteButtonClicked(View v) {
        Me me=(Me)this.getApplication();
        if (!me.psmSettings.inviteWarning().get()) {
            new AlertDialog.Builder(this)
                .setTitle(KeysInfoActivity.this.getString(R.string.inviteWarningTitle))
                .setMessage(KeysInfoActivity.this.getString(R.string.inviteWarning1)+"\n"+KeysInfoActivity.this.getString(R.string.inviteWarning2))
                .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton) {
                Protocol.sendInvitation(KeysInfoActivity.this, phoneNumber);
                }
            })
            .setNegativeButton(R.string.no, null).show();
        } else {
            Protocol.sendInvitation(this, phoneNumber);
        }
    }

}


class KeysArrayAdapter extends BaseAdapter
{
	private final Context context;
	private FingerPrint[] fingers;
    private long[] dates;

	public KeysArrayAdapter(Context context, Hashtable<Long, FingerPrint> data) {
		this.context = context;
        fingers=new FingerPrint[data.size()];
        dates=new long[data.size()];
        int i=0;
        for(long time:data.keySet()) {
            dates[i]=time;
            fingers[i++]=data.get(time);
        }
	}

    @Override
    public int getCount() {
        return fingers.length;
    }

    @Override
    public Object getItem(int position) {
        return fingers[position];
    }

    @Override
    public long getItemId(int position) {
        return dates[position];
    }


    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = inflater.inflate(R.layout.key_info_row, parent, false);
		TextView finger = (TextView) rowView.findViewById(R.id.keyFingerPrint);
        TextView type=(TextView) rowView.findViewById(R.id.keyType);
		TextView date = (TextView) rowView.findViewById(R.id.keyDate);
		finger.setText(fingers[position].toString());
        String[] s=context.getResources().getStringArray(R.array.keyExchangeProtocolEntries);
        switch(fingers[position].getType()) {
            case KeyExchange.KEY_EXCHANGE_DUMMY:
                type.setText(s[0]);
                break;
            case KeyExchange.KEY_EXCHANGE_DIFFIE_HELLMAN:
                type.setText(s[1]);
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B:
                type.setText(s[2]);
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B:
                type.setText(s[3]);
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B:
                type.setText(s[4]);
                break;
            default:
                type.setText(R.string.anonymous);
        }
        date.setText(new Date(dates[position]).toString());
		return rowView;
	}
}