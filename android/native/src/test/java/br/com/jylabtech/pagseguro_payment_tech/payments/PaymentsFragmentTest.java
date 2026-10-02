package br.com.jylabtech.pagseguro_payment_tech.payments;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import br.com.jylabtech.pagseguro_payment_tech.testing.RecordingEventSink;

/**
 * Contrato dos eventos de pagamento com o Dart (PaymentTypeHandler em payment_types.dart):
 * nome do método e formato do argumento.
 */
public class PaymentsFragmentTest {

  @Test
  public void cadaEventoUsaONomeEOArgumentoEsperadosPeloDart() {
    RecordingEventSink sink = new RecordingEventSink();
    PaymentsFragment fragment = new PaymentsFragment(sink);

    fragment.onTransactionSuccess();
    fragment.onError("erro");
    fragment.onMessage("mensagem");
    fragment.onFinishedResponse("{\"method\":\"transaction\"}");
    fragment.onLoading(true);
    fragment.onAbortedSuccessfully();
    fragment.disposeDialog();
    fragment.onActivationDialog();
    fragment.onAuthProgress("Terminal ativado");

    assertEquals(Arrays.asList(
        "onTransactionSuccess", "onError", "onMessage", "onFinishedResponse", "onLoading",
        "onAbortedSuccessfully", "disposeDialog", "activeDialog", "onAuthProgress"), sink.methods());
    assertEquals(true, sink.last("onTransactionSuccess"));
    assertEquals("erro", sink.last("onError"));
    assertEquals("mensagem", sink.last("onMessage"));
    assertEquals("{\"method\":\"transaction\"}", sink.last("onFinishedResponse"));
    assertEquals(true, sink.last("onLoading"));
    assertEquals("Terminal ativado", sink.last("onAuthProgress"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void writeToFileETransactionInfoEnviamMapaComCodigoIdEResposta() {
    RecordingEventSink sink = new RecordingEventSink();
    PaymentsFragment fragment = new PaymentsFragment(sink);

    fragment.writeToFile("TC", "TI", "{json}");
    fragment.onTransactionInfo("TC2", "TI2", "{json2}");

    Map<String, String> write = (Map<String, String>) sink.last("writeToFile");
    assertEquals("TC", write.get("transactionCode"));
    assertEquals("TI", write.get("transactionId"));
    assertEquals("{json}", write.get("response"));
    Map<String, String> info = (Map<String, String>) sink.last("onTransactionInfo");
    assertEquals("TC2", info.get("transactionCode"));
    assertEquals("TI2", info.get("transactionId"));
    assertEquals("{json2}", info.get("response"));
  }
}
