package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

import domain.User;
import domain.utils.LoginResponse;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class LoginActivity extends BaseActivity {
    private static final int RC_SIGN_IN = 9001;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleSignInButton;
    private GoogleSignInClient googleSignInClient;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("754830909329-mh2hdkceaca5bd097f09feaehdjfgbc7.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        android.util.Log.d("GoogleSignIn", "Package name: " + getPackageName());
        android.util.Log.d("GoogleSignIn", "Web Client ID: 754830909329-mh2hdkceaca5bd097f09feaehdjfgbc7.apps.googleusercontent.com");

        loginButton.setOnClickListener(v -> login());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void login() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            GlobalErrorBus.postError("Completati toate campurile!");
            return;
        }

        User loginRequest = new User();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse != null) {
                        User user = loginResponse.getUser();
                        Boolean requires2FA = loginResponse.getRequires2FA();

                        if (requires2FA != null && requires2FA) {
                            Intent intent = new Intent(LoginActivity.this, Verify2FAActivity.class);
                            intent.putExtra("email", user.getEmail());
                            startActivity(intent);
                            finish();
                        } else {
                            saveUserData(user);
                            navigateToMaps();
                        }
                    }
                } else {
                   GlobalErrorBus.postError("Email sau parola incorecte!");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {}
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null) {
                    Map<String, String> request = new HashMap<>();
                    request.put("idToken", idToken);

                    apiService.googleLogin(request).enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.isSuccessful()) {
                                LoginResponse loginResponse = response.body();
                                if (loginResponse != null) {
                                    User user = loginResponse.getUser();
                                    Boolean requires2FA = loginResponse.getRequires2FA();

                                    if (requires2FA != null && requires2FA) {
                                        Intent intent = new Intent(LoginActivity.this, Verify2FAActivity.class);
                                        intent.putExtra("email", user.getEmail());
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        saveUserData(user);
                                        navigateToMaps();
                                    }
                                }
                            } else {
                                String errorBody = "Unknown error";
                                try {
                                    if (response.errorBody() != null) {
                                        errorBody = response.errorBody().string();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                android.util.Log.e("GoogleSignIn", "Error: " + response.code() + " - " + errorBody);
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            android.util.Log.e("GoogleSignIn", "Network error", t);
                        }
                    });
                } else {
                    android.util.Log.e("GoogleSignIn", "ID token is null");
                }
            }
        } catch (ApiException e) {
            String errorMessage;

            switch (e.getStatusCode()) {
                case 10:
                    errorMessage = "Configurare incorectă. " +
                            "Te rugăm să contactezi echipa de suport.";
                    break;
                case 12500:
                    errorMessage = "Autentificare anulată.";
                    break;
                case 7:
                    errorMessage = "Eroare de conexiune." +
                            " Verifică conexiunea la internet.";
                    break;
                default:
                    errorMessage = "Autentificarea a eșuat." +
                            " Te rugăm să încerci din nou.";
            }

            GlobalErrorBus.postError(errorMessage);
            android.util.Log.e("GoogleSignIn",
                    "Error code: " + e.getStatusCode() + " - " +
                            e.getMessage(), e);
        }
    }

    private void saveUserData(User user) {
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        prefs.edit()
                .putInt("user_id", user.getId())
                .putString("email", user.getEmail())
                .putString("full_name", user.getNume())
                .apply();
    }

    private void navigateToMaps() {
        Intent intent = new Intent(LoginActivity.this,
                MapsActivity.class);
        startActivity(intent);
        finish();
    }
}

