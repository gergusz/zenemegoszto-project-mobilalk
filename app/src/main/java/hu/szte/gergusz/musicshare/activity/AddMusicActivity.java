package hu.szte.gergusz.musicshare.activity;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.content.CursorLoader;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.model.Music;

public class AddMusicActivity extends BaseActionBarActivity {

    private static final String TAG = "AddMusicActivity";
    FirebaseAuth auth;
    FirebaseStorage storage;
    TextView chosenSongPath;
    HorizontalScrollView chosenSongPathScrollView;
    ImageView chosenSongAlbumArt;
    TextInputEditText titleEditText;
    TextInputEditText artistEditText;
    CheckBox artistSameAsUploader;
    TextInputEditText albumEditText;
    AutoCompleteTextView genreAutoComplete;
    MediaPlayer mediaPlayer;
    Uri selectedSongUri;
    MaterialButton playPauseButton;
    TextView currentPos;
    TextView totalLength;
    SeekBar seekBar;
    List<String> genres;
    Music music;

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

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Music Share");

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null || auth.getCurrentUser().isAnonymous()) {
            finish();
        }

        storage = FirebaseStorage.getInstance();

        chosenSongPath = findViewById(R.id.chosenSongPath);

        chosenSongPathScrollView = findViewById(R.id.chosenSongPathScrollView);
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
                mediaPlayer.seekTo(seekBar.getProgress());
                seekBar.postDelayed(updateSeekBar, 100);
            }
        });

    }

    @NonNull
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
        mediaPlayer.release();
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

    private void updateSelected(Uri uri) {
        music = new Music();
        selectedSongUri = uri;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, selectedSongUri);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, selectedSongUri);
        music.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        music.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null ? retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) : retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        music.setAlbum(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        byte[] albumArt = retriever.getEmbeddedPicture();
        try {
            retriever.close();
        } catch (IOException e) {
            Log.e(TAG, "updateSelected: ", e);
        }
        if (albumArt != null) {
            Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length), 128, 128, false);
            music.setAlbumArt(bitmap);
            chosenSongAlbumArt.setImageBitmap(bitmap);
        } else {
            music.setAlbumArt(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_album_64));
            chosenSongAlbumArt.setImageResource(R.drawable.baseline_album_64);
        }
        chosenSongPath.setText(getFileName(selectedSongUri));
        titleEditText.setText(music.getTitle());
        artistEditText.setText(music.getArtist());
        albumEditText.setText(music.getAlbum());
        refreshTextAnimation();
        updateSeekBar.run();
    }

    public void playPauseSelectedSong(View view) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
                mediaPlayer.pause();
            } else {
                updateSeekBar.run();
                playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_pause_64));
                mediaPlayer.start();
            }
        }
    }

    public void stopSelectedSong(View view) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, selectedSongUri);
            playPauseButton.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_play_arrow_64));
        }
    }

    private final Runnable updateSeekBar = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            if (mediaPlayer != null) {
                int current = mediaPlayer.getCurrentPosition();
                int total = mediaPlayer.getDuration();
                totalLength.setText(String.format("%02d:%02d", total / 60000, (total % 60000) / 1000));
                currentPos.setText(String.format("%02d:%02d", current / 60000, (current % 60000) / 1000));
                seekBar.setMax(total);
                seekBar.setProgress(current, true);
                seekBar.postDelayed(this, 100);
                if (current + 1 >= total) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = MediaPlayer.create(AddMusicActivity.this, selectedSongUri);
                    playPauseButton.setIcon(AppCompatResources.getDrawable(AddMusicActivity.this, R.drawable.baseline_play_arrow_64));
                }
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
        music.setLength(mediaPlayer.getDuration());
        Log.d(TAG, "startUpload: " + music);

        FirebaseFirestore.getInstance().collection("music").add(music)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "startUpload: " + documentReference.getId());
                    storage.getReference().child("music").child(documentReference.getId()).putFile(selectedSongUri).addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "startUpload: " + Objects.requireNonNull(taskSnapshot.getMetadata()).getPath());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "startUpload: ", e);
                        Toast.makeText(this, "Valami hiba történt :(", Toast.LENGTH_SHORT).show();
                    });
                    
                    storage.getReference().child("albumArt").child(documentReference.getId()).putBytes(music._getAlbumArt())
                            .addOnSuccessListener(taskSnapshot -> {
                                Log.d(TAG, "startUpload: " + Objects.requireNonNull(taskSnapshot.getMetadata()).getPath());
                            })
                            .addOnFailureListener(e -> {
                        Log.e(TAG, "startUpload: ", e);
                        Toast.makeText(this, "Valami hiba történt :(", Toast.LENGTH_SHORT).show();
                    });

                    Toast.makeText(this, "Sikeres feltöltés!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "startUpload: ", e);
                    Toast.makeText(this, "Valami hiba történt :(", Toast.LENGTH_SHORT).show();
                });

    }
}