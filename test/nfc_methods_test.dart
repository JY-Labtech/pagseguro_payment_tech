import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

import 'helpers/fake_native.dart';

/// Cada método público de [Nfc]: nome/argumentos, sucesso e erro.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeNative native;
  late RecordingNfcHandler nfcHandler;
  late Nfc nfc;

  setUp(() async {
    PagseguroPaymentTech.resetForTesting();
    native = FakeNative();
    await initPayment(native, RecordingPaymentHandler());
    nfcHandler = RecordingNfcHandler();
    nfc = PagseguroPaymentTech.I.initNfc(handler: nfcHandler);
  });

  tearDown(() {
    native.dispose();
    PagseguroPaymentTech.resetForTesting();
  });

  final nfcCalls = <String, Future<bool> Function(Nfc n)>{
    'paymentReadNfc': (n) => n.readNfc('EV1'),
    'paymentWriteNfc': (n) => n.writeNfc('1000', 'Ana', '123', 'TAG1', '1199', '1', 'EV1'),
    'paymentReWriteNfc': (n) => n.reWriteNfc('500', 'EV1'),
    'paymentReFundNfc': (n) => n.refundNfc('300', 'EV1'),
    'paymentFormatNfc': (n) => n.formatNfc(),
    'paymentDebitNfc': (n) => n.debitNfc('EV1', '200'),
    'paymentAbort': (n) => n.abortTransaction(),
    'paymentLastTransaction': (n) => n.lastTransaction(),
    'paymentRefund': (n) => n.refund(transactionCode: 'TC', transactionId: 'TI'),
  };

  final expectedArgs = <String, Object?>{
    'paymentReadNfc': {'idEvento': 'EV1'},
    'paymentWriteNfc': {'valor': '1000', 'nome': 'Ana', 'cpf': '123', 'numeroTag': 'TAG1', 'celular': '1199', 'aberto': '1', 'idEvento': 'EV1'},
    'paymentReWriteNfc': {'valor': '500', 'idEvento': 'EV1'},
    'paymentReFundNfc': {'valor': '300', 'idEvento': 'EV1'},
    'paymentFormatNfc': null,
    'paymentDebitNfc': {'idEvento': 'EV1', 'valor': '200'},
    'paymentAbort': null,
    'paymentLastTransaction': null,
    'paymentRefund': {'transactionCode': 'TC', 'transactionId': 'TI'},
  };

  for (final entry in nfcCalls.entries) {
    test('${entry.key}: argumentos exatos e true no sucesso', () async {
      expect(await entry.value(nfc), isTrue);
      expect(native.last(entry.key).arguments, expectedArgs[entry.key]);
      expect(native.methods, [entry.key]);
    });

    test('${entry.key}: PlatformException genérica e LICENCE_ERROR são propagadas', () async {
      native.errorForAll = genericError;
      await expectLater(entry.value(nfc), throwsGenericError);
      native.errorForAll = licenceError;
      await expectLater(entry.value(nfc), throwsLicenceError);
    });
  }

  test('argumentos dynamic (números/bool) são enviados como estão', () async {
    await nfc.writeNfc(1000, 'Ana', 123, 7, 11999, true, 1);
    expect(native.last('paymentWriteNfc').arguments, {
      'valor': 1000,
      'nome': 'Ana',
      'cpf': 123,
      'numeroTag': 7,
      'celular': 11999,
      'aberto': true,
      'idEvento': 1,
    });
  });

  test('refund NFC com código/id vazios lança INVALID_ARGUMENT sem chamar o nativo', () async {
    final invalidArgument = throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'INVALID_ARGUMENT'));
    await expectLater(nfc.refund(transactionCode: '', transactionId: 'TI'), invalidArgument);
    await expectLater(nfc.refund(transactionCode: 'TC', transactionId: ' '), invalidArgument);
    expect(native.calls, isEmpty);
  });

  test('setHandler troca o handler NFC', () async {
    final other = RecordingNfcHandler();
    nfc.setHandler(other);
    expect(nfc.handler, same(other));
    await native.sendEvent('showErrorRead', 'cartão removido');
    expect(other.calls, ['showErrorRead:cartão removido']);
    expect(nfcHandler.calls, isEmpty);
  });

  test('sem instância, setHandler e chamadas lançam exceção', () async {
    PagseguroPaymentTech.resetForTesting();
    expect(() => nfc.setHandler(RecordingNfcHandler()), throwsException);
    await expectLater(nfc.readNfc('1'), throwsException);
  });
}
