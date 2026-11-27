// ignore_for_file: public_member_api_docs, sort_constructors_first
// ignore_for_file: constant_identifier_names
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/event_dispatcher.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/payment_handler.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/input_validation.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/payment_types.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/plugpag_installment.dart';

//Fixed channel name

final class Payment {
  final MethodChannel channel;
  PaymentHandler paymentHandler;
  int _maxTimeShowPopUp = 30;
  Future<bool> _ready = Future<bool>.value(false);

  /// Código do erro para argumento obrigatório ausente/vazio (ex.: estorno sem código/id).
  static const String invalidArgumentCode = 'INVALID_ARGUMENT';

  // Cache da instância criada
  static Payment? _instance;

  Payment._({
    required this.channel,
    required this.paymentHandler,
    int maxTimeShowPopUp = 30,
  }) {
    _maxTimeShowPopUp = maxTimeShowPopUp;
  }

  /// Cria (ou reaproveita) a instância única, registra o handler e inicializa o nativo.
  static Payment create({
    required MethodChannel channel,
    required PaymentHandler paymentHandler,
    int maxTimeShowPopUp = 30,
  }) {
    if (_instance == null) {
      final payment = Payment._(
        channel: channel,
        paymentHandler: paymentHandler,
        maxTimeShowPopUp: maxTimeShowPopUp,
      );
      PagseguroEventDispatcher.attachPayment(payment.channel, paymentHandler);
      payment._configure();
      _instance = payment;
    } else {
      _instance!._maxTimeShowPopUp = maxTimeShowPopUp;
      _instance!.paymentHandler = paymentHandler;
      PagseguroEventDispatcher.attachPayment(_instance!.channel, paymentHandler);
      _instance!._configure();
    }

    return _instance!;
  }

  void setHandler(PaymentHandler handler) {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }

