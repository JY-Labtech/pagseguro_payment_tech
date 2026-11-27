import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

import 'helpers/fake_native.dart';

/// Tela pronta PagseguroPage + PagseguroController com o nativo simulado.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeNative native;

  setUp(() {
    PagseguroPaymentTech.resetForTesting();
    native = FakeNative();
    PagseguroPaymentTech.I.initPayment(handler: RecordingPaymentHandler());
  });

  tearDown(() {
    native.dispose();
    PagseguroPaymentTech.resetForTesting();
  });

  PagseguroPageParams params({PagseguroPaymentType type = PagseguroPaymentType.debit, double amount = 25.90, int? parcels}) => PagseguroPageParams(
        amount: amount,
        parcels: parcels,
        type: type,
        options: PagseguroOptions(activationCode: '403938', isPrinterEstablishment: true),
      );

  /// Abre a PagseguroPage e devolve um holder com o resultado do pop.
  Future<List<PagseguroResponseModel?>> openPage(WidgetTester tester, PagseguroPageParams params) async {
    final results = <PagseguroResponseModel?>[];
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        builder: (context) => TextButton(
          onPressed: () async {
            final result = await Navigator.of(context).push<PagseguroResponseModel?>(
              MaterialPageRoute(builder: (_) => PagseguroPage(params: params)),
            );
            results.add(result);
          },
          child: const Text('pagar'),
        ),
      ),
    ));
    await tester.tap(find.text('pagar'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('Iniciando fluxo de pagamento.\nAguarde...'), findsOneWidget);
    // init: 1 s de espera + setHandler (initPayment) + 500 ms + isAuthenticated.
    await tester.pump(const Duration(seconds: 1));
    await tester.pump(const Duration(milliseconds: 600));
    return results;
  }

  Future<void> event(WidgetTester tester, String method, Object? args) async {
    await native.sendEvent(method, args);
    await tester.pump(const Duration(milliseconds: 600));
  }

  testWidgets('terminal ativado: paga com o valor em centavos e devolve a transação', (tester) async {
    final results = await openPage(tester, params());
    expect(native.methods, containsAllInOrder(['initPayment', 'paymentIsAuthenticated']));

    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal ativado', 'result': 0}));
    final debit = native.last('paymentDebit');
    expect(debit.arguments['value'], 2590);
    expect(debit.arguments['printReceipt'], isTrue);

    await event(tester, 'onMessage', 'APROXIME O CARTAO');
    expect(find.text('APROXIME O CARTAO'), findsOneWidget);
    expect(find.text('Cancelar Pagamento'), findsOneWidget);

    await event(tester, 'onMessage', 'AUTORIZADA');
    expect(find.text('Cancelar Pagamento'), findsNothing);

    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'transaction', 'result': 0, 'transactionCode': 'TC', 'transactionId': 'TI', 'hostNsu': '1', 'nsu': '2', 'paymentType': 2}));
    await tester.pumpAndSettle();
    expect(results, hasLength(1));
    expect(results.single!.transactionCode, 'TC');
    expect(results.single!.transactionID, 'TI');
    expect(results.single!.isTransactionValid, isTrue);
  });

  testWidgets('terminal não ativado: ativa com o código e depois paga', (tester) async {
    await openPage(tester, params(type: PagseguroPaymentType.creditParc, amount: 100, parcels: 3));
    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal não ativado'}));
    expect(native.last('paymentActivePinpad').arguments, {'code': '403938'});
    expect(native.methods, isNot(contains('paymentCreditParc')));

    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal ativado'}));
    final call = native.last('paymentCreditParc');
    expect(call.arguments['value'], 10000);
    expect(call.arguments['parc'], 3);
    expect(call.arguments['type'], 3);
    await event(tester, 'onError', 'fim');
    await tester.pumpAndSettle();
  });

  testWidgets('cada tipo chama o método de pagamento correspondente', (tester) async {
    const methods = {
      PagseguroPaymentType.credit: 'paymentCredit',
      PagseguroPaymentType.voucher: 'paymentVoucher',
      PagseguroPaymentType.pix: 'paymentPix',
    };
    for (final entry in methods.entries) {
      PagseguroPaymentTech.resetForTesting();
      PagseguroPaymentTech.I.initPayment(handler: RecordingPaymentHandler());
      native.calls.clear();
      await openPage(tester, params(type: entry.key, amount: 1));
      await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal ativado'}));
      expect(native.last(entry.value).arguments['value'], 100, reason: entry.value);
      await event(tester, 'onError', 'fim');
      await tester.pumpAndSettle();
    }
  });

  testWidgets('onError fecha a tela com resposta de erro', (tester) async {
    final results = await openPage(tester, params());
    await event(tester, 'onError', 'Transação negada');
    await tester.pumpAndSettle();
    expect(results.single!.resultCode, 9999999);
    expect(results.single!.errorMessage, 'Transação negada');
  });

  testWidgets('erro do nativo ao iniciar o pagamento fecha a tela (antes: tela presa)', (tester) async {
    native.responses['paymentDebit'] = (_) => throw genericError;
    final results = await openPage(tester, params());
    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal ativado'}));
    await tester.pumpAndSettle();
    expect(results.single!.resultCode, 9999999);
    expect(results.single!.errorMessage, 'terminal ocupado');
  });

  testWidgets('erro do nativo seguido de onError fecha a tela uma vez só', (tester) async {
    native.responses['paymentDebit'] = (_) => throw invalidArgumentError;
    final results = await openPage(tester, params());
    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'activate', 'message': 'Terminal ativado'}));
    await event(tester, 'onError', 'argumento inválido');
    await tester.pumpAndSettle();
    expect(results.single!.errorMessage, 'argumento inválido');
    expect(find.byType(PagseguroPage), findsNothing);
  });

  testWidgets('cancelar aborta uma única vez', (tester) async {
    await openPage(tester, params());
    await event(tester, 'onMessage', 'APROXIME O CARTAO');
    await tester.tap(find.text('Cancelar Pagamento'));
    await tester.pump();
    await tester.tap(find.text('Cancelar Pagamento'));
    await tester.pump();
    expect(native.methods.where((m) => m == 'paymentAbort'), hasLength(1));
    await event(tester, 'onError', 'OPERACAO CANCELADA');
    await tester.pumpAndSettle();
  });

  testWidgets('onFinishedResponse inválido ou de outro método é ignorado', (tester) async {
    final results = await openPage(tester, params());
    await event(tester, 'onFinishedResponse', 'não é json');
    await event(tester, 'onFinishedResponse', jsonEncode({'method': 'userData', 'result': 0}));
    await event(tester, 'onFinishedResponse', jsonEncode([1, 2]));
    await event(tester, 'onAuthProgress', 'x');
    await event(tester, 'onTransactionInfo', {'response': '{}'});
    expect(results, isEmpty);
    expect(find.byType(PagseguroPage), findsOneWidget);
    await event(tester, 'onError', 'fim');
    await tester.pumpAndSettle();
  });

  group('PagseguroController.onMessageMonitor', () {
    test('mostra o cancelar ao aproximar e esconde nos estados finais', () {
      final controller = PagseguroController();
      controller.onMessageMonitor('APROXIME, INSIRA OU PASSE O CARTAO');
      expect(controller.isVisibilityCancelButton, isTrue);
      for (final message in ['RETIRE O CARTAO', 'OPERACAO CANCELADA', 'NÃO AUTORIZADA', 'AUTORIZADO', 'Transação autorizada']) {
        controller.setVisibilityCancelButton(true);
        controller.onMessageMonitor(message);
        expect(controller.isVisibilityCancelButton, isFalse, reason: message);
      }
      controller.onMessageMonitor('PROCESSANDO');
      expect(controller.isVisibilityCancelButton, isFalse);
      controller.printerFromFile('/a.png');
    });

    test('init sem handler não faz nada', () async {
      final controller = PagseguroController();
      controller.init(handler: null, params: params(), options: params().options);
      await Future<void>.delayed(Duration.zero);
      expect(native.calls.where((c) => c.method != 'initPayment'), isEmpty);
    });
  });
}
