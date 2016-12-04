package com.kasha.kashawebapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kasha.kashawebapp.DB.KashaWebAppDBHelper;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.helper.Configs;
import com.kasha.kashawebapp.helper.Util;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;
import static com.kasha.kashawebapp.helper.Configs.closeCursor;
import static java.lang.Math.round;

public class LocationFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        android.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Marker marker;
    private LocationManager mLocationManager;
    private SharedPreferences sharedPreferences;

    private View rootView;
    private KashaWebAppDBHelper mydb;
    private ArrayList<String> activeOrders;

    private OnFragmentInteractionListener mListener;

    private LatLng clerkLocation;
    private LatLng myLocation;

    private LocationManager manager;

    private PolylineOptions mPolylineOptions;

    private boolean zoomToMyLocation = true;
    private boolean zoomToClerkLocation = true;

    private String notificationMSG;
    private TextView notificationTextview;
    private ImageView myLocationImg;
    private ImageView clerkLocationImg;

    private KashaWebAppDBHelper kashaWebAppDBHelper;

    public LocationFragment() {
        // Required empty public constructor
    }

    public static LocationFragment newInstance(String param1, String param2) {
        LocationFragment fragment = new LocationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        createLocationRequest();
        notificationMSG = "Looking for my location ...";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_location, container, false);

        kashaWebAppDBHelper = new KashaWebAppDBHelper(getContext());
        mydb = KashaWebAppDBHelper.getInstance(getContext());
        notificationTextview = (TextView) rootView.findViewById(R.id.notification_textview);
        notificationTextview.setSelected(true);
        //notificationTextview.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.move));

        myLocationImg = (ImageView) rootView.findViewById(R.id.imgMyLocation);
        clerkLocationImg = (ImageView) rootView.findViewById(R.id.imgClerkLocation);

        myLocationImg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myLocation != null && mMap != null) {
                    Toast.makeText(getActivity(), "Zooming to my location", Toast.LENGTH_SHORT).show();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLocation, 15);
                    mMap.animateCamera(cameraUpdate);
                }
            }
        });

        clerkLocationImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clerkLocation != null && mMap != null) {
                    Toast.makeText(getActivity(), "Zooming to my package", Toast.LENGTH_SHORT).show();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(clerkLocation, 15);
                    mMap.animateCamera(cameraUpdate);
                } else {
                    if(kashaWebAppDBHelper.onGoingOrders()) {
                        Toast.makeText(getActivity(), "Contacting the server .... ", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getActivity(), "No active delivery", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });

        notificationTextview.setText(notificationMSG);
        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, 0);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();

        createLocationRequest();

        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        activeOrders = Util.getStringArrayFromColumnCursor(mydb.getAllActiveOrders(), closeCursor);
        Toast.makeText(getActivity(),"active orders: "+activeOrders.toString(),Toast.LENGTH_LONG).show();

        //  PUBNUB
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(Configs.pubnub_subscribeKey);
        pnConfiguration.setPublishKey(Configs.pubnub_publishKey);
        PubNub pubnub = new PubNub(pnConfiguration);

        // Subscribe to a channel
        //pubnub.subscribe().channels(Arrays.asList(sharedPreferences.getString("orderKey",null))).execute();
        pubnub.subscribe().channels(activeOrders).execute();
        // Listen for incoming messages
        //pubnub.addListener(new MyPubnubListenerService());

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost

                } else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {


                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {


                    }
                } else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {


                } else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {
                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {

                JSONObject jsonRequest = null;
                try {
                    jsonRequest = new JSONObject(String.valueOf(message.getMessage()));

                    if (message.getMessage().toString().toLowerCase().contains("latlng")) {

                        zoomToMyLocation = false;
                        String latLon = message.getMessage().toString().split("(\\{)|(:)|(\\[)|(\\])")[5];
                        double lat = Double.parseDouble(latLon.split(",")[0]);
                        double lon = Double.parseDouble(latLon.split(",")[1]);

                        clerkLocation = new LatLng(lat, lon);
                        mPolylineOptions = new PolylineOptions();
                        mPolylineOptions.color(Color.BLUE).width(10);
                        LocationFragment.this.updatePolyline();
                    } else if (jsonRequest != null && jsonRequest.has("type")
                            && jsonRequest.getString("type").equalsIgnoreCase("Delivering")) {

                        zoomToMyLocation = false;
                        LocationFragment.this.changeStatusToDelivering();
                    } else if (jsonRequest != null && jsonRequest.has("type")
                            && jsonRequest.getString("type").equalsIgnoreCase("Delivered")) {
                        LocationFragment.this.changeStatusToDelivered();

                    } else if (jsonRequest != null && jsonRequest.has("type")
                            && jsonRequest.getString("type").equalsIgnoreCase("Update")) {


                        String updates = jsonRequest.getString("message");
                        JSONObject timeAndDistance = new JSONObject(String.valueOf(updates));
                        //String remDistance = timeAndDistance.getString("remaining_distance");
                        int remTime = timeAndDistance.getInt("remaining_time");
                        notificationMSG = "Your package reaches you in " + (round(remTime / 60)) + "Min.";
                        LocationFragment.this.changeNotificationRemainningTime(notificationMSG);

                    }
                } catch (JSONException e) {
                    Log.e("client app", e.toString());
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            }
        });
        /// End of pubnub////

        return rootView;
    }

    public void updateClerkLocation(JSONObject clerkLocation){
        Log.d("log","update clerk location");

        try {
            String latitude = clerkLocation.getString("Lat");
            String longitude = clerkLocation.getString("Long");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

///*    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        notificationMSG = " Synchronizing ..... ";
        notificationTextview.setText(notificationMSG);

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            notificationMSG = " Please enable the GPS and allow the permission for the app to access your location ";
            notificationTextview.setText(notificationMSG);
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double dLatitude = mLastLocation.getLatitude();
            double dLongitude = mLastLocation.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            mMap.addMarker(new MarkerOptions().position(myLocation).title("Me")).showInfoWindow();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,15));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            double dLatitude = location.getLatitude();
            double dLongitude = location.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            if (clerkLocation==null && mMap !=null) {
                mMap.clear();
                mMap.setOnMapLoadedCallback(null);
                mMap.addMarker(new MarkerOptions().position(myLocation).title("Me")).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,15));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                if(kashaWebAppDBHelper.onGoingOrders()) {
                    notificationMSG = " The package is being prepared .... ";
                }
                else{
                    notificationMSG = " No active delivery ";
                }
                notificationTextview.setText(notificationMSG);
            }
            else if(mMap!=null) {

                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        try {
                            mMap.clear();
                            mMap.setOnMapLoadedCallback(null);
                            mMap.addMarker(new MarkerOptions().position(myLocation).title("Me")).showInfoWindow();
                            mMap.addMarker(new MarkerOptions().position(clerkLocation)
                                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_motorcycle_black_35dp)));
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                    .title("My Package"));

                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(clerkLocation);
                            builder.include(myLocation);
                            LatLngBounds bounds = builder.build();

                            int padding = 65;
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                            mMap.moveCamera(cu);
                            mMap.animateCamera(cu);
                        }
                        catch (Exception e){

                        }
                    }
                });
                notificationMSG = " Delivery in progress .... ";
                notificationTextview.setText(notificationMSG);
            }
        }

        //remove previous current location Marker
        if (marker != null) {
            marker.remove();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(getActivity(),"Wait .... we are looking for your location",Toast.LENGTH_SHORT);
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.getUiSettings().setMapToolbarEnabled(true);
    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mLocationManager = (LocationManager) getActivity().getSystemService(getContext().LOCATION_SERVICE);
            //mActivity = context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 50, this);
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 50, this);
        zoomToMyLocation = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.removeUpdates(this);
    }

    private void updatePolyline() {
        if(getActivity()==null)
            return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mMap.clear();
                    mMap.addPolyline(mPolylineOptions.add(clerkLocation));
                    mMap.addMarker(new MarkerOptions().position(myLocation)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .title("Me")).showInfoWindow();

                    mMap.addMarker(new MarkerOptions().position(clerkLocation)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title("My Package")).showInfoWindow();

                    mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(clerkLocation);
                            builder.include(myLocation);
                            LatLngBounds bounds = builder.build();
                            int padding = 65;
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mMap.moveCamera(cu);
                            mMap.animateCamera(cu);
                        }
                    });
                }
                catch (Exception e){

                }
            }

        });

    }

    private void changeStatusToDelivering(){
        notificationMSG = "Your package is on the way";
        if(getActivity()==null)
            return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    notificationTextview.setText(notificationMSG);
                    Toast.makeText(getActivity(), "Your package is on the way", Toast.LENGTH_SHORT).show();
                }
                catch (Exception e){

                }
            }
        });


    }

    private void changeStatusToDelivered(){
        clerkLocation = null;
        zoomToMyLocation = true;
        notificationMSG = "Your package has been delivered, We hope to see you again soon";
        if(getActivity()==null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mMap.clear();
                    notificationTextview.setText(notificationMSG);
                    Toast.makeText(getActivity(), "Your package has been delivered!", Toast.LENGTH_SHORT).show();
                    mMap.addMarker(new MarkerOptions().position(myLocation).title("Me")).showInfoWindow();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                }
                catch (Exception e){

                }
            }
        });
    }

    private void changeNotificationRemainningTime(String notificationMSG) {
        final String notif = notificationMSG;
        if(getActivity()==null)
            return;
        getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run () {
            try {
                notificationTextview.setText(notif);
            }
            catch (Exception e){

            }
        }
    });
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}