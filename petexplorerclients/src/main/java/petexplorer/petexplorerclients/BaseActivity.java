package petexplorer.petexplorerclients;



import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import petexplorer.petexplorerclients.utils.GlobalErrorBus;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GlobalErrorBus.getErrorSignal().observe(this, this::showGlobalErrorDialog);
    }

    private void showGlobalErrorDialog(String message) {
        if (message == null || message.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Eroare Sistem")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}