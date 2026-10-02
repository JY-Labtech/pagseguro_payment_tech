import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';
import 'package:pagseguro_payment_tech/src/licence/constants/licence_constants.dart' show licenceCheck;

import 'fake_payment_handler.dart';

const _channelName = 'pagseguro_payment_tech';
const _channel = MethodChannel(_channelName);
const _licenceKey = 'test-licence-key-0000';
const _coreUrl = 'https://licence-server.invalid/pos-payments/licence/check';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  late List<MethodCall> calls;
  late bool licenceValid;

  /// Simula o nativo: com licença inválida, avisa o handler (onError) e devolve LICENCE_ERROR.
  Future<Object?> fakeNative(MethodCall call) async {
    calls.add(call);
    if (call.method == 'paymentSetPrintDialogMaxTime') return true;
    if (!licenceValid) {
      await messenger.handlePlatformMessage(
        _channelName,
        const StandardMethodCodec().encodeMethodCall(const MethodCall('onError', 'Licença vencida')),
        (_) {},
      );
      throw PlatformException(code: 'LICENCE_ERROR', message: 'Licença vencida');
    }
    switch (call.method) {
      case 'paymentGetSerialNumber':
        return 'SERIAL';
      case 'paymentCalculateInstallments':
        return [
          {'quantity': 1, 'amount': 1000, 'total': 1000},
        ];
      default:
        return true;
    }
  }

  setUp(() {
    calls = [];
    licenceValid = true;
    messenger.setMockMethodCallHandler(_channel, fakeNative);
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(_channel, null);
  });

  test('licenceCheck decodifica para o servidor oficial da JY Labtech', () {
    final url = utf8.decode(base64.decode(licenceCheck));
    expect(url, _coreUrl);
    final uri = Uri.parse(url);
    expect(uri.scheme, 'https');
    expect(uri.host, 'licence-server.invalid');
    expect(uri.path, startsWith('/pos-payments/'));
  });

  test('initPayment envia licenceKey e licenceInternalKey padrão (core) quando omitido', () async {
    final handler = FakePaymentHandler();
    final payment = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: _licenceKey);

    expect(await payment.licenceReady, isTrue);
    final init = calls.singleWhere((c) => c.method == 'initPayment');
    expect(init.arguments['licenceKey'], _licenceKey);
    expect(init.arguments['licenceInternalKey'], licenceCheck);
    expect(init.arguments['maxTimeShowPopUp'], 30);
    expect(handler.errors, isEmpty);
  });

  test('initPayment repassa licenceInternalKey informado', () async {
    final handler = FakePaymentHandler();
    final payment = PagseguroPaymentTech.I.initPayment(
      handler: handler,
      licenceKey: _licenceKey,
      licenceInternalKey: 'custom-value',
    );

    expect(await payment.licenceReady, isTrue);
    final init = calls.lastWhere((c) => c.method == 'initPayment');
    expect(init.arguments['licenceInternalKey'], 'custom-value');
  });

  test('LICENCE_ERROR no initPayment chega ao handler e licenceReady fica false', () async {
    licenceValid = false;
    final handler = FakePaymentHandler();
    final payment = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: _licenceKey);

    expect(await payment.licenceReady, isFalse);
    expect(handler.errors, ['Licença vencida']);
  });

  test('outro erro do nativo no initPayment também chega ao handler', () async {
    messenger.setMockMethodCallHandler(_channel, (call) async {
      throw PlatformException(code: 'ERROR', message: 'falha inesperada');
    });
    final handler = FakePaymentHandler();
    final payment = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: _licenceKey);

    expect(await payment.licenceReady, isFalse);
    expect(handler.errors, ['falha inesperada']);
  });

  group('com licença inválida nenhuma operação devolve sucesso', () {
    late Payment payment;
    late FakePaymentHandler handler;

    setUp(() async {
      handler = FakePaymentHandler();
      payment = PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: _licenceKey);
      await payment.licenceReady;
      licenceValid = false;
      calls.clear();
    });

    final licenceError = isA<PlatformException>().having((e) => e.code, 'code', 'LICENCE_ERROR');

    test('pagamentos, estorno e consultas lançam LICENCE_ERROR', () async {
      await expectLater(payment.creditPayment(1000), throwsA(licenceError));
      await expectLater(payment.creditPaymentParc(1000, 2), throwsA(licenceError));
      await expectLater(payment.debitPayment(1000), throwsA(licenceError));
      await expectLater(payment.pixPayment(1000), throwsA(licenceError));
      await expectLater(payment.voucherPayment(1000), throwsA(licenceError));
      await expectLater(payment.startPayment(PaymentType.TYPE_CREDITO, 1000), throwsA(licenceError));
      await expectLater(payment.refund(transactionCode: 'c', transactionId: 'i'), throwsA(licenceError));
      await expectLater(payment.abortTransaction(), throwsA(licenceError));
      await expectLater(payment.lastTransaction(), throwsA(licenceError));
      await expectLater(payment.isAuthenticated(), throwsA(licenceError));
      await expectLater(payment.getUserData(), throwsA(licenceError));
      expect(handler.errors, everyElement('Licença vencida'));
      expect(handler.transactionSuccess, 0);
    });

    test('métodos que retornam bool/valor padrão devolvem falha', () async {
      expect(await payment.activePinpad('123'), isFalse);
      expect(await payment.printerfromFile('/tmp/a.png'), isFalse);
      expect(await payment.printerFile('a.png'), isFalse);
      expect(await payment.printer('/tmp/a.png'), isFalse);
      expect(await payment.printerFilePath('/tmp/a.png'), isFalse);
      expect(await payment.getSerialNumber(), '');
      expect(await payment.rebootDevice(), isFalse);
      expect(await payment.beep(), isFalse);
      expect(await payment.calculateInstallments('1000', InstallmentType.A_VISTA), isEmpty);
    });

    test('com licença válida as mesmas chamadas passam', () async {
      licenceValid = true;
      expect(await payment.creditPayment(1000), isTrue);
      expect(await payment.getSerialNumber(), 'SERIAL');
      expect(await payment.calculateInstallments('1000', InstallmentType.A_VISTA), hasLength(1));
    });
  });
}
