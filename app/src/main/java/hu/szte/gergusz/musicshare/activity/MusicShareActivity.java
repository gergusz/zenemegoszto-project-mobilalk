package hu.szte.gergusz.musicshare.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Objects;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.adapter.MusicAdapter;
import hu.szte.gergusz.musicshare.adapter.UriTypeAdapter;
import hu.szte.gergusz.musicshare.model.Music;

public class MusicShareActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private CollectionReference musicCollection;
    private RecyclerView musicRecyclerView;
    private ArrayList<Music> musicItemsData;
    private MusicAdapter musicAdapter;

    private boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_share);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();


        musicCollection = db.collection("music");

        musicRecyclerView = findViewById(R.id.musicRecyclerView);

        musicRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        musicItemsData = new ArrayList<>();

        musicCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                    Music music = document.toObject(Music.class);
                    assert music != null;
                    music.setFirebaseId(document.getId());

                    StorageReference albumArtRef = storage.getReference().child("albumArt/" + document.getId());
                    albumArtRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        music.setAlbumArtUri(uri);
                        musicAdapter.notifyItemChanged(musicItemsData.indexOf(music));
                    }).addOnFailureListener(e -> {
                        Log.e("MusicShareActivity", "Error getting album art uri", e);
                    });

                    musicItemsData.add(music);
                    musicAdapter.notifyItemInserted(musicItemsData.size() - 1);
                }
            } else {
                Log.d("MusicShareActivity", "Error getting documents: ", task.getException());
            }
        });

        musicAdapter = new MusicAdapter(this, musicItemsData);

        musicRecyclerView.setAdapter(musicAdapter);

    }

    public void showMusicInfo(@NonNull Music music) {
        Intent intent = new Intent(this, MusicInfoActivity.class);
        Gson gson = new GsonBuilder().registerTypeAdapter(Uri.class, new UriTypeAdapter()).create();
        intent.putExtra("music", gson.toJson(music));
        startActivity(intent);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(!backPressed){
            backPressed = true;
            Toast.makeText(this, R.string.pressBackAgain, Toast.LENGTH_SHORT).show();
        } else {
            finishAffinity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (user != null && !user.isAnonymous()) {
            getMenuInflater().inflate(R.menu.default_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.guest_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.musicUpload) {
            Intent intent = new Intent(this, AddMusicActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout) {
            auth.signOut();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}