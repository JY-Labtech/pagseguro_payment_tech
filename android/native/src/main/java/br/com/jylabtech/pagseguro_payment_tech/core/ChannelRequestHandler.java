package br.com.jylabtech.pagseguro_payment_tech.core;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * Lógica do {@code onMethodCall} do plugin, sem dependência de Android (testada na JVM).
 *
 * <ul>
 *   <li>{@code initPayment}: aplica o tempo do dialog de impressão e responde {@code true};</li>
 *   <li>{@code paymentSetPrintDialogMaxTime}: configuração local do dialog de impressão;</li>
 *   <li>{@code payment*} e {@code startPayment}: executados direto pelo {@link Facade}
 *   (main thread, a mesma do MethodChannel);</li>
 *   <li>qualquer outro método: {@code notImplemented}.</li>
 * </ul>
 * Toda chamada é completada exatamente uma vez ({@link SingleReplyResult}).
 */
public final class ChannelRequestHandler {
  public static final String INIT_PAYMENT_METHOD = "initPayment";
  public static final String SET_PRINT_DIALOG_MAX_TIME_METHOD = "paymentSetPrintDialogMaxTime";
  public static final String START_PAYMENT_METHOD = "startPayment";
  public static final String ERROR_CODE = "ERROR";
  /** Plugin desanexado da engine (ou ainda não anexado). */
  public static final String NOT_INITIALIZED_CODE = "NOT_INITIALIZED";
  static final int DEFAULT_MAX_TIME_SHOW_POPUP = 30;

  /** Operações do plugin usadas pelo handler (implementadas por {@link PagSeguroPayment}). */
  public interface Facade {
    void setMaxTimeShowPopup(int maxTimeShowPopup);

    /** Executa um método de negócio (main thread). */
    void handleMethodCall(MethodCall call, Result result) throws Exception;
  }

  private volatile Facade facade;

  public ChannelRequestHandler(Facade facade) {
    this.facade = facade;
  }

  /** Finaliza o handler: chamadas futuras recebem {@code NOT_INITIALIZED}. */
  public void dispose() {
    facade = null;
  }

  public void onMethodCall(MethodCall call, Result rawResult) {
    final Result result = SingleReplyResult.wrap(rawResult);
    try {
      final Facade current = facade;
      if (INIT_PAYMENT_METHOD.equals(call.method)) {
        if (current == null) {
          result.error(NOT_INITIALIZED_CODE, "Plugin não inicializado.", null);
          return;
        }
        Integer maxTimeShowPopUp = call.argument("maxTimeShowPopUp");
        if (maxTimeShowPopUp != null) {
          current.setMaxTimeShowPopup(maxTimeShowPopUp);
        }
        result.success(true);
        return;
      }

      if (SET_PRINT_DIALOG_MAX_TIME_METHOD.equals(call.method)) {
        if (current == null) {
          result.error(NOT_INITIALIZED_CODE, "Plugin não inicializado.", null);
          return;
        }
        Integer maxTimeShowPopUp = call.argument("maxTimeShowPopUp");
        current.setMaxTimeShowPopup(maxTimeShowPopUp != null ? maxTimeShowPopUp : DEFAULT_MAX_TIME_SHOW_POPUP);
        result.success(true);
        return;
      }

      if (isBusinessMethod(call.method)) {
        if (current == null) {
          result.error(NOT_INITIALIZED_CODE, "Plugin não inicializado.", null);
          return;
        }
        current.handleMethodCall(call, result);
      } else {
        result.notImplemented();
      }
    } catch (Exception exception) {
      result.error(ERROR_CODE, exception.getMessage(), null);
    }
  }

  static boolean isBusinessMethod(String method) {
    return method != null && (method.startsWith("payment") || START_PAYMENT_METHOD.equals(method));
  }
}
