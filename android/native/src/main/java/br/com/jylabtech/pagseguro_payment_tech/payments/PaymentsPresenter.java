package br.com.jylabtech.pagseguro_payment_tech.payments;
import android.content.Context;
import android.util.Log;

import br.com.jylabtech.pagseguro_payment_tech.core.BaseResult;
import br.com.jylabtech.pagseguro_payment_tech.core.UserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import br.com.jylabtech.pagseguro_payment_tech.core.ActionResult;
import com.google.gson.Gson;
import io.flutter.plugin.common.MethodChannel;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
public class PaymentsPresenter {
  private PaymentsUseCase mUseCase;
  private PaymentsFragment mFragment;
  private Disposable mSubscribe;
  private Boolean hasAborted = false;
  private int countPassword = 0;
  @Inject
  public PaymentsPresenter(PlugPag plugPag, MethodChannel channel, Context context) {
    mFragment = new PaymentsFragment(channel);
    mUseCase = new PaymentsUseCase(plugPag, context, channel);
  }

  public void rebootDevice() {mUseCase.rebootDevice();}

  public void beep() {mUseCase.beep();}

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

  private void doAction(Observable<ActionResult> action, int value) throws Exception {
      ActionResult actionResult = new ActionResult();
      actionResult.setMethod("transaction");
    mSubscribe =
      mUseCase
        .isAuthenticated()
        .filter(aBoolean -> {
          if (!aBoolean) {
            mFragment.onActivationDialog();
            mSubscribe.dispose();
          }
          return aBoolean;
        })
        .flatMap(
          (Function<Boolean, ObservableSource<ActionResult>>) aBoolean -> action
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete(() -> mFragment.onTransactionSuccess())
        .doOnDispose(() -> mFragment.disposeDialog())
        .subscribe(
          (ActionResult result) -> {
            writeToFile(result);
            if (
              result.getEventCode() ==
              PlugPagEventData.EVENT_CODE_NO_PASSWORD ||
              result.getEventCode() ==
              PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD
            ) {
              mFragment.onMessage(
                checkMessagePassword(result.getEventCode(), value)
              );
            } else {
              mFragment.onMessage(checkMessage(result.getMessage()));
            }
            result.setMethod("transaction");
            checkAndSendResponse(result); //Verificar o que ele fazia antes para enviar o onFinishedResponse
          },
          throwable -> {
            mFragment.onMessage(throwable.getMessage());
            mFragment.onError(throwable.getMessage());
            actionResult.setErrorMessage(throwable.getMessage());
            actionResult.setResult(999999);
            sendResponse(actionResult);
            mFragment.disposeDialog();
          }
        );
  }


  private void sendResponse(BaseResult result) {
      String response = new Gson().toJson(result);
      mFragment.onFinishedResponse(response);
  }

   private void checkAndSendResponse(ActionResult result) {
    if (result.getBuildResponse() != 0) {
      sendResponse(result);
    }
  }

  private String checkMessagePassword(int eventCode, int value) {
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
    return String.format(
      "VALOR: %.2f\nSENHA: %s",
      (value / 100.0),
      strPassword.toString()
    );
  }
  private String checkMessage(String message) {
    if (message != null && !message.isEmpty() && message.contains("SENHA")) {
      String[] strings = message.split("SENHA");
      return strings[0].trim();
    }
    return message;
  }
  private void writeToFile(ActionResult result) {
    if (
      result.getTransactionCode() != null && result.getTransactionId() != null
    ) {
      String response;
      response = new Gson().toJson(result);
      mFragment.writeToFile(
        result.getTransactionCode(),
        result.getTransactionId(),
        response
      );
    }
  }
  public void abortTransaction() throws Exception {
    mSubscribe =
      mUseCase
        .abort()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
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
              mFragment.onLoading(false);
              mFragment.onMessage("Error ao ativar terminal");
              mFragment.disposeDialog();
              mFragment.onError(throwable.getMessage());
              resultAction.setErrorMessage(throwable.getMessage());
              resultAction.setResult(999999);
              sendResponse(resultAction);
              Log.d("print", "*** throw: " + throwable.getMessage());
          }
        );
  }
  public void isAuthenticate() throws Exception {
    ActionResult resultAction = new ActionResult();
    resultAction.setMethod("activate");
        mFragment.onMessage("Obtendo status do terminal");
        mSubscribe = mUseCase.isAuthenticated()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> mFragment.onLoading(true))
                .doOnComplete(()-> {
                    mFragment.onLoading(false);
                    mFragment.disposeDialog();
                })
                .doOnDispose(() -> mFragment.disposeDialog())
                .subscribe(
                        result -> {
                            if(result){
                                resultAction.setMethod("activate");
                                 resultAction.setMessage("Terminal ativado");
                                 resultAction.setResult(0);
                                mFragment.onAuthProgress("Terminal ativado");
                            }else {
                                mFragment.onAuthProgress("Terminal não ativado");
                                resultAction.setMessage("Terminal não ativado");
                                resultAction.setResult(999999);
                            }
                            sendResponse(resultAction);
                        },
                        throwable -> {
                            mFragment.onLoading(false);
                            mFragment.onMessage("Error ao obter status do terminal");
                            mFragment.disposeDialog();
                            mFragment.onError(throwable.getMessage());
                            resultAction.setErrorMessage(throwable.getMessage());
                            resultAction.setResult(999999);
                            sendResponse(resultAction);
                            Log.d("print", "*** throw: " + throwable.getMessage());
                        });
    }
  public void getLastTransaction() throws Exception {
    mSubscribe =
      mUseCase
        .getLastTransaction()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(
          actionResult -> {
            String response;
            response = new Gson().toJson(actionResult);
            mFragment.onTransactionInfo(
              actionResult.getTransactionCode(),
              actionResult.getTransactionId(),
              response
            );
          },
          throwable -> mFragment.onError(throwable.getMessage())
        );
  }

  public String getSerialNumber() {
    return mUseCase.getSerialNumber();
  }

    public boolean checkValidLicence(String licenceKey, String licenceInternalKey) throws Exception {
      boolean aux = false;
        boolean result =  this.mUseCase.checkValidLicence(licenceKey, licenceInternalKey);
        if(result) {
            aux = true;
        }

        return aux;
    }

    public void getUserData() throws Exception {
        UserDataResult result = this.mUseCase.getUserData();
        result.setMethod("userData");
        sendResponse(result);
    }

    public List<Map<String, Object>> calculateInstallments(String saleValue, int installmentType) throws Exception {
        List<PlugPagInstallment> installments = this.mUseCase.calculateInstallments(saleValue, installmentType);
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        for (PlugPagInstallment installment : installments) {
            Map<String, Object> installmentMap = new HashMap<>();
            installmentMap.put("quantity", installment.getQuantity());
            installmentMap.put("amount", installment.getAmount());
            installmentMap.put("total", installment.getTotal());
            resultList.add(installmentMap);
        }
        
        return resultList;
    }
  
  public void dispose() {
    if (mSubscribe != null) {
      mSubscribe.dispose();
    }
  }
}