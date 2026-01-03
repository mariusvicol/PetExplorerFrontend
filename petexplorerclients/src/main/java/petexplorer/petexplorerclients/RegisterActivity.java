package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;


import androidx.appcompat.app.AppCompatActivity;

import domain.User;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class RegisterActivity extends BaseActivity {
    protected Button registerButton;
    protected EditText emailEditText, nameEditText, phoneEditText, passwordEditText, retryPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        emailEditText = findViewById(R.id.emailEditText);
        nameEditText = findViewById(R.id.nameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        retryPasswordEditText = findViewById(R.id.retryPasswordEditText);

        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> register());
    }

    private void register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if(emailEditText.getText().isEmpty() || passwordEditText.getText().isEmpty() || nameEditText.getText().isEmpty() || phoneEditText.getText().isEmpty()){
                GlobalErrorBus.postError("Toate câmpurile trebuie completate!");
            }else{
                if(phoneEditText.getText().length()>9){
                    ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
                    User registerRequest=new User();
                    registerRequest.setEmail(emailEditText.getText().toString());
                    registerRequest.setPassword(passwordEditText.getText().toString());
                    registerRequest.setNrTelefon(phoneEditText.getText().toString());
                    registerRequest.setNume(nameEditText.getText().toString());
                    apiService.register(registerRequest).enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> call, Response<User> response) {
                                if (response.isSuccessful()) {
                                    User user = response.body();
                                    SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
                                    prefs.edit()
                                            .putInt("user_id", user.getId())
                                            .putString("email", user.getEmail())
                                            .apply();

                                    Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<User> call, Throwable t) {

                            }
                        });
                    }else{
                     GlobalErrorBus.postError("Numărul de telefon trebuie să aibă cel puțin 9 cifre!");
                    }
                }
        }
    }
}
