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

### PlugPagServiceWrapper: 1.30.52

## 🔐 Licenciamento

Para adquirir uma licença, cadastre-se no site [Licença JY Labtech](https://licenca.jylabtech.com.br).  
Cada licença é válida por terminal. Após a compra, será gerada uma chave `licenceKey` vinculada ao terminal.

### Inicialização com licença

```dart
 PagseguroPaymentTech.initPayment(
  handler: controller,
  licenceKey: '',
  licenceInternalKey: '',
);

 PagseguroPaymentTech.initNfc(
  handler: nfcHandler,
);
```

A partir da versão 2.0.0 é necessário adicionar as chaves no seu arquivo `local.properties` na raiz da pasta `android`.

```
licenceKey=<sua_licenca>
licenceInternalKey=aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjay9pbnZvaWNl
```

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
  pagseguro_payment_tech: any
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

## Inicialização & Licença

As chaves de licença **não** ficam mais em `local.properties`. O app deve fornecê-las em tempo de execução.

```dart
final payment = PagseguroPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  licenceKey: licenceKeyFromBackend,
  licenceInternalKey: licenceInternalKeyFromBackend,
);
```

- Busque as chaves com segurança (ex.: backend autenticado, serviço de segredos).
- O módulo nativo valida, armazena criptografado e reutiliza a licença quando possível.

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
  PaymentType.credit,
  2500,
  installments: 3,
  installmentType: PaymentTypeCredit.CLIENT,
);

// Operações auxiliares
await payment.lastTransaction();
await payment.abortTransaction();
await payment.refund(transactionCode: code, transactionId: id);
await payment.isAuthenticated();
```

---

## Impressão

```dart
await payment.printerfromFile('/storage/emulated/0/receipt.pdf');
await payment.printerFile('receipt.txt');
await payment.printer('TEXTO A SER IMPRESSO');

// Renderizar widgets Flutter
await PrintRenderWidget.print(
  context,
  pagseguroPaymentInstance: PagseguroPaymentTech.I,
  child: MyCustomReceipt(),
);
```

---

## NFC

```dart
final nfc = PagseguroPaymentTech.I.initNfc(handler: MyNfcHandler());

await nfc.writeNfc(
  eventId: 'evento',
  value: 500,
  name: 'Cliente',
  cpf: '12345678901',
  phone: '11999999999',
);

await nfc.readNfc(eventId: 'evento');
await nfc.debitNfc(eventId: 'evento', value: 500);
await nfc.refundNfc(eventId: 'evento', value: 500);
await nfc.formatNfc();
```

---

## Página pronta

O pacote disponibiliza `PagseguroPage` com fluxo completo de pagamento. Consulte `lib/src/app/presentation` para personalizar UI e handlers.

---

## Executando o exemplo

O app de exemplo usa `String.fromEnvironment` para obter as chaves:

```bash
flutter run \
  --dart-define=PAGSEGURO_LICENCE_KEY=SUA_LICENCE_KEY \
  --dart-define=PAGSEGURO_LICENCE_INTERNAL_KEY=SUA_LICENCE_INTERNAL_KEY
```

Certifique-se de que os binários nativos presentes em `android/libs/` estejam atualizados (AAR e `<plugin>-native.jar`).

---

## Suporte & Licença

- Projeto proprietário mantido por **JY Labtech**.
- Uso restrito conforme os termos internos da empresa.
- Para dúvidas, entre em contato com a equipe responsável ou abra um ticket interno.
