package br.com.jylabtech.pagseguro_payment_tech.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.licence.CheckLicenceEntity;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceResult;
import br.com.jylabtech.pagseguro_payment_tech.testing.QueuedAsyncExecutor;
import br.com.jylabtech.pagseguro_payment_tech.testing.RecordingResult;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Gate de licença do canal: todo método de negócio só chega ao SDK com licença válida, cada
 * chamada recebe exatamente uma resposta e métodos desconhecidos viram notImplemented.
 */
public class ChannelRequestHandlerTest {

  /** Lista de todos os métodos de negócio enviados pelo Dart (PaymentTypeCall.method). */
  static final List<String> BUSINESS_METHODS = Arrays.asList(
      "paymentCredit", "paymentCreditParc", "paymentDebit", "paymentPix", "paymentVoucher",
      "paymentAbort", "paymentLastTransaction", "paymentRefund", "paymentActivePinpad",
      "paymentIsAuthenticated", "paymentReadNfc", "paymentWriteNfc", "paymentReWriteNfc",
      "paymentReFundNfc", "paymentDebitNfc", "paymentFormatNfc", "paymentPrinterFile",
      "startPayment", "paymentPrinter", "paymentPrinterBasic", "paymentPrinterFilePath",
      "paymentReboot", "paymentBeep", "paymentGetSerialNumber", "paymentGetUserData",
      "paymentStyleDialog", "paymentCalculateInstallments");

  private FakeFacade facade;
  private QueuedAsyncExecutor executor;
  private ChannelRequestHandler handler;

  static final class FakeFacade implements ChannelRequestHandler.Facade {
    final List<String> calls = new ArrayList<>();
    LicenceResult licence = LicenceResult.success(new CheckLicenceEntity());
    RuntimeException checkThrows;
    Exception handleThrows;
    String licenceKey;
    String licenceInternalKey;
    Integer maxTime;

    @Override
    public void setMaxTimeShowPopup(int maxTimeShowPopup) {
      maxTime = maxTimeShowPopup;
      calls.add("setMaxTimeShowPopup:" + maxTimeShowPopup);
    }

    @Override
    public void updateLicenceKeys(String licenceKey, String licenceInternalKey) {
      this.licenceKey = licenceKey;
      this.licenceInternalKey = licenceInternalKey;
      calls.add("updateLicenceKeys");
    }

    @Override
    public LicenceResult checkLicence() {
      calls.add("checkLicence");
      if (checkThrows != null) {
        throw checkThrows;
      }
      return licence;
    }

    @Override
    public void notifyLicenceChecking() {
      calls.add("notifyLicenceChecking");
    }

    @Override
    public void notifyLicenceResult(LicenceResult licenceResult) {
      calls.add("notifyLicenceResult:" + (licenceResult != null && licenceResult.isSuccess()));
    }

    @Override
    public void handleMethodCall(MethodCall call, MethodChannel.Result result) throws Exception {
      calls.add("handle:" + call.method);
      if (handleThrows != null) {
        throw handleThrows;
      }
      result.success("ok:" + call.method);
    }

    boolean sdkCalled() {
      for (String call : calls) {
        if (call.startsWith("handle:")) {
          return true;
        }
      }
      return false;
    }
  }

  @Before
  public void setUp() {
    facade = new FakeFacade();
    executor = new QueuedAsyncExecutor();
    handler = new ChannelRequestHandler(facade, executor);
  }

  private static Map<String, Object> args(Object... keyValues) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  private RecordingResult call(String method, Object arguments) {
    RecordingResult result = new RecordingResult();
    handler.onMethodCall(new MethodCall(method, arguments), result);
    return result;
  }

  @Test
  public void initPaymentGuardaChavesTempoEValidaLicencaEmBackground() {
    RecordingResult result = call("initPayment",
        args("licenceKey", "KEY", "licenceInternalKey", "URL", "maxTimeShowPopUp", 45));

    assertEquals("KEY", facade.licenceKey);
    assertEquals("URL", facade.licenceInternalKey);
    assertEquals(Integer.valueOf(45), facade.maxTime);
    // Nada respondido antes de a validação (background) terminar.
    assertTrue(result.replies.isEmpty());
    assertFalse(facade.calls.contains("checkLicence"));

    executor.runBackground();
    assertTrue(facade.calls.contains("checkLicence"));
    assertTrue(result.replies.isEmpty());

    executor.runMain();
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals(true, result.successValue);
    assertTrue(facade.calls.contains("notifyLicenceResult:true"));
  }

  @Test
  public void initPaymentSemMaxTimeNaoAlteraOTempo() {
    call("initPayment", args("licenceKey", "KEY"));
    assertEquals(null, facade.maxTime);
  }

  @Test
  public void initPaymentComLicencaInvalidaDevolveLicenceError() {
    facade.licence = LicenceResult.error("Licença vencida");
    RecordingResult result = call("initPayment", args("licenceKey", "KEY"));
    executor.runAll();

    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("LICENCE_ERROR", result.errorCode);
    assertEquals("Licença vencida", result.errorMessage);
    assertTrue(facade.calls.contains("notifyLicenceResult:false"));
  }

