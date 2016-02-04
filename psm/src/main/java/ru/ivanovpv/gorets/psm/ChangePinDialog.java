/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Alexander Laschenko 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 456 $
 *    $LastChangedDate: 2013-12-13 13:22:35 +0400 (Пт, 13 дек 2013) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/ChangePinDialog.java $
 */

package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.Dialog;
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

public class ChangePinDialog extends Dialog implements TextWatcher, View.OnClickListener, View.OnKeyListener  {
    private EditText oldPinChar1, oldPinChar2, oldPinChar3, oldPinChar4;
    private EditText newPinChar1, newPinChar2, newPinChar3, newPinChar4;
    private EditText confirmPinChar1, confirmPinChar2, confirmPinChar3, confirmPinChar4;
    private Button ok;
    private Me me;
    private Activity activity;
    private TextView errorText;
    private volatile boolean onDelete=false;
    private boolean canceled;

    public ChangePinDialog(Activity activity) {
        super(activity);
        this.activity=activity;
        me =(Me )activity.getApplication();    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.change_password_dialog);
        ok = (Button)findViewById(R.id.ok);
        ok.setOnClickListener(this);
        Button button = (Button)findViewById(R.id.cancel);
        button.setOnClickListener(this);
        errorText=(TextView)this.findViewById(R.id.errorText);

        oldPinChar1 =(EditText)this.findViewById(R.id.oldPinChar1);
        oldPinChar2 =(EditText )this.findViewById(R.id.oldPinChar2);
        oldPinChar3 =(EditText )this.findViewById(R.id.oldPinChar3);
        oldPinChar4 =(EditText )this.findViewById(R.id.oldPinChar4);
        oldPinChar1.requestFocus();
        oldPinChar1.addTextChangedListener(this);
        oldPinChar2.addTextChangedListener(this);
        oldPinChar3.addTextChangedListener(this);
        oldPinChar4.addTextChangedListener(this);
        oldPinChar1.setOnKeyListener(this);
        oldPinChar2.setOnKeyListener(this);
        oldPinChar3.setOnKeyListener(this);
        oldPinChar4.setOnKeyListener(this);

        newPinChar1 =(EditText)this.findViewById(R.id.newPinChar1);
        newPinChar2 =(EditText )this.findViewById(R.id.newPinChar2);
        newPinChar3 =(EditText )this.findViewById(R.id.newPinChar3);
        newPinChar4 =(EditText )this.findViewById(R.id.newPinChar4);
        newPinChar1.addTextChangedListener(this);
        newPinChar2.addTextChangedListener(this);
        newPinChar3.addTextChangedListener(this);
        newPinChar4.addTextChangedListener(this);
        newPinChar1.setOnKeyListener(this);
        newPinChar2.setOnKeyListener(this);
        newPinChar3.setOnKeyListener(this);
        newPinChar4.setOnKeyListener(this);

