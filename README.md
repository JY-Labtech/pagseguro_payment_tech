# Pagseguro Payment Tech

<div align="center" id="top"> 
  <img src="assets/logo_pagseguro.png" alt="PagSeguro" height=80 />
</div>

<p align="center">
  <a href="#dart-sobre">Sobre</a> &#xa0; | &#xa0; 
  <a href="#rocket-tecnologias">Tecnologias</a> &#xa0; | &#xa0;
  <a href="#-requisitos-do-projeto">Requisitos</a> &#xa0; | &#xa0;
  <a href="#checkered_flag-configuração">Configuração</a> &#xa0; | &#xa0;
</p>

<br>

> ⚠️ Plugin não oficial!

## 🎯 Sobre

Este projeto visa facilitar a integração com o SDK PagSeguro Smart no Flutter.
Compatível apenas com máquinas POS Smart da PagSeguro.

### PlugPagServiceWrapper: 1.33.0

## 🔐 Licenciamento

O plugin exige uma licença válida da **JY Labtech**, validada no primeiro uso em cada terminal.

### Onde adquirir

- Adquira e gerencie suas licenças no portal **[portal.jylabtech.com.br](https://portal.jylabtech.com.br)** (o portal antigo foi substituído; use o mesmo e-mail e senha de antes).
- O produto é único — **"Licença plugin de pagamento"** — e vale para **qualquer** plugin de pagamento da JY Labtech.
- Planos:
  - **`mensal`**: cobrado por dispositivo (cada terminal que valida a licença ocupa uma vaga).
  - **`debug`**: cortesia concedida automaticamente no cadastro — até **2 aparelhos** por **365 dias**, para desenvolvimento e testes.
- No portal você pode consultar e **desativar aparelhos** vinculados à licença (para liberar vagas).
- **Chaves de licença já existentes continuam válidas** (os mesmos valores). Chaves novas no formato `JYL-...` também são aceitas.

### Como informar a licença

A chave é passada em tempo de execução no `initPayment` (não use mais `local.properties`):

```dart
final payment = PagseguroPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  licenceKey: '<sua_licenca>',
);

// Opcional: aguarde o resultado da validação inicial.
final licencaOk = await payment.licenceReady; // false = veja onError no handler

final nfc = PagseguroPaymentTech.I.initNfc(handler: MyNfcHandler());
```

- `licenceKey`: a chave da sua licença, obtida no portal. Obrigatória.
- `licenceInternalKey`: **opcional**. O plugin sempre valida no servidor oficial da JY Labtech (`https://licence-server.invalid/pos-payments/licence/check`), que é o padrão. Só são aceitos valores `https` desse mesmo host com caminho `/pos-payments/licence/check...` (texto puro ou base64, por exemplo a variante `.../check/invoice`). **Qualquer outro valor é ignorado** e o servidor oficial é usado no lugar, inclusive o endereço antigo (`***REMOVIDO***...`) e servidores de terceiros.

### Funcionamento

- O plugin envia ao servidor a chave (`Authorization: Bearer licence:<licenceKey>`) e a identificação do aparelho (ANDROID_ID, serial e modelo), com timeout de 15 s.
- A licença só é liberada se o servidor responder **HTTP 200** com uma licença **válida e ativa**: `expiredAt` no futuro e sem `isExpired`, `isInactive` ou `isCanceled`. Qualquer outra resposta (erro HTTP, timeout, sem internet, JSON inválido, licença vencida/suspensa/cancelada) bloqueia o uso.
- Se válida, a licença é **armazenada localmente de forma criptografada** e reutilizada **offline** até a data de vencimento (`expiredAt`), por no máximo **7 dias** sem nova consulta ao servidor. Depois disso o terminal precisa de internet para revalidar. Um cache com data de gravação no futuro (relógio adulterado) é descartado.
- Trocar a `licenceKey` (ou a URL oficial efetiva, por exemplo de `.../check` para `.../check/invoice`) invalida o cache e força uma nova consulta.
- **Toda operação** (pagamentos, estorno, cancelamento, última transação, ativação/consulta do pinpad, impressão, NFC, dados do usuário, parcelas, beep, reboot, serial e estilo do dialog) confere a licença no nativo antes de chamar o SDK da PagSeguro. A validação roda fora da main thread.
- Sem licença válida a operação **não é executada** e o Flutter recebe `PlatformException(code: 'LICENCE_ERROR', message: <motivo>)`. Os métodos que retornam `bool`/valor padrão (`activePinpad`, `printer*`, `beep`, `rebootDevice`, `getSerialNumber`, `calculateInstallments`) devolvem `false`/vazio; os demais lançam a exceção. O motivo também chega em `onError`/`onMessage` do `PaymentHandler`.

### Erros de licença

| HTTP | Mensagem (exemplo) | Causa / o que fazer |
|------|--------------------|---------------------|
| 404 | `Licença não encontrada com a chave fornecida [...]` | Chave inexistente ou digitada errada. Confira a `licenceKey` no portal. |
| 412 | `A quantidade de licenças verificadas excedeu o limite permitido. Quantidade atual: [N]` | Limite de aparelhos atingido (no plano `debug`, a mensagem cita "modo de depuração"). Desative aparelhos não usados no portal ou contrate mais dispositivos. |
| 412 | `Dispositivo desativado para esta licença` | Este aparelho foi desativado no portal. Reative-o no portal. |
| 428 | `A data de expiração da licença é inválida. [...]` | Licença vencida, suspensa ou cancelada. Regularize/renove no portal. |
| 403 | `Token estático incorreto` | `licenceKey` vazia ou cabeçalho de autorização ausente. |
| 400 | `Schema inválido [...]` | O aparelho não enviou um identificador estável (ANDROID_ID/serial). |
| 429 | — | Muitas consultas em pouco tempo. Aguarde alguns segundos e tente novamente. |

### Troubleshooting

- **`PlatformException` com código `LICENCE_ERROR`**: a validação falhou; veja a mensagem da exceção (ou a recebida em `onError`/`onMessage` do seu handler) e consulte a tabela acima.
- **Sem internet no primeiro uso**: a primeira validação (e as revalidações a cada 7 dias) exige conexão.
- **Trocou de aparelho**: desative o aparelho antigo no portal para liberar a vaga.
- **Ainda passa o `licenceInternalKey` antigo**: não é mais necessário; o valor é ignorado e o servidor oficial é usado. Pode remover o parâmetro.

## ⚙️ Requisitos do Projeto

Para utilizar este plugin, seu projeto Flutter precisa atender aos seguintes requisitos:

### Versões Mínimas

- **Flutter SDK**: >= 3.0.0
- **Dart SDK**: >= 3.0.0
- **Gradle**: >= 8.13
- **Android Gradle Plugin (AGP)**: >= 8.13.1
- **Kotlin**: >= 2.1.21
- **compileSdk**: >= 35
- **minSdk**: >= 23
- **Java**: >= 17 (para o projeto do exemplo)

### Configuração do Gradle

#### 1. `android/gradle/wrapper/gradle-wrapper.properties`

Certifique-se de que seu projeto está usando o Gradle 8.13 ou superior:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-all.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

#### 2. `android/gradle.properties`

Adicione as seguintes configurações ao seu arquivo `gradle.properties`:

```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048M -Dkotlin.daemon.jvm.options="-Xmx2048M"
```

#### 3. `android/build.gradle` (raiz do projeto)

No arquivo `build.gradle` na raiz do seu projeto Android, configure:

```gradle
buildscript {
    ext {
        kotlin_version = '2.1.21'
        // ... outras configurações
    }

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:8.13.1'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://github.com/pagseguro/PlugPagServiceWrapper/raw/master'
        }
    }
}
```

#### 4. `android/app/build.gradle` (módulo app)

Configure o módulo app com as versões corretas:

```gradle
android {
    namespace = "com.seu.pacote.app"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 35
        // ... outras configurações
    }
}
```

#### 5. `android/settings.gradle` (para projetos que usam o novo sistema de plugins)

Se você estiver usando a nova sintaxe de plugins do Flutter, seu `settings.gradle` deve seguir este padrão:

```gradle
pluginManagement {
    def flutterSdkPath = {
        def properties = new Properties()
        file("local.properties").withInputStream { properties.load(it) }
        def flutterSdkPath = properties.getProperty("flutter.sdk")
        assert flutterSdkPath != null, "flutter.sdk not set in local.properties"
        return flutterSdkPath
    }()

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id "dev.flutter.flutter-plugin-loader" version "1.0.0"
    id "com.android.application" version "8.13.0" apply false
    id "org.jetbrains.kotlin.android" version "2.1.21" apply false
}

include ":app"
```

### Verificação

Após configurar seu projeto, execute:

```bash
cd android
./gradlew --version
```

Certifique-se de que a versão do Gradle exibida seja **8.13** ou superior.

## 🚀 Tecnologias

- [Flutter](https://flutter.dev/)
- [PlugPagServiceWrapper (Android)]

## 🏁 Configuração

### pubspec.yaml

```yaml
dependencies:
  pagseguro_payment_tech: ^3.3.0
```

---

## Configuração Android

1. **Permissão obrigatória** no `AndroidManifest.xml` do app:

   ```xml
   <uses-permission android:name="br.com.uol.pagseguro.permission.MANAGE_PAYMENTS" />
   ```

2. **Intent filter** na `MainActivity` para receber callbacks:

   ```xml
   <intent-filter>
       <action android:name="br.com.uol.pagseguro.PAYMENT" />
       <category android:name="android.intent.category.DEFAULT" />
   </intent-filter>
   ```

3. **Configuração mínima do app**:

   ```gradle
   defaultConfig {
       applicationId "com.exemplo.pagamento"
       minSdkVersion 23
       targetSdkVersion flutter.targetSdkVersion
   }
   ```

4. O host precisa do app **PlugPagService** instalado e atualizado no equipamento.

---

## Inicialização

As chaves de licença **não** ficam mais em `local.properties`. O app deve fornecê-las em tempo de execução (veja [Licenciamento](#-licenciamento)).

```dart
final payment = PagseguroPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  licenceKey: licenceKeyFromBackend,
  maxTimeShowPopUp: 30, // opcional: tempo do dialog de impressão da via do cliente (padrão: 30s)
);
```

- Busque a `licenceKey` com segurança (ex.: backend autenticado, serviço de segredos).
- O módulo nativo valida no servidor oficial, armazena criptografado e reutiliza a licença quando possível (até 7 dias offline). Sem licença válida, toda operação falha com `LICENCE_ERROR`.
- O parâmetro `maxTimeShowPopUp` configura o tempo máximo (em segundos) que o dialog de impressão da via do cliente permanece visível no SDK PagSeguro.

---

## Operações de Pagamento

```dart
// Ativar pinpad
await payment.activePinpad('CODIGO_ATIVACAO');

// Pagamentos
await payment.debitPayment(1000);            // R$ 10,00
await payment.creditPayment(1000);
await payment.creditPaymentParc(1000, 2);    // Parcelado
await payment.voucherPayment(1000);
await payment.pixPayment(1000);

// Fluxo genérico
await payment.startPayment(
  PaymentType.TYPE_CREDITO,
  2500,
  installments: 3,
  installmentType: PaymentTypeCredit.CLIENT,
);

// Operações auxiliares
await payment.lastTransaction();
await payment.abortTransaction();
await payment.refund(transactionCode: code, transactionId: id); // ambos obrigatórios
await payment.isAuthenticated();
```

> **Retorno dos métodos:** com a licença válida, cada `Future` completa assim que a operação é
> disparada no nativo (`true`; `getSerialNumber` devolve o serial e `calculateInstallments` a
> lista de parcelas). O **resultado** da transação, impressão ou leitura NFC continua chegando pelos
> callbacks do `PaymentHandler`/`NfcHandler` (`onTransactionSuccess`, `onError`,
> `onFinishedResponse`, `showSuccess...`). Falha ao disparar a operação gera
> `PlatformException(code: 'ERROR')` (ou `false` nos métodos que retornam `bool` com tratamento
> interno). A partir da 3.3.0 `debitPayment` e os métodos NFC também completam (antes ficavam
> pendentes para sempre).
>
> **3.3.0:** `getSerialNumber`, `getUserData`, `calculateInstallments`, `beep`, `rebootDevice` e
> `printerfromFile` rodam numa thread de background no nativo (antes travavam a UI) e o `Future`
> completa quando o SDK responde. `beep`/`rebootDevice` passam a devolver `false` se o SDK falhar
> (antes a falha era engolida e devolviam `true`).

### Validação de entrada (3.3.0)

- `userReference`: o SDK aceita só letras e números, até 10 caracteres. O plugin remove os demais
  caracteres e corta em 10 (`'PED-123/A'` vira `'PED123A'`). Se nada sobrar, usa `'SMARTPOS'`.
- `refund` (em `Payment` e `Nfc`): `transactionCode` e `transactionId` são obrigatórios. Nulos ou
  vazios lançam `PlatformException(code: 'INVALID_ARGUMENT')` (`Payment.invalidArgumentCode`) sem
  chamar o SDK. O nativo faz a mesma checagem.

### Callbacks (eventos do nativo)

- `onAbortedSuccessfully` passa a ser enviado quando o SDK confirma o `abortTransaction()` (antes
  nunca era chamado). Se não havia nada para abortar, nenhum evento é enviado; falha do SDK chega
  por `onError`.

- `onTransactionSuccess` passa a ser entregue (o nativo enviava `onTransactionSuccess` e o Dart só
  reconhecia `onTransactionSucess`, então o callback nunca era chamado).
- `PaymentHandler` e `NfcHandler` podem ficar ativos ao mesmo tempo: `onFinishedResponse` vai
  sempre para o `PaymentHandler`, os eventos `show*` vão sempre para o `NfcHandler` e os eventos
  comuns (`onError`, `onMessage`, `onLoading`...) vão para o último handler registrado. Antes, o
  último `initNfc`/`setHandler` "roubava" o canal e os eventos do outro eram perdidos.
- `nfc.closeMethodCallHandler()` só desliga o NFC; os eventos de pagamento continuam chegando.
- Argumentos nulos vindos do nativo (ex.: erro sem mensagem) chegam como `''` em vez de quebrar o
  callback.
- `PagseguroResponseModel.fromJson` agora preenche `transactionID` (o nativo envia `transactionId`)
  e aceita números ou textos nos campos.
- Builds release com R8 (padrão do Flutter): o plugin traz regras de keep para o JSON de
  `onFinishedResponse`/`writeToFile`/`onTransactionInfo`. Antes o R8 removia os campos e o JSON
  chegava sem `hostNsu`, `nsu`, `cardBrand` etc.

---

## Impressão

```dart
await payment.printerfromFile('/storage/emulated/0/receipt.pdf');
await payment.printerFile('receipt.png'); // nome de arquivo no armazenamento do app (veja abaixo)
await payment.printer('/storage/emulated/0/Download/receipt.png'); // caminho do arquivo
await payment.printerFilePath('/storage/emulated/0/Download/receipt.png');

// printerFile(nome): desde a 3.3.0 o arquivo é procurado no armazenamento do APP, nesta ordem:
// caminho absoluto existente, getExternalFilesDir(DIRECTORY_DOWNLOADS), getExternalFilesDir(null)
// (o mesmo de getExternalStorageDirectory() do path_provider), getFilesDir() e getCacheDir().
// Antes era <armazenamento externo público>/Download (API obsoleta, sem acesso no Android 10+).
// Desde a 3.3.0 os métodos printer* executam a impressão no nativo (passando pela licença)
// e devolvem true quando ela é disparada; antes sempre devolviam false. O resultado da
// impressão chega por onMessage/onAuthProgress/onError do PaymentHandler.

// Renderizar widgets Flutter
PrintRenderWidget.print(
  context,
  pagseguroPaymentInstance: PagseguroPaymentTech.I,
  child: MyCustomReceipt(),
);
```

---

## Dialog de impressão da via do cliente

O SDK PagSeguro exibe um dialog para impressão da via do cliente após a transação.  
Por padrão, o plugin configura esse tempo em **30 segundos** via `PlugPagCustomPrinterLayout.setMaxTimeShowPopup()`.

### Definir na inicialização

```dart
PagseguroPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  licenceKey: licenceKeyFromBackend,
  maxTimeShowPopUp: 30, // padrão
);
```

### Alterar em runtime

```dart
await PagseguroPaymentTech.I.payment.setPrintDialogMaxTime(maxTimeShowPopUp: 45);
```

### Personalização completa do dialog (cores + tempo)

Se precisar customizar também título, cores e demais estilos do dialog:

```dart
await payment.setDialogstyle(
  title: 'Imprimir via do cliente?',
  buttonBackgroundColor: '#FFFFFF',
  buttonBackgroundColorDisabled: '#CCCCCC',
  cancelTextColor: '#37A964',
  confirmTextColor: '#37A964',
  sendSMSTextColor: '#37A964',
  titleColor: '#FFFFFF',
  windowBackgroundColor: '#37A964',
  maxTimeShowPopUp: 30, // tempo em segundos
);
```

> Após alterações no código nativo em `android/native`, recompile os binários com `make build-native` antes de testar no app.

---

## NFC

```dart
final nfc = PagseguroPaymentTech.I.initNfc(handler: MyNfcHandler());

// Todos os argumentos são enviados como String ao nativo (números/bool são convertidos).
// Cada chamada completa com true quando a operação é disparada; o resultado chega pelos
// callbacks do NfcHandler (showSuccess.../showError...). Antes da 3.3.0 o Future nunca completava.
// writeNfc(valor, nome, cpf, numeroTag, celular, aberto, idEvento)
await nfc.writeNfc('500', 'Cliente', '12345678901', 'TAG01', '11999999999', 'true', 'evento');

await nfc.readNfc('evento');                 // readNfc(idEvento)
await nfc.debitNfc('evento', '500');         // debitNfc(idEvento, valor)

// Só o UID do cartão (nada é gravado): ideal para saldo guardado no servidor.
final uid = await nfc.readNfcUid(timeoutSeconds: 20); // ex.: '04A1B2C3'
await nfc.refundNfc('500', 'evento');        // refundNfc(valor, idEvento)
await nfc.reWriteNfc('500', 'evento');       // reWriteNfc(valor, idEvento)
await nfc.formatNfc();
```

> **Atenção:** qualquer chamada ao plugin (inclusive de pagamento, `beep`, impressão etc.) feita
> enquanto uma operação NFC aguarda o cartão cancela a assinatura NFC pendente: o resultado dessa
> leitura/gravação deixa de ser entregue ao `NfcHandler`. É o comportamento das versões anteriores
> e foi mantido (serve como "cancelar"). Não faça outras chamadas durante uma operação NFC, ou
> refaça a operação depois.

---

## Página pronta

O pacote disponibiliza `PagseguroPage` com fluxo completo de pagamento. Consulte `lib/src/app/presentation` para personalizar UI e handlers.

```dart
final result = await Navigator.of(context).push<PagseguroResponseModel?>(
  MaterialPageRoute(
    builder: (_) => PagseguroPage(
      params: PagseguroPageParams(
        amount: 25.90,
        amountInCents: 2590, // opcional (3.3.0): evita arredondamento de double
        type: PagseguroPaymentType.debit,
        options: PagseguroOptions(activationCode: 'CODIGO', isPrinterEstablishment: true),
      ),
    ),
  ),
);
```

Erros do nativo ao iniciar o pagamento (exceto `LICENCE_ERROR`, que já chega por `onError`) agora
fecham a tela com `resultCode: 9999999` em vez de deixá-la presa em "Aguarde...".

---

## Executando o exemplo

O app de exemplo usa `String.fromEnvironment` para obter as chaves:

```bash
flutter run \
  --dart-define=PAGSEGURO_LICENCE_KEY=SUA_LICENCE_KEY \
  --dart-define=PAGSEGURO_LICENCE_INTERNAL_KEY=***REMOVIDO***
```

Certifique-se de que os binários nativos presentes em `android/libs/` estejam atualizados (AAR e `<plugin>-native.jar`).

---

## Testes

```bash
# Testes Dart (MethodChannel mockado): todos os métodos públicos, eventos, modelos,
# PagseguroPage/PagseguroController e PrintRenderWidget (123 testes)
flutter test

# Testes JVM do módulo nativo (JDK 17): licença, gate do canal (ChannelRequestHandler),
# roteamento de todos os métodos (PaymentMethodRouter), casos de uso e presenter de pagamento
# com SDK fake (PaymentGateway), eventos enviados ao Dart, mappers e busca de arquivo de impressão (160 testes)
FLUTTER_SDK=$(dirname $(dirname $(which flutter))) \
  example/android/gradlew -p android/native --no-daemon -Dorg.gradle.jvmargs=-Xmx2g \
  -Pandroid.jetifier.ignorelist=flutter.jar testDebugUnitTest
```

A análise técnica completa (tabela por método, bugs corrigidos e otimizações) está em
`doc/ANALISE.md` (fora do pacote publicado, via `.pubignore`).

Depois de alterar o código em `android/native`, regenere os binários com `make build-native` (ou `sh scripts/build_android_aar.sh`).

---

## Suporte & Licença

- Projeto proprietário mantido por **JY Labtech**.
- Uso restrito conforme os termos internos da empresa.
- Para dúvidas, entre em contato com a equipe responsável ou abra um ticket interno.
