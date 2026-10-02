package br.com.jylabtech.pagseguro_payment_tech.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.testing.QueuedAsyncExecutor;
import io.flutter.plugin.common.MethodCall;

/**
 * Cobertura completa do roteador: TODO método do canal tem um caso de sucesso (operação certa,
 * argumentos certos, resposta única) e um de erro (falha do SDK vira um único ERROR). Também
 * cobre a execução em background das chamadas bloqueantes.
 */
public class PaymentMethodRouterCoverageTest {

  private PaymentMethodRouterTest.FakeOperations ops;
  private QueuedAsyncExecutor async;

  @Before
  public void setUp() {
    ops = new PaymentMethodRouterTest.FakeOperations();
    async = new QueuedAsyncExecutor();
  }

  private static Map<String, Object> args(Object... keyValues) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  private static Map<String, Object> paymentArgs(Object... extra) {
    Map<String, Object> map = args("value", 2590, "userReference", "PEDIDO1", "printReceipt", false,
        "partialPay", true, "isCarne", true);
    map.putAll(args(extra));
    return map;
  }

  /** método -> argumentos realistas enviados pelo Dart. */
  private static Map<String, Map<String, Object>> allMethods() {
    Map<String, Map<String, Object>> methods = new LinkedHashMap<>();
    methods.put("paymentCredit", paymentArgs());
    methods.put("paymentCreditParc", paymentArgs("type", 2, "parc", 6));
    methods.put("paymentDebit", paymentArgs());
    methods.put("paymentPix", paymentArgs());
    methods.put("paymentVoucher", paymentArgs());
    methods.put("startPayment", args("type", 2, "amount", 990, "installmentType", 3, "installments", 4,
        "userReference", "REF", "printReceipt", true, "partialPay", false, "isCarne", false));
    methods.put("paymentAbort", null);
    methods.put("paymentLastTransaction", null);
    methods.put("paymentRefund", args("transactionCode", "TC", "transactionId", "TI"));
    methods.put("paymentActivePinpad", args("code", "403938"));
    methods.put("paymentIsAuthenticated", null);
    methods.put("paymentReadNfc", args("idEvento", "7"));
    methods.put("paymentWriteNfc", args("valor", "10", "nome", "Ana", "cpf", "1", "numeroTag", "T", "celular", "9", "aberto", "1", "idEvento", "7"));
    methods.put("paymentReWriteNfc", args("valor", "10", "idEvento", "7"));
    methods.put("paymentReFundNfc", args("valor", "10", "idEvento", "7"));
    methods.put("paymentDebitNfc", args("idEvento", "7", "valor", "10"));
    methods.put("paymentFormatNfc", null);
    methods.put("paymentPrinterFile", args("path", "/a.png"));
    methods.put("paymentPrinter", args("path", "a.png"));
    methods.put("paymentPrinterBasic", args("path", "/b.png"));
    methods.put("paymentPrinterFilePath", args("path", "/c.png"));
    methods.put("paymentReboot", null);
    methods.put("paymentBeep", null);
    methods.put("paymentGetSerialNumber", null);
    methods.put("paymentGetUserData", null);
    methods.put("paymentStyleDialog", args("title", "T", "maxTimeShowPopUp", 30));
    methods.put("paymentCalculateInstallments", args("saleValue", "2000", "installmentType", 3));
    methods.put("paymentSetPrintDialogMaxTime", args("maxTimeShowPopUp", 50));
    return methods;
  }

  private RecordingResult dispatch(String method, Object arguments) {
    RecordingResult result = new RecordingResult();
    PaymentMethodRouter.dispatch(new MethodCall(method, arguments), result, ops, async);
    async.runAll();
    return result;
  }

  @Test
  public void todoMetodoDoCanalRespondeSucessoUmaVezEChamaUmaOperacao() {
    for (Map.Entry<String, Map<String, Object>> entry : allMethods().entrySet()) {
      ops.calls.clear();
      RecordingResult result = dispatch(entry.getKey(), entry.getValue());
      assertEquals(entry.getKey(), Collections.singletonList("success"), result.replies);
      assertEquals(entry.getKey(), 1, ops.calls.size());
    }
  }

  @Test
  public void todoMetodoDoCanalTransformaFalhaDoSdkEmUmUnicoErro() {
    ops.toThrow = new IllegalStateException("falha SDK");
    for (Map.Entry<String, Map<String, Object>> entry : allMethods().entrySet()) {
      ops.calls.clear();
      RecordingResult result = dispatch(entry.getKey(), entry.getValue());
      assertEquals(entry.getKey(), Collections.singletonList("error"), result.replies);
      assertEquals(entry.getKey(), "ERROR", result.errorCode);
      assertEquals(entry.getKey(), "falha SDK", result.errorMessage);
    }
  }

