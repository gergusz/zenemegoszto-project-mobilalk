package hu.szte.gergusz.musicshare.activity;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.model.Music;
import hu.szte.gergusz.musicshare.services.MusicService;

public class AddMusicActivity extends AppCompatActivity {
    private static final String TAG = "AddMusicActivity";
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private TextView chosenSongPath;
    private ImageView chosenSongAlbumArt;
    private TextInputEditText titleEditText;
    private TextInputEditText artistEditText;
    private CheckBox artistSameAsUploader;
    private TextInputEditText albumEditText;
    private AutoCompleteTextView genreAutoComplete;
    private Uri selectedSongUri;
    private MaterialButton playPauseButton;
    private TextView currentPos;
    private TextView totalLength;
    private SeekBar seekBar;
    private List<String> genres;
    private Music music;
    private int total;
    private MusicService musicService;
    private boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_music);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_64);
        myToolbar.setNavigationOnClickListener(v -> finish());
        getSupportActionBar().setTitle("Zene feltöltése...");


        if (auth.getCurrentUser() == null || auth.getCurrentUser().isAnonymous()) {
            finish();
        }

        storage = FirebaseStorage.getInstance();

        chosenSongPath = findViewById(R.id.chosenSongPath);

        refreshTextAnimation();
        chosenSongAlbumArt = findViewById(R.id.chosenSongAlbumArt);
        titleEditText = findViewById(R.id.titleEditText);
        artistEditText = findViewById(R.id.artistEditText);
        artistSameAsUploader = findViewById(R.id.artistSameAsUploader);
        albumEditText = findViewById(R.id.albumEditText);
        genreAutoComplete = findViewById(R.id.genreAutoComplete);
        genres = Arrays.asList(getResources().getStringArray(R.array.music_genres));
        playPauseButton = findViewById(R.id.playPauseButton);
        seekBar = findViewById(R.id.seekBar);
        currentPos = findViewById(R.id.currentSongPosition);
        totalLength = findViewById(R.id.totalSongLength);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_popup_item, genres);
        genreAutoComplete.setAdapter(adapter);

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

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void refreshTextAnimation() {
        int pathLength = chosenSongPath.getText().length();
        int duration = pathLength * 150;
        float xValue = (float) pathLength / 150;

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -xValue,
                Animation.RELATIVE_TO_PARENT, xValue,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);

        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        chosenSongPath.startAnimation(animation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound){
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void checkBoxClicked(View view) {
        if (artistSameAsUploader.isChecked()) {
            artistEditText.setText(Objects.requireNonNull(auth.getCurrentUser()).getDisplayName());
            artistEditText.setEnabled(false);
        } else {
            if (music != null) {
                artistEditText.setText(music.getArtist());
                artistEditText.setEnabled(true);
            } else {
                artistEditText.setText("");
                artistEditText.setEnabled(true);
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openDocument();
                } else {
                    Toast.makeText(this, "Megtagadtad a hozzáférést a média könyvtáradhoz, így nem tudok zenét tallózni!", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    musicService.create(uri);
                    updateSelected(uri);
                } else {
                    Toast.makeText(this, "Nem válaszottál ki fájlt!", Toast.LENGTH_SHORT).show();
                }
            });

    public void onBrowse(View view) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_AUDIO") == PERMISSION_GRANTED) {
            openDocument();
        } else {
            requestPermissionLauncher.launch("android.permission.READ_MEDIA_AUDIO");
        }
    }

    private void openDocument() {
        openDocumentLauncher.launch(new String[]{"audio/*"});
    }

    @SuppressLint("DefaultLocale")
    private void updateSelected(Uri uri) {
        music = new Music();
        selectedSongUri = uri;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, selectedSongUri);
        music.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        music.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) : retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        music.setAlbum(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        byte[] albumArtBytes = retriever.getEmbeddedPicture();
        try {
            retriever.close();
        } catch (IOException e) {
            Log.e(TAG, "updateSelected: ", e);
        }
        if (albumArtBytes != null) {
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.length);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 128, 128, false);
            chosenSongAlbumArt.setImageBitmap(originalBitmap);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            music.setAlbumArtBytes(stream.toByteArray());
        } else {
            chosenSongAlbumArt.setImageResource(R.drawable.baseline_album_64);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.baseline_album_64);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            music.setAlbumArtBytes(stream.toByteArray());
        }

        chosenSongPath.setText(getFileName(selectedSongUri));
        titleEditText.setText(music.getTitle());
        artistEditText.setText(music.getArtist());
        albumEditText.setText(music.getAlbum());
        total = musicService.getDuration();
        seekBar.setMax(total);
        totalLength.setText(String.format("%02d:%02d", total / 60000, (total % 60000) / 1000));
        updateSeekBar.run();
    }

    public void playPauseSelectedSong(View view) {
        if (isBound && selectedSongUri != null) {
            if (musicService.isPlaying()) {
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
                musicService.pause();
            } else {
                musicService.start();
                updateSeekBar.run();
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_pause_64));
            }
        }
    }

    public void stopSelectedSong(View view) {
        if (isBound) {
            musicService.stop();
            playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
        }
    }

    private final Runnable updateSeekBar = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
                int current = musicService.getCurrentPosition();
                currentPos.setText(String.format("%02d:%02d", current / 60000, (current % 60000) / 1000));
                seekBar.setProgress(current, true);
                seekBar.postDelayed(this, 100);
                if (!musicService.isPlaying()) {
                    playPauseButton.setIcon(AppCompatResources.getDrawable(AddMusicActivity.this, R.drawable.baseline_play_arrow_64));
                }
        }
    };

    public String getFileName(@NonNull Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    if (cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME) != -1) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    }
                }
            }
        }
        return result;
    }

    public void startUpload(View view) {
        if (music == null) {
            Toast.makeText(this, "Kérlek válassz ki egy zenét!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Objects.requireNonNull(titleEditText.getText()).toString().isEmpty()) {
            Toast.makeText(this, "Kérlek add meg a címét a zenédnek!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Objects.requireNonNull(artistEditText.getText()).toString().isEmpty()) {
            Toast.makeText(this, "Kérlek add meg az előadót a zenédnek!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Objects.requireNonNull(albumEditText.getText()).toString().isEmpty()) {
            Toast.makeText(this, "Kérlek add meg az albumot a zenédnek!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Objects.requireNonNull(genreAutoComplete.getText()).toString().isEmpty()) {
            Toast.makeText(this, "Kérlek válassz ki egy műfajt a zenédnek!", Toast.LENGTH_SHORT).show();
            return;
        }

        music.setTitle(Objects.requireNonNull(titleEditText.getText()).toString());
        music.setArtist(Objects.requireNonNull(artistEditText.getText()).toString());
        music.setAlbum(Objects.requireNonNull(albumEditText.getText()).toString());
        music.setGenre(Objects.requireNonNull(genreAutoComplete.getText()).toString());
        music.setUploaderId(Objects.requireNonNull(auth.getCurrentUser()).getUid());
        music.setLength(musicService.getDuration());
        Log.d(TAG, "startUpload: " + music);


        FirebaseFirestore.getInstance().collection("/music").add(music)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Sikeres dokumentum feltöltés: " + documentReference.getId());
                    UploadTask uploadMusicTask = storage.getReference().child("/music").child(documentReference.getId()).putFile(selectedSongUri);
                    Log.d(TAG, "startUpload: selectedSongUri: "+ selectedSongUri);
                    UploadTask uploadAlbumArtTask = storage.getReference().child("/albumArt").child(documentReference.getId()).putBytes(music.getAlbumArtBytes());

                    Tasks.whenAllSuccess(uploadMusicTask, uploadAlbumArtTask).addOnSuccessListener(list -> {
                        Log.d(TAG, "Sikeres zenefeltöltés és albumkép feltöltés");
                        Toast.makeText(this, "Sikeres feltöltés!", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> Log.e(TAG, "Hiba történt zenefeltöltés vagy albumkép feltöltése közben: ", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hiba történt dokumentum feltöltés közben:: ", e);
                });
    }
}