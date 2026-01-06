// ignore_for_file: public_member_api_docs, sort_constructors_first
// ignore_for_file: constant_identifier_names
import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/licence/constants/licence_constants.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/payment_handler.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/payment_types.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/plugpag_installment.dart';

//Fixed channel name

final class Payment {
  final MethodChannel channel;
  PaymentHandler paymentHandler;
  late String _licenceKey;
  late String _licenceInternalKey;

  // Cache da instância criada
  static Payment? _instance;

  Payment._({
    required this.channel,
    required this.paymentHandler,
    required String licenceKey,
    required String licenceInternalKey,
  }) {
    _licenceKey = licenceKey;
    _licenceInternalKey = licenceInternalKey;
  }

  /// Método factory assíncrono para criação controlada e validação da licença
  static Payment create({
    required MethodChannel channel,
    required PaymentHandler paymentHandler,
    required String licenceKey,
    required String licenceInternalKey,
  }) {
    if (_instance == null) {
      final payment = Payment._(
        channel: channel,
        paymentHandler: paymentHandler,
        licenceKey: licenceKey,
        licenceInternalKey: licenceInternalKey,
      );
      payment.channel.setMethodCallHandler(payment._callHandler);
      payment._configureLicence();
      _instance = payment;
    } else {
      _instance!._licenceKey = licenceKey;
      _instance!._licenceInternalKey = licenceInternalKey;
      _instance!.paymentHandler = paymentHandler;
      _instance!.channel.setMethodCallHandler(_instance!._callHandler);
      _instance!._configureLicence();
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
    channel.setMethodCallHandler(_callHandler);
    _configureLicence();
  }

  void _configureLicence() {
    channel.invokeMethod("initPayment", {
      "licenceKey": _licenceKey,
      "licenceInternalKey": _licenceInternalKey,
    });
  }

  //Create external functions from invoke methodChannel
  //Function to active pinpad with sdk the PagSeguro
  Future<bool> activePinpad(String activationCode) async {
    try {
      await channel.invokeMethod(PaymentTypeCall.ACTIVEPINPAD.method, {"code": activationCode});
      return true;
    } catch (_) {
      return false;
    }
  }

  //Function to invoke method from credit payment with sdk the PagSeguro
  Future<bool> creditPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) async {
    return await channel.invokeMethod(PaymentTypeCall.CREDIT.method, {
      "value": value,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
  }

  //Function to invoke method from credit installment payment  with sdk the PagSeguro
  Future<bool> creditPaymentParc(int value, int parc, {PaymentTypeCredit type = PaymentTypeCredit.CLIENT, String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) async {
    return await channel.invokeMethod(PaymentTypeCall.CREDIT_PARC.method, {
      "value": value,
      "parc": parc,
      "type": type.value,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
  }

  //Function to invoke method from debit payment with sdk the PagSeguro
  Future<bool> debitPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) async {
    return await channel.invokeMethod(PaymentTypeCall.DEBIT.method, {
      "value": value,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
  }

  //Function to invoke method from pix payment with sdk the PagSeguro
  Future<bool> pixPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) async {
    return await channel.invokeMethod(PaymentTypeCall.PIX.method, {
      "value": value,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
  }

  //Function to invoke method from voucher payment with sdk the PagSeguro
  Future<bool> voucherPayment(int value, {String userReference = USER_REFERENCE, bool printReceipt = true, bool partialPay = false, bool isCarne = false}) async {
    return await channel.invokeMethod(PaymentTypeCall.VOUCHER.method, {
      "value": value,
      "userReference": _sanitizeUserReference(userReference),
      "printReceipt": printReceipt,
      "partialPay": partialPay,
      "isCarne": isCarne,
    });
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
  }) async {
    return await channel.invokeMethod(PaymentTypeCall.START_PAYMENT.method, {
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
  Future<bool> abortTransaction() async {
    return await channel.invokeMethod(PaymentTypeCall.ABORT.method);
  }

  //Function to invoke method from get last transaction with sdk the PagSeguro
  Future<bool> lastTransaction() async {
    return await channel.invokeMethod(PaymentTypeCall.LAST_TRANSACTION.method);
  }

  //Function to invoke method from refund transaction with sdk the PagSeguro
  Future<bool> refund({String? transactionCode, String? transactionId}) async {
    return await channel.invokeMethod(PaymentTypeCall.REFUND.method, {
      "transactionCode": transactionCode,
      "transactionId": transactionId,
    });
  }

  //Function to invoke method from return status of the pinpad
  Future<bool> isAuthenticated() async {
    return await channel.invokeMethod(PaymentTypeCall.PINPAD_AUTHENTICATED.method);
  }

  //Function to printer from file path
  Future<bool> printerfromFile(String path) async {
    try {
      await channel.invokeMethod(
        PaymentTypeCall.PRINTER_FILE.method,
        {
          "path": path,
        },
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  //Function to printerFile from fileName
  Future<bool> printerFile(String fileName) async {
    try {
      await channel.invokeMethod(
        PaymentTypeCall.PRINTER.method,
        {
          "path": fileName,
        },
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  //Function to printer from file path
  Future<bool> printer(String filePath) async {
    try {
      await channel.invokeMethod(
        PaymentTypeCall.PRINTER_BASIC.method,
        {
          "path": filePath,
        },
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<bool> printerFilePath(String filePath) async {
    try {
      await channel.invokeMethod(
        PaymentTypeCall.PRINTER_FILE_PATH.method,
        {
          "path": filePath,
        },
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  //Reboot device
  Future<bool> rebootDevice() async {
    try {
      channel.invokeMethod(PaymentTypeCall.REBOOT.method);
      return true;
    } catch (_) {
      return false;
    }
  }

  //Emitter beep device
  Future<bool> beep() async {
    try {
      channel.invokeMethod(PaymentTypeCall.BEEP.method);
      return true;
    } catch (_) {
      return false;
    }
  }

  //GetSerialNumber
  Future<String> getSerialNumber() async {
    try {
      final result = await channel.invokeMethod(PaymentTypeCall.GET_SERIAL_NUMBER.method);
      return result as String;
    } catch (_) {
      return '';
    }
  }

  //Function to get user data
  Future<bool> getUserData() async {
    return await channel.invokeMethod(PaymentTypeCall.GET_USER_DATA.method);
  }

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
  }) async {
    return await channel.invokeMethod(PaymentTypeCall.STYLE_DIALOG.method, {
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

  //Function to listen to pagseguro returns in the native environment and notify Flutter
  Future<dynamic> _callHandler(MethodCall call) async {
    switch (call.method.handler) {
      case PaymentTypeHandler.ON_TRANSACTION_SUCCESS:
        {
          paymentHandler.onTransactionSuccess();
        }
        break;
      case PaymentTypeHandler.ON_ERROR:
        {
          paymentHandler.onError(call.arguments);
        }
        break;
      case PaymentTypeHandler.ON_MESSAGE:
        {
          paymentHandler.onMessage(call.arguments);
        }
        break;
      case PaymentTypeHandler.ON_FINISHED_RESPONSE:
        {
          paymentHandler.onFinishedResponse(call.arguments);
        }
        break;
      case PaymentTypeHandler.ON_LOADING:
        {
          paymentHandler.onLoading(call.arguments);
        }
        break;
      case PaymentTypeHandler.WRITE_TO_FILE:
        {
          paymentHandler.writeToFile(
            transactionCode: call.arguments['transactionCode'],
            transactionId: call.arguments['transactionId'],
            response: call.arguments['response'],
          );
        }
        break;
      case PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY:
        {
          paymentHandler.onAbortedSuccessfully();
        }
        break;
      case PaymentTypeHandler.DISPOSE_DIALOG:
        {
          paymentHandler.disposeDialog();
        }
        break;
      case PaymentTypeHandler.ACTIVE_DIALOG:
        {
          paymentHandler.onActivationDialog();
        }
        break;
      case PaymentTypeHandler.ON_AUTH_PROGRESS:
        {
          paymentHandler.onAuthProgress(call.arguments);
        }
        break;
      case PaymentTypeHandler.ON_TRANSACTION_INFO:
        {
          paymentHandler.onTransactionInfo(
            transactionCode: call.arguments['transactionCode'],
            transactionId: call.arguments['transactionId'],
            response: call.arguments['response'],
          );
        }
        break;
      default:
        throw "METHOD NOT IMPLEMENTED";
    }
    return true;
  }

  String _sanitizeUserReference(String value) {
    if (value.length <= 10) {
      return value;
    }
    return value.substring(0, 10);
  }
}
