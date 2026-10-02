import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/event_dispatcher.dart';
import 'package:pagseguro_payment_tech/src/licence/constants/licence_constants.dart' show licenceCheck;
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

  /// Inicializa o pagamento, registra o handler e valida a licença no servidor da JY Labtech.
  /// [licenceInternalKey] é opcional (padrão: servidor oficial). Valores fora do servidor
  /// oficial, inclusive o endereço antigo, são ignorados pelo nativo.
  /// [maxTimeShowPopUp] tempo em segundos do dialog de impressão da via do cliente (padrão: 30).
  /// Sem licença válida, toda operação falha com `PlatformException(code: 'LICENCE_ERROR')`.
  Payment initPayment({
    required PaymentHandler handler,
    required String licenceKey,
    String licenceInternalKey = licenceCheck,
    int maxTimeShowPopUp = 30,
  }) {
    final payment = Payment.create(
      channel: _channel,
      paymentHandler: handler,
      licenceKey: licenceKey,
      licenceInternalKey: licenceInternalKey,
      maxTimeShowPopUp: maxTimeShowPopUp,
    );
    _payment = payment;
    return payment;
  }

  /// Inicializa o NFC e registra o handler. Chamado de novo, troca o handler.
  /// Os eventos de pagamento continuam chegando ao [PaymentHandler] mesmo com o NFC ativo.
  Nfc initNfc({
    required NfcHandler handler,
  }) {
    if (_nfc != null) {
      _nfc!.setHandler(handler);
      return _nfc!;
    }
    if (_payment == null) {
      throw Exception(
        "Payment não inicializado.\nChame: PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: '...')",
      );
    }
    _nfc = Nfc.internal(
      channel: _channel,
      nfcHandler: handler,
      payment: _payment,
    );
    return _nfc!;
  }

  /// Descarta Payment, Nfc e handlers registrados (somente testes).
  @visibleForTesting
  static void resetForTesting() {
    _instance?._payment = null;
    _instance?._nfc = null;
    Payment.resetForTesting();
    Nfc.resetForTesting();
    PagseguroEventDispatcher.reset();
  }

  Payment get payment {
    if (_payment == null) {
      throw Exception(
        "Payment não inicializado.\nChame: PagseguroPaymentTech.I.initPayment(handler: handler, licenceKey: '...')",
      );
    }
    return _payment!;
  }

  Nfc get nfc {
    if (_nfc == null) {
      throw Exception(
        "NFC não inicializado.\nChame: PagseguroPaymentTech.I.initNfc(handler)",
      );
    }
    return _nfc!;
  }
}
