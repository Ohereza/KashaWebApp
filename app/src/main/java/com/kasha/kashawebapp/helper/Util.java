package com.kasha.kashawebapp.helper;

import android.database.Cursor;

import com.kasha.kashawebapp.DB.KashaWebAppDBContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Created by rkabagamba on 11/29/2016.
 */

public class Util {

    public Util(){
    }

    public static String getCurrentTimestamp(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyy  HH:mm:ss");
        String timestamp = simpleDateFormat.format(new Date());
        return timestamp;
    }

    public static ArrayList<String> getStringArrayFromColumnCursor(Cursor rs){
        ArrayList<String> array = new ArrayList<String>();

        rs.moveToFirst();

        if (rs.getCount() > 0){
            do {
                array.add(rs.getString(0));
            } while ( rs.moveToNext() );
        }

        if(rs!=null){
            rs.close();
        }
        return array;
    }

    public static LinkedHashMap<String,String> getMapFromCursor(Cursor rs){
        LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();

        rs.moveToFirst();

        if (rs.getCount() > 0){
            do {
                map.put(rs.getString(rs.getColumnIndexOrThrow(KashaWebAppDBContract.Notifications.COLUMN_NAME_TIMESTAMP)),
                        rs.getString(rs.getColumnIndexOrThrow(KashaWebAppDBContract.Notifications.COLUMN_NAME_MESSAGE)));
            } while ( rs.moveToNext() );
        }

        if(rs!=null){
            rs.close();
        }
        return map;
    }

}
