package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ru.ivanovpv.gorets.psm.persistent.Hash;

import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 448 $
 *   $LastChangedDate: 2013-12-09 18:05:31 +0400 (Пн, 09 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/SetPINDialog.java $
 */

public class SetPINDialog extends Dialog implements TextWatcher, View.OnClickListener, View.OnKeyListener {
    private final static String TAG = SetPINDialog.class.getName();

    EditText pinChar1, pinChar2, pinChar3, pinChar4;
    EditText confirmPinChar1, confirmPinChar2, confirmPinChar3, confirmPinChar4;
    Button ok, cancel;
    TextView errorText;
    Me me;
    private boolean canceled;

    private Context context;
    private volatile boolean onDelete=false;

    ArrayList<EditText> cells;

    public SetPINDialog(Activity activity) {
        super(activity);
        this.context=activity;
        me=(Me )activity.getApplication();
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.password_set_layout);
        this.setTitle(R.string.setPin);
        pinChar1 =(EditText )findViewById(R.id.pinChar1);
        pinChar2 =(EditText )findViewById(R.id.pinChar2);
        pinChar3 =(EditText )findViewById(R.id.pinChar3);
        pinChar4 =(EditText )findViewById(R.id.pinChar4);
        confirmPinChar1 =(EditText )findViewById(R.id.confirmPinChar1);
        confirmPinChar2 =(EditText )findViewById(R.id.confirmPinChar2);
        confirmPinChar3 =(EditText )findViewById(R.id.confirmPinChar3);
        confirmPinChar4 =(EditText )findViewById(R.id.confirmPinChar4);
        ok=(Button )findViewById(R.id.ok);
        cancel=(Button )findViewById(R.id.cancel);
        ok.setOnClickListener(this);
        cancel.setOnClickListener(this);
        errorText=(TextView )findViewById(R.id.errorText);
        pinChar1.requestFocus();

        pinChar1.addTextChangedListener(this);
        pinChar2.addTextChangedListener(this);
        pinChar3.addTextChangedListener(this);
        pinChar4.addTextChangedListener(this);
        pinChar1.setOnKeyListener(this);
        pinChar2.setOnKeyListener(this);
        pinChar3.setOnKeyListener(this);
        pinChar4.setOnKeyListener(this);

        confirmPinChar1.addTextChangedListener(this);
        confirmPinChar2.addTextChangedListener(this);
        confirmPinChar3.addTextChangedListener(this);
        confirmPinChar4.addTextChangedListener(this);
        confirmPinChar1.setOnKeyListener(this);
        confirmPinChar2.setOnKeyListener(this);
        confirmPinChar3.setOnKeyListener(this);
        confirmPinChar4.setOnKeyListener(this);
    }

    public String getPassword() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(pinChar1.getText());
            stringBuilder.append(pinChar2.getText());
            stringBuilder.append(pinChar3.getText());
            stringBuilder.append(pinChar4.getText());
        return stringBuilder.toString();
    }

    public String getVerify() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(confirmPinChar1.getText());
            stringBuilder.append(confirmPinChar2.getText());
            stringBuilder.append(confirmPinChar3.getText());
            stringBuilder.append(confirmPinChar4.getText());
        return stringBuilder.toString();
    }

    @Override
    public void onClick(View view)
    {
        if(view.getId()==R.id.cancel) {
            clearFields();
            this.cancel();
            canceled=true;
            return;
        }
        String password=this.getPassword();
        String verify=this.getVerify();
        if(password==null || verify==null || password.length()!=4 || verify.length()!=4)
        {
            clearFields();
            errorText.setText(R.string.pinShouldBeEntered);
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        if(password.compareTo(verify)!=0)
        {
            clearFields();
            errorText.setText(R.string.pinsAreNotInentical);
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        Hash hash=new Hash(password, Me.getMe().getHashDAO().get());
        me.getHashDAO().save(context, hash);
        this.dismiss();
        this.canceled=false;
        return;
    }

    private void clearFields() {
        pinChar1.setText("");
        pinChar2.setText("");
        pinChar3.setText("");
        pinChar4.setText("");

        confirmPinChar1.setText("");
        confirmPinChar2.setText("");
        confirmPinChar3.setText("");
        confirmPinChar4.setText("");
        pinChar1.requestFocus();
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void afterTextChanged(Editable editable) {
        if(onDelete) { //don't fire listener for backspace
            onDelete=false;
            return;
        }
        if(this.pinChar1.hasFocus()) {
            pinChar2.requestFocus();
        } else if(this.pinChar2.hasFocus()) {
            pinChar3.requestFocus();
        } else if(this.pinChar2.hasFocus()) {
            pinChar3.requestFocus();
        } else if(this.pinChar3.hasFocus()) {
            pinChar4.requestFocus();
        } else if(this.pinChar4.hasFocus()) {
            confirmPinChar1.requestFocus();
        } else if(this.confirmPinChar1.hasFocus()) {
            confirmPinChar2.requestFocus();
        } else if(this.confirmPinChar2.hasFocus()) {
            confirmPinChar3.requestFocus();
        } else if(this.confirmPinChar3.hasFocus()) {
            confirmPinChar4.requestFocus();
        } else if(this.confirmPinChar4.hasFocus()) {
            ok.requestFocus();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
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
            if(this.confirmPinChar1.hasFocus()) {
                onDelete=true;
                confirmPinChar1.setText("");
                this.pinChar4.requestFocus();
                return true;
            }
            if(this.confirmPinChar2.hasFocus()) {
                onDelete=true;
                confirmPinChar2.setText("");
                this.confirmPinChar1.requestFocus();
                return true;
            }
            if(this.confirmPinChar3.hasFocus()) {
                onDelete=true;
                confirmPinChar3.setText("");
                this.confirmPinChar2.requestFocus();
                return true;
            }
            if(this.confirmPinChar4.hasFocus()) {
                onDelete=true;
                confirmPinChar4.setText("");
                this.confirmPinChar3.requestFocus();
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