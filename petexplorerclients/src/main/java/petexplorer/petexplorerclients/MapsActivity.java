package petexplorer.petexplorerclients;

import static android.content.ContentValues.TAG;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.widget.Button;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import domain.CabinetVeterinar;
import domain.Farmacie;
import domain.Magazin;
import domain.Parc;
import domain.PensiuneCanina;
import domain.Salon;
import domain.utils.CustomInfoWindowData;
import domain.utils.LocatieFavoritaDTO;
import domain.utils.SearchResultDTO;
import petexplorer.petexplorerclients.databinding.ActivityMapsBinding;
import petexplorer.petexplorerclients.notification.WebSocketStompClientManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private int currentUserId;

    private ActivityMapsBinding binding;
    private Button filterButton;
    private final int FINE_PERMISSION_CODE = 1;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    private Map<String, List<Integer>> favoritePlaces = new HashMap<>();
    private List<LocatieFavoritaDTO> favoritePlacesList = new ArrayList<>();
    private WebSocketStompClientManager stompClientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        this.currentUserId = prefs.getInt("user_id", -1);
        String fullName = prefs.getString("full_name", "utilizator");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        stompClientManager = WebSocketStompClientManager.getInstance(this);

        initializeFavoritePlacesMap(); // sa n-o incarc de fiecare data

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageButton menuButton = findViewById(R.id.menuButton);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // pentru numele custom la deschiderea meniului
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderTitle = headerView.findViewById(R.id.nav_header_title);
        navHeaderTitle.setText("Salut, " + fullName + "!");

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        Button animalePierduteButton = findViewById(R.id.animalePierduteButton);


        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_account) {
                Intent intent = new Intent(this, MyAccountActivity.class);
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);

            } else if (id == R.id.nav_favorites) {
                Toast.makeText(this, "Favorite", Toast.LENGTH_SHORT).show();
                loadFavLocationsForUser();

            } else if (id == R.id.nav_lost_pets) {
                Toast.makeText(this, "Anunțurile mele", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MapsActivity.this, MyAnnouncementsActivity.class);
                startActivity(intent);

			} else if (id == R.id.nav_pet_sitting) {
				Intent intent = new Intent(this, PetSittingActivity.class);
				startActivity(intent);
			} else if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsGeneralActivity.class);
                startActivity(intent);

            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Delogare", Toast.LENGTH_SHORT).show();
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(view -> showBottomSheet());


        animalePierduteButton.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, LostAnimalsActivity.class);
            startActivity(intent);
        });
    }


    private void showBottomSheet() {
        FiltrareBottomSheetFragment bottomSheet = new FiltrareBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_design));
        mMap = googleMap;

        // Verificăm permisiunile pentru locație
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Permitem utilizarea locației pe hartă
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Activăm controalele de zoom
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Creăm un FusedLocationProviderClient
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Solicităm locația curentă
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Adăugăm un marker (personalizat) pe hartă și mutăm camera
                    mMap.clear();
                    var markerCustom = new MarkerOptions().position(userLocation);
                    markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.userloc)));

                    mMap.addMarker(markerCustom.title("Locația curentă"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                } else {
                    Toast.makeText(MapsActivity.this, "Locația curentă nu poate fi obținută", Toast.LENGTH_SHORT).show();
                }
            }
        });
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Permisiunea de locație este necesară", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void loadVeterinaryOffices() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<CabinetVeterinar>> call = apiService.getCabineteVeterinare();
        call.enqueue(new Callback<List<CabinetVeterinar>>() {
            @Override
            public void onResponse(Call<List<CabinetVeterinar>> call, Response<List<CabinetVeterinar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Răspunsul serverului: " + response.body().toString()); // Log la răspunsul primit
                    List<CabinetVeterinar> cabinetVeterinarList = response.body();
                    mMap.clear();
                    for (CabinetVeterinar cabinet : cabinetVeterinarList) {
                        LatLng cabinetLocation = new LatLng(cabinet.getLatitudine(), cabinet.getLongitudine());
                        Log.d("DEBUG", "Cabinet: " + cabinet.getNumeCabinet() + " Lat: " + cabinet.getLatitudine() + " Long: " + cabinet.getLongitudine());

                        var markerCustom = new MarkerOptions().position(cabinetLocation);
                        markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.cabinet)));

                        var marker = mMap.addMarker(markerCustom.title(cabinet.getNumeCabinet()));
                        if (marker != null) {
                            String programText = cabinet.getNonStop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            String nrTel = cabinet.getNrTelefon() != null
                                    ? cabinet.getNrTelefon()
                                    : "Număr de telefon neafișat.";

                            boolean isFavorite = favoritePlaces.containsKey("cabinet") &&
                                                favoritePlaces.get("cabinet").contains(cabinet.getId());

                            marker.setTag(new CustomInfoWindowData(
                                    cabinet.getNumeCabinet(),
                                    nrTel,
                                    programText,
                                    R.drawable.cabinet,
                                    isFavorite,
                                    "cabinet",
                                    cabinet.getId()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);

                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });

                    }
                    if (!cabinetVeterinarList.isEmpty()) {
                        LatLng firstLocation = new LatLng(cabinetVeterinarList.get(0).getLatitudine(), cabinetVeterinarList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                } else {
                    Log.e(TAG, "Eroare răspuns server: " + response.code()); // Log pentru codul de răspuns al serverului
                    Toast.makeText(MapsActivity.this, "Eroare la obținerea cabinetelor veterinare", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CabinetVeterinar>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadPensiuni() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<PensiuneCanina>> call = apiService.getPensiuniCanine();

        call.enqueue(new Callback<List<PensiuneCanina>>() {
            @Override
            public void onResponse(Call<List<PensiuneCanina>> call, Response<List<PensiuneCanina>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Răspunsul serverului: " + response.body());
                    List<PensiuneCanina> pensiuniList = response.body();
                    mMap.clear();
                    for (PensiuneCanina p : pensiuniList) {
                        LatLng pLoc = new LatLng(p.getLatitude(), p.getLongitude());
                        Log.d("DEBUG", "Pensiune: " + p.getName() + " Lat: " + p.getLatitude() + " Long: " + p.getLongitude());

                        var markerCustom = new MarkerOptions().position(pLoc);
                        markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.pensiune)));

                        var marker = mMap.addMarker(markerCustom.title(p.getName()));
                        if (marker != null) {
                            String programText = p.getNon_stop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            String nrTel = p.getNrTel() != null
                                    ? p.getNrTel()
                                    : "Număr de telefon neafișat.";

                            boolean isFavorite = favoritePlaces.containsKey("pensiune") &&
                                    favoritePlaces.get("pensiune").contains(p.getId());

                            marker.setTag(new CustomInfoWindowData(
                                    p.getName(),
                                    nrTel,
                                    programText,
                                    R.drawable.pensiune,
                                    isFavorite,
                                    "pensiune",
                                    p.getId()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);
                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });
                    }
                    if (!pensiuniList.isEmpty()) {
                        LatLng firstLocation = new LatLng(pensiuniList.get(0).getLatitude(), pensiuniList.get(0).getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                } else {
                    Log.e(TAG, "Eroare răspuns server: " + response.code());
                    Toast.makeText(MapsActivity.this, "Eroare la obținerea pensiunilor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PensiuneCanina>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
     }

     public void loadSaloane() {
         ApiService apiService = RetrofitClient.getApiService();
         Call<List<Salon>> call = apiService.getSaloane();

         call.enqueue(new Callback<List<Salon>>() {
             @Override
             public void onResponse(Call<List<Salon>> call, Response<List<Salon>> response) {
                 if (response.isSuccessful() && response.body() != null) {
                     Log.d(TAG, "Răspunsul serverului: " + response.body());
                     List<Salon> saloaneList = response.body();
                     mMap.clear();
                     for (Salon s : saloaneList) {
                         LatLng sLoc = new LatLng(s.getLatitude(), s.getLongitude());
                         Log.d("DEBUG", "Salon: " + s.getName()+ " Lat: " + s.getLatitude() + " Long: " + s.getLongitude());

                         var markerCustom = new MarkerOptions().position(sLoc);
                         markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.salon)));

                         var marker = mMap.addMarker(markerCustom.title(s.getName()));
                         if (marker != null) {
                             String programText = s.getNon_stop()
                                     ? "Program non-stop"
                                     : "Disponibil în timpul programului de lucru";

                             String nrTel = s.getNrTel() != null
                                     ? s.getNrTel()
                                     : "Număr de telefon neafișat.";

                             boolean isFavorite = favoritePlaces.containsKey("salon") &&
                                     favoritePlaces.get("salon").contains(s.getId());

                             marker.setTag(new CustomInfoWindowData(
                                     s.getName(),
                                     nrTel,
                                     programText,
                                     R.drawable.salon,
                                     isFavorite,
                                     "salon",
                                     s.getId()));
                         }

                         // listener dupa ce markerii sunt adaugati
                         mMap.setOnMarkerClickListener(m -> {
                             Object tag = m.getTag();
                             if (tag instanceof CustomInfoWindowData) {
                                 CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                 data.setLatitude(marker.getPosition().latitude);
                                 data.setLongitude(marker.getPosition().longitude);
                                 PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                 bottomSheet.setData(data, currentUserId);

                                 bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                     if (added) {
                                         String type = locatie.getType().toLowerCase();
                                         favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                         favoritePlaces.get(type).add(locatie.getIdLocation());
                                     } else {
                                         String type = locatie.getType().toLowerCase();
                                         if (favoritePlaces.containsKey(type)) {
                                             favoritePlaces.get(type).remove(locatie.getIdLocation());
                                             if (favoritePlaces.get(type).isEmpty()) {
                                                 favoritePlaces.remove(type);
                                             }
                                         }
                                     }
                                 });

                                 bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                             }
                             return true;
                         });
                     }
                     if (!saloaneList.isEmpty()) {
                         LatLng firstLocation = new LatLng(saloaneList.get(0).getLatitude(), saloaneList.get(0).getLongitude());
                         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                     }
                 } else {
                     Log.e(TAG, "Eroare răspuns server: " + response.code());
                     Toast.makeText(MapsActivity.this, "Eroare la obținerea saloanelor", Toast.LENGTH_SHORT).show();
                 }
             }

             @Override
             public void onFailure(Call<List<Salon>> call, Throwable t) {
                 Log.e(TAG, "Eroare la conectarea la server: ", t);
                 Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
             }
         });
     }

    public void loadMagazine(){
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Magazin>> call = apiService.getMagazine();
        call.enqueue(new Callback<List<Magazin>>() {
            @Override
            public void onResponse(Call<List<Magazin>> call, Response<List<Magazin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Raspunsul serverului: " + response.body());
                    List<Magazin> magazinList = response.body();
                    mMap.clear();
                    for (Magazin magazin : magazinList) {
                        LatLng magazinLocation = new LatLng(magazin.getLatitudine(), magazin.getLongitudine());
                        Log.d("DEBUG", "Magazin: " + magazin.getNume() + " Lat: " + magazin.getLatitudine() + " Long: " + magazin.getLongitudine());

                        var markerCustom = new MarkerOptions().position(magazinLocation);
                        markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.magazin)));

                        var marker = mMap.addMarker(markerCustom.title(magazin.getNume()));
                        if (marker != null) {
                            String programText = magazin.getNon_stop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            boolean isFavorite = favoritePlaces.containsKey("magazin") &&
                                    favoritePlaces.get("magazin").contains(magazin.getId());

                            marker.setTag(new CustomInfoWindowData(
                                    magazin.getNume(),
                                    "Număr de telefon neafișat.",
                                    programText,
                                    R.drawable.magazin,
                                    isFavorite,
                                    "magazin",
                                    magazin.getId()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);
                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });
                    }
                    if (!magazinList.isEmpty()) {
                        LatLng firstLocation = new LatLng(magazinList.get(0).getLatitudine(), magazinList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }

                } else {
                    Log.e(TAG, "Eroare raspuns server: " + response.code());
                    Toast.makeText(MapsActivity.this, "Eroare la obtinerea magazinelor veterinare", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<List<Magazin>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void loadFarmaciiVeterinare(){
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Farmacie>> call = apiService.getFarmacii();
        call.enqueue(new Callback<List<Farmacie>>() {
            @Override
            public void onResponse(Call<List<Farmacie>> call, Response<List<Farmacie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Raspunsul serverului: " + response.body());
                    List<Farmacie> farmacieList = response.body();
                    mMap.clear();
                    for (Farmacie farmacie:farmacieList) {
                        LatLng farmacieLocation = new LatLng(farmacie.getLatitudine(), farmacie.getLongitudine());

                        var markerCustom = new MarkerOptions().position(farmacieLocation);
                        markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.farmacie)));

                        var marker = mMap.addMarker(markerCustom.title(farmacie.getNume()));
                        if (marker != null) {
                            String programText = farmacie.getNon_stop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            boolean isFavorite = favoritePlaces.containsKey("farmacie") &&
                                    favoritePlaces.get("farmacie").contains(farmacie.getId());

                            marker.setTag(new CustomInfoWindowData(
                                    farmacie.getNume(),
                                    "Număr de telefon neafișat.",
                                    programText,
                                    R.drawable.farmacie,
                                    isFavorite,
                                    "farmacie",
                                    farmacie.getId()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);
                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });
                    }

                    if (!farmacieList.isEmpty()) {
                        LatLng firstLocation = new LatLng(farmacieList.get(0).getLatitudine(), farmacieList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                } else {
                    Log.e(TAG, "Eroare raspuns server: " + response.code());
                    Toast.makeText(MapsActivity.this, "Eroare la obtinerea farmaciilor veterinare", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Farmacie>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadParcuri() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Parc>> call = apiService.getParcuri();

        call.enqueue(new Callback<List<Parc>>() {
            @Override
            public void onResponse(Call<List<Parc>> call, Response<List<Parc>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Răspunsul serverului: " + response.body().toString());
                    List<Parc> parcList = response.body();
                    mMap.clear();
                    for (Parc parc : parcList) {
                        LatLng parcLocation = new LatLng(parc.getLatitudine(), parc.getLongitudine());

                        var markerCustom = new MarkerOptions().position(parcLocation);
                        markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.parc)));

                        var marker = mMap.addMarker(markerCustom.title(parc.getNume()));
                        if (marker != null) {
                            String programText = parc.getNonStop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            boolean isFavorite = favoritePlaces.containsKey("parc") &&
                                    favoritePlaces.get("parc").contains(parc.getId());

                            marker.setTag(new CustomInfoWindowData(
                                    parc.getNume(),
                                    "Număr de telefon neafișat.",
                                    programText,
                                    R.drawable.parc,
                                    isFavorite,
                                    "parc",
                                    parc.getId()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);
                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });

                    }
                    if (!parcList.isEmpty()) {
                        LatLng firstLocation = new LatLng(parcList.get(0).getLatitudine(), parcList.get(0).getLongitudine());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));
                    }
                } else {
                    Log.e(TAG, "Eroare raspuns server: " + response.code());
                    Toast.makeText(MapsActivity.this, "Eroare la obținerea parcurilor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Parc>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeFavoritePlacesMap() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<LocatieFavoritaDTO>> call = apiService.getFavLocationsForUserDTO(this.currentUserId);

        call.enqueue(new Callback<List<LocatieFavoritaDTO>>() {
            @Override
            public void onResponse(Call<List<LocatieFavoritaDTO>> call, Response<List<LocatieFavoritaDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Răspunsul serverului: " + response.body().toString());
                    List<LocatieFavoritaDTO> theList = response.body();
                    favoritePlacesList.clear();
                    favoritePlacesList.addAll(theList);

                    for (var dto : theList) {
                        String type = dto.getType().toLowerCase();
                        Integer id = dto.getIdLocation();

                        favoritePlaces
                                .computeIfAbsent(type, k -> new ArrayList<>())
                                .add(id);
                    }

                    Log.d(TAG, "Favorite Places Map loaded with succes!" + favoritePlaces);
                } else {
                    Log.e(TAG, "Eroare la raspunsul serverului (favorit)" + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<LocatieFavoritaDTO>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadFavLocationsForUser() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<LocatieFavoritaDTO>> call = apiService.getFavLocationsForUserDTO(this.currentUserId);

        call.enqueue(new Callback<List<LocatieFavoritaDTO>>() {
            @Override
            public void onResponse(Call<List<LocatieFavoritaDTO>> call, Response<List<LocatieFavoritaDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Răspunsul serverului: " + response.body().toString());
                    List<LocatieFavoritaDTO> theList = response.body();
                    mMap.clear();

                    for (var place : theList) {
                        LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());

                        var markerCustom = new MarkerOptions().position(placeLocation);
                        String drawableName = place.getType().toLowerCase();
                        int drawableResId = getResources().getIdentifier(drawableName, "drawable", getPackageName());

                        if (drawableResId != 0) {
                            markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(drawableResId)));
                        } else {
                            markerCustom.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        }

                        var marker = mMap.addMarker(markerCustom);
                        if (marker != null) {
                            String programText = place.isNonStop()
                                    ? "Program non-stop"
                                    : "Disponibil în timpul programului de lucru";

                            String nrTel = place.getPhone() != null
                                    ? place.getPhone()
                                    : "Număr de telefon neafișat.";

                            marker.setTag(new CustomInfoWindowData(
                                    place.getTitle(),
                                    nrTel,
                                    programText,
                                    drawableResId,
                                    true,
                                    place.getType().toLowerCase(),
                                    place.getIdLocation()));
                        }

                        // listener dupa ce markerii sunt adaugati
                        mMap.setOnMarkerClickListener(m -> {
                            Object tag = m.getTag();
                            if (tag instanceof CustomInfoWindowData) {
                                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                                data.setLatitude(marker.getPosition().latitude);
                                data.setLongitude(marker.getPosition().longitude);
                                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                                bottomSheet.setData(data, currentUserId);

                                final Marker finalMarker = m;

                                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                                    if (added) {
                                        String type = locatie.getType().toLowerCase();
                                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                                        favoritePlaces.get(type).add(locatie.getIdLocation());
                                    } else {
                                        String type = locatie.getType().toLowerCase();
                                        if (favoritePlaces.containsKey(type)) {
                                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                                            if (favoritePlaces.get(type).isEmpty()) {
                                                favoritePlaces.remove(type);
                                            }
                                        }

                                        finalMarker.remove();
                                    }
                                });

                                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
                            }
                            return true;
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<List<LocatieFavoritaDTO>> call, Throwable t) {
                Log.e(TAG, "Eroare la conectarea la server: ", t);
                Toast.makeText(MapsActivity.this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;

                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(MapsActivity.this);
                }
            } else {
                Toast.makeText(MapsActivity.this, "Nu s-a putut obtine locatia curenta", Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stompClientManager.disconnect();
    }

    public void focusOnLocation(SearchResultDTO item) {
        LatLng position = new LatLng(item.getLatitude(), item.getLongitude());
        mMap.clear();

        var markerCustom = new MarkerOptions().position(position);
        String rawType = item.getType() != null ? item.getType().split(" ")[0].toLowerCase() : "";
        int drawableResId = getResources().getIdentifier(rawType, "drawable", getPackageName());

        if (drawableResId != 0) {
            markerCustom.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(drawableResId)));
        } else {
            markerCustom.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }

        var marker = mMap.addMarker(markerCustom);
        if (marker != null) {
            String programText = item.isNonStop()
                    ? "Program non-stop"
                    : "Disponibil în timpul programului de lucru";

            String nrTel = !Objects.equals(item.getPhone(), "")
                    ? item.getPhone()
                    : "Număr de telefon neafișat.";

            boolean isFavorite = favoritePlaces.containsKey(rawType) &&
                    favoritePlaces.get(rawType).contains(item.getIdLocation());

            marker.setTag(new CustomInfoWindowData(
                    item.getTitle(),
                    nrTel,
                    programText,
                    drawableResId,
                    isFavorite,
                    rawType,
                    item.getIdLocation()));
        }

        // listener dupa ce markerii sunt adaugati
        mMap.setOnMarkerClickListener(m -> {
            Object tag = m.getTag();
            if (tag instanceof CustomInfoWindowData) {
                CustomInfoWindowData data = (CustomInfoWindowData) tag;
                data.setLatitude(marker.getPosition().latitude);
                data.setLongitude(marker.getPosition().longitude);
                PlaceBottomSheet bottomSheet = new PlaceBottomSheet();
                bottomSheet.setData(data, currentUserId);

                final Marker finalMarker = m;

                bottomSheet.setFavoriteChangedListener((locatie, added) -> {
                    if (added) {
                        String type = locatie.getType().toLowerCase();
                        favoritePlaces.putIfAbsent(type, new ArrayList<>());
                        favoritePlaces.get(type).add(locatie.getIdLocation());
                    } else {
                        String type = locatie.getType().toLowerCase();
                        if (favoritePlaces.containsKey(type)) {
                            favoritePlaces.get(type).remove(locatie.getIdLocation());
                            if (favoritePlaces.get(type).isEmpty()) {
                                favoritePlaces.remove(type);
                            }
                        }
                    }
                });

                bottomSheet.show(getSupportFragmentManager(), "placeBottomSheet");
            }
            return true;
        });

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
    }

}
