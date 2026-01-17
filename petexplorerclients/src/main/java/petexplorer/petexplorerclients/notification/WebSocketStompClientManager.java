package petexplorer.petexplorerclients.notification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import domain.AnimalPierdut;
import io.reactivex.disposables.Disposable;
import petexplorer.petexplorerclients.utils.ServerConfig;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class WebSocketStompClientManager {

    public interface OnAnimalReceivedListener {
        void onAnimalReceived(AnimalPierdut animal);
    }

    private static WebSocketStompClientManager instance;
    private StompClient stompClient;
    private OnAnimalReceivedListener animalReceivedListener;
    private boolean isConnected = false;
    private final Context appContext;

    // Referințe catre abonari pentru a le putea inchide
    // Este sarcina clientului sa aleaga topicul corect (global sau privat)
    private Disposable globalSubscription;
    private Disposable privateSubscription;
    private int currentUserId;

    private WebSocketStompClientManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized WebSocketStompClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new WebSocketStompClientManager(context);
        }
        return instance;
    }

    public void setOnAnimalReceivedListener(OnAnimalReceivedListener listener) {
        this.animalReceivedListener = listener;
    }

    @SuppressLint("CheckResult")
    public void connect(int userId) {
        if (isConnected) return;
        this.currentUserId = userId;

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, ServerConfig.WS_URL);

        // Header pentru identificare utilizator (Spring are nevoie de el pentru convertAndSendToUser)
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("user-id", String.valueOf(userId)));

        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.d("Stomp", "Conectat la WebSocket");
                    isConnected = true;
                    // la inceput ne abonam la global
                    subscribeToGlobal();
                    break;
                case ERROR:
                    Log.e("Stomp", "Eroare WebSocket", lifecycleEvent.getException());
                    isConnected = false;
                    break;
                case CLOSED:
                    Log.d("Stomp", "WebSocket închis");
                    isConnected = false;
                    break;
            }
        });

        stompClient.connect(headers);
    }

    /**
     * Abonare la topicul global (Primește TOT).
     */
    public void subscribeToGlobal() {
        if (globalSubscription != null && !globalSubscription.isDisposed()) return;
        unsubscribePrivate();

        globalSubscription = stompClient.topic("/topic/animale-pierdute").subscribe(topicMessage -> {
            handleIncomingMessage(topicMessage.getPayload(), "GLOBAL");
        }, throwable -> Log.e("Stomp", "Eroare abonare global", throwable));

        Log.d("Stomp", "Abonat la topicul global");
    }

    /**
     * Abonare la coada privată (Doar proximitate).
     * Se apeleaza dupa ce server-ul a primit locatia clientului
     */
    public void subscribeToPrivate() {
        if (privateSubscription != null && !privateSubscription.isDisposed()) return;

        // Dacă trecem pe privat, oprim ascultarea pe global pentru a evita duplicatele
        unsubscribeGlobal();

        privateSubscription = stompClient.topic("/user/queue/notifications").subscribe(topicMessage -> {
            handleIncomingMessage(topicMessage.getPayload(), "PRIVAT");
        }, throwable -> Log.e("Stomp", "Eroare abonare privat", throwable));

        Log.d("Stomp", "Abonat la coada privata");
    }

    private void handleIncomingMessage(String json, String source) {
        AnimalPierdut animal = new Gson().fromJson(json, AnimalPierdut.class);
        Log.d("Stomp", "Mesaj primit via " + source + ": " + animal.getNumeAnimal());

        NotificationUtil.showNotification(
                appContext,
                "Animal " + animal.getTipCaz() + ": " + animal.getNumeAnimal(),
                animal.getDescriere()
        );

        if (animalReceivedListener != null) {
            animalReceivedListener.onAnimalReceived(animal);
        }
    }

    private void unsubscribeGlobal() {
        if (globalSubscription != null) {
            globalSubscription.dispose();
            globalSubscription = null;
            Log.d("Stomp", "Dezabonat de la global");
        }
    }

    private void unsubscribePrivate() {
        if (privateSubscription != null) {
            privateSubscription.dispose();
            privateSubscription = null;
            Log.d("Stomp", "Dezabonat de la privat");
        }
    }

    public void disconnect() {
        unsubscribeGlobal();
        unsubscribePrivate();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        isConnected = false;
    }
}