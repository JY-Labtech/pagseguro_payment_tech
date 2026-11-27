package br.com.jylabtech.pagseguro_payment_tech.payments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.testing.RecordingEventSink;
import br.com.jylabtech.pagseguro_payment_tech.testing.SdkResults;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Fluxo completo de pagamento até os eventos enviados ao Dart, com o SDK fake e os schedulers do
 * RxJava trocados por trampoline (execução síncrona, sem Looper do Android).
 */
public class PaymentsPresenterTest {

  private FakePaymentGateway gateway;
  private RecordingEventSink sink;
  private PaymentsPresenter presenter;

  @BeforeClass
  public static void setUpSchedulers() {
    RxAndroidPlugins.setInitMainThreadSchedulerHandler(callable -> Schedulers.trampoline());
    RxAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
  }

  @AfterClass
  public static void tearDownSchedulers() {
    RxAndroidPlugins.reset();
    RxJavaPlugins.reset();
  }

  @Before
  public void setUp() {
    gateway = new FakePaymentGateway();
    sink = new RecordingEventSink();
    presenter = new PaymentsPresenter(new PaymentsUseCase(gateway), new PaymentsFragment(sink));
  }

  private JSONObject lastFinishedResponse() throws Exception {
    return new JSONObject((String) sink.last("onFinishedResponse"));
  }

  @Test
  public void pagamentoAprovadoEnviaWriteToFileMensagemRespostaESucesso() throws Exception {
    presenter.doDebitPayment(2590, "R", true, false, false);

    assertEquals(Arrays.asList("writeToFile", "onMessage", "onFinishedResponse", "onTransactionSuccess"), sink.methods());
    assertEquals("APROVADA", sink.last("onMessage"));
    JSONObject response = lastFinishedResponse();
    assertEquals("transaction", response.getString("method"));
    assertEquals(0, response.getInt("result"));
    assertEquals("TC1", response.getString("transactionCode"));
    assertEquals("TI1", response.getString("transactionId"));
    @SuppressWarnings("unchecked")
    Map<String, String> file = (Map<String, String>) sink.last("writeToFile");
    assertEquals("TC1", file.get("transactionCode"));
    assertEquals("TI1", file.get("transactionId"));
    assertTrue(file.get("response").contains("\"transactionCode\":\"TC1\""));
  }

  @Test
  public void todosOsTiposDePagamentoUsamOMesmoFluxo() throws Exception {
    presenter.creditPayment(1000, "R", true, false, false);
    presenter.creditPaymentParc(3000, 3, 2, "R", true, false, false);
    presenter.doVoucherPayment(2000, "R", true, false, false);
    presenter.doPixPayment(2500, "R", true, false, false);
    presenter.startPayment(1, 990, 1, 1, "R", true, false, false);
    presenter.doRefund("TC", "TI");

    assertEquals(5, gateway.payments.size());
    assertTrue(gateway.calls.contains("voidPayment"));
    assertEquals(6, sink.argumentsOf("onTransactionSuccess").size());
    assertEquals(6, sink.argumentsOf("onFinishedResponse").size());
  }

