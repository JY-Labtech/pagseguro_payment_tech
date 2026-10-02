import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

import 'fake_payment_handler.dart';

const _channel = MethodChannel('pagseguro_payment_tech');

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  var licenceValid = true;

  setUp(() {
    messenger.setMockMethodCallHandler(_channel, (call) async {
      if (call.method == 'initPayment' || licenceValid) return true;
      throw PlatformException(code: 'LICENCE_ERROR', message: 'Licença cancelada');
    });
  });

  tearDown(() => messenger.setMockMethodCallHandler(_channel, null));

  test('operações NFC propagam LICENCE_ERROR', () async {
    final payment = PagseguroPaymentTech.I.initPayment(handler: FakePaymentHandler(), licenceKey: 'test-licence-key-0000');
    expect(await payment.licenceReady, isTrue);
    final nfc = PagseguroPaymentTech.I.initNfc(handler: _NoopNfcHandler());

    licenceValid = false;
    final licenceError = throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'LICENCE_ERROR'));
    await expectLater(nfc.readNfc(1), licenceError);
    await expectLater(nfc.writeNfc(1, 'n', 'c', 't', 'cel', true, 1), licenceError);
    await expectLater(nfc.reWriteNfc(1, 1), licenceError);
    await expectLater(nfc.refundNfc(1, 1), licenceError);
    await expectLater(nfc.formatNfc(), licenceError);
    await expectLater(nfc.debitNfc(1, 1), licenceError);
  });
}

class _NoopNfcHandler implements NfcHandler {
  @override
  dynamic noSuchMethod(Invocation invocation) => null;
}
