import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

import 'fake_payment_handler.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('sem initPayment (licença não configurada) não há Payment nem NFC', () {
    expect(() => PagseguroPaymentTech.I.payment, throwsException);
    expect(() => PagseguroPaymentTech.I.nfc, throwsException);
    expect(
      () => PagseguroPaymentTech.I.initNfc(handler: _UnusedNfcHandler()),
      throwsException,
    );
    // Garante que o fake compila com a interface atual.
    expect(FakePaymentHandler().errors, isEmpty);
  });
}

class _UnusedNfcHandler implements NfcHandler {
  @override
  dynamic noSuchMethod(Invocation invocation) => null;
}
