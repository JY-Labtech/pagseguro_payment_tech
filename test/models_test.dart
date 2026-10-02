import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';
import 'package:pagseguro_payment_tech/src/app/presentation/helper/pagseguro_strings.dart';

/// Modelos, enums e helpers públicos.
void main() {
  group('PagseguroResponseModel', () {
    // JSON real gerado pelo ActionResult nativo (Gson) para uma transação aprovada.
    const approvedJson = '{"method":"transaction","eventCode":0,"isBuildResponse":1,'
        '"mode":"RESPONSE_PAGSEGURO","message":"APROVADA","transactionCode":"TC123",'
        '"transactionId":"TI456","date":"01/10/2026","time":"12:00:00","hostNsu":"000123",'
        '"cardBrand":"VISA","bin":"411111","holder":"1234","userReference":"SMARTPOS",'
        '"terminalSerialNumber":"PBA123","amount":"2590","result":0,"nsu":"999",'
        '"installments":1,"originalAmount":2590,"paymentType":1,"userRegistered":0}';

    test('fromJson lê o JSON do nativo, inclusive transactionId', () {
      final model = PagseguroResponseModel.fromJson(approvedJson);
      expect(model.resultCode, 0);
      expect(model.message, 'APROVADA');
      expect(model.transactionCode, 'TC123');
      expect(model.transactionID, 'TI456');
      expect(model.hostNSU, '000123');
      expect(model.nsu, '999');
      expect(model.cardBin, '411111');
      expect(model.cardBrand, 'VISA');
      expect(model.amount, '2590');
      expect(model.date, '01/10/2026');
      expect(model.time, '12:00:00');
      expect(model.terminalSerialNumber, 'PBA123');
      expect(model.paymentType, 1);
      expect(model.errorCode, isNull);
      expect(model.errorMessage, isNull);
      expect(model.isTransactionValid, isTrue);
    });

    test('erro do nativo (result 999999 + errorMessage)', () {
      final model = PagseguroResponseModel.fromJson('{"method":"transaction","result":999999,"errorMessage":"A5 - NAO AUTORIZADA"}');
      expect(model.resultCode, 999999);
      expect(model.errorMessage, 'A5 - NAO AUTORIZADA');
      expect(model.isTransactionValid, isFalse);
    });

    test('campos ausentes ficam nulos e tipos trocados são aceitos', () {
      expect(PagseguroResponseModel.fromMap({}), PagseguroResponseModel());
      final model = PagseguroResponseModel.fromMap({'result': '0', 'amount': 2590, 'paymentType': 5.0, 'transactionID': 'OLD', 'errorCode': 7});
      expect(model.resultCode, 0);
      expect(model.amount, '2590');
      expect(model.paymentType, 5);
      expect(model.transactionID, 'OLD');
      expect(model.errorCode, '7');
      expect(PagseguroResponseModel.fromMap({'result': 'abc'}).resultCode, isNull);
    });

    test('toMap/fromMap e toJson/fromJson fazem round-trip', () {
      final model = PagseguroResponseModel.fromJson(approvedJson);
      expect(PagseguroResponseModel.fromMap(model.toMap()), model);
      expect(PagseguroResponseModel.fromJson(model.toJson()), model);
      expect(jsonDecode(model.toJson())['transactionID'], 'TI456');
    });

    test('isTransactionValid: PIX exige transactionCode; cartão exige NSUs', () {
      expect(PagseguroResponseModel(resultCode: 0, paymentType: 5, transactionCode: 'X').isTransactionValid, isTrue);
      expect(PagseguroResponseModel(resultCode: 0, paymentType: 5, transactionCode: '').isTransactionValid, isFalse);
      expect(PagseguroResponseModel(resultCode: 0, paymentType: 1, hostNSU: '', nsu: '1').isTransactionValid, isFalse);
      expect(PagseguroResponseModel(resultCode: 0, paymentType: 1, hostNSU: '1', nsu: '1').isTransactionValid, isTrue);
      expect(PagseguroResponseModel(resultCode: 1, paymentType: 1, hostNSU: '1', nsu: '1').isTransactionValid, isFalse);
    });

    test('copyWith, igualdade, hashCode e toString', () {
      final model = PagseguroResponseModel(resultCode: 0, message: 'a');
      final copy = model.copyWith(message: 'b', amount: '10');
      expect(copy.message, 'b');
      expect(copy.amount, '10');
      expect(copy.resultCode, 0);
      expect(model.copyWith(), model);
      expect(model.copyWith().hashCode, model.hashCode);
      expect(model == copy, isFalse);
      expect(model.toString(), contains('resultCode: 0'));
    });
  });

  group('PlugPagInstallment', () {
    test('fromMap/toMap com valores em centavos', () {
      final installment = PlugPagInstallment.fromMap({'quantity': 3, 'amount': 700, 'total': 2100});
      expect(installment.quantity, 3);
      expect(installment.amount, 700);
      expect(installment.total, 2100);
      expect(installment.toMap(), {'quantity': 3, 'amount': 700, 'total': 2100});
    });

    test('campos ausentes ou nulos viram 0', () {
      expect(PlugPagInstallment.fromMap({}), PlugPagInstallment(quantity: 0, amount: 0, total: 0));
      expect(PlugPagInstallment.fromMap({'quantity': null, 'amount': 1}).amount, 1);
    });

    test('igualdade, hashCode e toString', () {
      final a = PlugPagInstallment(quantity: 1, amount: 2, total: 2);
      expect(a, PlugPagInstallment(quantity: 1, amount: 2, total: 2));
      expect(a.hashCode, PlugPagInstallment(quantity: 1, amount: 2, total: 2).hashCode);
      expect(a == PlugPagInstallment(quantity: 2, amount: 2, total: 2), isFalse);
      expect(a.toString(), 'PlugPagInstallment(quantity: 1, amount: 2, total: 2)');
    });
  });

  group('enums e extensões', () {
    test('PaymentTypeCall.method == nomes tratados pelo nativo (PaymentMethodRouter)', () {
      const expected = {
        PaymentTypeCall.CREDIT: 'paymentCredit',
        PaymentTypeCall.CREDIT_PARC: 'paymentCreditParc',
        PaymentTypeCall.DEBIT: 'paymentDebit',
        PaymentTypeCall.PIX: 'paymentPix',
        PaymentTypeCall.VOUCHER: 'paymentVoucher',
        PaymentTypeCall.ABORT: 'paymentAbort',
        PaymentTypeCall.LAST_TRANSACTION: 'paymentLastTransaction',
        PaymentTypeCall.REFUND: 'paymentRefund',
        PaymentTypeCall.ACTIVEPINPAD: 'paymentActivePinpad',
        PaymentTypeCall.PINPAD_AUTHENTICATED: 'paymentIsAuthenticated',
        PaymentTypeCall.READ_NFC: 'paymentReadNfc',
        PaymentTypeCall.WRITE_NFC: 'paymentWriteNfc',
        PaymentTypeCall.REWRITE_NFC: 'paymentReWriteNfc',
        PaymentTypeCall.REFUND_NFC: 'paymentReFundNfc',
        PaymentTypeCall.DEBIT_NFC: 'paymentDebitNfc',
        PaymentTypeCall.FORMAT_NFC: 'paymentFormatNfc',
        PaymentTypeCall.PRINTER_FILE: 'paymentPrinterFile',
        PaymentTypeCall.START_PAYMENT: 'startPayment',
        PaymentTypeCall.PRINTER: 'paymentPrinter',
        PaymentTypeCall.PRINTER_BASIC: 'paymentPrinterBasic',
        PaymentTypeCall.PRINTER_FILE_PATH: 'paymentPrinterFilePath',
        PaymentTypeCall.REBOOT: 'paymentReboot',
        PaymentTypeCall.BEEP: 'paymentBeep',
        PaymentTypeCall.GET_SERIAL_NUMBER: 'paymentGetSerialNumber',
        PaymentTypeCall.GET_USER_DATA: 'paymentGetUserData',
        PaymentTypeCall.STYLE_DIALOG: 'paymentStyleDialog',
        PaymentTypeCall.CALCULATE_INSTALLMENTS: 'paymentCalculateInstallments',
        PaymentTypeCall.SET_PRINT_DIALOG_MAX_TIME: 'paymentSetPrintDialogMaxTime',
      };
      expect(expected.length, PaymentTypeCall.values.length);
      for (final value in PaymentTypeCall.values) {
        expect(value.method, expected[value], reason: value.toString());
      }
    });

    test('PaymentTypeHandler: method e handler são inversos (todos os eventos têm nome)', () {
      for (final value in PaymentTypeHandler.values) {
        expect(value.method, isA<String>(), reason: value.toString());
        expect(value.method.handler, value);
      }
      expect(PaymentTypeHandler.ON_TRANSACTION_SUCCESS.method, 'onTransactionSuccess');
      expect(PaymentTypeHandler.SHOW_ERROR_REFUND_NFC.method, 'showErrorRefundNfc');
      expect('onTransactionSucess'.handler, PaymentTypeHandler.ON_TRANSACTION_SUCCESS);
      expect(() => 'desconhecido'.handler, throwsA('NOT IMPLEMENTED'));
    });

    test('valores inteiros enviados ao SDK', () {
      expect(PaymentTypeCredit.SALESMAN.value, 2);
      expect(PaymentTypeCredit.CLIENT.value, 3);
      expect(InstallmentType.A_VISTA.value, 1);
      expect(InstallmentType.PARC_VENDEDOR.value, 2);
      expect(InstallmentType.PARC_COMPRADOR.value, 3);
      expect(PaymentType.values.map((e) => e.value), [1, 2, 3, 4, 5, 6, 7, 8]);
    });

    test('PagseguroPaymentType', () {
      expect(PagseguroPaymentType.values.map((e) => e.name), ['debit', 'credit', 'creditParc', 'voucher', 'pix', 'unknow']);
    });
  });

  group('tela pronta (PagseguroPage)', () {
    test('PagseguroPageParams: centavos sem erro de arredondamento de double', () {
      final options = PagseguroOptions(activationCode: '403938', isPrinterEstablishment: true);
      expect(PagseguroPageParams(amount: 19.99, type: PagseguroPaymentType.debit, options: options).amountInCentsOrRounded, 1999);
      expect(PagseguroPageParams(amount: 0.29, type: PagseguroPaymentType.debit, options: options).amountInCentsOrRounded, 29);
      expect(PagseguroPageParams(amount: 1.005, amountInCents: 101, type: PagseguroPaymentType.debit, options: options).amountInCentsOrRounded, 101);
      final params = PagseguroPageParams(amount: 10, parcels: 3, creditType: PaymentTypeCredit.SALESMAN, type: PagseguroPaymentType.creditParc, options: options);
      expect(params.parcels, 3);
      expect(params.creditType, PaymentTypeCredit.SALESMAN);
      expect(params.style, isNull);
      expect(params.options.activationCode, '403938');
      expect(params.options.isPrinterEstablishment, isTrue);
    });

    test('PagseguroStyle padrão', () {
      const style = PagseguroStyle();
      expect(style.backgroundColor, const Color.fromARGB(255, 55, 169, 100));
      expect(style.textColor, Colors.white);
      expect(style.buttonColor, const Color.fromARGB(255, 51, 128, 82));
      expect(style.buttonTextColor, Colors.white);
      expect(style.icon, isNull);
      expect(style.iconColor, Colors.white);
    });

    test('PagseguroHandler repassa os eventos relevantes', () {
      final transactions = <String>[];
      final messages = <String>[];
      final handler = PagseguroHandler(
        onTransaction: (method, value) => transactions.add('$method:$value'),
        onMessageMonitor: messages.add,
      );
      handler.onAuthProgress('a');
      handler.onError('e');
      handler.onFinishedResponse('f');
      handler.onTransactionInfo(response: 'r');
      handler.onTransactionInfo();
      handler.onMessage('m');
      handler.onLoading(true);
      handler.disposeDialog();
      handler.onAbortedSuccessfully();
      handler.onActivationDialog();
      handler.onTransactionSuccess();
      handler.writeToFile();
      expect(transactions, ['onAuthProgress:a', 'onError:e', 'onFinishedResponse:f', 'onTransactionInfo:r', 'onTransactionInfo:']);
      expect(messages, ['m']);
    });

    test('PagseguroStrings', () {
      expect(PagseguroStrings.pinpadActivate, 'terminal ativado');
      expect(PagseguroStrings.pinpadNotActivate, 'terminal não ativado');
      expect(PagseguroStrings.approach, 'aproxime');
      expect(PagseguroStrings.operationCanceled, 'operacao cancelada');
      expect(PagseguroStrings.notAuthorized, 'não autorizada');
      expect(PagseguroStrings.authorized, 'autorizado');
      expect(PagseguroStrings.remove, 'retire');
    });
  });
}
