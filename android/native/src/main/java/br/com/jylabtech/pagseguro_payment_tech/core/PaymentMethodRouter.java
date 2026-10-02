package br.com.jylabtech.pagseguro_payment_tech.core;

import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Roteia as chamadas de negócio do MethodChannel (já liberadas pelo gate de licença) para as
 * operações nativas e garante que o {@link MethodChannel.Result} seja completado exatamente uma vez:
 * <ul>
 *   <li>a operação é disparada e o result recebe o valor esperado pelo Dart ({@code true}, o
 *   número de série ou a lista de parcelas). O resultado da transação/impressão/NFC continua
 *   chegando pelos eventos do handler (onMessage, onError, onFinishedResponse, showSuccess...);</li>
 *   <li>exceção ao disparar a operação vira {@code PlatformException("ERROR")};</li>
 *   <li>método desconhecido vira {@code notImplemented()}.</li>
 * </ul>
 * Chamadas síncronas e bloqueantes ao PlugPagService (número de série, dados do usuário,
 * parcelas, beep, reboot e impressão por arquivo) rodam em background pelo {@link AsyncExecutor}
 * e respondem na main thread, para não travar a UI (ANR).
 */
public final class PaymentMethodRouter {

  public static final String PAYMENT_DEBIT = "paymentDebit";
  public static final String PAYMENT_CREDIT = "paymentCredit";
  public static final String PAYMENT_CREDIT_PARC = "paymentCreditParc";
  public static final String PAYMENT_VOUCHER = "paymentVoucher";
  public static final String PAYMENT_ABORT = "paymentAbort";
  public static final String LAST_TRANSACTION = "paymentLastTransaction";
  public static final String REFUND = "paymentRefund";
  public static final String PAYMENT_PIX = "paymentPix";
  public static final String START_PAYMENT = "startPayment";

  public static final String ACTIVE_PINPAD = "paymentActivePinpad";
  public static final String PINPAD_AUTHENTICATED = "paymentIsAuthenticated";
  public static final String REBOOT_DEVICE = "paymentReboot";
  public static final String BEEP_PAYMENT = "paymentBeep";
  public static final String GET_SERIAL_NUMBER = "paymentGetSerialNumber";

  public static final String WRITE_NFC = "paymentWriteNfc";
  public static final String READ_NFC = "paymentReadNfc";
  public static final String FORMAT_NFC = "paymentFormatNfc";
  public static final String REWRITE_NFC = "paymentReWriteNfc";
  public static final String REFUND_NFC = "paymentReFundNfc";
  public static final String DEBIT_NFC = "paymentDebitNfc";

  public static final String PRINTER_FILE = "paymentPrinterFile";
  public static final String PRINTER = "paymentPrinter";
  public static final String PRINTER_BASIC = "paymentPrinterBasic";
  public static final String PRINTER_FILE_PATH = "paymentPrinterFilePath";

  public static final String GET_USER_DATA = "paymentGetUserData";
  public static final String STYLE_DIALOG = "paymentStyleDialog";
  public static final String CALCULATE_INSTALLMENTS = "paymentCalculateInstallments";
  public static final String SET_PRINT_DIALOG_MAX_TIME = "paymentSetPrintDialogMaxTime";

  public static final String ERROR_CODE = "ERROR";
  /** Argumento obrigatório ausente/vazio: o SDK não é chamado. */
  public static final String INVALID_ARGUMENT_CODE = "INVALID_ARGUMENT";

  private static final int DEFAULT_MAX_TIME_SHOW_POPUP = 30;

  /** Operações nativas chamadas pelo roteador (implementadas por {@link PagSeguroPayment}). */
  public interface Operations {
    void setDialogStyle(MethodCall call) throws Exception;

    void setMaxTimeShowPopup(int maxTimeShowPopup) throws Exception;

    void printerFromFile(String path) throws Exception;

    void printFile(String fileName) throws Exception;

    void printer(String filePath) throws Exception;

    void printerByFilePath(String filePath) throws Exception;

