import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/licence/licence_service.dart';

import 'package:pagseguro_payment_tech/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/payments/nfc.dart';
import 'payments/handler/payment_handler.dart';
import 'payments/payment.dart';

export 'package:pagseguro_payment_tech/payments/print_render_widget.dart';

// ignore: constant_identifier_names
const String CHANNEL_NAME = 'pagseguro_payment_tech';

class PagseguroPaymentTech {
  static final PagseguroPaymentTech _instance = PagseguroPaymentTech._internal();

  factory PagseguroPaymentTech() => _instance;

  static PagseguroPaymentTech get instance => _instance;

  late final MethodChannel _channel;
  Payment? _payment;
  Nfc? _nfc;

  PagseguroPaymentTech._internal() {
    _channel = const MethodChannel(CHANNEL_NAME);
  }

  /// Inicializa o pagamento e registra o handler
  Future<Payment> initPayment({
    required PaymentHandler handler,
    required String licenceKey,
  }) async {
    try {
      final isValid = await LicenceService().checkLicence(licenceKey: licenceKey);
      if (!isValid) {
        throw Exception('Invalid licence key. Please check your licence key.');
      }

      _payment = Payment(channel: _channel, paymentHandler: handler);
      return _payment!;
    } catch (e) {
      print('Error initializing payment: $e');
      rethrow;
    }
  }

  /// Inicializa o NFC e registra o handler
  Future<Nfc> initNfc({
    required NfcHandler handler,
    required String licenceKey,
  }) async {
    try {
      final isValid = await LicenceService().checkLicence(licenceKey: licenceKey);
      if (!isValid) {
        throw Exception('Invalid licence key. Please check your licence key.');
      }

      _nfc = Nfc(channel: _channel, nfcHandler: handler);
      return _nfc!;
    } catch (e) {
      print('Error initializing payment: $e');
      rethrow;
    }
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
