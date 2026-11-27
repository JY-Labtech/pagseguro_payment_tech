package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.List;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.core.ActionResult;
import br.com.jylabtech.pagseguro_payment_tech.core.BaseResult;
import br.com.jylabtech.pagseguro_payment_tech.core.JsonResults;
import br.com.jylabtech.pagseguro_payment_tech.core.ResultMappers;
import br.com.jylabtech.pagseguro_payment_tech.core.UserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import io.flutter.plugin.common.MethodChannel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Orquestra os casos de uso de pagamento e traduz o resultado em eventos para o Dart
 * (onMessage, onError, onFinishedResponse, writeToFile...).
 */
public class PaymentsPresenter {
  /** Código de erro usado nos JSON de onFinishedResponse quando a operação falha. */
  static final int RESULT_ERROR = 999999;

  private final PaymentsUseCase mUseCase;
  private final PaymentsFragment mFragment;
  /** Transação/ativação/status/última transação em andamento. */
  private Disposable mSubscribe;
  /** Abort tem assinatura própria para não sobrescrever (e vazar) a da transação em andamento. */
  private Disposable mAbortSubscribe;
  private int countPassword = 0;

  public PaymentsPresenter(PaymentsUseCase useCase, PaymentsFragment fragment) {
    this.mUseCase = useCase;
    this.mFragment = fragment;
  }

  public PaymentsPresenter(PlugPag plugPag, MethodChannel channel) {
    this(new PaymentsUseCase(plugPag), new PaymentsFragment(channel));
  }

  public void rebootDevice() throws Exception {
    mUseCase.rebootDevice();
  }

  public void beep() throws Exception {
    mUseCase.beep();
  }