    void rebootDevice() throws Exception;

    void beep() throws Exception;

    String getSerialNumber() throws Exception;

    void getUserData() throws Exception;

    List<Map<String, Object>> calculateInstallments(String saleValue, int installmentType) throws Exception;

    void debitPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void activatePinpad(String code) throws Exception;

    void isAuthenticated() throws Exception;

    void pixPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void creditPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void creditPaymentParc(int value, int type, int parc, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void voucherPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void startPayment(int type, int amount, int installmentType, int installments, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception;

    void abortTransaction() throws Exception;

    void lastTransaction() throws Exception;

    void refund(String transactionCode, String transactionId) throws Exception;

    void readNfc(String idEvento) throws Exception;

    void writeNfc(String valor, String nome, String cpf, String numeroTag, String celular, String aberto, String idEvento) throws Exception;

    void reWriteNfc(String valor, String idEvento) throws Exception;

    void refundNfc(String valor, String idEvento) throws Exception;

    void formatNfc() throws Exception;

    void debitNfc(String idEvento, String valor) throws Exception;
  }

  private PaymentMethodRouter() {
  }

  /**
   * Argumentos NFC são {@code dynamic} no Dart: aceita número/bool convertendo para String
   * (o que o presenter espera) em vez de falhar com ClassCastException.
   */
  private static String stringArg(MethodCall call, String key) {
    Object value = call.argument(key);
    return value == null ? null : String.valueOf(value);
  }

  /** Trabalho executado em background que produz o valor da resposta. */
  interface Work {
    Object run() throws Exception;
  }

  static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /** Despacha a chamada executando tudo na thread atual (testes). */
  public static void dispatch(MethodCall call, MethodChannel.Result result, Operations ops) {
    dispatch(call, result, ops, AsyncExecutor.DIRECT);
  }

  /** Despacha a chamada e completa o result exatamente uma vez. */
  public static void dispatch(MethodCall call, MethodChannel.Result result, Operations ops, AsyncExecutor async) {
    final SingleReplyResult reply = SingleReplyResult.wrap(result);
    try {
      if (!route(call, reply, ops, async == null ? AsyncExecutor.DIRECT : async)) {
        reply.notImplemented();
      }
    } catch (Exception exception) {
      reply.error(ERROR_CODE, exception.getMessage(), null);
    }
  }

  /**
   * Executa {@code work} em background e responde na main thread (success com o valor devolvido,
   * ou ERROR com a mensagem da exceção).
   */
  private static void runInBackground(final SingleReplyResult reply, final AsyncExecutor async, final Work work) {
    async.background(() -> {
      Object value;
      try {
        value = work.run();
      } catch (Exception exception) {
        final String message = exception.getMessage();
        async.main(() -> reply.error(ERROR_CODE, message, null));
        return;
      }
      final Object finalValue = value;
      async.main(() -> reply.success(finalValue));
    });
  }

  /** @return {@code false} se o método não é atendido por este roteador. */
  private static boolean route(MethodCall call, SingleReplyResult reply, Operations ops, AsyncExecutor async) throws Exception {
    switch (call.method) {
      case STYLE_DIALOG:
        ops.setDialogStyle(call);
        reply.success(true);
        return true;
      case SET_PRINT_DIALOG_MAX_TIME: {
        Integer maxTimeShowPopUp = call.argument("maxTimeShowPopUp");
        ops.setMaxTimeShowPopup(maxTimeShowPopUp != null ? maxTimeShowPopUp : DEFAULT_MAX_TIME_SHOW_POPUP);
        reply.success(true);
        return true;
      }
      case PRINTER_FILE: {
        final String path = call.argument("path");
        runInBackground(reply, async, () -> {
          ops.printerFromFile(path);
          return true;
        });
        return true;
      }
      case PRINTER:
        ops.printFile(call.argument("path"));
        reply.success(true);
        return true;
      case PRINTER_BASIC:
        ops.printer(call.argument("path"));
        reply.success(true);
        return true;
      case PRINTER_FILE_PATH:
        ops.printerByFilePath(call.argument("path"));
        reply.success(true);
        return true;
      case REBOOT_DEVICE:
        runInBackground(reply, async, () -> {
          ops.rebootDevice();
          return true;
        });
        return true;
      case BEEP_PAYMENT:
        runInBackground(reply, async, () -> {
          ops.beep();
          return true;
        });
        return true;
      case GET_SERIAL_NUMBER:
        runInBackground(reply, async, ops::getSerialNumber);
        return true;
      case GET_USER_DATA:
        runInBackground(reply, async, () -> {
          ops.getUserData();
          return true;
        });
        return true;
      case CALCULATE_INSTALLMENTS: {
        final String saleValue = call.argument("saleValue");
        final int installmentType = call.argument("installmentType");
        runInBackground(reply, async, () -> ops.calculateInstallments(saleValue, installmentType));
        return true;
      }
      case PAYMENT_DEBIT:
        ops.debitPayment(
            (int) call.argument("value"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case ACTIVE_PINPAD:
        ops.activatePinpad(call.argument("code"));
        reply.success(true);
        return true;
      case PINPAD_AUTHENTICATED:
        ops.isAuthenticated();
        reply.success(true);
        return true;
      case PAYMENT_PIX:
        ops.pixPayment(
            (int) call.argument("value"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case PAYMENT_CREDIT:
        ops.creditPayment(
            (int) call.argument("value"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case PAYMENT_CREDIT_PARC:
        ops.creditPaymentParc(
            (int) call.argument("value"),
            (int) call.argument("type"),
            (int) call.argument("parc"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case PAYMENT_VOUCHER:
        ops.voucherPayment(
            (int) call.argument("value"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case START_PAYMENT:
        ops.startPayment(
            (int) call.argument("type"),
            (int) call.argument("amount"),
            (int) call.argument("installmentType"),
            (int) call.argument("installments"),
            (String) call.argument("userReference"),
            (Boolean) call.argument("printReceipt"),
            (Boolean) call.argument("partialPay"),
            (Boolean) call.argument("isCarne"));
        reply.success(true);
        return true;
      case PAYMENT_ABORT:
        ops.abortTransaction();
        reply.success(true);
        return true;
      case LAST_TRANSACTION:
        ops.lastTransaction();
        reply.success(true);
        return true;
      case REFUND: {
        String transactionCode = call.argument("transactionCode");
        String transactionId = call.argument("transactionId");
        if (isBlank(transactionCode) || isBlank(transactionId)) {
          // Antes os nulos iam ao SDK (NPE em background, erro só por onError).
          reply.error(INVALID_ARGUMENT_CODE, "transactionCode e transactionId são obrigatórios para o estorno.", null);
          return true;
        }
        ops.refund(transactionCode, transactionId);
        reply.success(true);
        return true;
      }
      case READ_NFC:
        ops.readNfc(stringArg(call, "idEvento"));
        reply.success(true);
        return true;
      case WRITE_NFC:
        ops.writeNfc(
            stringArg(call, "valor"),
            stringArg(call, "nome"),
            stringArg(call, "cpf"),
            stringArg(call, "numeroTag"),
            stringArg(call, "celular"),
            stringArg(call, "aberto"),
            stringArg(call, "idEvento"));
        reply.success(true);
        return true;
      case REWRITE_NFC:
        ops.reWriteNfc(stringArg(call, "valor"), stringArg(call, "idEvento"));
        reply.success(true);
        return true;
      case REFUND_NFC:
        ops.refundNfc(stringArg(call, "valor"), stringArg(call, "idEvento"));
        reply.success(true);
        return true;
      case FORMAT_NFC:
        ops.formatNfc();
        reply.success(true);
        return true;
      case DEBIT_NFC:
        ops.debitNfc(stringArg(call, "idEvento"), stringArg(call, "valor"));
        reply.success(true);
        return true;
      default:
        return false;
    }
  }
}
