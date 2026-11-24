package br.com.jylabtech.pagseguro_payment_tech.licence;

import static br.com.jylabtech.pagseguro_payment_tech.licence.LicenceTestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LicenceHttpResponseTest {

    @Test
    public void newResultStartsAsError() {
        assertFalse(new LicenceResult().isSuccess());
        assertEquals(LicenceConfig.RESULT_ERROR, new LicenceResult().getResult());
    }

    @Test
    public void ok200WithValidLicenceIsSuccess() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(200, validCoreJson(), NOW);
        assertTrue(result.isSuccess());
        assertEquals(0, result.getResult());
        assertNotNull(result.getLicenceEntity());
    }

    @Test
    public void ok200WithExpiredDateIsError() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(200, coreJson("2026-09-30T23:59:59", false, false, false), NOW);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    public void ok200WithIsExpiredIsError() {
        assertFalse(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", true, false, false), NOW).isSuccess());
    }

    @Test
    public void ok200WithIsInactiveIsError() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, true, false), NOW);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("inativa"));
    }

    @Test
    public void ok200WithIsCanceledIsError() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true), NOW);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("cancelada"));
    }

    @Test
    public void ok200WithInvalidJsonIsError() {
        assertFalse(LicenceRules.evaluateHttpResponse(200, "<html>oops</html>", NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, "", NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(200, null, NOW).isSuccess());
    }

    @Test
    public void ok200WithoutExpiredAtIsError() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(200, "{\"licenceEncrypt\":\"k\",\"isExpired\":false,\"isInactive\":false}", NOW);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    public void errorStatusesUseServerMessage() {
        int[] statuses = {404, 412, 428, 403, 400, 429, 500, 503};
        for (int status : statuses) {
            String message = "mensagem do servidor " + status;
            LicenceResult result = LicenceRules.evaluateHttpResponse(status, "{\"message\":\"" + message + "\"}", NOW);
            assertFalse("HTTP " + status, result.isSuccess());
            assertEquals(message, result.getErrorMessage());
        }
    }

    @Test
    public void emptyOrNonJsonErrorBodyUsesFallbackMessage() {
        assertEquals("Erro ao validar licença (HTTP 502)", LicenceRules.evaluateHttpResponse(502, null, NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 500)", LicenceRules.evaluateHttpResponse(500, "", NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.evaluateHttpResponse(404, "Not Found", NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 412)", LicenceRules.evaluateHttpResponse(412, "{\"message\":null}", NOW).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 428)", LicenceRules.evaluateHttpResponse(428, "{\"error\":\"x\"}", NOW).getErrorMessage());
    }

    @Test
    public void refusalsAreDefinitiveAndOutagesAreNot() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            assertTrue("HTTP " + code, LicenceRules.evaluateHttpResponse(code, "{\"message\":\"x\"}", NOW).isRejectedByServer());
        }
        for (int code : new int[]{408, 429, 500, 502, 503, 201, 204}) {
            assertFalse("HTTP " + code, LicenceRules.evaluateHttpResponse(code, "{\"message\":\"x\"}", NOW).isRejectedByServer());
        }
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson("2027-01-01T00:00:00", false, false, true), NOW).isRejectedByServer());
        assertTrue(LicenceRules.evaluateHttpResponse(200, coreJson("2026-09-30T23:59:59", false, false, false), NOW).isRejectedByServer());
        assertTrue(LicenceRules.evaluateHttpResponse(200, "{\"licenceEncrypt\":\"k\",\"isExpired\":false,\"isInactive\":false}", NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, validCoreJson(), NOW).isRejectedByServer());
        // corpo ilegível (proxy/captive portal) não é recusa da licença
        assertFalse(LicenceRules.evaluateHttpResponse(200, "<html>oops</html>", NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, "", NOW).isRejectedByServer());
        assertFalse(LicenceRules.evaluateHttpResponse(200, null, NOW).isRejectedByServer());
    }

    @Test
    public void customerBlockedReasonHasClearMessage() {
        LicenceResult result = LicenceRules.evaluateHttpResponse(428,
                "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}", NOW);
        assertTrue(result.isRejectedByServer());
        assertEquals("Cliente bloqueado. Entre em contato com a JY Labtech.", result.getErrorMessage());
    }

    @Test
    public void otherSuccessStatusesAreErrors() {
        assertFalse(LicenceRules.evaluateHttpResponse(201, validCoreJson(), NOW).isSuccess());
        assertFalse(LicenceRules.evaluateHttpResponse(204, null, NOW).isSuccess());
    }
}
