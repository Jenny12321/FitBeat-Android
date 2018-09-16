package com.example.michellewong.fitbeat;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotifyClient {

    // ?market={market}&min_tempo={min_tempo}&max_tempo={max_tempo}&target_tempo={target_tempo}&seed_genres={genres}
    @GET("/v1/recommendations")
    Call<JsonObject> loadRecommendations(@Query("market") String market,
                                         @Query("min_tempo") String min_tempo,
                                         @Query("max_tempo") String max_tempo,
                                         @Query("target_tempo") String target_tempo,
                                         @Query("seed_genres") String seed_genres);

    @GET("/v1/audio-features/{id}")
    Call<JsonObject> loadSongTempo(@Path("id") String id);

    @GET("/user")
    Call<UserDetails> getUserDetails(@Header("Authorization") String credentials);
}
