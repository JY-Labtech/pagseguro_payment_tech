package br.com.jylabtech.pagseguro_payment_tech.core;

import android.content.Context;

import java.util.List;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceConfig;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceResult;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsFragment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagCustomPrinterLayout;
import br.com.jylabtech.pagseguro_payment_tech.nfc.NFCPresenter;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsPresenter;
import br.com.jylabtech.pagseguro_payment_tech.printer.PrinterPresenter;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Fachada nativa do plugin: guarda as chaves de licença, cria os presenters sobre um único
 * {@link PlugPag} e executa as operações liberadas pelo gate de licença.
 */
public class PagSeguroPayment implements ChannelRequestHandler.Facade {
  private final PlugPag plugPag;
  private final MethodChannel mChannel;
  private final Context context;

  private PaymentsPresenter paymentsPresenter;
  private PrinterPresenter printerPresenter;
  private NFCPresenter nfcPresenter;

  private volatile String licenceKey;
  private volatile String licenceInternalKey;
  private final CheckLicenceTech licenceTech;
  private final PaymentsFragment licenceFragment;

  private static final int DEFAULT_MAX_TIME_SHOW_POPUP = 30;
  private int maxTimeShowPopup = DEFAULT_MAX_TIME_SHOW_POPUP;
  /** Background para as chamadas bloqueantes ao PlugPagService (número de série, impressão...). */
  private final AndroidAsyncExecutor sdkExecutor;
  /** Último layout aplicado ao dialog de impressão da via do cliente. */
  private PlugPagCustomPrinterLayout printerLayout;

  public PagSeguroPayment(Context context, MethodChannel channel) {
    this.context = context;
    PlugPag instancePlugPag = new PlugPag(context);
    this.plugPag = instancePlugPag;
    this.mChannel = channel;
    this.licenceTech = new CheckLicenceTech(context);
    this.licenceFragment = new PaymentsFragment(channel);
    this.sdkExecutor = new AndroidAsyncExecutor();
    applyPrintDialogLayout();
  }

  @Override
  public void setMaxTimeShowPopup(int maxTimeShowPopup) {
    this.maxTimeShowPopup = maxTimeShowPopup > 0 ? maxTimeShowPopup : DEFAULT_MAX_TIME_SHOW_POPUP;
    applyPrintDialogLayout();
  }

  /**
   * Reaplica o layout do dialog de impressão com o tempo atual, preservando as cores/título
   * definidos em setDialogstyle (antes, cada initPayment/setPrintDialogMaxTime criava um layout
   * novo e descartava o estilo).
   */
  private void applyPrintDialogLayout() {
    PlugPagCustomPrinterLayout customDialog = this.printerLayout;
    if (customDialog == null) {
      customDialog = new PlugPagCustomPrinterLayout();
      this.printerLayout = customDialog;
    }
    customDialog.setMaxTimeShowPopup(this.maxTimeShowPopup);
    // Os presenters usam o mesmo PlugPag: não é preciso recriá-los para o layout valer.
    this.plugPag.setPlugPagCustomPrinterLayout(customDialog);
  }

  /**
   * Guarda as chaves de licença. O licenceInternalKey é opcional: valores vazios ou fora do
   * servidor oficial são substituídos pela URL do core em {@link CheckLicenceTech}.
   * Chave ausente não lança: a validação seguinte devolve LICENCE_ERROR.
   */
  @Override
  public void updateLicenceKeys(String licenceKey, String licenceInternalKey) {
    this.licenceKey = licenceKey == null ? null : licenceKey.trim();
    this.licenceInternalKey = (licenceInternalKey == null || licenceInternalKey.trim().isEmpty())
        ? LicenceConfig.LICENCE_CHECK_URL
        : licenceInternalKey.trim();
    initializePresenters();
  }

  /**
   * Valida a licença (cache válido ou servidor). Pode fazer rede: chamar FORA da main thread.
   * Não toca no MethodChannel.
   */
  @Override
  public LicenceResult checkLicence() {
    return licenceTech.validateLicence(licenceKey, licenceInternalKey);
  }

