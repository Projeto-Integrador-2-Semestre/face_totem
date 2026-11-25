package br.com.pi2.facetotem.api;

import com.google.gson.annotations.SerializedName;

/** Liberação manual de catraca devolvida pela API de gestão. */
public class LiberacaoResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("catracaId")
    public long catracaId;

    @SerializedName("catracaNome")
    public String catracaNome;

    @SerializedName("operador")
    public String operador;

    @SerializedName("motivo")
    public String motivo;

    @SerializedName("status")
    public String status;
}
