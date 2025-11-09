package petexplorer.petexplorerclients;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import domain.PetSittingOffer;
import petexplorer.petexplorerclients.adapters.PetSittingOfferAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import service.ApiService;

public class PetSittingActivity extends AppCompatActivity implements PetSittingOfferAdapter.OnOfferClickListener {

	private RecyclerView recyclerView;
	private PetSittingOfferAdapter adapter;
	private EditText locationFilterEdit;
	private EditText availabilityFilterEdit;
	private Button filterButton;
	private Button addButton;
	private ProgressBar progressBar;
	private ApiService api;
	private CheckBox myPostsCheck;

	// Inline form views
	private LinearLayout formContainer;
	private EditText nameEdit, descEdit, locationEdit, phoneEdit, expEdit, availEdit;
	private Button saveBtn, cancelBtn;
	private int currentUserId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pet_sitting);

		api = RetrofitClient.getApiService();
		SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
		currentUserId = prefs.getInt("user_id", -1);

		ImageButton backBtn = findViewById(R.id.btnBackToMap);
		backBtn.setOnClickListener(v -> finish());

		recyclerView = findViewById(R.id.recyclerPetSittingOffers);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new PetSittingOfferAdapter(new ArrayList<>(), this);
		recyclerView.setAdapter(adapter);

		locationFilterEdit = findViewById(R.id.editLocation);
		availabilityFilterEdit = findViewById(R.id.editAvailability);
		filterButton = findViewById(R.id.btnApplyFilters);
		addButton = findViewById(R.id.btnAddOffer);
		progressBar = findViewById(R.id.progressBar);
		myPostsCheck = findViewById(R.id.chkMyPosts);

		formContainer = findViewById(R.id.formContainer);
		nameEdit = findViewById(R.id.editName);
		descEdit = findViewById(R.id.editDescription);
		locationEdit = findViewById(R.id.editFormLocation);
		phoneEdit = findViewById(R.id.editPhone);
		expEdit = findViewById(R.id.editExperience);
		availEdit = findViewById(R.id.editFormAvailability);
		saveBtn = findViewById(R.id.btnSaveOffer);
		cancelBtn = findViewById(R.id.btnCancelOffer);

		filterButton.setOnClickListener(v -> loadOffers());
		myPostsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> loadOffers());
		addButton.setOnClickListener(v -> toggleForm(true));
		cancelBtn.setOnClickListener(v -> toggleForm(false));
		saveBtn.setOnClickListener(v -> saveOffer());

		toggleForm(false);
		loadOffers();
	}

	private void toggleForm(boolean show) {
		formContainer.setVisibility(show ? View.VISIBLE : View.GONE);
		recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
	}

	private void loadOffers() {
		showLoading(true);
		String location = locationFilterEdit.getText() != null ? locationFilterEdit.getText().toString() : "";
		String availability = availabilityFilterEdit.getText() != null ? availabilityFilterEdit.getText().toString() : "";
		if (TextUtils.isEmpty(location)) location = null;
		if (TextUtils.isEmpty(availability)) availability = null;

		Integer userIdFilter = myPostsCheck.isChecked() ? currentUserId : null;
		api.getPetSittingOffers(location, availability, userIdFilter).enqueue(new Callback<List<PetSittingOffer>>() {
			@Override
			public void onResponse(Call<List<PetSittingOffer>> call, Response<List<PetSittingOffer>> response) {
				showLoading(false);
				if (response.isSuccessful() && response.body() != null) {
					List<PetSittingOffer> items = response.body();
					adapter.setItems(items);
				} else {
					Toast.makeText(PetSittingActivity.this, "Eroare la încărcarea ofertelor", Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onFailure(Call<List<PetSittingOffer>> call, Throwable t) {
				showLoading(false);
				Toast.makeText(PetSittingActivity.this, "Eroare rețea", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void showLoading(boolean show) {
		progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
		if (formContainer.getVisibility() != View.VISIBLE) {
			recyclerView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
		}
	}

	private void saveOffer() {
		String name = nameEdit.getText().toString();
		String location = locationEdit.getText().toString();
		String phone = phoneEdit.getText().toString();

		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(phone)) {
			Toast.makeText(this, "Completează nume, locație și telefon", Toast.LENGTH_SHORT).show();
			return;
		}

		PetSittingOffer offer = new PetSittingOffer();
		offer.setUserId(currentUserId);
		offer.setName(name);
		offer.setLocation(location);
		offer.setPhoneNumber(phone);
		offer.setDescription(descEdit.getText().toString());
		offer.setExperience(expEdit.getText().toString());
		offer.setAvailability(availEdit.getText().toString());

		api.createPetSittingOffer(offer).enqueue(new Callback<PetSittingOffer>() {
			@Override
			public void onResponse(Call<PetSittingOffer> call, Response<PetSittingOffer> response) {
				if (response.isSuccessful()) {
					Toast.makeText(PetSittingActivity.this, "Ofertă salvată", Toast.LENGTH_SHORT).show();
					clearForm();
					toggleForm(false);
					loadOffers();
				} else {
					Toast.makeText(PetSittingActivity.this, "Eroare la salvare", Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onFailure(Call<PetSittingOffer> call, Throwable t) {
				Toast.makeText(PetSittingActivity.this, "Eroare rețea", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void clearForm() {
		nameEdit.setText("");
		descEdit.setText("");
		locationEdit.setText("");
		phoneEdit.setText("");
		expEdit.setText("");
		availEdit.setText("");
	}

	@Override
	public void onOfferClick(PetSittingOffer offer) {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pet_sitting_detail, null);
		((android.widget.TextView) dialogView.findViewById(R.id.txtTitle)).setText(offer.getName());
		((android.widget.TextView) dialogView.findViewById(R.id.txtLocation)).setText("Locație: " + (offer.getLocation() != null ? offer.getLocation() : ""));
		((android.widget.TextView) dialogView.findViewById(R.id.txtAvailability)).setText("Disponibilitate: " + (offer.getAvailability() != null ? offer.getAvailability() : ""));
		((android.widget.TextView) dialogView.findViewById(R.id.txtExperience)).setText("Experiență: " + (offer.getExperience() != null ? offer.getExperience() : ""));
		((android.widget.TextView) dialogView.findViewById(R.id.txtDescription)).setText("Descriere: " + (offer.getDescription() != null ? offer.getDescription() : ""));
		((android.widget.TextView) dialogView.findViewById(R.id.txtPhone)).setText("Telefon: " + (offer.getPhoneNumber() != null ? offer.getPhoneNumber() : ""));

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setView(dialogView)
				.create();

		Button callBtn = dialogView.findViewById(R.id.btnCall);
		callBtn.setOnClickListener(v -> {
			if (offer.getPhoneNumber() == null || offer.getPhoneNumber().isBlank()) return;
			Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + offer.getPhoneNumber()));
			startActivity(dialIntent);
		});

		Button deleteBtn = dialogView.findViewById(R.id.btnDelete);
		if (offer.getUserId() != null && offer.getUserId() == currentUserId) {
			deleteBtn.setVisibility(View.VISIBLE);
			deleteBtn.setOnClickListener(v -> {
				api.deletePetSittingOffer(offer.getId(), currentUserId).enqueue(new Callback<Void>() {
					@Override
					public void onResponse(Call<Void> call, Response<Void> response) {
						if (response.isSuccessful()) {
							Toast.makeText(PetSittingActivity.this, "Ofertă ștearsă", Toast.LENGTH_SHORT).show();
							dialog.dismiss();
							loadOffers();
						} else {
							Toast.makeText(PetSittingActivity.this, "Eroare la ștergere", Toast.LENGTH_SHORT).show();
						}
					}
					@Override
					public void onFailure(Call<Void> call, Throwable t) {
						Toast.makeText(PetSittingActivity.this, "Eroare rețea", Toast.LENGTH_SHORT).show();
					}
				});
			});
		} else {
			deleteBtn.setVisibility(View.GONE);
		}

		ImageButton closeBtn = dialogView.findViewById(R.id.btnCloseDialog);
		closeBtn.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}

	@Override
	public void onCallClick(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.isBlank()) return;
		Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
		startActivity(dialIntent);
	}
}


