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

import br.com.jylabtech.pagseguro_payment_tech.testing.RecordingResult;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Roteamento do canal: todo método de negócio chega ao SDK (facade) direto, cada chamada recebe
 * exatamente uma resposta e métodos desconhecidos viram notImplemented.
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
      "paymentStyleDialog", "paymentCalculateInstallments", "paymentReadNfcUid");

  private FakeFacade facade;
  private ChannelRequestHandler handler;

  static final class FakeFacade implements ChannelRequestHandler.Facade {
    final List<String> calls = new ArrayList<>();
    Exception handleThrows;
    Integer maxTime;

    @Override
    public void setMaxTimeShowPopup(int maxTimeShowPopup) {
      maxTime = maxTimeShowPopup;
      calls.add("setMaxTimeShowPopup:" + maxTimeShowPopup);
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
    handler = new ChannelRequestHandler(facade);
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
  public void initPaymentAplicaOTempoERespondeTrue() {
    RecordingResult result = call("initPayment", args("maxTimeShowPopUp", 45));

    assertEquals(Integer.valueOf(45), facade.maxTime);
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals(true, result.successValue);
    assertFalse(facade.sdkCalled());
  }

  @Test
  public void initPaymentSemMaxTimeNaoAlteraOTempo() {
    RecordingResult result = call("initPayment", args());
    assertEquals(null, facade.maxTime);
    assertEquals(true, result.successValue);
  }

  @Test
  public void todoMetodoDeNegocioChegaAoSdkUmaVez() {
    for (String method : BUSINESS_METHODS) {
      facade.calls.clear();
      RecordingResult result = call(method, args());
      assertEquals(method, Collections.singletonList("handle:" + method), facade.calls);
      assertEquals(method, Collections.singletonList("success"), result.replies);
      assertEquals(method, "ok:" + method, result.successValue);
    }
  }

  @Test
  public void excecaoAoExecutarViraErrorUnico() {
    facade.handleThrows = new IllegalStateException("terminal ocupado");
    RecordingResult result = call("paymentDebit", args());
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("ERROR", result.errorCode);
    assertEquals("terminal ocupado", result.errorMessage);
  }

  @Test
  public void setPrintDialogMaxTimeEConfiguracaoLocal() {
    RecordingResult result = call("paymentSetPrintDialogMaxTime", args("maxTimeShowPopUp", 60));
    assertEquals(Collections.singletonList("success"), result.replies);
    assertEquals(true, result.successValue);
    assertEquals(Collections.singletonList("setMaxTimeShowPopup:60"), facade.calls);

    RecordingResult semValor = call("paymentSetPrintDialogMaxTime", null);
    assertEquals(Collections.singletonList("success"), semValor.replies);
    assertEquals(Integer.valueOf(30), facade.maxTime);
  }

  @Test
  public void metodoDesconhecidoRespondeNotImplemented() {
    for (String method : new String[] {"getPlatformVersion", "foo", "initPaymentX"}) {
      facade.calls.clear();
      RecordingResult result = call(method, null);
      assertEquals(method, Collections.singletonList("notImplemented"), result.replies);
      assertTrue(method, facade.calls.isEmpty());
    }
  }

  @Test
  public void argumentoComTipoErradoViraErrorSemCrash() {
    RecordingResult result = call("initPayment", args("maxTimeShowPopUp", "45"));
    assertEquals(Collections.singletonList("error"), result.replies);
    assertEquals("ERROR", result.errorCode);
  }

  @Test
  public void handlerFinalizadoRecusaNovasChamadas() {
    handler.dispose();
    for (String method : new String[] {"initPayment", "paymentDebit", "paymentSetPrintDialogMaxTime"}) {
      RecordingResult result = call(method, args());
      assertEquals(method, Collections.singletonList("error"), result.replies);
      assertEquals(method, "NOT_INITIALIZED", result.errorCode);
    }
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
