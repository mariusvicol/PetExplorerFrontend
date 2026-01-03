package petexplorer.petexplorerclients;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.SupportMapFragment;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import domain.AnimalPierdut;
import petexplorer.petexplorerclients.adapters.AnimalAdapter;
import petexplorer.petexplorerclients.adapters.AnimalUserAdapter;
import petexplorer.petexplorerclients.notification.WebSocketStompClientManager;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class MyAnnouncementsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private Button btnVeziPierdute, btnVeziGasite;
    private AnimalUserAdapter adapter;
    private String cazCurent = "pierdut";
    private List<AnimalPierdut> allAnimals = new ArrayList<>();
    private int currentUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_announcements);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        if (currentUserId == -1) {
            finish();
            return;
        }


        TextView title = findViewById(R.id.tvTitle);
        title.setText("Anunțurile tale");

        recyclerView = findViewById(R.id.recyclerMyAnimals);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnimalUserAdapter(new ArrayList<>(), this::markAnimalAsResolved);
        recyclerView.setAdapter(adapter);


        btnVeziPierdute = findViewById(R.id.btnUserPierdute);
        btnVeziGasite = findViewById(R.id.btnUserGasite);

        btnVeziPierdute.setOnClickListener(v -> {
            cazCurent = "pierdut";
            updateFilteredList();

            btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
            btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));
        });

        btnVeziGasite.setOnClickListener(v -> {
            cazCurent = "vazut";
            updateFilteredList();

            btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
            btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));
        });


        btnVeziPierdute.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_button));
        btnVeziGasite.setBackground(ContextCompat.getDrawable(this, R.drawable.not_focused_button));

        loadUserAnimals();

        ImageButton btnBackToMap = findViewById(R.id.btnBackToMap);
        btnBackToMap.setOnClickListener(v -> {
            Intent intent = new Intent(MyAnnouncementsActivity.this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void updateFilteredList() {
        List<AnimalPierdut> filtered = allAnimals.stream()
                .filter(a -> a.getTipCaz().equalsIgnoreCase(cazCurent))
                .sorted((a, b) -> {
                    LocalDateTime dataA = LocalDateTime.parse(a.getDataCaz());
                    LocalDateTime dataB = LocalDateTime.parse(b.getDataCaz());

                    if (!a.getRezolvat() && b.getRezolvat()) return -1;
                    if (a.getRezolvat() && !b.getRezolvat()) return 1;

                    return dataB.compareTo(dataA);
                })
                .collect(Collectors.toList());

        adapter.updateData(filtered);
    }



    private void loadUserAnimals() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnimalPierdut>> call = apiService.getAnimalePierdute();

        call.enqueue(new Callback<List<AnimalPierdut>>() {
            @Override
            public void onResponse(Call<List<AnimalPierdut>> call, Response<List<AnimalPierdut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allAnimals = response.body().stream()
                            .filter(a -> a.getIdUser() == currentUserId)
                            .collect(Collectors.toList());
                    updateFilteredList();
                }
            }

            @Override
            public void onFailure(Call<List<AnimalPierdut>> call, Throwable t) {
                      }
        });
    }

    private void markAnimalAsResolved(AnimalPierdut animal) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.markAsResolved(animal.getId());

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    animal.setRezolvat(true);
                    updateFilteredList();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