        confirmPinChar1 =(EditText)this.findViewById(R.id.confirmPinChar1);
        confirmPinChar2 =(EditText )this.findViewById(R.id.confirmPinChar2);
        confirmPinChar3 =(EditText )this.findViewById(R.id.confirmPinChar3);
        confirmPinChar4 =(EditText )this.findViewById(R.id.confirmPinChar4);
        confirmPinChar1.addTextChangedListener(this);
        confirmPinChar2.addTextChangedListener(this);
        confirmPinChar3.addTextChangedListener(this);
        confirmPinChar4.addTextChangedListener(this);
        confirmPinChar1.setOnKeyListener(this);
        confirmPinChar2.setOnKeyListener(this);
        confirmPinChar3.setOnKeyListener(this);
        confirmPinChar4.setOnKeyListener(this);
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
        if(this.oldPinChar1.hasFocus()) {
            this.oldPinChar2.requestFocus();
        } else if(this.oldPinChar2.hasFocus()) {
            this.oldPinChar3.requestFocus();
        } else if(this.oldPinChar3.hasFocus()) {
            this.oldPinChar4.requestFocus();
        } else if(this.oldPinChar4.hasFocus()) {
            this.newPinChar1.requestFocus();
        } else if(this.newPinChar1.hasFocus()) {
            this.newPinChar2.requestFocus();
        } else if(this.newPinChar2.hasFocus()) {
            this.newPinChar3.requestFocus();
        } else if(this.newPinChar3.hasFocus()) {
            this.newPinChar4.requestFocus();
        } else if(this.newPinChar4.hasFocus()) {
            this.confirmPinChar1.requestFocus();
        } else if(this.confirmPinChar1.hasFocus()) {
            this.confirmPinChar2.requestFocus();
        } else if(this.confirmPinChar2.hasFocus()) {
            this.confirmPinChar3.requestFocus();
        } else if(this.confirmPinChar3.hasFocus()) {
            this.confirmPinChar4.requestFocus();
        } else if(this.confirmPinChar4.hasFocus()) {
            this.ok.requestFocus();
        }
    }

    private String getOldPassword() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(oldPinChar1.getText());
            stringBuilder.append(oldPinChar2.getText());
            stringBuilder.append(oldPinChar3.getText());
            stringBuilder.append(oldPinChar4.getText());
        return stringBuilder.toString();
    }

    private String getNewPassword() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(newPinChar1.getText());
            stringBuilder.append(newPinChar2.getText());
            stringBuilder.append(newPinChar3.getText());
            stringBuilder.append(newPinChar4.getText());
        return stringBuilder.toString();
    }

    private String getNewPasswordVerify() {
        StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(confirmPinChar1.getText());
            stringBuilder.append(confirmPinChar2.getText());
            stringBuilder.append(confirmPinChar3.getText());
            stringBuilder.append(confirmPinChar4.getText());
        return stringBuilder.toString();
    }

    private void clearOldPassword() {
        oldPinChar1.setText("");
        oldPinChar2.setText("");
        oldPinChar3.setText("");
        oldPinChar4.setText("");
    }

    private void clearNewPasswords() {
        newPinChar1.setText("");
        newPinChar2.setText("");
        newPinChar3.setText("");
        newPinChar4.setText("");

        confirmPinChar1.setText("");
        confirmPinChar2.setText("");
        confirmPinChar3.setText("");
        confirmPinChar4.setText("");
        oldPinChar1.requestFocus();
    }


    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.cancel) {
            this.cancel();
            this.canceled=true;
            return;
        }
        String password=this.getOldPassword();
        if(password==null || password.length()!=4) {
            errorText.setText(R.string.pinShouldBeEntered);
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        ArrayList<Hash> hashes=me.getHashDAO().getAll(activity);
        if(hashes==null || hashes.size()==0)
            return;
        if(!hashes.get(0).checkPassword(password))  {
            errorText.setText(activity.getText(R.string.incorrectPin));
            errorText.setVisibility(View.VISIBLE);
            clearOldPassword();
            oldPinChar1.requestFocus();
            return;
        }
        password=this.getNewPassword();
        String verify=this.getNewPasswordVerify();
        if(password==null || verify==null || password.length()!=4 || verify.length()!=4) {
            clearNewPasswords();
            errorText.setText(R.string.pinShouldBeEntered);
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        if(password.compareTo(verify)!=0) {
            errorText.setText(R.string.pinsAreNotInentical);
            errorText.setVisibility(View.VISIBLE);
            clearNewPasswords();
            newPinChar1.requestFocus();
            return;
        }
        Hash hash=new Hash(password, hashes.get(0));
        me.getHashDAO().save(activity, hash);
        errorText.setVisibility(View.GONE);
        this.dismiss();
        this.canceled=false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction()==KeyEvent.ACTION_DOWN) {
            if(this.oldPinChar1.hasFocus()) {
                onDelete=true;
                oldPinChar1.setText("");
                this.oldPinChar1.requestFocus();
                return true;
            }
            if(this.oldPinChar2.hasFocus()) {
                onDelete=true;
                oldPinChar2.setText("");
                this.oldPinChar1.requestFocus();
                return true;
            }
            if(this.oldPinChar3.hasFocus()) {
                onDelete=true;
                oldPinChar3.setText("");
                this.oldPinChar2.requestFocus();
                return true;
            }
            if(this.oldPinChar4.hasFocus()) {
                onDelete=true;
                oldPinChar4.setText("");
                this.oldPinChar3.requestFocus();
                return true;
            }
            if(this.newPinChar1.hasFocus()) {
                onDelete=true;
                newPinChar1.setText("");
                this.oldPinChar4.requestFocus();
                return true;
            }
            if(this.newPinChar2.hasFocus()) {
                onDelete=true;
                newPinChar2.setText("");
                this.newPinChar1.requestFocus();
                return true;
            }
            if(this.newPinChar3.hasFocus()) {
                onDelete=true;
                newPinChar3.setText("");
                this.newPinChar2.requestFocus();
                return true;
            }
            if(this.newPinChar4.hasFocus()) {
                onDelete=true;
                newPinChar4.setText("");
                this.newPinChar3.requestFocus();
                return true;
            }
            if(this.confirmPinChar1.hasFocus()) {
                onDelete=true;
                confirmPinChar1.setText("");
                this.newPinChar4.requestFocus();
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
            this.canceled=true;
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
