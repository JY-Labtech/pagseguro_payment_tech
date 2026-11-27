# pagseguro_payment_tech

> Open source (MIT) Flutter plugin for PagSeguro Smart POS terminals, built on the PlugPagServiceWrapper SDK: payments, refunds, printing and NFC.

[![pub package](https://img.shields.io/pub/v/pagseguro_payment_tech.svg)](https://pub.dev/packages/pagseguro_payment_tech)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

<div align="center">
  <img src="https://raw.githubusercontent.com/JY-Labtech/pagseguro_payment_tech/main/assets/logo_pagseguro.png" alt="PagSeguro" height="80" />
</div>

Plugin Flutter para as maquininhas **PagSeguro Smart** (Android), usando o SDK
**PlugPagServiceWrapper 1.33.0**. Faz pagamentos (crédito à vista e parcelado, débito, voucher e
PIX), cancelamento, estorno, consulta da última transação, ativação do terminal, impressão de
arquivos e de widgets Flutter, leitura e gravação de cartões NFC, além de uma tela de pagamento
pronta (`PagseguroPage`).

> Plugin não oficial: não é mantido nem endossado pela PagSeguro/PagBank.

Desde a **4.0.0** o plugin é open source sob a licença MIT: não há chave de licença, cobrança nem
chamada a servidores da JY Labtech.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Maquininhas compatíveis e requisitos](#maquininhas-compatíveis-e-requisitos)
- [Instalação](#instalação)
- [Configuração Android](#configuração-android)
- [Uso rápido](#uso-rápido)
- [Referência da API](#referência-da-api)
- [Exemplos por operação](#exemplos-por-operação)
- [Tratamento de erros](#tratamento-de-erros)
- [App de exemplo](#app-de-exemplo)
- [Testes](#testes)
- [Migração da 3.x para a 4.0.0](#migração-da-3x-para-a-400)
- [Contribuindo](#contribuindo)
- [Licença](#licença)
- [Créditos](#créditos)

## Funcionalidades

Pagamentos (`Payment`):

- `creditPayment`: crédito à vista;
- `creditPaymentParc`: crédito parcelado (lojista ou comprador);
- `debitPayment`: débito;
- `voucherPayment`: voucher (vale-refeição/alimentação);
- `pixPayment`: PIX;
- `startPayment`: fluxo genérico com qualquer `PaymentType` (inclusive QR Code e pré-autorização);
- `abortTransaction`: cancela a operação em andamento;
- `lastTransaction`: consulta a última transação aprovada;
- `refund`: estorno de uma transação;
- `calculateInstallments`: simula as parcelas de uma venda.

Terminal:

- `activePinpad`: ativa o terminal com o código de ativação;
- `isAuthenticated`: verifica se o terminal está ativado;
- `getSerialNumber`: número de série;
- `getUserData`: dados do estabelecimento;
- `beep` e `rebootDevice`.

Impressão:

- `printerfromFile`, `printerFile`, `printer` e `printerFilePath`: imprime um arquivo de imagem;
- `PrintRenderWidget.print`: renderiza um widget Flutter e o imprime;
- `setDialogstyle` e `setPrintDialogMaxTime`: estilo e tempo do dialog de impressão da via do cliente.

NFC (`Nfc`):

- `readNfc`, `writeNfc`, `reWriteNfc`, `debitNfc`, `refundNfc` e `formatNfc` (cartão com saldo
  gravado nos blocos);
- `readNfcUid`: lê só o UID do cartão, sem gravar nada;
- `abortTransaction`, `lastTransaction` e `refund` também disponíveis no `Nfc`.

Interface pronta:

- `PagseguroPage`: tela completa de pagamento que devolve um `PagseguroResponseModel`.

## Maquininhas compatíveis e requisitos

- Maquininhas **PagSeguro Smart** (Android) com o app **PlugPagService** instalado e atualizado
  (ele já vem nas maquininhas; o plugin conversa com ele pelo PlugPagServiceWrapper).
- Só Android. A pasta `ios/` existe apenas para o Flutter reconhecer o plugin: não há
  implementação para iOS.
- Flutter **>= 3.16.0** e Dart **>= 3.2.0**.
- Android **minSdk 23** e **compileSdk 35**.
- Android Gradle Plugin **8.13.x**, Gradle **8.13+**, Kotlin **2.1.21** e Java 17 no app.
- Permissão `br.com.uol.pagseguro.permission.MANAGE_PAYMENTS` (veja abaixo).

## Instalação

```bash
flutter pub add pagseguro_payment_tech
```

Ou no `pubspec.yaml`:

```yaml
dependencies:
  pagseguro_payment_tech: ^4.0.0
```

## Configuração Android

### 1. Repositório Maven do PlugPagServiceWrapper

O SDK da PagSeguro é baixado do repositório público no GitHub (sem credenciais). O plugin já o
adiciona a todos os projetos, mas, se o seu build usar `dependencyResolutionManagement` com
`repositoriesMode` restrito, declare-o você mesmo.

`android/build.gradle` (Groovy):

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://github.com/pagseguro/PlugPagServiceWrapper/raw/master' }
    }
}
```

Ou `android/settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://github.com/pagseguro/PlugPagServiceWrapper/raw/master") }
    }
}
```

### 2. `AndroidManifest.xml` do app

Permissão do PlugPagService e o intent filter na `MainActivity`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="br.com.uol.pagseguro.permission.MANAGE_PAYMENTS" />

    <application ...>
        <activity android:name=".MainActivity" android:exported="true" ...>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="br.com.uol.pagseguro.PAYMENT" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 3. `android/app/build.gradle`

```gradle
android {
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    defaultConfig {
        minSdk = 23
        targetSdk = 35
    }
}
```

### 4. `android/gradle.properties`

O SDK depende de bibliotecas `com.android.support`, então o Jetifier precisa estar ligado:

```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048M
```

### 5. Gradle wrapper

`android/gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-all.zip
```

Não há flavors nem credenciais a configurar.

## Uso rápido

```dart
import 'package:flutter/material.dart';
import 'package:pagseguro_payment_tech/pagseguro_payment_tech.dart';

/// Recebe os eventos do SDK. Todos os métodos precisam ser implementados.
class MeuPaymentHandler extends PaymentHandler {
  @override
  void onTransactionSuccess() => debugPrint('Transação aprovada');

  @override
  void onError(String message) => debugPrint('Erro: $message');

  @override
  void onMessage(String message) => debugPrint('Mensagem do terminal: $message');

  @override
  void onFinishedResponse(String message) {
    // JSON com o resultado (method "transaction", "activate" ou "userData").
    final resposta = PagseguroResponseModel.fromJson(message);
    debugPrint('Resultado: ${resposta.resultCode} NSU: ${resposta.nsu}');
  }

  @override
  void onLoading(bool show) {}

  @override
  void writeToFile({String? transactionCode, String? transactionId, String? response}) {}

  @override
  void onAbortedSuccessfully() => debugPrint('Operação cancelada');

  @override
  void disposeDialog() {}

  @override
  void onActivationDialog() => debugPrint('Terminal não ativado');

  @override
  void onAuthProgress(String message) {}

  @override
  void onTransactionInfo({String? transactionCode, String? transactionId, String? response}) {}
}

class PagamentoPage extends StatefulWidget {
  const PagamentoPage({super.key});

  @override
  State<PagamentoPage> createState() => _PagamentoPageState();
}

class _PagamentoPageState extends State<PagamentoPage> {
  @override
  void initState() {
    super.initState();
    PagseguroPaymentTech.I.initPayment(handler: MeuPaymentHandler());
  }

  Future<void> _pagar() async {
    try {
      // R$ 25,90 no crédito à vista. O Future indica que a operação foi disparada;
      // o resultado chega pelos callbacks do handler.
      await PagseguroPaymentTech.I.payment.creditPayment(2590);
    } on PlatformException catch (e) {
      debugPrint('Falha ao iniciar o pagamento: ${e.code} ${e.message}');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ElevatedButton(onPressed: _pagar, child: const Text('Pagar R\$ 25,90')),
      ),
    );
  }
}
```

`PlatformException` vem de `package:flutter/services.dart`.

## Referência da API

### `PagseguroPaymentTech`

Ponto de entrada (singleton `PagseguroPaymentTech.I`).

| Membro | Descrição |
|---|---|
| `Payment initPayment({required PaymentHandler handler, int maxTimeShowPopUp = 30})` | Inicializa o plugin e registra o handler. Chamado de novo, reaproveita a instância e troca o handler. `maxTimeShowPopUp`: segundos do dialog de impressão da via do cliente. |
| `Nfc initNfc({required NfcHandler handler})` | Inicializa o NFC. Exige `initPayment` antes. Chamado de novo, troca o handler. |
| `Payment get payment` | Instância criada por `initPayment` (lança `Exception` se não houver). |
| `Nfc get nfc` | Instância criada por `initNfc` (lança `Exception` se não houver). |

```dart
final payment = PagseguroPaymentTech.I.initPayment(handler: MeuPaymentHandler(), maxTimeShowPopUp: 45);
final ok = await payment.ready; // true quando o nativo respondeu
final nfc = PagseguroPaymentTech.I.initNfc(handler: MeuNfcHandler());
```

### `Payment`

Os valores são sempre em **centavos** (`int`). `userReference` aceita só letras e números, até 10
caracteres: o plugin remove os outros caracteres, corta em 10 e usa `'SMARTPOS'` se nada sobrar.

| Método | Retorno | Descrição |
|---|---|---|
| `ready` | `Future<bool>` | Resultado do último `initPayment`/`setHandler`. `false` em erro (que também vai para `onError`). |
| `setHandler(PaymentHandler handler)` | `void` | Troca o handler. |
| `creditPayment(int value, {String userReference = 'SMARTPOS', bool printReceipt = true, bool partialPay = false, bool isCarne = false})` | `Future<bool>` | Crédito à vista. |
| `creditPaymentParc(int value, int parc, {PaymentTypeCredit type = PaymentTypeCredit.CLIENT, userReference, printReceipt, partialPay, isCarne})` | `Future<bool>` | Crédito parcelado em `parc` vezes; `type`: `CLIENT` (juros do comprador) ou `SALESMAN` (lojista). |
| `debitPayment(int value, {userReference, printReceipt, partialPay, isCarne})` | `Future<bool>` | Débito. |
| `voucherPayment(int value, {userReference, printReceipt, partialPay, isCarne})` | `Future<bool>` | Voucher. |
| `pixPayment(int value, {userReference, printReceipt, partialPay, isCarne})` | `Future<bool>` | PIX (QR Code na tela do terminal). |
| `startPayment(PaymentType type, int amount, {PaymentTypeCredit installmentType = CLIENT, int installments = 1, userReference, printReceipt, partialPay, isCarne})` | `Future<bool>` | Fluxo genérico. |
| `abortTransaction()` | `Future<bool>` | Cancela a operação em andamento; confirmação em `onAbortedSuccessfully`. |
| `lastTransaction()` | `Future<bool>` | Última transação aprovada, entregue em `onTransactionInfo`. |
| `refund({String? transactionCode, String? transactionId})` | `Future<bool>` | Estorno. Os dois são obrigatórios: nulos/vazios lançam `INVALID_ARGUMENT` sem chamar o SDK. |
| `activePinpad(String activationCode)` | `Future<bool>` | Ativa o terminal. `false` em erro. |
| `isAuthenticated()` | `Future<bool>` | Verifica a ativação; o resultado chega em `onFinishedResponse` (method `activate`). |
| `getSerialNumber()` | `Future<String>` | Número de série; `''` em erro. |
| `getUserData()` | `Future<bool>` | Dados do estabelecimento em `onFinishedResponse` (method `userData`). |
| `calculateInstallments(String saleValue, dynamic installmentType)` | `Future<List<PlugPagInstallment>>` | Parcelas para `saleValue` em centavos (ex.: `'2000'`); `installmentType` é `InstallmentType` ou `int` (1, 2, 3). `[]` em erro. |
| `printerfromFile(String path)` | `Future<bool>` | Imprime o arquivo do caminho (roda em background). `false` em erro. |
| `printerFile(String fileName)` | `Future<bool>` | Imprime um arquivo pelo nome, procurado no armazenamento do app. `false` em erro. |
| `printer(String filePath)` | `Future<bool>` | Imprime o arquivo do caminho (usado pelo `PrintRenderWidget`). `false` em erro. |
| `printerFilePath(String filePath)` | `Future<bool>` | Imprime o arquivo do caminho. `false` em erro. |
| `setDialogstyle({required String title, buttonBackgroundColor, buttonBackgroundColorDisabled, cancelTextColor, confirmTextColor, sendSMSTextColor, titleColor, windowBackgroundColor, required int maxTimeShowPopUp})` | `Future<bool>` | Estilo do dialog de impressão da via do cliente (cores em `#RRGGBB`). |
| `setPrintDialogMaxTime({int maxTimeShowPopUp = 30})` | `Future<bool>` | Só o tempo do dialog, mantendo o estilo. |
| `rebootDevice()` / `beep()` | `Future<bool>` | Reinicia / emite um beep. `false` em erro. |
| `Payment.invalidArgumentCode` | `String` | `'INVALID_ARGUMENT'`. |

**Como funciona o retorno:** o `Future<bool>` completa com `true` assim que a operação é disparada
no terminal. O **resultado** (aprovada, negada, impressa, lida) chega pelos callbacks do
`PaymentHandler`/`NfcHandler`.

### `Nfc`

Os argumentos são `dynamic` e chegam ao nativo como `String`. Cada método completa com `true` ao
disparar a operação; o resultado chega pelo `NfcHandler`.

| Método | Resultado no `NfcHandler` |
|---|---|
| `readNfc(idEvento)` | `showSuccess(List)` / `showErrorRead` |
| `writeNfc(valor, nome, cpf, numeroTag, celular, aberto, idEvento)` | `showSuccessWrite(int)` / `showErrorWrite` |
| `reWriteNfc(valor, idEvento)` | `showSuccessReWrite(String)` / `showErrorReWrite` |
| `debitNfc(idEvento, valor)` | `showSuccessDebitNfc(String)` / `showErrorDebitNfc` |
| `refundNfc(valor, idEvento)` | `showSuccessRefundNfc(String)` / `showErrorRefundNfc` |
| `formatNfc()` | `showSuccessFormat(int)` / `showErrorFormat` |
| `readNfcUid({int timeoutSeconds = 20})` → `Future<String>` | Não usa o handler: devolve o UID em hexadecimal maiúsculo (ex.: `04A1B2C3`) ou lança `PlatformException`. |
| `abortTransaction()`, `lastTransaction()`, `refund({required transactionCode, required transactionId})` | Iguais aos do `Payment`. |
| `setHandler(NfcHandler)` / `closeMethodCallHandler()` | Troca o handler / para de entregar eventos ao NFC (os de pagamento continuam). |

> Qualquer chamada ao plugin feita enquanto uma operação NFC espera o cartão cancela essa operação
> (o resultado deixa de ser entregue). Não faça outras chamadas durante uma leitura/gravação.

### Handlers

`PaymentHandler` (classe abstrata, implemente todos):

| Callback | Quando |
|---|---|
| `onTransactionSuccess()` | Transação aprovada. |
| `onError(String message)` | Erro do SDK (negada, cancelada, falha de comunicação etc.). |
| `onMessage(String message)` | Mensagens do terminal ("APROXIME, INSIRA OU PASSE O CARTÃO", "SENHA" etc.). |
| `onFinishedResponse(String json)` | Resultado em JSON. `method`: `transaction`, `activate` ou `userData`. |
| `onLoading(bool show)` | Início/fim de processamento. |
| `writeToFile({transactionCode, transactionId, response})` | Transação aprovada, com o JSON completo em `response`. |
| `onTransactionInfo({transactionCode, transactionId, response})` | Resposta de `lastTransaction`. |
| `onAbortedSuccessfully()` | O SDK confirmou o `abortTransaction`. |
| `onActivationDialog()` / `disposeDialog()` | Terminal não ativado / fechar o dialog. |
| `onAuthProgress(String message)` | Progresso da ativação/autenticação. |

`NfcHandler` tem `onTransactionSuccess`, `onError`, `onMessage`, `onLoading`, os `showSuccess*` e
`showError*` da tabela do NFC e os mesmos `writeToFile`, `onAbortedSuccessfully`, `disposeDialog`,
`onActivationDialog`, `onAuthProgress` e `onTransactionInfo` (estes dois sem `response`).

`PaymentHandler` e `NfcHandler` podem ficar ativos juntos: `onFinishedResponse` vai sempre para o
`PaymentHandler`, os `show*` sempre para o `NfcHandler` e os eventos comuns para o último registrado.

### Modelos

`PagseguroResponseModel` (lido do JSON de `onFinishedResponse`/`writeToFile`):

| Campo | Tipo | Descrição |
|---|---|---|
| `resultCode` | `int?` | `0` = sucesso. |
| `message` / `errorMessage` / `errorCode` | `String?` | Mensagens do SDK. |
| `transactionCode` / `transactionID` | `String?` | Identificadores da transação (necessários no estorno). |
| `hostNSU` / `nsu` | `String?` | NSU do host e do terminal. |
| `amount` | `String?` | Valor em centavos. |
| `cardBrand` / `cardBin` | `String?` | Bandeira e BIN do cartão. |
| `date` / `time` | `String?` | Data e hora. |
| `terminalSerialNumber` | `String?` | Serial do terminal. |
| `paymentType` | `int?` | Tipo do pagamento (`PaymentType.value`). |
| `isTransactionValid` | `bool` | `resultCode == 0` com NSU (ou `transactionCode` no PIX). |

Construtores: `PagseguroResponseModel.fromJson(String)` e `.fromMap(Map)`; também `toMap`, `toJson`
e `copyWith`.

`PlugPagInstallment`: `quantity`, `amount` e `total` (centavos).

### Enums

| Enum | Valores |
|---|---|
| `PaymentType` | `TYPE_CREDITO` (1), `TYPE_DEBITO` (2), `TYPE_VOUCHER` (3), `TYPE_QRCODE` (4), `TYPE_PIX` (5), `TYPE_PREAUTO_CARD` (6), `TYPE_QRCODE_CREDITO` (7), `TYPE_PREAUTO_KEYED` (8) |
| `PaymentTypeCredit` | `SALESMAN` (2, parcelado lojista), `CLIENT` (3, parcelado comprador) |
| `InstallmentType` | `A_VISTA` (1), `PARC_VENDEDOR` (2), `PARC_COMPRADOR` (3) |
| `PagseguroPaymentType` | `debit`, `credit`, `creditParc`, `voucher`, `pix`, `unknow` (para a `PagseguroPage`) |

O número enviado ao SDK é o `.value` de cada enum.

### `PagseguroPage`

Tela pronta: verifica a ativação, ativa o terminal se preciso, faz o pagamento, mostra as mensagens
do SDK com botão de cancelar e fecha devolvendo um `PagseguroResponseModel?`. Exige `initPayment`
antes.

- `PagseguroPageParams`: `amount` (`double`, reais), `amountInCents` (`int?`, tem prioridade),
  `type` (`PagseguroPaymentType`), `parcels`, `creditType`, `options` e `style`.
- `PagseguroOptions`: `activationCode` e `isPrinterEstablishment`.
- `PagseguroStyle`: `backgroundColor`, `textColor`, `buttonColor`, `buttonTextColor`, `icon` e `iconColor`.

### `PrintRenderWidget`

`PrintRenderWidget.print(context, pagseguroPaymentInstance: PagseguroPaymentTech.I, child: widget)`
abre uma tela que renderiza o `child`, gera um PNG, imprime com `payment.printer` e fecha.

## Exemplos por operação

```dart
final payment = PagseguroPaymentTech.I.payment;
```

### Crédito à vista e parcelado

```dart
await payment.creditPayment(10000);                                  // R$ 100,00 à vista
await payment.creditPaymentParc(30000, 3);                            // 3x, juros do comprador
await payment.creditPaymentParc(30000, 3, type: PaymentTypeCredit.SALESMAN); // 3x sem juros (lojista)

final parcelas = await payment.calculateInstallments('30000', InstallmentType.PARC_COMPRADOR);
for (final p in parcelas) {
  debugPrint('${p.quantity}x de ${p.amount} (total ${p.total})');
}
```

### Débito, voucher e PIX

```dart
await payment.debitPayment(1500, userReference: 'PEDIDO42');
await payment.voucherPayment(3200);
await payment.pixPayment(5000);
```

### Fluxo genérico

```dart
await payment.startPayment(
  PaymentType.TYPE_CREDITO,
  2500,
  installmentType: PaymentTypeCredit.CLIENT,
  installments: 2,
  printReceipt: false,
);
```

### Ativação do terminal

```dart
if (!await payment.activePinpad('403938')) {
  debugPrint('Não foi possível ativar');
}
await payment.isAuthenticated(); // resultado em onFinishedResponse (method "activate")
```

### Cancelamento, última transação e estorno

```dart
await payment.abortTransaction(); // durante "APROXIME O CARTÃO"; confirma em onAbortedSuccessfully

await payment.lastTransaction();  // dados em onTransactionInfo

// transactionCode e transactionId vêm do writeToFile/onFinishedResponse da venda
await payment.refund(transactionCode: codigo, transactionId: id);
```

### Impressão

```dart
await payment.printerfromFile('/data/user/0/com.exemplo/files/cupom.png');
await payment.printerFile('cupom.png'); // procura no armazenamento do app
await payment.setPrintDialogMaxTime(maxTimeShowPopUp: 45);
await payment.setDialogstyle(
  title: 'Imprimir via do cliente?',
  buttonBackgroundColor: '#FFFFFF',
  buttonBackgroundColorDisabled: '#CCCCCC',
  cancelTextColor: '#37A964',
  confirmTextColor: '#37A964',
  sendSMSTextColor: '#37A964',
  titleColor: '#FFFFFF',
  windowBackgroundColor: '#37A964',
  maxTimeShowPopUp: 30,
);

// Imprimir um widget Flutter
PrintRenderWidget.print(
  context,
  pagseguroPaymentInstance: PagseguroPaymentTech.I,
  child: const SizedBox(width: 380, child: Text('Comprovante')),
);
```

`printerFile(nome)` procura o arquivo, nesta ordem: caminho absoluto, `getExternalFilesDir(DIRECTORY_DOWNLOADS)`,
`getExternalFilesDir(null)` (o `getExternalStorageDirectory()` do `path_provider`), `getFilesDir()` e
`getCacheDir()`.

### NFC

```dart
final nfc = PagseguroPaymentTech.I.initNfc(handler: MeuNfcHandler());

await nfc.writeNfc('500', 'Cliente', '12345678901', 'TAG01', '11999999999', 'true', 'evento1');
await nfc.readNfc('evento1');           // showSuccess(List) com os dados gravados
await nfc.debitNfc('evento1', '200');   // showSuccessDebitNfc
await nfc.refundNfc('200', 'evento1');  // showSuccessRefundNfc
await nfc.reWriteNfc('800', 'evento1'); // showSuccessReWrite
await nfc.formatNfc();                  // showSuccessFormat

// Cashless com saldo no servidor: use só o UID como chave do cartão
try {
  final uid = await nfc.readNfcUid(timeoutSeconds: 15);
  debugPrint('Cartão $uid');
} on PlatformException catch (e) {
  debugPrint('Cartão não lido: ${e.message}');
}
```

### Tela pronta

```dart
final resultado = await Navigator.of(context).push<PagseguroResponseModel?>(
  MaterialPageRoute(
    builder: (_) => PagseguroPage(
      params: PagseguroPageParams(
        amount: 25.90,
        amountInCents: 2590,
        type: PagseguroPaymentType.creditParc,
        parcels: 3,
        creditType: PaymentTypeCredit.CLIENT,
        options: PagseguroOptions(activationCode: '403938', isPrinterEstablishment: true),
      ),
    ),
  ),
);

if (resultado?.isTransactionValid ?? false) {
  debugPrint('Aprovada: ${resultado!.transactionCode}');
} else {
  debugPrint('Falhou: ${resultado?.errorMessage}');
}
```

Em erro a tela fecha com `resultCode: 9999999` e a mensagem em `errorMessage`.

## Tratamento de erros

Erros do lado Dart/canal chegam como `PlatformException`:

| Código | Significado |
|---|---|
| `INVALID_ARGUMENT` | Argumento obrigatório ausente ou vazio (ex.: `refund` sem `transactionCode`/`transactionId`). O SDK não é chamado. |
| `ERROR` | Falha ao disparar a operação no SDK (mensagem do SDK em `message`), ou `readNfcUid` sem cartão no tempo. |
| `NOT_INITIALIZED` | O plugin nativo não está anexado à engine Flutter (ex.: chamada depois de a engine ser destruída). |
| `notImplemented` (`MissingPluginException`) | Método inexistente no nativo, ou app rodando fora do Android. |

Métodos que tratam o erro e devolvem um valor padrão: `activePinpad`, `printer*`, `beep` e
`rebootDevice` devolvem `false`; `getSerialNumber` devolve `''`; `calculateInstallments` devolve
`[]`. Os demais repassam a `PlatformException`.

Erros da transação em si (cartão negado, senha errada, operação cancelada) chegam em
`PaymentHandler.onError` e no JSON de `onFinishedResponse` (`resultCode` diferente de `0`,
`errorCode` e `errorMessage` do SDK).

## App de exemplo

O diretório `example/` tem um app com pagamentos, ativação, cancelamento, última transação,
estorno e impressão. Com uma maquininha Smart conectada:

```bash
cd example
flutter pub get
flutter run
```

## Testes

```bash
# Testes Dart (MethodChannel simulado): todos os métodos públicos, eventos, modelos,
# PagseguroPage e PrintRenderWidget
flutter test

# Testes JVM do módulo nativo (JDK 17 e FLUTTER_SDK apontando para o SDK do Flutter)
FLUTTER_SDK=$(dirname $(dirname $(which flutter))) \
  example/android/gradlew -p android/native --no-daemon -Dorg.gradle.jvmargs=-Xmx2g \
  -Pandroid.jetifier.ignorelist=flutter.jar testDebugUnitTest
```

O código nativo fica em `android/native` e é distribuído pré-compilado em `android/libs`
(`pagseguro_payment_tech-release.aar` e `pagseguro_payment_tech-native.jar`). Depois de alterar
`android/native`, gere os binários de novo:

```bash
JAVA_TOOL_OPTIONS=-Xmx2g sh scripts/build_android_aar.sh   # ou: make build-native
```

## Migração da 3.x para a 4.0.0

A 4.0.0 remove a licença e a cobrança. O que muda:

- `initPayment` e `Payment.create` não recebem mais `licenceKey` nem `licenceInternalKey`.
- `payment.licenceReady` passou a se chamar `payment.ready`.
- `Payment.licenceErrorCode` e o código `LICENCE_ERROR` não existem mais.
- O handler não recebe mais `onMessage("Verificando licença")`/`("Licença ativada")` nem
  `onFinishedResponse` com `method: "licence"`.
- Remova o `--dart-define` das chaves e qualquer chave de licença do `local.properties`.
- O pacote passa a exigir Flutter 3.16+ e Dart 3.2+.

Antes (3.x):

```dart
final payment = PagseguroPaymentTech.I.initPayment(
  handler: MeuPaymentHandler(),
  licenceKey: 'SUA_LICENCA',
  licenceInternalKey: 'aHR0cHM6Ly9...',
  maxTimeShowPopUp: 30,
);
final ok = await payment.licenceReady;

try {
  await payment.debitPayment(1000);
} on PlatformException catch (e) {
  if (e.code == Payment.licenceErrorCode) {
    // licença inválida
  }
}
```

Depois (4.0.0):

```dart
final payment = PagseguroPaymentTech.I.initPayment(
  handler: MeuPaymentHandler(),
  maxTimeShowPopUp: 30,
);
final ok = await payment.ready;

try {
  await payment.debitPayment(1000);
} on PlatformException catch (e) {
  debugPrint('Falha ao iniciar: ${e.code} ${e.message}');
}
```

## Contribuindo

Issues e pull requests são bem-vindos em
[github.com/JY-Labtech/pagseguro_payment_tech](https://github.com/JY-Labtech/pagseguro_payment_tech/issues).

1. Abra uma issue descrevendo o bug (modelo da maquininha, versão do PlugPagService, logs) ou a melhoria.
2. Faça um fork, crie um branch e mantenha `flutter analyze` e `flutter test` sem erros.
3. Se mexer em `android/native`, rode os testes JVM e gere os binários com `scripts/build_android_aar.sh`.
4. Atualize o `CHANGELOG.md` e abra o PR.

## Licença

[MIT](LICENSE). Copyright (c) 2023 Gabuldev e Copyright (c) 2026 JY Labtech.

## Créditos

- Mantido por **JY Labtech**: [jylabtech.com.br](https://jylabtech.com.br).
- Derivado de um projeto open source (MIT) de **Gabuldev** (2023), cujo aviso de copyright é mantido no `LICENSE`.
- SDK [PlugPagServiceWrapper](https://github.com/pagseguro/PlugPagServiceWrapper), da PagSeguro.
