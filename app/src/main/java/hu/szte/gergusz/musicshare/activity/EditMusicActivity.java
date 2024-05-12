package hu.szte.gergusz.musicshare.activity;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.google.firestore.v1.UpdateDocumentRequestOrBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.adapter.UriTypeAdapter;
import hu.szte.gergusz.musicshare.model.Music;
import hu.szte.gergusz.musicshare.services.MusicService;

public class EditMusicActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private Music music;
    private ImageView albumArt;
    private TextInputEditText titleEditText;
    private TextInputEditText artistEditText;
    private CheckBox artistSameAsUploader;
    private TextInputEditText albumEditText;
    private AutoCompleteTextView genreAutoComplete;
    private Uri musicUri;

    private List<String> genres;
    private boolean albumArtChanged = false;
    private TextView idTextView;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_music);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null || auth.getCurrentUser().isAnonymous()) {
            finish();
        }

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_64);
        myToolbar.setNavigationOnClickListener(v -> finish());

        Gson gson = new GsonBuilder().registerTypeAdapter(Uri.class, new UriTypeAdapter()).create();
        music = gson.fromJson(Objects.requireNonNull(getIntent().getStringExtra("music")), Music.class);

        getSupportActionBar().setTitle(music.getTitle() + " szerkesztése");

        albumArt = findViewById(R.id.chosenSongAlbumArt);
        titleEditText = findViewById(R.id.titleEditText);
        artistEditText = findViewById(R.id.artistEditText);
        artistSameAsUploader = findViewById(R.id.artistSameAsUploader);
        albumEditText = findViewById(R.id.albumEditText);
        genreAutoComplete = findViewById(R.id.genreAutoComplete);
        idTextView = findViewById(R.id.idTextView);
        genres = Arrays.asList(getResources().getStringArray(R.array.music_genres));

        titleEditText.setText(music.getTitle());
        artistEditText.setText(music.getArtist());
        albumEditText.setText(music.getAlbum());
        idTextView.setText(getString(R.string.idString, music.getFirebaseId()));

        Glide.with(this).load(music.getAlbumArtUri()).fitCenter().into(albumArt);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_menu_popup_item, genres);
        genreAutoComplete.setAdapter(adapter);
        genreAutoComplete.setText(music.getGenre());
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openDocument();
                } else {
                    Toast.makeText(this, "Megtagadtad a hozzáférést a képeidhez, így nem tudok zenét tallózni!", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    updateImageUri(uri);
                } else {
                    Toast.makeText(this, "Nem válaszottál ki képet!", Toast.LENGTH_SHORT).show();
                }
            });

    private void updateImageUri(Uri uri) {
        albumArtChanged = true;
        music.setAlbumArtUri(uri);
        Glide.with(this).load(uri).fitCenter().into(albumArt);
    }

    public void onBrowse(View view) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") == PERMISSION_GRANTED) {
            openDocument();
        } else {
            requestPermissionLauncher.launch("android.permission.READ_MEDIA_IMAGES");
        }
    }

    private void openDocument() {
        openDocumentLauncher.launch(new String[]{"image/*"});
    }


    public void checkBoxClicked(View view) {
        if (artistSameAsUploader.isChecked()) {
            artistEditText.setText(Objects.requireNonNull(auth.getCurrentUser()).getDisplayName());
            artistEditText.setEnabled(false);
        } else {
            artistEditText.setText(music.getArtist());
            artistEditText.setEnabled(true);
        }
    }


    public void startUpload(View view) {
        Map<String, Object> updatedValues = new HashMap<>();

        if (titleEditText.getText() == null || titleEditText.getText().toString().isEmpty()) {
            titleEditText.setError("A cím nem lehet üres!");
            return;
        }
        if (artistEditText.getText() == null || artistEditText.getText().toString().isEmpty()) {
            artistEditText.setError("Az előadó nem lehet üres!");
            return;
        }
        if (albumEditText.getText() == null || albumEditText.getText().toString().isEmpty()) {
            albumEditText.setError("Az album nem lehet üres!");
            return;
        }
        if (genreAutoComplete.getText() == null || genreAutoComplete.getText().toString().isEmpty()) {
            genreAutoComplete.setError("A műfaj nem lehet üres!");
            return;
        }

        if (!titleEditText.getText().toString().equals(music.getTitle())) {
            updatedValues.put("title", titleEditText.getText().toString());
        }
        if (!artistEditText.getText().toString().equals(music.getArtist())) {
            updatedValues.put("artist", artistEditText.getText().toString());
        }
        if (!albumEditText.getText().toString().equals(music.getAlbum())) {
            updatedValues.put("album", albumEditText.getText().toString());
        }
        if (!genreAutoComplete.getText().toString().equals(music.getGenre())) {
            updatedValues.put("genre", genreAutoComplete.getText().toString());
        }

        if (updatedValues.isEmpty() && !albumArtChanged) {
            Toast.makeText(this, "Nem változtattál semmin!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<Task<?>> taskList = new ArrayList<>();

        if (albumArtChanged){
            UploadTask updateAlbumArtTask = storage.getReference("albumArt/" + music.getFirebaseId()).putFile(music.getAlbumArtUri());
            taskList.add(updateAlbumArtTask);
        }

        Task<Void> updateFirestoreMusicTask = db.collection("music").document(music.getFirebaseId()).update(updatedValues);
        taskList.add(updateFirestoreMusicTask);

        Tasks.whenAllSuccess(taskList).addOnSuccessListener(list -> {
            Toast.makeText(this, "Sikeresen frissítetted a zenét!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Valami hiba történt a frissítés közben :(", Toast.LENGTH_SHORT).show();
        });

    }
}