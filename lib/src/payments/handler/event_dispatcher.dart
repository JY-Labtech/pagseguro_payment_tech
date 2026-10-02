import 'package:flutter/services.dart';

import '../utils/payment_types.dart';
import 'nfc_handler.dart';
import 'payment_handler.dart';

enum _Target { payment, nfc }

/// Recebe TODOS os eventos que o nativo envia pelo canal `pagseguro_payment_tech` e entrega ao
/// handler certo.
///
/// `Payment` e `Nfc` usam o mesmo MethodChannel. Antes, cada um chamava
/// `setMethodCallHandler` e o último registrado "roubava" o canal: depois de `initNfc`, o
/// `onFinishedResponse` (licença, transação) e os eventos de pagamento caíam no handler NFC (que
/// não os conhece) e eram perdidos; depois de `Payment.setHandler`, os eventos NFC (`show*`) eram
/// perdidos. Agora:
/// - eventos exclusivos do NFC (`show*`) vão sempre para o [NfcHandler];
/// - `onFinishedResponse` vai sempre para o [PaymentHandler];
/// - os eventos comuns (`onError`, `onMessage`, `onLoading`...) vão para o handler registrado por
///   último (mesmo comportamento das versões anteriores) ou para o outro, se ele não existir.
///
/// Os argumentos são convertidos com segurança: `null` do nativo (ex.: exceção sem mensagem)
/// vira `''` em vez de lançar TypeError e perder o evento.
class PagseguroEventDispatcher {
  PagseguroEventDispatcher._();

  static PaymentHandler? _payment;
  static NfcHandler? _nfc;
  static _Target? _active;
  static MethodChannel? _channel;

  static void attachPayment(MethodChannel channel, PaymentHandler handler) {
    _payment = handler;
    _active = _Target.payment;
    _install(channel);
  }

  static void attachNfc(MethodChannel channel, NfcHandler handler) {
    _nfc = handler;
    _active = _Target.nfc;
    _install(channel);
  }

  /// Remove o handler NFC. Se não houver handler de pagamento, o canal deixa de receber eventos.
  static void detachNfc() {
    _nfc = null;
    if (_payment != null) {
      _active = _Target.payment;
      return;
    }
    _active = null;
    _channel?.setMethodCallHandler(null);
    _channel = null;
  }

  static void _install(MethodChannel channel) {
    _channel = channel;
    channel.setMethodCallHandler(handle);
  }

  /// Remove todos os handlers (uso interno: PagseguroPaymentTech.resetForTesting).
  static void reset() {
    _channel?.setMethodCallHandler(null);
    _channel = null;
    _payment = null;
    _nfc = null;
    _active = null;
  }

  /// Converte o nome do evento enviado pelo nativo. Aceita a grafia antiga `onTransactionSucess`
  /// e a enviada pelo nativo (`onTransactionSuccess`), que antes não era reconhecida.
  static PaymentTypeHandler? parseEvent(String method) {
    if (method == 'onTransactionSuccess') {
      return PaymentTypeHandler.ON_TRANSACTION_SUCCESS;
    }
    for (final value in PaymentTypeHandler.values) {
      if (value.method == method) {
        return value;
      }
    }
    if (method == 'onTransactionSucess') {
      return PaymentTypeHandler.ON_TRANSACTION_SUCCESS;
    }
    return null;
  }

  static String _string(dynamic value) => value == null ? '' : value.toString();

  static int _int(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    return int.tryParse(_string(value)) ?? 0;
  }

  static String _field(dynamic arguments, String key) => arguments is Map ? _string(arguments[key]) : '';

  static _Target? _commonTarget() {
    if (_active == _Target.nfc && _nfc != null) return _Target.nfc;
    if (_active == _Target.payment && _payment != null) return _Target.payment;
    if (_payment != null) return _Target.payment;
    if (_nfc != null) return _Target.nfc;
    return null;
  }

