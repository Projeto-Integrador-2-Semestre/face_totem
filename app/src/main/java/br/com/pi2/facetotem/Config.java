package br.com.pi2.facetotem;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.Locale;

/**
 * Configurações do totem guardadas no aparelho (SharedPreferences).
 */
public final class Config {

    private static final String PREFS = "face_totem";
    private static final String KEY_URL = "api_url";

    /**
     * API publicada no Render. Para usar a API rodando no PC, troque pela configuração escondida:
     * http://10.0.2.2:8000/ no emulador ou http://IP-do-PC:8000/ num tablet real.
     */
    public static final String URL_PADRAO = "https://faceapi-pd4q.onrender.com/";

    private Config() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context c) {
        return prefs(c).getString(KEY_URL, URL_PADRAO);
    }

    /** Normaliza a URL: garante o esquema http:// e a barra final que o Retrofit exige. */
    public static void setBaseUrl(Context c, String url) {
        String u = url == null ? "" : url.trim();
        if (u.isEmpty()) {
            u = URL_PADRAO;
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        if (!u.endsWith("/")) {
            u = u + "/";
        }
        prefs(c).edit().putString(KEY_URL, u).apply();
    }

    /**
     * true se a URL aponta para a rede local (localhost, 10.x, 172.16-31.x, 192.168.x, *.local).
     * Só nesses casos o Android 16+ exige a permissão ACCESS_LOCAL_NETWORK.
     */
    public static boolean isUrlRedeLocal(String url) {
        String host;
        try {
            host = Uri.parse(url).getHost();
        } catch (RuntimeException e) {
            return false;
        }
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);

        if (host.equals("localhost") || host.endsWith(".local")) return true;
        if (host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")) return true;
        if (host.startsWith("172.")) {
            String[] partes = host.split("\\.");
            if (partes.length > 1) {
                try {
                    int segundo = Integer.parseInt(partes[1]);
                    return segundo >= 16 && segundo <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    // ---- Modo launcher (iniciar junto com o aparelho) ----

    private static ComponentName aliasLauncher(Context c) {
        return new ComponentName(c, "br.com.pi2.facetotem.TotemLauncher");
    }

    public static boolean isLauncherAtivo(Context c) {
        int estado = c.getPackageManager().getComponentEnabledSetting(aliasLauncher(c));
        return estado == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /**
     * Liga/desliga o alias com intent-filter HOME. Com ele ligado, ao apertar Home o Android
     * pergunta qual launcher usar; escolhendo FaceTotem "sempre", o app abre sozinho no boot.
     */
    public static void setLauncherAtivo(Context c, boolean ativo) {
        c.getPackageManager().setComponentEnabledSetting(
                aliasLauncher(c),
                ativo ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