  @Test
  public void terminalNaoAtivadoPedeAtivacaoSemPagarESemSucesso() {
    gateway.authenticated = false;
    try {
      presenter.creditPayment(1000, "R", true, false, false);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    assertEquals(Arrays.asList("activeDialog", "disposeDialog"), sink.methods());
    assertTrue(gateway.payments.isEmpty());
  }

  @Test
  public void pagamentoRecusadoEnviaErroERespostaComResult999999() throws Exception {
    gateway.transactionResult = SdkResults.transaction("result", 5, "message", "NAO AUTORIZADA", "errorCode", "A5");
    presenter.doDebitPayment(1000, "R", true, false, false);

    assertEquals(Arrays.asList("onMessage", "onError", "onFinishedResponse", "disposeDialog"), sink.methods());
    // O PlugPagException do SDK compõe "código - mensagem".
    assertEquals("A5 - NAO AUTORIZADA", sink.last("onError"));
    JSONObject response = lastFinishedResponse();
    assertEquals("transaction", response.getString("method"));
    assertEquals(PaymentsPresenter.RESULT_ERROR, response.getInt("result"));
    assertEquals("A5 - NAO AUTORIZADA", response.getString("errorMessage"));
    assertFalse(sink.methods().contains("onTransactionSuccess"));
  }

  @Test
  public void erroSemMensagemNaoEnviaNullAoDart() throws Exception {
    gateway.toThrow = new NullPointerException();
    presenter.doDebitPayment(1000, "R", true, false, false);
    assertEquals("NullPointerException", sink.last("onError"));
    assertEquals("NullPointerException", lastFinishedResponse().getString("errorMessage"));
  }

  @Test
  public void eventosDeSenhaMostramValorEAsteriscos() throws Exception {
    gateway.eventsDuringOperation.add(new Object[] {17, null});
    gateway.eventsDuringOperation.add(new Object[] {16, null});
    gateway.eventsDuringOperation.add(new Object[] {16, null});
    gateway.eventsDuringOperation.add(new Object[] {0, "PROCESSANDO SENHA OK"});
    presenter.doDebitPayment(1500, "R", true, false, false);

    List<Object> messages = sink.argumentsOf("onMessage");
    assertEquals(String.format("VALOR: %.2f\nSENHA: %s", 15.0, ""), messages.get(0));
    assertEquals(String.format("VALOR: %.2f\nSENHA: %s", 15.0, "*"), messages.get(1));
    assertEquals(String.format("VALOR: %.2f\nSENHA: %s", 15.0, "**"), messages.get(2));
    assertEquals("PROCESSANDO", messages.get(3));
  }

  @Test
  public void contadorDeSenhaReiniciaACadaTransacao() throws Exception {
    gateway.eventsDuringOperation.add(new Object[] {16, null});
    presenter.doDebitPayment(1000, "R", true, false, false);
    sink.events.clear();
    presenter.doDebitPayment(1000, "R", true, false, false);
    assertEquals(String.format("VALOR: %.2f\nSENHA: %s", 10.0, "*"), sink.argumentsOf("onMessage").get(0));
  }

  @Test
  public void checkMessageRemoveOTrechoDeSenha() {
    assertEquals("VALOR 10,00", PaymentsPresenter.checkMessage("VALOR 10,00 SENHA:"));
    assertEquals("APROVADA", PaymentsPresenter.checkMessage("APROVADA"));
    assertEquals("", PaymentsPresenter.checkMessage(""));
    assertEquals("", PaymentsPresenter.checkMessage("SENHA"));
    assertEquals(null, PaymentsPresenter.checkMessage(null));
  }

  @Test
  public void abortChamaOSdkEFalhaViraOnErrorSemDerrubarOApp() throws Exception {
    presenter.abortTransaction();
    assertTrue(gateway.calls.contains("abort"));
    // Abort confirmado pelo SDK: evento onAbortedSuccessfully (antes nunca era enviado).
    assertEquals(Collections.singletonList("onAbortedSuccessfully"), sink.methods());
    assertEquals(true, sink.last("onAbortedSuccessfully"));

    // Abort não confirmado (nada em andamento): nenhum evento.
    sink.events.clear();
    gateway.abortConfirmed = false;
    presenter.abortTransaction();
    assertTrue(sink.events.isEmpty());

    gateway.toThrow = new PlugPagException("Nada para abortar");
    presenter.abortTransaction();
    assertEquals(Collections.singletonList("onError"), sink.methods());
    assertEquals("Nada para abortar", sink.last("onError"));
  }

  @Test
  public void ativacaoComSucessoEnviaProgressoERespostaActivate() throws Exception {
    presenter.activate("403938");

    assertEquals("403938", gateway.lastActivationCode);
    assertTrue(sink.argumentsOf("onAuthProgress").contains("Terminal ativado"));
    assertEquals(Arrays.asList(true, false), sink.argumentsOf("onLoading"));
    JSONObject response = lastFinishedResponse();
    assertEquals("activate", response.getString("method"));
    assertEquals(0, response.getInt("result"));
    assertEquals("Terminal ativado", response.getString("message"));
  }

  @Test
  public void ativacaoRecusadaEnviaErroERespostaComFalha() throws Exception {
    gateway.activationResult = new PlugPagInitializationResult(-1, "E1", "Código inválido");
    presenter.activate("000");

    assertEquals("Código inválido", sink.last("onError"));
    assertEquals("Error ao ativar terminal", sink.argumentsOf("onMessage").get(1));
    JSONObject response = lastFinishedResponse();
    assertEquals("activate", response.getString("method"));
    assertEquals(PaymentsPresenter.RESULT_ERROR, response.getInt("result"));
  }

  @Test
  public void statusDoTerminalAtivadoENaoAtivado() throws Exception {
    presenter.isAuthenticate();
    assertEquals("Terminal ativado", sink.last("onAuthProgress"));
    assertEquals(0, lastFinishedResponse().getInt("result"));

    gateway.authenticated = false;
    presenter.isAuthenticate();
    assertEquals("Terminal não ativado", sink.last("onAuthProgress"));
    JSONObject response = lastFinishedResponse();
    assertEquals("activate", response.getString("method"));
    assertEquals("Terminal não ativado", response.getString("message"));
    assertEquals(PaymentsPresenter.RESULT_ERROR, response.getInt("result"));
  }

  @Test
  public void statusDoTerminalComFalhaDoSdk() throws Exception {
    gateway.toThrow = new PlugPagException("Serviço indisponível");
    presenter.isAuthenticate();
    assertEquals("Serviço indisponível", sink.last("onError"));
    assertEquals(PaymentsPresenter.RESULT_ERROR, lastFinishedResponse().getInt("result"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void ultimaTransacaoChegaEmOnTransactionInfo() throws Exception {
    presenter.getLastTransaction();
    Map<String, String> info = (Map<String, String>) sink.last("onTransactionInfo");
    assertEquals("TC1", info.get("transactionCode"));
    assertEquals("TI1", info.get("transactionId"));
    assertEquals("TC1", new JSONObject(info.get("response")).getString("transactionCode"));

    gateway.transactionResult = SdkResults.transaction("result", -1, "message", "Sem transação");
    presenter.getLastTransaction();
    assertEquals("Sem transação", sink.last("onError"));
  }

  @Test
  public void dadosDoUsuarioChegamEmOnFinishedResponse() throws Exception {
    presenter.getUserData();
    JSONObject response = lastFinishedResponse();
    assertEquals("userData", response.getString("method"));
    assertEquals("Empresa", response.getString("companyName"));
    assertEquals("e@x.com", response.getString("email"));
  }

  @Test
  public void parcelasSaoConvertidasParaMapas() throws Exception {
    gateway.installments = Arrays.asList(new PlugPagInstallment(2, 1000, 2000));
    List<Map<String, Object>> maps = presenter.calculateInstallments("2000", 3);
    assertEquals(1, maps.size());
    assertEquals(2, maps.get(0).get("quantity"));
    assertEquals(2000, maps.get(0).get("total"));
  }

  @Test
  public void rebootBeepESerialDelegamAoSdk() throws Exception {
    presenter.rebootDevice();
    presenter.beep();
    assertEquals("SN-1", presenter.getSerialNumber());
    assertEquals(Arrays.asList("reboot", "beep", "getSerialNumber"), gateway.calls);
  }

  @Test
  public void errorMessageNuncaDevolveNull() {
    assertEquals("Erro desconhecido", PaymentsPresenter.errorMessage(null));
    assertEquals("IllegalStateException", PaymentsPresenter.errorMessage(new IllegalStateException()));
    assertEquals("x", PaymentsPresenter.errorMessage(new IllegalStateException("x")));
  }

  @Test
  public void disposeSemOperacaoNaoQuebra() {
    presenter.dispose();
    presenter.dispose();
  }
}
