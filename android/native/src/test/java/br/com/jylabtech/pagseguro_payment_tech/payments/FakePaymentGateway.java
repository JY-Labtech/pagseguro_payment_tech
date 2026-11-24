package br.com.jylabtech.pagseguro_payment_tech.payments;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.jylabtech.pagseguro_payment_tech.licence.CheckLicenceEntity;
import br.com.jylabtech.pagseguro_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceResult;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceStorage;
import br.com.jylabtech.pagseguro_payment_tech.testing.SdkResults;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagUserDataResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;

/** Fake do SDK PlugPag: registra as chamadas e devolve respostas configuráveis. */
final class FakePaymentGateway implements PaymentGateway {
  final List<String> calls = new ArrayList<>();
  final List<PlugPagPaymentData> payments = new ArrayList<>();
  PlugPagVoidData lastVoid;
  String lastActivationCode;
  EventCallback listener;

  boolean authenticated = true;
  boolean abortConfirmed = true;
  PlugPagTransactionResult transactionResult = SdkResults.transaction(
      "result", 0, "message", "APROVADA", "transactionCode", "TC1", "transactionId", "TI1",
      "hostNsu", "HN", "nsu", "N", "amount", "2590", "paymentType", 1);
  PlugPagInitializationResult activationResult = new PlugPagInitializationResult(0, "", "");
  List<PlugPagInstallment> installments = new ArrayList<>();
  PlugPagUserDataResult userData = new PlugPagUserDataResult("Rua", "Cidade", "Doc", "Comp", "Empresa", "Apelido", "UF", "e@x.com");
  String serial = "SN-1";
  Exception toThrow;
  /** Eventos (código, mensagem) emitidos durante doPayment/voidPayment/ativação. */
  final List<Object[]> eventsDuringOperation = new ArrayList<>();

  private void record(String call) throws Exception {
    calls.add(call);
    if (toThrow != null) {
      throw toThrow;
    }
  }

  private void emitEvents() {
    if (listener != null) {
      for (Object[] event : eventsDuringOperation) {
        listener.onEvent((Integer) event[0], (String) event[1]);
      }
    }
  }

  @Override
  public void setEventListener(EventCallback callback) {
    calls.add("setEventListener");
    listener = callback;
  }

  @Override
  public PlugPagTransactionResult doPayment(PlugPagPaymentData paymentData) throws Exception {
    record("doPayment");
    payments.add(paymentData);
    emitEvents();
    return transactionResult;
  }

  @Override
  public PlugPagTransactionResult voidPayment(PlugPagVoidData voidData) throws Exception {
    record("voidPayment");
    lastVoid = voidData;
    emitEvents();
    return transactionResult;
  }

  @Override
  public PlugPagTransactionResult getLastApprovedTransaction() throws Exception {
    record("getLastApprovedTransaction");
    return transactionResult;
  }

  @Override
  public boolean abort() throws Exception {
    record("abort");
    return abortConfirmed;
  }

  @Override
  public boolean isAuthenticated() throws Exception {
    record("isAuthenticated");
    return authenticated;
  }

  @Override
  public PlugPagInitializationResult initializeAndActivatePinpad(String activationCode) throws Exception {
    record("initializeAndActivatePinpad");
    lastActivationCode = activationCode;
    emitEvents();
    return activationResult;
  }

  @Override
  public void reboot() throws Exception {
    record("reboot");
  }

  @Override
  public void beep() throws Exception {
    record("beep");
  }

  @Override
  public String getSerialNumber() throws Exception {
    record("getSerialNumber");
    return serial;
  }

  @Override
  public PlugPagUserDataResult getUserData() throws Exception {
    record("getUserData");
    return userData;
  }

  @Override
  public List<PlugPagInstallment> calculateInstallments(String saleValue, int installmentType) throws Exception {
    record("calculateInstallments:" + saleValue + ":" + installmentType);
    return installments;
  }

  // ---------------------------------------------------------------------------------------------
  // Licença (cache local) usada pela segunda barreira do PaymentsUseCase
  // ---------------------------------------------------------------------------------------------

  /** Storage em memória. */
  static final class MemoryStorage implements LicenceStorage {
    CheckLicenceEntity entity;

    @Override
    public CheckLicenceEntity read() {
      return entity;
    }

    @Override
    public void save(CheckLicenceEntity entity) {
      this.entity = entity;
    }

    @Override
    public void delete() {
      entity = null;
    }
  }

  /**
   * CheckLicenceTech já liberada pelo gate: validada online agora (vence em 1 ano), como acontece
   * no plugin antes de qualquer caso de uso. O atalho só-cache exige essa validação no processo.
   */
  static CheckLicenceTech validLicence() {
    MemoryStorage storage = new MemoryStorage();
    Date now = new Date();
    CheckLicenceEntity entity = new CheckLicenceEntity();
    entity.setExpiredAt(new Date(now.getTime() + 365L * 24 * 60 * 60 * 1000));
    entity.setLicenceKey("test-licence-key-0000");
    entity.setLicenceEncrypt("test-licence-key-0000");
    CheckLicenceTech tech = new CheckLicenceTech(storage, (url, key) -> LicenceResult.success(entity), Date::new);
    if (!tech.validateLicence("test-licence-key-0000", null).isSuccess()) {
      throw new IllegalStateException("licença de teste não validou");
    }
    return tech;
  }

  /** CheckLicenceTech sem cache (licença nunca validada ou removida). */
  static CheckLicenceTech noLicence() {
    return licence(new MemoryStorage());
  }

  private static CheckLicenceTech licence(LicenceStorage storage) {
    return new CheckLicenceTech(storage, (url, key) -> LicenceResult.error("sem rede"), Date::new);
  }
}
