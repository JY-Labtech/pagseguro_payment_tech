package br.com.jylabtech.pagseguro_payment_tech.core;

import android.os.Handler;
import android.os.Looper;

import io.flutter.plugin.common.MethodChannel;

/**
 * Envia eventos ao Dart sempre pela main thread.
 *
 * O MethodChannel só pode ser usado na main thread (o Flutter lança
 * "Methods marked with @UiThread must be executed on the main thread"). Vários fluxos do SDK
 * emitem eventos a partir de threads de background (RxJava io, listeners do PlugPag); aqui o
 * evento é repassado para a main thread em vez de falhar.
 */
public final class MainThreadEventSink implements EventSink {
  private static volatile Handler mainHandler;

  private final MethodChannel channel;

  public MainThreadEventSink(MethodChannel channel) {
    this.channel = channel;
  }

  private static Handler mainHandler() {
    Handler handler = mainHandler;
    if (handler == null) {
      synchronized (MainThreadEventSink.class) {
        handler = mainHandler;
        if (handler == null) {
          handler = new Handler(Looper.getMainLooper());
          mainHandler = handler;
        }
      }
    }
    return handler;
  }

  @Override
  public void send(final String method, final Object arguments) {
    if (channel == null) {
      return;
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      channel.invokeMethod(method, arguments);
    } else {
      mainHandler().post(() -> channel.invokeMethod(method, arguments));
    }
  }
}
