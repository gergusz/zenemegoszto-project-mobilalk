package hu.szte.gergusz.musicshare.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.adapter.UriTypeAdapter;
import hu.szte.gergusz.musicshare.model.Music;
import hu.szte.gergusz.musicshare.model.UserData;
import hu.szte.gergusz.musicshare.services.MusicNotificationManager;
import hu.szte.gergusz.musicshare.services.MusicService;

public class MusicInfoActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private Music music;
    private ImageView albumArt;
    private TextView title;
    private TextView artist;
    private TextView album;
    private TextView genre;
    private TextView uploader;
    private MaterialButton playPauseButton;
    private SeekBar seekBar;
    private TextView currentPos;
    private TextView totalLength;
    private FirebaseFirestore firestore;
    private CollectionReference userDataCollection;
    private FirebaseStorage storage;
    private Uri musicUri;
    private int total;
    private MusicService musicService;
    private boolean isBound = false;
    private MusicNotificationManager musicNotificationManager;


    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        musicNotificationManager = new MusicNotificationManager(this);

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_64);
        myToolbar.setNavigationOnClickListener(v -> finish());

        Gson gson = new GsonBuilder().registerTypeAdapter(Uri.class, new UriTypeAdapter()).create();

        music = gson.fromJson(Objects.requireNonNull(getIntent().getStringExtra("music")), Music.class);

        getSupportActionBar().setTitle(music.getTitle());

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        albumArt = findViewById(R.id.albumArt);
        title = findViewById(R.id.infoTitle);
        artist = findViewById(R.id.infoArtist);
        album = findViewById(R.id.infoAlbum);
        genre = findViewById(R.id.infoGenre);
        uploader = findViewById(R.id.infoUploader);
        playPauseButton = findViewById(R.id.playPauseButton);
        seekBar = findViewById(R.id.seekBar);
        currentPos = findViewById(R.id.currentSongPosition);
        totalLength = findViewById(R.id.totalSongLength);
        uploader.setEnabled(false);

        Glide.with(this).load(music.getAlbumArtUri()).into(albumArt);

        title.setText(music.getTitle());
        artist.setText(music.getArtist());
        album.setText(music.getAlbum());
        genre.setText(music.getGenre());

        total = music.getLength();
        totalLength.setText(String.format("%02d:%02d", total / 60000, (total % 60000) / 1000));
        seekBar.setMax(total);

        firestore = FirebaseFirestore.getInstance();
        userDataCollection = firestore.collection("userData");
        storage = FirebaseStorage.getInstance();

        ProgressBar progressBar = findViewById(R.id.progressBar);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                uploader.setVisibility(View.GONE);
            });

            userDataCollection.document(music.getUploaderId()).get().addOnSuccessListener(documentSnapshot -> {
                String text = getString(R.string.uploadedBy, Objects.requireNonNull(documentSnapshot.get("username")));
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    uploader.setVisibility(View.VISIBLE);
                    uploader.setText(text);
                });
            });
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPos.setText(String.format("%02d:%02d", progress / 60000, (progress % 60000) / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(isBound){
                    musicService.seekTo(seekBar.getProgress());
                    seekBar.postDelayed(updateSeekBar, 100);
                }
            }
        });

    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            storage.getReference("music/" + music.getFirebaseId()).getDownloadUrl().addOnSuccessListener(uri -> {
                musicUri = uri;
                musicService.create(musicUri);
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final Runnable updateSeekBar = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            if(isBound){
                int current = musicService.getCurrentPosition();
                currentPos.setText(String.format("%02d:%02d", current / 60000, (current % 60000) / 1000));
                seekBar.setProgress(current, true);
                seekBar.postDelayed(this, 100);
                if (!musicService.isPlaying()) {
                    playPauseButton.setIcon(AppCompatResources.getDrawable(MusicInfoActivity.this, R.drawable.baseline_play_arrow_64));
                }
            }
        }
    };

    public void playPauseSelectedSong(View view) {
        if(isBound){
            if (musicService.isPlaying()) {
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
                musicService.pause();
            } else {
                updateSeekBar.run();
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_pause_64));
                musicService.start();
                musicNotificationManager.showNotification(music);
            }
        }
    }

    public void stopSelectedSong(View view) {
        if(isBound){
            musicService.stop();
            musicService.create(musicUri);
            playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (user.getUid().equals(music.getUploaderId())) {
            getMenuInflater().inflate(R.menu.edit_or_delete_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.editMusic && user.getUid().equals(music.getUploaderId())) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Uri.class, new UriTypeAdapter()).create();
            Intent intent = new Intent(this, EditMusicActivity.class);
            intent.putExtra("music", gson.toJson(music));
            startActivity(intent);
        } else if (item.getItemId() == R.id.deleteMusic && user.getUid().equals(music.getUploaderId())) {
            Task<Void> deleteFirestoreMusicTask = firestore.collection("music").document(music.getFirebaseId()).delete();
            Task<Void> deleteMusicTask = storage.getReference().child("/music").child(music.getFirebaseId()).delete();
            Task<Void> deleteAlbumArtTask = storage.getReference().child("/albumArt").child(music.getFirebaseId()).delete();

            Tasks.whenAllSuccess(deleteMusicTask, deleteAlbumArtTask, deleteFirestoreMusicTask).addOnSuccessListener(list -> finish()).addOnFailureListener(e -> {
                Log.e("MusicInfoActivity", "Törlés közben valami hiba történt: ", e);
                Toast.makeText(MusicInfoActivity.this, "Törlés közben valami hiba történt :(", Toast.LENGTH_SHORT).show();
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        musicNotificationManager.getNotificationManager().cancelAll();
    }
}