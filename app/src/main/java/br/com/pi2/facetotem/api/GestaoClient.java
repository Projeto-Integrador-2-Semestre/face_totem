package br.com.pi2.facetotem.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.com.pi2.facetotem.Config;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fábrica do Retrofit da API de gestão (Java). A URL vem da configuração escondida;
 * sem URL configurada devolve null e o totem não consulta liberações manuais.
 */
public class GestaoClient {

    private static GestaoApi instancia;
    private static String urlDaInstancia;

    public static synchronized GestaoApi get(Context context) {
        String url = Config.getUrlGestao(context);
        if (url.isEmpty()) {
            return null;
        }
        if (instancia == null || !url.equals(urlDaInstancia)) {
            // Sem log de corpo: a consulta roda a cada 1,5 s e encheria o Logcat.
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instancia = retrofit.create(GestaoApi.class);
            urlDaInstancia = url;
        }
        return instancia;
    }
}
