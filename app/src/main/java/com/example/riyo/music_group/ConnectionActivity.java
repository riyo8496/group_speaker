package com.example.riyo.music_group;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;

/**
 * Created by Sunil Choudhary on 7/24/2017.
 */

public class ConnectionActivity extends SetupActivity {
    private final static String SERVICEID = "com.riyo.music_group";
    private final static String TAG = "musicbroadcaster";

    //request codes for distinguishing different activity results
    private static final int CHOOSE_FILE_TO_SEND = 1;
    private static final int CHOOSE_FILE_TO_PLAY = 2;
    private static final int CHOOSE_FILE_TO_STREAM = 3;


    private NotificationManager mNotificationManager;
    private Map<Long, Payload> mIncomingFilePayloads = new HashMap<Long, Payload>();
    private SimpleArrayMap<Long, String> mIncomingPayloadFileNames = new SimpleArrayMap<>();

    private SimpleArrayMap<Long, Payload> mIncomingStreamPayloads = new SimpleArrayMap<>();

    private SimpleArrayMap<Long, NotificationCompat.Builder> incomingFilePayloadNotifications = new SimpleArrayMap<>();
    private SimpleArrayMap<Long, NotificationCompat.Builder> outgoingFilePayloadNotifications = new SimpleArrayMap<>();

    private SimpleArrayMap<Long, NotificationCompat.Builder> incomingStreamNotifications = new SimpleArrayMap<>();
    private SimpleArrayMap<Long, NotificationCompat.Builder> outgoingStreamNotifications = new SimpleArrayMap<>();

    private boolean mIsReady = true;


    private Fragment mDeviceFragment;



    public ConnectionActivity()
    {
        super(ConnectionActivity.TAG);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection);

        Log.d(TAG, "application started");

        setupUI();

    }



    private void setupUI()
    {
        try
        {

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Button advertiseButton = (Button) findViewById(R.id.advertise_button);
            advertiseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAdvertising();
                }
            });

            Button discoverButton = (Button) findViewById(R.id.discover_button);
            discoverButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startDiscovering();
                }
            });

            Button sendFileButton = (Button) findViewById(R.id.send_file_button);
            sendFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");

                    startActivityForResult(intent, CHOOSE_FILE_TO_SEND);

                }
            });

            Button playFileButton = (Button) findViewById(R.id.play_file_button);
            playFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");

                    startActivityForResult(intent, CHOOSE_FILE_TO_PLAY);

                }
            });

            Button streamFileButton = (Button) findViewById(R.id.stream_file_button);
            streamFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");

                    startActivityForResult(intent, CHOOSE_FILE_TO_STREAM);

                }
            });

            FragmentManager fm = getSupportFragmentManager();

            mDeviceFragment = fm.findFragmentById(R.id.fragment_container);

            if(mDeviceFragment == null)
            {
                mDeviceFragment = new DeviceListFragment();
                fm.beginTransaction().add(R.id.fragment_container, mDeviceFragment).commit();
            }


            //setting visibilities of various buttons

            sendFileButton.setVisibility(View.GONE);
            streamFileButton.setVisibility(View.GONE);

        }
        catch (Exception e)
        {
            Log.d(TAG, "exception caught in setting up UI: " + e.getMessage());
        }


    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), R.string.result_cancelled, Toast.LENGTH_LONG).show();
            return;
        }

        if(requestCode == CHOOSE_FILE_TO_SEND ) {
            Log.d(TAG, "onActivityResult() called to send music file");
            Uri uri = data.getData();

            sendFile(uri);
        }
        else if(requestCode == CHOOSE_FILE_TO_PLAY)
        {
            Uri uri = data.getData();

            //preparing intent  to play the song
            Intent playIntent = new Intent(this, PlayerActivity.class);
            playIntent.setAction(PlayerActivity.ACTION_PLAY_FILE);
            playIntent.setData(uri);

            startActivity(playIntent);
        }
        else if(requestCode == CHOOSE_FILE_TO_STREAM)
        {
            sendStream(data.getData());
        }
    }



    private void sendFile(Uri uri)
    {
        ParcelFileDescriptor pfd = null;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "r");

            Log.d(TAG, "sending file to remote endpoint");

            Payload payload = Payload.fromFile(pfd);

            //sending name of file to receiver as prelude to file
            String fileName = payload.getId() + ":" + getFileName(uri);
            sendBytes(fileName);

            send(payload);
            pfd.close();

// Build and start showing the notification.
            NotificationCompat.Builder notification = buildNotification(payload, false /*isIncoming*/);
            mNotificationManager.notify((int) payload.getId(), notification.build());

