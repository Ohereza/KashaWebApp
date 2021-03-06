package com.kasha.kashawebapp.helper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Created by rkabagamba on 10/13/2016.
 */

public class LocationUpdater {

    @Expose
    public String order_number;
    @Expose
    public String type;
    @Expose
    public String longitude;
    @Expose
    public String latitude;


    public LocationUpdater( String order_id, String mtype, String mlongitude, String mlatitude) {

        order_number = order_id;
        type = mtype;
        longitude = mlongitude;
        latitude = mlatitude;

    }

    private String getJsonString() {
        // Before converting to GSON check value of id
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }
}
