package com.example.michellewong.fitbeat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "2e6beef170124eccbc991779c72830f6";
    private static final String REDIRECT_URI = "fitbeat://callback";
    private static final String TAG = "MainActivity";
    private SpotifyAppRemote mSpotifyAppRemote;
    SpotifyClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.CONNECTOR.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);
                    }
                });

    }

    private void connected() {
        mSpotifyAppRemote.getPlayerApi().play("spotify:user:spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");

        final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Authorization", "Bearer BQAJgT_e7p61fLE_ur-YUXoUtJIgTB4hFgusMzoCUu-9_VIykncMcgWC8_js2bLvOZYWMTaUMs3a4SeUpNPEgID3VE7jefFb_hghJIuMhUmBTK91u9tKqVnE-MTEcoiaRAlJNG1Y4BM7A6L86wSXgPIhzk2RRZdt8SO4eR0mqvzdW2Sedp04c5eenzpteTxWSp0LCXIuyt3MulJFwEH1bJy5C4xwVr3hVFfZO7Ns_P1ac95pBOrzdit6vJTpXx6wrG-UhVR-FFuJ")
                        .header("Accept", "application/json")
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }}).build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();
        client = retrofit.create(SpotifyClient.class);
        Call<JsonObject> call = client.loadRecommendations("US", "60", "80", "70", "pop");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                JsonObject songs = response.body();
                JsonElement first_song = songs.getAsJsonArray("tracks").get(0);
                Song song = getFirstSong((JsonObject) first_song);
                mSpotifyAppRemote.getPlayerApi().play(song.getUri());

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "did not work");
            }
        });
    }

    public Song getFirstSong(JsonObject jsonObject) {
        Song song = new Song();
        song.setHref(jsonObject.get("href").toString().substring(1, jsonObject.get("href").toString().length() - 1));
        song.setId(jsonObject.get("id").toString().substring(1, jsonObject.get("id").toString().length() - 1));
        song.setName(jsonObject.get("name").toString().substring(1, jsonObject.get("name").toString().length() - 1));
        song.setType(jsonObject.get("type").toString().substring(1, jsonObject.get("type").toString().length() - 1));
        song.setUri(jsonObject.get("uri").toString().substring(1, jsonObject.get("uri").toString().length() - 1));
        Call<JsonObject> call = client.loadSongTempo(song.getId());

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                JsonObject features = response.body();
                int tempo = (int) Math.round(Double.parseDouble(features.get("tempo").toString()));
                System.out.println(tempo);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "did not work");
            }
        });

        return song;

    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);
    }
}
