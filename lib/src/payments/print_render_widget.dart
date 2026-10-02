import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

import 'package:path_provider/path_provider.dart';

import 'package:pagseguro_payment_tech/src/payments/pagseguro_payment_tech.dart';

/// Widget auxiliar para renderização e impressão de um widget específico.
///
/// O [PrintRenderWidget] permite exibir um widget na tela e gerar uma imagem
/// renderizada dele para impressão usando o `PagseguroPaymentTech`.
///
/// ### Parâmetros:
/// - `pagseguroPayment`: Instância de `PagseguroPaymentTech` para gerenciar a impressão.
/// - `child`: O widget a ser renderizado e impresso.
///
/// ### Métodos
/// - [print]: Método estático que navega para a página de impressão, renderizando o widget fornecido.
///
class PrintRenderWidget {
  final PagseguroPaymentTech pagseguroPayment;
  final Widget child;

  PrintRenderWidget({required this.pagseguroPayment, required this.child});

  /// Método estático para iniciar o processo de impressão.
  ///
  /// Este método coloca a `_PrintRenderPage` stack do `Navigator` atual,
  /// renderizando o widget fornecido (`child`) e usando a instância
  /// de `PagseguroPaymentTech` fornecida para impressão.
  ///
  /// ### Parâmetros:
  /// - `context`: Contexto do `BuildContext`.
  /// - `pagseguroPaymentInstance`: Instância de `PagseguroPaymentTech` para impressão.
  /// - `child`: Widget que será renderizado e impresso.
  ///
  /// ### Exemplo de uso:
  /// ```dart
  /// PrintRenderWidget.print(
  ///   context,
  ///   pagseguroPaymentInstance: pagseguroPayment,
  ///   child: MyWidget(),
  /// );
  /// ```
  static void print(
    BuildContext context, {
    required PagseguroPaymentTech pagseguroPaymentInstance,
    required Widget child,
  }) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (context) => _PrintRenderPage(
          pagseguroPayment: pagseguroPaymentInstance,
          child: child,
        ),
      ),
    );
  }
}

/// Página interna responsável pela renderização e impressão do widget.
///
/// A [_PrintRenderPage] usa um [GlobalKey] para capturar e renderizar
/// a imagem do widget especificado para a impressão. A captura ocorre
/// automaticamente após o carregamento do widget na tela.
class _PrintRenderPage extends StatefulWidget {
  final PagseguroPaymentTech pagseguroPayment;
  final Widget child;

  const _PrintRenderPage({
    Key? key,
    required this.child,
    required this.pagseguroPayment,
  }) : super(key: key);

  @override
  State<_PrintRenderPage> createState() => _PrinterRenderPageState();
}

/// Tempo de espera depois de enviar a imagem à impressora, antes de fechar a tela.
const Duration _printFinishDelay = Duration(seconds: 2);

class _PrinterRenderPageState extends State<_PrintRenderPage> {
  final GlobalKey _globalKey = GlobalKey();

  @override
  void initState() {
    // Após a renderização inicial, chama a função de impressão.
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await Future.delayed(const Duration(milliseconds: 100));

      await _printImage();
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // Necessário o fundo branco para garantir uma boa qualidade de impressão.
    const backgroundColor = Colors.white;

    return Material(
      child: SingleChildScrollView(
        child: RepaintBoundary(
          key: _globalKey,
          child: Container(
            color: backgroundColor,
            child: widget.child,
          ),
        ),
      ),
    );
  }

  /// Captura uma imagem da área de renderização associada a um [GlobalKey] e a
  /// retorna como um array de bytes (no formato PNG).
  ///
  /// Esta função utiliza o [GlobalKey] fornecido para localizar o contexto atual e,
  /// em seguida, captura uma imagem da área renderizada (representada por um
  /// [RenderRepaintBoundary]). A imagem capturada é convertida para o formato PNG
  /// e retornada como uma lista de bytes ([Uint8List]).
  ///
  /// Retorna `null` em caso de falha na captura, como nos seguintes casos:
  ///   - O [BuildContext] atual não existe (por exemplo, se o widget foi desmontado).
  ///   - O [RenderRepaintBoundary] associado ao [BuildContext] não está disponível.
  ///   - A conversão da imagem para [ByteData] falha.
  ///
  /// ### Retorno
  /// - [Uint8List?]: Um array de bytes representando a imagem no formato PNG,
  /// ou `null` se não for possível capturar a imagem.
  ///
  /// ### Exemplo de uso
  /// ```dart
  /// Uint8List? imageBytes = await _captureImageBytes();
  /// if (imageBytes != null) {
  ///   // Utilize os bytes da imagem, por exemplo, para salvar ou exibir a imagem.
  /// }
  /// ```
  ///
  /// ### Notas
  /// - Certifique-se de que o widget associado ao [_globalKey] esteja visível e
  /// renderizado na tela antes de chamar essa função.
  ///
  /// ### Erros
  /// - Retorna `null` caso o [GlobalKey] fornecido não esteja associado a um
  ///   [BuildContext] ou se a captura da imagem falhar.
  Future<Uint8List?> _captureImageBytes() async {
    final BuildContext? currentContext = _globalKey.currentContext;
    if (currentContext == null) {
      return null;
    }

    // Obtém o objeto de renderização associado ao contexto atual.
    final RenderRepaintBoundary? boundary = currentContext.findRenderObject() as RenderRepaintBoundary?;
    if (boundary == null) {
      return null;
    }

    // Em debug, capturar um boundary ainda não pintado (ex.: durante a animação de entrada da
    // rota) dispara a assertion '!debugNeedsPaint'. Aguarda o próximo frame nesse caso.
    if (kDebugMode && boundary.debugNeedsPaint) {
      await WidgetsBinding.instance.endOfFrame;
    }

    // Captura a imagem da área de renderização.
    final ui.Image image = await boundary.toImage();

    // Converte a imagem capturada para o formato PNG.
    final ByteData? byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    if (byteData == null) {
      return null;
    }

    // Retorna a imagem como um array de bytes (Uint8List).
    return byteData.buffer.asUint8List();
  }

