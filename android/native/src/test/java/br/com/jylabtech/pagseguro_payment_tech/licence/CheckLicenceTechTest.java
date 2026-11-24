package br.com.jylabtech.pagseguro_payment_tech.licence;

import static br.com.jylabtech.pagseguro_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

public class CheckLicenceTechTest {

    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long HOUR = 60L * 60L * 1000L;
    private static final long MINUTE = 60L * 1000L;

    private FakeStorage storage;
    private FakeRemote remote;
    private Date now;
    private CheckLicenceTech tech;

    static final class FakeStorage implements LicenceStorage {
        String json;
        int deletes;
        int saves;

        @Override
        public CheckLicenceEntity read() {
            return json == null ? null : CheckLicenceEntity.fromJson(json);
        }

        @Override
        public void save(CheckLicenceEntity entity) {
            saves++;
            json = entity.toJson();
        }

        @Override
        public void delete() {
            deletes++;
            json = null;
        }
    }

    static final class FakeRemote implements LicenceRemote {
        int calls;
        String lastUrl;
        String lastKey;
        int status = 200;
        String body = validCoreJson();
        Exception toThrow;

        @Override
        public LicenceResult check(String url, String licenceKey) throws Exception {
            calls++;
            lastUrl = url;
            lastKey = licenceKey;
            if (toThrow != null) throw toThrow;
            return LicenceRules.evaluateHttpResponse(status, body, NOW);
        }
    }

    @Before
    public void setUp() {
        storage = new FakeStorage();
        remote = new FakeRemote();
        now = NOW;
        tech = new CheckLicenceTech(storage, remote, () -> now);
    }

    @Test
    public void onlineSuccessSavesCacheWithResolvedUrlAndNow() {
        LicenceResult result = tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        assertTrue(result.isSuccess());
        assertEquals(1, remote.calls);
        assertEquals(CORE_URL, remote.lastUrl);
        assertEquals(LICENCE_KEY, remote.lastKey);
        assertEquals(1, storage.saves);
        CheckLicenceEntity saved = storage.read();
        assertEquals(CORE_URL, saved.getLicenceInternalKey());
        assertEquals(NOW, saved.getSavedLocalDatabaseAt());
    }

    @Test
    public void untrustedInternalKeyStillCallsCore() {
        assertTrue(tech.validateLicence(LICENCE_KEY, EVIL_BASE64).isSuccess());
        assertEquals(CORE_URL, remote.lastUrl);
        assertTrue(tech.validateLicence("k2", LEGACY_BASE64).isSuccess());
        assertEquals(CORE_URL, remote.lastUrl);
        assertTrue(tech.validateLicence("k3", null).isSuccess());
        assertEquals(CORE_URL, remote.lastUrl);
    }

    private void advance(long millis) {
        now = new Date(now.getTime() + millis);
    }

    private LicenceResult validate() {
        return tech.validateLicence(LICENCE_KEY, CORE_BASE64);
    }

    @Test
    public void firstCheckOfProcessGoesOnlineEvenWithValidCache() {
        storage.json = validCache(now).toJson();
        assertTrue(validate().isSuccess());
        assertEquals(1, remote.calls);
        assertEquals(now, storage.read().getSavedLocalDatabaseAt());
    }

    @Test
    public void validCacheSkipsNetworkAfterOnlineCheckUntilRevalidationInterval() {
        validate();
        assertEquals(1, remote.calls);

        advance(HOUR);
        assertTrue(validate().isSuccess());
        assertEquals(1, remote.calls);
        assertTrue(tech.validateExistsLicence());

        advance(LicenceConfig.REVALIDATE_INTERVAL_MS);
        assertTrue(validate().isSuccess());
        assertEquals(2, remote.calls);
    }

    @Test
    public void cacheOnlyShortcutRequiresOnlineCheckInThisProcess() {
        storage.json = validCache(now).toJson();
        // app recém-aberto: o atalho sem rede não libera, força a consulta ao servidor
        assertFalse(tech.validateExistsLicence());
        assertNotNull(storage.json);

        validate();
        assertTrue(tech.validateExistsLicence());

        advance(LicenceConfig.REVALIDATE_INTERVAL_MS);
        assertFalse(tech.validateExistsLicence());

        // servidor fora do ar: o atalho libera pelo cache durante a espera entre tentativas
        remote.toThrow = new IOException("sem internet");
        assertTrue(validate().isSuccess());
        assertTrue(tech.validateExistsLicence());
        advance(LicenceConfig.RETRY_AFTER_FAILURE_MS);
        assertFalse(tech.validateExistsLicence());
    }

    @Test
    public void customerBlockedClearsCacheAndBlocksImmediately() {
        validate();
        advance(LicenceConfig.REVALIDATE_INTERVAL_MS);
        remote.status = 428;
        remote.body = "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}";

        LicenceResult result = validate();
        assertFalse(result.isSuccess());
        assertTrue(result.isRejectedByServer());
        assertEquals(LicenceConfig.CUSTOMER_BLOCKED_MESSAGE, result.getErrorMessage());
        assertNull(storage.json);
        assertFalse(tech.validateExistsLicence());
        // sem cache: a próxima operação também consulta o servidor e continua bloqueada
        assertFalse(validate().isSuccess());
        assertEquals(3, remote.calls);
    }

