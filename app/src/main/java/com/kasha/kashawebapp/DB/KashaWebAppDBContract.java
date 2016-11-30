package com.kasha.kashawebapp.DB;

import android.provider.BaseColumns;

/**
 * Created by rkabagamba on 11/28/2016.
 */

public class KashaWebAppDBContract {

    private KashaWebAppDBContract() {}

    /* Inner class that defines the table contents */
    public static abstract class Deliveries implements BaseColumns {
        public static final String TABLE_NAME = "deliveries";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_ORDER_ID = "order_id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_STATUS = "status";
        public static final int ACTIVE_STATUS = 1;
        public static final int NON_ACTIVE_STATUS = 2;
    }

    /* Inner class that defines the table contents */
    public static abstract class Notifications implements BaseColumns {
        public static final String TABLE_NAME = "notifications";
        public static final String COLUMN_NAME_ORDER_ID = "order_id";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

}
