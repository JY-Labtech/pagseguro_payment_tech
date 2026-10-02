import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

import 'fake_payment_handler.dart';

const _channel = MethodChannel('pagseguro_payment_tech');

/// Contrato do canal após a correção da 3.3.0: com licença válida, o nativo completa o result
/// de debitPayment, dos métodos NFC e dos printer* com `true` (o resultado da operação continua
/// chegando pelos eventos do handler).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  late List<MethodCall> calls;

  /// Simula o PagseguroPaymentTechNativePlugin corrigido: toda operação responde `true`.
  Future<Object?> fixedNative(MethodCall call) async {
    calls.add(call);
    return true;
  }

  setUp(() {
    calls = [];
    messenger.setMockMethodCallHandler(_channel, fixedNative);
  });

  tearDown(() => messenger.setMockMethodCallHandler(_channel, null));

  test('debitPayment completa com true', () async {
    final payment = PagseguroPaymentTech.I.initPayment(handler: FakePaymentHandler(), licenceKey: 'test-licence-key-0000');
    expect(await payment.licenceReady, isTrue);

    expect(await payment.debitPayment(1500).timeout(const Duration(seconds: 1)), isTrue);
    final call = calls.singleWhere((c) => c.method == 'paymentDebit');
    expect(call.arguments['value'], 1500);
  });

  test('métodos NFC completam com true', () async {
    final payment = PagseguroPaymentTech.I.initPayment(handler: FakePaymentHandler(), licenceKey: 'test-licence-key-0000');
    expect(await payment.licenceReady, isTrue);
    final nfc = PagseguroPaymentTech.I.initNfc(handler: _NoopNfcHandler());
    const timeout = Duration(seconds: 1);

    expect(await nfc.readNfc('evento').timeout(timeout), isTrue);
    expect(await nfc.writeNfc('500', 'Cliente', '123', 'TAG', '119', 'true', 'evento').timeout(timeout), isTrue);
    expect(await nfc.reWriteNfc('500', 'evento').timeout(timeout), isTrue);
    expect(await nfc.refundNfc('500', 'evento').timeout(timeout), isTrue);
    expect(await nfc.formatNfc().timeout(timeout), isTrue);
    expect(await nfc.debitNfc('evento', '500').timeout(timeout), isTrue);
  });

  test('printer* devolvem true quando o nativo responde success', () async {
    final payment = PagseguroPaymentTech.I.initPayment(handler: FakePaymentHandler(), licenceKey: 'test-licence-key-0000');
    expect(await payment.licenceReady, isTrue);

    expect(await payment.printerfromFile('/a.png'), isTrue);
    expect(await payment.printerFile('a.png'), isTrue);
    expect(await payment.printer('/b.png'), isTrue);
    expect(await payment.printerFilePath('/c.png'), isTrue);
    expect(
      calls.where((c) => c.method.startsWith('paymentPrinter')).map((c) => '${c.method}:${c.arguments['path']}'),
      [
        'paymentPrinterFile:/a.png',
        'paymentPrinter:a.png',
        'paymentPrinterBasic:/b.png',
        'paymentPrinterFilePath:/c.png',
      ],
    );
  });

  test('printer* devolvem false se o nativo não implementa o método (comportamento antigo)', () async {
    final payment = PagseguroPaymentTech.I.initPayment(handler: FakePaymentHandler(), licenceKey: 'test-licence-key-0000');
    expect(await payment.licenceReady, isTrue);
    messenger.setMockMethodCallHandler(_channel, (call) async {
      if (call.method.startsWith('paymentPrinter')) throw MissingPluginException();
      return true;
    });

    expect(await payment.printerfromFile('/a.png'), isFalse);
    expect(await payment.printer('/b.png'), isFalse);
  });
}

class _NoopNfcHandler implements NfcHandler {
  @override
  dynamic noSuchMethod(Invocation invocation) => null;
}
