package br.com.jylabtech.pagseguro_payment_tech.payments;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import br.com.jylabtech.pagseguro_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagCustomPrinterLayout;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagBeepData;
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException;
import br.com.jylabtech.pagseguro_payment_tech.core.ActionResult;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.flutter.plugin.common.MethodChannel;

public class PaymentsUseCase {

  private final PlugPag mPlugPag;

  private final int TYPE_CREDITO = 1;
  private final int TYPE_DEBITO = 2;
  private final int TYPE_VOUCHER = 3;
  private final int TYPE_PIX = 5;

  private final int INSTALLMENT_TYPE_A_VISTA = 1;
  private final int INSTALLMENT_TYPE_PARC_VENDEDOR = 2;
  private final int INSTALLMENT_TYPE_PARC_COMPRADOR = 3;

  private CheckLicenceTech licenceTech;
  private PaymentsFragment mFragment;
  
	public PaymentsUseCase(PlugPag plugPag, Context context, MethodChannel channel) {
    mPlugPag = plugPag;
    this.licenceTech = new CheckLicenceTech(context);
    this.mFragment = new PaymentsFragment(channel);
  }

  public Observable<ActionResult> doCreditPayment(
    int value,
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        TYPE_CREDITO,
        value,
        INSTALLMENT_TYPE_A_VISTA,
        1,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }

  public Observable<ActionResult> doCreditPaymentParc(
    int value,
    int type,
    int parc,
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne    
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        TYPE_CREDITO,
        value,
        type,
        parc,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }

  public Observable<ActionResult> doDebitPayment(
    int value,
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        TYPE_DEBITO,
        value,
        INSTALLMENT_TYPE_A_VISTA,
        1,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }

  public Observable<ActionResult> doVoucherPayment(
    int value,
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        TYPE_VOUCHER,
        value,
        INSTALLMENT_TYPE_A_VISTA,
        1,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }

  public Observable<ActionResult> doPixPayment(
    int value,
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        TYPE_PIX,
        value,
        INSTALLMENT_TYPE_A_VISTA,
        1,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }

  public Observable<ActionResult> doStartPayment(
    int type,
    int amount,
    int installmentType,
    int installments,    
    String userReference,
    Boolean printReceipt, 
    Boolean partialPay, 
    Boolean isCarne    
  ) throws Exception {
    isValidLicence();
    return doPayment(
      new PlugPagPaymentData(
        type,
        amount,
        installmentType,
        installments,
        userReference,
        printReceipt,
        partialPay,
        isCarne
      )
    );
  }  

