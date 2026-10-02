package br.com.jylabtech.pagseguro_payment_tech.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.testing.SdkResults;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagUserDataResult;

/** Mappers nativos: objeto do SDK -> Map/JSON exatamente no formato lido pelo Dart. */
public class ResultMappersTest {

  static PlugPagTransactionResult transactionResult(Object... fieldValues) {
    return SdkResults.transaction(fieldValues);
  }

  @Test
  public void parcelasViramListaDeMapasComQuantityAmountTotal() {
    List<Map<String, Object>> maps = ResultMappers.installmentsToMaps(Arrays.asList(
        new PlugPagInstallment(1, 2000, 2000),
        new PlugPagInstallment(3, 700, 2100)));

    assertEquals(2, maps.size());
    assertEquals(1, maps.get(0).get("quantity"));
    assertEquals(2000, maps.get(0).get("amount"));
    assertEquals(2000, maps.get(0).get("total"));
    assertEquals(3, maps.get(1).get("quantity"));
    assertEquals(700, maps.get(1).get("amount"));
    assertEquals(2100, maps.get(1).get("total"));
    assertEquals(3, maps.get(1).size());
  }

  @Test
  public void parcelasNulasOuComItemNuloNaoQuebram() {
    assertTrue(ResultMappers.installmentsToMaps(null).isEmpty());
    assertEquals(1, ResultMappers.installmentsToMaps(Arrays.asList(null, new PlugPagInstallment(1, 1, 1))).size());
  }

  @Test
  public void dadosDoUsuarioViramUserDataResultComMethodUserData() throws Exception {
    PlugPagUserDataResult sdk = new PlugPagUserDataResult("Rua A", "Campinas", "12345678000199", "Sala 2",
        "Loja X", "lojax", "SP", "loja@x.com");
    UserDataResult mapped = ResultMappers.toUserDataResult(sdk);

    assertEquals("userData", mapped.getMethod());
    assertEquals(sdk.getAddress(), mapped.getAddress());
    assertEquals(sdk.getCity(), mapped.getCity());
    assertEquals(sdk.getCnpjCpf(), mapped.getCnpjCpf());
    assertEquals(sdk.getAddressComplement(), mapped.getAddressComplement());
    assertEquals(sdk.getCompanyName(), mapped.getCompanyName());
    assertEquals(sdk.getUserNickName(), mapped.getUserNickName());
    assertEquals(sdk.getAddressState(), mapped.getAddressState());
    assertEquals(sdk.getEmail(), mapped.getEmail());

    JSONObject json = new JSONObject(JsonResults.toJson(mapped));
    assertEquals("userData", json.getString("method"));
    assertEquals(sdk.getCompanyName(), json.getString("companyName"));
  }

  @Test
  public void dadosDoUsuarioNulosViramResultadoVazio() {
    UserDataResult mapped = ResultMappers.toUserDataResult(null);
    assertEquals("userData", mapped.getMethod());
    assertNull(mapped.getCompanyName());
  }

  @Test
  public void transacaoAprovadaViraJsonComAsChavesLidasPeloDart() throws Exception {
    PlugPagTransactionResult sdk = transactionResult(
        "result", 0, "message", "APROVADA", "transactionCode", "TC1", "transactionId", "TI1",
        "hostNsu", "HN1", "nsu", "N1", "bin", "411111", "cardBrand", "VISA", "amount", "2590",
        "date", "01/10/2026", "time", "12:00:00", "terminalSerialNumber", "SN1",
        "paymentType", 1, "installments", 3, "originalAmount", 2590);
    ActionResult action = new ActionResult();
    action.setMethod("transaction");
    action.buildResponse(sdk);

    JSONObject json = new JSONObject(JsonResults.toJson(action));
    assertEquals("transaction", json.getString("method"));
    assertEquals(0, json.getInt("result"));
    assertEquals("APROVADA", json.getString("message"));
    assertEquals("TC1", json.getString("transactionCode"));
    assertEquals("TI1", json.getString("transactionId"));
    assertEquals("HN1", json.getString("hostNsu"));
    assertEquals("N1", json.getString("nsu"));
    assertEquals("411111", json.getString("bin"));
    assertEquals("VISA", json.getString("cardBrand"));
    assertEquals("2590", json.getString("amount"));
    assertEquals("SN1", json.getString("terminalSerialNumber"));
    assertEquals(1, json.getInt("paymentType"));
    assertEquals(3, json.getInt("installments"));
    assertEquals(2590, json.getInt("originalAmount"));
  }

  @Test
  public void inteirosNulosDoSdkNaoLancamNullPointerException() throws Exception {
    // Antes: setResult(int)/setOriginalAmount(int)/setPaymentType(int) com Integer null => NPE.
    ActionResult action = new ActionResult();
    action.buildResponse(transactionResult("transactionCode", "TC"));
    JSONObject json = new JSONObject(JsonResults.toJson(action));
    assertEquals(0, json.getInt("result"));
    assertEquals(0, json.getInt("paymentType"));
    assertEquals(0, json.getInt("originalAmount"));
    assertFalse(json.has("installments"));
  }

  @Test
  public void getBuildResponseSinalizaUmaUnicaVez() throws Exception {
    ActionResult action = new ActionResult();
    assertEquals(0, action.getBuildResponse());
    action.buildResponse(transactionResult("result", 0));
    assertEquals(1, action.getBuildResponse());
    assertEquals(0, action.getBuildResponse());
  }

  @Test
  public void intOrZero() {
    assertEquals(0, ActionResult.intOrZero(null));
    assertEquals(7, ActionResult.intOrZero(7));
  }
}
