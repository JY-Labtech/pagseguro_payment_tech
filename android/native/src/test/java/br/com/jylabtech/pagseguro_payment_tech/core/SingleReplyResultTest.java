package br.com.jylabtech.pagseguro_payment_tech.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SingleReplyResultTest {

  @Test
  public void soAPrimeiraRespostaChegaAoFlutter() {
    RecordingResult raw = new RecordingResult();
    SingleReplyResult result = SingleReplyResult.wrap(raw);

    assertFalse(result.hasReplied());
    result.success(true);
    result.error("ERROR", "depois do success", null);
    result.notImplemented();
    result.success(false);

    assertTrue(result.hasReplied());
    assertEquals(Collections.singletonList("success"), raw.replies);
    assertEquals(true, raw.successValue);
  }

  @Test
  public void erroPrimeiroBloqueiaOResto() {
    RecordingResult raw = new RecordingResult();
    SingleReplyResult result = SingleReplyResult.wrap(raw);

    result.error("ERROR", "Terminal ocupado", null);
    result.success(true);

    assertEquals(Arrays.asList("error"), raw.replies);
    assertEquals("ERROR", raw.errorCode);
  }

  @Test
  public void wrapNaoEmpilhaGuardas() {
    SingleReplyResult result = SingleReplyResult.wrap(new RecordingResult());
    assertSame(result, SingleReplyResult.wrap(result));
  }
}
