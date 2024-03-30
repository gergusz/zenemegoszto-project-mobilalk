package hu.szte.gergusz.musicshare;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MusicShareActivity extends AppCompatActivity {

    FirebaseUser user;
    FirebaseAuth auth;
    TextView dummyText;
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

        dummyText = findViewById(R.id.dummyText);
        user = auth.getCurrentUser();
        assert user != null;
        if (user.getEmail() != null){
            dummyText.setText(user.getEmail());
        } else {
            dummyText.setText(R.string.anonymousUser);
        }
    }
}