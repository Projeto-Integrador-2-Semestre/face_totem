package br.com.pi2.facetotem.api;

import com.google.gson.annotations.SerializedName;

public class HealthResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("embeddings_em_cache")
    public int embeddingsEmCache;

    @SerializedName("anti_spoofing")
    public boolean antiSpoofing;
}