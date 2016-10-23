package com.kasha.kashawebapp.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.services.LocatorService;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ACCESS_FINE_LOCATION = 0;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest;
    private String orderKey;


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private WebView myWebView;
    private FloatingActionButton fabMap;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);


        mVisible = true;
        myWebView = (WebView) findViewById(R.id.webview);
        fabMap = (FloatingActionButton) findViewById(R.id.fabMap);
        fabMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent mapIntent = new Intent( MainActivity.this ,MapsActivity.class );
                startActivity(mapIntent);
            }
        });

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        //myWebView.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        //myWebView.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        myWebView.getSettings().setAllowFileAccess( true );
        //myWebView.getSettings().setAppCacheEnabled( true );
        myWebView.getSettings().setJavaScriptEnabled( true );
        myWebView.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default

        if ( !isNetworkAvailable() ) { // loading offline
            myWebView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }

        myWebView.loadUrl("http://ec2-52-57-159-28.eu-central-1.compute.amazonaws.com/");
        //myWebView.loadUrl("http://www.google.com/");

        myWebView.setWebViewClient(new WebViewClient() {

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                //multiple split to ensure even if the url format change it doesn't break easily
                String[] linkContent = url.split("\\/");
                String[] linkContent1 = (linkContent[linkContent.length-1]). split("\\?");
                String[] linkContent2 = (linkContent1[linkContent1.length-1]).split("=");
                orderKey = linkContent2[linkContent2.length-1];

                if(orderKey.startsWith("wc_order_")) {
                    Log.d(TAG, "new orderKey: "+orderKey);
                    // Build the alert dialog
 /*                   AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
                    builder.setTitle("NOTICE");
                    builder.setMessage("Using the Premium Delivery service will enable GPS " +
                            "and other Location Services. " +
                            "This in order to collect your location and deliver your package where you are. ");

                    builder.setNegativeButton("Disagree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            promptDropOffPoint();
                        }
                    });
                    builder.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {*/
                            trackUserLocation();
                            fabMap.setVisibility(View.VISIBLE);
                        /*}
                    });

                    Dialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();*/

                }
                view.loadUrl(url);
                return false;
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                //multiple split to ensure even if the url format change it doesn't break easily
                String[] linkContent = url.split("\\/");
                String[] linkContent1 = (linkContent[linkContent.length-1]). split("\\?");
                String[] linkContent2 = (linkContent1[linkContent1.length-1]).split("=");
                orderKey = linkContent2[linkContent2.length-1];

                if(orderKey.startsWith("wc_order_")) {
                    Log.d(TAG, "new orderKey: "+orderKey);

                    // Build the alert dialog
                   /* AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
                    builder.setTitle("NOTICE");
                    builder.setMessage("Using the Premium Delivery service will enable GPS " +
                            "and other Location Services. " +
                            "This in order to collect your location and deliver your package where you are. ");

                    builder.setNegativeButton("Disagree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            promptDropOffPoint();
                        }
                    });
                    builder.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {*/
                            trackUserLocation();
                            fabMap.setVisibility(View.VISIBLE);
                        /*}
                    });

                    Dialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
*/


                }

                view.loadUrl(url);
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                myWebView.loadUrl("file:///android_asset/error.html");
            }

            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                myWebView.loadUrl("file:///android_asset/error.html");
            }

        });

    }

    protected void trackUserLocation(){

        // Programmatically request ACCESS_FINE_LOCATION PERMISSION if missing
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getParent(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
        createLocationRequest();

        // Locator service testing:
        Intent locationServiceIntent = new Intent(this,LocatorService.class);
        locationServiceIntent.putExtra("orderKey", orderKey);
        startService(locationServiceIntent);

    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        //mLocationRequest.setSmallestDisplacement(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates locationSettingsStates
                        = locationSettingsResult.getLocationSettingsStates();

                switch (status.getStatusCode()){
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try{
                            status.startResolutionForResult(MainActivity.this,
                                    1);

                        }catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });

    }

    protected void promptDropOffPoint(){
        Log.d(TAG, "Prompt drop-off point");

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack()) {
                        myWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }


    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private  boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager
            = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
