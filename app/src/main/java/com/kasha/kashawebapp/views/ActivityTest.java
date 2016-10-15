package com.kasha.kashawebapp.views;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
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

public class ActivityTest extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_ACCESS_FINE_LOCATION = 0;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private EditText mLatitudeText;
    private EditText mLongitudeText;
    private Button sendToServerButton;
    ClearableCookieJar cookieJar;
    OkHttpClient okHttpClient;
    Retrofit retrofit;
    PdsAPI pdsAPI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        mLatitudeText = (EditText) findViewById(R.id.latitude_edit_text);
        mLongitudeText = (EditText) findViewById(R.id.longitude_edit_text);
        sendToServerButton = (Button) findViewById(R.id.send_button);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
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

        createLocationRequest();







        sendToServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

                        pdsAPI.updateLocation( new LocationUpdater("kasha899","Client",
                                        String.valueOf(mLastLocation.getLongitude()),
                                        String.valueOf(mLastLocation.getLatitude()))).enqueue(
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

            }
        });
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
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
                            status.startResolutionForResult(ActivityTest.this,
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

      /*  result.setResultCallback(new ResultCallback<LocationSettingsResult>()) {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    OuterClass.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });*/

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
                if (mLastLocation != null) {
                    mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
                    mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
                }
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

}