// Add it to the tracking list so we can update it.
            outgoingFilePayloadNotifications.put(payload.getId(), notification);
        }
        catch(IOException e)
        {
            Log.d(TAG, "exception: " + e.getMessage());
        }
        catch(Exception e)
        {
            Log.d(TAG, "exception caught: " + e.getMessage());
        }
    }



    private void sendStream(Uri uri)
    {

        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);

            Payload payload = Payload.fromStream(in);

            send(payload);

            //in.close();
        }
        catch(IOException ioe)
        {
            Log.d(TAG, "error in opening the file uri for streaming");
        }
        catch(Exception e)
        {
            Log.d(TAG, e.getMessage());
        }
        finally {


        }
    }




    private void sendBytes(String message)
    {
        try {
            send(Payload.fromBytes(message.getBytes("UTF-8")));
        }
        catch(Exception e)
        {
            Log.d(TAG, "error in sending byte message");
        }
    }



    private NotificationCompat.Builder buildNotification(Payload payload, boolean isIncoming) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle(isIncoming ? "Receiving..." : "Sending...")
                .setContentText(isIncoming ? "Downstreaming the file..." : "Upstreaming the file...")
                .setSmallIcon(android.R.color.holo_orange_dark)
                .setOngoing(true);

        boolean indeterminate = false;
        if(payload.getType() == Payload.Type.STREAM)
            indeterminate = true;

        notification.setProgress(100, 0, indeterminate);
        return notification;
    }



    @Override
    protected void onEndpointDiscovered(final Endpoint endpoint)
    {

        if(mDeviceFragment != null && mDeviceFragment instanceof DeviceListFragment)
            ((DeviceListFragment) mDeviceFragment).setList(getDiscoveredEndpoints());

    }




    @Override
    protected void onConnectionInitiated(final Endpoint endpoint, ConnectionInfo connectionInfo)
    {

        if(!connectionInfo.isIncomingConnection())
        {
            acceptConnection(endpoint);
            findViewById(R.id.send_file_button).setVisibility(View.VISIBLE);
            findViewById(R.id.stream_file_button).setVisibility(View.VISIBLE);

            return;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(String.format("Got a connection request from %s, Do you want to connect", endpoint.getName()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "accepting connection");
                acceptConnection(endpoint);
                findViewById(R.id.send_file_button).setVisibility(View.VISIBLE);
                findViewById(R.id.stream_file_button).setVisibility(View.VISIBLE);
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                rejectConnection(endpoint);
            }
        });

        builder.create().show();

    }



    @Override
    public void onReceive(Endpoint endpoint, final Payload payload)
    {
        Log.d(TAG, "payload received from " + endpoint.getName());

        switch (payload.getType())
        {
            case Payload.Type.BYTES:
                Log.d(TAG, "received byte payload from " + endpoint.getName());
                String fileName = null;
                try {
                    fileName = new String(payload.asBytes(), "UTF-8");
                } catch(UnsupportedEncodingException e)
                {
                    Log.d(TAG, "unsupported encoding for file name");
                }
                addPayloadFileName(fileName);
                break;

            case Payload.Type.FILE:
                Log.d(TAG, "file sent from " + endpoint.getName());
                try {
                    Log.d(TAG, "uri: " + payload.asFile().asJavaFile().getAbsolutePath());
                }
                catch (Exception e)
                {
                    Log.d(TAG, e.getMessage());
                }

                mIncomingFilePayloads.put(payload.getId(), payload);

                // Build and start showing the notification.
                NotificationCompat.Builder notification = buildNotification(payload, true /*isIncoming*/);
                mNotificationManager.notify((int) payload.getId(), notification.build());

// Add it to the tracking list so we can update it.
                incomingFilePayloadNotifications.put(payload.getId(), notification);
                break;
/*
            case Payload.Type.STREAM:
                Log.d(TAG, "stream received from " + endpoint.getName());

                mIncomingStreamPayloads.put(payload.getId(), payload);
                Intent playIntent = new Intent(this, PlayerActivity.class);
                playIntent.setAction(PlayerActivity.ACTION_PLAY_STREAM);
                if(payload.asStream().asInputStream() == null)
                    Log.d(TAG, "unable to open sent stream in connection activity");
                else
                    Log.d(TAG, "successfully opened sent stream in connection activity");

            /*
            ExoPlayer player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());
            player.setPlayWhenReady(true);

            MediaSource mediaSource = new ExtractorMediaSource(null, new InputStreamDataSourceFactory(payload.asStream().asInputStream()), new DefaultExtractorsFactory(), null, null);
            player.prepare(mediaSource, true, true);
**

                PersistentResource.getInstance().setInputStreamer(payload.asStream().asInputStream());
                //startActivity(playIntent);

                break;
*/

            default:
                Log.d(TAG, "unknown tiype of payload");
        }
    }


    private void addPayloadFileName(String fileNameMessage)
    {
        int colonIndex = fileNameMessage.indexOf(':');
        String payloadId = fileNameMessage.substring(0, colonIndex);
        String fileName = fileNameMessage.substring(colonIndex + 1);
        mIncomingPayloadFileNames.put(new Long(payloadId), fileName);
    }



    @Override
    protected void onPayloadUpdate(Endpoint endpoint, PayloadTransferUpdate update)
    {

        Payload payload = null;

        try {
            switch (getUpdateType(update.getPayloadId())) {
                case Payload.Type.BYTES:
                    if(update.getStatus() == PayloadTransferUpdate.Status.FAILURE)
                        Log.d(TAG, "error in transferring byte payload");
                    return;
                //break;

                case Payload.Type.FILE:
                    updateFileNotifications(endpoint, update);
                    break;

                case Payload.Type.STREAM:

                    break;

                default:
                    Log.d(TAG, "unknown type for payload update");
            }
        }
        catch(Exception e)
        {
            Log.d(TAG, "exception caught in payload update " + e.getMessage());
        }


    }



    private  int getUpdateType(long payloadId)
    {
        if(mIncomingFilePayloads.containsKey(payloadId))
            return Payload.Type.FILE;
        else if(mIncomingStreamPayloads.containsKey(payloadId))
            return Payload.Type.STREAM;
        else
            return Payload.Type.BYTES;
    }




    private void updateFileNotifications(Endpoint endpoint, PayloadTransferUpdate update)
    {
        NotificationCompat.Builder notification = null ;
        Payload payload = null;

        boolean isIncoming = false;

        if (incomingFilePayloadNotifications.containsKey(update.getPayloadId())) {
            notification = incomingFilePayloadNotifications.get(update.getPayloadId());
            isIncoming = true;
        } else if (outgoingFilePayloadNotifications.containsKey(update.getPayloadId())) {
            notification = outgoingFilePayloadNotifications.get(update.getPayloadId());
        }

        switch(update.getStatus()) {
        case PayloadTransferUpdate.Status.IN_PROGRESS:
            long size = update.getTotalBytes();
            float progress = (((float) update.getBytesTransferred()) / size) * 100;
            Log.d(TAG, String.format("progress value %d %d", update.getBytesTransferred(), update.getTotalBytes()));
            notification.setContentText(String.format("%2.02f percent complete", progress))
                    .setProgress(100, (int) progress, false);
            break;

        case PayloadTransferUpdate.Status.FAILURE:

            Log.d(TAG, "update failed: ");

            notification
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setContentText("Transfer failed");

            if(isIncoming) {
                mIncomingFilePayloads.remove(update.getPayloadId());
                incomingFilePayloadNotifications.remove(update.getPayloadId());
                mIncomingPayloadFileNames.remove(update.getPayloadId());
            }
            else
                outgoingFilePayloadNotifications.remove(update.getPayloadId());
            break;

        case PayloadTransferUpdate.Status.SUCCESS:
// SUCCESS always means that we transferred 100%.
            notification
                    .setProgress(100, 100, false /* indeterminate */)
                    .setOngoing(false)
                    .setContentText("Transfer complete!");

            if(isIncoming) {
                //finishing and renaming the files

                incomingFilePayloadNotifications.remove(update.getPayloadId());
                payload = mIncomingFilePayloads.remove(update.getPayloadId());

                String fileName = mIncomingPayloadFileNames.remove(payload.getId());
                Log.d(TAG, "successfully received a file " + fileName);

                File payloadFile = payload.asFile().asJavaFile();
                File newFile = new File(payloadFile.getParentFile(), fileName);
                payloadFile.renameTo(newFile);

            }
            else
                outgoingFilePayloadNotifications.remove(update.getPayloadId());
            break;
    }

        mNotificationManager.notify((int) update.getPayloadId(), notification.build());
    }




    @Override
    public String getName()
    {
        return Build.MODEL;
    }


    @Override
    public String getServiceId()
    {
        return SERVICEID;
    }



    private void startPlaying(final FileDescriptor fd)
    {
        Thread thread = new Thread() {
            @Override
            public void run()
            {
                setThreadPriority(THREAD_PRIORITY_AUDIO);

                MediaPlayer player = new MediaPlayer();

                try {

                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);

                    player.setDataSource(fd);
                    player.prepare();
                    player.start();

//player.release();
                }
                catch(Exception e)
                {
                    Log.d(TAG, "exception caught: " + e.getMessage());
                }
            }
        };

        thread.start();

    }


    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


}


