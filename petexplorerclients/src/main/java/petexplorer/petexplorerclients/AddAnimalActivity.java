package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import domain.AnimalPierdut;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import domain.utils.AiDescriptionResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class AddAnimalActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LatLng selectedLatLng;
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private Uri selectedImageUri;
    private String tipCaz;

    private String getIdUserFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        return userId != -1 ? String.valueOf(userId) : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_animal);

        tipCaz = getIntent().getStringExtra("tipCaz");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        findViewById(R.id.btnSubmit).setOnClickListener(v -> submitAnimal());
        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> showImagePickerOptions());
        findViewById(R.id.btnGenerateAiDescription).setOnClickListener(v -> generateAiDescription());

        ImageButton btnBack = findViewById(R.id.btnBackToLostAnimals);
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Enable scrolling in description field
        EditText editDescriere = findViewById(R.id.editDescriere);
        editDescriere.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if ((event.getAction() & android.view.MotionEvent.ACTION_MASK) == android.view.MotionEvent.ACTION_UP) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }

    private void showImagePickerOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Selectează o opțiune")
                .setItems(new CharSequence[]{"Galerie", "Cameră"}, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Set Cluj-Napoca as default location
        LatLng cluj = new LatLng(46.7712, 23.6236);
        map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cluj, 13));

        map.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            map.clear();
            map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Locație selectată")
                    .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.location))));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            ImageView imagePreview = findViewById(R.id.imagePreview);
            Button btnGenerateAi = findViewById(R.id.btnGenerateAiDescription);

            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                selectedImageUri = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri == null && data.getExtras() != null) {
                    Toast.makeText(this, "Imagine capturată (thumbnail)", Toast.LENGTH_SHORT).show();
                }
            }

            if (selectedImageUri != null) {
                imagePreview.setImageURI(selectedImageUri);
                imagePreview.setVisibility(View.VISIBLE);
                btnGenerateAi.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Imagine selectată!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void submitAnimal() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Completează toate câmpurile", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MultipartBody.Part imagePart = null;
            if (selectedImageUri != null) {
                File imageFile = createTempFileFromUri(selectedImageUri);
                RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/*"));
                imagePart = MultipartBody.Part.createFormData("imagine", imageFile.getName(), requestFile);
            }

            RequestBody nume = RequestBody.create(getTextFromField(R.id.editNume), MediaType.parse("text/plain"));
            RequestBody descriere = RequestBody.create(getTextFromField(R.id.editDescriere), MediaType.parse("text/plain"));
            RequestBody lat = RequestBody.create(String.valueOf(selectedLatLng.latitude), MediaType.parse("text/plain"));
            RequestBody lng = RequestBody.create(String.valueOf(selectedLatLng.longitude), MediaType.parse("text/plain"));
            RequestBody caz = RequestBody.create(tipCaz, MediaType.parse("text/plain"));
            String telefonText = getTextFromField(R.id.editTelefon);
            RequestBody telefon = RequestBody.create(telefonText, MediaType.parse("text/plain"));
            String idUser = getIdUserFromPreferences();
            RequestBody rezolvat = RequestBody.create("false", MediaType.parse("text/plain"));
            RequestBody id_user = idUser != null
                    ? RequestBody.create(idUser, MediaType.parse("text/plain"))
                    : RequestBody.create("", MediaType.parse("text/plain"));


            ApiService apiService = RetrofitClient.getApiService();
            Call<AnimalPierdut> call = apiService.uploadAnimal(imagePart, nume, descriere, lat, lng, caz, telefon, id_user, rezolvat);

            call.enqueue(new Callback<AnimalPierdut>() {
                @Override
                public void onResponse(Call<AnimalPierdut> call, Response<AnimalPierdut> response) {
                    if (response.isSuccessful()) {
                        AnimalPierdut animal = response.body();
                        Toast.makeText(AddAnimalActivity.this, "Animal adăugat: " + animal.getNumeAnimal(), Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } else {
                        Toast.makeText(AddAnimalActivity.this, "Eroare la trimitere", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<AnimalPierdut> call, Throwable t) {
                    Toast.makeText(AddAnimalActivity.this, "Eroare rețea: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Eroare la procesarea fișierului", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(getCacheDir(), fileName);

        if (inputStream != null) {
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();
        }

        return tempFile;
    }


    private String getTextFromField(int fieldId) {
        EditText field = findViewById(fieldId);
        return field.getText().toString().trim();
    }

    private Bitmap getBitmapFromDrawable(@DrawableRes int resId) {
        Bitmap bitmap = null;
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), resId, null);

        if (drawable != null) {
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }

    private void generateAiDescription() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Te rog selectează o imagine mai întâi!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressBar progressBar = findViewById(R.id.progressBarDescription);
        EditText editDescriere = findViewById(R.id.editDescriere);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        progressBar.setVisibility(View.VISIBLE);
        editDescriere.setEnabled(false);
        btnSubmit.setEnabled(false);

        try {
            File imageFile = createTempFileFromUri(selectedImageUri);
            RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/*"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

            ApiService apiService = RetrofitClient.getPythonAiApiService();
            Call<AiDescriptionResponse> call = apiService.generateAiDescription(imagePart);

            call.enqueue(new Callback<AiDescriptionResponse>() {
                @Override
                public void onResponse(Call<AiDescriptionResponse> call, Response<AiDescriptionResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    editDescriere.setEnabled(true);
                    btnSubmit.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        AiDescriptionResponse aiResponse = response.body();
                        if ("success".equals(aiResponse.getStatus()) && aiResponse.getDescription() != null) {
                            editDescriere.setText(aiResponse.getDescription());
                            Toast.makeText(AddAnimalActivity.this, "Descriere generată cu succes!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = aiResponse.getMessage() != null ? aiResponse.getMessage() : "Eroare necunoscută";
                            Toast.makeText(AddAnimalActivity.this, "Eroare: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String errorMsg = "Eroare la generarea descrierii (Cod: " + response.code() + ")";
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(AddAnimalActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<AiDescriptionResponse> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    editDescriere.setEnabled(true);
                    btnSubmit.setEnabled(true);
                    String errorMsg = "Eroare rețea: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
                    Toast.makeText(AddAnimalActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    t.printStackTrace();
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            editDescriere.setEnabled(true);
            btnSubmit.setEnabled(true);
            e.printStackTrace();
            Toast.makeText(this, "Eroare la procesarea imaginii", Toast.LENGTH_SHORT).show();
        }
    }
}
