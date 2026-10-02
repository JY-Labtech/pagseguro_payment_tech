// ignore_for_file: constant_identifier_names
//Fixed payment type hanldle from return functions
enum PaymentTypeHandler {
  ON_TRANSACTION_SUCCESS,
  ON_ERROR,
  ON_MESSAGE,
  ON_LOADING,
  WRITE_TO_FILE,
  ON_ABORTED_SUCCESSFULLY,
  DISPOSE_DIALOG,
  ACTIVE_DIALOG,
  ON_AUTH_PROGRESS,
  ON_TRANSACTION_INFO,
  ON_FINISHED_RESPONSE,
  SHOW_SUCCESS,
  SHOW_SUCCESS_WRITE,
  SHOW_SUCCESS_RE_WRITE,
  SHOW_SUCCESS_FORMAT,
  SHOW_SUCCESS_DEBIT_NFC,
  SHOW_SUCCESS_REFUND_NFC,
  SHOW_ERROR_READ,
  SHOW_ERROR_WRITE,
  SHOW_ERROR_RE_WRITE,
  SHOW_ERROR_FORMAT,
  SHOW_ERROR_DEBIT_NFC,
  SHOW_ERROR_REFUND_NFC,
}

/// Nome do evento enviado pelo nativo -> [PaymentTypeHandler]. Lança para nomes desconhecidos.
extension StringPaymentHandlerExt on String {
  PaymentTypeHandler get handler {
    switch (this) {
      // O nativo envia "onTransactionSuccess"; "onTransactionSucess" é a grafia antiga do Dart.
      case "onTransactionSuccess":
      case "onTransactionSucess":
        return PaymentTypeHandler.ON_TRANSACTION_SUCCESS;
      case "onError":
        return PaymentTypeHandler.ON_ERROR;
      case "onMessage":
        return PaymentTypeHandler.ON_MESSAGE;
      case "onFinishedResponse":
        return PaymentTypeHandler.ON_FINISHED_RESPONSE;
      case "onLoading":
        return PaymentTypeHandler.ON_LOADING;
      case "writeToFile":
        return PaymentTypeHandler.WRITE_TO_FILE;
      case "onAbortedSuccessfully":
        return PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY;
      case "disposeDialog":
        return PaymentTypeHandler.DISPOSE_DIALOG;
      case "activeDialog":
        return PaymentTypeHandler.ACTIVE_DIALOG;
      case "onAuthProgress":
        return PaymentTypeHandler.ON_AUTH_PROGRESS;
      case "onTransactionInfo":
        return PaymentTypeHandler.ON_TRANSACTION_INFO;
      case "showSuccess":
        return PaymentTypeHandler.SHOW_SUCCESS;
      case "showSuccessWrite":
        return PaymentTypeHandler.SHOW_SUCCESS_WRITE;
      case "showSuccessReWrite":
        return PaymentTypeHandler.SHOW_SUCCESS_RE_WRITE;
      case "showSuccessFormat":
        return PaymentTypeHandler.SHOW_SUCCESS_FORMAT;
      case "showSuccessDebitNfc":
        return PaymentTypeHandler.SHOW_SUCCESS_DEBIT_NFC;
      case "showSuccessRefundNfc":
        return PaymentTypeHandler.SHOW_SUCCESS_REFUND_NFC;
      case "showErrorRead":
        return PaymentTypeHandler.SHOW_ERROR_READ;
      case "showErrorWrite":
        return PaymentTypeHandler.SHOW_ERROR_WRITE;
      case "showErrorReWrite":
        return PaymentTypeHandler.SHOW_ERROR_RE_WRITE;
      case "showErrorFormat":
        return PaymentTypeHandler.SHOW_ERROR_FORMAT;
      case "showErrorDebitNfc":
        return PaymentTypeHandler.SHOW_ERROR_DEBIT_NFC;
      case "showErrorRefundNfc":
        return PaymentTypeHandler.SHOW_ERROR_REFUND_NFC;
      default:
        throw "NOT IMPLEMENTED";
    }
  }
}

