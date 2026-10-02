package br.com.jylabtech.pagseguro_payment_tech.payments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.jylabtech.pagseguro_payment_tech.core.ActionResult;
import br.com.jylabtech.pagseguro_payment_tech.core.UserDataResult;
import br.com.jylabtech.pagseguro_payment_tech.testing.SdkResults;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException;
import io.reactivex.observers.TestObserver;

/** Casos de uso de pagamento sobre um SDK fake: dados enviados ao PlugPag e tradução do resultado. */
public class PaymentsUseCaseTest {

  private FakePaymentGateway gateway;
  private PaymentsUseCase useCase;

  @Before
  public void setUp() {
    gateway = new FakePaymentGateway();
    useCase = new PaymentsUseCase(gateway, FakePaymentGateway.validLicence());
  }

  private static void assertPayment(PlugPagPaymentData data, int type, int amount, int installmentType, int installments,
                                    String ref, boolean print, boolean partial, boolean carne) {
    assertEquals(type, data.getType());
    assertEquals(amount, data.getAmount());
    assertEquals(installmentType, data.getInstallmentType());
    assertEquals(installments, data.getInstallments());
    assertEquals(ref, data.getUserReference());
    assertEquals(print, data.getPrintReceipt());
    assertEquals(partial, data.getPartialPay());
    assertEquals(carne, data.isCarne());
  }

  @Test
  public void cadaPagamentoMontaOPlugPagPaymentDataCerto() throws Exception {
    useCase.doCreditPayment(1000, "R1", true, false, false).test().assertComplete();
    useCase.doCreditPaymentParc(3000, 3, 6, "R2", false, true, true).test().assertComplete();
    useCase.doDebitPayment(1500, "R3", true, false, false).test().assertComplete();
    useCase.doVoucherPayment(2000, "R4", true, false, false).test().assertComplete();
    useCase.doPixPayment(2500, "R5", false, false, false).test().assertComplete();
    useCase.doStartPayment(7, 990, 2, 4, "R6", true, true, false).test().assertComplete();

    assertEquals(6, gateway.payments.size());
    assertPayment(gateway.payments.get(0), 1, 1000, 1, 1, "R1", true, false, false);
    assertPayment(gateway.payments.get(1), 1, 3000, 3, 6, "R2", false, true, true);
    assertPayment(gateway.payments.get(2), 2, 1500, 1, 1, "R3", true, false, false);
    assertPayment(gateway.payments.get(3), 3, 2000, 1, 1, "R4", true, false, false);
    assertPayment(gateway.payments.get(4), 5, 2500, 1, 1, "R5", false, false, false);
    assertPayment(gateway.payments.get(5), 7, 990, 2, 4, "R6", true, true, false);
  }

  @Test
  public void booleanosNulosUsamOsPadroesDoDart() {
    PlugPagPaymentData data = PaymentsUseCase.paymentData(1, 100, 1, 1, "R", null, null, null);
    assertTrue(data.getPrintReceipt());
    assertFalse(data.getPartialPay());
    assertFalse(data.isCarne());
  }

  @Test
  public void pagamentoAprovadoEmiteActionResultPreenchido() throws Exception {
    TestObserver<ActionResult> observer = useCase.doDebitPayment(2590, "R", true, false, false).test();
    observer.assertComplete().assertNoErrors().assertValueCount(1);
    ActionResult result = observer.values().get(0);
    assertEquals("TC1", result.getTransactionCode());
    assertEquals("TI1", result.getTransactionId());
    assertEquals(0, result.getResult());
    assertEquals(1, result.getBuildResponse());
  }

  @Test
  public void eventosDoSdkSaoRepassadosAntesDoResultado() throws Exception {
    gateway.eventsDuringOperation.add(new Object[] {1, "INSIRA OU PASSE O CARTAO"});
    gateway.eventsDuringOperation.add(new Object[] {16, "SENHA"});
    TestObserver<ActionResult> observer = useCase.doCreditPayment(1000, "R", true, false, false).test();
    observer.assertComplete().assertValueCount(3);
    assertTrue(gateway.calls.indexOf("setEventListener") < gateway.calls.indexOf("doPayment"));
  }

  @Test
  public void pagamentoRecusadoViraErroComMensagemECodigoDoSdk() throws Exception {
    gateway.transactionResult = SdkResults.transaction("result", 5, "message", "NAO AUTORIZADA", "errorCode", "A5");
    TestObserver<ActionResult> observer = useCase.doDebitPayment(1000, "R", true, false, false).test();
    observer.assertNoValues().assertError(PlugPagException.class);
    PlugPagException error = (PlugPagException) observer.errors().get(0);
    assertEquals("A5 - NAO AUTORIZADA", error.getMessage());
    assertEquals("A5", error.getErrorCode());
  }

  @Test
  public void resultadoSemCodigoOuNuloViraErroSemNullPointerException() throws Exception {
    gateway.transactionResult = SdkResults.transaction("result", null, "message", "falhou");
    useCase.doDebitPayment(1000, "R", true, false, false).test().assertError(PlugPagException.class);
    gateway.transactionResult = null;
    useCase.doDebitPayment(1000, "R", true, false, false).test().assertError(PlugPagException.class);
  }

  @Test
  public void excecaoDoSdkViraErroDoObservable() throws Exception {
    gateway.toThrow = new PlugPagException("Serviço indisponível");
    useCase.doPixPayment(1000, "R", true, false, false).test().assertError(PlugPagException.class);
  }

