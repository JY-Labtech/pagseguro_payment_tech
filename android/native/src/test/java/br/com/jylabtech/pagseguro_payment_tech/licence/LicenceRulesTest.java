package br.com.jylabtech.pagseguro_payment_tech.licence;

import static br.com.jylabtech.pagseguro_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LicenceRulesTest {

    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long MINUTE = 60L * 1000L;
    private static final long HOUR = 60L * MINUTE;

    // isLicenceUsable --------------------------------------------------------

    @Test
    public void validLicenceIsUsable() {
        assertTrue(LicenceRules.isLicenceUsable(entity(plusMillis(NOW, DAY), false, false, false), NOW));
    }

    @Test
    public void expiredByDateIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity(plusMillis(NOW, -1000), false, false, false), NOW));
        assertFalse(LicenceRules.isLicenceUsable(entity(NOW, false, false, false), NOW));
    }

    @Test
    public void isExpiredFlagIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity(plusMillis(NOW, DAY), true, false, false), NOW));
    }

    @Test
    public void isInactiveFlagIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity(plusMillis(NOW, DAY), false, true, false), NOW));
    }

    @Test
    public void isCanceledFlagIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity(plusMillis(NOW, DAY), false, false, true), NOW));
    }

    @Test
    public void nullExpiredAtIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity(null, false, false, false), NOW));
    }

    @Test
    public void nullEntityIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(null, NOW));
    }

    @Test
    public void masterKey2099IsUsable() {
        assertTrue(LicenceRules.isLicenceUsable(entity(date("2099-12-31T23:59:59"), false, false, false), NOW));
    }

    // Cache ------------------------------------------------------------------

    @Test
    public void freshCacheWithSameKeyAndUrlIsUsable() {
        assertTrue(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -DAY)), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheSavedExactly72HoursAgoIsUsable() {
        assertTrue(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -72 * HOUR)), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheOlderThan72HoursIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -72 * HOUR - 1000)), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheOfFourDaysIsNoLongerValid() {
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -4 * DAY)), LICENCE_KEY, CORE_URL, NOW));
        assertFalse(LicenceRules.isCacheFresh(validCache(plusMillis(NOW, -4 * DAY)), NOW));
    }

    @Test
    public void shouldRevalidateOnFirstCheckOfProcessAndAfterInterval() {
        CheckLicenceEntity cache = validCache(plusMillis(NOW, -10 * MINUTE));
        assertTrue(LicenceRules.shouldRevalidate(cache, false, NOW));
        assertFalse(LicenceRules.shouldRevalidate(cache, true, NOW));
        assertTrue(LicenceRules.shouldRevalidate(null, true, NOW));
        cache.setSavedLocalDatabaseAt(plusMillis(NOW, -LicenceConfig.REVALIDATE_INTERVAL_MS));
        assertTrue(LicenceRules.shouldRevalidate(cache, true, NOW));
    }

    @Test
    public void definitiveRejectionCodes() {
        for (int code : new int[]{400, 401, 402, 403, 404, 409, 410, 412, 423, 428}) {
            assertTrue("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
        for (int code : new int[]{200, 201, 204, 301, 408, 429, 500, 502, 503, 504}) {
            assertFalse("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
    }

    @Test
    public void cacheSavedInTheFutureIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, 6 * MINUTE)), LICENCE_KEY, CORE_URL, NOW));
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, 3 * DAY)), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheSavedSlightlyInTheFutureWithinToleranceIsUsable() {
        assertTrue(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, 4 * MINUTE)), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheWithoutSavedAtIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(validCache(null), LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheWithDifferentKeyIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -DAY)), "other-key", CORE_URL, NOW));
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -DAY)), null, CORE_URL, NOW));
    }

    @Test
    public void cacheWithDifferentUrlIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(validCache(plusMillis(NOW, -DAY)), LICENCE_KEY, CORE_URL + "/invoice", NOW));
    }

    @Test
    public void cacheSavedWithUntrustedUrlIsNotUsable() {
        CheckLicenceEntity evil = validCache(plusMillis(NOW, -DAY));
        evil.setLicenceInternalKey(EVIL_BASE64);
        assertFalse(LicenceRules.isCacheUsable(evil, LICENCE_KEY, CORE_URL, NOW));

        CheckLicenceEntity legacy = validCache(plusMillis(NOW, -DAY));
        legacy.setLicenceInternalKey(LEGACY_BASE64);
        assertFalse(LicenceRules.isCacheUsable(legacy, LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheFromPreviousVersionWithBase64CoreKeyIsUsable() {
        CheckLicenceEntity old = validCache(plusMillis(NOW, -DAY));
        old.setLicenceInternalKey(CORE_BASE64);
        assertTrue(LicenceRules.isCacheUsable(old, LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void expiredLicenceInCacheIsNotUsable() {
        CheckLicenceEntity cache = validCache(plusMillis(NOW, -DAY));
        cache.setExpiredAt(plusMillis(NOW, -1000));
        assertFalse(LicenceRules.isCacheUsable(cache, LICENCE_KEY, CORE_URL, NOW));

        CheckLicenceEntity canceled = validCache(plusMillis(NOW, -DAY));
        canceled.setCanceled(true);
        assertFalse(LicenceRules.isCacheUsable(canceled, LICENCE_KEY, CORE_URL, NOW));
    }

    @Test
    public void cacheSurvivesJsonRoundTrip() {
        CheckLicenceEntity cache = validCache(plusMillis(NOW, -DAY));
        CheckLicenceEntity restored = CheckLicenceEntity.fromJson(cache.toJson());
        assertTrue(LicenceRules.isCacheUsable(restored, LICENCE_KEY, CORE_URL, NOW));
    }
}