/// Nome do método usado pelo nativo para cada evento.
extension PaymentTypeHandlerExt on PaymentTypeHandler {
  String get method {
    switch (this) {
      case PaymentTypeHandler.ON_TRANSACTION_SUCCESS:
        return "onTransactionSuccess";
      case PaymentTypeHandler.ON_ERROR:
        return "onError";
      case PaymentTypeHandler.ON_MESSAGE:
        return "onMessage";
      case PaymentTypeHandler.ON_LOADING:
        return "onLoading";
      case PaymentTypeHandler.WRITE_TO_FILE:
        return "writeToFile";
      case PaymentTypeHandler.ON_ABORTED_SUCCESSFULLY:
        return "onAbortedSuccessfully";
      case PaymentTypeHandler.DISPOSE_DIALOG:
        return "disposeDialog";
      case PaymentTypeHandler.ACTIVE_DIALOG:
        return "activeDialog";
      case PaymentTypeHandler.ON_AUTH_PROGRESS:
        return "onAuthProgress";
      case PaymentTypeHandler.ON_TRANSACTION_INFO:
        return "onTransactionInfo";
      case PaymentTypeHandler.ON_FINISHED_RESPONSE:
        return "onFinishedResponse";
      case PaymentTypeHandler.SHOW_SUCCESS:
        return "showSuccess";
      case PaymentTypeHandler.SHOW_SUCCESS_WRITE:
        return "showSuccessWrite";
      case PaymentTypeHandler.SHOW_SUCCESS_RE_WRITE:
        return "showSuccessReWrite";
      case PaymentTypeHandler.SHOW_SUCCESS_FORMAT:
        return "showSuccessFormat";
      case PaymentTypeHandler.SHOW_SUCCESS_DEBIT_NFC:
        return "showSuccessDebitNfc";
      case PaymentTypeHandler.SHOW_SUCCESS_REFUND_NFC:
        return "showSuccessRefundNfc";
      case PaymentTypeHandler.SHOW_ERROR_READ:
        return "showErrorRead";
      case PaymentTypeHandler.SHOW_ERROR_WRITE:
        return "showErrorWrite";
      case PaymentTypeHandler.SHOW_ERROR_RE_WRITE:
        return "showErrorReWrite";
      case PaymentTypeHandler.SHOW_ERROR_FORMAT:
        return "showErrorFormat";
      case PaymentTypeHandler.SHOW_ERROR_DEBIT_NFC:
        return "showErrorDebitNfc";
      case PaymentTypeHandler.SHOW_ERROR_REFUND_NFC:
        return "showErrorRefundNfc";
    }
  }
}

//fixed payment type to call from channel
enum PaymentTypeCall {
  CREDIT,
  CREDIT_PARC,
  DEBIT,
  PIX,
  VOUCHER,
  ABORT,
  LAST_TRANSACTION,
  REFUND,
  ACTIVEPINPAD,
  PINPAD_AUTHENTICATED,
  READ_NFC,
  WRITE_NFC,
  REWRITE_NFC,
  REFUND_NFC,
  DEBIT_NFC,
  FORMAT_NFC,
  PRINTER_FILE,
  START_PAYMENT,
  PRINTER,
  PRINTER_BASIC,
  PRINTER_FILE_PATH,
  REBOOT,
  BEEP,
  GET_SERIAL_NUMBER,
  GET_USER_DATA,
  STYLE_DIALOG,
  CALCULATE_INSTALLMENTS,
  SET_PRINT_DIALOG_MAX_TIME,
}

enum PaymentTypeCredit {
  SALESMAN,
  CLIENT,
}

extension PaymentTypeCreditExt on PaymentTypeCredit {
  int get value {
    switch (this) {
      case PaymentTypeCredit.SALESMAN:
        return 2;
      case PaymentTypeCredit.CLIENT:
        return 3;
    }
  }
}

enum InstallmentType {
  A_VISTA,
  PARC_VENDEDOR,
  PARC_COMPRADOR,
}