  @Test
  public void argumentosDosPagamentosChegamExatamenteComoEnviados() {
    dispatch("paymentDebit", paymentArgs());
    dispatch("paymentCreditParc", paymentArgs("type", 2, "parc", 6));
    dispatch("startPayment", args("type", 2, "amount", 990, "installmentType", 3, "installments", 4,
        "userReference", "REF", "printReceipt", true, "partialPay", false, "isCarne", false));
    dispatch("paymentCalculateInstallments", args("saleValue", "2000", "installmentType", 3));
    assertEquals(java.util.Arrays.asList(
        "debitPayment:2590:PEDIDO1:false:true:true",
        "creditPaymentParc:2590:2:6",
        "startPayment:2:990:3:4",
        "calculateInstallments:2000:3"), ops.calls);
  }

  @Test
  public void chamadasBloqueantesRodamEmBackgroundERespondemNaMainThread() {
    String[] blocking = {"paymentGetSerialNumber", "paymentGetUserData", "paymentCalculateInstallments",
        "paymentBeep", "paymentReboot", "paymentPrinterFile"};
    for (String method : blocking) {
      ops.calls.clear();
      RecordingResult result = new RecordingResult();
      PaymentMethodRouter.dispatch(new MethodCall(method, args("saleValue", "1", "installmentType", 1, "path", "/x")), result, ops, async);
      // Nada executado na thread que recebeu a chamada (main thread).
      assertTrue(method, ops.calls.isEmpty());
      assertTrue(method, result.replies.isEmpty());
      async.runBackground();
      assertEquals(method, 1, ops.calls.size());
      // A resposta só sai na main thread.
      assertTrue(method, result.replies.isEmpty());
      async.runMain();
      assertEquals(method, Collections.singletonList("success"), result.replies);
    }
  }

  @Test
  public void numeroDeSerieEParcelasVoltamPelaMainThread() {
    RecordingResult serial = dispatch("paymentGetSerialNumber", null);
    assertEquals("SERIAL-123", serial.successValue);

    Map<String, Object> parcela = args("quantity", 2, "amount", 1000, "total", 2000);
    ops.installments.add(parcela);
    RecordingResult parcelas = dispatch("paymentCalculateInstallments", args("saleValue", "2000", "installmentType", 2));
    assertEquals(Collections.singletonList(parcela), parcelas.successValue);
  }

  @Test
  public void falhaEmBackgroundViraErroNaMainThread() {
    ops.toThrow = new IllegalStateException("PlugPagService indisponível");
    RecordingResult result = new RecordingResult();
    PaymentMethodRouter.dispatch(new MethodCall("paymentGetSerialNumber", null), result, ops, async);
    async.runBackground();
    assertTrue(result.replies.isEmpty());
    async.runMain();
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("PlugPagService indisponível", result.errorMessage);
  }

  @Test
  public void executorFinalizadoViraErroUnico() {
    async.shutdown = true;
    RecordingResult result = new RecordingResult();
    PaymentMethodRouter.dispatch(new MethodCall("paymentBeep", null), result, ops, async);
    assertEquals(Collections.singletonList("error"), result.replies);
    assertTrue(ops.calls.isEmpty());
  }

  @Test
  public void calculateInstallmentsSemTipoViraErroSemChamarOSdk() {
    RecordingResult result = dispatch("paymentCalculateInstallments", args("saleValue", "2000"));
    assertEquals(Collections.singletonList("error"), result.replies);
    assertTrue(ops.calls.isEmpty());
  }

  @Test
  public void refundSemCodigoOuIdDevolveInvalidArgumentSemChamarOSdk() {
    Object[][] invalid = {
        {null, "TI"}, {"TC", null}, {"", "TI"}, {"TC", "  "}, {null, null}};
    for (Object[] pair : invalid) {
      ops.calls.clear();
      RecordingResult result = dispatch("paymentRefund", args("transactionCode", pair[0], "transactionId", pair[1]));
      assertEquals(Collections.singletonList("error"), result.replies);
      assertEquals("INVALID_ARGUMENT", result.errorCode);
      assertTrue(ops.calls.isEmpty());
    }
    RecordingResult semArgs = dispatch("paymentRefund", null);
    assertEquals("INVALID_ARGUMENT", semArgs.errorCode);
    assertTrue(ops.calls.isEmpty());
  }

  @Test
  public void asyncNuloUsaExecucaoDireta() {
    RecordingResult result = new RecordingResult();
    PaymentMethodRouter.dispatch(new MethodCall("paymentGetSerialNumber", null), result, ops, null);
    assertEquals(Collections.singletonList("success"), result.replies);
  }
}