  /// Cria um diretório temporário personalizado no armazenamento externo do dispositivo.
  ///
  /// Esta função obtém o caminho do diretório de armazenamento externo do dispositivo e cria um
  /// subdiretório chamado `temp_directory` dentro desse diretório. Se o diretório já existir,
  /// ele não será recriado.
  ///
  /// ### Retorno
  /// - [Future<Directory>]: Um `Future` que completa com o diretório temporário criado
  ///   (ou o existente, caso já esteja presente).
  ///
  /// ### Exemplo de uso
  /// ```dart
  /// Directory tempDir = await _createTemporaryDirectory();
  /// print('Temporary directory created at: ${tempDir.path}');
  /// ```
  ///
  /// ### Erros
  /// - Retorna um diretório com caminho vazio (`''`) se não for possível acessar o diretório de
  ///   armazenamento externo.
  /// - Pode lançar uma exceção se ocorrer um erro durante a criação do diretório, como falta
  ///   de permissões ou problemas de armazenamento.
  Future<Directory> _createTemporaryDirectory() async {
    // Obtém o caminho do diretório de armazenamento externo.
    final String tempPath = (await getExternalStorageDirectory())?.path ?? '';

    // Define o caminho para o novo diretório temporário personalizado.
    final Directory customTempDir = Directory('$tempPath/temp_directory');

    // Verifica se o diretório já existe; se não, cria-o.
    if (!await customTempDir.exists()) {
      await customTempDir.create(recursive: true);
    }

    // Retorna o diretório temporário criado ou existente.
    return customTempDir;
  }

  /// Gera o caminho de um arquivo de imagem temporário para salvar uma captura de tela no formato PNG.
  ///
  /// Esta função captura a imagem da tela como uma lista de bytes chamando a função `_captureImageBytes()`.
  /// Em seguida, ela utiliza um diretório temporário criado pela função `_createTemporaryDirectory()`,
  /// e define um caminho para o arquivo de imagem `print_file.png` dentro deste diretório.
  ///
  /// ### Retorno
  /// - [Future<String?>]: Um `Future` que completa com o caminho completo do arquivo de imagem temporário
  ///   (no formato PNG), ou `null` se a captura da imagem falhar.
  ///
  /// ### Exemplo de uso
  /// ```dart
  /// String? imagePath = await _generateImagePath();
  /// if (imagePath != null) {
  ///   print('Image path generated: $imagePath');
  /// }
  /// ```
  ///
  /// ### Erros
  /// - Retorna `null` caso `_captureImageBytes()` falhe ao capturar a imagem.
  Future<String?> _generateImagePath() async {
    // Captura os bytes da imagem da tela.
    final Uint8List? bytes = await _captureImageBytes();
    if (bytes == null) {
      return null;
    }

    // Cria um diretório temporário e obtém seu caminho.
    final Directory externalStorageDir = await _createTemporaryDirectory();
    final String tempPath = externalStorageDir.path;

    // Define o caminho final do arquivo de imagem PNG.
    final String finalPath = '$tempPath/print_file.png';

    //Cria o arquivo da imagem a ser impressa
    final File file = await File(finalPath).create();
    await file.writeAsBytes(bytes);

    // Retorna o caminho completo do arquivo.
    return finalPath;
  }

  /// Gera e envia uma imagem para impressão, aguardando o processamento e, em seguida, fecha a tela atual.
  ///
  /// Esta função tenta gerar o caminho de uma imagem chamando a função `_generateImagePath()`.
  /// Caso o caminho da imagem seja gerado com sucesso, ela o envia para impressão usando o método
  /// `printerFilePath` da classe de pagamento `pagseguroPayment` do widget pai.
  /// Após o envio para impressão, aguarda por 2 segundos para garantir o processamento,
  /// e em seguida, fecha a tela atual com o `Navigator`.
  ///
  /// ### Erro
  /// - Caso ocorra uma exceção durante o processo de geração de imagem ou impressão,
  ///   uma mensagem de erro é registrada no console.
  ///
  /// ### Exemplo de uso
  /// ```dart
  /// await _printImage();
  /// ```
  ///
  /// ### Notas
  /// - O método `printerFilePath` imprime a imagem localizada no caminho fornecido.
  /// - É importante que `_generateImagePath()` retorne um caminho válido; caso contrário,
  ///   uma string vazia será usada, o que pode resultar em falha na impressão.
  ///
  /// ### Tratamento de Exceção
  /// - Em caso de erro, a função captura e imprime o erro no console, mas não interrompe o fluxo da aplicação.
  Future<void> _printImage() async {
    try {
      // Gera o caminho da imagem para impressão.
      final imagePath = await _generateImagePath();

      // Envia o caminho da imagem para o método de impressão.
      await widget.pagseguroPayment.payment.printer(imagePath ?? '');

      // Aguarda 2 segundos para o processamento e encerra a tela atual.
      await Future.delayed(_printFinishDelay);
    } catch (e) {
      // Captura e imprime a mensagem de erro no console.
      debugPrint('Erro ao gerar a imagem.\n\tErro: $e');
    } finally {
      // Fecha a tela também em caso de erro (antes ela ficava aberta indefinidamente).
      if (mounted) {
        Navigator.of(context).pop();
      }
    }
  }
}
