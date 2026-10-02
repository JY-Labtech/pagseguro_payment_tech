package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.List;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagAbortResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagUserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagBeepData;

/** {@link PaymentGateway} de produção: delega ao {@link PlugPag} sem mudar o comportamento. */
public final class PlugPagPaymentGateway implements PaymentGateway {
  private final PlugPag plugPag;

  public PlugPagPaymentGateway(PlugPag plugPag) {
    this.plugPag = plugPag;
  }

  @Override
  public void setEventListener(EventCallback callback) {
    plugPag.setEventListener(eventData -> callback.onEvent(eventData.getEventCode(), eventData.getCustomMessage()));
  }

  @Override
  public PlugPagTransactionResult doPayment(PlugPagPaymentData paymentData) throws Exception {
    return plugPag.doPayment(paymentData);
  }

  @Override
  public PlugPagTransactionResult voidPayment(PlugPagVoidData voidData) throws Exception {
    return plugPag.voidPayment(voidData);
  }

  @Override
  public PlugPagTransactionResult getLastApprovedTransaction() throws Exception {
    return plugPag.getLastApprovedTransaction();
  }

  @Override
  public boolean abort() throws Exception {
    PlugPagAbortResult result = plugPag.abort();
    return result != null && result.getResult() == PlugPag.RET_OK;
  }

  @Override
  public boolean isAuthenticated() throws Exception {
    return plugPag.isAuthenticated();
  }

  @Override
  public PlugPagInitializationResult initializeAndActivatePinpad(String activationCode) throws Exception {
    return plugPag.initializeAndActivatePinpad(new PlugPagActivationData(activationCode));
  }

  @Override
  public void reboot() throws Exception {
    plugPag.reboot();
  }

  @Override
  public void beep() throws Exception {
    plugPag.beep(new PlugPagBeepData(PlugPagBeepData.FREQUENCE_LEVEL_1, 200));
  }

  @Override
  public String getSerialNumber() {
    return plugPag.getSerialNumber();
  }

  @Override
  public PlugPagUserDataResult getUserData() throws Exception {
    return plugPag.getUserData();
  }

  @Override
  public List<PlugPagInstallment> calculateInstallments(String saleValue, int installmentType) throws Exception {
    return plugPag.calculateInstallments(saleValue, installmentType);
  }
}
