package petexplorer.petexplorerclients;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import domain.User;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class MyAccountActivity extends BaseActivity {

    private EditText emailEdit, nameEdit, phoneEdit, passwordEdit;
    private Button saveButton;
    private int userId;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings);

        userId = getIntent().getIntExtra("USER_ID", -1);
        api = RetrofitClient.getApiService();
        Log.d("DEBUG_SETTINGS", "User ID primit: " + userId);


        emailEdit = findViewById(R.id.emailEditText);
        nameEdit = findViewById(R.id.nameEditText);
        phoneEdit = findViewById(R.id.phoneEditText);
        passwordEdit = findViewById(R.id.passwordEditText);
        saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        if (userId != -1) {
            loadUserData(userId);
        }

        saveButton.setOnClickListener(v -> updateUser(userId));
    }

    private void loadUserData(int userId) {
        api.getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    Log.d("DEBUG_API", "User primit: " + user.toString());
                    emailEdit.setText(user.getEmail());
                    nameEdit.setText(user.getNume());
                    phoneEdit.setText(user.getNrTelefon());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void updateUser(int userId) {
        User updatedUser = new User();
        updatedUser.setEmail(emailEdit.getText().toString());
        updatedUser.setNume(nameEdit.getText().toString());
        updatedUser.setNrTelefon(phoneEdit.getText().toString());
        updatedUser.setPassword(passwordEdit.getText().toString());

        api.updateUser(userId, updatedUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    GlobalErrorBus.postError("Profilul NU a fost actualizat!");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }
}
