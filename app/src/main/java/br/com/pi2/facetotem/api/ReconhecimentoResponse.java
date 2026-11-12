package br.com.pi2.facetotem.api;

import com.google.gson.annotations.SerializedName;

/**
 * Resposta do POST /faces/recognize. Os nomes seguem o modelo Pydantic `Reconhecimento` da API.
 */
public class ReconhecimentoResponse {

    // Valores possíveis de `motivo` quando autorizado == false
    public static final String MOTIVO_NENHUM_ROSTO = "nenhum_rosto";
    public static final String MOTIVO_ROSTO_NAO_VIVO = "rosto_nao_vivo";
    public static final String MOTIVO_ROSTO_CORTADO = "rosto_cortado";
    public static final String MOTIVO_NAO_RECONHECIDO = "nao_reconhecido";

    @SerializedName("autorizado")
    public boolean autorizado;

    @SerializedName("motivo")
    public String motivo;

    @SerializedName("cpf")
    public String cpf;

    @SerializedName("nome")
    public String nome;

    @SerializedName("similaridade")
    public double similaridade;

    @SerializedName("live_score")
    public Double liveScore;

    @SerializedName("acesso")
    public Acesso acesso;

    @SerializedName("tempo_ms")
    public int tempoMs;

    public static class Acesso {
        @SerializedName("tipo")
        public String tipo;

        @SerializedName("data_hora")
        public String dataHora;

        @SerializedName("registrado")
        public boolean registrado;
    }

    /**
     * Um resultado é "decisivo" quando o totem deve mostrar a tela verde ou vermelha.
     * `nenhum_rosto` e `rosto_cortado` não são: a pessoa ainda está se posicionando.
     */
    public boolean isDecisivo() {
        if (autorizado) return true;
        return MOTIVO_NAO_RECONHECIDO.equals(motivo) || MOTIVO_ROSTO_NAO_VIVO.equals(motivo);
    }
}
