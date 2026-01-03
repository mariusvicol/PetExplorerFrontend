package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import domain.User;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class Verify2FAActivity extends BaseActivity {
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
            GlobalErrorBus.postError("Eroare: nu s-a primit email-ul!");
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
           GlobalErrorBus.postError("Cod 2FA invalid!");
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

                        Intent intent = new Intent(Verify2FAActivity.this, MapsActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    GlobalErrorBus.postError("Cod incorect");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }
}

