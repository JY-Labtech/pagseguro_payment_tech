package br.com.jylabtech.pagseguro_payment_tech.testing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.RejectedExecutionException;

import br.com.jylabtech.pagseguro_payment_tech.core.AsyncExecutor;

/**
 * AsyncExecutor de teste que enfileira as tarefas: permite verificar o que acontece ANTES de a
 * thread de background/main thread rodarem e então drenar as filas.
 */
public final class QueuedAsyncExecutor implements AsyncExecutor {
  public final Deque<Runnable> backgroundTasks = new ArrayDeque<>();
  public final Deque<Runnable> mainTasks = new ArrayDeque<>();
  public boolean shutdown;

  @Override
  public void background(Runnable task) {
    if (shutdown) {
      throw new RejectedExecutionException("shutdown");
    }
    backgroundTasks.add(task);
  }

  @Override
  public void main(Runnable task) {
    mainTasks.add(task);
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  public void runBackground() {
    while (!backgroundTasks.isEmpty()) {
      backgroundTasks.poll().run();
    }
  }

  public void runMain() {
    while (!mainTasks.isEmpty()) {
      mainTasks.poll().run();
    }
  }

  public void runAll() {
    while (!backgroundTasks.isEmpty() || !mainTasks.isEmpty()) {
      runBackground();
      runMain();
    }
  }
}
