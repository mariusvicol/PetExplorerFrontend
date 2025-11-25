package petexplorer.petexplorerclients.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import domain.utils.RatingResponseDTO;
import petexplorer.petexplorerclients.R;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.RatingViewHolder> {

    private final List<RatingResponseDTO> ratings;

    public RatingAdapter() {
        this.ratings = new ArrayList<>();
    }

    public void updateData(List<RatingResponseDTO> newRatings) {
        this.ratings.clear();
        if (newRatings != null) {
            this.ratings.addAll(newRatings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new RatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
        RatingResponseDTO rating = ratings.get(position);
        holder.reviewerNameTextView.setText(rating.getUserName() != null ? rating.getUserName() : holder.itemView.getContext().getString(R.string.rating_unknown_user));
        holder.reviewTimestampTextView.setText(formatTimestamp(rating.getTimestamp()));
        holder.reviewRatingBar.setRating(rating.getRatingValue() != null ? rating.getRatingValue() : 0);

        String reviewText = rating.getReviewText();
        holder.reviewTextView.setText(
                (reviewText != null && !reviewText.trim().isEmpty())
                        ? reviewText
                        : holder.itemView.getContext().getString(R.string.rating_no_comment)
        );
    }

    @Override
    public int getItemCount() {
        return ratings.size();
    }

    static class RatingViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerNameTextView;
        TextView reviewTimestampTextView;
        RatingBar reviewRatingBar;
        TextView reviewTextView;

        RatingViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerNameTextView = itemView.findViewById(R.id.reviewerNameTextView);
            reviewTimestampTextView = itemView.findViewById(R.id.reviewTimestampTextView);
            reviewRatingBar = itemView.findViewById(R.id.reviewRatingBar);
            reviewTextView = itemView.findViewById(R.id.reviewTextView);
        }
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        String sanitized = timestamp.replace('T', ' ').replace('Z', ' ').trim();
        if (sanitized.length() > 16) {
            sanitized = sanitized.substring(0, 16);
        }
        return sanitized.toLowerCase(Locale.getDefault());
    }
}

