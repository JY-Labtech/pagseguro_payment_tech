package br.com.jylabtech.pagseguro_payment_tech.testing;

import java.util.ArrayList;
import java.util.List;

import br.com.jylabtech.pagseguro_payment_tech.core.EventSink;

/** EventSink de teste: registra cada evento enviado ao Dart, em ordem. */
public final class RecordingEventSink implements EventSink {
  public static final class Event {
    public final String method;
    public final Object arguments;

    Event(String method, Object arguments) {
      this.method = method;
      this.arguments = arguments;
    }

    @Override
    public String toString() {
      return method + "(" + arguments + ")";
    }
  }

  public final List<Event> events = new ArrayList<>();

  @Override
  public void send(String method, Object arguments) {
    events.add(new Event(method, arguments));
  }

  public List<String> methods() {
    List<String> methods = new ArrayList<>();
    for (Event event : events) {
      methods.add(event.method);
    }
    return methods;
  }

  /** Argumentos de todos os eventos com o nome informado. */
  public List<Object> argumentsOf(String method) {
    List<Object> args = new ArrayList<>();
    for (Event event : events) {
      if (event.method.equals(method)) {
        args.add(event.arguments);
      }
    }
    return args;
  }

  public Object last(String method) {
    List<Object> args = argumentsOf(method);
    return args.isEmpty() ? null : args.get(args.size() - 1);
  }
}
