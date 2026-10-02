package br.com.jylabtech.pagseguro_payment_tech.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;

/**
 * Garante que todo método de negócio do canal completa o result exatamente uma vez, com o tipo
 * esperado pelo Dart, e chama a operação nativa correspondente.
 */
public class PaymentMethodRouterTest {

  private FakeOperations ops;

  /** Registra as operações chamadas; pode lançar para simular falha do SDK. */
  static final class FakeOperations implements PaymentMethodRouter.Operations {
    final List<String> calls = new ArrayList<>();
    Exception toThrow;
    String serial = "SERIAL-123";
    List<Map<String, Object>> installments = new ArrayList<>();

    private void record(String call) throws Exception {
      calls.add(call);
      if (toThrow != null) {
        throw toThrow;
      }
    }

    @Override public void setDialogStyle(MethodCall call) throws Exception { record("setDialogStyle"); }
    @Override public void setMaxTimeShowPopup(int max) throws Exception { record("setMaxTimeShowPopup:" + max); }
    @Override public void printerFromFile(String path) throws Exception { record("printerFromFile:" + path); }
    @Override public void printFile(String fileName) throws Exception { record("printFile:" + fileName); }
    @Override public void printer(String filePath) throws Exception { record("printer:" + filePath); }
    @Override public void printerByFilePath(String filePath) throws Exception { record("printerByFilePath:" + filePath); }
    @Override public void rebootDevice() throws Exception { record("rebootDevice"); }
    @Override public void beep() throws Exception { record("beep"); }
    @Override public String getSerialNumber() throws Exception { record("getSerialNumber"); return serial; }
    @Override public void getUserData() throws Exception { record("getUserData"); }

    @Override
    public List<Map<String, Object>> calculateInstallments(String saleValue, int installmentType) throws Exception {
      record("calculateInstallments:" + saleValue + ":" + installmentType);
      return installments;
    }

    @Override
    public void debitPayment(int value, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("debitPayment:" + value + ":" + ref + ":" + print + ":" + partial + ":" + carne);
    }

    @Override public void activatePinpad(String code) throws Exception { record("activatePinpad:" + code); }
    @Override public void isAuthenticated() throws Exception { record("isAuthenticated"); }

    @Override
    public void pixPayment(int value, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("pixPayment:" + value);
    }

    @Override
    public void creditPayment(int value, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("creditPayment:" + value);
    }

    @Override
    public void creditPaymentParc(int value, int type, int parc, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("creditPaymentParc:" + value + ":" + type + ":" + parc);
    }

    @Override
    public void voucherPayment(int value, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("voucherPayment:" + value);
    }

    @Override
    public void startPayment(int type, int amount, int installmentType, int installments, String ref, Boolean print, Boolean partial, Boolean carne) throws Exception {
      record("startPayment:" + type + ":" + amount + ":" + installmentType + ":" + installments);
    }

    @Override public void abortTransaction() throws Exception { record("abortTransaction"); }
    @Override public void lastTransaction() throws Exception { record("lastTransaction"); }
    @Override public void refund(String code, String id) throws Exception { record("refund:" + code + ":" + id); }
    @Override public void readNfc(String idEvento) throws Exception { record("readNfc:" + idEvento); }

    @Override
    public void writeNfc(String valor, String nome, String cpf, String tag, String cel, String aberto, String idEvento) throws Exception {
      record("writeNfc:" + valor + ":" + nome + ":" + cpf + ":" + tag + ":" + cel + ":" + aberto + ":" + idEvento);
    }

    @Override public void reWriteNfc(String valor, String idEvento) throws Exception { record("reWriteNfc:" + valor + ":" + idEvento); }
    @Override public void refundNfc(String valor, String idEvento) throws Exception { record("refundNfc:" + valor + ":" + idEvento); }
    @Override public void formatNfc() throws Exception { record("formatNfc"); }
    @Override public void debitNfc(String idEvento, String valor) throws Exception { record("debitNfc:" + idEvento + ":" + valor); }
  }

  @Before
  public void setUp() {
    ops = new FakeOperations();
  }

  private static Map<String, Object> args(Object... keyValues) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  private static Map<String, Object> paymentArgs(Object... extra) {
    Map<String, Object> map = args("value", 1500, "userReference", "REF", "printReceipt", true,
        "partialPay", false, "isCarne", false);
    map.putAll(args(extra));
    return map;
  }

  private RecordingResult dispatch(String method, Object arguments) {
    RecordingResult result = new RecordingResult();
    PaymentMethodRouter.dispatch(new MethodCall(method, arguments), result, ops);
    return result;
  }

  /** Verifica resposta única {@code success(true)} e a operação chamada. */
  private void assertRespondsTrueOnce(String method, Object arguments, String expectedCall) {
    ops.calls.clear();
    RecordingResult result = dispatch(method, arguments);
    assertEquals(method, Collections.singletonList("success"), result.replies);
    assertEquals(method, true, result.successValue);
    assertEquals(method, Collections.singletonList(expectedCall), ops.calls);
  }

  @Test
  public void paymentDebitCompletaOResultComTrue() {
    assertRespondsTrueOnce("paymentDebit", paymentArgs(), "debitPayment:1500:REF:true:false:false");
  }

