package de.androidcrypto.androidplayaudiovideo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btn01, btn02, btn03, btn04,
            btn05, btn06, btn07, btn08;
    Button btn01mp, btn02mp, btn03mp, btn04mp,
            btn05mp, btn06mp, btn07mp, btn08mp;
    Button mpStart, mpStop, mpPause;
    Button btn01ep, btn02ep, btn03ep, btn04ep,
            btn05ep, btn06ep, btn07ep, btn08ep;


    TextView tvG07;
    EditText etG07;

    final int PICK_AUDIO_REQUEST = 11;
    Context context;

    // Instantiating the MediaPlayer class
    static MediaPlayer mediaPlayer = null;
    // Show played audio progress.
    private ProgressBar playAudioProgress;
    // Used when update audio progress thread send message to progress bar handler.
    private static final int UPDATE_AUDIO_PROGRESS_BAR = 3;
    // Wait update audio progress thread sent message, then update audio play progress.
    private Handler audioProgressHandler = null;
    // The thread that send message to audio progress handler to update progress every one second.
    private Thread updateAudioPalyerProgressThread = null;
    // Record whether audio is playing or not.
    private boolean audioIsPlaying = false;

    // new for check and grant permissions within the app
    private boolean isReadPermissionGranted = false;
    private boolean isWritePermissionGranted = false;
    //private boolean isCameraPermissionGranted = false;
    private boolean isInternetPermissionGranted = false;
    //private boolean isRecordAudioPermissionGranted = false;
    ActivityResultLauncher<String[]> myPermissionResultLauncher;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn01 = findViewById(R.id.btn01);
        btn02 = findViewById(R.id.btn02);
        btn03 = findViewById(R.id.btn03);
        btn04 = findViewById(R.id.btn04);
        btn05 = findViewById(R.id.btn05);
        btn06 = findViewById(R.id.btn06);
        btn07 = findViewById(R.id.btn07);
        btn08 = findViewById(R.id.btn08);

        btn01mp = findViewById(R.id.btn01mp);
        btn01ep = findViewById(R.id.btn01ep);
        btn02mp = findViewById(R.id.btn02mp);
        btn02ep = findViewById(R.id.btn02ep);

        mpStart = findViewById(R.id.btnMpStart);
        mpStop = findViewById(R.id.btnMpStop);
        mpPause = findViewById(R.id.btnMpPause);

        tvG07 = findViewById(R.id.tvG07);
        etG07 = findViewById(R.id.etG07E01);

        playAudioProgress = (ProgressBar)findViewById(R.id.play_audio_progressbar);
        /* Initialize audio progress handler. */
        if(audioProgressHandler==null) {
            audioProgressHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == UPDATE_AUDIO_PROGRESS_BAR) {
                        if(mediaPlayer!=null) {
                            // Get current play time.
                            int currPlayPosition = mediaPlayer.getCurrentPosition();
                            // Get total play time.
                            int totalTime = mediaPlayer.getDuration();
                            // Calculate the percentage.
                            //int currProgress = (currPlayPosition * 1000) / totalTime;
                            int currProgress = (currPlayPosition * 100) / totalTime;
                            /*
                            System.out.println("### cuPos: " + currPlayPosition +
                                    " toTime: " + totalTime +
                                    " cuProgr: " + currProgress);
                            */
                            // Update progressbar.
                            playAudioProgress.setProgress(currProgress);
                        }
                    }
                }
            };
        }

        btn01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pick an audio file and play it
                System.out.println("*** pick an audio file and play it with internal player");
                tvG07.setText("");
                context = v.getContext();
                Intent audioIntent = new Intent();
                audioIntent.setType("audio/*");
                //audioIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                audioIntent.setAction(Intent.ACTION_GET_CONTENT);
                //startActivityForResult(audioIntent,PICK_AUDIO_REQUEST); // deprecated
                pickAudioFileAndPlayItViaIntentActivityResultLauncher.launch(audioIntent);
