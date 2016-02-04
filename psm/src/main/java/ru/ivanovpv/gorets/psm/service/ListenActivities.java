package ru.ivanovpv.gorets.psm.service;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 17.09.13
 * Time: 10:05
 * To change this template use File | Settings | File Templates.
 */
public class ListenActivities extends Thread {
    private final static String TAG=ListenActivities.class.getName();
    boolean exit = false;
    ActivityManager am = null;
    Context context = null;

    public ListenActivities(Context context){
        this.context = context;
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void run() {
        boolean flag=false;

        Looper.prepare();

        while(!exit) {
             // get the info from the currently running task
            List< ActivityManager.RunningTaskInfo > taskInfo = am.getRunningTasks(MAX_PRIORITY);

            String activityName = taskInfo.get(0).topActivity.getClassName();
            while(activityName.contains("/")) {
                int index=activityName.indexOf('/');
                String s1=activityName.substring(0, index);
                String s2=activityName.substring(index+1);
                activityName=s1.concat(s2);
            }
            //Log.d(TAG, "CURRENT Activity ::" + activityName);

            if (activityName.equals("com.android.packageinstaller.UninstallerActivity")) {
               Log.i(TAG, "You are going to uninstall PSM!");
                // User has clicked on the Uninstall button under the Manage Apps settings

                 //do whatever pre-uninstallation task you want to perform here
                 // show dialogue or start another activity or database operations etc..etc..

                 // context.startActivity(new Intent(context, MyPreUninstallationMsgActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                 exit = true;
                 Toast.makeText(context, "Done with preuninstallation tasks... Exiting Now", Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Activity name="+activityName);
                if(activityName.equals("com.android.settings.applications.ManageApplications")) {
                    // back button was pressed and the user has been taken back to Manage Applications window
                    // we should close the activity monitoring now
                    exit=true;
                }
            }
        }
        Looper.loop();
    }
}