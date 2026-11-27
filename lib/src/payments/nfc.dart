// ignore_for_file: constant_identifier_names
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/event_dispatcher.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/src/payments/payment.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/input_validation.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/payment_types.dart';

const CHANNEL_NFC_NAME = "pagseguro_payment_tech";

final class Nfc {
  final MethodChannel _channel;
  NfcHandler _nfcHandler;

  // Cache da instância criada
  static Nfc? _instance;

  Nfc._internal({
    required MethodChannel channel,
    required NfcHandler nfcHandler,
  })  : _channel = channel,
        _nfcHandler = nfcHandler;

  /// Exposto apenas internamente no package
  static Nfc internal({
    required MethodChannel channel,
    required NfcHandler nfcHandler,
    Payment? payment,
  }) {
    // Instância já criada: atualiza o handler (antes o novo handler era ignorado).
    final current = _instance;
    if (current != null) {
      current._nfcHandler = nfcHandler;
      PagseguroEventDispatcher.attachNfc(current._channel, nfcHandler);
      return current;
    }

    final nfc = Nfc._internal(
      channel: channel,
      nfcHandler: nfcHandler,
    );

    _instance = nfc;
    PagseguroEventDispatcher.attachNfc(nfc._channel, nfcHandler);
    return nfc;
  }

  /// Handler que recebe os eventos NFC.
  NfcHandler get handler => _nfcHandler;

  /// Limpa a instância em cache (uso interno: PagseguroPaymentTech.resetForTesting).
  static void resetForTesting() {
    _instance = null;
  }

  void setHandler(NfcHandler handler) {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }

    _instance!._nfcHandler = handler;
    PagseguroEventDispatcher.attachNfc(_instance!._channel, handler);
  }

  /// Para de entregar eventos ao [NfcHandler]. Os eventos de pagamento continuam chegando ao
  /// `PaymentHandler` (antes, o canal inteiro ficava sem handler).
  void closeMethodCallHandler() {
    PagseguroEventDispatcher.detachNfc();
  }

  Future<bool> _invoke(String method, [Map<String, dynamic>? arguments]) async {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }
    await _channel.invokeMethod(method, arguments);
    return true;
  }

  Future<bool> abortTransaction() => _invoke(PaymentTypeCall.ABORT.method);

  Future<bool> lastTransaction() => _invoke(PaymentTypeCall.LAST_TRANSACTION.method);

  /// Estorno: código/id vazios lançam `PlatformException(code: 'INVALID_ARGUMENT')` sem chamar o nativo.
  Future<bool> refund({
    required String transactionCode,
    required String transactionId,
  }) async {
    checkRefundArguments(transactionCode, transactionId);
    return _invoke(PaymentTypeCall.REFUND.method, {
        "transactionCode": transactionCode,
        "transactionId": transactionId,
      });
  }

  Future<bool> readNfc(dynamic idEvento) => _invoke(PaymentTypeCall.READ_NFC.method, {"idEvento": idEvento});

  Future<bool> writeNfc(dynamic valor, dynamic nome, dynamic cpf, dynamic numeroTag, dynamic celular, dynamic aberto, dynamic idEvento) => _invoke(PaymentTypeCall.WRITE_NFC.method, {
        "valor": valor,
        "nome": nome,
        "cpf": cpf,
        "numeroTag": numeroTag,
        "celular": celular,
        "aberto": aberto,
        "idEvento": idEvento,
      });

  Future<bool> reWriteNfc(dynamic valor, dynamic idEvento) => _invoke(PaymentTypeCall.REWRITE_NFC.method, {"valor": valor, "idEvento": idEvento});

  Future<bool> refundNfc(dynamic valor, dynamic idEvento) => _invoke(PaymentTypeCall.REFUND_NFC.method, {"valor": valor, "idEvento": idEvento});

  /// Espera o cartão por até [timeoutSeconds] e devolve o UID (número de série) em hexadecimal
  /// maiúsculo, ex.: `04A1B2C3`. Não autentica blocos nem grava nada no cartão: use o UID como
  /// chave do cartão no seu servidor. Diferente dos outros métodos NFC, o resultado vem no
  /// próprio `Future` (não pelo [NfcHandler]). Falha com `PlatformException` (cartão não
  /// detectado no tempo ou erro do leitor).
  Future<String> readNfcUid({int timeoutSeconds = 20}) async {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }
    final uid = await _channel.invokeMethod<String>(PaymentTypeCall.READ_NFC_UID.method, {"timeoutSeconds": timeoutSeconds});
    if (uid == null || uid.isEmpty) {
      throw PlatformException(code: 'ERROR', message: 'Cartão não identificado.');
    }
    return uid.toUpperCase();
  }

  Future<bool> formatNfc() => _invoke(PaymentTypeCall.FORMAT_NFC.method);

  Future<bool> debitNfc(dynamic idEvento, dynamic valor) => _invoke(PaymentTypeCall.DEBIT_NFC.method, {"idEvento": idEvento, "valor": valor});
}
