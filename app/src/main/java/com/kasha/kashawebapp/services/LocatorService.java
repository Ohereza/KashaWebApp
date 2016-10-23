package com.kasha.kashawebapp.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.kasha.kashawebapp.helper.Configs;
import com.kasha.kashawebapp.helper.LocationUpdateResponse;
import com.kasha.kashawebapp.helper.LocationUpdater;
import com.kasha.kashawebapp.helper.LoginResponse;
import com.kasha.kashawebapp.interfaces.PdsAPI;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by rkabagamba on 10/13/2016.
 */

public class LocatorService
            extends Service
            implements  GoogleApiClient.ConnectionCallbacks,
                        GoogleApiClient.OnConnectionFailedListener,
                        LocationListener {

    private static final String TAG = "LocatorService";

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest;
    ClearableCookieJar cookieJar;
    OkHttpClient okHttpClient;
    Retrofit retrofit;
    PdsAPI pdsAPI;

    private Boolean mRequestingLocationUpdates = true;

    private String orderKey;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "on create LocatorService", Toast.LENGTH_SHORT).show();

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

        // send last known location to server
        if (mLastLocation != null) {
            Toast.makeText(this, "last location: "+mLastLocation.getLatitude()
                    +" "+mLastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            LocationWebService locationWebService = new LocationWebService();
            locationWebService.execute(mLastLocation);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        orderKey = intent.getStringExtra("orderKey");
        return START_NOT_STICKY;
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

       /* result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
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
                            status.startResolutionForResult(getApplicationContext(),
                                    1);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });*/

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "OnLocation changed", Toast.LENGTH_SHORT).show();
        LocationWebService locationWebService = new LocationWebService();
        locationWebService.execute(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "on Connected LocatorService", Toast.LENGTH_SHORT).show();
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "requesting the last known location", Toast.LENGTH_SHORT).show();
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        }

        if (mRequestingLocationUpdates) {
            Toast.makeText(this, "set update location", Toast.LENGTH_SHORT).show();
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public class LocationWebService extends AsyncTask<Location, Void, Void> {
        private Location mCurrentLocation;

        public LocationWebService() {
        }

        @Override
        protected Void doInBackground(Location... locations) {
            mCurrentLocation = locations[0];
            cookieJar = new PersistentCookieJar(new SetCookieCache(),
                    new SharedPrefsCookiePersistor(getApplicationContext()));
            okHttpClient = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Configs.serverAddress)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();

            pdsAPI = retrofit.create(PdsAPI.class);

            pdsAPI.login("Administrator", "pds").enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call,
                                       Response<LoginResponse> response){

                    pdsAPI.updateLocation( new LocationUpdater(orderKey,"Client",
                            String.valueOf(mCurrentLocation.getLongitude()),
                            String.valueOf(mCurrentLocation.getLatitude()))).enqueue(
                            new Callback<LocationUpdateResponse>() {
                                @Override
                                public void onResponse(Call<LocationUpdateResponse> call,
                                                       Response<LocationUpdateResponse> response){
                                    Toast.makeText(getApplicationContext(),"Posting successfull",
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Call<LocationUpdateResponse> call, Throwable t){
                                    Toast.makeText(getApplicationContext(),"Posting unsuccessfull",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                }
            });

            return null;
        }
    }
}



