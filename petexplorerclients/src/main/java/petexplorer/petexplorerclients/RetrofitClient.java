package petexplorer.petexplorerclients;

import petexplorer.petexplorerclients.utils.ServerConfig;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import service.ApiService;

public class RetrofitClient {
    // private static final String BASE_URL = "http://192.168.39.224:8080";
    private static final String BASE_URL = ServerConfig.BASE_URL;
    private static final String PYTHON_AI_URL = ServerConfig.PYTHON_AI_URL;

    private static Retrofit retrofit;
    private static Retrofit pythonAiRetrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }

    public static ApiService getPythonAiApiService() {
        return getPythonAiRetrofitInstance().create(ApiService.class);
    }

    public static Retrofit getPythonAiRetrofitInstance() {
        if (pythonAiRetrofit == null) {
            pythonAiRetrofit = new Retrofit.Builder()
                    .baseUrl(PYTHON_AI_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return pythonAiRetrofit;
    }
}
