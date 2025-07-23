  <h1 align="center">Pagseguro Payment Tech</h1>

<div align="center" id="top"> 
  <img src="assets/logo_pagseguro.png" alt="PagSeguro" height=80 />
</div>

<p align="center">
  <a href="#dart-sobre">Sobre</a> &#xa0; | &#xa0; 
  <a href="#rocket-tecnologias">Tecnologias</a> &#xa0; | &#xa0;
  <a href="#checkered_flag-configuração">Configuração</a> &#xa0; | &#xa0;
  <a href="#memo-autores">Autores</a> &#xa0; | &#xa0;
</p>

<br>

### Plugin não oficial!

## :dart: Sobre

Projeto destinado a facilitar a integração com o SDK da PagSeguro Smart no Flutter.
Funciona somente com máquinas smarts

## ** Licenças **

Para adquirir uma licença, se cadastre no site <a href="https://licenca.jylabtech.com.br" target="_blank">Licença JY Labtech</a>, e adquira sua licença informando os dados do terminal.

\*\* Importante informar que a licença é válida por terminal.

Após adquirir sua licença, uma chave `licenceKey` será liberada no cadastro do terminal. Inicialize o plugin passando como parâmetro essa chave.

```dart
await PagseguroPaymentTech.initPayment({
    required PaymentHandler handler,
    required String licenceKey,
  })

await PagseguroPaymentTech.initNfc({
    required NfcHandler handler,
    required String licenceKey,
  })

```

Essa função é responsável por validar a licença de seu plugin e liberar para uso.

## :rocket: Tecnologias

As seguintes ferramentas foram usadas na construção do projeto:

