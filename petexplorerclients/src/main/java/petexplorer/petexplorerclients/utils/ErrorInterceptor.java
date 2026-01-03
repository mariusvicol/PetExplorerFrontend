package petexplorer.petexplorerclients.utils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class ErrorInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            Response response = chain.proceed(chain.request());

            if (!response.isSuccessful()) {
                String errorMessage;
                switch (response.code()) {
                    case 401:
                        errorMessage = "Session expired. Please log in again.";
                        break;
                    case 404:
                        errorMessage = "Resource not found.";
                        break;
                    case 500:
                        errorMessage = "Server error. Please try later.";
                        break;
                    default:
                        errorMessage = "An unexpected error occurred.";
                        break;
                }

                GlobalErrorBus.publish("Eroare Server: "+errorMessage);
            }
            return response;
        } catch (IOException e) {
            GlobalErrorBus.postError("Problemă de conexiune. Verifică internetul.");
            throw e;
        }
    }

}