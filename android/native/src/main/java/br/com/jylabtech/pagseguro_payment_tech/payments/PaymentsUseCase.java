package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.List;

import br.com.jylabtech.pagseguro_payment_tech.core.ActionResult;
import br.com.jylabtech.pagseguro_payment_tech.core.ResultMappers;
import br.com.jylabtech.pagseguro_payment_tech.core.UserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException;
import io.reactivex.Single;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/**
 * Casos de uso de pagamento sobre o {@link PaymentGateway} (fachada do SDK PlugPag).
 * Os métodos que devolvem Observable só chamam o SDK quando inscritos (em background).
 */
public class PaymentsUseCase {

  static final int TYPE_CREDITO = 1;
  static final int TYPE_DEBITO = 2;
  static final int TYPE_VOUCHER = 3;
  static final int TYPE_PIX = 5;

  static final int INSTALLMENT_TYPE_A_VISTA = 1;

  private final PaymentGateway mGateway;

  public PaymentsUseCase(PaymentGateway gateway) {
    this.mGateway = gateway;
  }

  public PaymentsUseCase(PlugPag plugPag) {
    this(new PlugPagPaymentGateway(plugPag));
  }

  public Observable<ActionResult> doCreditPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(TYPE_CREDITO, value, INSTALLMENT_TYPE_A_VISTA, 1, userReference, printReceipt, partialPay, isCarne));
  }

  public Observable<ActionResult> doCreditPaymentParc(int value, int type, int parc, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(TYPE_CREDITO, value, type, parc, userReference, printReceipt, partialPay, isCarne));
  }

  public Observable<ActionResult> doDebitPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(TYPE_DEBITO, value, INSTALLMENT_TYPE_A_VISTA, 1, userReference, printReceipt, partialPay, isCarne));
  }

  public Observable<ActionResult> doVoucherPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(TYPE_VOUCHER, value, INSTALLMENT_TYPE_A_VISTA, 1, userReference, printReceipt, partialPay, isCarne));
  }

  public Observable<ActionResult> doPixPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(TYPE_PIX, value, INSTALLMENT_TYPE_A_VISTA, 1, userReference, printReceipt, partialPay, isCarne));
  }

  public Observable<ActionResult> doStartPayment(int type, int amount, int installmentType, int installments, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    return doPayment(paymentData(type, amount, installmentType, installments, userReference, printReceipt, partialPay, isCarne));
  }

  /**
   * Monta o PlugPagPaymentData. Os Boolean vindos do Dart sempre chegam preenchidos; null vira o
   * padrão do Dart (printReceipt = true, partialPay/isCarne = false) em vez de NullPointerException.
   */
  static PlugPagPaymentData paymentData(int type, int amount, int installmentType, int installments, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) {
    return new PlugPagPaymentData(
        type,
        amount,
        installmentType,
        installments,
        userReference,
        printReceipt == null || printReceipt,
        partialPay != null && partialPay,
        isCarne != null && isCarne);
  }

  private Observable<ActionResult> doPayment(final PlugPagPaymentData paymentData) {
    return Observable.create(emitter -> {
      ActionResult result = new ActionResult();
      setListener(emitter, result);
      PlugPagTransactionResult plugPagTransactionResult = mGateway.doPayment(paymentData);
      sendResponse(emitter, plugPagTransactionResult, result);
    });
  }

  /**
   * Resultado do SDK: result 0 = aprovado (emite o ActionResult preenchido); qualquer outro valor
   * (ou resultado ausente) vira erro com a mensagem e o código do SDK.
   */
  static void sendResponse(ObservableEmitter<ActionResult> emitter, PlugPagTransactionResult plugPagTransactionResult, ActionResult result) {
    if (plugPagTransactionResult == null) {
      emitter.onError(new PlugPagException("Sem resposta do PlugPag."));
      return;
    }
    Integer code = plugPagTransactionResult.getResult();
    if (code == null || code != 0) {
      emitter.onError(new PlugPagException(plugPagTransactionResult.getMessage(), plugPagTransactionResult.getErrorCode()));
    } else {
      result.setTransactionCode(plugPagTransactionResult.getTransactionCode());
      result.setTransactionId(plugPagTransactionResult.getTransactionId());
      result.buildResponse(plugPagTransactionResult);
      emitter.onNext(result);
    }
    emitter.onComplete();
  }

  private void setListener(ObservableEmitter<ActionResult> emitter, ActionResult result) {
    mGateway.setEventListener((eventCode, customMessage) -> {
      result.setEventCode(eventCode);
      result.setMessage(customMessage);
      emitter.onNext(result);
    });
  }

  /** Aborta a operação atual; emite {@code true} se o SDK confirmou o abort. */
  public Single<Boolean> abort() throws Exception {
    return Single.fromCallable(mGateway::abort);
  }

  public Observable<Boolean> isAuthenticated() throws Exception {
    return Observable.create(emitter -> {
      emitter.onNext(mGateway.isAuthenticated());
      emitter.onComplete();
    });
  }

  public Observable<ActionResult> initializeAndActivatePinpad(String activationCode) throws Exception {
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      mGateway.setEventListener((eventCode, customMessage) -> {
        actionResult.setEventCode(eventCode);
        actionResult.setMessage(customMessage);
        emitter.onNext(actionResult);
      });
      PlugPagInitializationResult result = mGateway.initializeAndActivatePinpad(activationCode);
      if (result != null && result.getResult() == PlugPag.RET_OK) {
        emitter.onNext(new ActionResult());
      } else {
        emitter.onError(new RuntimeException(result != null ? result.getErrorMessage() : "Sem resposta do PlugPag."));
      }
      emitter.onComplete();
    });
  }

  public Observable<ActionResult> doRefund(String transactionCode, String transactionId) {
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      setListener(emitter, actionResult);
      PlugPagTransactionResult result = mGateway.voidPayment(new PlugPagVoidData(transactionCode, transactionId, true));
      sendResponse(emitter, result, actionResult);
    });
  }

  public Observable<ActionResult> getLastTransaction() throws Exception {
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      PlugPagTransactionResult result = mGateway.getLastApprovedTransaction();
      sendResponse(emitter, result, actionResult);
    });
  }

  /** Reinicia o terminal. Falha do SDK agora é propagada (antes era engolida e o Dart recebia true). */
  public void rebootDevice() throws Exception {
    mGateway.reboot();
  }

  /** Emite um beep. Falha do SDK agora é propagada (antes era engolida e o Dart recebia true). */
  public void beep() throws Exception {
    mGateway.beep();
  }

  public String getSerialNumber() throws Exception {
    return mGateway.getSerialNumber();
  }

  public UserDataResult getUserData() throws Exception {
    return ResultMappers.toUserDataResult(mGateway.getUserData());
  }

  public List<PlugPagInstallment> calculateInstallments(String saleValue, int installmentType) throws Exception {
    return mGateway.calculateInstallments(saleValue, installmentType);
  }
}
