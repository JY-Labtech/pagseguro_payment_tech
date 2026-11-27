package br.com.jylabtech.pagseguro_payment_tech;

import androidx.annotation.NonNull;

import br.com.jylabtech.pagseguro_payment_tech.core.ChannelRequestHandler;
import br.com.jylabtech.pagseguro_payment_tech.core.PagSeguroPayment;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * Implementação nativa completa do plugin Pagseguro encapsulada no AAR.
 * Recebe as chamadas do method channel e delega para {@link ChannelRequestHandler}, que chama
 * {@link PagSeguroPayment} (SDK PlugPag) e garante uma única resposta por chamada.
 */
public final class PagseguroPaymentTechNativePlugin implements FlutterPlugin, MethodCallHandler {
  private static final String CHANNEL_NAME = "pagseguro_payment_tech";

  private MethodChannel channel;
  private PagSeguroPayment pagseguroPayment;
  private ChannelRequestHandler requestHandler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    pagseguroPayment = new PagSeguroPayment(binding.getApplicationContext(), channel);
    requestHandler = new ChannelRequestHandler(pagseguroPayment);
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    ChannelRequestHandler handler = requestHandler;
    if (handler == null) {
      result.error(ChannelRequestHandler.NOT_INITIALIZED_CODE, "Plugin não inicializado.", null);
      return;
    }
    handler.onMethodCall(call, result);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }
    if (requestHandler != null) {
      requestHandler.dispose();
      requestHandler = null;
    }
    if (pagseguroPayment != null) {
      pagseguroPayment.dispose();
      pagseguroPayment = null;
    }
  }
}
