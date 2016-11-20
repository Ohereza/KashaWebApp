package com.kasha.kashawebapp.fragments;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.services.LocatorService;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
        // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private final String webUrl = "http://ec2-52-29-162-226.eu-central-1.compute.amazonaws.com/";

    private String mParam1;
    private String mParam2;

    private View rootView;
    private WebView kWebView;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 0;
//    private GoogleApiClient mGoogleApiClient = null;
//    private Location mLastLocation = null;
//    private LocationRequest mLocationRequest;
    private String orderKey;

    private static final String TAG = "HomeTabbedActivity";
    private SharedPreferences sharedPreferences;

    //private OnFragmentInteractionListener mListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////


        //mVisible = true;
        kWebView = (WebView) rootView.findViewById(R.id.kashaWebView);

        WebSettings webSettings = kWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        //myWebView.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        //myWebView.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        kWebView.getSettings().setAllowFileAccess( true );
        //myWebView.getSettings().setAppCacheEnabled( true );
        kWebView.getSettings().setJavaScriptEnabled( true );
        kWebView.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default

        if (!isNetworkAvailable()) { // loading offline
            kWebView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
            }

        //myWebView.loadUrl("http://ec2-52-57-159-28.eu-central-1.compute.amazonaws.com/");
        kWebView.loadUrl(webUrl);


        promptUserToEnableGPS();

        kWebView.setWebViewClient(new WebViewClient() {

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
                    promptToEnableLocationAndStartTracking();
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
                    promptToEnableLocationAndStartTracking();

                }

                view.loadUrl(url);
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                kWebView.loadUrl("file:///android_asset/error.html");
            }

            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                kWebView.loadUrl("file:///android_asset/error.html");
            }

        });


        kWebView.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if(event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    WebView kWebView = (WebView) v;
                    switch(keyCode)
                    {
                        case KeyEvent.KEYCODE_BACK:
                            if(kWebView.canGoBack() && (!kWebView.getUrl().contains(webUrl) && !kWebView.getUrl().contains("order-received"))) {
                                kWebView.goBack();
                                return true;
                            }
                            else if(kWebView.getUrl().contains("order-received") ==true && kWebView.canGoBack()){
                                kWebView.loadUrl(webUrl);
                                return true;

                            }
                            break;
                    }
                }
                return false;
            }
        });

        return rootView;

    }

    protected void promptUserToEnableGPS(){
        // Prompt to enable GPS if not enabled and a delivery request was submitted
        LocationManager manager = (LocationManager) getActivity().getSystemService(
                Context.LOCATION_SERVICE);
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && sharedPreferences.getString("DeliveryStatus","OFF").equalsIgnoreCase("ON")){

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("NOTICE");
            builder.setMessage("Please enable GPS to be able to take advantage of the " +
                    "Premium Delivery service tracking option.\n" +
                    "Delivery where you are." );

            builder.setPositiveButton("Press here", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(onGPS);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    protected void promptToEnableLocationAndStartTracking(){
        // Prompt to enable GPS if not enabled and a delivery request was submitted
        LocationManager manager = (LocationManager) getActivity().getSystemService(
                Context.LOCATION_SERVICE);
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && sharedPreferences.getString("DeliveryStatus","OFF").equalsIgnoreCase("ON")){

            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("NOTICE");
            builder.setMessage("Please enable GPS to be able to take advantage of the " +
                    "Premium Delivery service tracking option.\n" +
                    "Delivery where you are.");

            builder.setPositiveButton("Press here", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Prompt to enable location.
                    Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(onGPS);

                    sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("DeliveryStatus","ON");
                    editor.commit();

                    trackUserLocation();
                }
            });

            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    protected void trackUserLocation(){

        //Programmatically request ACCESS_FINE_LOCATION PERMISSION if missing
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        // Create an instance of GoogleAPIClient.
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(LocationServices.API)
//                    .build();
//        }
//
//        mGoogleApiClient.connect();
        //createLocationRequest();

        // Locator service testing:
        Intent locationServiceIntent = new Intent(getContext(),LocatorService.class);


        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("orderKey",orderKey);
        editor.commit();
        getActivity().startService(locationServiceIntent);

    }

//    protected void createLocationRequest() {
//
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        //mLocationRequest.setSmallestDisplacement(100);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
//                .addLocationRequest(mLocationRequest);
//
//        final PendingResult<LocationSettingsResult> result =
//                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
//                        builder.build());
//
//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
//                final Status status = locationSettingsResult.getStatus();
//                final LocationSettingsStates locationSettingsStates
//                        = locationSettingsResult.getLocationSettingsStates();
//
//                switch (status.getStatusCode()){
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        try{
//                            status.startResolutionForResult(getActivity(),1);
//
//                        }catch (IntentSender.SendIntentException e) {
//                            // Ignore the error.
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        break;
//                }
//            }
//        });
//
//    }

    protected void promptDropOffPoint(){
        Log.d(TAG, "Prompt drop-off point");
    }

    @Override
    public void onResume(){
        super.onResume();

        // Prompt to enable GPS if not enabled and a delivery request was submitted
        LocationManager manager = (LocationManager) getActivity().getSystemService(
                Context.LOCATION_SERVICE);
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && sharedPreferences.getString("DeliveryStatus","OFF").equalsIgnoreCase("ON")){

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("NOTICE");
            builder.setMessage("Please enable GPS to be able to take advantage of the " +
                    "Premium Delivery service tracking option.\n" +
                    "Delivery where you are." );

            builder.setPositiveButton("Press here", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(onGPS);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            switch (keyCode) {
//                case KeyEvent.KEYCODE_BACK:
//                    if (kWebView.canGoBack()) {
//                        kWebView.goBack();
//                    } else {
//                        finish();
//                    }
//                    return true;
//            }
//
//        }
//        return super.onKeyDown(keyCode, event);
//    }


//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        delayedHide(100);
//    }


//    private void toggle() {
//        if (kWebView) {
//            hide();
//        } else {
//            show();
//        }
//    }

//    private void hide() {
//        // Hide UI first
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
//        mVisible = false;
//
//        // Schedule a runnable to remove the status and navigation bar after a delay
//        mHideHandler.removeCallbacks(mShowPart2Runnable);
//        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
//    }

//    @SuppressLint("InlinedApi")
//    private void show() {
//        // Show the system bar
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//        mVisible = true;
//
//        // Schedule a runnable to display UI elements after a delay
//        mHideHandler.removeCallbacks(mHidePart2Runnable);
//        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
//    }

//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    private  boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

//    @Override
//    public void onConnected(@Nullable Bundle bundle) {
//        int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
//                android.Manifest.permission.ACCESS_FINE_LOCATION);
//        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                    mGoogleApiClient);
//        }
//
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//
//    }
//}



///////////////////////////////////////////////////////////////////////////////////////////////////////
    }

//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }

