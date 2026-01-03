package petexplorer.petexplorerclients;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.widget.SearchView;

import android.widget.ImageButton;
import android.widget.LinearLayout;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import domain.utils.SearchResultDTO;
import petexplorer.petexplorerclients.adapters.SearchAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class FiltrareBottomSheetFragment extends BottomSheetDialogFragment {

    protected RecyclerView resultsRV;
    private View rootView;
    private LinearLayout noResultsLayout;

    private Handler debounceHandler = new Handler();
    private Runnable debounceRunnable;
    private final long DEBOUNCE_DELAY = 300;

    public FiltrareBottomSheetFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflatează layout-ul pentru BottomSheet
        rootView = inflater.inflate(R.layout.filtrare_bottom_sheet_fragment, container, false);

        // Butoane pentru filtrare
        ImageButton filterCabineteButton = rootView.findViewById(R.id.filterCabineteButton);
        ImageButton filterFarmaciiButton = rootView.findViewById(R.id.filterFarmaciiButton);
        ImageButton filterSaloaneButton = rootView.findViewById(R.id.filterSaloaneButton);
        ImageButton filterPensiuniButton = rootView.findViewById(R.id.filterPensiuniButton);
        ImageButton filterMagazineButton = rootView.findViewById(R.id.filterMagazineButton);
        ImageButton filterParcuriButton = rootView.findViewById(R.id.filterParcuriButton);

        LinearLayout buttonsLayout = rootView.findViewById(R.id.buttonsLayout);
        ConstraintLayout resultsLayout = rootView.findViewById(R.id.resultsLayout);
        SearchView searchView = rootView.findViewById(R.id.searchView);

        noResultsLayout = rootView.findViewById(R.id.noResultsLayout);
        resultsRV = rootView.findViewById(R.id.resultsRecyclerView);
        resultsRV.setLayoutManager(new LinearLayoutManager(getContext()));

        SearchAdapter adapter = new SearchAdapter(new ArrayList<>(), item -> {

            MapsActivity mapsActivity = (MapsActivity) getActivity();
            if (mapsActivity != null) {
                mapsActivity.focusOnLocation(item); // trimite pinul selectat
                dismiss(); // inchidere fragment
            }
        });


        resultsRV.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // stergere cerere
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                // noua cerere cu delay sa nu se suprapuna sesiunile
                debounceRunnable = () -> {
                    if (newText.trim().length() >= 3) {
                        buttonsLayout.setVisibility(View.GONE);
                        resultsLayout.setVisibility(View.VISIBLE);
                        handleSearchAction(newText);
                    } else {
                        noResultsLayout.setVisibility(View.GONE);
                        resultsLayout.setVisibility(View.GONE);
                        buttonsLayout.setVisibility(View.VISIBLE);
                    }
                };

                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
                return true;
            }
        });


        filterMagazineButton.setOnClickListener(v -> {
            MapsActivity mapsActivity = (MapsActivity) getActivity();
            if (mapsActivity != null) {
                mapsActivity.loadMagazine();
                dismiss();
            }
        });


        filterFarmaciiButton.setOnClickListener(v -> {

             MapsActivity mapsActivity = (MapsActivity) getActivity();
            if (mapsActivity != null) {
                mapsActivity.loadFarmaciiVeterinare();
                dismiss();
            }
        });


        filterCabineteButton.setOnClickListener(v -> {

            MapsActivity mapsActivity = (MapsActivity) getActivity();
            if (mapsActivity != null) {
                mapsActivity.loadVeterinaryOffices();
                dismiss();
            }
        });

        filterParcuriButton.setOnClickListener(v -> {

            MapsActivity mapsActivity = (MapsActivity) getActivity();
            if (mapsActivity != null) {
                mapsActivity.loadParcuri();
                dismiss();
            }
        });

        filterPensiuniButton.setOnClickListener(v -> {

            MapsActivity mapsActivity = (MapsActivity) getActivity();

            if (mapsActivity != null) {
                mapsActivity.loadPensiuni();
                dismiss();
            }
        });

        filterSaloaneButton.setOnClickListener(v -> {

            MapsActivity mapsActivity = (MapsActivity) getActivity();

            if (mapsActivity != null) {
                mapsActivity.loadSaloane();
                dismiss();
            }
        });


        Button closeButton = rootView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        //setTouchListenerForButton(filterCabineteButton);
        //setTouchListenerForButton(filterFarmaciiButton);
        //setTouchListenerForButton(filterSaloaneButton);
        //setTouchListenerForButton(filterPensiuniButton);

        return rootView;
    }


    private void handleSearchAction(String text) {
        ApiService apiService = RetrofitClient.getApiService();
        noResultsLayout = rootView.findViewById(R.id.noResultsLayout);

        apiService.getSearchResults(text).enqueue(new Callback<List<SearchResultDTO>>() {
            @Override
            public void onResponse(Call<List<SearchResultDTO>> call, Response<List<SearchResultDTO>> response) {
                if (response.isSuccessful() && response.body() != null){
                    List<SearchResultDTO> all = response.body();

                    // actualizare lista in adapter
                    if (resultsRV.getAdapter() instanceof SearchAdapter) {
                        SearchAdapter adapter = (SearchAdapter) resultsRV.getAdapter();
                        adapter.submitList(all);
                    }

                    if (all.isEmpty()) {
                        resultsRV.setVisibility(View.GONE);
                        noResultsLayout.setVisibility(View.VISIBLE);
                    } else {
                        resultsRV.setVisibility(View.VISIBLE);
                        noResultsLayout.setVisibility(View.GONE);
                    }

                    Log.d("DEBUG", "Rezultate primite: " + all.size());
                } else {
                    Log.e("ERROR", "Eroare la server: " + response.code());
                    // Toast.makeText(getContext(), "Eroare la cautare", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SearchResultDTO>> call, Throwable t) {
                Log.e("ERROR", "Eroare la conectarea cu serverul: " + t);
            }
        });
    }


    // Funcție pentru a permite mișcarea butoanelor
    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListenerForButton(Button button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Nu se face nimic special când apăsăm pe buton
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Mișcăm butonul conform mișcării degetului
                        float x = event.getRawX();
                        float y = event.getRawY();

                        // Calculăm poziția butonului
                        // Setează noua poziție a butonului, doar pe orizontală
                        float minX = 0;
                        float maxX = getResources().getDisplayMetrics().widthPixels - v.getWidth();
                        float minY = 0;
                        float maxY = getResources().getDisplayMetrics().heightPixels - v.getHeight();

                        // Permitem mișcarea doar pe orizontală (adică păstrăm y constant)
                        v.setX(Math.max(minX, Math.min(x - v.getWidth() / 2, maxX)));
                        v.setY(Math.max(minY, Math.min(y - v.getHeight() / 2, maxY)));

                        return true;

                    case MotionEvent.ACTION_UP:
                        // La eliberarea butonului, mișcarea se oprește
                        return true;

                    default:
                        return false;
                }
            }
        });
    }
}
