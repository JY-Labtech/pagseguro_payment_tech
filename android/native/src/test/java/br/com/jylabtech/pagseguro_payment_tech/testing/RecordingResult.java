package br.com.jylabtech.pagseguro_payment_tech.testing;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodChannel;

/** Result de teste que registra todas as respostas recebidas. */
public final class RecordingResult implements MethodChannel.Result {
  public final List<String> replies = new ArrayList<>();
  public Object successValue;
  public String errorCode;
  public String errorMessage;

  @Override
  public void success(Object value) {
    replies.add("success");
    successValue = value;
  }

  @Override
  public void error(String code, String message, Object details) {
    replies.add("error");
    errorCode = code;
    errorMessage = message;
  }

  @Override
  public void notImplemented() {
    replies.add("notImplemented");
  }
}
