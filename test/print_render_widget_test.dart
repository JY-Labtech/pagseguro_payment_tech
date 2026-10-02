import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';
import 'package:path_provider_platform_interface/path_provider_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'helpers/fake_native.dart';

class _FakePathProvider extends PathProviderPlatform with MockPlatformInterfaceMixin {
  _FakePathProvider(this.path);

  final String? path;

  @override
  Future<String?> getExternalStoragePath() async => path;
}

/// PrintRenderWidget: renderiza o widget, salva o PNG e chama printer (paymentPrinterBasic).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeNative native;
  late Directory tempDir;

  setUp(() {
    PagseguroPaymentTech.resetForTesting();
    native = FakeNative();
    PagseguroPaymentTech.I.initPayment(handler: RecordingPaymentHandler(), licenceKey: testLicenceKey);
    tempDir = Directory.systemTemp.createTempSync('print_render_test');
  });

  tearDown(() {
    native.dispose();
    PagseguroPaymentTech.resetForTesting();
    if (tempDir.existsSync()) tempDir.deleteSync(recursive: true);
  });

  Future<void> openAndPrint(WidgetTester tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        builder: (context) => TextButton(
          onPressed: () => PrintRenderWidget.print(
            context,
            pagseguroPaymentInstance: PagseguroPaymentTech.I,
            child: const SizedBox(width: 100, height: 50, child: Text('CUPOM')),
          ),
          child: const Text('imprimir'),
        ),
      ),
    ));
    await tester.tap(find.text('imprimir'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 16));
    expect(find.text('CUPOM', skipOffstage: false), findsOneWidget);
    // Captura da imagem e escrita do arquivo usam IO real: alterna tempo real e frames.
    for (var i = 0; i < 100 && !native.methods.contains('paymentPrinterBasic') && find.text('CUPOM', skipOffstage: false).evaluate().isNotEmpty; i++) {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 20)));
      await tester.pump(const Duration(milliseconds: 50));
    }
    // Espera de 2 s depois de imprimir e fechamento da tela.
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
  }

  testWidgets('gera o PNG, imprime pelo path e fecha a tela', (tester) async {
    PathProviderPlatform.instance = _FakePathProvider(tempDir.path);
    await openAndPrint(tester);

    final path = native.last('paymentPrinterBasic').arguments['path'] as String;
    expect(path, '${tempDir.path}/temp_directory/print_file.png');
    expect(File(path).existsSync(), isTrue);
    expect(File(path).lengthSync(), greaterThan(0));
    expect(find.text('CUPOM'), findsNothing);
  });

  testWidgets('falha ao gerar o arquivo também fecha a tela (antes: ficava aberta)', (tester) async {
    PathProviderPlatform.instance = _FakePathProvider('/caminho/que/nao/existe/\u0000');
    await openAndPrint(tester);
    expect(find.text('CUPOM'), findsNothing);
  });

  test('construtor guarda instância e filho', () {
    const child = Text('x');
    final widget = PrintRenderWidget(pagseguroPayment: PagseguroPaymentTech.I, child: child);
    expect(widget.child, same(child));
    expect(widget.pagseguroPayment, same(PagseguroPaymentTech.I));
  });
}