  static Future<dynamic> handle(MethodCall call) async {
    final event = parseEvent(call.method);
    if (event == null) {
      throw MissingPluginException('Evento não tratado: ${call.method}');
    }
    final args = call.arguments;
    final payment = _payment;
    final nfc = _nfc;

    switch (event) {
      // Exclusivo do pagamento.
      case PaymentTypeHandler.ON_FINISHED_RESPONSE:
        payment?.onFinishedResponse(_string(args));
        return true;
      // Exclusivos do NFC.
      case PaymentTypeHandler.SHOW_SUCCESS:
        nfc?.showSuccess(args is List ? List<dynamic>.from(args) : <dynamic>[]);
        return true;
      case PaymentTypeHandler.SHOW_SUCCESS_WRITE:
        nfc?.showSuccessWrite(_int(args));
        return true;
      case PaymentTypeHandler.SHOW_SUCCESS_RE_WRITE:
        nfc?.showSuccessReWrite(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_SUCCESS_FORMAT:
        nfc?.showSuccessFormat(_int(args));
        return true;
      case PaymentTypeHandler.SHOW_SUCCESS_DEBIT_NFC:
        nfc?.showSuccessDebitNfc(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_SUCCESS_REFUND_NFC:
        nfc?.showSuccessRefundNfc(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_READ:
        nfc?.showErrorRead(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_WRITE:
        nfc?.showErrorWrite(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_RE_WRITE:
        nfc?.showErrorReWrite(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_FORMAT:
        nfc?.showErrorFormat(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_DEBIT_NFC:
        nfc?.showErrorDebitNfc(_string(args));
        return true;
      case PaymentTypeHandler.SHOW_ERROR_REFUND_NFC:
        nfc?.showErrorRefundNfc(_string(args));
        return true;
      default:
        break;
    }

    // Eventos comuns aos dois handlers.
    switch (_commonTarget()) {
      case _Target.payment:
        _dispatchPayment(event, args, payment!);
        break;
      case _Target.nfc:
        _dispatchNfc(event, args, nfc!);
        break;
      case null:
        break;
    }
    return true;
  }

  static void _dispatchPayment(PaymentTypeHandler event, dynamic args, PaymentHandler handler) {
    switch (event) {
      case PaymentTypeHandler.ON_TRANSACTION_SUCCESS:
        handler.onTransactionSuccess();
        break;
      case PaymentTypeHandler.ON_ERROR:
        handler.onError(_string(args));
        break;
      case PaymentTypeHandler.ON_MESSAGE:
        handler.onMessage(_string(args));
        break;
      case PaymentTypeHandler.ON_LOADING:
        handler.onLoading(args == true);
        break;
      case PaymentTypeHandler.WRITE_TO_FILE:
        handler.writeToFile(
          transactionCode: _field(args, 'transactionCode'),
          transactionId: _field(args, 'transactionId'),
          response: _field(args, 'response'),
        );
        break;
      case PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY:
        handler.onAbortedSuccessfully();
        break;
      case PaymentTypeHandler.DISPOSE_DIALOG:
        handler.disposeDialog();
        break;
      case PaymentTypeHandler.ACTIVE_DIALOG:
        handler.onActivationDialog();
        break;
      case PaymentTypeHandler.ON_AUTH_PROGRESS:
        handler.onAuthProgress(_string(args));
        break;
      case PaymentTypeHandler.ON_TRANSACTION_INFO:
        handler.onTransactionInfo(
          transactionCode: _field(args, 'transactionCode'),
          transactionId: _field(args, 'transactionId'),
          response: _field(args, 'response'),
        );
        break;
      default:
        break;
    }
  }

  static void _dispatchNfc(PaymentTypeHandler event, dynamic args, NfcHandler handler) {
    switch (event) {
      case PaymentTypeHandler.ON_TRANSACTION_SUCCESS:
        handler.onTransactionSuccess();
        break;
      case PaymentTypeHandler.ON_ERROR:
        handler.onError(_string(args));
        break;
      case PaymentTypeHandler.ON_MESSAGE:
        handler.onMessage(_string(args));
        break;
      case PaymentTypeHandler.ON_LOADING:
        handler.onLoading(args == true);
        break;
      case PaymentTypeHandler.WRITE_TO_FILE:
        handler.writeToFile(
          transactionCode: _field(args, 'transactionCode'),
          transactionId: _field(args, 'transactionId'),
        );
        break;
      case PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY:
        handler.onAbortedSuccessfully();
        break;
      case PaymentTypeHandler.DISPOSE_DIALOG:
        handler.disposeDialog();
        break;
      case PaymentTypeHandler.ACTIVE_DIALOG:
        handler.onActivationDialog();
        break;
      case PaymentTypeHandler.ON_AUTH_PROGRESS:
        handler.onAuthProgress(_string(args));
        break;
      case PaymentTypeHandler.ON_TRANSACTION_INFO:
        handler.onTransactionInfo(
          transactionCode: _field(args, 'transactionCode'),
          transactionId: _field(args, 'transactionId'),
        );
        break;
      default:
        break;
    }
  }
}