  /** Avisa o Flutter que a licença está sendo verificada (main thread). */
  @Override
  public void notifyLicenceChecking() {
    licenceFragment.onMessage("Verificando licença");
  }

  /**
   * Repassa o resultado da validação ao handler do Flutter (main thread), mantendo os mesmos
   * eventos das versões anteriores (onMessage / onError / onFinishedResponse com method "licence").
   */
  @Override
  public void notifyLicenceResult(LicenceResult licenceResult) {
    ActionResult result = new ActionResult();
    result.setMethod("licence");
    if (licenceResult != null && licenceResult.isSuccess()) {
      licenceFragment.onMessage("Licença ativada");
      result.setResult(0);
      result.setMessage("Licença ativada com sucesso");
    } else {
      String errorMessage = licenceErrorMessage(licenceResult);
      licenceFragment.onMessage(errorMessage);
      result.setResult(LicenceConfig.RESULT_ERROR);
      result.setErrorMessage(errorMessage);
      licenceFragment.onError(errorMessage);
    }
    licenceFragment.onFinishedResponse(JsonResults.toJson(result));
  }

  public static String licenceErrorMessage(LicenceResult licenceResult) {
    return ChannelRequestHandler.licenceErrorMessage(licenceResult);
  }

  private void initializePresenters() {
    if (this.paymentsPresenter == null) {
      // Mesma CheckLicenceTech do gate: um único cache/EncryptedSharedPreferences.
      this.paymentsPresenter = new PaymentsPresenter(this.plugPag, this.mChannel, this.licenceTech);
    }

    if (this.printerPresenter == null) {
      this.printerPresenter = new PrinterPresenter(this.plugPag, this.mChannel, this.context);
    }

    if (this.nfcPresenter == null) {
      this.nfcPresenter = new NFCPresenter(this.plugPag, this.mChannel);
    } else {
      // Comportamento mantido das versões anteriores: cada nova chamada cancela a
      // assinatura NFC anterior (o resultado de uma leitura pendente deixa de ser entregue).
      this.nfcPresenter.dispose();
    }
  }

  /**
   * Executa a operação. Só pode ser chamado DEPOIS de {@link #checkLicence()} devolver sucesso
   * (o PagseguroPaymentTechNativePlugin garante isso para todo método de negócio).
   * O result é completado exatamente uma vez por {@link PaymentMethodRouter}; o resultado da
   * transação/impressão/NFC continua chegando pelos eventos do handler.
   */
  @Override
  public void handleMethodCall(MethodCall call, MethodChannel.Result result) throws Exception {
    initializePresenters();
    PaymentMethodRouter.dispatch(call, result, operations, sdkExecutor);
  }

