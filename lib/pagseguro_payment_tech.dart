import 'package:check_licence_tech/check_licence_tech.dart';
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/payments/handler/nfc_handler.dart';
import 'package:pagseguro_payment_tech/payments/nfc.dart';

import 'payments/handler/payment_handler.dart';
import 'payments/payment.dart';

export 'package:pagseguro_payment_tech/payments/print_render_widget.dart';

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

  final _check = CheckLicenceTech.instance();

  Future<String> getSerialNumber() async => await _check.getSerialNumber();
  Future<bool> wipeLicenceLocal() async => await _check.wipeLicence();

  /// Inicializa o pagamento e registra o handler
  Future<Payment> initPayment({
    required PaymentHandler handler,
    required String licenceKey,
  }) async {
    try {
      if (_payment != null) {
        return _payment!;
      }
      _payment = await Payment.create(channel: _channel, paymentHandler: handler, licenceKey: licenceKey);
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
      if (_nfc != null) {
        return _nfc!;
      }
      _nfc = await Nfc.internal(
        channel: _channel,
        nfcHandler: handler,
        licenceKey: licenceKey,
        payment: _payment,
      );
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
