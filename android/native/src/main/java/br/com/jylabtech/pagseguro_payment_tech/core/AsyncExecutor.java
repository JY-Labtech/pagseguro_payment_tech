package br.com.jylabtech.pagseguro_payment_tech.core;

/**
 * Executa trabalho bloqueante (rede, IPC com o PlugPagService) fora da main thread e devolve a
 * resposta na main thread, onde o MethodChannel.Result e os eventos podem ser usados.
 */
public interface AsyncExecutor {
  /** Agenda uma tarefa em background. Pode lançar RejectedExecutionException se já foi finalizado. */
  void background(Runnable task);

  /** Agenda uma tarefa na main thread. */
  void main(Runnable task);

  /** {@code true} depois de finalizado (onDetachedFromEngine). */
  boolean isShutdown();

  /** Executa tudo na thread atual (testes e chamadas que já estão na thread certa). */
  AsyncExecutor DIRECT = new AsyncExecutor() {
    @Override
    public void background(Runnable task) {
      task.run();
    }

    @Override
    public void main(Runnable task) {
      task.run();
    }

    @Override
    public boolean isShutdown() {
      return false;
    }
  };
}
