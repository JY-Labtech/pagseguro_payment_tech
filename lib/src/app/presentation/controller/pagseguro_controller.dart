import 'dart:async';
import 'dart:convert';
import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/payments/pagseguro_payment_tech.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/payment_types.dart';

import '../enum/pagseguro_payment_type.dart' show PagseguroPaymentType;
import '../handler/pagseguro_handler.dart';
import '../helper/pagseguro_strings.dart';
import '../models/plugpag_response_model.dart';
import '../pages/pagseguro_page.dart';

class PagseguroController {
  final PagseguroPaymentTech _pagseguroTech = PagseguroPaymentTech.I;
  PagseguroOptions? _options;

  PagseguroPaymentType _paymentType = PagseguroPaymentType.unknow;
  int _amountInCents = 0;
  int _parcels = 1;
  bool _isActivePinpad = false;
  bool isVisibilityCancelButton = false;
  bool _isAbortTransaction = false;
  PaymentTypeCredit _creditType = PaymentTypeCredit.CLIENT;
  Function(PagseguroResponseModel?)? _onPopResult;

  void setVisibilityCancelButton(bool value) {
    isVisibilityCancelButton = value;
  }

  void init({
    required PagseguroHandler? handler,
    required PagseguroPageParams params,
    required PagseguroOptions options,
  }) async {
    if (handler == null) return;
    _isActivePinpad = false;
    _isAbortTransaction = false;
    _options = options;
    await Future.delayed(const Duration(seconds: 1));
    try {
      _paymentType = params.type;
      _amountInCents = params.amountInCentsOrRounded;
      _parcels = params.parcels ?? 1;
      _creditType = params.creditType ?? PaymentTypeCredit.CLIENT;
      _initializePagseguro(handler: handler);
    } catch (e) {
      log('***Erro_init: $e');
    }
  }

  void _initializePagseguro({required PagseguroHandler handler}) async {
    _pagseguroTech.payment.setHandler(handler);
    await Future.delayed(const Duration(milliseconds: 500));
    _guard(_pagseguroTech.payment.isAuthenticated());
  }

  /// Acompanha uma chamada ao nativo. Qualquer erro (ex.: PlatformException "ERROR") encerra a tela com a resposta de erro em vez de
  /// deixar o Future falhar sem tratamento e a tela presa em "Aguarde...".
  void _guard(Future<dynamic> call) {
    unawaited(call.then<void>((_) {}, onError: (Object error) {
      final message = error is PlatformException ? (error.message ?? error.code) : error.toString();
      log('***Erro_pagseguro: $message');
      final onPopResult = _onPopResult;
      if (onPopResult != null) {
        onPopResult(_generateErrorResponse(errorMessage: message));
      }
    }));
  }

  void onMessageMonitor(String message) {
    final stateMessage = message.toLowerCase();
    if (stateMessage.contains(PagseguroStrings.approach)) {
      setVisibilityCancelButton(true);
    }
    if (stateMessage.contains(PagseguroStrings.authorized) || stateMessage.contains('autorizada')) {
      setVisibilityCancelButton(false);
    }

    if (stateMessage.contains(PagseguroStrings.remove)) {
      setVisibilityCancelButton(false);
    }

    if (stateMessage.contains(PagseguroStrings.operationCanceled)) {
      setVisibilityCancelButton(false);
    }

    if (stateMessage.contains(PagseguroStrings.notAuthorized)) {
      setVisibilityCancelButton(false);
    }
  }

  void onTransactionMonitor({
    required String method,
    required String message,
    required Function(PagseguroResponseModel?) onPopResult,
  }) async {
    _onPopResult = onPopResult;
    switch (method) {
      case 'onAuthProgress':
        // _onAuthProgress(message);
        break;
      case 'onError':
        _onError(message, onPopResult: onPopResult);
        break;
      case 'onFinishedResponse':
        _onFinishedResponse(message, onPopResult: onPopResult);
        break;
      case 'onTransactionInfo':
        _onTransactionInfo(message);
        break;
      default:
    }
  }

  void onAbortCurrentTransaction() {
    if (_isAbortTransaction) return;
    _guard(_pagseguroTech.payment.abortTransaction());
    _isAbortTransaction = true;
  }

  void _onAuthProgress(String message) {
    final message0 = message.toLowerCase();
    if (message0 == PagseguroStrings.pinpadActivate) {
      _isActivePinpad = true;
      _callPayment();
    }
    if (message0 == PagseguroStrings.pinpadNotActivate) {
      if (_isActivePinpad) {
        _callPayment();
        return;
      }
      _guard(_pagseguroTech.payment.activePinpad(_options?.activationCode ?? ''));
    }
  }

  void _onError(
    String message, {
    required Function(PagseguroResponseModel?) onPopResult,
  }) {
    final plugpagDecode = _generateErrorResponse(errorMessage: message);
    onPopResult(plugpagDecode);
  }

  void _onFinishedResponse(
    String message, {
    required Function(PagseguroResponseModel?) onPopResult,
  }) {
    final dynamic transactionDecoded;
    try {
      transactionDecoded = jsonDecode(message);
    } on FormatException catch (e) {
      log('***onFinishedResponse inválido: $e');
      return;
    }
    if (transactionDecoded is! Map) return;
    final method = transactionDecoded['method'] ?? '';

    if (method == 'activate') {
      final messageProgress = transactionDecoded['message'] ?? '';
      _onAuthProgress(messageProgress);
    }
    if (method == 'transaction') {
      final plugpagDecode = PagseguroResponseModel.fromJson(message);
      onPopResult(plugpagDecode);
    }
  }

  PagseguroResponseModel _generateErrorResponse({String? errorMessage}) => PagseguroResponseModel(resultCode: 9999999, errorMessage: errorMessage ?? 'Erro ao concluir transação.', message: errorMessage ?? 'Erro ao concluir transação.');

  void _onTransactionInfo(String message) {
    log('***onTransactionInfo: $message');
  }

  void _callPayment() {
    final saleValue = _amountInCents;
    final printReceipt = _options?.isPrinterEstablishment == true;
    switch (_paymentType) {
      case PagseguroPaymentType.credit:
        _guard(_pagseguroTech.payment.creditPayment(saleValue, printReceipt: printReceipt));
        break;
      case PagseguroPaymentType.creditParc:
        _guard(_pagseguroTech.payment.creditPaymentParc(
          saleValue,
          _parcels,
          printReceipt: printReceipt,
          type: _creditType,
        ));
        break;
      case PagseguroPaymentType.debit:
        _guard(_pagseguroTech.payment.debitPayment(saleValue, printReceipt: printReceipt));
        break;
      case PagseguroPaymentType.voucher:
        _guard(_pagseguroTech.payment.voucherPayment(saleValue, printReceipt: printReceipt));
        break;
      case PagseguroPaymentType.pix:
        _guard(_pagseguroTech.payment.pixPayment(saleValue, printReceipt: printReceipt));
        break;
      default:
    }
  }

  void printerFromFile(String filePath) {
    // _pagseguroTech.payment.printer(filePath);
  }
}
