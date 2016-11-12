package com.kasha.kashawebapp.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.kasha.kashawebapp.services.LocatorService;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;

public class OnBootReceiver extends BroadcastReceiver {

    private SharedPreferences sharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {

        sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);


        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
                && sharedPreferences.getString("DeliveryStatus","OFF").equalsIgnoreCase("ON") ) {
            // on boot restart locator service
            Intent locationServiceIntent = new Intent(context,LocatorService.class);
            context.startService(locationServiceIntent);

        }
    }
}