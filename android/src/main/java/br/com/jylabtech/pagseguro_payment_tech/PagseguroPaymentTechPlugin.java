package br.com.jylabtech.pagseguro_payment_tech;
import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagAppIdentification;
import br.com.jylabtech.pagseguro_payment_tech.core.PagSeguroPayment;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsFragment;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsPresenter;
import br.com.jylabtech.pagseguro_payment_tech.payments.PaymentsUseCase;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodChannel.Result;
// import io.flutter.plugin.common.PluginRegistry.Registrar;

public class PagseguroPaymentTechPlugin
  implements FlutterPlugin, MethodCallHandler {
  private static final String CHANNEL_NAME = "pagseguro_payment_tech";
  private MethodChannel channel;
  private Context context;
  private PagSeguroPayment pagseguroPayment;
  
  public PagseguroPaymentTechPlugin() {}

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
    //Get context to application
    context = binding.getApplicationContext();
    channel.setMethodCallHandler(this);
    //Create instance to PagSeguroPayment class
    pagseguroPayment = new PagSeguroPayment(context, channel);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    //Function responsible for listening to methods called by flutter
    if (call.method.startsWith("payment") || call.method.equals("startPayment")) {
      //Call payment method
      pagseguroPayment.initPayment(call, result);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    //Dispose plugin
    channel.setMethodCallHandler(null);
    channel = null;
    pagseguroPayment.dispose(); //Dispose PagSeguroPayment
    pagseguroPayment = null;
  }
}