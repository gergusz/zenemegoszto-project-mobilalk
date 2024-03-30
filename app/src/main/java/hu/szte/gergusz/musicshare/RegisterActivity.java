package hu.szte.gergusz.musicshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;


import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private static final String LOG_TAG = RegisterActivity.class.getName();
    private static final int SECRET_KEY = 42069420;
    //EditText usernameEditText;
    EditText emailEditText;
    EditText passwordEditText;
    EditText passwordAgainEditText;
    //RadioGroup roleRadioGroup;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if (secret_key != 42069420) {
            finish();
        }

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordAgainEditText = findViewById(R.id.passwordAgainEditText);
    }

    public void backToLogin(View view) {
        finish();
    }

    public void onRegister(View view) {

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordAgain = passwordAgainEditText.getText().toString();

        if (!password.equals(passwordAgain)) {
            Toast.makeText(this, "A két jelszó nem egyezik meg!", Toast.LENGTH_LONG).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "A jelszó túl rövid!", Toast.LENGTH_LONG).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(LOG_TAG, "createUserWithEmail:success");
                Intent intent = new Intent(this, MusicShareActivity.class);
                intent.putExtra("SECRET_KEY", SECRET_KEY);
                startActivity(intent);
            } else {
                Log.w(LOG_TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(this, "Sikertelen regisztráció!\n" + Objects.requireNonNull(task.getException()).getMessage() + "\nPróbáld újra!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}