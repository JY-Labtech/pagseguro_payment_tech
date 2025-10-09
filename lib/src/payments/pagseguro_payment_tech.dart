import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/src/payments/nfc.dart';

import 'handler/payment_handler.dart';
import 'payment.dart';

export 'package:pagseguro_payment_tech/src/payments/print_render_widget.dart';

// ignore: constant_identifier_names
const String CHANNEL_NAME = 'pagseguro_payment_tech';

class PagseguroPaymentTech {
  static PagseguroPaymentTech? _instance;

  static PagseguroPaymentTech get I {
    _instance ??= PagseguroPaymentTech._internal();
    return _instance!;
  }

  late final MethodChannel _channel;
  Payment? _payment;
  Nfc? _nfc;

  PagseguroPaymentTech._internal() {
    _channel = const MethodChannel(CHANNEL_NAME);
  }

  /// Inicializa o pagamento e registra o handler
  Payment initPayment({
    required PaymentHandler handler,
  }) {
    if (_payment != null) {
      return _payment!;
    }
    _payment = Payment.create(channel: _channel, paymentHandler: handler);
    return _payment!;
  }

  /// Inicializa o NFC e registra o handler
  Nfc initNfc({
    required NfcHandler handler,
  }) {
    if (_nfc != null) {
      return _nfc!;
    }
    _nfc = Nfc.internal(
      channel: _channel,
      nfcHandler: handler,
      payment: _payment,
    );
    return _nfc!;
  }

  Payment get payment {
    if (_payment == null) {
      throw Exception(
        "Payment não inicializado.\nChame: PagseguroPaymentTech.instance.initPayment(handler)",
      );
    }
    return _payment!;
  }

  Nfc get nfc {
    if (_nfc == null) {
      throw Exception(
        "NFC não inicializado.\nChame: PagseguroPaymentTech.instance.initNfc(handler)",
      );
    }
    return _nfc!;
  }
}
