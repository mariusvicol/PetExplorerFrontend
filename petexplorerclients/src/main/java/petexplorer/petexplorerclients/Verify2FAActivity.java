package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import domain.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class Verify2FAActivity extends AppCompatActivity {
    private EditText codeEditText;
    private Button verifyButton;
    private String userEmail;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_2fa);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Eroare: Email lipsă!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        codeEditText = findViewById(R.id.codeEditText);
        verifyButton = findViewById(R.id.verifyButton);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        verifyButton.setOnClickListener(v -> verify2FA());
    }

    private void verify2FA() {
        String code = codeEditText.getText().toString().trim();

        if (code.isEmpty() || code.length() != 6) {
            Toast.makeText(this, "Introdu codul de 6 cifre!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("code", code);

        apiService.verify2FALogin(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    if (user != null) {
                        // Save user data
                        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
                        prefs.edit()
                                .putInt("user_id", user.getId())
                                .putString("email", user.getEmail())
                                .putString("full_name", user.getNume())
                                .apply();

                        // Navigate to MapsActivity
                        Intent intent = new Intent(Verify2FAActivity.this, MapsActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(Verify2FAActivity.this, "Cod 2FA incorect!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(Verify2FAActivity.this, "Eroare de rețea!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

