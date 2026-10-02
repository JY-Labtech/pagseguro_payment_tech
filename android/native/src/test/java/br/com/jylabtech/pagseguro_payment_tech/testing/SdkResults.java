package br.com.jylabtech.pagseguro_payment_tech.testing;

import java.lang.reflect.Field;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;

/** Fábrica de objetos do SDK PlugPag para testes (os campos são val do Kotlin, sem setters). */
public final class SdkResults {
  private SdkResults() {
  }

  /** Cria um PlugPagTransactionResult com os campos informados em pares nome/valor. */
  public static PlugPagTransactionResult transaction(Object... fieldValues) {
    try {
      PlugPagTransactionResult result = new PlugPagTransactionResult();
      for (int i = 0; i < fieldValues.length; i += 2) {
        Field field = PlugPagTransactionResult.class.getDeclaredField((String) fieldValues[i]);
        field.setAccessible(true);
        field.set(result, fieldValues[i + 1]);
      }
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
