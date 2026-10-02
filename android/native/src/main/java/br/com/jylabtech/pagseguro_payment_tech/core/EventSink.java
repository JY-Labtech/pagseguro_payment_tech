package br.com.jylabtech.pagseguro_payment_tech.core;

/**
 * Destino dos eventos enviados ao Dart (onMessage, onError, onFinishedResponse, showSuccess...).
 * Em produção é {@link MainThreadEventSink}; nos testes, um fake que registra as chamadas.
 */
public interface EventSink {
  void send(String method, Object arguments);
}
