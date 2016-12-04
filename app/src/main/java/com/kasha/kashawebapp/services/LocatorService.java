package com.kasha.kashawebapp.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.kasha.kashawebapp.DB.KashaWebAppDBHelper;
import com.kasha.kashawebapp.helper.Configs;
import com.kasha.kashawebapp.helper.LocationUpdateResponse;
import com.kasha.kashawebapp.helper.LocationUpdater;
import com.kasha.kashawebapp.helper.LoginResponse;
import com.kasha.kashawebapp.helper.Util;
import com.kasha.kashawebapp.interfaces.PdsAPI;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.kasha.kashawebapp.helper.Configs.PREFS_NAME;
import static com.kasha.kashawebapp.helper.Configs.bckend_password;
import static com.kasha.kashawebapp.helper.Configs.bckend_username;
import static com.kasha.kashawebapp.helper.Configs.closeCursor;

/**
 * Created by rkabagamba on 10/13/2016.
 */

public class LocatorService extends Service implements  GoogleApiClient.ConnectionCallbacks,
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
    private SharedPreferences sharedPreferences;
    private String orderKey;

    private KashaWebAppDBHelper mydb;
    private ArrayList<String> activeOrders;

    @Override
    public void onCreate() {
        super.onCreate();
        mydb = KashaWebAppDBHelper.getInstance(getApplicationContext());

        Toast.makeText(this, "order sent", Toast.LENGTH_SHORT).show();

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
            LocationWebService locationWebService = new LocationWebService();
            locationWebService.execute(mLastLocation);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        orderKey = sharedPreferences.getString("orderKey","NONE");
        //orderKey = intent.getStringExtra("orderKey");
        return START_NOT_STICKY;
    }


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        //mLocationRequest.setSmallestDisplacement(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

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
        if(sharedPreferences.getString("DeliveryStatus","OFF").equalsIgnoreCase("ON")){
            //Toast.makeText(this, "OnLocation changed", Toast.LENGTH_SHORT).show();
            activeOrders = Util.getStringArrayFromColumnCursor(mydb.getAllActiveOrders(),closeCursor);
            LocationWebService locationWebService = new LocationWebService();
            locationWebService.execute(location);
        } else {
            stopSelf();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Toast.makeText(this, "on Connected LocatorService", Toast.LENGTH_SHORT).show();
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        }

        if (mRequestingLocationUpdates) {
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

    @Override
    public void onDestroy(){
        super.onDestroy();
        Toast.makeText(this, "on destroy LocatorService", Toast.LENGTH_LONG).show();
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

            pdsAPI.login(bckend_username, bckend_password).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call,
                                       Response<LoginResponse> response) {
                    for (int i = 0; i < activeOrders.size(); i++){
                        pdsAPI.updateLocation(new LocationUpdater(activeOrders.get(i), "Client",
                                String.valueOf(mCurrentLocation.getLongitude()),
                                String.valueOf(mCurrentLocation.getLatitude()))).enqueue(
                                new Callback<LocationUpdateResponse>() {
                                    @Override
                                    public void onResponse(Call<LocationUpdateResponse> call,
                                                           Response<LocationUpdateResponse> response) {
/*                                   Toast.makeText(getApplicationContext(),"order_id "+ orderKey+
                                           " response status: "+ response.code() + " " +
                                           response.message(),Toast.LENGTH_LONG).show();*/
                                    }

                                    @Override
                                    public void onFailure(Call<LocationUpdateResponse> call, Throwable t) {
               /*                     Toast.makeText(getApplicationContext(),"Posting unsuccessfull",
                                            Toast.LENGTH_LONG).show();*/
                                    }
                                }
                        );

                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                }
            });

            return null;
        }
    }
}



