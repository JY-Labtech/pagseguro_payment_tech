package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.List;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagUserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;

/**
 * Fachada fina sobre as chamadas do SDK PlugPag usadas pelos pagamentos.
 * Em produção é {@link PlugPagPaymentGateway}; nos testes, um fake (o PlugPag real exige
 * Context e o PlugPagService instalado no terminal).
 */
public interface PaymentGateway {

  /** Evento de progresso da transação (código do evento + mensagem customizada do SDK). */
  interface EventCallback {
    void onEvent(int eventCode, String customMessage);
  }

  void setEventListener(EventCallback callback);

  PlugPagTransactionResult doPayment(PlugPagPaymentData paymentData) throws Exception;

  PlugPagTransactionResult voidPayment(PlugPagVoidData voidData) throws Exception;

  PlugPagTransactionResult getLastApprovedTransaction() throws Exception;

  /** Aborta a operação atual. @return {@code true} se o SDK confirmou o abort (RET_OK). */
  boolean abort() throws Exception;

  boolean isAuthenticated() throws Exception;

  PlugPagInitializationResult initializeAndActivatePinpad(String activationCode) throws Exception;

  void reboot() throws Exception;

  void beep() throws Exception;

  String getSerialNumber() throws Exception;

  PlugPagUserDataResult getUserData() throws Exception;

  List<PlugPagInstallment> calculateInstallments(String saleValue, int installmentType) throws Exception;
}
