package br.com.pi2.facetotem.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.com.pi2.facetotem.Config;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fábrica do Retrofit. A URL base vem das configurações salvas no aparelho (ver {@link Config}),
 * então pode ser trocada pela tela de configuração sem recompilar.
 */
public class ApiClient {

    private static FaceApi instancia;
    private static String urlDaInstancia;

    public static synchronized FaceApi get(Context context) {
        String url = Config.getBaseUrl(context);

        // Recria o cliente se a URL mudou desde a última vez.
        if (instancia == null || !url.equals(urlDaInstancia)) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);

            // Timeouts curtos de propósito: num totem, esperar 30 s por uma API morta é inaceitável.
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .writeTimeout(8, TimeUnit.SECONDS)
                    .addInterceptor(log)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instancia = retrofit.create(FaceApi.class);
            urlDaInstancia = url;
        }
        return instancia;
    }
}
