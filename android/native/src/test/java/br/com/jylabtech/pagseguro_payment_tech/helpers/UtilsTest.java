package br.com.jylabtech.pagseguro_payment_tech.helpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Helpers puros usados na gravação/leitura NFC. */
public class UtilsTest {

  @Test
  public void asteriscosCompletamAte16CaracteresESaoRemovidosNaLeitura() {
    String padded = Utils.adicionaAsterisco("1000");
    assertEquals("************1000", padded);
    assertEquals("1000", Utils.removeAsterisco(padded));
    assertEquals("12345678901234567", Utils.adicionaAsterisco("12345678901234567"));
    assertEquals("", Utils.removeAsterisco(null));
  }

  @Test
  public void stringParaBlocoDe16BytesEVolta() {
    byte[] block = Utils.convertString2Bytes("Ana");
    assertEquals(16, block.length);
    assertEquals("Ana", Utils.convertBytes2String(block, false));
    assertEquals("414E41", Utils.convertBytes2String(new byte[] {0x41, 0x4E, 0x41}, true));
  }

  @Test
  public void hexEInteiros() {
    assertArrayEquals(new byte[] {(byte) 0xFF, 0x0A}, Utils.hexStringToByteArray("FF0A"));
    assertArrayEquals(new byte[] {0, 0, 1, 0}, Utils.convertInt2Bytes(256, 4));
  }
}
