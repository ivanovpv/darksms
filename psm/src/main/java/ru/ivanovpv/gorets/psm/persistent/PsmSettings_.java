package ru.ivanovpv.gorets.psm.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import org.androidannotations.api.sharedpreferences.BooleanPrefEditorField;
import org.androidannotations.api.sharedpreferences.BooleanPrefField;
import org.androidannotations.api.sharedpreferences.EditorHelper;
import org.androidannotations.api.sharedpreferences.IntPrefEditorField;
import org.androidannotations.api.sharedpreferences.IntPrefField;
import org.androidannotations.api.sharedpreferences.LongPrefEditorField;
import org.androidannotations.api.sharedpreferences.LongPrefField;
import org.androidannotations.api.sharedpreferences.SharedPreferencesHelper;
import org.androidannotations.api.sharedpreferences.StringPrefEditorField;
import org.androidannotations.api.sharedpreferences.StringPrefField;
import ru.ivanovpv.gorets.psm.Me;

/**
 * Got from android annotations!!!!
 * Solely purpose is to define SharedPreferences name, since annotations does not permit to set user defined name of
 * preferences
 *
 * Need to be regenerated if PsmSettings.java will be edited!
 */
public final class PsmSettings_ extends SharedPreferencesHelper
{


    public PsmSettings_(Context context) {
        super(context.getSharedPreferences(Me.PREFERENCES_NAME, 0));
    }

   /* private static String getLocalClassName(Context context) {
        String packageName = context.getPackageName();
        String className = context.getClass().getName();
        int packageLen = packageName.length();
        if (((!className.startsWith(packageName))||(className.length()<= packageLen))||(className.charAt(packageLen)!= '.')) {
            return className;
        }
        return className.substring((packageLen + 1));
    }*/

    public PsmSettings_.PsmSettingsEditor_ edit() {
        return new PsmSettings_.PsmSettingsEditor_(getSharedPreferences());
    }

    public StringPrefField deviceSalt() {
        return stringField("deviceSalt", "");
    }

    public StringPrefField passwordSalt() {
        return stringField("passwordSalt", "");
    }

    public IntPrefField rateMe() {
        return intField("rateMe", 5);
    }

    public IntPrefField defaultPage() {
        return intField("defaultPage", 0);
    }

    public BooleanPrefField rateApp() {
        return booleanField("rateApp", true);
    }

    public BooleanPrefField eulaAccepted() {
        return booleanField("eulaAccepted", false);
    }

    public BooleanPrefField inviteWarning() {
        return booleanField("inviteWarning", false);
    }

    public BooleanPrefField additionalChargeWarning() {
        return booleanField("additionalChargeWarning", false);
    }

    public BooleanPrefField firstRun() {
        return booleanField("firstRun", true);
    }

    public StringPrefField installationId() {
        return stringField("installationId", "");
    }

    public StringPrefField protectionPrivacyLevel() {
        return (Me.MILITARY) ? stringField("protectionPrivacyLevel", "2") : stringField("protectionPrivacyLevel", "0");
    }

    public StringPrefField messagingPrivacyLevel() {
        return (Me.MILITARY) ? stringField("messagingPrivacyLevel", "2") : stringField("messagingPrivacyLevel", "0");
    }

    public StringPrefField smsRingTone() {
        return stringField("smsRingTone", "");
    }

    public BooleanPrefField askPIN() {
        return booleanField("askPIN", false);
    }

    public BooleanPrefField checkDefaultSMSApplication() {
        return booleanField("checkDefaultSMSApplication", true);
    }

    public final static class PsmSettingsEditor_
        extends EditorHelper<PsmSettings_.PsmSettingsEditor_>
    {


        PsmSettingsEditor_(SharedPreferences sharedPreferences) {
            super(sharedPreferences);
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> deviceSalt() {
            return stringField("deviceSalt");
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> passwordSalt() {
            return stringField("passwordSalt");
        }

        public IntPrefEditorField<PsmSettings_.PsmSettingsEditor_> rateMe() {
            return intField("rateMe");
        }

        public IntPrefEditorField<PsmSettings_.PsmSettingsEditor_> defaultPage() {
            return intField("defaultPage");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> rateApp() {
            return booleanField("rateApp");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> eulaAccepted() {
            return booleanField("eulaAccepted");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> inviteWarning() {
            return booleanField("inviteWarning");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> additionalChargeWarning() {
            return booleanField("additionalChargeWarning");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> firstRun() {
            return booleanField("firstRun");
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> installationId() {
            return stringField("installationId");
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> protectionPrivacyLevel() {
            return stringField("protectionPrivacyLevel");
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> messagingPrivacyLevel() {
            return stringField("messagingPrivacyLevel");
        }

        public StringPrefEditorField<PsmSettings_.PsmSettingsEditor_> smsRingTone() {
            return stringField("smsRingTone");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> askPIN() {
            return booleanField("askPIN");
        }

        public BooleanPrefEditorField<PsmSettings_.PsmSettingsEditor_> checkDefaultSMSApplication() {
            return booleanField("checkDefaultSMSApplication");
        }

    }

}
