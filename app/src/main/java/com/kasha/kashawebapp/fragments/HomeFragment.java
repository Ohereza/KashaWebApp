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
import android.widget.Toast;

import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.services.LocatorService;
import com.kasha.kashawebapp.services.MyPubnubListenerService;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private final String webUrl = "http://ec2-52-29-162-226.eu-central-1.compute.amazonaws.com/";

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

        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME,0);

        //mVisible = true;
        kWebView = (WebView) rootView.findViewById(R.id.kashaWebView);

        WebSettings webSettings = kWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        kWebView.getSettings().setAllowFileAccess( true );
        kWebView.getSettings().setJavaScriptEnabled( true );
        kWebView.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT );

        if (!isNetworkAvailable()) { // loading offline
            kWebView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
            }

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
                    // set delivery status to ON for deliver request made
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("DeliveryStatus","ON");
                    editor.commit();

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
                    // set delivery status to ON for deliver request made
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("DeliveryStatus","ON");
                    editor.commit();

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

                }
            });

            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }

        Toast.makeText(getActivity(),"Start tracking user location",
                Toast.LENGTH_LONG).show();
        trackUserLocation();

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

        Toast.makeText(getActivity(),"Start locator service",
                Toast.LENGTH_LONG).show();
        // Locator service:
        Intent locationServiceIntent = new Intent(getContext(),LocatorService.class);

        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("orderKey",orderKey);
        editor.commit();

        getActivity().startService(locationServiceIntent);

    }


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

    private  boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

