package petexplorer.petexplorerclients;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import domain.utils.CustomInfoWindowData;
import domain.utils.LocationRatingsDTO;
import domain.utils.LocatieFavoritaDTO;
import domain.utils.RatingRequestDTO;
import domain.utils.RatingResponseDTO;
import petexplorer.petexplorerclients.adapters.RatingAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

import java.util.List;
import java.util.Locale;

public class PlaceBottomSheet extends BottomSheetDialogFragment {

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged(LocatieFavoritaDTO place, boolean added);
    }

    protected CustomInfoWindowData data;
    private Integer userId;
    private OnFavoriteChangedListener favoriteChangedListener;
    private RatingAdapter ratingAdapter;
    private RatingBar averageRatingBar;
    private TextView averageRatingTextView;
    private TextView reviewCountTextView;
    private RatingBar userRatingBar;
    private EditText reviewEditText;
    private ProgressBar ratingsProgressBar;
    private TextView emptyReviewsTextView;
    private Button submitReviewButton;

    public PlaceBottomSheet() { }

    public void setData(CustomInfoWindowData data, Integer userId) {
        this.data = data;
        this.userId = userId;
    }

    public void setFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favoriteChangedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.place_bottom_sheet, container, false);

        // setarea textview-urilor cu valorile de care am nevoie
        if (data != null) {
            TextView nameTextView = rootView.findViewById(R.id.nameTextView);
            TextView phoneTextView = rootView.findViewById(R.id.nrTelTextView);
            TextView programTextView = rootView.findViewById(R.id.programTextView);
            ImageView iconImageView = rootView.findViewById(R.id.iconImageView);
            CheckBox checkBox = rootView.findViewById(R.id.checkBox);

            nameTextView.setText(data.getTitle());
            phoneTextView.setText(data.getNrTel());
            programTextView.setText(data.getProgram());
            iconImageView.setImageResource(data.getImage());
            checkBox.setChecked(data.isChecked());

            checkBox.setOnCheckedChangeListener((btnView, isChecked) -> {
                ApiService apiService = RetrofitClient.getApiService();

                LocatieFavoritaDTO place = new LocatieFavoritaDTO();
                place.setIdUser(userId);
                place.setType(data.getLocationType());
                place.setIdLocation(data.getEntityId());

                if (isChecked) {
                    Log.d("Tag", "Is about to add: " + place);
                    Call<Void> addCall = apiService.addFavoritePlace(place);
                    addCall.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Locație adăugată la favorite cu succes!", Toast.LENGTH_SHORT).show();
                                data.setChecked(true);

                                if (favoriteChangedListener != null) {
                                    favoriteChangedListener.onFavoriteChanged(place, true);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), "Eroare la salvare", Toast.LENGTH_SHORT).show();
                            checkBox.setChecked(false);
                        }
                    });
                } else {
                    Log.d("Tag", "Is about to be deleted: " + place);
                    Call<Void> deleteCall = apiService.deleteFavoritePlace(place.getIdUser(), place.getIdLocation(), place.getType());

                    deleteCall.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Locație ștearsă de la favorite cu succes!", Toast.LENGTH_SHORT).show();
                                data.setChecked(false);

                                if (favoriteChangedListener != null) {
                                    favoriteChangedListener.onFavoriteChanged(place, false);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), "Eroare la ștergere: ", Toast.LENGTH_SHORT).show();
                            checkBox.setChecked(false);
                        }
                    });
                }
            });
        }

        setupRatingSection(rootView);
        loadRatings();

        Button closeButton = rootView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        Button openInMapsButton = rootView.findViewById(R.id.openInMapsButton);
        openInMapsButton.setOnClickListener(v -> {
            if (data != null) {
                double lat = data.getLatitude();
                double lng = data.getLongitude();

                String uri = "google.navigation:q=" + lat + "," + lng;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");

                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Google Maps nu este instalat.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return rootView;
    }

    private void setupRatingSection(View rootView) {
        averageRatingBar = rootView.findViewById(R.id.averageRatingBar);
        averageRatingTextView = rootView.findViewById(R.id.averageRatingTextView);
        reviewCountTextView = rootView.findViewById(R.id.reviewCountTextView);
        userRatingBar = rootView.findViewById(R.id.userRatingBar);
        reviewEditText = rootView.findViewById(R.id.reviewEditText);
        ratingsProgressBar = rootView.findViewById(R.id.ratingsProgressBar);
        emptyReviewsTextView = rootView.findViewById(R.id.emptyReviewsTextView);
        submitReviewButton = rootView.findViewById(R.id.submitReviewButton);
        RecyclerView reviewsRecyclerView = rootView.findViewById(R.id.reviewsRecyclerView);

        ratingAdapter = new RatingAdapter();
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsRecyclerView.setAdapter(ratingAdapter);

        submitReviewButton.setOnClickListener(v -> submitRating());
    }

    private void loadRatings() {
        if (data == null) {
            return;
        }

        toggleRatingsLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        Call<LocationRatingsDTO> call = apiService.getRatingsForLocation(data.getEntityId(), data.getLocationType());
        call.enqueue(new Callback<LocationRatingsDTO>() {
            @Override
            public void onResponse(Call<LocationRatingsDTO> call, Response<LocationRatingsDTO> response) {
                toggleRatingsLoading(false);
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    applyRatings(response.body());
                } else {
                    Toast.makeText(getContext(), getString(R.string.rating_load_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LocationRatingsDTO> call, Throwable t) {
                toggleRatingsLoading(false);
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), getString(R.string.rating_load_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyRatings(LocationRatingsDTO dto) {
        updateSummary(dto.getAverageRating(), dto.getReviewCount());

        List<RatingResponseDTO> ratings = dto.getRatings();
        ratingAdapter.updateData(ratings);

        boolean hasReviews = ratings != null && !ratings.isEmpty();
        emptyReviewsTextView.setVisibility(hasReviews ? View.GONE : View.VISIBLE);

        prefillUserReview(ratings);
    }

    private void prefillUserReview(List<RatingResponseDTO> ratings) {
        if (ratings == null || userId == null) {
            userRatingBar.setRating(0);
            reviewEditText.setText("");
            return;
        }

        for (RatingResponseDTO rating : ratings) {
            if (rating.getUserId() != null && rating.getUserId().equals(userId)) {
                if (rating.getRatingValue() != null) {
                    userRatingBar.setRating(rating.getRatingValue());
                } else {
                    userRatingBar.setRating(0);
                }
                reviewEditText.setText(rating.getReviewText() != null ? rating.getReviewText() : "");
                return;
            }
        }

        userRatingBar.setRating(0);
        reviewEditText.setText("");
    }

    private void updateSummary(Double averageRating, Integer reviewCount) {
        if (averageRating != null) {
            averageRatingBar.setRating(averageRating.floatValue());
            averageRatingTextView.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
        } else {
            averageRatingBar.setRating(0);
            averageRatingTextView.setText(getString(R.string.rating_average_placeholder));
        }

        int count = reviewCount != null ? reviewCount : 0;
        reviewCountTextView.setText(getString(R.string.rating_count_format, count));
    }

    private void submitRating() {
        if (data == null || userRatingBar == null) {
            return;
        }

        if (userId == null || userId < 0) {
            Toast.makeText(getContext(), getString(R.string.rating_submit_error), Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRating = Math.round(userRatingBar.getRating());
        if (selectedRating < 1) {
            Toast.makeText(getContext(), getString(R.string.rating_error_select_value), Toast.LENGTH_SHORT).show();
            return;
        }

        String reviewText = reviewEditText != null ? reviewEditText.getText().toString().trim() : "";

        RatingRequestDTO requestDTO = new RatingRequestDTO();
        requestDTO.setUserId(userId);
        requestDTO.setLocationId(data.getEntityId());
        requestDTO.setLocationType(data.getLocationType());
        requestDTO.setRatingValue(selectedRating);
        requestDTO.setReviewText(reviewText.isEmpty() ? null : reviewText);

        submitReviewButton.setEnabled(false);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.addOrUpdateRating(requestDTO).enqueue(new Callback<RatingResponseDTO>() {
            @Override
            public void onResponse(Call<RatingResponseDTO> call, Response<RatingResponseDTO> response) {
                submitReviewButton.setEnabled(true);
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), getString(R.string.rating_submit_success), Toast.LENGTH_SHORT).show();
                    loadRatings();
                } else {
                    Toast.makeText(getContext(), getString(R.string.rating_submit_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RatingResponseDTO> call, Throwable t) {
                submitReviewButton.setEnabled(true);
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), getString(R.string.rating_submit_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleRatingsLoading(boolean show) {
        if (ratingsProgressBar != null) {
            ratingsProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
