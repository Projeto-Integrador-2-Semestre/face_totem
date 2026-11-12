package br.com.pi2.facetotem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.pi2.facetotem.api.ApiClient;
import br.com.pi2.facetotem.api.ReconhecimentoResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 2 do protótipo: preview da câmera frontal com a moldura do rosto.
 *
 * Fluxo: a cada ~0,8 s um frame é convertido em JPEG e enviado para POST /faces/recognize.
 * - nenhum_rosto / rosto_cortado: continua tentando (a pessoa ainda está se posicionando)
 * - autorizado / nao_reconhecido / rosto_nao_vivo: abre a tela de resultado e fecha esta
 * - sem resultado decisivo em 15 s: volta para a tela inicial
 */
public class CameraActivity extends TotemActivity {

    private static final String TAG = "FaceTotem";

    /** Intervalo mínimo entre dois envios. A API tem cooldown próprio de 4 s para a mesma pessoa. */
    private static final long INTERVALO_ENVIO_MS = 800;
    /** Sem resultado decisivo nesse tempo, volta para a tela inicial. */
    private static final long TEMPO_MAXIMO_MS = 15_000;
    /** Maior lado da imagem enviada. Menos bytes, menos latência; a API já reduz internamente. */
    private static final int LADO_MAXIMO_PX = 640;
    private static final int QUALIDADE_JPEG = 80;

    private PreviewView preview;
    private TextView txtDica;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorAnalise = Executors.newSingleThreadExecutor();

    private volatile boolean emVoo = false;      // há uma requisição em andamento
    private volatile boolean finalizado = false; // a tela já decidiu o que fazer; ignorar o resto
    private long ultimoEnvio = 0;

    private final ActivityResultLauncher<String> pedirCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    iniciarCamera();
                } else {
                    Toast.makeText(this, R.string.sem_permissao_camera, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        preview = findViewById(R.id.preview);
        txtDica = findViewById(R.id.txtDica);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamera();
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finalizado = true;
        handler.removeCallbacksAndMessages(null);
        executorAnalise.shutdown();
    }

    private void iniciarCamera() {
        // Relógio de desistência: se ninguém for reconhecido, volta para a tela inicial.
        handler.postDelayed(this::desistir, TEMPO_MAXIMO_MS);

        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider provider = futuro.get();

                // Use case 1: Preview — mostra a imagem no PreviewView.
                Preview previewUseCase = new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());

                // Use case 2: ImageAnalysis — entrega os frames para o nosso analisador.
                ResolutionSelector resolucao = new ResolutionSelector.Builder()
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                ImageAnalysis analise = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolucao)
                        // Se o analisador estiver ocupado, descarta frames antigos e entrega só o mais novo.
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // RGBA deixa a conversão para Bitmap direta (sem lidar com YUV na mão).
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analise.setAnalyzer(executorAnalise, this::analisarFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase, analise);

            } catch (Exception e) {
                Log.e(TAG, "Falha ao iniciar a câmera", e);
                Toast.makeText(this, R.string.erro_camera, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Roda na thread do executorAnalise, para cada frame que a câmera entrega. */
    private void analisarFrame(ImageProxy image) {
        try {
            long agora = SystemClock.elapsedRealtime();
            if (finalizado || emVoo || agora - ultimoEnvio < INTERVALO_ENVIO_MS) {
                return; // descarta este frame
            }
            emVoo = true;
            ultimoEnvio = agora;

            int rotacao = image.getImageInfo().getRotationDegrees();
            Bitmap bitmap = image.toBitmap();
            byte[] jpeg = paraJpeg(bitmap, rotacao);
            enviar(jpeg);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao processar frame", e);
            emVoo = false;
        } finally {
            image.close(); // obrigatório, senão a câmera para de entregar frames
        }
    }

    /** Reduz, corrige a rotação do sensor e comprime em JPEG. */
    private static byte[] paraJpeg(Bitmap original, int rotacaoGraus) {
        int w = original.getWidth();
        int h = original.getHeight();
        float escala = Math.min(1f, (float) LADO_MAXIMO_PX / Math.max(w, h));

        Matrix m = new Matrix();
        m.postScale(escala, escala);
        m.postRotate(rotacaoGraus);

        Bitmap ajustado = Bitmap.createBitmap(original, 0, 0, w, h, m, true);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ajustado.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
        return saida.toByteArray();
    }

    private void enviar(byte[] jpeg) {
        RequestBody corpo = RequestBody.create(jpeg, MediaType.get("image/jpeg"));
        // "file" precisa bater com o parâmetro `file: UploadFile` do endpoint.
        MultipartBody.Part parte = MultipartBody.Part.createFormData("file", "frame.jpg", corpo);

        // enqueue: roda em background e chama os callbacks na thread principal.
        ApiClient.get(this).recognize(parte).enqueue(new Callback<ReconhecimentoResponse>() {
            @Override
            public void onResponse(Call<ReconhecimentoResponse> call, Response<ReconhecimentoResponse> response) {
                emVoo = false;
                if (finalizado) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "API respondeu HTTP " + response.code());
                    txtDica.setText(R.string.dica_camera);
                    return;
                }
                tratarResultado(response.body());
            }

            @Override
            public void onFailure(Call<ReconhecimentoResponse> call, Throwable t) {
                emVoo = false;
                if (finalizado) return;
                Log.e(TAG, "Falha ao chamar /faces/recognize", t);
                txtDica.setText(R.string.api_indisponivel);
            }
        });
    }

    private void tratarResultado(ReconhecimentoResponse r) {
        Log.i(TAG, "recognize: autorizado=" + r.autorizado + " motivo=" + r.motivo
                + " nome=" + r.nome + " sim=" + r.similaridade + " tempo=" + r.tempoMs + "ms");

        if (r.isDecisivo()) {
            mostrarResultado(r.autorizado, r.nome);
            return;
        }

        // Não decisivo: orienta a pessoa e continua enviando frames.
        if (ReconhecimentoResponse.MOTIVO_ROSTO_CORTADO.equals(r.motivo)) {
            txtDica.setText(R.string.dica_centralize);
        } else {
            txtDica.setText(R.string.dica_camera);
        }
    }

    private void mostrarResultado(boolean autorizado, String nome) {
        finalizado = true;
        handler.removeCallbacksAndMessages(null);

        Intent intent = new Intent(this, ResultadoActivity.class);
        intent.putExtra(ResultadoActivity.EXTRA_AUTORIZADO, autorizado);
        intent.putExtra(ResultadoActivity.EXTRA_NOME, nome);
        startActivity(intent);
        finish(); // sai da pilha: quando o resultado fechar, volta direto para a MainActivity
    }

    private void desistir() {
        if (finalizado) return;
        finalizado = true;
        Toast.makeText(this, R.string.tempo_esgotado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