  @Test
  public void todoMetodoDeNegocioComLicencaValidaChegaAoSdkUmaVez() {
    for (String method : BUSINESS_METHODS) {
      facade.calls.clear();
      RecordingResult result = call(method, args());
      assertFalse(method, facade.sdkCalled());
      executor.runAll();
      assertEquals(method, Arrays.asList("notifyLicenceChecking", "checkLicence", "notifyLicenceResult:true", "handle:" + method), facade.calls);
      assertEquals(method, Collections.singletonList("success"), result.replies);
      assertEquals(method, "ok:" + method, result.successValue);
    }
  }

  @Test
  public void todoMetodoDeNegocioComLicencaInvalidaDevolveLicenceErrorSemChamarOSdk() {
    facade.licence = LicenceResult.error("Licença cancelada");
    for (String method : BUSINESS_METHODS) {
      facade.calls.clear();
      RecordingResult result = call(method, args());
      executor.runAll();
      assertFalse(method, facade.sdkCalled());
      assertEquals(method, Collections.singletonList("error"), result.replies);
      assertEquals(method, "LICENCE_ERROR", result.errorCode);
      assertEquals(method, "Licença cancelada", result.errorMessage);
    }
  }

  @Test
  public void licencaNulaOuSemMensagemViraLicencaNaoAtivada() {
    facade.licence = null;
    RecordingResult result = call("paymentDebit", args());
    executor.runAll();
    assertEquals("LICENCE_ERROR", result.errorCode);
    assertEquals("Licença não ativada", result.errorMessage);
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void excecaoNaValidacaoViraLicenceError() {
    facade.checkThrows = new IllegalStateException("sem rede");
    RecordingResult result = call("paymentCredit", args());
    executor.runAll();
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("LICENCE_ERROR", result.errorCode);
    assertEquals("Não foi possível validar a licença: sem rede", result.errorMessage);
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void excecaoAoExecutarViraErrorUnico() {
    facade.handleThrows = new IllegalStateException("terminal ocupado");
    RecordingResult result = call("paymentDebit", args());
    executor.runAll();
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("ERROR", result.errorCode);
    assertEquals("terminal ocupado", result.errorMessage);
  }

  @Test
  public void setPrintDialogMaxTimeFicaForaDoGate() {
    facade.licence = LicenceResult.error("Licença vencida");
    RecordingResult result = call("paymentSetPrintDialogMaxTime", args("maxTimeShowPopUp", 60));
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals(true, result.successValue);
    assertEquals(Collections.singletonList("setMaxTimeShowPopup:60"), facade.calls);
    assertTrue(executor.backgroundTasks.isEmpty());

    RecordingResult semValor = call("paymentSetPrintDialogMaxTime", null);
    assertEquals(Collections.singletonList("success"), semValor.replies);
    assertEquals(Integer.valueOf(30), facade.maxTime);
  }

  @Test
  public void metodoDesconhecidoRespondeNotImplementedSemValidarLicenca() {
    for (String method : new String[] {"getPlatformVersion", "foo", "initPaymentX"}) {
      facade.calls.clear();
      RecordingResult result = call(method, null);
      assertEquals(method, Collections.singletonList("notImplemented"), result.replies);
      assertTrue(method, facade.calls.isEmpty());
    }
  }

  @Test
  public void argumentoComTipoErradoViraErrorSemCrash() {
    RecordingResult result = call("initPayment", args("licenceKey", 123));
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("ERROR", result.errorCode);
  }

  @Test
  public void handlerFinalizadoAntesDaRespostaDevolvePluginFinalizado() {
    RecordingResult result = call("paymentDebit", args());
    executor.runBackground();
    handler.dispose();
    executor.runMain();
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("LICENCE_ERROR", result.errorCode);
    assertEquals("Plugin finalizado.", result.errorMessage);
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void handlerFinalizadoRecusaNovasChamadas() {
    handler.dispose();
    for (String method : new String[] {"initPayment", "paymentDebit", "paymentSetPrintDialogMaxTime"}) {
      RecordingResult result = call(method, args("licenceKey", "KEY"));
      assertEquals(method, Collections.singletonList("error"), result.replies);
      assertEquals(method, "LICENCE_ERROR", result.errorCode);
    }
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void executorFinalizadoDevolveLicenceError() {
    executor.shutdown = true;
    RecordingResult result = call("paymentDebit", args());
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("LICENCE_ERROR", result.errorCode);
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void isBusinessMethodReconhecePrefixoEStartPayment() {
    assertTrue(ChannelRequestHandler.isBusinessMethod("paymentDebit"));
    assertTrue(ChannelRequestHandler.isBusinessMethod("startPayment"));
    assertFalse(ChannelRequestHandler.isBusinessMethod("initPayment"));
    assertFalse(ChannelRequestHandler.isBusinessMethod(null));
  }
}
