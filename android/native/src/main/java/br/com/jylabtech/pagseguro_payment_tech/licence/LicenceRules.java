package br.com.jylabtech.pagseguro_payment_tech.licence;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

/**
 * Regras puras (sem Android) da validação de licença. Testáveis em JVM.
 */
public final class LicenceRules {

    private LicenceRules() {}

    // ---------------------------------------------------------------------
    // URL
    // ---------------------------------------------------------------------

    /**
     * Resolve a URL de validação a partir do licenceInternalKey (texto com "http" ou base64).
     * Só aceita URLs https do servidor oficial; qualquer outro valor cai na URL padrão do core.
     */
    public static String resolveLicenceUrl(String licenceInternalKey) {
        String accepted = resolveOfficialUrl(licenceInternalKey);
        return accepted != null ? accepted : LicenceConfig.LICENCE_CHECK_URL;
    }

    /**
     * Igual a {@link #resolveLicenceUrl(String)}, mas devolve null quando o valor não é uma URL oficial
     * (em vez de cair no padrão). Usado para comparar o valor salvo no cache.
     */
    public static String resolveOfficialUrl(String licenceInternalKey) {
        String candidate = decodeCandidate(licenceInternalKey);
        if (candidate == null) {
            return null;
        }
        return isOfficialUrl(candidate) ? candidate : null;
    }

    static String decodeCandidate(String licenceInternalKey) {
        if (licenceInternalKey == null) {
            return null;
        }
        String value = licenceInternalKey.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.toLowerCase(Locale.US).contains("http")) {
            return value;
        }
        String decoded = decodeBase64Utf8(value);
        return decoded != null ? decoded.trim() : null;
    }

    static boolean isOfficialUrl(String url) {
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            if (uri.getRawUserInfo() != null) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || !LicenceConfig.LICENCE_HOST.equalsIgnoreCase(host)) {
                return false;
            }
            if (uri.getPort() != -1 && uri.getPort() != 443) {
                return false;
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            String path = uri.getPath();
            if (path == null || path.contains("..") || path.contains("//")) {
                return false;
            }
            return path.equals(LicenceConfig.LICENCE_PATH_PREFIX)
                    || path.startsWith(LicenceConfig.LICENCE_PATH_PREFIX + "/");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodifica base64 (padrão ou url-safe) para UTF-8. Devolve null se o texto não for base64 válido.
     * Implementação própria porque java.util.Base64 exige API 26 e android.util.Base64 não roda em JVM.
     */
    static String decodeBase64Utf8(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '-') c = '+';
            if (c == '_') c = '/';
            clean.append(c);
        }
        String s = clean.toString();
        int firstPad = s.indexOf('=');
        if (firstPad >= 0) {
            for (int i = firstPad; i < s.length(); i++) {
                if (s.charAt(i) != '=') {
                    return null;
                }
            }
            if (s.length() - firstPad > 2) {
                return null;
            }
            s = s.substring(0, firstPad);
        }
        if (s.isEmpty() || s.length() % 4 == 1) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < s.length(); i++) {
            int value = base64Value(s.charAt(i));
            if (value < 0) {
                return null;
            }
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xFF);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int base64Value(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }

    // ---------------------------------------------------------------------
    // Licença
    // ---------------------------------------------------------------------

    /**
     * A licença só pode ser usada se existir, tiver vencimento futuro e não estiver
     * vencida, inativa/suspensa ou cancelada.
     */
    public static boolean isLicenceUsable(CheckLicenceEntity entity, Date now) {
        if (entity == null || now == null) {
            return false;
        }
        Date expiredAt = entity.getExpiredAt();
        if (expiredAt == null || !expiredAt.after(now)) {
            return false;
        }
        return !entity.isExpired() && !entity.isInactive() && !entity.isCanceled();
    }

    /** Mensagem amigável para uma licença que não pode ser usada. */
    public static String describeUnusable(CheckLicenceEntity entity, Date now) {
        if (entity == null) {
            return "Resposta de licença inválida.";
        }
        if (entity.getExpiredAt() == null) {
            return "Resposta de licença inválida (sem data de vencimento).";
        }
        if (entity.isCanceled()) {
            return "Licença cancelada. Consulte o suporte do plugin.";
        }
        if (entity.isInactive()) {
            return "Licença inativa ou suspensa. Consulte o suporte do plugin.";
        }
        return "Licença vencida. Renove em https://portal.jylabtech.com.br.";
    }

    /**
     * Cache sem considerar chave/URL: licença utilizável e salva há no máximo 7 dias
     * (e não no futuro, com tolerância de 5 minutos).
     */
    public static boolean isCacheFresh(CheckLicenceEntity cache, Date now) {
        if (!isLicenceUsable(cache, now)) {
            return false;
        }
        Date savedAt = cache.getSavedLocalDatabaseAt();
        if (savedAt == null) {
            return false;
        }
        long diff = now.getTime() - savedAt.getTime();
        if (diff < -LicenceConfig.CLOCK_SKEW_TOLERANCE_MS) {
            return false;
        }
        return diff <= LicenceConfig.CACHE_MAX_AGE_MS;
    }

    /**
     * Cache offline só vale se estiver fresco, for da mesma chave de licença e da mesma URL resolvida.
     */
    public static boolean isCacheUsable(CheckLicenceEntity cache, String licenceKey, String resolvedUrl, Date now) {
        if (!isCacheFresh(cache, now)) {
            return false;
        }
        if (licenceKey == null || licenceKey.isEmpty() || !licenceKey.equals(cache.getLicenceEncrypt())) {
            return false;
        }
        String cachedUrl = resolveOfficialUrl(cache.getLicenceInternalKey());
        return cachedUrl != null && cachedUrl.equals(resolvedUrl);
    }

    // ---------------------------------------------------------------------
    // Resposta HTTP
    // ---------------------------------------------------------------------

    /**
     * Decide o resultado da validação a partir do status HTTP e do corpo da resposta.
     * Só devolve sucesso para HTTP 200 com licença utilizável.
     */
    public static LicenceResult evaluateHttpResponse(int statusCode, String body, Date now) {
        if (statusCode == 200) {
            CheckLicenceEntity entity = body == null ? null : CheckLicenceEntity.fromJson(body);
            if (entity == null) {
                return LicenceResult.error("Resposta de licença inválida.");
            }
            if (!isLicenceUsable(entity, now)) {
                return LicenceResult.error(describeUnusable(entity, now));
            }
            return LicenceResult.success(entity);
        }
        return LicenceResult.error(extractErrorMessage(statusCode, body));
    }

    static String extractErrorMessage(int statusCode, String body) {
        String fallback = "Erro ao validar licença (HTTP " + statusCode + ")";
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject json = new JSONObject(body);
            if (json.isNull("message")) {
                return fallback;
            }
            String message = json.optString("message", "").trim();
            return message.isEmpty() ? fallback : message;
        } catch (Exception e) {
            return fallback;
        }
    }
}