extension InstallmentTypeExt on InstallmentType {
  int get value {
    switch (this) {
      case InstallmentType.A_VISTA:
        return 1;
      case InstallmentType.PARC_VENDEDOR:
        return 2;
      case InstallmentType.PARC_COMPRADOR:
        return 3;
    }
  }
}

//Fixed method to call on methodChannel
extension PaymentTypeCallExt on PaymentTypeCall {
  String get method {
    switch (this) {
      case PaymentTypeCall.CREDIT:
        return "paymentCredit";
      case PaymentTypeCall.CREDIT_PARC:
        return "paymentCreditParc";
      case PaymentTypeCall.DEBIT:
        return "paymentDebit";
      case PaymentTypeCall.VOUCHER:
        return "paymentVoucher";
      case PaymentTypeCall.ABORT:
        return "paymentAbort";
      case PaymentTypeCall.LAST_TRANSACTION:
        return "paymentLastTransaction";
      case PaymentTypeCall.REFUND:
        return "paymentRefund";
      case PaymentTypeCall.ACTIVEPINPAD:
        return "paymentActivePinpad";
      case PaymentTypeCall.PINPAD_AUTHENTICATED:
        return "paymentIsAuthenticated";
      case PaymentTypeCall.PIX:
        return "paymentPix";
      case PaymentTypeCall.READ_NFC:
        return "paymentReadNfc";
      case PaymentTypeCall.WRITE_NFC:
        return "paymentWriteNfc";
      case PaymentTypeCall.REWRITE_NFC:
        return "paymentReWriteNfc";
      case PaymentTypeCall.DEBIT_NFC:
        return "paymentDebitNfc";
      case PaymentTypeCall.FORMAT_NFC:
        return "paymentFormatNfc";
      case PaymentTypeCall.REFUND_NFC:
        return "paymentReFundNfc";
      case PaymentTypeCall.PRINTER_FILE:
        return "paymentPrinterFile";
      case PaymentTypeCall.PRINTER:
        return "paymentPrinter";
      case PaymentTypeCall.PRINTER_BASIC:
        return "paymentPrinterBasic";
      case PaymentTypeCall.START_PAYMENT:
        return "startPayment";
      case PaymentTypeCall.PRINTER_FILE_PATH:
        return "paymentPrinterFilePath";
      case PaymentTypeCall.REBOOT:
        return 'paymentReboot';
      case PaymentTypeCall.BEEP:
        return 'paymentBeep';
      case PaymentTypeCall.GET_SERIAL_NUMBER:
        return 'paymentGetSerialNumber';
      case PaymentTypeCall.GET_USER_DATA:
        return 'paymentGetUserData';
      case PaymentTypeCall.STYLE_DIALOG:
        return 'paymentStyleDialog';
      case PaymentTypeCall.CALCULATE_INSTALLMENTS:
        return 'paymentCalculateInstallments';
      case PaymentTypeCall.SET_PRINT_DIALOG_MAX_TIME:
        return 'paymentSetPrintDialogMaxTime';
    }
  }
}

enum PaymentType {
  TYPE_CREDITO,
  TYPE_DEBITO,
  TYPE_VOUCHER,
  TYPE_QRCODE,
  TYPE_PIX,
  TYPE_PREAUTO_CARD,
  TYPE_QRCODE_CREDITO,
  TYPE_PREAUTO_KEYED,
}

extension PaymentTypeExt on PaymentType {
  int get value {
    switch (this) {
      case PaymentType.TYPE_CREDITO:
        return 1;
      case PaymentType.TYPE_DEBITO:
        return 2;
      case PaymentType.TYPE_VOUCHER:
        return 3;
      case PaymentType.TYPE_QRCODE:
        return 4;
      case PaymentType.TYPE_PIX:
        return 5;
      case PaymentType.TYPE_PREAUTO_CARD:
        return 6;
      case PaymentType.TYPE_QRCODE_CREDITO:
        return 7;
      case PaymentType.TYPE_PREAUTO_KEYED:
        return 8;
    }
  }
}