  private final PaymentMethodRouter.Operations operations = new PaymentMethodRouter.Operations() {
    @Override
    public void setDialogStyle(MethodCall call) {
      PagSeguroPayment.this.setDialogStyle(call);
    }

    @Override
    public void setMaxTimeShowPopup(int maxTimeShowPopup) {
      PagSeguroPayment.this.setMaxTimeShowPopup(maxTimeShowPopup);
    }

    @Override
    public void printerFromFile(String path) {
      printerPresenter.printerFromFile(path);
    }

    @Override
    public void printFile(String fileName) {
      printerPresenter.printFile(fileName);
    }

    @Override
    public void printer(String filePath) {
      printerPresenter.printer(filePath);
    }

    @Override
    public void printerByFilePath(String filePath) {
      printerPresenter.printerByFilePath(filePath);
    }

    @Override
    public void rebootDevice() throws Exception {
      paymentsPresenter.rebootDevice();
    }

    @Override
    public void beep() throws Exception {
      paymentsPresenter.beep();
    }

    @Override
    public String getSerialNumber() throws Exception {
      return paymentsPresenter.getSerialNumber();
    }

    @Override
    public void getUserData() throws Exception {
      paymentsPresenter.getUserData();
    }

    @Override
    public List<Map<String, Object>> calculateInstallments(String saleValue, int installmentType) throws Exception {
      return paymentsPresenter.calculateInstallments(saleValue, installmentType);
    }

    @Override
    public void debitPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.doDebitPayment(value, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void activatePinpad(String code) throws Exception {
      paymentsPresenter.activate(code);
    }

    @Override
    public void isAuthenticated() throws Exception {
      paymentsPresenter.isAuthenticate();
    }

    @Override
    public void pixPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.doPixPayment(value, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void creditPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.creditPayment(value, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void creditPaymentParc(int value, int type, int parc, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.creditPaymentParc(value, type, parc, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void voucherPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.doVoucherPayment(value, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void startPayment(int type, int amount, int installmentType, int installments, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
      paymentsPresenter.startPayment(type, amount, installmentType, installments, userReference, printReceipt, partialPay, isCarne);
    }

    @Override
    public void abortTransaction() throws Exception {
      paymentsPresenter.abortTransaction();
    }

    @Override
    public void lastTransaction() throws Exception {
      paymentsPresenter.getLastTransaction();
    }

    @Override
    public void refund(String transactionCode, String transactionId) throws Exception {
      paymentsPresenter.doRefund(transactionCode, transactionId);
    }

    @Override
    public void readNfc(String idEvento) {
      nfcPresenter.readNFCCard(idEvento);
    }

    @Override
    public void writeNfc(String valor, String nome, String cpf, String numeroTag, String celular, String aberto, String idEvento) {
      nfcPresenter.writeNFCCard(valor, nome, cpf, numeroTag, celular, aberto, idEvento);
    }

    @Override
    public void reWriteNfc(String valor, String idEvento) {
      nfcPresenter.reWriteNFCCard(valor, idEvento);
    }

    @Override
    public void refundNfc(String valor, String idEvento) {
      nfcPresenter.reFundNFCCard(valor, idEvento);
    }

    @Override
    public void formatNfc() {
      nfcPresenter.formatNFCCard();
    }

    @Override
    public void debitNfc(String idEvento, String valor) {
      nfcPresenter.debitNFCCard(idEvento, valor);
    }
  };

  private void setDialogStyle(MethodCall call) {
    String title = call.argument("title");
    String buttonBackgroundColor = call.argument("buttonBackgroundColor");
    String buttonBackgroundColorDisabled = call.argument("buttonBackgroundColorDisabled");
    String cancelTextColor = call.argument("cancelTextColor");
    String confirmTextColor = call.argument("confirmTextColor");
    String sendSMSTextColor = call.argument("sendSMSTextColor");
    String titleColor = call.argument("titleColor");
    String windowBackgroundColor = call.argument("windowBackgroundColor");
    Integer maxTimeShowPopUpArg = call.argument("maxTimeShowPopUp");
    int maxTimeShowPopUp = maxTimeShowPopUpArg != null ? maxTimeShowPopUpArg : DEFAULT_MAX_TIME_SHOW_POPUP;
    this.maxTimeShowPopup = maxTimeShowPopUp > 0 ? maxTimeShowPopUp : DEFAULT_MAX_TIME_SHOW_POPUP;

    PlugPagCustomPrinterLayout customDialog = new PlugPagCustomPrinterLayout();
    customDialog.setMaxTimeShowPopup(this.maxTimeShowPopup);
    customDialog.setButtonBackgroundColor(buttonBackgroundColor);
    customDialog.setTitle(title);
    customDialog.setButtonBackgroundColorDisabled(buttonBackgroundColorDisabled);
    customDialog.setCancelTextColor(cancelTextColor);
    customDialog.setConfirmTextColor(confirmTextColor);
    customDialog.setSendSMSTextColor(sendSMSTextColor);
    customDialog.setTitleColor(titleColor);
    customDialog.setWindowBackgroundColor(windowBackgroundColor);

    this.printerLayout = customDialog;
    this.plugPag.setPlugPagCustomPrinterLayout(customDialog);
  }

  /** Libera assinaturas RxJava de todos os presenters e a thread de background do SDK. */
  public void dispose() {
    if (this.paymentsPresenter != null) {
      this.paymentsPresenter.dispose();
    }
    if (this.printerPresenter != null) {
      this.printerPresenter.dispose();
    }
    if (this.nfcPresenter != null) {
      this.nfcPresenter.dispose();
    }
    this.sdkExecutor.shutdown();
  }
}