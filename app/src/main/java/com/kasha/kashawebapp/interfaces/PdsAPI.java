package com.kasha.kashawebapp.interfaces;

import com.kasha.kashawebapp.helper.LocationUpdateResponse;
import com.kasha.kashawebapp.helper.LocationUpdater;
import com.kasha.kashawebapp.helper.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * Created by rkabagamba on 10/9/2016.
 */

public interface PdsAPI {

    @FormUrlEncoded
    @POST("/api/method/login")
    Call<LoginResponse> login(@Field("usr") String username, @Field("pwd") String password);

/*    @FormUrlEncoded
    @POST("/api/resource/Location")
    Call<LocationUpdateResponse> updateLocation(@Field("order_number") String username,
                                                @Field("type") String type,
                                                @Field("longitude") String longitude,
                                                @Field("latitude") String latitude);*/

    @POST("/api/resource/Location")
    Call<LocationUpdateResponse> updateLocation(@Body LocationUpdater data);


    @FormUrlEncoded
    @PUT("/api/resource/User/administrator")
    Call<Void> updateFirebaseInstanceId(@Field("fcm_instance_id") String instanceId);

   /* @PUT("/api/resource/User/administrator")
    Call<FCMInstanceUpdate> updateFCMInstanceId(@Body FCMInstanceUpdate data);
*/

}
