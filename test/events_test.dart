import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';
import 'package:pagseguro_payment_tech/src/payments/handler/event_dispatcher.dart';

import 'helpers/fake_native.dart';

/// Eventos nativo -> Dart (nomes enviados por PaymentsFragment/NFCFragment no Java).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeNative native;
  late RecordingPaymentHandler paymentHandler;

  setUp(() async {
    PagseguroPaymentTech.resetForTesting();
    native = FakeNative();
    paymentHandler = RecordingPaymentHandler();
    await initPayment(native, paymentHandler);
  });

  tearDown(() {
    native.dispose();
    PagseguroPaymentTech.resetForTesting();
  });

  const transaction = {'transactionCode': 'TC', 'transactionId': 'TI', 'response': '{"result":0}'};

  group('PaymentHandler', () {
    test('cada evento de pagamento chega ao callback certo com o argumento certo', () async {
      await native.sendEvent('onTransactionSuccess', true);
      await native.sendEvent('onError', 'erro');
      await native.sendEvent('onMessage', 'APROXIME O CARTAO');
      await native.sendEvent('onFinishedResponse', '{"method":"transaction"}');
      await native.sendEvent('onLoading', true);
      await native.sendEvent('onLoading', false);
      await native.sendEvent('writeToFile', transaction);
      await native.sendEvent('onAbortedSuccessfully', true);
      await native.sendEvent('disposeDialog', true);
      await native.sendEvent('activeDialog', true);
      await native.sendEvent('onAuthProgress', 'Terminal ativado');
      await native.sendEvent('onTransactionInfo', transaction);

      expect(paymentHandler.calls, [
        'onTransactionSuccess',
        'onError:erro',
        'onMessage:APROXIME O CARTAO',
        'onFinishedResponse:{"method":"transaction"}',
        'onLoading:true',
        'onLoading:false',
        'writeToFile:TC|TI|{"result":0}',
        'onAbortedSuccessfully',
        'disposeDialog',
        'onActivationDialog',
        'onAuthProgress:Terminal ativado',
        'onTransactionInfo:TC|TI|{"result":0}',
      ]);
    });

    test('onTransactionSuccess enviado pelo nativo agora é reconhecido (antes: "NOT IMPLEMENTED")', () async {
      final reply = await native.sendEvent('onTransactionSuccess', true);
      expect(paymentHandler.calls, ['onTransactionSuccess']);
      expect(const StandardMethodCodec().decodeEnvelope(reply!), isTrue);
      // Grafia antiga continua aceita.
      await native.sendEvent('onTransactionSucess', true);
      expect(paymentHandler.calls, ['onTransactionSuccess', 'onTransactionSuccess']);
    });

    test('argumentos nulos do nativo não perdem o evento', () async {
      await native.sendEvent('onError', null);
      await native.sendEvent('onMessage', null);
      await native.sendEvent('onFinishedResponse', null);
      await native.sendEvent('onAuthProgress', null);
      await native.sendEvent('onLoading', null);
      await native.sendEvent('writeToFile', {'transactionCode': null, 'transactionId': 'TI', 'response': null});
      await native.sendEvent('onTransactionInfo', null);
      expect(paymentHandler.calls, [
        'onError:',
        'onMessage:',
        'onFinishedResponse:',
        'onAuthProgress:',
        'onLoading:false',
        'writeToFile:|TI|',
        'onTransactionInfo:||',
      ]);
    });

    test('evento desconhecido responde notImplemented sem chamar o handler', () async {
      final reply = await native.sendEvent('onReadCard', true);
      expect(reply, isNull);
      expect(paymentHandler.calls, isEmpty);
    });

    test('eventos NFC sem NfcHandler são ignorados', () async {
      await native.sendEvent('showSuccess', ['1']);
      await native.sendEvent('showErrorRead', 'x');
      expect(paymentHandler.calls, isEmpty);
    });
  });

  group('NfcHandler', () {
    late RecordingNfcHandler nfcHandler;

    setUp(() {
      nfcHandler = RecordingNfcHandler();
      PagseguroPaymentTech.I.initNfc(handler: nfcHandler);
    });

    test('cada evento NFC chega ao callback certo', () async {
      await native.sendEvent('showSuccess', ['1000', 'Ana', '123', 'TAG', '119', '1', '500']);
      await native.sendEvent('showSuccessWrite', 1);
      await native.sendEvent('showSuccessReWrite', '1500');
      await native.sendEvent('showSuccessFormat', 1);
      await native.sendEvent('showSuccessDebitNfc', '800');
      await native.sendEvent('showSuccessRefundNfc', '1300');
      await native.sendEvent('showErrorRead', 'r');
      await native.sendEvent('showErrorWrite', 'w');
      await native.sendEvent('showErrorReWrite', 'rw');
      await native.sendEvent('showErrorFormat', 'f');
      await native.sendEvent('showErrorDebitNfc', 'd');
      await native.sendEvent('showErrorRefundNfc', 'rf');

      expect(nfcHandler.calls, [
        'showSuccess:1000,Ana,123,TAG,119,1,500',
        'showSuccessWrite:1',
        'showSuccessReWrite:1500',
        'showSuccessFormat:1',
        'showSuccessDebitNfc:800',
        'showSuccessRefundNfc:1300',
        'showErrorRead:r',
        'showErrorWrite:w',
        'showErrorReWrite:rw',
        'showErrorFormat:f',
        'showErrorDebitNfc:d',
        'showErrorRefundNfc:rf',
      ]);
      expect(paymentHandler.calls, isEmpty);
    });

    test('eventos comuns vão para o NFC (último registrado), como antes', () async {
      await native.sendEvent('onTransactionSuccess', true);
      await native.sendEvent('onError', 'e');
      await native.sendEvent('onMessage', 'm');
      await native.sendEvent('onLoading', true);
      await native.sendEvent('writeToFile', transaction);
      await native.sendEvent('onAbortedSuccessfully', true);
      await native.sendEvent('disposeDialog', true);
      await native.sendEvent('activeDialog', true);
      await native.sendEvent('onAuthProgress', 'a');
      await native.sendEvent('onTransactionInfo', transaction);
      expect(nfcHandler.calls, [
        'onTransactionSuccess',
        'onError:e',
        'onMessage:m',
        'onLoading:true',
        'writeToFile:TC|TI',
        'onAbortedSuccessfully',
        'disposeDialog',
        'onActivationDialog',
        'onAuthProgress:a',
        'onTransactionInfo:TC|TI',
      ]);
      expect(paymentHandler.calls, isEmpty);
    });

    test('onFinishedResponse continua chegando ao PaymentHandler com o NFC ativo (antes: perdido)', () async {
      await native.sendEvent('onFinishedResponse', '{"method":"userData"}');
      expect(paymentHandler.calls, ['onFinishedResponse:{"method":"userData"}']);
      expect(nfcHandler.calls, isEmpty);
    });

    test('eventos NFC continuam chegando depois de Payment.setHandler (antes: perdidos)', () async {
      final other = RecordingPaymentHandler();
      PagseguroPaymentTech.I.payment.setHandler(other);
      await native.sendEvent('showSuccessWrite', 1);
      await native.sendEvent('onMessage', 'pagamento');
      expect(nfcHandler.calls, ['showSuccessWrite:1']);
      expect(other.calls, ['onMessage:pagamento']);
    });

    test('argumentos NFC com tipos inesperados são convertidos', () async {
      await native.sendEvent('showSuccess', null);
      await native.sendEvent('showSuccessWrite', '2');
      await native.sendEvent('showSuccessFormat', null);
      await native.sendEvent('showSuccessFormat', 3.0);
      await native.sendEvent('showSuccessReWrite', 15);
      await native.sendEvent('showErrorRead', null);
      expect(nfcHandler.calls, [
        'showSuccess:',
        'showSuccessWrite:2',
        'showSuccessFormat:0',
        'showSuccessFormat:3',
        'showSuccessReWrite:15',
        'showErrorRead:',
      ]);
    });

    test('closeMethodCallHandler devolve os eventos ao PaymentHandler (antes: canal sem handler)', () async {
      PagseguroPaymentTech.I.nfc.closeMethodCallHandler();
      await native.sendEvent('onMessage', 'm');
      await native.sendEvent('showSuccessWrite', 1);
      expect(paymentHandler.calls, ['onMessage:m']);
      expect(nfcHandler.calls, isEmpty);
    });
  });

  group('PagseguroEventDispatcher', () {
    test('parseEvent reconhece todos os eventos e as duas grafias de sucesso', () {
      for (final value in PaymentTypeHandler.values) {
        expect(PagseguroEventDispatcher.parseEvent(value.method), value);
      }
      expect(PagseguroEventDispatcher.parseEvent('onTransactionSucess'), PaymentTypeHandler.ON_TRANSACTION_SUCCESS);
      expect(PagseguroEventDispatcher.parseEvent('nada'), isNull);
    });

    test('só NFC registrado: eventos comuns vão para ele; onFinishedResponse é ignorado', () async {
      PagseguroEventDispatcher.reset();
      final nfc = RecordingNfcHandler();
      PagseguroEventDispatcher.attachNfc(channel, nfc);
      await native.sendEvent('onMessage', 'm');
      await native.sendEvent('onFinishedResponse', '{}');
      expect(nfc.calls, ['onMessage:m']);
    });

    test('sem handlers o canal fica sem tratamento depois do detach', () async {
      PagseguroEventDispatcher.reset();
      final nfc = RecordingNfcHandler();
      PagseguroEventDispatcher.attachNfc(channel, nfc);
      PagseguroEventDispatcher.detachNfc();
      expect(await native.sendEvent('onMessage', 'm'), isNull);
      expect(nfc.calls, isEmpty);
    });

    test('handle sem nenhum handler não lança para eventos conhecidos', () async {
      PagseguroEventDispatcher.reset();
      expect(await PagseguroEventDispatcher.handle(const MethodCall('onMessage', 'x')), isTrue);
      expect(() => PagseguroEventDispatcher.handle(const MethodCall('x')), throwsA(isA<MissingPluginException>()));
    });
  });
}
