package com.kasha.kashawebapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LocationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocationFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        android.location.LocationListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker marker;
    private LocationManager mLocationManager;

    private View rootView;

    private OnFragmentInteractionListener mListener;

    private LatLng clientLocation;
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
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_location, container, false);
        rootView = inflater.inflate(R.layout.fragment_location, container, false);

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


        // ################################################################################
        //  PUBNUB EXAMPLE
        //###################################################################################
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(Configs.pubnub_subscribeKey);
        pnConfiguration.setPublishKey(Configs.pubnub_publishKey);
        PubNub pubnub = new PubNub(pnConfiguration);

        // Subscribe to a channel
        pubnub.subscribe().channels(Arrays.asList("6fecf37679", "mymaps")).execute();

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
                zoomToMyLocation = false;
                    String latLon = message.getMessage().toString().split("(\\{)|(:)|(\\[)|(\\])")[5];
                    double lat = Double.parseDouble(latLon.split(",")[0]);
                    double lon = Double.parseDouble(latLon.split(",")[1]);

                    clientLocation = new LatLng(lat, lon);
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
            }

            private void updatePolyline() {
                mMap.clear();
                mMap.addPolyline(mPolylineOptions.add(clientLocation));

            }

            private void updateMarker() {
                mMap.addMarker(new MarkerOptions().position(clientLocation).title("My Package"));
            }

            private void updateCamera() {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clientLocation, 14));
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
            Toast.makeText(getContext(), "Make sure the gps permission is allowed", Toast.LENGTH_LONG);
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double dLatitude = mLastLocation.getLatitude();
            double dLongitude = mLastLocation.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16.0f));
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

        Log.i("Test Location", String.valueOf(location.getLatitude()));
        if (location != null) {
            double dLatitude = location.getLatitude();
            double dLongitude = location.getLongitude();
            myLocation = new LatLng(dLatitude, dLongitude);
            if (zoomToMyLocation) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
            } else {
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true); //addMarker(new MarkerOptions().position(myLocation));
            }
        }

        mLastLocation = location;

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
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.getUiSettings().setMapToolbarEnabled(true);

        //check if location permission is granted
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

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

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Activity my;

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
