import 'dart:developer';

import 'package:pagseguro_payment_tech/src/payments/handler/payment_handler.dart';

class PagseguroHandler extends PaymentHandler {
  final Function(String, String) onTransaction;
  final Function(String) onMessageMonitor;

  PagseguroHandler({
    required this.onTransaction,
    required this.onMessageMonitor,
  });

  @override
  void onAuthProgress(String message) {
    log('onAuthProgress: $message');
    onTransaction('onAuthProgress', message);
  }

  @override
  void onError(String message) {
    log('onError: $message');
    onTransaction('onError', message);
  }

  @override
  void onFinishedResponse(String message) {
    log('onFinishedResponse: $message');
    onTransaction('onFinishedResponse', message);
  }

  @override
  void onMessage(String message) {
    onMessageMonitor(message);
  }

  @override
  void onTransactionInfo({String? transactionCode, String? transactionId, String? response}) {
    log('onTransactionInfo: $response');
    onTransaction('onTransactionInfo', response ?? '');
  }

  @override
  void onLoading(bool show) {}

  @override
  void disposeDialog() {}

  @override
  void onAbortedSuccessfully() {}

  @override
  void onActivationDialog() {}

  @override
  void onTransactionSuccess() {}

  @override
  void writeToFile({String? transactionCode, String? transactionId, String? response}) {}
}