- [Flutter](https://flutter.dev/)
- [PlugPagServiceWrapper]()

## :checkered_flag: Configuração

### # Pubspec.yaml

Para usar este plugin, adicione `plugpag_flutter` como [dependência](https://flutter.io/using-packages/) ao seu arquivo `pubspec.yaml`.

```yaml
dependencies:
  pagseguro_payment_tech: any
```

This will get you the latest version.

### # Android Manifest

Para integrar a biblioteca a biblioteca PlugPagService em aplicativos para Android é
necessário adicionar a seguinte permissão ao AndroidManifest.xml.

```xml
<uses-permission android:name="br.com.uol.pagseguro.permission.MANAGE_PAYMENTS"/>
```

### # Intent-filter

Para que seu aplicativo possa ser escolhido como aplicativo padrão de pagamento e receber
Intents de inserção de cartão, é necessário adicionar o seguinte código em seu
AndroidManifest.xml dentro da sua Activity principal.

```xml
<intent-filter>
 <action android:name="br.com.uol.pagseguro.PAYMENT"/>
 <category android:name="android.intent.category.DEFAULT"/>
</intent-filter>
```

### # Build.gradle

#### => Wrapper v1.30.51

Em seu build.gradle a nivel do app, a propriedade `minSdkVersion` precisa ser level 23. Recurso este exigido pela versão do plugpagservice da PagSeguro.

```xml
...
defaultConfig {
        applicationId "com.example.pagseguro_example"
        minSdkVersion 23
        targetSdkVersion flutter.targetSdkVersion
        versionCode flutterVersionCode.toInteger()
        versionName flutterVersionName
    }
...
```

### # Implementação

Para começar é necessário criar uma classe que implemente ´PaymentHandler´, sendo que essa é a responsável por monitorar e retornar os dados da pagseguro.

### Criando classe PaymentController

```dart
class PaymentController extends PaymentHandler {
  int saleValue = 0;
  bool enable = false;
  bool clickPayment = false;
  bool enableRefund = false;
  String? transactionCode;
  String? transactionId;
  String? response;

  void setSaleValue(double value) {
    if (value > 0.0) {
      saleValue = (value * 100).toInt();
      clickPayment = false;
      enable = true;
    } else {
      clickPayment = false;
      enable = false;
    }
  }

  @override
  void disposeDialog() {
    BotToast.cleanAll();
  }

  @override
  void onAbortedSuccessfully() {
    BotToast.showText(text: "Operação cancelada");
  }

  @override
  void onActivationDialog() {}

  @override
  void onAuthProgress(String message) {
    BotToast.showLoading();
  }

  @override
  void onError(String message) {
    BotToast.showText(text: message);
  }

  @override
  void onMessage(String message) {
    BotToast.showText(text: message);
  }

  @override
  void onFinishedResponse(String message) {
    BotToast.showText(text: message);
  }

  @override
  void onTransactionSuccess() {
    BotToast.showText(text: "Transacao com successo!");
  }

  @override
  void writeToFile({
    String? transactionCode,
    String? transactionId,
    String? response,
  }) {}

  @override
  void onLoading(bool show) {
    if (show) {
      BotToast.showLoading();
      return;
    }
    BotToast.closeAllLoading();
  }

  @override
  void onTransactionInfo({
    String? transactionCode,
    String? transactionId,
    String? response,
  }) {
    this.transactionCode = transactionCode;
    this.transactionId = transactionId;
    this.response = response;
    BotToast.showText(
        text:
            "{transactionCode: $transactionCode \n transactionId: $transactionId}");
    enableRefund = true;
  }
}

```

#### Métodos da `PaymentHandler`

##### onAbortedSuccessfully

Acionado quando uma transação de abort é concluída com sucesso.

##### onAuthProgress

Acionado quando uma transação está em progresso.
Retorno do status do Pinpad também é mapeado aqui.

##### onError

Acionado quando uma transação retorna um estado de ´Erro´, devolvendo como parâmetro uma ´String´ com a mensagem.

##### onMessage

Método responsável por devolver para o usuário uma mensagem retornada da PagSeguro.

##### onFinishedResponse

Método responsável por devolver uma response da transação.

##### onTransactionSuccess

Método acionado quando a transação foi concluída com sucesso.

##### onTransactionInfo

Método resposável por devolver uma response completa da transação, sendo possível mapear vários campos retornados.

#### Iniciar transação

Para iniciar a transação é necessário primeiro chamar a função de ativação do PinPad, passando como parâmetro o código de ativação daquele POS (código este informado na sua conta PagBank).

`PagseguroPaymentTech.instance().payment.activePinpad('12345');`

Logo após ativação, o SDK da PagSeguro fornece algumas opções de transação como:

- Crédito = `PagseguroPaymentTech.instance().payment.creditPayment(12.50)`

- Crédito Parcelado = `PagseguroPaymentTech.instance().payment.creditPaymentParc(controller.saleValue, 2)`

- Débito = `PagseguroPaymentTech.instance().payment.debitPayment(12.50)`

- PIX = `PagseguroPaymentTech.instance().payment.pixPayment.(12.50)`

- Voucher (alimentação) = `PagseguroPaymentTech.instance().payment.voucherPayment(12.50)`

- startPayment(Permite chamar diretamente o método startPayment da biblioteca PlugPag) = `PagseguroPaymentTech.instance().payment.startPayment( int type, int amount, int installmentType, int installments, String userReference, bool printReceipt, bool partialPay, bool isCarne)`

- Estorno = `PagseguroPaymentTech.instance().payment.refund(transactionCode: controller.transactionCode, transactionId: controller.transactionId)`

- Abortar transação = `PagseguroPaymentTech.instance().payment.abortTransaction()`

- Última transação = `PagseguroPaymentTech.instance().payment.lastTransaction()`

- Obter status de ativação do Pinpad = `PagseguroPaymentTech.instance().payment.isAuthenticated()`

\*\*Obs: A via do estabelecimento do estabelecimento pode ser impressa ou não baseada no parâmetro printReceipt. Após a finalização do pagamento, um popUp é exibido perguntando se deseja a via do consumidor.

### Método de impressão

Deve-se verificar se seu projeto tem a permissão de acesso aos arquivos dentro do dispositivo.

#### Imprimir um arquivo

`PagseguroPaymentTech.instance().payment.printerfromFile(path)` => Fornecer o caminho absoluto ao arquivo que deve ser impresso.

#### Imprimir um widget

Basta chamar a função abaixo, passando o widget a ser impresso pela POS.

```dart
PrintRenderWidget.print(
  context,
  pagseguroPaymentInstance: pagseguroPayment,
  child: MyCustomWidget(),
);
```

---

## NfcSmart

Igualmente a classe PagSeguroPayment, essa também é necessário configurar um hanlder e inicializar o plugin antes de qualquer função.

Handler -> Você precisa em uma hanlder que extenda de `NfcHandler`

Inicializar -> `NfcSmart.instance.initNfc(handler);`

Para a classe responsável com a integração por meio do NFC, temos os seguintes métodos:
`NfcSmart.instance.nfc.<método>`

//Responsável por fechar o handler de chamadas
`Future<void> closeMethodCallHandler();`

//Responsável por abortar uma transação em andamento
`Future<bool> abortTransaction();`

//Responsável por obter a última transação
`Future<bool> lastTransaction();`

//Responsável por realizar o estorno de uma transação
`Future<bool> refund({required String transactionCode, required String transactionId});`

//Responsável por ler as informações apartir do NFC
`Future<bool> readNfc(idEvento);`

//Responsável pela escrita do método NFC
`Future<bool> writeNfc(valor, nome, cpf, numeroTag, celular, aberto, idEvento);`

//Responsável por regravar informações
`Future<bool> reWriteNfc(valor, idEvento);`

//Responsável por fazer o estorno de uma transação utilizando NFC
`Future<bool> refundNfc(valor, idEvento);`

//Responsável por formatar os dados do cartão
`Future<bool> formatNfc();`

//Responsável por chamar função de débito utilizando NFC
`Future<bool> debitNfc(idEvento, valor);`

## :memo: Autores

<table>
  <tr>
    <td align="center"><a href="https://github.com/JY-Labtech"><img src="https://avatars.githubusercontent.com/u/185066406?s=200&v=4" width="100px;" alt=""/><br /><sub><b>JY Labtech</b></sub></a><br /> <a href="https://github.com/JY-Labtech" title="Autor"></a></td>
  </tr>

</table>

&#xa0;

<a href="#top">Voltar para o topo</a>
