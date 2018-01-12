package com.example.riyo.music_group;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.ContentDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "musicbroadcaster";

    public static final String ACTION_PLAY_STREAM = "com.reyoexplorer.musicbroadcaster.musicbroadcaster.PLAY_STREAM";
    public static final String ACTION_PLAY_FILE = "com.reyoexplorer.musicbroadcaster.musicbroadcaster.PLAY_FILE";

    boolean whenReady = true;
    private SimpleExoPlayer player;
    private SimpleExoPlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);


    }


    @Override
    protected void onStart()
    {
        super.onStart();
        if(Util.SDK_INT > 23)
            initializePlayer();
    }



    @Override
    protected void onResume()
    {
        super.onResume();
        if(Util.SDK_INT <= 23 || player == null)
            initializePlayer();
    }




    private void initializePlayer()
    {
        try {
            player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());

            playerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
            playerView.setPlayer(player);
            player.setPlayWhenReady(whenReady);

            MediaSource mediaSource = null;

            if(ACTION_PLAY_STREAM.equals(getIntent().getAction()))
            {
                /*
                if(PersistentResource.getInstance().getInputStreamer() != null )
                    mediaSource = buildMediaSource(null, new InputStreamDataSourceFactory(PersistentResource.getInstance().getInputStreamer()));
                else
                    Log.d(TAG, "no input stream received from connection activity");
                */
            }
            else if(ACTION_PLAY_FILE.equals(getIntent().getAction())) {
                //obtaining uri of file to play

                Uri uri = getIntent().getData();
                mediaSource = buildMediaSource(uri, new DefaultDataSourceFactory(getApplicationContext(), "ua"));
            }

            if(mediaSource == null)
                Log.d(TAG, "unable to open the media source");
            player.prepare(mediaSource, true, false);
        }
        catch (Exception e)
        {
            Log.d(TAG, e.getMessage());
        }
    }



    private MediaSource buildMediaSource(Uri uri, DataSource.Factory df)
    {
        try {

            return new ExtractorMediaSource(uri, df, new DefaultExtractorsFactory(), null, null);
        }
        catch(Exception e)
        {
            Log.d(TAG, e.getMessage());
            return null;
        }
    }

}


