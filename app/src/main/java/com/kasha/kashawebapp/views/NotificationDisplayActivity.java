package com.kasha.kashawebapp.views;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.kasha.kashawebapp.DB.KashaWebAppDBHelper;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.adapter.MessageListAdapter;
import com.kasha.kashawebapp.helper.Msg;
import com.kasha.kashawebapp.helper.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by rkabagamba on 12/2/2016.
 */

public class NotificationDisplayActivity extends AppCompatActivity {

    private KashaWebAppDBHelper mydb;
    private String selectedOrder;

    @Override
    protected void attachBaseContext(Context context) {
        //super.attachBaseContext(context);
        super.attachBaseContext(CalligraphyContextWrapper.wrap(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_display);

        mydb = KashaWebAppDBHelper.getInstance(this);
        // Start the chatview
        ListView listMsg;
        listMsg = (ListView) findViewById(R.id.notifications_list_view);
        ArrayList<Msg> listMessages;
        MessageListAdapter adapter;
        listMessages = new ArrayList<Msg>();

        selectedOrder = getIntent().getExtras().getString("order");
        LinkedHashMap<String,String> notifications = Util.getMapFromCursor(
                mydb.getAllNotificationsToAnOrder(selectedOrder));

        for(String timestamp:notifications.keySet()){
            listMessages.add(new Msg("", "", notifications.get(timestamp), "", false, "", "", timestamp ));
        }

        adapter = new MessageListAdapter(getApplicationContext(), listMessages);
        listMsg.setAdapter(adapter);

    }

}
