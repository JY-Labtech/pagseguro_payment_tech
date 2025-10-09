import 'dart:convert';
import 'dart:developer';

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
  double _amount = 0;
  int _parcels = 1;
  bool _isActivePinpad = false;
  bool isVisibilityCancelButton = false;
  bool _isAbortTransaction = false;
  PaymentTypeCredit _creditType = PaymentTypeCredit.CLIENT;

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
      _amount = params.amount;
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
    _pagseguroTech.payment.isAuthenticated();
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
    _pagseguroTech.payment.abortTransaction();
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
      _pagseguroTech.payment.activePinpad(_options?.activationCode ?? '');
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
    final transactionDecoded = jsonDecode(message);
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
    final saleValue = _getSaleValueInt(_amount);
    switch (_paymentType) {
      case PagseguroPaymentType.credit:
        _pagseguroTech.payment.creditPayment(saleValue, printReceipt: _options?.isPrinterEstablishment == true);
        break;
      case PagseguroPaymentType.creditParc:
        _pagseguroTech.payment.creditPaymentParc(
          saleValue,
          _parcels,
          printReceipt: _options?.isPrinterEstablishment == true,
          type: _creditType,
        );
        break;
      case PagseguroPaymentType.debit:
        _pagseguroTech.payment.debitPayment(saleValue, printReceipt: _options?.isPrinterEstablishment == true);
        break;
      case PagseguroPaymentType.voucher:
        _pagseguroTech.payment.voucherPayment(saleValue, printReceipt: _options?.isPrinterEstablishment == true);
        break;
      case PagseguroPaymentType.pix:
        _pagseguroTech.payment.pixPayment(saleValue, printReceipt: _options?.isPrinterEstablishment == true);

        break;
      default:
    }
  }

  int _getSaleValueInt(double value) {
    int saleValue = 0;
    final valueFixed = (value * 100).toStringAsFixed(0);
    final valueInt = int.parse(valueFixed);
    saleValue = valueInt;
    return saleValue;
  }

  void printerFromFile(String filePath) {
    // _pagseguroTech.payment.printer(filePath);
  }
}