/*                // check that an audio player is available
                if (audioIntent.resolveActivity(getPackageManager()) != null) {
                    pickAudioFileAndPlayItViaIntentActivityResultLauncher.launch(audioIntent);
                    //startActivityForResult(audioIntent,PICK_AUDIO_REQUEST); // deprecated
                } else {
                    tvG07.setText("ERROR: no audio player available on phone");
                }

 */
            }
        });

        btn01mp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("*** pick an audio file and play it with media player");
                tvG07.setText("");
                context = v.getContext();
                Intent audioIntent = new Intent();
                audioIntent.setType("audio/*");
                //audioIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                audioIntent.setAction(Intent.ACTION_GET_CONTENT);
                //startActivityForResult(audioIntent,PICK_AUDIO_REQUEST); // deprecated
                pickAudioFileAndPlayItViaMediaplayerActivityResultLauncher.launch(audioIntent);
            }
        });

        mpStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo check for state. After stopping don't start again
                // start called in state 64, mPlayer
                mediaPlayer.start();
            }
        });

        mpStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                updateAudioPalyerProgressThread = null;
                playAudioProgress.setProgress(0);
                audioIsPlaying = false;
            }
        });

        mpPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.pause();
                updateAudioPalyerProgressThread = null;
            }
        });

        btn01ep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("*** pick an audio file and play it with exo player");
                tvG07.setText("");
                context = v.getContext();
                Intent audioIntent = new Intent();
                audioIntent.setType("audio/*");
                //audioIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                audioIntent.setAction(Intent.ACTION_GET_CONTENT);
                //startActivityForResult(audioIntent,PICK_AUDIO_REQUEST); // deprecated
                //pickAudioFileAndPlayItViaExoplayerActivityResultLauncher.launch(audioIntent);
            }
        });

        btn02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pick a video file and play it
                System.out.println("*** pick a video file and play it with internal player");
                tvG07.setText("");
                context = v.getContext();
                Intent videoIntent = new Intent();
                videoIntent.setType("video/*");
                //videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                videoIntent.setAction(Intent.ACTION_GET_CONTENT);
                pickVideoFileAndPlayItViaIntentActivityResultLauncher.launch(videoIntent);
                /*// check that a video player is available
                if (videoIntent.resolveActivity(getPackageManager()) != null) {
                    pickVideoFileAndPlayItViaIntentActivityResultLauncher.launch(videoIntent);
                } else {
                    tvG07.setText("ERROR: no video player available on phone");
                }*/
            }
        });

        btn03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // enter an url and play it
