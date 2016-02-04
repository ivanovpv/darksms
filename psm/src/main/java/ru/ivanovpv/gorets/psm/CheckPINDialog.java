package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Purse;

import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 412 $
 *   $LastChangedDate: 2013-11-11 16:39:28 +0400 (Пн, 11 ноя 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/CheckPINDialog.java $
 */

public class CheckPINDialog extends Dialog implements TextWatcher, View.OnKeyListener
{
    private final static String TAG = CheckPINDialog.class.getName();
    public final static int MODE_DOESNT_MATTER=-1;
    public final static int MODE_CLEAR_CHECK=0;
    public final static int MODE_SET_CHECK=1;

    private EditText pinChar1, pinChar2, pinChar3, pinChar4;
    private TextView errorText;
    private volatile boolean onClear=false;
    private volatile boolean onDelete=false;
    private boolean canceled;
//    private Button ok, cancel;
    private Activity activity;
    private Me me;
    private int mode;

    ArrayList<EditText> cells;

    public CheckPINDialog(Activity activity, int mode) {
        super(activity);
        this.activity=activity;
        me =(Me )activity.getApplication();
        this.mode=mode;
    }

    @Override
    public void onCreate(Bundle onSavedInstanceState) {
        super.onCreate(onSavedInstanceState);
        this.setContentView(R.layout.password_layout);
        this.setTitle(activity.getString(R.string.enterPin));
        errorText=(TextView )this.findViewById(R.id.errorText);
/*        ok=(Button )this.findViewById(R.id.ok);
        cancel=(Button )this.findViewById(R.id.cancel);
        ok.setOnClickListener(this);
        cancel.setOnClickListener(this);*/

        pinChar1 =(EditText )this.findViewById(R.id.pinChar1);
        pinChar2 =(EditText )this.findViewById(R.id.pinChar2);
        pinChar3 =(EditText )this.findViewById(R.id.pinChar3);
        pinChar4 =(EditText )this.findViewById(R.id.pinChar4);
        pinChar1.setOnKeyListener(this);
        pinChar2.setOnKeyListener(this);
        pinChar3.setOnKeyListener(this);
        pinChar4.setOnKeyListener(this);
        cells= new ArrayList<EditText>();
            cells.add(pinChar1);
            cells.add(pinChar2);
            cells.add(pinChar3);
            cells.add(pinChar4);
        pinChar1.requestFocus();
        pinChar1.addTextChangedListener(this);
        pinChar2.addTextChangedListener(this);
        pinChar3.addTextChangedListener(this);
        pinChar4.addTextChangedListener(this);

        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
    }

    public String getPassword() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(pinChar1.getText());
            stringBuilder.append(pinChar2.getText());
            stringBuilder.append(pinChar3.getText());
            stringBuilder.append(pinChar4.getText());
        return stringBuilder.toString();
    }

    private boolean isFull() {
        if(pinChar1.getText().length() > 0 && pinChar2.getText().length() > 0 && pinChar3.getText().length() > 0 && pinChar4.getText().length() > 0)
            return true;
        return false;
    }

    private void clearPasswordLine() {
        onClear=true;
        pinChar1.setText("");
        pinChar2.setText("");
        pinChar3.setText("");
        pinChar4.setText("");
        pinChar1.requestFocus();
        onClear=false;
    }

    public void checkPin()
    {
        Hash hash=me.getHashDAO().get();
        if(hash.checkPassword(this.getPassword()))  {
            me.setLogged(true);
            Purse purse=Me.getMe().getMessagePurseDAO().get(this.getContext());
            if(purse.isComfortPIN()) {
                purse.setComfortPINEnabled(true);
                Me.getMe().getMessagePurseDAO().save(this.getContext(), purse);
            }
            this.dismiss();
            canceled=false;
        }
        else {
            errorText.setText(activity.getText(R.string.incorrectPin));
            pinChar1.requestFocus();
        }
        clearPasswordLine();
    }

    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        if(onDelete) { //don't fire listener for backspace
            onDelete=false;
            return;
        }
        if(this.pinChar1.hasFocus()) {
            if(isFull())
                this.checkPin();
            this.pinChar2.requestFocus();
        } else if(this.pinChar2.hasFocus()) {
            if(isFull())
                this.checkPin();
            this.pinChar3.requestFocus();
        } else if(this.pinChar3.hasFocus()) {
            if(isFull())
                this.checkPin();
            this.pinChar4.requestFocus();
        } else if(this.pinChar4.hasFocus()) {
            if(isFull())
                this.checkPin();
            this.pinChar1.requestFocus();
        }
    }

    public int getMode() {
        return this.mode;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction()==KeyEvent.ACTION_DOWN) {
            if(this.pinChar1.hasFocus()) {
                onDelete=true;
                pinChar1.setText("");
                this.pinChar1.requestFocus();
                return true;
            }
            if(this.pinChar2.hasFocus()) {
                onDelete=true;
                pinChar2.setText("");
                this.pinChar1.requestFocus();
                return true;
            }
            if(this.pinChar3.hasFocus()) {
                onDelete=true;
                pinChar3.setText("");
                this.pinChar2.requestFocus();
                return true;
            }
            if(this.pinChar4.hasFocus()) {
                onDelete=true;
                pinChar4.setText("");
                this.pinChar3.requestFocus();
                return true;
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN) {
            this.cancel();
            canceled=true;
            return true;
        }
        return false;
    }

    public boolean isCanceled()
    {
        return canceled;
    }

    public void setCanceled(boolean canceled)
    {
        this.canceled=canceled;
    }
}