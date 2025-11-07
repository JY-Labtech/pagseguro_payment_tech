package br.com.jylabtech.pagseguro_payment_tech;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Classe bridge que delega toda lógica para o plugin real empacotado no AAR.
 */
public final class PagseguroPaymentTechPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
  private final PagseguroPaymentTechNativePlugin delegate = new PagseguroPaymentTechNativePlugin();

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    delegate.onAttachedToEngine(binding);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    delegate.onDetachedFromEngine(binding);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    delegate.onMethodCall(call, result);
  }
}