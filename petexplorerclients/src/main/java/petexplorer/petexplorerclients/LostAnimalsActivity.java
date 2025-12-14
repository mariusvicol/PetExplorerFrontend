package petexplorer.petexplorerclients;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import domain.AnimalPierdut;
import petexplorer.petexplorerclients.adapters.AnimalAdapter;
import petexplorer.petexplorerclients.notification.WebSocketStompClientManager;
import petexplorer.petexplorerclients.utils.ServerConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class LostAnimalsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RecyclerView recyclerView;
    private Button btnVeziPierdute, btnVeziGasite;
    private String cazCurent = "pierdut";
    private AnimalAdapter adapter;
    private static final int REQUEST_ADD_ANIMAL = 1001;
    private final java.util.HashMap<Marker, AnimalPierdut> markerAnimalMap = new java.util.HashMap<>();
    private final java.util.HashMap<String, Bitmap> imageCache = new java.util.HashMap<>();

    private WebSocketStompClientManager stompClientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_animals);

        stompClientManager = WebSocketStompClientManager.getInstance(this);
        stompClientManager.setOnAnimalReceivedListener(animal -> {
            runOnUiThread(() -> {
                if ("pierdut".equals(cazCurent)) {
                    loadAnimalePierdute();
                } else {
                    loadAnimaleGasite();
                }
            });
        });

        recyclerView = findViewById(R.id.animalsListRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnimalAdapter(new ArrayList<>(), animal -> {
            // Move camera to animal location and show InfoWindow
            LatLng location = new LatLng(animal.getLatitudine(), animal.getLongitudine());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

            // Find and show marker InfoWindow
            for (java.util.Map.Entry<Marker, AnimalPierdut> entry : markerAnimalMap.entrySet()) {
                if (entry.getValue().getId().equals(animal.getId())) {
                    entry.getKey().showInfoWindow();
                    break;
                }
            }
        });
        recyclerView.setAdapter(adapter);


        btnVeziPierdute = findViewById(R.id.btnVeziPierdute);
        btnVeziGasite = findViewById(R.id.btnVeziGasite);
        Button btnAddAnimal = findViewById(R.id.btnAddAnimal);

        btnVeziPierdute.setOnClickListener(v -> {
            cazCurent = "pierdut";
            loadAnimalePierdute();
            btnAddAnimal.setText("+ Animal Pierdut");
            btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
            btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));
        });

        btnVeziGasite.setOnClickListener(v -> {
            cazCurent = "vazut";
            loadAnimaleGasite();
            btnAddAnimal.setText("+ Am găsit un animal");
            btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
            btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.lost_animals_map);
        mapFragment.getMapAsync(this);

        loadAnimalePierdute();
        btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
        btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));

        btnAddAnimal.setOnClickListener(v -> {
            Intent intent = new Intent(LostAnimalsActivity.this, AddAnimalActivity.class);
            intent.putExtra("tipCaz", cazCurent);
            startActivityForResult(intent, REQUEST_ADD_ANIMAL);
        });

        ImageButton btnBackToMap = findViewById(R.id.btnBackToMap);
        btnBackToMap.setOnClickListener(v -> {
            Intent intent = new Intent(LostAnimalsActivity.this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_design));
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng initialLoc = new LatLng(46.770519, 23.590103);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLoc, 12));

        // Set custom InfoWindowAdapter
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Use default frame
            }

            @Override
            public View getInfoContents(Marker marker) {
                View view = LayoutInflater.from(LostAnimalsActivity.this)
                        .inflate(R.layout.custom_info_window_animal, null);

                TextView tvTitle = view.findViewById(R.id.tvInfoTitle);
                ImageView ivImage = view.findViewById(R.id.ivInfoImage);

                tvTitle.setText(marker.getTitle());

                // Get animal data from marker
                AnimalPierdut animal = markerAnimalMap.get(marker);
                if (animal != null && animal.getPoza() != null && !animal.getPoza().isEmpty()) {
                    String imageUrl = ServerConfig.BASE_URL + animal.getPoza();

                    // Check if image is cached
                    if (imageCache.containsKey(imageUrl)) {
                        ivImage.setImageBitmap(imageCache.get(imageUrl));
                    } else {
                        // Load image asynchronously and refresh InfoWindow
                        ivImage.setImageResource(R.drawable.dog2);
                        loadImageForMarker(imageUrl, marker);
                    }
                } else {
                    ivImage.setImageResource(R.drawable.dog2);
                }

                return view;
            }
        });
    }

    private void loadImageForMarker(String imageUrl, Marker marker) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.dog2)
                .error(R.drawable.warning)
                .into(new com.bumptech.glide.request.target.SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        imageCache.put(imageUrl, resource);
                        // Refresh the info window if it's currently showing
                        if (marker.isInfoWindowShown()) {
                            marker.hideInfoWindow();
                            marker.showInfoWindow();
                        }
                    }
                });
    }

    public void loadAnimalePierdute() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnimalPierdut>> call = apiService.getAnimalePierdute();

        call.enqueue(new Callback<List<AnimalPierdut>>() {
            @Override
            public void onResponse(Call<List<AnimalPierdut>> call, Response<List<AnimalPierdut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Raspunsul serverului: " + response.body());
                    List<AnimalPierdut> animaleList = response.body();
                    mMap.clear();
                    markerAnimalMap.clear();

                    for (AnimalPierdut animal : animaleList) {
                        if ("pierdut".equals(animal.getTipCaz()) && !animal.getRezolvat()) {
                            LatLng animalLocation = new LatLng(animal.getLatitudine(), animal.getLongitudine());

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(animalLocation)
                                    .title(animal.getNumeAnimal())
                                    .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.location))));

                            // Store marker and animal data
                            if (marker != null) {
                                markerAnimalMap.put(marker, animal);

                                // Preload image for InfoWindow
                                if (animal.getPoza() != null && !animal.getPoza().isEmpty()) {
                                    String imageUrl = ServerConfig.BASE_URL + animal.getPoza();
                                    if (!imageCache.containsKey(imageUrl)) {
                                        preloadImage(imageUrl);
                                    }
                                }
                            }
                        }
                    }
                    if (!animaleList.isEmpty()) {
                        LatLng firstLocation = new LatLng(animaleList.get(0).getLatitudine(), animaleList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                    adapter.updateData(
                            sorteazaLista(animaleList.stream()
                                    .filter(a -> "pierdut".equals(a.getTipCaz()))
                                    .collect(Collectors.toList()))
                    );

                } else {
                    Toast.makeText(LostAnimalsActivity.this, "Eroare la obținerea animalelor pierdute", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnimalPierdut>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(LostAnimalsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void loadAnimaleGasite() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnimalPierdut>> call = apiService.getAnimalePierdute();

        call.enqueue(new Callback<List<AnimalPierdut>>() {
            @Override
            public void onResponse(Call<List<AnimalPierdut>> call, Response<List<AnimalPierdut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Raspunsul serverului: " + response.body());
                    List<AnimalPierdut> animaleList = response.body();
                    mMap.clear();
                    markerAnimalMap.clear();

                    for (AnimalPierdut animal : animaleList) {
                        if ("vazut".equals(animal.getTipCaz()) && !animal.getRezolvat()) {
                            LatLng loc = new LatLng(animal.getLatitudine(), animal.getLongitudine());

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(loc)
                                    .title(animal.getNumeAnimal())
                                    .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.location))));

                            // Store marker and animal data
                            if (marker != null) {
                                markerAnimalMap.put(marker, animal);

                                // Preload image for InfoWindow
                                if (animal.getPoza() != null && !animal.getPoza().isEmpty()) {
                                    String imageUrl = ServerConfig.BASE_URL + animal.getPoza();
                                    if (!imageCache.containsKey(imageUrl)) {
                                        preloadImage(imageUrl);
                                    }
                                }
                            }
                        }
                    }
                    if (!animaleList.isEmpty()) {
                        LatLng firstLocation = new LatLng(animaleList.get(0).getLatitudine(), animaleList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                    adapter.updateData(
                            sorteazaLista(animaleList.stream()
                                    .filter(a -> "vazut".equals(a.getTipCaz()))
                                    .collect(Collectors.toList()))
                    );

                } else {
                    Toast.makeText(LostAnimalsActivity.this, "Eroare la obținerea animalelor vazute", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnimalPierdut>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(LostAnimalsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_ANIMAL && resultCode == RESULT_OK) {
            if ("pierdut".equals(cazCurent)) {
                loadAnimalePierdute();
            } else {
                loadAnimaleGasite();
            }
        }
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

    private void preloadImage(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.dog2)
                .error(R.drawable.warning)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        imageCache.put(imageUrl, resource);
                    }
                });
    }

    private List<AnimalPierdut> sorteazaLista(List<AnimalPierdut> lista) {
        return lista.stream()
                .sorted((a, b) -> {
                    try {
                        LocalDateTime dataA = LocalDateTime.parse(a.getDataCaz());
                        LocalDateTime dataB = LocalDateTime.parse(b.getDataCaz());

                        if (a.getRezolvat() && !b.getRezolvat()) return 1;
                        if (!a.getRezolvat() && b.getRezolvat()) return -1;

                        return dataB.compareTo(dataA);

                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

}
