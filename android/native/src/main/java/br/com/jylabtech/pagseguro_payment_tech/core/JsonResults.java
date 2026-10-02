package br.com.jylabtech.pagseguro_payment_tech.core;

import com.google.gson.Gson;

/** Serialização JSON dos resultados enviados ao Dart (uma única instância de Gson, thread-safe). */
public final class JsonResults {
  private static final Gson GSON = new Gson();

  private JsonResults() {
  }

  public static String toJson(Object result) {
    return GSON.toJson(result);
  }
}
