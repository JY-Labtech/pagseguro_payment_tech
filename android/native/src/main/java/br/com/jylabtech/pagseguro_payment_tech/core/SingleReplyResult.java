package br.com.jylabtech.pagseguro_payment_tech.core;

import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.MethodChannel;

/**
 * Envolve o {@link MethodChannel.Result} e garante que ele seja completado UMA única vez.
 * Respostas extras (ex.: erro depois de um success) são ignoradas, evitando o
 * "Reply already submitted" do Flutter.
 */
public final class SingleReplyResult implements MethodChannel.Result {
  private final MethodChannel.Result delegate;
  private final AtomicBoolean replied = new AtomicBoolean(false);

  private SingleReplyResult(MethodChannel.Result delegate) {
    this.delegate = delegate;
  }

  public static SingleReplyResult wrap(MethodChannel.Result result) {
    if (result instanceof SingleReplyResult) {
      return (SingleReplyResult) result;
    }
    return new SingleReplyResult(result);
  }

  /** {@code true} se o result já foi completado. */
  public boolean hasReplied() {
    return replied.get();
  }

  @Override
  public void success(Object value) {
    if (replied.compareAndSet(false, true)) {
      delegate.success(value);
    }
  }

  @Override
  public void error(String errorCode, String errorMessage, Object errorDetails) {
    if (replied.compareAndSet(false, true)) {
      delegate.error(errorCode, errorMessage, errorDetails);
    }
  }

  @Override
  public void notImplemented() {
    if (replied.compareAndSet(false, true)) {
      delegate.notImplemented();
    }
  }
}
