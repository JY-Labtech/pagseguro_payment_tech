import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';
import 'package:pagseguro_payment_tech/src/licence/constants/licence_constants.dart' show licenceCheck;

import 'helpers/fake_native.dart';

/// Cada método público de [Payment]: (a) nome e argumentos enviados ao nativo,
/// (b) tratamento do sucesso e (c) tratamento do erro (PlatformException genérica e LICENCE_ERROR).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeNative native;
  late RecordingPaymentHandler handler;
  late Payment payment;

  setUp(() async {
    PagseguroPaymentTech.resetForTesting();
    native = FakeNative();
    handler = RecordingPaymentHandler();
    payment = await initPayment(native, handler);
  });

  tearDown(() {
    native.dispose();
    PagseguroPaymentTech.resetForTesting();
  });

  Map<String, dynamic> argsOf(String method) => Map<String, dynamic>.from(native.last(method).arguments as Map);

  group('initPayment / licença', () {
    test('envia licenceKey, licenceInternalKey padrão e maxTimeShowPopUp', () async {
      PagseguroPaymentTech.resetForTesting();
      final p = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: 'KEY', maxTimeShowPopUp: 45);
      expect(await p.licenceReady, isTrue);
      expect(native.last('initPayment').arguments, {
        'licenceKey': 'KEY',
        'licenceInternalKey': licenceCheck,
        'maxTimeShowPopUp': 45,
      });
      expect(PagseguroPaymentTech.I.payment, same(p));
    });

    test('nativo respondendo algo diferente de true deixa licenceReady false', () async {
      native.responses['initPayment'] = (_) => null;
      final p = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: 'KEY');
      expect(await p.licenceReady, isFalse);
    });

    test('LICENCE_ERROR no initPayment não duplica o onError (o nativo já enviou)', () async {
      native.responses['initPayment'] = (_) => throw licenceError;
      final p = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: 'KEY');
      expect(await p.licenceReady, isFalse);
      expect(handler.calls.where((c) => c.startsWith('onError')), isEmpty);
    });

    test('erro inesperado (não PlatformException) vira onError', () async {
      native.responses['initPayment'] = (_) => throw StateError('falhou');
      final p = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: 'KEY');
      expect(await p.licenceReady, isFalse);
      expect(handler.calls.single, startsWith('onError:'));
    });

    test('setHandler troca o handler e revalida a licença', () async {
      final other = RecordingPaymentHandler();
      payment.setHandler(other);
      expect(await payment.licenceReady, isTrue);
      expect(native.methods, ['initPayment']);
      await native.sendEvent('onMessage', 'oi');
      expect(other.calls, ['onMessage:oi']);
      expect(handler.calls, isEmpty);
    });

    test('initPayment de novo reaproveita a instância e atualiza chaves e handler', () async {
      final other = RecordingPaymentHandler();
      final again = PagseguroPaymentTech.I.initPayment(handler: other, licenceKey: 'OUTRA', maxTimeShowPopUp: 10);
      expect(again, same(payment));
      await again.licenceReady;
      expect(native.last('initPayment').arguments['licenceKey'], 'OUTRA');
      expect(native.last('initPayment').arguments['maxTimeShowPopUp'], 10);
      expect(again.paymentHandler, same(other));
    });
  });

  group('pagamentos', () {
    test('creditPayment: argumentos, padrões e userReference limitado a 10 caracteres', () async {
      expect(await payment.creditPayment(2590), isTrue);
      expect(argsOf('paymentCredit'), {'value': 2590, 'userReference': 'SMARTPOS', 'printReceipt': true, 'partialPay': false, 'isCarne': false});

      await payment.creditPayment(100, userReference: 'PEDIDO123456789', printReceipt: false, partialPay: true, isCarne: true);
      expect(argsOf('paymentCredit'), {'value': 100, 'userReference': 'PEDIDO1234', 'printReceipt': false, 'partialPay': true, 'isCarne': true});
    });

    test('creditPaymentParc: parcelas e tipo (CLIENT=3, SALESMAN=2)', () async {
      expect(await payment.creditPaymentParc(3000, 3), isTrue);
      expect(argsOf('paymentCreditParc'), {'value': 3000, 'parc': 3, 'type': 3, 'userReference': 'SMARTPOS', 'printReceipt': true, 'partialPay': false, 'isCarne': false});
      await payment.creditPaymentParc(3000, 6, type: PaymentTypeCredit.SALESMAN, userReference: 'R1');
      expect(argsOf('paymentCreditParc')['type'], 2);
      expect(argsOf('paymentCreditParc')['parc'], 6);
      expect(argsOf('paymentCreditParc')['userReference'], 'R1');
    });

    test('userReference: só letras e números, até 10; vazio vira SMARTPOS', () async {
      const cases = {
        'PED-123/ABC': 'PED123ABC',
        'Pedido #4567 89012': 'Pedido4567',
        'ção-çã': 'o',
        '---': 'SMARTPOS',
        '': 'SMARTPOS',
        'ABC123': 'ABC123',
      };
      for (final entry in cases.entries) {
        await payment.debitPayment(100, userReference: entry.key);
        expect(argsOf('paymentDebit')['userReference'], entry.value, reason: entry.key);
      }
      await payment.startPayment(PaymentType.TYPE_PIX, 100, userReference: 'a.b.c');
      expect(argsOf('startPayment')['userReference'], 'abc');
      await payment.creditPaymentParc(100, 2, userReference: 'x y');
      expect(argsOf('paymentCreditParc')['userReference'], 'xy');
    });

    test('debitPayment, pixPayment e voucherPayment', () async {
      expect(await payment.debitPayment(1500), isTrue);
      expect(await payment.pixPayment(2000, printReceipt: false), isTrue);
      expect(await payment.voucherPayment(2500, isCarne: true), isTrue);
      expect(argsOf('paymentDebit'), {'value': 1500, 'userReference': 'SMARTPOS', 'printReceipt': true, 'partialPay': false, 'isCarne': false});
      expect(argsOf('paymentPix'), {'value': 2000, 'userReference': 'SMARTPOS', 'printReceipt': false, 'partialPay': false, 'isCarne': false});
      expect(argsOf('paymentVoucher'), {'value': 2500, 'userReference': 'SMARTPOS', 'printReceipt': true, 'partialPay': false, 'isCarne': true});
    });

    test('startPayment converte enums em inteiros do SDK', () async {
      expect(await payment.startPayment(PaymentType.TYPE_PIX, 990), isTrue);
      expect(argsOf('startPayment'), {
        'type': 5,
        'amount': 990,
        'installmentType': 3,
        'installments': 1,
        'userReference': 'SMARTPOS',
        'printReceipt': true,
        'partialPay': false,
        'isCarne': false,
      });
      await payment.startPayment(PaymentType.TYPE_CREDITO, 5000, installmentType: PaymentTypeCredit.SALESMAN, installments: 4, userReference: 'ABCDEFGHIJKLMNOP');
      expect(argsOf('startPayment'), containsPair('installmentType', 2));
      expect(argsOf('startPayment'), containsPair('installments', 4));
      expect(argsOf('startPayment'), containsPair('userReference', 'ABCDEFGHIJ'));
    });

    test('resposta nula do nativo vira false em vez de TypeError', () async {
      native.defaultResponse = null;
      expect(await payment.debitPayment(100), isFalse);
      expect(await payment.abortTransaction(), isFalse);
    });

    final paymentCalls = <String, Future<bool> Function(Payment p)>{
      'creditPayment': (p) => p.creditPayment(1),
      'creditPaymentParc': (p) => p.creditPaymentParc(1, 2),
      'debitPayment': (p) => p.debitPayment(1),
      'pixPayment': (p) => p.pixPayment(1),
      'voucherPayment': (p) => p.voucherPayment(1),
      'startPayment': (p) => p.startPayment(PaymentType.TYPE_DEBITO, 1),
      'abortTransaction': (p) => p.abortTransaction(),
      'lastTransaction': (p) => p.lastTransaction(),
      'refund': (p) => p.refund(transactionCode: 'c', transactionId: 'i'),
      'isAuthenticated': (p) => p.isAuthenticated(),
      'getUserData': (p) => p.getUserData(),
      'setPrintDialogMaxTime': (p) => p.setPrintDialogMaxTime(maxTimeShowPopUp: 10),
      'setDialogstyle': (p) => p.setDialogstyle(
            title: 't',
            buttonBackgroundColor: '#000',
            buttonBackgroundColorDisabled: '#111',
            cancelTextColor: '#222',
            confirmTextColor: '#333',
            sendSMSTextColor: '#444',
            titleColor: '#555',
            windowBackgroundColor: '#666',
            maxTimeShowPopUp: 20,
          ),
    };

    for (final entry in paymentCalls.entries) {
      test('${entry.key}: PlatformException genérica e LICENCE_ERROR são propagadas', () async {
        native.errorForAll = genericError;
        await expectLater(entry.value(payment), throwsGenericError);
        native.errorForAll = licenceError;
        await expectLater(entry.value(payment), throwsLicenceError);
        expect(handler.calls, isNot(contains('onTransactionSuccess')));
      });
    }
  });

  group('operações', () {
    test('abortTransaction, lastTransaction, isAuthenticated e getUserData não enviam argumentos', () async {
      expect(await payment.abortTransaction(), isTrue);
      expect(await payment.lastTransaction(), isTrue);
      expect(await payment.isAuthenticated(), isTrue);
      expect(await payment.getUserData(), isTrue);
      expect(native.methods, ['paymentAbort', 'paymentLastTransaction', 'paymentIsAuthenticated', 'paymentGetUserData']);
      expect(native.calls.map((c) => c.arguments), everyElement(isNull));
    });

    test('refund envia código e id', () async {
      expect(await payment.refund(transactionCode: 'TC', transactionId: 'TI'), isTrue);
      expect(argsOf('paymentRefund'), {'transactionCode': 'TC', 'transactionId': 'TI'});
    });

    test('refund com código/id nulos ou vazios lança INVALID_ARGUMENT sem chamar o nativo', () async {
      final invalidArgument = throwsA(isA<PlatformException>().having((e) => e.code, 'code', Payment.invalidArgumentCode));
      await expectLater(payment.refund(), invalidArgument);
      await expectLater(payment.refund(transactionCode: 'TC'), invalidArgument);
      await expectLater(payment.refund(transactionId: 'TI'), invalidArgument);
      await expectLater(payment.refund(transactionCode: '', transactionId: 'TI'), invalidArgument);
      await expectLater(payment.refund(transactionCode: 'TC', transactionId: '  '), invalidArgument);
      expect(native.calls, isEmpty);
    });

    test('setPrintDialogMaxTime envia o tempo (padrão 30)', () async {
      expect(await payment.setPrintDialogMaxTime(), isTrue);
      expect(argsOf('paymentSetPrintDialogMaxTime'), {'maxTimeShowPopUp': 30});
      await payment.setPrintDialogMaxTime(maxTimeShowPopUp: 90);
      expect(argsOf('paymentSetPrintDialogMaxTime'), {'maxTimeShowPopUp': 90});
    });

    test('setDialogstyle envia todas as cores, título e tempo', () async {
      expect(
        await payment.setDialogstyle(
          title: 'Via do cliente',
          buttonBackgroundColor: '#00AA00',
          buttonBackgroundColorDisabled: '#CCCCCC',
          cancelTextColor: '#FF0000',
          confirmTextColor: '#FFFFFF',
          sendSMSTextColor: '#0000FF',
          titleColor: '#000000',
          windowBackgroundColor: '#EEEEEE',
          maxTimeShowPopUp: 15,
        ),
        isTrue,
      );
      expect(argsOf('paymentStyleDialog'), {
        'title': 'Via do cliente',
        'buttonBackgroundColor': '#00AA00',
        'buttonBackgroundColorDisabled': '#CCCCCC',
        'cancelTextColor': '#FF0000',
        'confirmTextColor': '#FFFFFF',
        'sendSMSTextColor': '#0000FF',
        'titleColor': '#000000',
        'windowBackgroundColor': '#EEEEEE',
        'maxTimeShowPopUp': 15,
      });
    });

    test('activePinpad envia o código; erro devolve false', () async {
      expect(await payment.activePinpad('403938'), isTrue);
      expect(argsOf('paymentActivePinpad'), {'code': '403938'});
      native.errorForAll = genericError;
      expect(await payment.activePinpad('403938'), isFalse);
      native.errorForAll = licenceError;
      expect(await payment.activePinpad('403938'), isFalse);
    });

    test('rebootDevice e beep: true no sucesso, false em erro', () async {
      expect(await payment.rebootDevice(), isTrue);
      expect(await payment.beep(), isTrue);
      expect(native.methods, ['paymentReboot', 'paymentBeep']);
      native.errorForAll = genericError;
      expect(await payment.rebootDevice(), isFalse);
      expect(await payment.beep(), isFalse);
      native.errorForAll = licenceError;
      expect(await payment.beep(), isFalse);
    });

    test('getSerialNumber devolve o serial; nulo ou erro viram string vazia', () async {
      native.responses['paymentGetSerialNumber'] = (_) => 'PBA123';
      expect(await payment.getSerialNumber(), 'PBA123');
      native.responses['paymentGetSerialNumber'] = (_) => null;
      expect(await payment.getSerialNumber(), '');
      native.responses['paymentGetSerialNumber'] = (_) => throw genericError;
      expect(await payment.getSerialNumber(), '');
      native.responses['paymentGetSerialNumber'] = (_) => throw licenceError;
      expect(await payment.getSerialNumber(), '');
    });
  });

  group('impressão', () {
    test('cada printer* usa o método e o path corretos', () async {
      expect(await payment.printerfromFile('/sdcard/a.png'), isTrue);
      expect(await payment.printerFile('b.png'), isTrue);
      expect(await payment.printer('/sdcard/c.png'), isTrue);
      expect(await payment.printerFilePath('/sdcard/d.png'), isTrue);
      expect(native.calls.map((c) => '${c.method}:${c.arguments['path']}'), [
        'paymentPrinterFile:/sdcard/a.png',
        'paymentPrinter:b.png',
        'paymentPrinterBasic:/sdcard/c.png',
        'paymentPrinterFilePath:/sdcard/d.png',
      ]);
    });

    test('erro genérico ou de licença devolve false', () async {
      for (final error in [genericError, licenceError]) {
        native.errorForAll = error;
        expect(await payment.printerfromFile('/a'), isFalse);
        expect(await payment.printerFile('a'), isFalse);
        expect(await payment.printer('/a'), isFalse);
        expect(await payment.printerFilePath('/a'), isFalse);
      }
    });
  });

  group('calculateInstallments', () {
    test('aceita InstallmentType ou int e parseia a lista', () async {
      native.responses['paymentCalculateInstallments'] = (_) => [
            {'quantity': 1, 'amount': 2000, 'total': 2000},
            {'quantity': 2, 'amount': 1050, 'total': 2100},
          ];
      final list = await payment.calculateInstallments('2000', InstallmentType.PARC_COMPRADOR);
      expect(argsOf('paymentCalculateInstallments'), {'saleValue': '2000', 'installmentType': 3});
      expect(list, [
        PlugPagInstallment(quantity: 1, amount: 2000, total: 2000),
        PlugPagInstallment(quantity: 2, amount: 1050, total: 2100),
      ]);

      await payment.calculateInstallments('2000', 2);
      expect(argsOf('paymentCalculateInstallments')['installmentType'], 2);
    });

    test('tipo inválido não chama o nativo e devolve lista vazia', () async {
      expect(await payment.calculateInstallments('2000', 'x'), isEmpty);
      expect(native.calls, isEmpty);
    });

    test('resposta que não é lista, erro genérico ou LICENCE_ERROR devolvem lista vazia', () async {
      native.responses['paymentCalculateInstallments'] = (_) => 'nada';
      expect(await payment.calculateInstallments('2000', 1), isEmpty);
      native.responses['paymentCalculateInstallments'] = (_) => throw genericError;
      expect(await payment.calculateInstallments('2000', 1), isEmpty);
      native.responses['paymentCalculateInstallments'] = (_) => throw licenceError;
      expect(await payment.calculateInstallments('2000', 1), isEmpty);
    });
  });

  group('PagseguroPaymentTech', () {
    test('I é singleton e payment devolve a instância criada', () {
      expect(PagseguroPaymentTech.I, same(PagseguroPaymentTech.I));
      expect(PagseguroPaymentTech.I.payment, same(payment));
    });

    test('initNfc exige payment e é idempotente', () {
      PagseguroPaymentTech.resetForTesting();
      expect(() => PagseguroPaymentTech.I.initNfc(handler: RecordingNfcHandler()), throwsException);
      expect(() => PagseguroPaymentTech.I.nfc, throwsException);
      PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: 'KEY');
      final nfc = PagseguroPaymentTech.I.initNfc(handler: RecordingNfcHandler());
      final other = RecordingNfcHandler();
      expect(PagseguroPaymentTech.I.initNfc(handler: other), same(nfc));
      expect(PagseguroPaymentTech.I.nfc, same(nfc));
      expect(nfc.handler, same(other));
    });

    test('constantes do canal', () {
      expect(CHANNEL_NAME, channelName);
      expect(CHANNEL_NFC_NAME, channelName);
      expect(Payment.licenceErrorCode, 'LICENCE_ERROR');
    });
  });

  test('chamadas são feitas no canal pagseguro_payment_tech', () {
    expect(payment.channel.name, channelName);
    expect(payment.channel.codec, isA<StandardMethodCodec>());
  });
}
