package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class IntroActivity extends BaseActivity {

    protected Button loginOptionBtn, registerOptionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        loginOptionBtn = findViewById(R.id.loginOptionBtn);
        registerOptionBtn = findViewById(R.id.registerOptionBtn);

        loginOptionBtn.setOnClickListener(v -> onLoginOptionChosen());
        registerOptionBtn.setOnClickListener(v -> onRegisterOptionChosen());
    }

    private void onLoginOptionChosen() {
        Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void onRegisterOptionChosen() {
        Intent intent = new Intent(IntroActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

}
