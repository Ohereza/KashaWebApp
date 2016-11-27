package com.kasha.kashawebapp.helper;

import android.app.Application;

import com.kasha.kashawebapp.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by rkabagamba on 11/27/2016.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Medium.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }

}
