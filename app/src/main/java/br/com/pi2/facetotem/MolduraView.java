package br.com.pi2.facetotem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Sobreposição da tela da câmera: escurece tudo ao redor de uma "janela" arredondada
 * onde o rosto deve ficar, e desenha quatro cantos brancos nessa janela.
 */
public class MolduraView extends View {

    private static final float LARGURA_JANELA = 0.62f;   // fração da largura da tela
    private static final float PROPORCAO = 4f / 3f;      // altura / largura (retrato)
    private static final float CENTRO_VERTICAL = 0.42f;  // fração da altura onde fica o centro

    private final Paint paintScrim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCanto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pathScrim = new Path();
    private final RectF janela = new RectF();

    private float raio;
    private float tamanhoCanto;

    public MolduraView(Context context) {
        this(context, null);
    }

    public MolduraView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        raio = 24 * dp;
        tamanhoCanto = 36 * dp;

        paintScrim.setColor(ContextCompat.getColor(context, R.color.totem_scrim));
        paintScrim.setStyle(Paint.Style.FILL);

        paintCanto.setColor(Color.WHITE);
        paintCanto.setStyle(Paint.Style.STROKE);
        paintCanto.setStrokeWidth(6 * dp);
        paintCanto.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float largura = w * LARGURA_JANELA;
        float altura = largura * PROPORCAO;
        float cx = w / 2f;
        float cy = h * CENTRO_VERTICAL;
        janela.set(cx - largura / 2, cy - altura / 2, cx + largura / 2, cy + altura / 2);

        // Retângulo inteiro menos a janela (EVEN_ODD recorta o buraco).
        pathScrim.reset();
        pathScrim.setFillType(Path.FillType.EVEN_ODD);
        pathScrim.addRect(0, 0, w, h, Path.Direction.CW);
        pathScrim.addRoundRect(janela, raio, raio, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(pathScrim, paintScrim);

        float l = janela.left, t = janela.top, r = janela.right, b = janela.bottom;
        float c = tamanhoCanto;
        // Superior esquerdo
        canvas.drawLine(l, t + c, l, t + raio, paintCanto);
        canvas.drawArc(l, t, l + 2 * raio, t + 2 * raio, 180, 90, false, paintCanto);
        canvas.drawLine(l + raio, t, l + c, t, paintCanto);
        // Superior direito
        canvas.drawLine(r - c, t, r - raio, t, paintCanto);
        canvas.drawArc(r - 2 * raio, t, r, t + 2 * raio, 270, 90, false, paintCanto);
        canvas.drawLine(r, t + raio, r, t + c, paintCanto);
        // Inferior direito
        canvas.drawLine(r, b - c, r, b - raio, paintCanto);
        canvas.drawArc(r - 2 * raio, b - 2 * raio, r, b, 0, 90, false, paintCanto);
        canvas.drawLine(r - raio, b, r - c, b, paintCanto);
        // Inferior esquerdo
        canvas.drawLine(l + c, b, l + raio, b, paintCanto);
        canvas.drawArc(l, b - 2 * raio, l + 2 * raio, b, 90, 90, false, paintCanto);
        canvas.drawLine(l, b - raio, l, b - c, paintCanto);
    }
}
