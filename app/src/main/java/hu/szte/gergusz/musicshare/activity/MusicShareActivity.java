package hu.szte.gergusz.musicshare.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Objects;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.adapter.MusicAdapter;
import hu.szte.gergusz.musicshare.model.Music;

public class MusicShareActivity extends BaseActionBarActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
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

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Music Share");

        auth = FirebaseAuth.getInstance();

        user = auth.getCurrentUser();

        musicRecyclerView = findViewById(R.id.musicRecyclerView);

        musicRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        musicItemsData = new ArrayList<>();

        musicAdapter = new MusicAdapter(this, musicItemsData);

        musicRecyclerView.setAdapter(musicAdapter);

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
}