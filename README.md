# Pagseguro Payment Tech

<div align="center" id="top"> 
  <img src="assets/logo_pagseguro.png" alt="PagSeguro" height=80 />
</div>

<p align="center">
  <a href="#dart-sobre">Sobre</a> &#xa0; | &#xa0; 
  <a href="#rocket-tecnologias">Tecnologias</a> &#xa0; | &#xa0;
  <a href="#checkered_flag-configuração">Configuração</a> &#xa0; | &#xa0;
</p>

<br>

> ⚠️ Plugin não oficial!

## 🎯 Sobre

Este projeto visa facilitar a integração com o SDK PagSeguro Smart no Flutter.
Compatível apenas com máquinas POS Smart da PagSeguro.

## 🔐 Licenciamento

Para adquirir uma licença, cadastre-se no site [Licença JY Labtech](https://licenca.jylabtech.com.br).  
Cada licença é válida por terminal. Após a compra, será gerada uma chave `licenceKey` vinculada ao terminal.

### Inicialização com licença

```dart
 PagseguroPaymentTech.initPayment(
  handler: controller,
);

 PagseguroPaymentTech.initNfc(
  handler: nfcHandler,
);
```

A partir da versão 1.0.0 é necessário adicionar as chaves no seu arquivo `local.properties` na raiz da pasta `android`.

```
licenceKey=<sua_licenca>
licenceInternalKey=aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjaw==
```

## 🚀 Tecnologias

- [Flutter](https://flutter.dev/)
- [PlugPagServiceWrapper (Android)]

## 🏁 Configuração

### pubspec.yaml

```yaml
dependencies:
  pagseguro_payment_tech: any
```

### Android Manifest

```xml
<uses-permission android:name="br.com.uol.pagseguro.permission.MANAGE_PAYMENTS"/>
```

### Intent-filter

```xml
<intent-filter>
 <action android:name="br.com.uol.pagseguro.PAYMENT"/>
 <category android:name="android.intent.category.DEFAULT"/>
</intent-filter>
```

### build.gradle (app level)

```gradle
defaultConfig {
  applicationId "com.exemplo.pagamento"
  minSdkVersion 23
  targetSdkVersion flutter.targetSdkVersion
  ...
}
```

## 📦 Implementação

### Criar Handler

```dart
class PaymentController extends PaymentHandler {
  // implementação omitida para brevidade
}
```

#### Métodos principais do Handler

- `onTransactionSuccess()` — Transação aprovada
- `onError(String)` — Erro na transação
- `onMessage(String)` — Mensagem da PagSeguro
- `onTransactionInfo(...)` — Detalhes da transação
- `writeToFile(...)` — Personalizável

### Iniciando transações

```dart
PagseguroPaymentTech.I.payment.activePinpad('CODIGO_ATIVACAO');

// Exemplos de pagamento:
PagseguroPaymentTech.I.payment.creditPayment(12.50);
PagseguroPaymentTech.I.payment.creditPaymentParc(1250, 2);
PagseguroPaymentTech.I.payment.debitPayment(12.50);
PagseguroPaymentTech.I.payment.pixPayment(12.50);
PagseguroPaymentTech.I.payment.voucherPayment(12.50);
PagseguroPaymentTech.I.payment.lastTransaction();
```

#### Estorno e cancelamento

```dart
PagseguroPaymentTech.I.payment.refund(
  transactionCode: controller.transactionCode,
  transactionId: controller.transactionId,
);

PagseguroPaymentTech.I.payment.abortTransaction();
```

### Impressão

```dart
PagseguroPaymentTech.I.payment.printerfromFile('path/to/file.pdf');

PrintRenderWidget.print(
  context,
  pagseguroPaymentInstance: pagseguroPayment,
  child: MyCustomWidget(),
);
```

## 📡 NFC Smart

### Inicialização

```dart
 PagseguroPaymentTech.initNfc(
  handler: SeuNfcHandler(),
);
```

### Principais métodos NFC

```dart
await NfcSmart.instance.readNfc(idEvento);
await NfcSmart.instance.writeNfc(valor, nome, cpf, numeroTag, celular, aberto, idEvento);
await NfcSmart.instance.refundNfc(valor, idEvento);
await NfcSmart.instance.formatNfc();
await NfcSmart.instance.debitNfc(idEvento, valor);
await NfcSmart.instance.closeMethodCallHandler();
await NfcSmart.instance.lastTransaction();
```

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/JY-Labtech">
        <img src="https://avatars.githubusercontent.com/u/185066406?s=200&v=4" width="100px;" alt=""/>
        <br /><sub><b>JY Labtech</b></sub>
      </a>
    </td>
  </tr>
</table>

---

⬆️ [Voltar ao topo](#pagseguro-payment-tech)