  @Test
  public void metodosNfcCompletamOResultComTrue() {
    assertRespondsTrueOnce("paymentReadNfc", args("idEvento", "7"), "readNfc:7");
    assertRespondsTrueOnce("paymentWriteNfc",
        args("valor", "10", "nome", "Ana", "cpf", "123", "numeroTag", "T1", "celular", "999", "aberto", "1", "idEvento", "7"),
        "writeNfc:10:Ana:123:T1:999:1:7");
    assertRespondsTrueOnce("paymentReWriteNfc", args("valor", "10", "idEvento", "7"), "reWriteNfc:10:7");
    assertRespondsTrueOnce("paymentReFundNfc", args("valor", "10", "idEvento", "7"), "refundNfc:10:7");
    assertRespondsTrueOnce("paymentFormatNfc", null, "formatNfc");
    assertRespondsTrueOnce("paymentDebitNfc", args("idEvento", "7", "valor", "10"), "debitNfc:7:10");
  }

  @Test
  public void argumentosNfcNumericosSaoConvertidosParaString() {
    // No Dart os parâmetros NFC são dynamic (ex.: nfc.readNfc(1)).
    assertRespondsTrueOnce("paymentReadNfc", args("idEvento", 1), "readNfc:1");
    assertRespondsTrueOnce("paymentDebitNfc", args("idEvento", 1, "valor", 500), "debitNfc:1:500");
    assertRespondsTrueOnce("paymentWriteNfc",
        args("valor", 1, "nome", "n", "cpf", "c", "numeroTag", "t", "celular", "cel", "aberto", true, "idEvento", 1),
        "writeNfc:1:n:c:t:cel:true:1");
  }

  @Test
  public void metodosDeImpressaoChamamAImpressoraERespondemTrue() {
    assertRespondsTrueOnce("paymentPrinterFile", args("path", "/a.png"), "printerFromFile:/a.png");
    assertRespondsTrueOnce("paymentPrinter", args("path", "a.png"), "printFile:a.png");
    assertRespondsTrueOnce("paymentPrinterBasic", args("path", "/b.png"), "printer:/b.png");
    assertRespondsTrueOnce("paymentPrinterFilePath", args("path", "/c.png"), "printerByFilePath:/c.png");
  }

  @Test
  public void demaisMetodosBooleanosRespondemTrueUmaVez() {
    assertRespondsTrueOnce("paymentCredit", paymentArgs(), "creditPayment:1500");
    assertRespondsTrueOnce("paymentCreditParc", paymentArgs("type", 3, "parc", 2), "creditPaymentParc:1500:3:2");
    assertRespondsTrueOnce("paymentVoucher", paymentArgs(), "voucherPayment:1500");
    assertRespondsTrueOnce("paymentPix", paymentArgs(), "pixPayment:1500");
    assertRespondsTrueOnce("startPayment",
        args("type", 1, "amount", 2000, "installmentType", 1, "installments", 1, "userReference", "REF",
            "printReceipt", true, "partialPay", false, "isCarne", false),
        "startPayment:1:2000:1:1");
    assertRespondsTrueOnce("paymentAbort", null, "abortTransaction");
    assertRespondsTrueOnce("paymentLastTransaction", null, "lastTransaction");
    assertRespondsTrueOnce("paymentRefund", args("transactionCode", "C", "transactionId", "I"), "refund:C:I");
    assertRespondsTrueOnce("paymentActivePinpad", args("code", "403938"), "activatePinpad:403938");
    assertRespondsTrueOnce("paymentIsAuthenticated", null, "isAuthenticated");
    assertRespondsTrueOnce("paymentReboot", null, "rebootDevice");
    assertRespondsTrueOnce("paymentBeep", null, "beep");
    assertRespondsTrueOnce("paymentGetUserData", null, "getUserData");
    assertRespondsTrueOnce("paymentStyleDialog", args("maxTimeShowPopUp", 30), "setDialogStyle");
    assertRespondsTrueOnce("paymentSetPrintDialogMaxTime", args("maxTimeShowPopUp", 45), "setMaxTimeShowPopup:45");
    assertRespondsTrueOnce("paymentSetPrintDialogMaxTime", null, "setMaxTimeShowPopup:30");
  }

  @Test
  public void getSerialNumberRespondeOSerialUmaVez() {
    RecordingResult result = dispatch("paymentGetSerialNumber", null);
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals("SERIAL-123", result.successValue);
  }

  @Test
  public void calculateInstallmentsRespondeAListaUmaVez() {
    Map<String, Object> item = args("quantity", 1, "amount", 1000, "total", 1000);
    ops.installments.add(item);
    RecordingResult result = dispatch("paymentCalculateInstallments", args("saleValue", "1000", "installmentType", 1));
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals(ops.installments, result.successValue);
    assertEquals(Collections.singletonList("calculateInstallments:1000:1"), ops.calls);
  }

  @Test
  public void falhaDaOperacaoViraUmUnicoErro() {
    ops.toThrow = new IllegalStateException("terminal ocupado");
    for (String method : new String[] {"paymentDebit", "paymentReadNfc", "paymentPrinterBasic", "paymentGetSerialNumber"}) {
      RecordingResult result = dispatch(method, paymentArgs("idEvento", "7", "path", "/x.png"));
      assertEquals(method, Collections.singletonList("error"), result.replies);
      assertEquals(PaymentMethodRouter.ERROR_CODE, result.errorCode);
      assertEquals("terminal ocupado", result.errorMessage);
    }
  }

  @Test
  public void argumentoInvalidoViraErroSemChamarOSdk() {
    RecordingResult result = dispatch("paymentDebit", args("userReference", "REF"));
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals(Collections.emptyList(), ops.calls);
  }

  @Test
  public void metodoDesconhecidoRespondeNotImplementedUmaVez() {
    RecordingResult result = dispatch("paymentInexistente", null);
    assertEquals(Collections.singletonList("notImplemented"), result.replies);
    assertNull(result.successValue);
    assertEquals(Collections.emptyList(), ops.calls);
  }
}
