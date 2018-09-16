package com.example.michellewong.fitbeat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.io.IOException;

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
    private List<Song> played = new ArrayList<Song>();
    Song nowPlaying = null;
    SpotifyClient client;


    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference ref = database.getReference("bpm").child("message").child("heartRate");

    private String bpm;
    TextView songTempo = (TextView)findViewById(R.id.textView2);
    TextView songTitle = (TextView)findViewById(R.id.textView3);
    ImageButton button = (ImageButton)findViewById(R.id.imageButton7);

    private int max;
    private int min;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        // Read from the database
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.


                Object dataVal = dataSnapshot.getValue();
                //dataSnapshot.getValue(Post.class);

                Log.d(TAG, "BEFORE Value is::: " + dataVal.toString());
                bpm = dataVal.toString();
                max = (Integer.parseInt(bpm) + 10);
                min = (Integer.parseInt(bpm) - 10);
            }
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

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

    @Override
    protected void onStart() {
        super.onStart();
    }
  
    public void skipPressed(View v)
    {
        switch (v.getId())   // v is the button that was clicked
        {

            case (R.id.imageButton7):  // this is the oddball
                mSpotifyAppRemote.getPlayerApi().skipNext();
                break;
            default:   // this will run the same code for any button clicked that doesn't have id of button1 defined in xml
                break;
        }
    }

    private void connected() {
//        mSpotifyAppRemote.getPlayerApi()
//                .subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
//            @Override
//            public void onEvent(PlayerState playerState) {
//                final Track track = playerState.track;
//                if (track != null && nowPlaying != null) {
//                    if (!track.uri.equals(nowPlaying.getUri())) {
//                        loadNewList("US", Integer.toString(min), Integer.toString(max), bpm, "edm");
//                    }
//                    Log.d("MainActivity", track.name + " by " + track.artist.name);
//                }
//            }
//        });
        final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Authorization", "Bearer BQDNDHesWErfisuuAwf-Ev2G5HKjJYiyRCOuOEDRg4Emk0i9nPq1XZZA7C9uAWSYLoJKstF5DL-F6errbCpYEcB4c7ejz5hkjdDK-PjzomOaP86u8vDmneIjml98cA9a4jIwh6bQ5YI7qZtajjg59OaMfLltTLOvmuq4AbHloMShgyqVM9zstXGo9RPRBjAgrsaVSD-C4IEr2LA3SnuRRILRYyLDly4eB2WAd_f8N_ksFm5rbM-Acarz3bX70t3rXOwNxrOBoB_V")
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
        if (nowPlaying == null) {
            loadNewList("US", Integer.toString(min), Integer.toString(max), bpm, "edm");
        }
    }

    public void loadNewList(String market, String min_tempo, String max_tempo, String target_tempo, String seed_genres) {

        Call<JsonObject> call = client.loadRecommendations(market, min_tempo, max_tempo, target_tempo, seed_genres);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                JsonArray songs = response.body().getAsJsonArray("tracks");
                Song song = findNextSong(songs);
                getTempo(song);
                mSpotifyAppRemote.getPlayerApi().play(song.getUri());
                nowPlaying = song;
                songTitle.setText(song.getName());
                played.add(song);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "did not work");
            }
        });
    }

    public void getTempo(Song song) {
        Call<JsonObject> call = client.loadSongTempo(song.getId());

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {

                JsonObject features = response.body();
                int tempo = (int) Math.round(Double.parseDouble(features.get("tempo").toString()));
                System.out.println(tempo);
                songTempo.setText(String.valueOf(tempo));



            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "did not work");
            }
        });

    }

    protected Song createSong(JsonObject jsonObject) {
        Song song = new Song();
        song.setHref(jsonObject.get("href").toString().substring(1, jsonObject.get("href").toString().length() - 1));
        song.setId(jsonObject.get("id").toString().substring(1, jsonObject.get("id").toString().length() - 1));
        song.setName(jsonObject.get("name").toString().substring(1, jsonObject.get("name").toString().length() - 1));
        song.setType(jsonObject.get("type").toString().substring(1, jsonObject.get("type").toString().length() - 1));
        song.setUri(jsonObject.get("uri").toString().substring(1, jsonObject.get("uri").toString().length() - 1));
        return song;
    }

    protected Song findNextSong(JsonArray playlist) {
        int index = new Random().nextInt(playlist.size());
        Song song;
        while(playlist.size() > 0) {
            JsonElement songElement = playlist.get(index);
            song = createSong((JsonObject) songElement);
            if (played.indexOf(song) != -1) {
                playlist.remove(index);
                index = new Random().nextInt(playlist.size());
            } else {
                if (played.size() > 4) {
                    played.add(song);
                    played.remove(0);
                } else {
                    played.add(song);
                }
                return song;
            }
        }
        song = played.remove(0);
        played.add(song);
        return song;
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);
    }

}
