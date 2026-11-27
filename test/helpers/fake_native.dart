import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

const channelName = 'pagseguro_payment_tech';
const channel = MethodChannel(channelName);

/// Simula o lado nativo do canal: registra as chamadas e responde conforme configurado.
class FakeNative {
  FakeNative() {
    messenger.setMockMethodCallHandler(channel, _handle);
  }

  TestDefaultBinaryMessenger get messenger => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  final List<MethodCall> calls = [];

  /// Respostas por método; função recebe a chamada e devolve o valor (ou lança).
  final Map<String, Object? Function(MethodCall call)> responses = {};

  /// Quando definido, toda chamada (exceto initPayment) lança este erro.
  PlatformException? errorForAll;

  /// Valor padrão devolvido pelo nativo (o plugin responde `true`).
  Object? defaultResponse = true;

  Future<Object?> _handle(MethodCall call) async {
    calls.add(call);
    final response = responses[call.method];
    if (response != null) return response(call);
    if (errorForAll != null && call.method != 'initPayment') throw errorForAll!;
    return defaultResponse;
  }

  MethodCall last(String method) => calls.lastWhere((c) => c.method == method);

  List<String> get methods => calls.map((c) => c.method).toList();

  /// Envia um evento do nativo para o Dart (como `channel.invokeMethod` no Java).
  /// Devolve o envelope de resposta (null = evento não tratado / notImplemented).
  Future<ByteData?> sendEvent(String method, [Object? arguments]) async {
    ByteData? reply;
    await messenger.handlePlatformMessage(
      channelName,
      const StandardMethodCodec().encodeMethodCall(MethodCall(method, arguments)),
      (data) => reply = data,
    );
    return reply;
  }

  void dispose() => messenger.setMockMethodCallHandler(channel, null);
}

/// Erros simulados do nativo.
final invalidArgumentError = PlatformException(code: 'INVALID_ARGUMENT', message: 'argumento inválido');
final genericError = PlatformException(code: 'ERROR', message: 'terminal ocupado');

final throwsInvalidArgumentError = throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'INVALID_ARGUMENT'));
final throwsGenericError = throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERROR'));

/// PaymentHandler que registra cada callback como "nome:argumentos".
class RecordingPaymentHandler implements PaymentHandler {
  final List<String> calls = [];

  @override
  void onTransactionSuccess() => calls.add('onTransactionSuccess');

  @override
  void onError(String message) => calls.add('onError:$message');

  @override
  void onMessage(String message) => calls.add('onMessage:$message');

  @override
  void onFinishedResponse(String message) => calls.add('onFinishedResponse:$message');

  @override
  void onLoading(bool show) => calls.add('onLoading:$show');

  @override
  void writeToFile({String? transactionCode, String? transactionId, String? response}) =>
      calls.add('writeToFile:$transactionCode|$transactionId|$response');

  @override
  void onAbortedSuccessfully() => calls.add('onAbortedSuccessfully');

  @override
  void disposeDialog() => calls.add('disposeDialog');

  @override
  void onActivationDialog() => calls.add('onActivationDialog');

  @override
  void onAuthProgress(String message) => calls.add('onAuthProgress:$message');

  @override
  void onTransactionInfo({String? transactionCode, String? transactionId, String? response}) =>
      calls.add('onTransactionInfo:$transactionCode|$transactionId|$response');
}

/// NfcHandler que registra cada callback como "nome:argumentos".
class RecordingNfcHandler implements NfcHandler {
  final List<String> calls = [];

  @override
  void onTransactionSuccess() => calls.add('onTransactionSuccess');

  @override
  void onError(String message) => calls.add('onError:$message');

  @override
  void onMessage(String message) => calls.add('onMessage:$message');

  @override
  void showSuccess(List<dynamic> result) => calls.add('showSuccess:${result.join(',')}');

  @override
  void showSuccessWrite(int result) => calls.add('showSuccessWrite:$result');

  @override
  void showSuccessReWrite(String result) => calls.add('showSuccessReWrite:$result');

  @override
  void showSuccessFormat(int result) => calls.add('showSuccessFormat:$result');

  @override
  void showSuccessDebitNfc(String result) => calls.add('showSuccessDebitNfc:$result');

  @override
  void showSuccessRefundNfc(String result) => calls.add('showSuccessRefundNfc:$result');

  @override
  void showErrorRead(String message) => calls.add('showErrorRead:$message');

  @override
  void showErrorWrite(String message) => calls.add('showErrorWrite:$message');

  @override
  void showErrorReWrite(String message) => calls.add('showErrorReWrite:$message');

  @override
  void showErrorFormat(String message) => calls.add('showErrorFormat:$message');

  @override
  void showErrorDebitNfc(String message) => calls.add('showErrorDebitNfc:$message');

  @override
  void showErrorRefundNfc(String message) => calls.add('showErrorRefundNfc:$message');

  @override
  void onLoading(bool show) => calls.add('onLoading:$show');

  @override
  void writeToFile({String? transactionCode, String? transactionId}) => calls.add('writeToFile:$transactionCode|$transactionId');

  @override
  void onAbortedSuccessfully() => calls.add('onAbortedSuccessfully');

  @override
  void disposeDialog() => calls.add('disposeDialog');

  @override
  void onActivationDialog() => calls.add('onActivationDialog');

  @override
  void onAuthProgress(String message) => calls.add('onAuthProgress:$message');

  @override
  void onTransactionInfo({String? transactionCode, String? transactionId}) => calls.add('onTransactionInfo:$transactionCode|$transactionId');
}

/// Inicializa o Payment e limpa as chamadas registradas.
Future<Payment> initPayment(FakeNative native, PaymentHandler handler) async {
  final payment = PagseguroPaymentTech.I.initPayment(handler: handler);
  expect(await payment.ready, isTrue);
  native.calls.clear();
  return payment;
}
