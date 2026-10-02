package br.com.jylabtech.pagseguro_payment_tech.core;

import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceConfig;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceResult;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * Lógica do {@code onMethodCall} do plugin, sem dependência de Android (testada na JVM).
 *
 * <ul>
 *   <li>{@code initPayment}: guarda as chaves e valida a licença; responde {@code true} ou
 *   {@code LICENCE_ERROR};</li>
 *   <li>{@code paymentSetPrintDialogMaxTime}: configuração local, fora do gate de licença;</li>
 *   <li>{@code payment*} e {@code startPayment}: passam pelo gate de licença (validação em
 *   background) e, só com licença válida, são executados na main thread pelo {@link Facade};</li>
 *   <li>qualquer outro método: {@code notImplemented}.</li>
 * </ul>
 * Toda chamada é completada exatamente uma vez ({@link SingleReplyResult}).
 */
public final class ChannelRequestHandler {
  public static final String INIT_PAYMENT_METHOD = "initPayment";
  public static final String SET_PRINT_DIALOG_MAX_TIME_METHOD = "paymentSetPrintDialogMaxTime";
  public static final String START_PAYMENT_METHOD = "startPayment";
  public static final String ERROR_CODE = "ERROR";
  static final int DEFAULT_MAX_TIME_SHOW_POPUP = 30;

  /** Operações do plugin usadas pelo handler (implementadas por {@link PagSeguroPayment}). */
  public interface Facade {
    void setMaxTimeShowPopup(int maxTimeShowPopup);

    void updateLicenceKeys(String licenceKey, String licenceInternalKey);

    /** Valida a licença; pode fazer rede (chamado em background). */
    LicenceResult checkLicence();

    void notifyLicenceChecking();

    void notifyLicenceResult(LicenceResult licenceResult);

    /** Executa um método de negócio já liberado pelo gate (main thread). */
    void handleMethodCall(MethodCall call, Result result) throws Exception;
  }

  /** Ação executada na main thread depois que a licença foi validada. */
  private interface LicensedAction {
    void run() throws Exception;
  }

  private volatile Facade facade;
  private final AsyncExecutor licenceExecutor;

  public ChannelRequestHandler(Facade facade, AsyncExecutor licenceExecutor) {
    this.facade = facade;
    this.licenceExecutor = licenceExecutor;
  }

  /** Finaliza o handler: chamadas pendentes e futuras recebem LICENCE_ERROR. */
  public void dispose() {
    facade = null;
  }

  public void onMethodCall(MethodCall call, Result rawResult) {
    final Result result = SingleReplyResult.wrap(rawResult);
    try {
      final Facade current = facade;
      if (INIT_PAYMENT_METHOD.equals(call.method)) {
        if (current == null) {
          result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin não inicializado.", null);
          return;
        }
        Integer maxTimeShowPopUp = call.argument("maxTimeShowPopUp");
        if (maxTimeShowPopUp != null) {
          current.setMaxTimeShowPopup(maxTimeShowPopUp);
        }
        String licenceKey = call.argument("licenceKey");
        String licenceInternalKey = call.argument("licenceInternalKey");
        current.updateLicenceKeys(licenceKey, licenceInternalKey);
        runWithLicence(result, () -> result.success(true));
        return;
      }

      // Configuração visual local (não aciona o SDK de pagamento): fora do gate de propósito.
      if (SET_PRINT_DIALOG_MAX_TIME_METHOD.equals(call.method)) {
        if (current == null) {
          result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin não inicializado.", null);
          return;
        }
        Integer maxTimeShowPopUp = call.argument("maxTimeShowPopUp");
        current.setMaxTimeShowPopup(maxTimeShowPopUp != null ? maxTimeShowPopUp : DEFAULT_MAX_TIME_SHOW_POPUP);
        result.success(true);
        return;
      }

      if (isBusinessMethod(call.method)) {
        runWithLicence(result, () -> {
          Facade active = facade;
          if (active == null) {
            result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin finalizado.", null);
            return;
          }
          active.handleMethodCall(call, result);
        });
      } else {
        result.notImplemented();
      }
    } catch (Exception exception) {
      result.error(ERROR_CODE, exception.getMessage(), null);
    }
  }

  /** Mensagem do LICENCE_ERROR: a do resultado ou "Licença não ativada". */
  public static String licenceErrorMessage(LicenceResult licenceResult) {
    String errorMessage = licenceResult != null ? licenceResult.getErrorMessage() : null;
    if (errorMessage == null || errorMessage.isEmpty()) {
      errorMessage = "Licença não ativada";
    }
    return errorMessage;
  }

  static boolean isBusinessMethod(String method) {
    return method != null && (method.startsWith("payment") || START_PAYMENT_METHOD.equals(method));
  }

  /** Valida a licença em background e executa a ação na main thread somente se ela for válida. */
  private void runWithLicence(final Result result, final LicensedAction action) {
    final Facade payment = facade;
    final AsyncExecutor executor = licenceExecutor;
    if (payment == null || executor == null || executor.isShutdown()) {
      result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin não inicializado.", null);
      return;
    }
    payment.notifyLicenceChecking();
    try {
      executor.background(() -> {
        LicenceResult licenceResult;
        try {
          licenceResult = payment.checkLicence();
        } catch (Exception e) {
          licenceResult = LicenceResult.error("Não foi possível validar a licença: " + e.getMessage());
        }
        final LicenceResult finalResult = licenceResult;
        executor.main(() -> {
          if (facade != payment) {
            result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin finalizado.", null);
            return;
          }
          payment.notifyLicenceResult(finalResult);
          if (finalResult == null || !finalResult.isSuccess()) {
            result.error(LicenceConfig.LICENCE_ERROR_CODE, licenceErrorMessage(finalResult), null);
            return;
          }
          try {
            action.run();
          } catch (Exception exception) {
            result.error(ERROR_CODE, exception.getMessage(), null);
          }
        });
      });
    } catch (Exception rejected) {
      result.error(LicenceConfig.LICENCE_ERROR_CODE, "Não foi possível validar a licença.", null);
    }
  }
}
