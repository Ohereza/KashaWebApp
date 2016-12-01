package com.kasha.kashawebapp.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by rkabagamba on 11/28/2016.
 */

public class KashaWebAppDBHelper extends SQLiteOpenHelper {
    private static KashaWebAppDBHelper sInstance;

    //increment the database version when the schema is updated
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "kashawebapp.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String DATE_TYPE = " TEXT";

    private static final String SQL_CREATE_DELIVERIES =
            "CREATE TABLE " + KashaWebAppDBContract.Deliveries.TABLE_NAME + " (" +
                    KashaWebAppDBContract.Deliveries.COLUMN_NAME_ID + INT_TYPE + "AUTOINCREMENT," +
                    KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID + TEXT_TYPE + "," +
                    KashaWebAppDBContract.Deliveries.COLUMN_NAME_TIMESTAMP + DATE_TYPE + "," +
                    KashaWebAppDBContract.Deliveries.COLUMN_NAME_STATUS + INT_TYPE + "," +
                    " PRIMARY KEY ("+KashaWebAppDBContract.Deliveries.COLUMN_NAME_ID +","
                    +KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID+"))";

    private static final String SQL_CREATE_NOTIFICATIONS =
            "CREATE TABLE " + KashaWebAppDBContract.Notifications.TABLE_NAME + " (" +
                    KashaWebAppDBContract.Notifications.COLUMN_NAME_ID + INT_TYPE + "AUTOINCREMENT,"+
                    KashaWebAppDBContract.Notifications.COLUMN_NAME_ORDER_ID + TEXT_TYPE + "," +
                    KashaWebAppDBContract.Notifications.COLUMN_NAME_MESSAGE + TEXT_TYPE + "," +
                    KashaWebAppDBContract.Notifications.COLUMN_NAME_TIMESTAMP + DATE_TYPE + "," +
                    " PRIMARY KEY ("+KashaWebAppDBContract.Deliveries.COLUMN_NAME_ID + ","
                    +KashaWebAppDBContract.Notifications.COLUMN_NAME_ORDER_ID + ","
                    +KashaWebAppDBContract.Notifications.COLUMN_NAME_MESSAGE+"))";

    private static final String SQL_DELETE_DELIVERIES =
            "DROP TABLE IF EXISTS " + KashaWebAppDBContract.Deliveries.TABLE_NAME;

    private static final String SQL_DELETE_NOTIFICATIONS =
            "DROP TABLE IF EXISTS " + KashaWebAppDBContract.Notifications.TABLE_NAME;


    public static synchronized KashaWebAppDBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KashaWebAppDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    public KashaWebAppDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DELIVERIES );
        db.execSQL(SQL_CREATE_NOTIFICATIONS );
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_DELIVERIES );
        db.execSQL(SQL_DELETE_NOTIFICATIONS );
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public boolean insertDelivery  (String order_id, String timestamp, int status)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID , order_id);
        contentValues.put(KashaWebAppDBContract.Deliveries.COLUMN_NAME_TIMESTAMP, timestamp);
        contentValues.put(KashaWebAppDBContract.Deliveries.COLUMN_NAME_STATUS, status);
        db.insert(KashaWebAppDBContract.Deliveries.TABLE_NAME, null, contentValues);
        return true;
    }

    public boolean insertNotification  (String order_id, String message, String timestamp)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KashaWebAppDBContract.Notifications.COLUMN_NAME_ORDER_ID , order_id);
        contentValues.put(KashaWebAppDBContract.Notifications.COLUMN_NAME_MESSAGE, message);
        contentValues.put(KashaWebAppDBContract.Notifications.COLUMN_NAME_TIMESTAMP, timestamp);
        db.insert(KashaWebAppDBContract.Notifications.TABLE_NAME, null, contentValues);
        return true;
    }

    public Cursor getAllOrders(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        res =  db.rawQuery( "select "+ KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID
                +" from " + KashaWebAppDBContract.Deliveries.TABLE_NAME
                + " order by "+KashaWebAppDBContract.Deliveries.COLUMN_NAME_ID +" desc", null );
        return res;
    }

    public Cursor getAllActiveOrders(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        res =  db.rawQuery( "select " + KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID
                + " from " + KashaWebAppDBContract.Deliveries.TABLE_NAME +
                " where "+KashaWebAppDBContract.Deliveries.COLUMN_NAME_STATUS
                +"="+ KashaWebAppDBContract.Deliveries.ACTIVE_STATUS, null );
        return res;
    }

    public void setDeliveryStatus(String order_id, int newStatus){
            SQLiteDatabase db = this.getWritableDatabase();
            if (newStatus == KashaWebAppDBContract.Deliveries.ACTIVE_STATUS ||
                    newStatus == KashaWebAppDBContract.Deliveries.NON_ACTIVE_STATUS) {
                db.execSQL("update " + KashaWebAppDBContract.Deliveries.TABLE_NAME +
                        " set " + KashaWebAppDBContract.Deliveries.COLUMN_NAME_STATUS + "=" + newStatus +
                        " where " + KashaWebAppDBContract.Deliveries.COLUMN_NAME_ORDER_ID +
                        "='" + order_id + "'");
            }
        }

    public Cursor getAllNotificationsToAnOrder(String order_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        res =  db.rawQuery( "select * from " + KashaWebAppDBContract.Notifications.TABLE_NAME +
                " where "+KashaWebAppDBContract.Notifications.COLUMN_NAME_ORDER_ID
                +"='"+ order_id + "'"+" order by "+
                KashaWebAppDBContract.Notifications.COLUMN_NAME_ID +" desc", null );
        return res;
    }

}
