package br.com.jylabtech.pagseguro_payment_tech.licence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Cliente HTTP do servidor de licença. Síncrono: deve ser chamado fora da main thread.
 */
public class LicenceProvider implements LicenceRemote {

    private final Context context;

    public LicenceProvider(Context context) {
        this.context = context;
    }

    @Override
    public LicenceResult check(String urlString, String licenceKey) throws Exception {
        return checkLicence(urlString, licenceKey);
    }

    /**
     * Faz o POST no servidor oficial de licença e decide o resultado via {@link LicenceRules}.
     */
    public LicenceResult checkLicence(final String urlString, final String licenceKey) throws Exception {
        // Defesa extra: nunca consulta um host fora do servidor oficial.
        String safeUrl = LicenceRules.resolveLicenceUrl(urlString);
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("deviceId", getAndroidId());
            body.put("deviceUid", getSerialNumber());
            body.put("deviceLocalId", getDeviceUUID());
            body.put("deviceName", Build.MODEL);

            URL url = new URL(safeUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(LicenceConfig.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(LicenceConfig.READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer licence:" + licenceKey);
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readFully(stream);

            return LicenceRules.evaluateHttpResponse(responseCode, responseBody, new Date());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readFully(InputStream stream) {
        if (stream == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            return responseStr.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint("HardwareIds")
    private String getAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @SuppressLint("HardwareIds")
    private String getSerialNumber() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial();
            } else {
                return Build.SERIAL != null ? Build.SERIAL : "UNKNOWN_SERIAL";
            }
        } catch (SecurityException e) {
            return "UNKNOWN_SERIAL";
        } catch (Exception e) {
            return "UNKNOWN_SERIAL";
        }
    }

    private String getDeviceUUID() {
        return UUID.randomUUID().toString();
    }
}
