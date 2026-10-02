package br.com.jylabtech.pagseguro_payment_tech.licence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Dados de teste (sem segredos reais). */
final class LicenceTestData {

    private LicenceTestData() {}

    static final String LICENCE_KEY = "test-licence-key-0000";
    static final String CORE_URL = "https://licence-server.invalid/pos-payments/licence/check";
    static final String CORE_BASE64 = "***REMOVIDO***";
    static final String CORE_INVOICE_BASE64 = "***REMOVIDO***";
    static final String LEGACY_URL = "https://licence-server.invalid/pos-payments/licence/check";
    static final String LEGACY_BASE64 = "***REMOVIDO***";
    static final String EVIL_BASE64 = "aHR0cHM6Ly9ldmlsLmNvbS9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjaw==";

    /** "Agora" fixo: 2026-10-01T12:00:00 em Brasília. */
    static final Date NOW = date("2026-10-01T12:00:00");

    static Date date(String value) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
            return sdf.parse(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Date plusMillis(Date base, long millis) {
        return new Date(base.getTime() + millis);
    }

    /** JSON no formato do LegacyLicence do jylabtech_core. */
    static String coreJson(String expiredAt, boolean isExpired, boolean isInactive, boolean isCanceled) {
        return "{"
                + "\"id\":\"64f000000000000000000001\","
                + "\"uid\":\"64f000000000000000000001\","
                + "\"deviceUid\":\"\","
                + "\"deviceSerialNumber\":\"\","
                + "\"userUid\":\"64f000000000000000000002\","
                + "\"productId\":\"64f000000000000000000003\","
                + "\"productName\":\"Plugin PagSeguro\","
                + "\"productDescription\":\"\","
                + "\"integrationLibId\":\"\","
                + "\"integrationLibName\":\"pagseguro_payment_tech\","
                + "\"checkoutSessionId\":\"\","
                + "\"licenceKey\":\"" + LICENCE_KEY + "\","
                + "\"licenceEncrypt\":\"" + LICENCE_KEY + "\","
                + "\"expiredAt\":\"" + expiredAt + "\","
                + "\"isExpired\":" + isExpired + ","
                + "\"isInactive\":" + isInactive + ","
                + "\"isTrial\":false,"
                + "\"isDebug\":false,"
                + "\"isCanceled\":" + isCanceled + ","
                + "\"canceledAt\":null,"
                + "\"licencesQuantity\":-1,"
                + "\"subscriptionId\":\"\","
                + "\"subscription\":null,"
                + "\"checkoutType\":\"one_time\","
                + "\"createdAt\":\"2026-01-10T08:30:00\","
                + "\"updatedAt\":\"2026-09-30T18:00:00\","
                + "\"coreToken\":\"header.payload.signature\""
                + "}";
    }

    static String validCoreJson() {
        return coreJson("2027-01-01T00:00:00", false, false, false);
    }

    static CheckLicenceEntity entity(Date expiredAt, boolean isExpired, boolean isInactive, boolean isCanceled) {
        CheckLicenceEntity entity = new CheckLicenceEntity();
        entity.setExpiredAt(expiredAt);
        entity.setExpired(isExpired);
        entity.setInactive(isInactive);
        entity.setCanceled(isCanceled);
        entity.setLicenceKey(LICENCE_KEY);
        entity.setLicenceEncrypt(LICENCE_KEY);
        return entity;
    }

    static CheckLicenceEntity validCache(Date savedAt) {
        CheckLicenceEntity entity = entity(date("2027-01-01T00:00:00"), false, false, false);
        entity.setSavedLocalDatabaseAt(savedAt);
        entity.setLicenceInternalKey(CORE_URL);
        return entity;
    }
}
