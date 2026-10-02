package br.com.jylabtech.pagseguro_payment_tech.licence;

import static br.com.jylabtech.pagseguro_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LicenceUrlTest {

    @Test
    public void defaultConstantPointsToCore() {
        assertEquals(CORE_URL, LicenceConfig.LICENCE_CHECK_URL);
    }

    @Test
    public void acceptsCorePlainText() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(CORE_URL));
    }

    @Test
    public void acceptsCoreBase64() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(CORE_BASE64));
    }

    @Test
    public void acceptsInvoiceAndSubscriptionVariants() {
        assertEquals(CORE_URL + "/invoice", LicenceRules.resolveLicenceUrl(CORE_INVOICE_BASE64));
        assertEquals(CORE_URL + "/invoice", LicenceRules.resolveLicenceUrl(CORE_URL + "/invoice"));
        assertEquals(CORE_URL + "/subscription", LicenceRules.resolveLicenceUrl(CORE_URL + "/subscription"));
    }

    @Test
    public void acceptsUppercaseHostAndExplicitPort443() {
        String url = "https://PORTAL-API.jylabtech.com.br:443/pos-payments/licence/check";
        assertEquals(url, LicenceRules.resolveLicenceUrl(url));
    }

    @Test
    public void legacyCloudRunFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(LEGACY_URL));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(LEGACY_BASE64));
    }

    @Test
    public void otherHostFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(EVIL_BASE64));
    }

    @Test
    public void maliciousHostSuffixFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br.evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://evil.com/portal-api.jylabtech.com.br/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://evilportal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void userInfoFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br@evil.com/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://user:pass@portal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void plainHttpFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("http://portal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void wrongPathPortOrQueryFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br/other/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br/pos-payments/licence/checkout"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br/pos-payments/licence/check/../../evil"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br:8443/pos-payments/licence/check"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("https://portal-api.jylabtech.com.br/pos-payments/licence/check?x=1"));
    }

    @Test
    public void emptyOrNullFallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(null));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl(""));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("   "));
    }

    @Test
    public void garbageBase64FallsBackToCore() {
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("!!!not-base64!!!"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("abcde"));
        assertEquals(CORE_URL, LicenceRules.resolveLicenceUrl("YWJjZA=="));
    }

    @Test
    public void resolveOfficialUrlReturnsNullForUntrustedValues() {
        assertNull(LicenceRules.resolveOfficialUrl(LEGACY_BASE64));
        assertNull(LicenceRules.resolveOfficialUrl(EVIL_BASE64));
        assertNull(LicenceRules.resolveOfficialUrl(null));
        assertEquals(CORE_URL, LicenceRules.resolveOfficialUrl(CORE_BASE64));
    }

    @Test
    public void base64DecoderHandlesUrlSafeAndWhitespace() {
        assertEquals("abcd", LicenceRules.decodeBase64Utf8("YWJjZA=="));
        assertEquals("abcd", LicenceRules.decodeBase64Utf8("YWJjZA"));
        assertEquals(CORE_URL, LicenceRules.decodeBase64Utf8(CORE_BASE64.substring(0, 40) + "\n" + CORE_BASE64.substring(40)));
        assertNull(LicenceRules.decodeBase64Utf8("YW=JjZA"));
    }
}
