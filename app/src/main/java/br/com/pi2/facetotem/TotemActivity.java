package br.com.pi2.facetotem;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Base de todas as telas do totem: tela sempre ligada e barras do sistema escondidas.
 * As barras voltam temporariamente se alguém deslizar da borda, e somem de novo sozinhas.
 */
public abstract class TotemActivity extends AppCompatActivity {

    private LiberacaoRemota liberacaoRemota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        liberacaoRemota = new LiberacaoRemota(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        esconderBarras();
        if (consultaLiberacaoManual()) {
            liberacaoRemota.iniciar();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        liberacaoRemota.parar();
    }

    /**
     * Se esta tela fica de olho nas liberações manuais feitas pelo painel web.
     * A tela de resultado desliga (já está mostrando um resultado).
     */
    protected boolean consultaLiberacaoManual() {
        return true;
    }

    /** Reinicia a consulta (ex.: depois de salvar a configuração). */
    protected void reiniciarLiberacaoManual() {
        liberacaoRemota.parar();
        if (consultaLiberacaoManual()) {
            liberacaoRemota.iniciar();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            esconderBarras();
        }
    }

    private void esconderBarras() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
