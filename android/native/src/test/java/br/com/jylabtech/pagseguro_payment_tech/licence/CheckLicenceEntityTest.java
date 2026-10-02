package br.com.jylabtech.pagseguro_payment_tech.licence;

import static br.com.jylabtech.pagseguro_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CheckLicenceEntityTest {

    @Test
    public void parsesRealCoreResponse() {
        CheckLicenceEntity entity = CheckLicenceEntity.fromJson(validCoreJson());
        assertNotNull(entity);
        assertEquals(LICENCE_KEY, entity.getLicenceEncrypt());
        assertEquals(LICENCE_KEY, entity.getLicenceKey());
        assertEquals(date("2027-01-01T00:00:00"), entity.getExpiredAt());
        assertEquals(date("2026-01-10T08:30:00"), entity.getCreatedAt());
        assertFalse(entity.isExpired());
        assertFalse(entity.isInactive());
        assertFalse(entity.isCanceled());
        assertEquals("one_time", entity.getCheckoutType());
        assertTrue(LicenceRules.isLicenceUsable(entity, NOW));
    }

    @Test
    public void parsesMasterKeyExpiry() {
        CheckLicenceEntity entity = CheckLicenceEntity.fromJson(coreJson("2099-12-31T23:59:59", false, false, false));
        assertNotNull(entity);
        assertEquals(date("2099-12-31T23:59:59"), entity.getExpiredAt());
        assertTrue(LicenceRules.isLicenceUsable(entity, NOW));
    }

    @Test
    public void parsesIsCanceled() {
        CheckLicenceEntity entity = CheckLicenceEntity.fromJson(coreJson("2027-01-01T00:00:00", false, false, true));
        assertNotNull(entity);
        assertTrue(entity.isCanceled());
    }

    @Test
    public void missingExpiredAtLeavesNullAndIsNotUsable() {
        CheckLicenceEntity entity = CheckLicenceEntity.fromJson("{\"licenceEncrypt\":\"k\",\"isExpired\":false}");
        assertNotNull(entity);
        assertNull(entity.getExpiredAt());
        assertFalse(LicenceRules.isLicenceUsable(entity, NOW));
    }

    @Test
    public void nullOrInvalidExpiredAtIsNotUsable() {
        CheckLicenceEntity nullDate = CheckLicenceEntity.fromJson("{\"expiredAt\":null}");
        assertNotNull(nullDate);
        assertNull(nullDate.getExpiredAt());
        CheckLicenceEntity badDate = CheckLicenceEntity.fromJson("{\"expiredAt\":\"amanha\"}");
        assertNotNull(badDate);
        assertNull(badDate.getExpiredAt());
    }

    @Test
    public void invalidJsonReturnsNull() {
        assertNull(CheckLicenceEntity.fromJson("not json"));
        assertNull(CheckLicenceEntity.fromJson(""));
    }

    @Test
    public void toJsonKeepsCacheFields() {
        CheckLicenceEntity cache = validCache(NOW);
        cache.setCanceled(true);
        CheckLicenceEntity restored = CheckLicenceEntity.fromJson(cache.toJson());
        assertNotNull(restored);
        assertEquals(NOW, restored.getSavedLocalDatabaseAt());
        assertEquals(CORE_URL, restored.getLicenceInternalKey());
        assertEquals(LICENCE_KEY, restored.getLicenceEncrypt());
        assertTrue(restored.isCanceled());
    }
}
