package br.com.jylabtech.pagseguro_payment_tech.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link AsyncExecutor} de produção: uma thread de background única (preserva a ordem das
 * chamadas) e a main thread do Android. Finalizado em {@link #shutdown()}.
 */
public final class AndroidAsyncExecutor implements AsyncExecutor {
  private final ExecutorService background;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public AndroidAsyncExecutor() {
    this.background = Executors.newSingleThreadExecutor();
  }

  @Override
  public void background(Runnable task) {
    background.execute(task);
  }

  @Override
  public void main(Runnable task) {
    mainHandler.post(task);
  }

  @Override
  public boolean isShutdown() {
    return background.isShutdown();
  }

  /** Interrompe o background. Respostas já postadas na main thread ainda são entregues. */
  public void shutdown() {
    background.shutdownNow();
  }
}
