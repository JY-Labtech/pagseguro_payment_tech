// ignore_for_file: constant_identifier_names
import 'package:check_licence_tech/check_licence_tech.dart';
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/licence/constants/licence_constants.dart';
import 'package:pagseguro_payment_tech/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/payments/utils/payment_types.dart';

const CHANNEL_NFC_NAME = "pagseguro_payment_tech";

final class Nfc {
  final MethodChannel _channel;
  final NfcHandler _nfcHandler;
  final String _licenceKey;
  final CheckLicenceTech _licence = CheckLicenceTech.instance();

  bool _isValidLicence = false;

  Nfc._internal({
    required MethodChannel channel,
    required NfcHandler nfcHandler,
    required String licenceKey,
  })  : _channel = channel,
        _nfcHandler = nfcHandler,
        _licenceKey = licenceKey;

  /// Exposto apenas internamente no package
  static Future<Nfc> internal({
    required MethodChannel channel,
    required NfcHandler nfcHandler,
    required String licenceKey,
  }) async {
    final nfc = Nfc._internal(
      channel: channel,
      nfcHandler: nfcHandler,
      licenceKey: licenceKey,
    );
    await nfc._checkLicence();
    if (!nfc._isValidLicence) {
      throw Exception('Invalid licence key. Please check your licence key.');
    }
    nfc._channel.setMethodCallHandler(nfc._callHandler);
    return nfc;
  }

  void closeMethodCallHandler() {
    _channel.setMethodCallHandler(null);
  }

  Future<void> _checkLicence() async {
    if (_isValidLicence) return;
    if (_licenceKey.isEmpty) throw Exception('Licence key is empty.');
    _isValidLicence = await _licence.checkLicence(_licenceKey, url: licenceCheck);
    if (!_isValidLicence) {
      throw Exception('Invalid licence key. Please check your licence key.');
    }
  }

  Future<bool> _invoke(String method, [Map<String, dynamic>? arguments]) async {
    await _checkLicence();
    await _channel.invokeMethod(method, arguments);
    return true;
  }

  Future<bool> abortTransaction() => _invoke(PaymentTypeCall.ABORT.method);

  Future<bool> lastTransaction() => _invoke(PaymentTypeCall.LAST_TRANSACTION.method);

  Future<bool> refund({
    required String transactionCode,
    required String transactionId,
  }) =>
      _invoke(PaymentTypeCall.REFUND.method, {
        "transactionCode": transactionCode,
        "transactionId": transactionId,
      });

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

  Future<bool> formatNfc() => _invoke(PaymentTypeCall.FORMAT_NFC.method);

  Future<bool> debitNfc(dynamic idEvento, dynamic valor) => _invoke(PaymentTypeCall.DEBIT_NFC.method, {"idEvento": idEvento, "valor": valor});

  Future<dynamic> _callHandler(MethodCall call) async {
    switch (call.method.handler) {
      case PaymentTypeHandler.ON_TRANSACTION_SUCCESS:
        _nfcHandler.onTransactionSuccess();
        break;
      case PaymentTypeHandler.ON_ERROR:
        _nfcHandler.onError(call.arguments);
        break;
      case PaymentTypeHandler.ON_MESSAGE:
        _nfcHandler.onMessage(call.arguments);
        break;
      case PaymentTypeHandler.ON_LOADING:
        _nfcHandler.onLoading(call.arguments);
        break;
      case PaymentTypeHandler.WRITE_TO_FILE:
        _nfcHandler.writeToFile(
          transactionCode: call.arguments['transactionCode'],
          transactionId: call.arguments['transactionId'],
        );
        break;
      case PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY:
        _nfcHandler.onAbortedSuccessfully();
        break;
      case PaymentTypeHandler.DISPOSE_DIALOG:
        _nfcHandler.disposeDialog();
        break;
      case PaymentTypeHandler.ACTIVE_DIALOG:
        _nfcHandler.onActivationDialog();
        break;
      case PaymentTypeHandler.ON_AUTH_PROGRESS:
        _nfcHandler.onAuthProgress(call.arguments);
        break;
      case PaymentTypeHandler.ON_TRANSACTION_INFO:
        _nfcHandler.onTransactionInfo(
          transactionCode: call.arguments['transactionCode'],
          transactionId: call.arguments['transactionId'],
        );
        break;
      case PaymentTypeHandler.SHOW_SUCCESS:
        _nfcHandler.showSuccess(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_SUCCESS_WRITE:
        _nfcHandler.showSuccessWrite(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_SUCCESS_RE_WRITE:
        _nfcHandler.showSuccessReWrite(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_SUCCESS_DEBIT_NFC:
        _nfcHandler.showSuccessDebitNfc(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_SUCCESS_REFUND_NFC:
        _nfcHandler.showSuccessRefundNfc(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_SUCCESS_FORMAT:
        _nfcHandler.showSuccessFormat(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_READ:
        _nfcHandler.showErrorRead(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_WRITE:
        _nfcHandler.showErrorWrite(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_RE_WRITE:
        _nfcHandler.showErrorReWrite(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_FORMAT:
        _nfcHandler.showErrorFormat(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_DEBIT_NFC:
        _nfcHandler.showErrorDebitNfc(call.arguments);
        break;
      case PaymentTypeHandler.SHOW_ERROR_REFUND_NFC:
        _nfcHandler.showErrorRefundNfc(call.arguments);
        break;
      default:
        throw "METHOD NOT IMPLEMENTED";
    }
    return true;
  }
}
