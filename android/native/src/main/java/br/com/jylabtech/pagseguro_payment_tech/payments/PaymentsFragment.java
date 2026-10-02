package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.HashMap;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.core.EventSink;
import br.com.jylabtech.pagseguro_payment_tech.core.MainThreadEventSink;
import io.flutter.plugin.common.MethodChannel;

/**
 * Eventos de pagamento enviados ao {@code PaymentHandler} do Dart. Os nomes dos métodos são o
 * contrato com {@code PaymentTypeHandler} (lib/src/payments/utils/payment_types.dart).
 * Os eventos sempre chegam ao canal pela main thread ({@link MainThreadEventSink}).
 */
public class PaymentsFragment implements PaymentsContract {

  static final String ON_TRANSACTION_SUCCESS = "onTransactionSuccess";
  static final String ON_ERROR = "onError";
  static final String ON_MESSAGE = "onMessage";
  static final String ON_FINISHED_RESPONSE = "onFinishedResponse";
  static final String ON_LOADING = "onLoading";
  static final String WRITE_TO_FILE = "writeToFile";
  static final String ON_ABORTED_SUCCESSFULLY = "onAbortedSuccessfully";
  static final String DISPOSE_DIALOG = "disposeDialog";
  static final String ACTIVE_DIALOG = "activeDialog";
  static final String ON_AUTH_PROGRESS = "onAuthProgress";
  static final String ON_TRANSACTION_INFO = "onTransactionInfo";

  private final EventSink sink;

  public PaymentsFragment(MethodChannel channel) {
    this(new MainThreadEventSink(channel));
  }

  public PaymentsFragment(EventSink sink) {
    this.sink = sink;
  }

  @Override
  public void onTransactionSuccess() {
    sink.send(ON_TRANSACTION_SUCCESS, true);
  }

  @Override
  public void onError(String message) {
    sink.send(ON_ERROR, message);
  }

  @Override
  public void onMessage(String message) {
    sink.send(ON_MESSAGE, message);
  }

  @Override
  public void onFinishedResponse(String message) {
    sink.send(ON_FINISHED_RESPONSE, message);
  }

  @Override
  public void onLoading(boolean show) {
    sink.send(ON_LOADING, show);
  }

  @Override
  public void writeToFile(String transactionCode, String transactionId, String response) {
    sink.send(WRITE_TO_FILE, transactionMap(transactionCode, transactionId, response));
  }

  @Override
  public void onAbortedSuccessfully() {
    sink.send(ON_ABORTED_SUCCESSFULLY, true);
  }

  @Override
  public void disposeDialog() {
    sink.send(DISPOSE_DIALOG, true);
  }

  @Override
  public void onActivationDialog() {
    sink.send(ACTIVE_DIALOG, true);
  }

  @Override
  public void onAuthProgress(String message) {
    sink.send(ON_AUTH_PROGRESS, message);
  }

  @Override
  public void onTransactionInfo(String transactionCode, String transactionId, String response) {
    sink.send(ON_TRANSACTION_INFO, transactionMap(transactionCode, transactionId, response));
  }

  private static Map<String, String> transactionMap(String transactionCode, String transactionId, String response) {
    Map<String, String> map = new HashMap<>();
    map.put("transactionCode", transactionCode);
    map.put("transactionId", transactionId);
    map.put("response", response);
    return map;
  }
}
