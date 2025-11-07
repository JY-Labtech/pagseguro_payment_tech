package br.com.jylabtech.pagseguro_payment_tech;

import android.content.Context;

import androidx.annotation.NonNull;

import br.com.jylabtech.pagseguro_payment_tech.core.PagSeguroPayment;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * Implementação nativa completa do plugin Pagseguro encapsulada no AAR.
 * Recebe as chamadas do método channel e delega para {@link PagSeguroPayment},
 * garantindo que a licença seja configurada via `initPayment`.
 */
public final class PagseguroPaymentTechNativePlugin implements FlutterPlugin, MethodCallHandler {
  private static final String CHANNEL_NAME = "pagseguro_payment_tech";
  private static final String INIT_PAYMENT_METHOD = "initPayment";

  private MethodChannel channel;
  private Context applicationContext;
  private PagSeguroPayment pagseguroPayment;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    applicationContext = binding.getApplicationContext();
    pagseguroPayment = new PagSeguroPayment(applicationContext, channel);
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      if (INIT_PAYMENT_METHOD.equals(call.method)) {
        String licenceKey = call.argument("licenceKey");
        String licenceInternalKey = call.argument("licenceInternalKey");
        pagseguroPayment.updateLicenceKeys(licenceKey, licenceInternalKey);
        boolean licenceValid = pagseguroPayment.ensureLicenceValid();
        result.success(licenceValid);
        return;
      }

      if (call.method.startsWith("payment") || "startPayment".equals(call.method)) {
        pagseguroPayment.handleMethodCall(call, result);
      } else {
        result.notImplemented();
      }
    } catch (Exception exception) {
      result.error("ERROR", exception.getMessage(), null);
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) {
      channel.setMethodCallHandler(null);
      channel = null;
    }
    if (pagseguroPayment != null) {
      pagseguroPayment.dispose();
      pagseguroPayment = null;
    }
    applicationContext = null;
  }
}