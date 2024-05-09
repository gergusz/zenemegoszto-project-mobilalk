package hu.szte.gergusz.musicshare.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import hu.szte.gergusz.musicshare.R;

public class BaseActionBarActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (item.getItemId() == R.id.musicUpload) {
            if (auth.getCurrentUser() != null && !auth.getCurrentUser().isAnonymous()){
                if (!this.getClass().equals(AddMusicActivity.class)) {
                    Intent intent = new Intent(this, AddMusicActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Már itt vagy!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Vendég fiókkal nem tölthetsz fel zenét!", Toast.LENGTH_SHORT).show();
            }

            return true;
        } else if (item.getItemId() == R.id.logout) {
            auth.signOut();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
