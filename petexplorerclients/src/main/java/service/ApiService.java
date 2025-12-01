package service;
import java.util.List;

import domain.AnimalPierdut;
import domain.CabinetVeterinar;
import domain.Farmacie;
import domain.Magazin;
import domain.Parc;
import domain.PensiuneCanina;
import domain.Salon;
import domain.User;
import domain.utils.LocationRatingsDTO;
import domain.PetSittingOffer;
import domain.utils.LocatieFavoritaDTO;
import domain.utils.SearchResultDTO;
import domain.utils.LoginResponse;
import domain.utils.Enable2FAResponse;
import domain.utils.RatingRequestDTO;
import domain.utils.RatingResponseDTO;
import domain.utils.AiDescriptionResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/cabinete")
    Call<List<CabinetVeterinar>> getCabineteVeterinare();

    @GET("api/farmacii")
    Call<List<Farmacie>> getFarmacii();

    @GET("api/magazine")
    Call<List<Magazin>> getMagazine();

    @GET("api/parcuri")
    Call<List<Parc>> getParcuri();

    @GET("api/animale_pierdute")
    Call<List<AnimalPierdut>> getAnimalePierdute();

    @GET("api/pensiuni")
    Call<List<PensiuneCanina>> getPensiuniCanine();

    @GET("api/saloane")
    Call<List<Salon>> getSaloane();

    @GET("api/search")
    Call<List<SearchResultDTO>> getSearchResults(@Query("text") String text);

    @GET("api/locatii/favoritesDTO/{userId}")
    Call<List<LocatieFavoritaDTO>> getFavLocationsForUserDTO(@Path("userId") Integer userId);

    @POST("api/locatii/add")
    Call<Void> addFavoritePlace(@Body LocatieFavoritaDTO place);

    @DELETE("api/locatii/delete")
    Call<Void> deleteFavoritePlace(
            @Query("idUser") int userId,
            @Query("idLocation") int locationId,
            @Query("type") String type
    );

    @POST("/api/users/login")
    Call<LoginResponse> login(@Body User loginRequest);

    @POST("/api/users/google-login")
    Call<LoginResponse> googleLogin(@Body java.util.Map<String, String> request);

    @POST("/api/users/enable-2fa")
    Call<Enable2FAResponse> enable2FA(@Body java.util.Map<String, Integer> request);

    @POST("/api/users/verify-2fa-setup")
    Call<java.util.Map<String, Object>> verify2FASetup(@Body java.util.Map<String, Object> request);

    @POST("/api/users/verify-2fa-login")
    Call<User> verify2FALogin(@Body java.util.Map<String, Object> request);

    @POST("/api/users/register")
    Call<User> register(@Body User registerRequest);

    @GET("/api/users/{id}")
    Call<User> getUserById(@Path("id") int id);

    @PUT("/api/users/{id}")
    Call<User> updateUser(@Path("id") int id, @Body User user);

    @Multipart
    @POST("/api/animale_pierdute")
    Call<AnimalPierdut> uploadAnimal(
            @Part MultipartBody.Part imagine,
            @Part("nume_animal") RequestBody nume,
            @Part("descriere") RequestBody descriere,
            @Part("latitudine") RequestBody lat,
            @Part("longitudine") RequestBody lng,
            @Part("tip_caz") RequestBody tipCaz,
            @Part("nr_telefon") RequestBody telefon,
            @Part("id_user") RequestBody userId,
            @Part("rezolvat") RequestBody rezolvat
    );

    @PATCH("api/animale_pierdute/{id}/rezolvat")
    Call<Void> markAsResolved(@Path("id") int id);

    @POST("/api/ratings")
    Call<RatingResponseDTO> addOrUpdateRating(@Body RatingRequestDTO ratingRequestDTO);

    @GET("/api/locations/{locationId}/ratings")
    Call<LocationRatingsDTO> getRatingsForLocation(@Path("locationId") int locationId,
                                                   @Query("type") String locationType);

    // Pet Sitting Offers
    @POST("api/petsitting")
    Call<PetSittingOffer> createPetSittingOffer(@Body PetSittingOffer offer);

    @GET("api/petsitting")
    Call<List<PetSittingOffer>> getPetSittingOffers(@Query("location") String location, @Query("availability") String availability, @Query("userId") Integer userId);

    @GET("api/petsitting/{id}")
    Call<PetSittingOffer> getPetSittingOfferById(@Path("id") int id);

    @PUT("api/petsitting/{id}")
    Call<PetSittingOffer> updatePetSittingOffer(@Path("id") int id, @Body PetSittingOffer offer);

    @DELETE("api/petsitting/{id}")
    Call<Void> deletePetSittingOffer(@Path("id") int id, @Query("userId") int userId);


    // AI Description
    @Multipart
    @POST("python_app/analyze-pet")
    Call<AiDescriptionResponse> generateAiDescription(@Part MultipartBody.Part file);
}
