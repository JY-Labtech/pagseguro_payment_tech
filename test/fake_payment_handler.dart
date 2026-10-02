import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

/// Handler de teste que só registra o que recebeu.
class FakePaymentHandler implements PaymentHandler {
  final List<String> errors = [];
  final List<String> messages = [];
  final List<String> finishedResponses = [];
  int transactionSuccess = 0;

  @override
  void onTransactionSuccess() => transactionSuccess++;

  @override
  void onError(String message) => errors.add(message);

  @override
  void onMessage(String message) => messages.add(message);

  @override
  void onFinishedResponse(String message) => finishedResponses.add(message);

  @override
  void onLoading(bool show) {}

  @override
  void writeToFile({String? transactionCode, String? transactionId, String? response}) {}

  @override
  void onAbortedSuccessfully() {}

  @override
  void disposeDialog() {}

  @override
  void onActivationDialog() {}

  @override
  void onAuthProgress(String message) {}

  @override
  void onTransactionInfo({String? transactionCode, String? transactionId, String? response}) {}
}
