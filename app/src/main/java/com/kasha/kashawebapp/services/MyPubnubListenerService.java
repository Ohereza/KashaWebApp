package com.kasha.kashawebapp.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.helper.Configs;
import com.kasha.kashawebapp.views.MainActivity;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;

/**
 * Created by rkabagamba on 11/2/2016.
 */


public class MyPubnubListenerService extends IntentService {

    private SharedPreferences sharedPreferences;
    private static final String TAG_PUBNUBLISTENER = "MyPubnubListenerService";
    private PNConfiguration pnConfiguration;
    private PubNub pubnub;
    private String orderKey;

    private LatLng clientLocation;
    private boolean zoomToClient = true;

    private PolylineOptions mPolylineOptions;

    public MyPubnubListenerService() {
        super("MyPubnubListenerService");
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Pubnub listener service started", Toast.LENGTH_SHORT).show();
        super.onCreate();

        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(Configs.pubnub_subscribeKey);
        pnConfiguration.setPublishKey(Configs.pubnub_publishKey);
        pubnub = new PubNub(pnConfiguration);

        // Get username
        orderKey = sharedPreferences.getString("orderKey",null);
        // Subscribe to a channel - the same as the username
        pubnub.subscribe().channels(Arrays.asList(orderKey)).execute();

        Log.v(TAG_PUBNUBLISTENER, "order id: "+orderKey);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        // Listen for incoming messages
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                } else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc
                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    }
                } else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                } else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {
                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {

/*                if ( ! MyApplication.isMapActivityVisible() ) {*/
                    // Handle new message stored in message.message
                    Log.v(TAG_PUBNUBLISTENER, "message(" + message.getMessage() + ")");
                    Log.v(TAG_PUBNUBLISTENER, "MAP NOT VISIBLE");
                    // {"order_id":"f88d553b6b","type":"Delivery Request"}
                    JSONObject jsonRequest = null;

                    try {
                        jsonRequest = new JSONObject(String.valueOf(message.getMessage()));
                        Log.v(TAG_PUBNUBLISTENER, "json object: " + jsonRequest);

                        if (jsonRequest != null && jsonRequest.has("type")
                                && jsonRequest.getString("type").equalsIgnoreCase("Delivering")) {

                            Log.v(TAG_PUBNUBLISTENER, "Opening a notification bar");
                            notifyUser(1);

                        }else if (jsonRequest != null && jsonRequest.has("type")
                                    && jsonRequest.getString("type").equalsIgnoreCase("Delivered")) {

                            notifyUser(2);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

/*                } else {
                    // if the map is visible don't handle
                    // pubnub requests from here.
                    stopSelf();
                }*/
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            }

            // notificationType = 1: delivering
            // notificationType = 2: delivered
            private void notifyUser(int notificationType){
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.notification_icon);
                mBuilder.setContentTitle("Kasha Delivery");

                if (notificationType == 1){
                    mBuilder.setContentText("Your delivery is under way and" +
                            " progress can be visualized from the map.\n" +
                            "Thank you for shopping with us");
                }
                else if (notificationType == 2){
                    mBuilder.setContentText("Your delivery is completed.\n" +
                            "Thank you for shopping with us");
                }

                mBuilder.setAutoCancel(true);

                // Start of MainActivity on click
                Intent resultIntent = new Intent(MyPubnubListenerService.this,
                        MainActivity.class);
                resultIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(MyPubnubListenerService.this, 1,
                                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager =
                        (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

                mNotificationManager.notify(0, mBuilder.build());

                mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
                mBuilder.setLights(Color.RED, 3000, 3000);
                mBuilder.setVibrate(new long[]{1000, 1000});

/*                            Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(500);*/

                // stop tracking if delivery is completed
                if (notificationType == 2){
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("DeliveryStatus","ON");
                    editor.apply();
                }
            }
        });
    }
}