  @Test
  public void semLicencaNenhumPagamentoChamaOSdk() {
    PaymentsUseCase semLicenca = new PaymentsUseCase(gateway, FakePaymentGateway.noLicence());
    String[] operations = {"credit", "creditParc", "debit", "voucher", "pix", "start", "abort",
        "isAuthenticated", "activate", "lastTransaction", "userData", "installments"};
    for (String operation : operations) {
      try {
        switch (operation) {
          case "credit": semLicenca.doCreditPayment(1, "R", true, false, false); break;
          case "creditParc": semLicenca.doCreditPaymentParc(1, 3, 2, "R", true, false, false); break;
          case "debit": semLicenca.doDebitPayment(1, "R", true, false, false); break;
          case "voucher": semLicenca.doVoucherPayment(1, "R", true, false, false); break;
          case "pix": semLicenca.doPixPayment(1, "R", true, false, false); break;
          case "start": semLicenca.doStartPayment(1, 1, 1, 1, "R", true, false, false); break;
          case "abort": semLicenca.abort(); break;
          case "isAuthenticated": semLicenca.isAuthenticated(); break;
          case "activate": semLicenca.initializeAndActivatePinpad("1"); break;
          case "lastTransaction": semLicenca.getLastTransaction(); break;
          case "userData": semLicenca.getUserData(); break;
          default: semLicenca.calculateInstallments("1", 1); break;
        }
        fail(operation + " deveria lançar sem licença");
      } catch (Exception expected) {
        assertEquals(operation, PaymentsUseCase.INVALID_LICENCE_MESSAGE, expected.getMessage());
      }
    }
    assertEquals(Collections.emptyList(), gateway.calls);
  }

  @Test
  public void observablesSoChamamOSdkQuandoInscritos() throws Exception {
    useCase.doDebitPayment(1000, "R", true, false, false);
    useCase.getLastTransaction();
    useCase.isAuthenticated();
    useCase.abort();
    assertEquals(Collections.emptyList(), gateway.calls);
  }

  @Test
  public void estornoEnviaCodigoEIdComImpressao() throws Exception {
    useCase.doRefund("TC9", "TI9").test().assertComplete().assertValueCount(1);
    assertEquals("TC9", gateway.lastVoid.getTransactionCode());
    assertEquals("TI9", gateway.lastVoid.getTransactionId());
    assertTrue(gateway.lastVoid.getPrintReceipt());
  }

  @Test
  public void ultimaTransacaoAprovada() throws Exception {
    useCase.getLastTransaction().test().assertComplete().assertValueCount(1);
    gateway.transactionResult = SdkResults.transaction("result", -1, "message", "Sem transação");
    useCase.getLastTransaction().test().assertError(PlugPagException.class);
  }

  @Test
  public void abortCompletaEPropagaFalha() throws Exception {
    useCase.abort().test().assertValue(true);
    assertTrue(gateway.calls.contains("abort"));
    gateway.abortConfirmed = false;
    useCase.abort().test().assertValue(false);
    gateway.abortConfirmed = true;
    gateway.toThrow = new PlugPagException("falha abort");
    useCase.abort().test().assertError(PlugPagException.class);
  }

  @Test
  public void isAuthenticatedEmiteOStatusDoTerminal() throws Exception {
    useCase.isAuthenticated().test().assertValue(true).assertComplete();
    gateway.authenticated = false;
    useCase.isAuthenticated().test().assertValue(false).assertComplete();
  }

  @Test
  public void ativacaoRepassaEventosECompletaComSucesso() throws Exception {
    gateway.eventsDuringOperation.add(new Object[] {0, "Ativando"});
    TestObserver<ActionResult> observer = useCase.initializeAndActivatePinpad("403938").test();
    observer.assertComplete().assertValueCount(2);
    assertEquals("403938", gateway.lastActivationCode);
    assertEquals("Ativando", observer.values().get(0).getMessage());
  }

  @Test
  public void ativacaoRecusadaViraErroComMensagemDoSdk() throws Exception {
    gateway.activationResult = new PlugPagInitializationResult(-1, "E1", "Código inválido");
    TestObserver<ActionResult> observer = useCase.initializeAndActivatePinpad("000").test();
    observer.assertError(RuntimeException.class);
    assertEquals("Código inválido", observer.errors().get(0).getMessage());
  }

  @Test
  public void rebootBeepESerialChamamOSdkEPropagamFalha() throws Exception {
    useCase.rebootDevice();
    useCase.beep();
    assertEquals("SN-1", useCase.getSerialNumber());
    assertEquals(Arrays.asList("reboot", "beep", "getSerialNumber"), gateway.calls);

    gateway.toThrow = new PlugPagException("falha");
    try {
      useCase.beep();
      fail("beep deveria propagar a falha");
    } catch (PlugPagException expected) {
      assertEquals("falha", expected.getMessage());
    }
  }

  @Test
  public void dadosDoUsuarioEParcelas() throws Exception {
    UserDataResult user = useCase.getUserData();
    assertEquals("userData", user.getMethod());
    assertEquals(gateway.userData.getCompanyName(), user.getCompanyName());
    assertEquals(gateway.userData.getEmail(), user.getEmail());

    gateway.installments = Arrays.asList(new PlugPagInstallment(1, 2000, 2000));
    List<PlugPagInstallment> installments = useCase.calculateInstallments("2000", 3);
    assertEquals(1, installments.size());
    assertTrue(gateway.calls.contains("calculateInstallments:2000:3"));
  }

  @Test
  public void licencaNulaTambemBloqueia() {
    PaymentsUseCase semTech = new PaymentsUseCase(gateway, null);
    try {
      semTech.getUserData();
      fail();
    } catch (Exception expected) {
      assertEquals(PaymentsUseCase.INVALID_LICENCE_MESSAGE, expected.getMessage());
    }
    assertNull(gateway.lastVoid);
  }
}
