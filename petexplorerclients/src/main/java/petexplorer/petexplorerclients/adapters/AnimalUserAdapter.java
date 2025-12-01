package petexplorer.petexplorerclients.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;

import domain.AnimalPierdut;
import petexplorer.petexplorerclients.R;
import petexplorer.petexplorerclients.utils.ServerConfig;

public class AnimalUserAdapter extends RecyclerView.Adapter<AnimalUserAdapter.AnimalUserViewHolder> {

    public interface OnResolveClickListener {
        void onResolveClick(AnimalPierdut animal);
    }

    private List<AnimalPierdut> animale;
    private final OnResolveClickListener resolveClickListener;

    public AnimalUserAdapter(List<AnimalPierdut> animale, OnResolveClickListener resolveClickListener) {
        this.animale = animale;
        this.resolveClickListener = resolveClickListener;
    }

    @NonNull
    @Override
    public AnimalUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal_user, parent, false);
        return new AnimalUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalUserViewHolder holder, int position) {
        holder.bind(animale.get(position), resolveClickListener);
    }

    @Override
    public int getItemCount() {
        return animale.size();
    }

    public void updateData(List<AnimalPierdut> newList) {
        this.animale = newList;
        notifyDataSetChanged();
    }

    public static class AnimalUserViewHolder extends RecyclerView.ViewHolder {
        TextView tvNume, tvData, tvDescriere, tvTelefon, tvRezolvat;
        ImageView imgPoza;
        Button btnRezolvat;
        ImageButton btnExpandDescriere;
        boolean isExpanded = false;

        public AnimalUserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNume = itemView.findViewById(R.id.tvNume);
            tvData = itemView.findViewById(R.id.tvData);
            tvDescriere = itemView.findViewById(R.id.tvDescriere);
            tvTelefon = itemView.findViewById(R.id.tvTelefon);
            imgPoza = itemView.findViewById(R.id.imgPoza);
            btnRezolvat = itemView.findViewById(R.id.btnMarkResolved);
            tvRezolvat = itemView.findViewById(R.id.tvRezolvat);
            btnExpandDescriere = itemView.findViewById(R.id.btnExpandDescriere);
        }

        public void bind(AnimalPierdut animal, OnResolveClickListener resolveClickListener) {
            tvNume.setText(animal.getNumeAnimal());
            String dataRaw = animal.getDataCaz();
            String dataFormatted;
            if (dataRaw != null && !dataRaw.isEmpty()) {
                try {
                    LocalDateTime data = LocalDateTime.parse(dataRaw);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("ro"));
                    dataFormatted = data.format(formatter);
                } catch (Exception e) {
                    dataFormatted = "data invalida";
                }
            } else {
                dataFormatted = "fără dată";
            }

            tvData.setText(dataFormatted);
            tvDescriere.setText(animal.getDescriere());
            tvTelefon.setText(animal.getNrTelefon());

            // Reset expand state
            isExpanded = false;
            tvDescriere.setMaxLines(2);
            btnExpandDescriere.setRotation(0);

            // Handle expand/collapse button
            btnExpandDescriere.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                if (isExpanded) {
                    tvDescriere.setMaxLines(Integer.MAX_VALUE);
                    btnExpandDescriere.setRotation(180);
                } else {
                    tvDescriere.setMaxLines(2);
                    btnExpandDescriere.setRotation(0);
                }
            });

            if (animal.getPoza() != null && !animal.getPoza().isEmpty()) {
                String imageUrl = ServerConfig.BASE_URL + animal.getPoza();
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.dog2) // imagine default
                        .error(R.drawable.warning)
                        .fallback(R.drawable.dog2)
                        .into(imgPoza);
            } else {
                imgPoza.setImageResource(R.drawable.dog2);
            }


            if (animal.getRezolvat()) {
                itemView.setAlpha(0.7f);
                btnRezolvat.setVisibility(View.GONE);
                tvRezolvat.setVisibility(View.VISIBLE);
            } else {
                itemView.setAlpha(1f);
                btnRezolvat.setVisibility(View.VISIBLE);
                tvRezolvat.setVisibility(View.GONE);
                btnRezolvat.setOnClickListener(v -> resolveClickListener.onResolveClick(animal));
            }

        }

    }
}