/*
<!-- Permissions of internet -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
                // sample data: https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3
                tvG07.setText("");
                String url = etG07.getText().toString();
                if (url.equals("")) {
                    tvG07.setText("ERROR: paste an audio URL to play");
                    return;
                }
                Uri uri = Uri.parse(url);
                Intent viewMediaIntent = new Intent();
                viewMediaIntent.setAction(Intent.ACTION_VIEW);
                viewMediaIntent.setDataAndType(uri, "audio/*");
                viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(viewMediaIntent);
                if (viewMediaIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(viewMediaIntent);
                } else {
                    tvG07.setText("ERROR: something got wrong (e.g. false URL ? no internet ?)");
                }

            }
        });

        btn04.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// enter an url and play it
/*
<!-- Permissions of internet -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
                // sample data: https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_2MB.mp4
                tvG07.setText("");
                String url = etG07.getText().toString();
                if (url.equals("")) {
                    tvG07.setText("ERROR: paste a video URL to play");
                    return;
                }
                Uri uri = Uri.parse(url);
                Intent viewMediaIntent = new Intent();
                viewMediaIntent.setAction(Intent.ACTION_VIEW);
                viewMediaIntent.setDataAndType(uri, "video/*");
                viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(viewMediaIntent);
                if (viewMediaIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(viewMediaIntent);
                } else {
                    tvG07.setText("ERROR: something got wrong (e.g. false URL ? no internet ?)");
                }

            }
        });

        btn05.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("*** list audio files ***");
                // funktioniert
                // siehe auch: https://stackoverflow.com/a/37496502/8166854

                ArrayList audio=new ArrayList();
                Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.AudioColumns.DATA,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.ArtistColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.TRACK,
                        MediaStore.Audio.AudioColumns.DURATION};
                //Cursor c=getContentResolver().query(uri, new String[]{MediaStore.Audio.Media.DISPLAY_NAME}, null, null, null);
                Cursor c=getContentResolver().query(uri, projection, null, null, null);
                while(c.moveToNext())
                {
                    if (c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME) >= 0) {
                        @SuppressLint("Range") String name=c.getString(c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                        audio.add(name);
                        @SuppressLint("Range") String duration = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        @SuppressLint("Range") String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        @SuppressLint("Range") String album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        @SuppressLint("Range") String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        @SuppressLint("Range") String track = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TRACK));

                        //convert the song duration into string reading hours, mins seconds
                        int dur = Integer.valueOf(duration);
                        int hrs = (dur / 3600000);
                        int mns = (dur / 60000) % 60000;
                        int scs = dur % 60000 / 1000;
                        String songTime = String.format("%02d:%02d:%02d", hrs,  mns, scs);

                        String output = "# name: " + name +
                                " artist: " + artist +
                                " album: " + album +
                                " title: " + title +
                                " track: " + track +
                                " duration: " + duration +
                                " songTime: " + songTime;
                        //"";
                        System.out.println("# output: " + output);
                        tvG07.setText(output);
                    }
                }
            }
        });

        btn06.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        btn07.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btn08.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        myPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.READ_EXTERNAL_STORAGE) != null) {
                    isReadPermissionGranted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                if (result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) != null) {
                    isWritePermissionGranted = result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if (result.get(Manifest.permission.INTERNET) != null) {
                    isInternetPermissionGranted = result.get(Manifest.permission.INTERNET);
                }

                /*
                if (result.get(Manifest.permission.CAMERA) != null) {
                    isCameraPermissionGranted = result.get(Manifest.permission.CAMERA);
                }
                */
                /*if (result.get(Manifest.permission.RECORD_AUDIO) != null) {
                    isRecordAudioPermissionGranted = result.get(Manifest.permission.RECORD_AUDIO);
                }*/
            }
        });

        // check and request read and write permissions
        // don't place this above
        // mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>()
        requestPermission();

    }

    ActivityResultLauncher<Intent> pickAudioFileAndPlayItViaIntentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        System.out.println("pickAudioFileAndPlayItViaIntentActivityResultLauncher");
                        // There are no request codes
                        Intent resultData = result.getData();
                        Uri uri = resultData.getData();
                        Intent viewMediaIntent = new Intent();
                        viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);
                        viewMediaIntent.setDataAndType(uri, "audio/*");
                        viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(viewMediaIntent);
                    }
                }
            });

    ActivityResultLauncher<Intent> pickAudioFileAndPlayItViaMediaplayerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        System.out.println("pickAudioFileAndPlayItViaMediaplayerActivityResultLauncher");
                        // There are no request codes
                        Intent resultData = result.getData();
                        Uri uri = resultData.getData();

                        //Uri myUri = ....; // initialize Uri here
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                        );
                        try {
                            mediaPlayer.setDataSource(getApplicationContext(), uri);
                            mediaPlayer.prepare();
                            mediaPlayer.start();

                            // progressbar
                            audioIsPlaying = true;
                            // Display progressbar.
                            playAudioProgress.setVisibility(ProgressBar.VISIBLE);
                            if(updateAudioPalyerProgressThread == null) {
                                // Create the thread.
                                updateAudioPalyerProgressThread = new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            while (audioIsPlaying) {
                                                if (audioProgressHandler != null) {
                                                    // Send update audio player progress message to main thread message queue.
                                                    Message msg = new Message();
                                                    msg.what = UPDATE_AUDIO_PROGRESS_BAR;
                                                    audioProgressHandler.sendMessage(msg);
                                                    Thread.sleep(1000);
                                                }
                                            }
                                        } catch (InterruptedException ex) {
                                            Log.e("TAG_PLAY_AUDIO", ex.getMessage(), ex);
                                        }
                                    }
                                };
                                updateAudioPalyerProgressThread.start();
                            }



                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        /*
                        Intent viewMediaIntent = new Intent();
                        viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);
                        viewMediaIntent.setDataAndType(uri, "audio/*");
                        viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(viewMediaIntent);*/
                    }
                }
            });


    ActivityResultLauncher<Intent> pickVideoFileAndPlayItViaIntentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        System.out.println("pickVideoFileAndPlayItViaIntentActivityResultLauncher");
                        // There are no request codes
                        Intent resultData = result.getData();
                        Uri uri = resultData.getData();
                        Intent viewMediaIntent = new Intent();
                        viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);
                        viewMediaIntent.setDataAndType(uri, "video/*");
                        viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(viewMediaIntent);
                    }
                }
            });


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            // error
            return;
        }
        if (requestCode == PICK_AUDIO_REQUEST) {
            try {
                System.out.println("*** PICK_AUDIO_REQUEST");
                Uri uri = data.getData();
                Intent viewMediaIntent = new Intent();
                viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);
                viewMediaIntent.setDataAndType(uri, "audio/*");
                viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(viewMediaIntent);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private void contentResolverExample() {
        // https://developer.android.com/guide/topics/media/mediaplayer#viacontentresolver
        ContentResolver contentResolver = getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            // query failed, handle error.
        } else if (!cursor.moveToFirst()) {
            // no media on the device
        } else {
            int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            do {
                long thisId = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                // ...process entry...
            } while (cursor.moveToNext());
        }
    }

    private void contentResolverRun() {
        long id = 1L; /* retrieve it from somewhere */
        ;
        Uri contentUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            mediaPlayer.setDataSource(getApplicationContext(), contentUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ...prepare and start...
    }

    /*
    // https://stackoverflow.com/q/53869942/8166854
    public void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.ArtistColumns.ARTIST,};
        Cursor songCursor = contentResolver.query(songUri, projection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%/storage/emulated/0/Download/Music/%"}, null, null);
        Log.e("COUNT", "" + songCursor.getCount());

        if (songCursor != null && songCursor.moveToFirst()) {
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songPath = songCursor.getColumnIndex((MediaStore.Audio.Media.DATA));

            do {
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                String currentPath = songCursor.getString(songPath);
                Uri uriSong = MediaStore.Audio.Media.getContentUriForPath(currentPath);

                // Testing purposes
                Log.e("TEST", "Name: " + currentTitle + " Artist: " + currentArtist + " Path: " + currentPath + uriSong.toString());

                songList.add(new Song(currentPath, "Artist", currentPath));
            } while (songCursor.moveToNext());
        }
        songCursor.close();
        Log.e("LIST", "" + songList.size());
    }
*/

    // https://riptutorial.com/android/example/23916/fetch-audio-mp3-files-from-specific-folder-of-device-or-fetch-all-files
    // list all audio files on device
    public List<AudioModel> getAllAudioFromDevice(final Context context) {
        final List<AudioModel> tempAudioList = new ArrayList<>();
        System.out.println("*** getAllAudioFromDevice ***");

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.ArtistColumns.ARTIST,};
        Cursor c = context.getContentResolver().query(uri, projection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%utm%"}, null);

        // In order to read files of a specific folder, use the following query (you need to replace the folder name):
        Cursor c2 = context.getContentResolver().query(uri,projection,MediaStore.Audio.Media.DATA + " like ? ",new String[]{"%yourFolderName%"}, null);
        // If you want to retrieve all files from your device, then use the following query:
        Cursor c3 = context.getContentResolver().query(uri,projection,null,null,null);

        if (c != null) {
            while (c.moveToNext()) {
                AudioModel audioModel = new AudioModel();
                String path = c.getString(0);
                String name = c.getString(1);
                String album = c.getString(2);
                String artist = c.getString(3);

                audioModel.setaName(name);
                audioModel.setaAlbum(album);
                audioModel.setaArtist(artist);
                audioModel.setaPath(path);

                Log.d("Name :" + name, " Album :" + album);
                Log.d("Path :" + path, " Artist :" + artist);

                tempAudioList.add(audioModel);
            }
            c.close();
        } else {
            System.out.println("*** c == NULL ***");
        }
        return tempAudioList;
    }



    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

// new for permissions start

    private void requestPermission() {
        boolean minSDK = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        isReadPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
        isWritePermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
        isWritePermissionGranted = isWritePermissionGranted || minSDK;
        isInternetPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED;

        /*
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;

         */
        /*isRecordAudioPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;*/
        List<String> permissionRequest = new ArrayList<String>();
        if (!isReadPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!isWritePermissionGranted) {
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!isInternetPermissionGranted) {
            permissionRequest.add(Manifest.permission.INTERNET);
        }
        /*if (!isCameraPermissionGranted) {
            permissionRequest.add(Manifest.permission.CAMERA);
        }*/
        /*if (!isRecordAudioPermissionGranted) {
            permissionRequest.add(Manifest.permission.RECORD_AUDIO);
        }*/
        if (!permissionRequest.isEmpty()) {
            myPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }
    // new for permissions end

}