package br.com.pi2.facetotem.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
     * catracaId vai para o log de entrada/saída (o painel mostra em qual catraca a pessoa passou);
     * null quando o ID da catraca não foi configurado (o Retrofit omite a parte).
     */
    @Multipart
    @POST("faces/recognize")
    Call<ReconhecimentoResponse> recognize(@Part MultipartBody.Part file,
                                           @Part("catraca_id") RequestBody catracaId);
}
