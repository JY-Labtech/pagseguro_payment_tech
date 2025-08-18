// ignore_for_file: constant_identifier_names
import 'dart:developer';

import 'package:check_licence_tech/check_licence_tech.dart';
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/licence/constants/licence_constants.dart';
import 'package:pagseguro_payment_tech/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/payments/payment.dart';
import 'package:pagseguro_payment_tech/payments/utils/payment_types.dart';

const CHANNEL_NFC_NAME = "pagseguro_payment_tech";

final class Nfc {
  final MethodChannel _channel;
  NfcHandler _nfcHandler;
  final String _licenceKey;

  // Cache da instância criada
  static Nfc? _instance;
  static bool _isLicenceValidated = false;

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
    Payment? payment,
  }) async {
    // Se já existe instância e licença validada, retorna ela diretamente
    if (_instance != null && _isLicenceValidated) {
      return _instance!;
    }

    final nfc = Nfc._internal(
      channel: channel,
      nfcHandler: nfcHandler,
      licenceKey: licenceKey,
    );

    if (payment != null && payment.isActivatedLicence) {
      _isLicenceValidated = true;
    } else {
      _isLicenceValidated = await nfc._checkLicence();
    }

    if (!_isLicenceValidated) {
      _instance = null; // Limpa a instância se a licença não for válida
      _isLicenceValidated = false;
      throw Exception('Invalid licence key. Please check your licence key.');
    }
    _instance = nfc;
    _isLicenceValidated = true;
    nfc._channel.setMethodCallHandler(nfc._callHandler);
    return nfc;
  }

  void setHandler(NfcHandler handler) {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }

    _instance?._nfcHandler = handler;
    _instance?._channel.setMethodCallHandler(_instance?._callHandler);
  }

  void closeMethodCallHandler() {
    _channel.setMethodCallHandler(null);
  }

  Future<bool> _checkLicence() async {
    if (_licenceKey.isEmpty) {
      return false;
    }
    try {
      final _licence = CheckLicenceTech.instance();
      final result = await _licence.checkLicence(
        _licenceKey,
        url: licenceCheck,
      );
      return result;
    } catch (e) {
      log('Error checking licence: $e');
      return false;
    }
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