  public void creditPaymentParc(int value, int type, int parc, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doCreditPaymentParc(value, type, parc, userReference, printReceipt, partialPay, isCarne), value);
  }

  public void creditPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doCreditPayment(value, userReference, printReceipt, partialPay, isCarne), value);
  }

  public void doDebitPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doDebitPayment(value, userReference, printReceipt, partialPay, isCarne), value);
  }

  public void doPixPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doPixPayment(value, userReference, printReceipt, partialPay, isCarne), value);
  }

  public void doVoucherPayment(int value, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doVoucherPayment(value, userReference, printReceipt, partialPay, isCarne), value);
  }

  public void doRefund(String transactionCode, String transactionId) throws Exception {
    doAction(mUseCase.doRefund(transactionCode, transactionId), 0);
  }

  public void startPayment(int type, int amount, int installmentType, int installments, String userReference, Boolean printReceipt, Boolean partialPay, Boolean isCarne) throws Exception {
    doAction(mUseCase.doStartPayment(type, amount, installmentType, installments, userReference, printReceipt, partialPay, isCarne), amount);
  }

  /**
   * Executa a transação se o terminal estiver autenticado.
   * Terminal não ativado: envia activeDialog + disposeDialog e encerra SEM onTransactionSuccess.
   * (Antes o activeDialog era enviado da thread io, o que o Flutter rejeita, e o mSubscribe podia
   * ainda ser null nesse momento.)
   */
  private void doAction(Observable<ActionResult> action, int value) throws Exception {
    final ActionResult actionResult = new ActionResult();
    actionResult.setMethod("transaction");
    final boolean[] notAuthenticated = {false};
    countPassword = 0;
    mSubscribe =
        mUseCase
            .isAuthenticated()
            .flatMap(authenticated -> {
              if (!authenticated) {
                notAuthenticated[0] = true;
                return Observable.<ActionResult>empty();
              }
              return action;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose(() -> mFragment.disposeDialog())
            .subscribe(
                (ActionResult result) -> {
                  writeToFile(result);
                  if (result.getEventCode() == PlugPagEventData.EVENT_CODE_NO_PASSWORD
                      || result.getEventCode() == PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD) {
                    mFragment.onMessage(checkMessagePassword(result.getEventCode(), value));
                  } else {
                    mFragment.onMessage(checkMessage(result.getMessage()));
                  }
                  result.setMethod("transaction");
                  checkAndSendResponse(result);
                },
                throwable -> {
                  String message = errorMessage(throwable);
                  mFragment.onMessage(message);
                  mFragment.onError(message);
                  actionResult.setErrorMessage(message);
                  actionResult.setResult(RESULT_ERROR);
                  sendResponse(actionResult);
                  mFragment.disposeDialog();
                },
                () -> {
                  if (notAuthenticated[0]) {
                    mFragment.onActivationDialog();
                    mFragment.disposeDialog();
                  } else {
                    mFragment.onTransactionSuccess();
                  }
                });
  }

  static String errorMessage(Throwable throwable) {
    if (throwable == null) {
      return "Erro desconhecido";
    }
    String message = throwable.getMessage();
    return message != null ? message : throwable.getClass().getSimpleName();
  }

  private void sendResponse(BaseResult result) {
    mFragment.onFinishedResponse(JsonResults.toJson(result));
  }

  private void checkAndSendResponse(ActionResult result) {
    if (result.getBuildResponse() != 0) {
      sendResponse(result);
    }
  }

  String checkMessagePassword(int eventCode, int value) {
    StringBuilder strPassword = new StringBuilder();
    if (eventCode == PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD) {
      countPassword++;
    }
    if (eventCode == PlugPagEventData.EVENT_CODE_NO_PASSWORD) {
      countPassword = 0;
    }
    for (int count = countPassword; count > 0; count--) {
      strPassword.append("*");
    }
    return String.format("VALOR: %.2f\nSENHA: %s", (value / 100.0), strPassword.toString());
  }

  static String checkMessage(String message) {
    if (message != null && !message.isEmpty() && message.contains("SENHA")) {
      String[] strings = message.split("SENHA");
      return strings.length > 0 ? strings[0].trim() : "";
    }
    return message;
  }

  private void writeToFile(ActionResult result) {
    if (result.getTransactionCode() != null && result.getTransactionId() != null) {
      mFragment.writeToFile(result.getTransactionCode(), result.getTransactionId(), JsonResults.toJson(result));
    }
  }

  /**
   * Aborta a operação atual. Abort confirmado pelo SDK envia onAbortedSuccessfully (antes o evento
   * nunca era enviado). Falha do SDK vira onError (antes derrubava o app: subscribe sem onError).
   */
  public void abortTransaction() throws Exception {
    disposeAbort();
    mAbortSubscribe =
        mUseCase
            .abort()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                aborted -> {
                  if (aborted) {
                    mFragment.onAbortedSuccessfully();
                  }
                },
                throwable -> mFragment.onError(errorMessage(throwable)));
  }

  public void activate(String activationCode) throws Exception {
    ActionResult resultAction = new ActionResult();
    resultAction.setMethod("activate");
    mFragment.onMessage("Ativando terminal");

    mSubscribe =
        mUseCase
            .initializeAndActivatePinpad(activationCode)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(disposable -> mFragment.onLoading(true))
            .doOnComplete(() -> {
              mFragment.onLoading(false);
              resultAction.setMessage("Terminal ativado");
              resultAction.setResult(0);
              mFragment.onMessage("Terminal ativado");
              mFragment.onAuthProgress("Terminal ativado");
              mFragment.disposeDialog();
              sendResponse(resultAction);
            })
            .doOnDispose(() -> mFragment.disposeDialog())
            .subscribe(
                actionResult -> {
                  actionResult.setMethod("activate");
                  mFragment.onAuthProgress(actionResult.getMessage());
                },
                throwable -> {
                  String message = errorMessage(throwable);
                  mFragment.onLoading(false);
                  mFragment.onMessage("Error ao ativar terminal");
                  mFragment.disposeDialog();
                  mFragment.onError(message);
                  resultAction.setErrorMessage(message);
                  resultAction.setResult(RESULT_ERROR);
                  sendResponse(resultAction);
                });
  }

  public void isAuthenticate() throws Exception {
    ActionResult resultAction = new ActionResult();
    resultAction.setMethod("activate");
    mFragment.onMessage("Obtendo status do terminal");
    mSubscribe =
        mUseCase
            .isAuthenticated()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(disposable -> mFragment.onLoading(true))
            .doOnComplete(() -> {
              mFragment.onLoading(false);
              mFragment.disposeDialog();
            })
            .doOnDispose(() -> mFragment.disposeDialog())
            .subscribe(
                result -> {
                  if (result) {
                    resultAction.setMessage("Terminal ativado");
                    resultAction.setResult(0);
                    mFragment.onAuthProgress("Terminal ativado");
                  } else {
                    mFragment.onAuthProgress("Terminal não ativado");
                    resultAction.setMessage("Terminal não ativado");
                    resultAction.setResult(RESULT_ERROR);
                  }
                  sendResponse(resultAction);
                },
                throwable -> {
                  String message = errorMessage(throwable);
                  mFragment.onLoading(false);
                  mFragment.onMessage("Error ao obter status do terminal");
                  mFragment.disposeDialog();
                  mFragment.onError(message);
                  resultAction.setErrorMessage(message);
                  resultAction.setResult(RESULT_ERROR);
                  sendResponse(resultAction);
                });
  }

  public void getLastTransaction() throws Exception {
    mSubscribe =
        mUseCase
            .getLastTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                actionResult -> mFragment.onTransactionInfo(
                    actionResult.getTransactionCode(),
                    actionResult.getTransactionId(),
                    JsonResults.toJson(actionResult)),
                throwable -> mFragment.onError(errorMessage(throwable)));
  }

  public String getSerialNumber() throws Exception {
    return mUseCase.getSerialNumber();
  }

  /** Envia os dados do estabelecimento em onFinishedResponse (method "userData"). */
  public void getUserData() throws Exception {
    UserDataResult result = this.mUseCase.getUserData();
    sendResponse(result);
  }

  public List<Map<String, Object>> calculateInstallments(String saleValue, int installmentType) throws Exception {
    return ResultMappers.installmentsToMaps(this.mUseCase.calculateInstallments(saleValue, installmentType));
  }

  private void disposeAbort() {
    if (mAbortSubscribe != null) {
      mAbortSubscribe.dispose();
      mAbortSubscribe = null;
    }
  }

  public void dispose() {
    if (mSubscribe != null) {
      mSubscribe.dispose();
    }
    disposeAbort();
  }
}
