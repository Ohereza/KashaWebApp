package com.kasha.kashawebapp.helper;

/**
 * Created by rkabagamba on 9/28/2016.
 */

public class Configs {
    public static final String PREFS_NAME = "MyPrefsFile";

    // related to the server connector class
    public static String serverAddress = "http://pds.intego.rw:8000";
    public static String loginRelatedUri = "/api/method/login";
    public static String updateLocationUri = "/api/resource/Location";

    //pubnub configs
    public static String pubnub_subscribeKey = "sub-c-266bcbc0-9884-11e6-b146-0619f8945a4f";
    public static String pubnub_publishKey = "pub-c-21663d8a-850d-4d99-adb3-3dda55a02abd";

    // credentials for connection to the backend
    public static String bckend_username = "Administrator";
    public static String bckend_password = "pds";


}
