package br.com.jylabtech.pagseguro_payment_tech.core;
import android.content.Context;
import android.util.Log;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagCustomPrinterLayout;
import br.com.jylabtech.pagseguro_payment_tech.nfc.NFCPresenter;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsPresenter;
import br.com.jylabtech.pagseguro_payment_tech.printer.PrinterPresenter;

import br.com.jylabtech.pagseguro_payment_tech.BuildConfig;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class PagSeguroPayment {
  private final PlugPag plugPag;
  private final MethodChannel mChannel;
  private PaymentsPresenter paymentsPresenter;
  private PrinterPresenter printerPresenter;
  private NFCPresenter nfcPresenter;
  // METHODS
  private static final String PAYMENT_DEBIT = "paymentDebit";
  private static final String PAYMENT_CREDIT = "paymentCredit";
  private static final String PAYMENT_CREDIT_PARC = "paymentCreditParc";
  private static final String PAYMENT_VOUCHER = "paymentVoucher";
  private static final String PAYMENT_ABORT = "paymentAbort";
  private static final String LAST_TRANSACTION = "paymentLastTransaction";
  private static final String REFUND = "paymentRefund";
  private static final String PAYMENT_PIX = "paymentPix";
  private static final String START_PAYMENT = "startPayment";

  private static final String ACTIVE_PINPAD = "paymentActivePinpad";
  private static final String PINPAD_AUTHENTICATED = "paymentIsAuthenticated";
  private static final String REBOOT_DEVICE = "paymentReboot";
  private static final String BEEP_PAYMENT = "paymentBeep";
  private static final String GET_SERIAL_NUMBER = "paymentGetSerialNumber";


  //NFC
  private static final String WRITE_NFC = "paymentWriteNfc";
  private static final String READ_NFC = "paymentReadNfc";
  private static final String FORMAT_NFC = "paymentFormatNfc";
  private static final String REWRITE_NFC = "paymentReWriteNfc";
  private static final String REFUND_NFC = "paymentReFundNfc";
  private static final String DEBIT_NFC = "paymentDebitNfc";

  //Printer
  private static final String PRINTER_FILE = "paymentPrinterFile";
  private static final String PRINTER = "paymentPrinter";

  private static final String PRINTER_BASIC = "paymentPrinterBasic";
  private static final String PRINTER_FILE_PATH = "paymentPrinterFilePath";

  private static final String GET_USER_DATA = "paymentGetUserData";

  private static final String STYLE_DIALOG = "paymentStyleDialog";

  private Context context;

  public PagSeguroPayment(Context context, MethodChannel channel) {
    this.context = context;
    PlugPag instancePlugPag = new PlugPag(context);
    PlugPagCustomPrinterLayout customDialog = new PlugPagCustomPrinterLayout();
    customDialog.setMaxTimeShowPopup(30);
    instancePlugPag.setPlugPagCustomPrinterLayout(customDialog);
    this.plugPag = instancePlugPag;
    this.mChannel = channel;

  }

  private void initializePresenters() {
    if (this.paymentsPresenter == null) {
      this.paymentsPresenter = new PaymentsPresenter(this.plugPag, this.mChannel, this.context);
    }

    if(this.printerPresenter == null) {
      this.printerPresenter =  new PrinterPresenter(this.plugPag, this.mChannel);
    }

    if(this.nfcPresenter == null) {
      this.nfcPresenter = new NFCPresenter(this.plugPag, this.mChannel);
    } else {
      this.nfcPresenter.dispose();
    }
  }

  private void restartPresenters() {
      this.paymentsPresenter = new PaymentsPresenter(this.plugPag, this.mChannel, this.context);
      this.printerPresenter =  new PrinterPresenter(this.plugPag, this.mChannel);
      this.nfcPresenter = new NFCPresenter(this.plugPag, this.mChannel);
      this.nfcPresenter.dispose();

  }
  public void initPayment(MethodCall call, MethodChannel.Result result) throws Exception {
    initializePresenters();

    String licenceKeyLocal = BuildConfig.LICENCE_KEY;
    String licenceInternalKey = BuildConfig.LICENCE_INTERNAL_KEY;

    boolean resultLicence = this.paymentsPresenter.checkValidLicence(licenceKeyLocal, licenceInternalKey);
    if(!resultLicence) {
      throw new Exception("Sua licença não foi ativada.");
    }

    if(call.method.equals(STYLE_DIALOG)) {
      setDialogStyle(call);
      result.success(true);
    }


    if(call.method.equals(PRINTER_FILE)) {
      String filePath = call.argument("path");
      printerPresenter.printerFromFile(filePath);
    }

    if(call.method.equals(PRINTER)) {
      String filePath = call.argument("path");
      printerPresenter.printFile(filePath);
    }

    if(call.method.equals(PRINTER_BASIC)) {
      String filePath = call.argument("path");
      printerPresenter.printer(filePath);
    }

    if(call.method.equals(PRINTER_FILE_PATH)) {
      String filePath = call.argument("path");
      printerPresenter.printerByFilePath(filePath);
    }


    if (call.method.equals(REBOOT_DEVICE)) {
      this.paymentsPresenter.rebootDevice();
      result.success(true);
    }

    if (call.method.equals(BEEP_PAYMENT)) {
      this.paymentsPresenter.beep();
      result.success(true);
    }

    if (call.method.equals(GET_SERIAL_NUMBER)) {
      String serialNumber = this.paymentsPresenter.getSerialNumber();
      result.success(serialNumber);
      return;
    }

    if (call.method.equals(GET_USER_DATA)) {
      this.paymentsPresenter.getUserData();
      result.success(true);
      return;
    }

    if (call.method.equals(PAYMENT_DEBIT)) {
      this.paymentsPresenter.doDebitPayment(
              (int) call.argument("value"),
              (String) call.argument("userReference"),
              (Boolean) call.argument("printReceipt"),
              (Boolean) call.argument("partialPay"),
              (Boolean) call.argument("isCarne")
      );
    } else if (call.method.equals(ACTIVE_PINPAD)) {
      this.paymentsPresenter.activate(call.argument("code"));
      result.success(true);
    } else if (call.method.equals(PINPAD_AUTHENTICATED)) {
      this.paymentsPresenter.isAuthenticate();
      result.success(true);
    } else if (call.method.equals(PAYMENT_PIX)) {
      this.paymentsPresenter.doPixPayment(
              (int) call.argument("value"),
              (String) call.argument("userReference"),
              (Boolean) call.argument("printReceipt"),
              (Boolean) call.argument("partialPay"),
              (Boolean) call.argument("isCarne")
      );
      result.success(true);
    } else if (call.method.equals(PAYMENT_CREDIT)) {
      this.paymentsPresenter.creditPayment(
              (int) call.argument("value"),
              (String) call.argument("userReference"),
              (Boolean) call.argument("printReceipt"),
              (Boolean) call.argument("partialPay"),
              (Boolean) call.argument("isCarne")
      );
      result.success(true);
    } else if (call.method.equals(PAYMENT_CREDIT_PARC)) {
      this.paymentsPresenter.creditPaymentParc(
          (int) call.argument("value"),
          (int) call.argument("type"),
          (int) call.argument("parc"),
          (String) call.argument("userReference"),
          (Boolean) call.argument("printReceipt"),
          (Boolean) call.argument("partialPay"),
          (Boolean) call.argument("isCarne")
        );
        result.success(true);
    } else if (call.method.equals(PAYMENT_VOUCHER)) {
      this.paymentsPresenter.doVoucherPayment(
              (int) call.argument("value"),
              (String) call.argument("userReference"),
              (Boolean) call.argument("printReceipt"),
              (Boolean) call.argument("partialPay"),
              (Boolean) call.argument("isCarne")
      );
      result.success(true);
    } else if (call.method.equals(START_PAYMENT)) {
      this.paymentsPresenter.startPayment(
              (int) call.argument("type"),
              (int) call.argument("amount"),
              (int) call.argument("installmentType"),
              (int) call.argument("installments"),
              (String) call.argument("userReference"),
              (Boolean) call.argument("printReceipt"),
              (Boolean) call.argument("partialPay"),
              (Boolean) call.argument("isCarne")
      );
      result.success(true);
    } else if (call.method.equals(PAYMENT_ABORT)) {
      this.paymentsPresenter.abortTransaction();
      result.success(true);
    } else if (call.method.equals(LAST_TRANSACTION)) {
      this.paymentsPresenter.getLastTransaction();
      result.success(true);
    } else if (call.method.equals(REFUND)) {
      this.paymentsPresenter.doRefund(
          call.argument("transactionCode"),
          call.argument("transactionId")
        );
      result.success(true);
    } else if(call.method.equals(READ_NFC)) {
      this.nfcPresenter.readNFCCard(call.argument("idEvento"));
    }
    else if(call.method.equals(WRITE_NFC)) {
      this.nfcPresenter.writeNFCCard(call.argument("valor"), call.argument("nome"), call.argument("cpf"), call.argument("numeroTag"), call.argument("celular"), call.argument("aberto"), call.argument("idEvento"));
    }
    else if(call.method.equals(REWRITE_NFC)) {
      this.nfcPresenter.reWriteNFCCard(call.argument("valor"), call.argument("idEvento"));
    }

    else if(call.method.equals(REFUND_NFC)) {
      this.nfcPresenter.reFundNFCCard(call.argument("valor"), call.argument("idEvento"));
    }
    else if(call.method.equals(FORMAT_NFC)) {
      this.nfcPresenter.formatNFCCard();
    }
    else if(call.method.equals(DEBIT_NFC)) {
      this.nfcPresenter.debitNFCCard(call.argument("idEvento"),call.argument("valor"));
    } else {
      result.notImplemented();
    }
  }

  private void setDialogStyle(MethodCall call)
    {
      String title = call.argument("title");;
      String buttonBackgroundColor = call.argument("buttonBackgroundColor");;
      String buttonBackgroundColorDisabled = call.argument("buttonBackgroundColorDisabled");;
      String cancelTextColor = call.argument("cancelTextColor");;
      String confirmTextColor = call.argument("confirmTextColor");;
      String sendSMSTextColor = call.argument("sendSMSTextColor");;
      String titleColor = call.argument("titleColor");;
      String windowBackgroundColor = call.argument("windowBackgroundColor");;
      int maxTimeShowPopUp = call.argument("maxTimeShowPopUp");;

      PlugPagCustomPrinterLayout customDialog = new PlugPagCustomPrinterLayout();
      customDialog.setMaxTimeShowPopup(maxTimeShowPopUp);
      customDialog.setButtonBackgroundColor(buttonBackgroundColor);
      customDialog.setTitle(title);
      customDialog.setButtonBackgroundColorDisabled(buttonBackgroundColorDisabled);
      customDialog.setCancelTextColor(cancelTextColor);
      customDialog.setConfirmTextColor(confirmTextColor);
      customDialog.setSendSMSTextColor(sendSMSTextColor);
      customDialog.setTitleColor(titleColor);
      customDialog.setWindowBackgroundColor(windowBackgroundColor);

      this.plugPag.setPlugPagCustomPrinterLayout(customDialog);
      restartPresenters();
  }
  public void dispose() {
    if (this.paymentsPresenter != null) {
      this.paymentsPresenter.dispose();
    }
  }
}