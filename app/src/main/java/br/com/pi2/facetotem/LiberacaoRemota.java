package br.com.pi2.facetotem;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import br.com.pi2.facetotem.api.GestaoApi;
import br.com.pi2.facetotem.api.GestaoClient;
import br.com.pi2.facetotem.api.LiberacaoResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Consulta periódica da API de gestão para saber se o operador liberou esta catraca
 * pelo botão "Liberar catraca" do painel web. Quando há uma liberação, abre a tela
 * "ACESSO LIBERADO".
 *
 * Roda só enquanto a tela está em primeiro plano (início em onResume, fim em onPause)
 * e só quando a URL da API de gestão e o ID da catraca estão configurados.
 */
class LiberacaoRemota {

    private static final String TAG = "FaceTotem";
    static final long INTERVALO_MS = 1500;

    private final TotemActivity tela;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ativa = false;
    private Call<LiberacaoResponse> chamadaAtual;

    private final Runnable consultar = this::consultar;

    LiberacaoRemota(TotemActivity tela) {
        this.tela = tela;
    }

    void iniciar() {
        if (ativa || !Config.isLiberacaoRemotaConfigurada(tela)) {
            return;
        }
        ativa = true;
        handler.post(consultar);
    }

    void parar() {
        ativa = false;
        handler.removeCallbacks(consultar);
        if (chamadaAtual != null) {
            chamadaAtual.cancel();
            chamadaAtual = null;
        }
    }

    private void consultar() {
        if (!ativa) return;
        GestaoApi api = GestaoClient.get(tela);
        long catracaId = Config.getCatracaId(tela);
        if (api == null || catracaId <= 0) {
            ativa = false;
            return;
        }

        // A próxima consulta só é agendada quando esta termina: nunca há duas ao mesmo tempo.
        chamadaAtual = api.confirmarLiberacao(catracaId);
        chamadaAtual.enqueue(new Callback<LiberacaoResponse>() {
            @Override
            public void onResponse(Call<LiberacaoResponse> call, Response<LiberacaoResponse> response) {
                chamadaAtual = null;
                if (!ativa) return;
                LiberacaoResponse liberacao = response.body();
                if (response.code() == 200 && liberacao != null) {
                    Log.i(TAG, "Liberação manual #" + liberacao.id + " recebida para a catraca " + catracaId);
                    ativa = false;
                    abrirAcessoLiberado(liberacao);
                    return;
                }
                if (!response.isSuccessful()) {
                    Log.w(TAG, "API de gestão respondeu HTTP " + response.code() + " ao consultar liberação");
                }
                agendar();
            }

            @Override
            public void onFailure(Call<LiberacaoResponse> call, Throwable t) {
                chamadaAtual = null;
                if (call.isCanceled() || !ativa) return;
                Log.w(TAG, "Falha ao consultar liberação manual: " + t.getMessage());
                agendar();
            }
        });
    }

    private void agendar() {
        if (ativa) {
            handler.postDelayed(consultar, INTERVALO_MS);
        }
    }

    private void abrirAcessoLiberado(LiberacaoResponse liberacao) {
        Intent intent = new Intent(tela, ResultadoActivity.class);
        intent.putExtra(ResultadoActivity.EXTRA_AUTORIZADO, true);
        intent.putExtra(ResultadoActivity.EXTRA_MANUAL, true);
        intent.putExtra(ResultadoActivity.EXTRA_OPERADOR, liberacao.operador);
        intent.putExtra(ResultadoActivity.EXTRA_MOTIVO, liberacao.motivo);
        tela.startActivity(intent);
        // Na câmera, sai da pilha: quando o resultado fechar, volta para a tela inicial.
        if (tela instanceof CameraActivity) {
            tela.finish();
        }
    }
}
