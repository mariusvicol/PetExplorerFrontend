package petexplorer.petexplorerclients.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import domain.PetSittingOffer;
import petexplorer.petexplorerclients.R;

public class PetSittingOfferAdapter extends RecyclerView.Adapter<PetSittingOfferAdapter.ViewHolder> {

	public interface OnOfferClickListener {
		void onOfferClick(PetSittingOffer offer);
		void onCallClick(String phoneNumber);
	}

	private final List<PetSittingOffer> items;
	private final OnOfferClickListener listener;

	public PetSittingOfferAdapter(List<PetSittingOffer> items, OnOfferClickListener listener) {
		this.items = items != null ? items : new ArrayList<>();
		this.listener = listener;
	}

	public void setItems(List<PetSittingOffer> newItems) {
		this.items.clear();
		if (newItems != null) {
			this.items.addAll(newItems);
		}
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet_sitting_offer, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		PetSittingOffer offer = items.get(position);
		holder.title.setText(offer.getName());
		holder.subtitle.setText(offer.getLocation() != null ? offer.getLocation() : "");
		holder.availability.setText(offer.getAvailability() != null ? offer.getAvailability() : "");

		holder.itemView.setOnClickListener(v -> {
			if (listener != null) listener.onOfferClick(offer);
		});
		holder.callButton.setOnClickListener(v -> {
			if (listener != null) listener.onCallClick(offer.getPhoneNumber());
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		TextView subtitle;
		TextView availability;
		ImageButton callButton;

		ViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.offerTitle);
			subtitle = itemView.findViewById(R.id.offerSubtitle);
			availability = itemView.findViewById(R.id.offerAvailability);
			callButton = itemView.findViewById(R.id.btnCall);
		}
	}
}