  private Observable<ActionResult> doPayment(
    final PlugPagPaymentData paymentData
  ) {
    PlugPagCustomPrinterLayout customDialog = new PlugPagCustomPrinterLayout();
    customDialog.setMaxTimeShowPopup(30);
    mPlugPag.setPlugPagCustomPrinterLayout(customDialog);
    return Observable.create(emitter -> {
      ActionResult result = new ActionResult();
      setListener(emitter, result);
      PlugPagTransactionResult plugPagTransactionResult = mPlugPag.doPayment(
        paymentData
      );
      sendResponse(emitter, plugPagTransactionResult, result);
    });
  }
  private void sendResponse(
    ObservableEmitter<ActionResult> emitter,
    PlugPagTransactionResult plugPagTransactionResult,
    ActionResult result
  ) {
    if (plugPagTransactionResult.getResult() != 0) {
      emitter.onError(
        new PlugPagException(
          plugPagTransactionResult.getMessage(),
          plugPagTransactionResult.getErrorCode()
        )
      );
    } else {
      result.setTransactionCode(plugPagTransactionResult.getTransactionCode());
      result.setTransactionId(plugPagTransactionResult.getTransactionId());
      result.buildResponse(plugPagTransactionResult);
      emitter.onNext(result);
    }
    emitter.onComplete();
  }
  private void setListener(
    ObservableEmitter<ActionResult> emitter,
    ActionResult result
  ) {
    mPlugPag.setEventListener(plugPagEventData -> {
      result.setEventCode(plugPagEventData.getEventCode());
      result.setMessage(plugPagEventData.getCustomMessage());
      emitter.onNext(result);
    });
  }
  public Completable abort() throws Exception {
    isValidLicence();
      return Completable.create(emitter -> mPlugPag.abort());
  }
  public Observable<Boolean> isAuthenticated() throws Exception {
    isValidLicence();
    return Observable.create(emitter -> {
      emitter.onNext(mPlugPag.isAuthenticated());
      emitter.onComplete();
    });
  }
  public Observable<ActionResult> initializeAndActivatePinpad(
    String activationCode
  ) throws Exception {
    isValidLicence();
    PlugPagCustomPrinterLayout customDialog = new PlugPagCustomPrinterLayout();
    customDialog.setMaxTimeShowPopup(30);
    mPlugPag.setPlugPagCustomPrinterLayout(customDialog);
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      mPlugPag.setEventListener(plugPagEventData -> {
        actionResult.setEventCode(plugPagEventData.getEventCode());
        actionResult.setMessage(plugPagEventData.getCustomMessage());
        emitter.onNext(actionResult);
      });
      PlugPagInitializationResult result = mPlugPag.initializeAndActivatePinpad(
        new PlugPagActivationData(activationCode)
      );
      if (result.getResult() == PlugPag.RET_OK) {
        emitter.onNext(new ActionResult());
      } else {
        emitter.onError(new RuntimeException(result.getErrorMessage()));
      }
      emitter.onComplete();
    });
  }
  public Observable<ActionResult> doRefund(
    String transactionCode,
    String transactionId
  )  {
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      setListener(emitter, actionResult);
      PlugPagTransactionResult result = mPlugPag.voidPayment(
        new PlugPagVoidData(transactionCode, transactionId, true)
      );
      sendResponse(emitter, result, actionResult);
    });
  }
  public Observable<ActionResult> getLastTransaction() throws Exception {
    isValidLicence();
    return Observable.create(emitter -> {
      ActionResult actionResult = new ActionResult();
      PlugPagTransactionResult result = mPlugPag.getLastApprovedTransaction();
      sendResponse(emitter, result, actionResult);
    });
  }

  public void rebootDevice() {
    try {
      mPlugPag.reboot();
    }catch (Exception e){
    }
  }

  public void beep() {
    try {
      PlugPagBeepData beepData = new PlugPagBeepData(
              PlugPagBeepData.FREQUENCE_LEVEL_1, 200);
      mPlugPag.beep(beepData);
    }catch (Exception e){
    }
  }

  public String getSerialNumber() {
    return mPlugPag.getSerialNumber();
  }

  public boolean checkValidLicence(String licenceKey, String licenceInternalKey) throws Exception {
    boolean aux = false;
    ActionResult result = new ActionResult();
    result.setMethod("licence");
    this.mFragment.onMessage("Verificando licença");
    LicenceResult resultLicence =  licenceTech.validateLicence(licenceKey, licenceInternalKey);

    if(resultLicence.getResult() == 0) {
      aux = true;
      this.mFragment.onMessage("Licença ativada");
      result.setResult(0);
      result.setMessage("Licença ativada com sucesso");
      String response = new Gson().toJson(result);
      this.mFragment.onFinishedResponse(response);
    }

    if(!aux) {
      String errorMessage = resultLicence.getErrorMessage();
      if(errorMessage == null || errorMessage.isEmpty()) {
        errorMessage = "Licença não ativada";
      }
      this.mFragment.onMessage(errorMessage);
      result.setResult(999999);
      result.setErrorMessage(errorMessage);
      this.mFragment.onError(errorMessage);
      String response = new Gson().toJson(result);
      this.mFragment.onFinishedResponse(response);
      throw new Exception("Licença não ativada, consulte o suporte do plugin.");
    }

    return aux;
  }

  private void isValidLicence() throws Exception {
    boolean result = licenceTech.validateExistsLicence();
    if(result) {
      return;
    }

    throw new Exception("Licença inválida ou não foi inicializada!");
  }


}