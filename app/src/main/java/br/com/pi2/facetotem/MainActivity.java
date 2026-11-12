package br.com.pi2.facetotem;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import br.com.pi2.facetotem.api.ApiClient;
import br.com.pi2.facetotem.api.HealthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela inicial: "Reconhecimento Facial" com o botão de câmera.
 * A API é verificada em segundo plano; só aparece um aviso se ela estiver fora do ar.
 * Toque longo no título abre a configuração escondida (URL da API e modo launcher).
 */
public class MainActivity extends TotemActivity {

    private static final String TAG = "FaceTotem";

    // Permissão exigida pelo Android 16+ para falar com IPs da rede local (ex.: 10.0.2.2, 192.168.x.x)
    private static final String PERM_REDE_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK";

    private FloatingActionButton btnReconhecer;
    private TextView txtTitulo;
    private View layoutStatus;
    private TextView txtStatusApi;

    // Evita pedir a permissão em loop: o diálogo dispara onResume de novo ao fechar.
    private boolean permissaoJaPedida = false;

    // Negada ou não, tenta a API: se a URL for mesmo da rede local e estiver bloqueada,
    // a chamada falha e a tela mostra "Sistema indisponível".
    private final ActivityResultLauncher<String> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (!concedida) {
                    Log.w(TAG, "Permissão de rede local negada; tentando a API mesmo assim");
                }
                verificarApi();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReconhecer = findViewById(R.id.btnReconhecer);
        txtTitulo = findViewById(R.id.txtTitulo);
        layoutStatus = findViewById(R.id.layoutStatus);
        txtStatusApi = findViewById(R.id.txtStatusApi);

        btnReconhecer.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));

        // Configuração escondida: toque longo no título.
        txtTitulo.setOnLongClickListener(v -> {
            abrirConfiguracao();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (precisaPedirRedeLocal() && !permissaoJaPedida) {
            permissaoJaPedida = true;
            pedirPermissao.launch(PERM_REDE_LOCAL);
        } else {
            verificarApi();
        }
    }

    /**
     * A permissão de rede local só importa quando a API está num IP da rede local
     * (API no PC, emulador). Para a API na internet (Render), não é pedida.
     */
    private boolean precisaPedirRedeLocal() {
        // A permissão só existe a partir do Android 16 (API 36).
        if (Build.VERSION.SDK_INT < 36) return false;
        if (!Config.isUrlRedeLocal(Config.getBaseUrl(this))) return false;
        return ContextCompat.checkSelfPermission(this, PERM_REDE_LOCAL) != PackageManager.PERMISSION_GRANTED;
    }

    private void verificarApi() {
        ApiClient.get(this).health().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "API online, rostos em cache: " + response.body().embeddingsEmCache);
                    mostrarDisponivel();
                } else {
                    Log.w(TAG, "API respondeu HTTP " + response.code());
                    mostrarIndisponivel(getString(R.string.sistema_indisponivel));
                }
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                Log.e(TAG, "Falha ao chamar /health", t);
                mostrarIndisponivel(getString(R.string.sistema_indisponivel));
            }
        });
    }

    private void mostrarDisponivel() {
        layoutStatus.setVisibility(View.GONE);
        btnReconhecer.setEnabled(true);
        btnReconhecer.setAlpha(1f);
    }

    private void mostrarIndisponivel(String mensagem) {
        txtStatusApi.setText(mensagem);
        layoutStatus.setVisibility(View.VISIBLE);
        btnReconhecer.setEnabled(false);
        btnReconhecer.setAlpha(0.4f);
    }

    private void abrirConfiguracao() {
        View conteudo = getLayoutInflater().inflate(R.layout.dialog_config, null);
        EditText edtUrl = conteudo.findViewById(R.id.edtUrl);
        CheckBox chkLauncher = conteudo.findViewById(R.id.chkLauncher);

        edtUrl.setText(Config.getBaseUrl(this));
        chkLauncher.setChecked(Config.isLauncherAtivo(this));

        new AlertDialog.Builder(this)
                .setTitle(R.string.config_titulo)
                .setView(conteudo)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.salvar, (d, w) -> {
                    Config.setBaseUrl(this, edtUrl.getText().toString());
                    Config.setLauncherAtivo(this, chkLauncher.isChecked());
                    Toast.makeText(this, R.string.config_salva, Toast.LENGTH_SHORT).show();
                    verificarApi();
                })
                .show();
    }
}
