package br.com.jylabtech.pagseguro_payment_tech;

import androidx.annotation.NonNull;

import br.com.jylabtech.pagseguro_payment_tech.core.AndroidAsyncExecutor;
import br.com.jylabtech.pagseguro_payment_tech.core.ChannelRequestHandler;
import br.com.jylabtech.pagseguro_payment_tech.core.PagSeguroPayment;
import br.com.jylabtech.pagseguro_payment_tech.licence.LicenceConfig;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * Implementação nativa completa do plugin Pagseguro encapsulada no AAR.
 * Recebe as chamadas do método channel e delega para {@link ChannelRequestHandler}, que aplica o
 * gate de licença antes de chamar {@link PagSeguroPayment}.
 *
 * Toda operação de negócio passa pelo gate de licença: a licença é validada (cache válido ou
 * servidor oficial) numa thread de background e, só se estiver válida e ativa, a operação é
 * executada na main thread. Caso contrário o Flutter recebe PlatformException LICENCE_ERROR e o
 * SDK da PagSeguro não é chamado.
 */
public final class PagseguroPaymentTechNativePlugin implements FlutterPlugin, MethodCallHandler {
  private static final String CHANNEL_NAME = "pagseguro_payment_tech";

  private MethodChannel channel;
  private PagSeguroPayment pagseguroPayment;
  // Thread única: preserva a ordem das validações e tira a rede da main thread.
  private AndroidAsyncExecutor licenceExecutor;
  private ChannelRequestHandler requestHandler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    pagseguroPayment = new PagSeguroPayment(binding.getApplicationContext(), channel);
    licenceExecutor = new AndroidAsyncExecutor();
    requestHandler = new ChannelRequestHandler(pagseguroPayment, licenceExecutor);
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    ChannelRequestHandler handler = requestHandler;
    if (handler == null) {
      result.error(LicenceConfig.LICENCE_ERROR_CODE, "Plugin não inicializado.", null);
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
    if (licenceExecutor != null) {
      licenceExecutor.shutdown();
      licenceExecutor = null;
    }
    if (pagseguroPayment != null) {
      pagseguroPayment.dispose();
      pagseguroPayment = null;
    }
  }
}