    @Test
    public void everyDefinitiveRefusalClearsValidCache() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            setUp();
            storage.json = validCache(now).toJson();
            remote.status = code;
            remote.body = "{\"message\":\"recusada\"}";
            LicenceResult result = validate();
            assertFalse("HTTP " + code, result.isSuccess());
            assertEquals("recusada", result.getErrorMessage());
            assertNull("HTTP " + code, storage.json);
        }
    }

    @Test
    public void online200CanceledClearsValidCache() {
        storage.json = validCache(now).toJson();
        remote.body = coreJson("2027-01-01T00:00:00", false, false, true);
        LicenceResult result = validate();
        assertFalse(result.isSuccess());
        assertTrue(result.isRejectedByServer());
        assertNull(storage.json);
    }

    @Test
    public void outageUsesCacheUpTo72Hours() {
        validate();
        remote.toThrow = new IOException("sem internet");

        advance(71 * HOUR);
        assertTrue(validate().isSuccess());
        assertNotNull(storage.json);

        advance(HOUR + 1000);
        LicenceResult result = validate();
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("sem internet"));
        assertNull(storage.json);
    }

    @Test
    public void serverErrorsUseCache() {
        for (int code : new int[]{500, 502, 503, 408, 429}) {
            setUp();
            storage.json = validCache(now).toJson();
            remote.status = code;
            remote.body = "{\"message\":\"fora do ar\"}";
            assertTrue("HTTP " + code, validate().isSuccess());
            assertNotNull("HTTP " + code, storage.json);
        }
    }

    @Test
    public void unreadable200UsesCache() {
        storage.json = validCache(now).toJson();
        remote.body = "<html>captive portal</html>";
        assertTrue(validate().isSuccess());
        assertNotNull(storage.json);
    }

    @Test
    public void afterOutageWaitsBeforeRetryingServer() {
        storage.json = validCache(now).toJson();
        remote.toThrow = new IOException("sem internet");
        validate();
        assertEquals(1, remote.calls);

        advance(MINUTE);
        assertTrue(validate().isSuccess());
        assertEquals(1, remote.calls);

        advance(LicenceConfig.RETRY_AFTER_FAILURE_MS);
        remote.toThrow = null;
        remote.status = 428;
        remote.body = "{\"message\":\"Licença vencida\"}";
        assertFalse(validate().isSuccess());
        assertEquals(2, remote.calls);
        assertNull(storage.json);
    }

    @Test
    public void outageWithoutCacheBlocksAndKeepsTrying() {
        remote.toThrow = new IOException("sem internet");
        assertFalse(validate().isSuccess());
        assertFalse(validate().isSuccess());
        assertEquals(2, remote.calls);
    }

    @Test
    public void staleCacheIsDeletedAndRevalidatedOnline() {
        tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        now = new Date(NOW.getTime() + 73 * HOUR);
        remote.status = 428;
        remote.body = "{\"message\":\"Licença vencida\"}";
        LicenceResult result = tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        assertFalse(result.isSuccess());
        assertEquals("Licença vencida", result.getErrorMessage());
        assertEquals(2, remote.calls);
        assertNull(storage.json);
    }

    @Test
    public void differentKeyInvalidatesCache() {
        tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        remote.status = 404;
        remote.body = "{\"message\":\"Licença não encontrada\"}";
        LicenceResult result = tech.validateLicence("other-key", CORE_BASE64);
        assertFalse(result.isSuccess());
        assertEquals(2, remote.calls);
        assertNull(storage.json);
    }

    @Test
    public void ok200WithExpiredLicenceIsErrorAndNotSaved() {
        remote.body = coreJson("2026-09-01T00:00:00", false, false, false);
        LicenceResult result = tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        assertFalse(result.isSuccess());
        assertEquals(0, storage.saves);
        assertNull(storage.json);
    }

    @Test
    public void ok200WithCanceledLicenceIsErrorAndNotSaved() {
        remote.body = coreJson("2027-01-01T00:00:00", false, false, true);
        assertFalse(tech.validateLicence(LICENCE_KEY, CORE_BASE64).isSuccess());
        assertEquals(0, storage.saves);
    }

    @Test
    public void networkExceptionIsErrorWithMessage() {
        remote.toThrow = new IOException("timeout");
        LicenceResult result = tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("timeout"));
    }

    @Test
    public void missingLicenceKeyIsErrorWithoutNetwork() {
        assertFalse(tech.validateLicence(null, CORE_BASE64).isSuccess());
        assertFalse(tech.validateLicence("  ", CORE_BASE64).isSuccess());
        assertEquals(0, remote.calls);
    }

    @Test
    public void tamperedFutureCacheIsIgnored() {
        CheckLicenceEntity cache = validCache(new Date(NOW.getTime() + 3 * DAY));
        storage.json = cache.toJson();
        remote.status = 412;
        remote.body = "{\"message\":\"Limite de aparelhos\"}";
        assertFalse(tech.validateLicence(LICENCE_KEY, CORE_BASE64).isSuccess());
        assertEquals(1, remote.calls);
    }

    @Test
    public void validateExistsLicenceFollowsCacheRules() {
        assertFalse(tech.validateExistsLicence());
        tech.validateLicence(LICENCE_KEY, CORE_BASE64);
        assertTrue(tech.validateExistsLicence());
        now = new Date(NOW.getTime() + 72 * HOUR + 1000);
        assertFalse(tech.validateExistsLicence());
        assertNull(storage.json);
    }
}
