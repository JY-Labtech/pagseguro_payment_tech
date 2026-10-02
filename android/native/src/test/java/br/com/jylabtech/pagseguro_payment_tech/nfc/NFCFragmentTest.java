package br.com.jylabtech.pagseguro_payment_tech.nfc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

import br.com.jylabtech.pagseguro_payment_tech.testing.RecordingEventSink;
import br.com.jylabtech.pagseguro_payment_tech.user.UserData;

/** Contrato dos eventos NFC com o NfcHandler do Dart. */
public class NFCFragmentTest {

  @Test
  public void eventosDeSucessoEErroUsamOsNomesDoDart() {
    RecordingEventSink sink = new RecordingEventSink();
    NFCFragment fragment = new NFCFragment(sink);

    fragment.showSuccessWrite(1);
    fragment.showSuccessReWrite("10");
    fragment.showSuccessFormat(1);
    fragment.showSuccessDebitNfc("5");
    fragment.showSuccessRefundNfc("15");
    fragment.showErrorRead("r");
    fragment.showErrorWrite("w");
    fragment.showErrorReWrite("rw");
    fragment.showErrorFormat("f");
    fragment.showErrorDebitNfc("d");
    fragment.showErrorRefundNfc("rf");
    fragment.showError("e");

    assertEquals(Arrays.asList(
        "showSuccessWrite", "showSuccessReWrite", "showSuccessFormat", "showSuccessDebitNfc",
        "showSuccessRefundNfc", "showErrorRead", "showErrorWrite", "showErrorReWrite",
        "showErrorFormat", "showErrorDebitNfc", "showErrorRefundNfc", "onError"), sink.methods());
    assertEquals(1, sink.last("showSuccessWrite"));
    assertEquals("10", sink.last("showSuccessReWrite"));
    assertEquals("rf", sink.last("showErrorRefundNfc"));
    assertEquals("e", sink.last("onError"));
  }

  @Test
  public void leituraEnviaListaComOsCamposSemAsteriscos() {
    RecordingEventSink sink = new RecordingEventSink();
    NFCFragment fragment = new NFCFragment(sink);
    UserData data = new UserData("1000****", "Ana*****", "123*****", "TAG*****", "999*****", "1*******", "7*******", "500*****");

    fragment.showSuccess(data);

    assertEquals(Arrays.asList(Arrays.asList(
        data.getValue().replace("*", ""), data.getName().replace("*", ""), data.getCpf().replace("*", ""),
        data.getNumberTag().replace("*", ""), data.getCellPhone().replace("*", ""),
        data.getActive().replace("*", ""), data.getOpenValue().replace("*", ""))), sink.argumentsOf("showSuccess"));
  }
}
