package br.com.pi2.facetotem.api;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Endpoints da API Java de gestão (ApiControleDeAcessoPI2) usados pelo totem.
 */
public interface GestaoApi {

    /**
     * Busca uma liberação manual pendente para esta catraca, pedida pelo operador no painel web.
     * HTTP 200 com a liberação (já marcada como confirmada) ou HTTP 204 sem corpo quando não há nenhuma.
     */
    @POST("catracas/{id}/liberacao/confirmar")
    Call<LiberacaoResponse> confirmarLiberacao(@Path("id") long catracaId);
}
