package ru.ivanovpv.gorets.psm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 17.09.13
 * Time: 10:02
 * To change this template use File | Settings | File Templates.
 */
public class UninstallIntentReceiver extends BroadcastReceiver {

    private final static String TAG=UninstallIntentReceiver.class.getName();

    @Override
        public void onReceive(Context context, Intent intent) {
            // fetching package names from extras
            String[] packageNames = intent.getStringArrayExtra("android.intent.extra.PACKAGES");

            if(packageNames!=null) {
                for(String packageName: packageNames){
                    if(packageName!=null && packageName.equals("ru.ivanovpv.gorets.psm")){
                        // User has selected our application under the Manage Apps settings
                        // now initiating background thread to watch for activity
                        new ListenActivities(context).start();
                    }
                }
            }
        }

}
