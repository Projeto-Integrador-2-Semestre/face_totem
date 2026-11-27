package br.com.pi2.facetotem;

import android.animation.ObjectAnimator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Tela de resultado: LIBERADO (verde) ou NEGADO (vermelho).
 * Fica 5 s na tela com uma barra que esvazia, toca um aviso sonoro e volta para a inicial.
 */
public class ResultadoActivity extends TotemActivity {

    public static final String EXTRA_AUTORIZADO = "autorizado";
    public static final String EXTRA_NOME = "nome";
    /** Liberação manual feita pelo operador no painel web (botão "Liberar catraca"). */
    public static final String EXTRA_MANUAL = "manual";
    public static final String EXTRA_OPERADOR = "operador";
    public static final String EXTRA_MOTIVO = "motivo";
    /** Reconhecida, mas com o acesso bloqueado no painel (motivo "pessoa_bloqueada" da face_api). */
    public static final String EXTRA_BLOQUEADA = "bloqueada";
    /** Bloqueio geral de emergência ativo (motivo "bloqueio_geral" da face_api). */
    public static final String EXTRA_BLOQUEIO_GERAL = "bloqueio_geral";

    private static final long TEMPO_NA_TELA_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ToneGenerator tom;
    private ObjectAnimator animacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        boolean autorizado = getIntent().getBooleanExtra(EXTRA_AUTORIZADO, false);
        String nome = getIntent().getStringExtra(EXTRA_NOME);

        View fundo = findViewById(R.id.main);
        ImageView imgIcone = findViewById(R.id.imgIcone);
        TextView txtStatus = findViewById(R.id.txtStatus);
        TextView txtNome = findViewById(R.id.txtNome);
        TextView txtSub = findViewById(R.id.txtSub);
        LinearProgressIndicator progresso = findViewById(R.id.progresso);

        boolean manual = getIntent().getBooleanExtra(EXTRA_MANUAL, false);

        if (manual) {
            String operador = getIntent().getStringExtra(EXTRA_OPERADOR);
            String motivo = getIntent().getStringExtra(EXTRA_MOTIVO);
            fundo.setBackgroundColor(ContextCompat.getColor(this, R.color.totem_success));
            imgIcone.setImageResource(R.drawable.ic_check);
            txtStatus.setText(R.string.acesso_liberado);
            txtNome.setText(motivo != null && !motivo.trim().isEmpty() ? motivo : getString(R.string.liberacao_manual));
            txtSub.setText(operador != null && !operador.trim().isEmpty()
                    ? getString(R.string.liberado_por, operador)
                    : getString(R.string.liberado_sub));
        } else if (autorizado) {
            fundo.setBackgroundColor(ContextCompat.getColor(this, R.color.totem_success));
            imgIcone.setImageResource(R.drawable.ic_check);
            txtStatus.setText(R.string.liberado);
            txtNome.setText(nome != null ? nome : "");
            txtSub.setText(R.string.liberado_sub);
        } else if (getIntent().getBooleanExtra(EXTRA_BLOQUEIO_GERAL, false)) {
            fundo.setBackgroundColor(ContextCompat.getColor(this, R.color.totem_danger));
            imgIcone.setImageResource(R.drawable.ic_close);
            txtStatus.setText(R.string.bloqueado);
            txtNome.setText(R.string.bloqueio_geral);
            txtSub.setText(R.string.bloqueio_geral_sub);
        } else if (getIntent().getBooleanExtra(EXTRA_BLOQUEADA, false)) {
            fundo.setBackgroundColor(ContextCompat.getColor(this, R.color.totem_danger));
            imgIcone.setImageResource(R.drawable.ic_close);
            txtStatus.setText(R.string.bloqueado);
            txtNome.setText(nome != null ? nome : "");
            txtSub.setText(R.string.bloqueado_sub);
        } else {
            fundo.setBackgroundColor(ContextCompat.getColor(this, R.color.totem_danger));
            imgIcone.setImageResource(R.drawable.ic_close);
            txtStatus.setText(R.string.negado);
            txtNome.setText("");
            txtSub.setText(R.string.negado_sub);
        }

        tocarAviso(autorizado);

        // Barra de tempo: de cheia a vazia nos 5 s.
        animacao = ObjectAnimator.ofInt(progresso, "progress", progresso.getMax(), 0);
        animacao.setDuration(TEMPO_NA_TELA_MS);
        animacao.setInterpolator(new LinearInterpolator());
        animacao.start();

        // Aqui entraria o acionamento físico da catraca quando autorizado (relé, ESP32, etc.).
        // Decisão de hardware pendente; ver README.

        handler.postDelayed(this::finish, TEMPO_NA_TELA_MS);
    }

    @Override
    protected boolean consultaLiberacaoManual() {
        return false;
    }

    private void tocarAviso(boolean autorizado) {
        try {
            tom = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
            tom.startTone(autorizado ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 300);
        } catch (RuntimeException e) {
            // Sem áudio disponível: segue sem som.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (animacao != null) {
            animacao.cancel();
        }
        if (tom != null) {
            tom.release();
        }
    }
}
