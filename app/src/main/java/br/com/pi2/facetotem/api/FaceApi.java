package br.com.pi2.facetotem.api;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Endpoints da face_api. O Retrofit gera a implementação desta interface.
 */
public interface FaceApi {

    @GET("health")
    Call<HealthResponse> health();

    /**
     * Envia uma imagem (multipart, campo "file") e recebe o resultado do reconhecimento.
     */
    @Multipart
    @POST("faces/recognize")
    Call<ReconhecimentoResponse> recognize(@Part MultipartBody.Part file);
}