    _instance!._updateHandler(handler);
  }

  void _updateHandler(PaymentHandler handler) {
    paymentHandler = handler;
    PagseguroEventDispatcher.attachPayment(channel, handler);
    _configure();
  }

  /// Limpa a instância em cache (uso interno: PagseguroPaymentTech.resetForTesting).
  static void resetForTesting() {
    _instance = null;
  }

  /// Chama um método do nativo que responde `bool`. Resposta nula vira `false` (antes: TypeError).
  /// Erros do nativo são propagados como [PlatformException].
  Future<bool> _invokeBool(String method, [Map<String, dynamic>? arguments]) async {
    final result = await channel.invokeMethod<bool>(method, arguments);
    return result ?? false;
  }

  /// Chama um método do nativo e devolve `true` se ele completar, `false` em qualquer erro.
  Future<bool> _invokeOrFalse(String method, [Map<String, dynamic>? arguments]) async {
    try {
      await channel.invokeMethod(method, arguments);
      return true;
    } catch (_) {
      return false;
    }
  }

  Map<String, dynamic> _paymentArguments(int value, String userReference, bool printReceipt, bool partialPay, bool isCarne) => {
        "value": value,
        "userReference": _sanitizeUserReference(userReference),
        "printReceipt": printReceipt,
        "partialPay": partialPay,
        "isCarne": isCarne,
      };

  /// Resultado da última inicialização do nativo disparada por `initPayment`/`setHandler`.
  /// `true` quando o nativo respondeu; `false` em qualquer erro (o erro também é entregue em
  /// [PaymentHandler.onError]).
  Future<bool> get ready => _ready;

  void _configure() {
    _ready = _initNative();
  }

  Future<bool> _initNative() async {
    try {
      final result = await channel.invokeMethod("initPayment", {
        "maxTimeShowPopUp": _maxTimeShowPopUp,
      });
      return result == true;
    } on PlatformException catch (e) {
      paymentHandler.onError(e.message ?? e.code);
      return false;
    } catch (e) {
      paymentHandler.onError(e.toString());
      return false;
    }
  }

  /// Define o tempo máximo (em segundos) do dialog de impressão da via do cliente.
  /// Padrão: 30 segundos.
  Future<bool> setPrintDialogMaxTime({int maxTimeShowPopUp = 30}) async {
    _maxTimeShowPopUp = maxTimeShowPopUp;
    return _invokeBool(
      PaymentTypeCall.SET_PRINT_DIALOG_MAX_TIME.method,
      {"maxTimeShowPopUp": maxTimeShowPopUp},
    );
  }

  //Create external functions from invoke methodChannel
  //Function to active pinpad with sdk the PagSeguro
  Future<bool> activePinpad(String activationCode) => _invokeOrFalse(PaymentTypeCall.ACTIVEPINPAD.method, {"code": activationCode});

  //Function to invoke method from credit payment with sdk the PagSeguro
  Future<bool> creditPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) {
    return _invokeBool(PaymentTypeCall.CREDIT.method, _paymentArguments(value, userReference, printReceipt, partialPay, isCarne));
  }

  //Function to invoke method from credit installment payment  with sdk the PagSeguro
  Future<bool> creditPaymentParc(int value, int parc, {PaymentTypeCredit type = PaymentTypeCredit.CLIENT, String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) {
    return _invokeBool(PaymentTypeCall.CREDIT_PARC.method, {
      ..._paymentArguments(value, userReference, printReceipt, partialPay, isCarne),
      "parc": parc,
      "type": type.value,
    });
  }

  //Function to invoke method from debit payment with sdk the PagSeguro
  Future<bool> debitPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) {
    return _invokeBool(PaymentTypeCall.DEBIT.method, _paymentArguments(value, userReference, printReceipt, partialPay, isCarne));
  }

  //Function to invoke method from pix payment with sdk the PagSeguro
  Future<bool> pixPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) {
    return _invokeBool(PaymentTypeCall.PIX.method, _paymentArguments(value, userReference, printReceipt, partialPay, isCarne));
  }

  //Function to invoke method from voucher payment with sdk the PagSeguro
  Future<bool> voucherPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) {
    return _invokeBool(PaymentTypeCall.VOUCHER.method, _paymentArguments(value, userReference, printReceipt, partialPay, isCarne));
  }

  //Function to invoke method from credit installment payment with sdk the PagSeguro
  Future<bool> startPayment(
    PaymentType type,
    int amount, {
    PaymentTypeCredit installmentType = PaymentTypeCredit.CLIENT,
    int installments = 1,
    String userReference = USER_REFERENCE,
    bool printReceipt = true,
    bool partialPay = false,
    bool isCarne = false,
  }) {
    return _invokeBool(PaymentTypeCall.START_PAYMENT.method, {
      "type": type.value,
      "amount": amount,
      "installmentType": installmentType.value,
      "installments": installments,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
  }

  //OPERATIONS
  //Function to invoke method from abort current transaction with sdk the PagSeguro
  Future<bool> abortTransaction() => _invokeBool(PaymentTypeCall.ABORT.method);

  //Function to invoke method from get last transaction with sdk the PagSeguro
  Future<bool> lastTransaction() => _invokeBool(PaymentTypeCall.LAST_TRANSACTION.method);

  //Function to invoke method from refund transaction with sdk the PagSeguro
  /// Estorna a transação. `transactionCode` e `transactionId` são obrigatórios: nulos ou vazios
  /// lançam `PlatformException(code: 'INVALID_ARGUMENT')` sem chamar o nativo.
  Future<bool> refund({String? transactionCode, String? transactionId}) async {
    checkRefundArguments(transactionCode, transactionId);
    return _invokeBool(PaymentTypeCall.REFUND.method, {
      "transactionCode": transactionCode,
      "transactionId": transactionId,
    });
  }

  //Function to invoke method from return status of the pinpad
  Future<bool> isAuthenticated() => _invokeBool(PaymentTypeCall.PINPAD_AUTHENTICATED.method);

  //Function to printer from file path
  Future<bool> printerfromFile(String path) => _invokeOrFalse(PaymentTypeCall.PRINTER_FILE.method, {"path": path});

  //Function to printerFile from fileName
  Future<bool> printerFile(String fileName) => _invokeOrFalse(PaymentTypeCall.PRINTER.method, {"path": fileName});

  //Function to printer from file path
  Future<bool> printer(String filePath) => _invokeOrFalse(PaymentTypeCall.PRINTER_BASIC.method, {"path": filePath});

  Future<bool> printerFilePath(String filePath) => _invokeOrFalse(PaymentTypeCall.PRINTER_FILE_PATH.method, {"path": filePath});

  //Reboot device
  Future<bool> rebootDevice() => _invokeOrFalse(PaymentTypeCall.REBOOT.method);

  //Emitter beep device
  Future<bool> beep() => _invokeOrFalse(PaymentTypeCall.BEEP.method);

  /// Número de série do terminal; `''` em qualquer erro.
  Future<String> getSerialNumber() async {
    try {
      final result = await channel.invokeMethod<String>(PaymentTypeCall.GET_SERIAL_NUMBER.method);
      return result ?? '';
    } catch (_) {
      return '';
    }
  }

  /// Solicita os dados do estabelecimento. O resultado chega em
  /// [PaymentHandler.onFinishedResponse] com `method: "userData"`.
  Future<bool> getUserData() => _invokeBool(PaymentTypeCall.GET_USER_DATA.method);

  //Function to calculate installments
  // Calculate installment values for a given sale value and installment type
  // saleValue: Value of the sale in cents (e.g., "2000" for R$ 20,00)
  // installmentType: Type of installment (InstallmentType enum or int: 1=A_VISTA, 2=PARC_VENDEDOR, 3=PARC_COMPRADOR)
  Future<List<PlugPagInstallment>> calculateInstallments(
    String saleValue,
    dynamic installmentType,
  ) async {
    try {
      int typeValue;
      if (installmentType is InstallmentType) {
        typeValue = installmentType.value;
      } else if (installmentType is int) {
        typeValue = installmentType;
      } else {
        throw ArgumentError('installmentType must be InstallmentType or int');
      }
      
      final result = await channel.invokeMethod(
        PaymentTypeCall.CALCULATE_INSTALLMENTS.method,
        {
          "saleValue": saleValue,
          "installmentType": typeValue,
        },
      );
      
      if (result is List) {
        return result
            .map((item) => PlugPagInstallment.fromMap(
                Map<String, dynamic>.from(item as Map)))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  //Function to set dialog style
  Future<bool> setDialogstyle({
    required String title,
    required String buttonBackgroundColor,
    required String buttonBackgroundColorDisabled,
    required String cancelTextColor,
    required String confirmTextColor,
    required String sendSMSTextColor,
    required String titleColor,
    required String windowBackgroundColor,
    required int maxTimeShowPopUp, //Tempo máximo em segundos que o pop up ficará visível
  }) {
    return _invokeBool(PaymentTypeCall.STYLE_DIALOG.method, {
      "title": title,
      "buttonBackgroundColor": buttonBackgroundColor,
      "buttonBackgroundColorDisabled": buttonBackgroundColorDisabled,
      "cancelTextColor": cancelTextColor,
      "confirmTextColor": confirmTextColor,
      "sendSMSTextColor": sendSMSTextColor,
      "titleColor": titleColor,
      "windowBackgroundColor": windowBackgroundColor,
      "maxTimeShowPopUp": maxTimeShowPopUp,
    });
  }

  /// Só letras e números, até 10 caracteres (exigência do SDK); vazio vira "SMARTPOS".
  String _sanitizeUserReference(String value) => sanitizeUserReference(value);
}
