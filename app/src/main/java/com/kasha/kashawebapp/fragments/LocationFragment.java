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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.helper.Configs;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;

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

    private OnFragmentInteractionListener mListener;

    private LatLng clerkLocation;
    private LatLng myLocation;

    private LocationManager manager;

    private PolylineOptions mPolylineOptions;

    private boolean zoomToMyLocation = true;

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_location, container, false);
        rootView = inflater.inflate(R.layout.fragment_location, container, false);
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


        //  PUBNUB
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(Configs.pubnub_subscribeKey);
        pnConfiguration.setPublishKey(Configs.pubnub_publishKey);
        PubNub pubnub = new PubNub(pnConfiguration);

        // Subscribe to a channel
        pubnub.subscribe().channels(Arrays.asList(sharedPreferences.getString("orderKey",null))).execute();
        // Listen for incoming messages
        //pubnub.addListener(new MyPubnubListenerService());

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost

                } else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc

                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {


                    }
                } else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.

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


                if(message.getMessage().toString().toLowerCase().contains("latlng")){

                    zoomToMyLocation = false;
                    String latLon = message.getMessage().toString().split("(\\{)|(:)|(\\[)|(\\])")[5];
                    double lat = Double.parseDouble(latLon.split(",")[0]);
                    double lon = Double.parseDouble(latLon.split(",")[1]);

                    clerkLocation = new LatLng(lat, lon);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.BLUE).width(10);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updatePolyline();
                            updateCamera();
                            updateMarker();
                        }

                    });
                } else if (jsonRequest != null && jsonRequest.has("type")
                        && jsonRequest.getString("type").equalsIgnoreCase("Delivered")) {
                    mMap.clear();
                    }
                    }catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            private void updatePolyline() {
                mMap.clear();
                mMap.addPolyline(mPolylineOptions.add(clerkLocation));

            }

            private void updateMarker() {
                mMap.addMarker(new MarkerOptions().position(clerkLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title("My Package"));
                mMap.addMarker(new MarkerOptions().position(myLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .title("Me"));
            }

            private void updateCamera() {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clerkLocation, 14));
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            }
        });
        ///////////////////////
        /// End of pubnub////
        //////////////////////

        //rootView.
        return rootView;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Please enable the GPS", Toast.LENGTH_LONG);
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double dLatitude = mLastLocation.getLatitude();
            double dLongitude = mLastLocation.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16.0f));
            mMap.addMarker(new MarkerOptions().position(myLocation).title("Me"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,15));
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
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

        //Log.i("Test Location", String.valueOf(location.getLatitude()));
        if (location != null) {
            double dLatitude = location.getLatitude();
            double dLongitude = location.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            if (zoomToMyLocation) {
                //mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(myLocation).title("Me"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,15));
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
                //mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
            } else {
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //mMap.setMyLocationEnabled(true); //addMarker(new MarkerOptions().position(myLocation));
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(myLocation).title("Me"));
                if(clerkLocation!=null){
                    mMap.addMarker(new MarkerOptions().position(clerkLocation)
                            .title("My Package")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clerkLocation,15));
                    mMap.animateCamera(CameraUpdateFactory.zoomIn());
                }
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
        Toast.makeText(getActivity(),"Wait .... we are looking for your location",Toast.LENGTH_LONG);
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        //check if location permission is granted
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(10000);
        //mLocationRequest.setSmallestDisplacement(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

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
            //act = (Activity) context;

            mLocationManager = (LocationManager) getActivity().getSystemService(getContext().LOCATION_SERVICE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
