package petexplorer.petexplorerclients.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GlobalErrorBus {
    private static MutableLiveData<String> errorSignal = new MutableLiveData<>();

    public static LiveData<String> getErrorSignal() {
        return errorSignal;
    }

    public static void publish(String message) {
        errorSignal.postValue(message);
    }

    public static void postError(String s) {
        errorSignal.postValue(s);

    }
}