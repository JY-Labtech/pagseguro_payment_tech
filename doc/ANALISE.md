# Análise técnica: pagseguro_payment_tech 3.3.0

Escopo: API Dart pública, MethodChannel `pagseguro_payment_tech` (nos dois sentidos), módulo nativo
pré-compilado (`android/native`, distribuído em `android/libs`) até o SDK PlugPag
(`br.com.uol.pagseguro.plugpagservice.wrapper:wrapper:1.33.0`).

## 1. Arquitetura

```
App Flutter
  PagseguroPaymentTech.I ──► Payment / Nfc ──invokeMethod──►  MethodChannel "pagseguro_payment_tech"
        ▲                                                                │
        │ PaymentHandler / NfcHandler                                     ▼
  PagseguroEventDispatcher ◄──eventos (onMessage, show*...)──  PagseguroPaymentTechPlugin (bridge, android/src)
                                                                         │
                                                PagseguroPaymentTechNativePlugin (AAR) ─► ChannelRequestHandler
                                                                         │   gate de licença (thread de background)
                                                                         ▼
                                                PagSeguroPayment ─► PaymentMethodRouter (resposta única)
                                                 │                       │ chamadas bloqueantes ─► AsyncExecutor (background)
                                                 ▼                       ▼
                                  PaymentsPresenter / PrinterPresenter / NFCPresenter (RxJava)
                                                 │
                                  PaymentsUseCase ─► PaymentGateway ─► PlugPag (SDK)
                                  PrinterUsecase / NFCUseCase ─► PlugPag (SDK)
                                                 │
                                  PaymentsFragment / NFCFragment ─► EventSink (main thread) ─► Dart
```

- **Resposta do `invokeMethod`**: indica só que a operação foi disparada (`true`), salvo
  `getSerialNumber` (String) e `calculateInstallments` (lista). O resultado de pagamento, impressão
  e NFC chega pelos eventos.
- **Licença**: toda chamada `payment*`/`startPayment` passa pelo gate (`ChannelRequestHandler`), que
  valida em background: a 1ª validação do processo sempre consulta o servidor, depois revalida a
  cada 4h; recusa do servidor (4xx exceto 408/429, ou 200 com licença inutilizável) apaga o cache e
  bloqueia; falha de rede/408/429/5xx usa o cache por até 72h desde a última validação online.
  Licença inválida devolve `LICENCE_ERROR` sem tocar no SDK. Os casos de uso de pagamento ainda
  conferem o cache local (segunda barreira, `validateExistsLicence`), que só libera se houve
  validação online no processo há < 4h ou falha de rede há < 5 min.
- **Resposta única**: `SingleReplyResult` em todas as camadas; o roteador completa cada chamada
  exatamente uma vez (sucesso, `ERROR` ou `notImplemented`).

## 2. Tabela por método (Dart → canal → nativo)

Legenda. **Gate**: passa pela validação de licença antes do SDK. **Testado**: D = teste Dart
(argumentos, sucesso, `PlatformException` genérica e `LICENCE_ERROR`); N = JUnit (gate, roteamento
com sucesso/erro/resposta única e, onde indicado, caso de uso/presenter com SDK fake).

| Método Dart | Método do canal | Handler nativo → SDK | Gate? | Resposta (sucesso / erro) | Testado? | Problemas encontrados |
|---|---|---|---|---|---|---|
| `PagseguroPaymentTech.initPayment` / `Payment.create` / `Payment.setHandler` | `initPayment` | `ChannelRequestHandler` → `PagSeguroPayment.updateLicenceKeys` + `checkLicence` | é o próprio gate | `true` (`licenceReady` = true) / `LICENCE_ERROR` (o nativo já envia `onError` + `onFinishedResponse` method `licence`) | D, N | Nenhum novo. |
| `Payment.setPrintDialogMaxTime` | `paymentSetPrintDialogMaxTime` | `ChannelRequestHandler` (fora do gate, configuração local) → `PlugPagCustomPrinterLayout` | Não (de propósito) | `true` / `ERROR` | D, N | **Corrigido:** criava um layout novo e descartava o estilo de `setDialogstyle` (o mesmo acontecia em todo `initPayment`). |
| `Payment.setDialogstyle` | `paymentStyleDialog` | Router → `PagSeguroPayment.setDialogStyle` → `setPlugPagCustomPrinterLayout` | Sim | `true` / `ERROR`, `LICENCE_ERROR` | D, N | **Otimizado:** recriava os 3 presenters sem necessidade. `maxTimeShowPopUp` nulo dava NPE (agora usa 30). |
| `Payment.activePinpad` | `paymentActivePinpad` | Router → `PaymentsPresenter.activate` → `initializeAndActivatePinpad` | Sim | `true` + `onAuthProgress`/`onFinishedResponse` (method `activate`) / `false` (erro tratado) | D, N (presenter + caso de uso) | Mensagem de erro nula enviada ao Dart (corrigido). |
| `Payment.creditPayment` | `paymentCredit` | Router → `PaymentsPresenter.creditPayment` → `PaymentsUseCase.doCreditPayment` → `doPayment(TYPE_CREDITO, À vista)` | Sim | `true` + eventos (`writeToFile`, `onMessage`, `onFinishedResponse`, `onTransactionSuccess`) / `ERROR`, `LICENCE_ERROR` | D, N (presenter + caso de uso) | **Corrigidos:** `onTransactionSuccess` nunca chegava ao Dart (nome divergente); terminal não ativado enviava `activeDialog` da thread io (rejeitado pelo Flutter) e podia dar NPE em `mSubscribe`; erro sem mensagem chegava `null` e quebrava o `onError` no Dart. |
| `Payment.creditPaymentParc` | `paymentCreditParc` | idem → `doPayment(TYPE_CREDITO, type, parc)` | Sim | idem | D, N | Idem acima. |
| `Payment.debitPayment` | `paymentDebit` | idem → `doPayment(TYPE_DEBITO)` | Sim | idem | D, N | Idem acima. |
| `Payment.pixPayment` | `paymentPix` | idem → `doPayment(TYPE_PIX)` | Sim | idem | D, N | Idem acima. |
| `Payment.voucherPayment` | `paymentVoucher` | idem → `doPayment(TYPE_VOUCHER)` | Sim | idem | D, N | Idem acima. |
| `Payment.startPayment` | `startPayment` | idem → `doPayment(type, amount, installmentType, installments)` | Sim | idem | D, N | Idem acima. |
| `Payment.abortTransaction` / `Nfc.abortTransaction` | `paymentAbort` | Router → `PaymentsPresenter.abortTransaction` → `PlugPag.abort` (`PlugPagAbortResult`) | Sim | `true` + `onAbortedSuccessfully` se o SDK confirmar / `ERROR`, `LICENCE_ERROR`, `onError` | D, N | **Corrigidos:** falha do SDK derrubava o app (`subscribe()` sem `onError`); sobrescrevia `mSubscribe` da transação (vazamento); o `Completable` nunca completava; `onAbortedSuccessfully` nunca era enviado. |
| `Payment.lastTransaction` / `Nfc.lastTransaction` | `paymentLastTransaction` | Router → `getLastTransaction` → `getLastApprovedTransaction` | Sim | `true` + `onTransactionInfo` / `onError` | D, N | **Corrigido:** `Integer` nulo do SDK (`result`, `originalAmount`, `paymentType`) gerava NPE. |
| `Payment.refund` / `Nfc.refund` | `paymentRefund` | Router (valida código/id) → `doRefund` → `voidPayment(code, id, print=true)` | Sim | `true` + eventos / `INVALID_ARGUMENT` (Dart e nativo, sem chamar o SDK), `ERROR`, `LICENCE_ERROR` | D, N | **Corrigido:** código/id nulos chegavam ao SDK. Mesma correção de NPE acima. |
| `Payment.isAuthenticated` | `paymentIsAuthenticated` | Router → `isAuthenticate` → `PlugPag.isAuthenticated` | Sim | `true` + `onAuthProgress` + `onFinishedResponse` (method `activate`) | D, N | Mensagem de erro nula (corrigido). |
| `Payment.printerfromFile` | `paymentPrinterFile` | Router (**background**) → `PrinterPresenter.printerFromFile` → `printFromFile` | Sim | `true` / `false` | D, N (router) | **Otimizado:** impressão síncrona rodava na main thread (risco de ANR). |
| `Payment.printerFile` | `paymentPrinter` | Router → `printFile` → `PrintFileResolver` (armazenamento do app) → `printFromFile` | Sim | `true` / `false` | D, N (router + resolver) | **Corrigido:** usava `Environment.getExternalStorageDirectory()/Download` (obsoleto, sem acesso no Android 10+). |
| `Payment.printer` (usado por `PrintRenderWidget`) | `paymentPrinterBasic` | Router → `printer` → `printFromFile` | Sim | `true` / `false` | D, N (router) | **Corrigido:** eventos de "arquivo não encontrado" eram enviados da thread io. |
| `Payment.printerFilePath` | `paymentPrinterFilePath` | Router → `printerByFilePath` | Sim | `true` / `false` | D, N (router) | Idem acima. |
| `Payment.rebootDevice` | `paymentReboot` | Router (**background**) → `PlugPag.reboot` | Sim | `true` / `false` | D, N | **Corrigido:** falha do SDK era engolida (sempre `true`). |
| `Payment.beep` | `paymentBeep` | Router (**background**) → `PlugPag.beep` | Sim | `true` / `false` | D, N | Idem acima. |
| `Payment.getSerialNumber` | `paymentGetSerialNumber` | Router (**background**) → `PlugPag.getSerialNumber` | Sim | serial / `''` | D, N | IPC na main thread (otimizado). |
| `Payment.getUserData` | `paymentGetUserData` | Router (**background**) → `getUserData` → `onFinishedResponse` (method `userData`) | Sim | `true` / `ERROR`, `LICENCE_ERROR` | D, N | IPC na main thread (otimizado). |
| `Payment.calculateInstallments` | `paymentCalculateInstallments` | Router (**background**) → `calculateInstallments` → `List<Map>` | Sim | `List<PlugPagInstallment>` / `[]` | D, N | IPC na main thread (otimizado). Tipo inválido devolve `[]` sem chamar o nativo. |
| `PagseguroPaymentTech.initNfc` / `Nfc.setHandler` | (só Dart) | `PagseguroEventDispatcher` | n/a | `Nfc` / exceção sem `initPayment` | D | **Corrigidos:** a segunda chamada ignorava o handler novo; registrar o NFC "roubava" o canal do `Payment` (eventos perdidos). |
| `Nfc.closeMethodCallHandler` | (só Dart) | `PagseguroEventDispatcher.detachNfc` | n/a | n/a | D | **Corrigido:** removia o handler do canal inteiro (pagamento também parava de receber eventos). |
| `Nfc.readNfc` | `paymentReadNfc` | Router → `NFCPresenter.readNFCCard` → LED + `NFCUseCase.readNfc` (`startNFCCardDirectly`/`readNFCCardDirectly`) | Sim | `true` + `showSuccess`/`showErrorRead` | D, N (router) | `NFCUseCase` sem teste JVM (depende do PlugPag/hardware). |
| `Nfc.writeNfc` | `paymentWriteNfc` | Router → `writeNFCCard` → `UserDataManager.writeUserData` | Sim | `true` + `showSuccessWrite`/`showErrorWrite` | D, N (router) | Argumento nulo dá NPE em `Utils.adicionaAsterisco` (vira `ERROR`). |
| `Nfc.reWriteNfc` | `paymentReWriteNfc` | Router → `reWriteNFCCard` | Sim | `true` + `showSuccessReWrite`/`showErrorReWrite` | D, N (router) | idem |
| `Nfc.refundNfc` | `paymentReFundNfc` | Router → `reFundNFCCard` | Sim | `true` + `showSuccessRefundNfc`/`showErrorRefundNfc` | D, N (router) | **Corrigido (Dart):** `SHOW_ERROR_REFUND_NFC.method` devolvia `null`. |
| `Nfc.formatNfc` | `paymentFormatNfc` | Router → `formatNFCCard` → `clearBlocks` | Sim | `true` + `showSuccessFormat`/`showErrorFormat` | D, N (router) | idem |
| `Nfc.debitNfc` | `paymentDebitNfc` | Router → `debitNFCCard` | Sim | `true` + `showSuccessDebitNfc`/`showErrorDebitNfc` | D, N (router) | idem |
| `PrintRenderWidget.print` | `paymentPrinterBasic` | idem `printer` | Sim | fecha a tela após imprimir | D (widget) | **Corrigido:** a tela ficava aberta se a imagem falhasse; assertion `!debugNeedsPaint` em debug durante a animação. |
| `PagseguroPage` / `PagseguroController` | `initPayment`, `paymentIsAuthenticated`, `paymentActivePinpad`, pagamentos, `paymentAbort` | idem | Sim | `Navigator.pop(PagseguroResponseModel)` | D (widget) | **Corrigidos:** erro do nativo ao iniciar pagamento deixava a tela presa e gerava erro não tratado; JSON inválido quebrava; valor em `double` (adicionado `amountInCents`). |
| Método desconhecido | qualquer outro | `ChannelRequestHandler` / Router | Não | `notImplemented` | N | — |

### Eventos nativo → Dart

| Evento (nome no canal) | Enviado por | Argumento | Handler Dart | Testado | Problemas |
|---|---|---|---|---|---|
| `onTransactionSuccess` | `PaymentsFragment` | `true` | `onTransactionSuccess()` | D, N | **Corrigido:** o Dart esperava `onTransactionSucess` e lançava "NOT IMPLEMENTED". |
| `onError` / `onMessage` / `onAuthProgress` | Payments/NFC/Printer | String (podia ser `null`) | `onError`/`onMessage`/`onAuthProgress` | D, N | **Corrigido:** `null` lançava TypeError e o callback não era chamado. |
| `onFinishedResponse` | `PaymentsFragment` | JSON (`ActionResult`/`UserDataResult`) | `PaymentHandler.onFinishedResponse` | D, N | **Corrigidos:** perdido com o NFC ativo; campos removidos pelo R8 em release. |
| `onLoading` | Payments/Printer | bool | `onLoading` | D, N | — |
| `writeToFile` / `onTransactionInfo` | `PaymentsFragment` | `{transactionCode, transactionId, response}` | `writeToFile`/`onTransactionInfo` | D, N | Chaves nulas lançavam TypeError (corrigido). |
| `disposeDialog` / `activeDialog` / `onAbortedSuccessfully` | `PaymentsFragment` | `true` | callbacks | D, N | **Corrigido:** `onAbortedSuccessfully` nunca era enviado (agora após abort confirmado). |
| `showSuccess` | `NFCFragment` | `List<String>` (7 campos) | `NfcHandler.showSuccess` | D, N | Perdido depois de `payment.setHandler` (corrigido). |
| `showSuccessWrite` / `showSuccessFormat` | `NFCFragment` | int | `showSuccessWrite`/`showSuccessFormat` | D, N | idem |
| `showSuccessReWrite` / `showSuccessDebitNfc` / `showSuccessRefundNfc` / `showError*` | `NFCFragment` | String | callbacks NFC | D, N | idem |
| `onReadCard` / `onWriteCard` / `onAbort` | (removidos do `NFCFragment`) | — | — | D (notImplemented) | Código morto removido. |

## 3. Bugs encontrados e corrigidos

1. **`onTransactionSuccess` nunca chegava ao app**: nome divergente (`onTransactionSuccess` no Java,
   `onTransactionSucess` no Dart). O Dart aceita as duas grafias.
2. **Canal compartilhado entre `Payment` e `Nfc`**: o último `setMethodCallHandler` recebia tudo.
   Com o NFC ativo, `onFinishedResponse` (resultado da licença e das transações) era perdido; depois de
   `payment.setHandler`, os eventos `show*` eram perdidos; `closeMethodCallHandler` desligava também o
   pagamento. Corrigido com `PagseguroEventDispatcher`.
3. **Eventos enviados de thread de background**: `MethodChannel.invokeMethod` fora da main thread é
   rejeitado pelo Flutter. Acontecia no `activeDialog` (terminal não ativado) e nas mensagens de
   impressão. Agora todo evento passa por `MainThreadEventSink`.
4. **Terminal não ativado**: além do problema de thread, `mSubscribe.dispose()` dentro do `filter`
   podia rodar com `mSubscribe` nulo. O fluxo agora envia `activeDialog` + `disposeDialog`, sem
   `onTransactionSuccess` e sem erro.
5. **`abortTransaction` derrubava o app** quando o SDK falhava (`subscribe()` sem `onError` no
   RxJava 2 lança `OnErrorNotImplementedException`). Também sobrescrevia a assinatura da transação
   em andamento e nunca completava.
6. **NPE com `Integer` nulo do SDK** em `ActionResult.buildResponse` (`setResult(int)` etc.).
7. **R8 removia os campos do JSON** em release. Os modelos são lidos pelo Gson por reflexão e o R8
   os via como campos só gravados. Confirmado no `example` (`flutter build apk --release`): sem as
   regras, `hostNsu`, `companyName`, `partialPayRemainingAmount` não existem no dex; com as regras de
   keep, existem.
8. **`PagseguroResponseModel.transactionID` sempre nulo**: o nativo envia `transactionId`. O parse
   também falhava com tipos trocados e `fromMap(toMap())` perdia campos.
9. **Argumentos nulos do nativo** quebravam os callbacks Dart (TypeError).
10. **Estilo do dialog perdido** a cada `initPayment`/`setPrintDialogMaxTime` (a `PagseguroPage`
    chama `setHandler` ao abrir, ou seja, a cada pagamento).
11. **`beep`/`rebootDevice` sempre `true`** (falha do SDK engolida no caso de uso).
12. **`PagseguroPage` presa em "Aguarde..."** quando o nativo devolvia `ERROR` ao iniciar o pagamento,
    o abort ou a ativação (Future com erro sem tratamento). JSON inválido em `onFinishedResponse`
    lançava `FormatException`.
13. **`PrintRenderWidget` não fechava** quando a geração da imagem falhava, e em debug podia capturar
    o boundary antes de ser pintado.
14. **`initNfc` ignorava o handler novo** quando chamado de novo.
15. **`PaymentTypeHandler.SHOW_ERROR_REFUND_NFC.method` devolvia `null`**.
16. **Resposta `null` do nativo em métodos `Future<bool>`** gerava TypeError. Agora devolve `false`.
17. **`userReference` com caracteres especiais** fazia a transação falhar no SDK (aceita só letras e
    números, até 10). Agora o plugin remove os outros caracteres e corta em 10 (vazio vira `SMARTPOS`).
18. **`refund` sem código/id** chegava ao SDK. Agora lança `INVALID_ARGUMENT` no Dart e o nativo
    também rejeita, sem chamar o SDK.
19. **`printerFile` usava armazenamento externo público** (`Environment.getExternalStorageDirectory()`,
    obsoleto). Agora procura no armazenamento do app (`PrintFileResolver`).
20. **`onAbortedSuccessfully` nunca era enviado**: agora é enviado quando `PlugPagAbortResult` é `RET_OK`.

## 4. Otimizações (todas cobertas por testes antes/depois)

| Otimização | Por quê |
|---|---|
| `getSerialNumber`, `getUserData`, `calculateInstallments`, `beep`, `rebootDevice` e `printerfromFile` em background (`AsyncExecutor`), resposta na main thread | Eram chamadas bloqueantes (IPC com o PlugPagService; a impressão leva segundos) executadas na main thread, com risco de ANR. O `Future` continua completando depois que o SDK responde. |
| `EncryptedLicenceStorage` cria `MasterKey` + `EncryptedSharedPreferences` uma vez por instância | Eram recriados (Keystore + Tink) em toda leitura/gravação do cache, inclusive na main thread a cada pagamento (checagem local da licença). Nome das prefs, chave e formato mantidos. |
| Uma única `CheckLicenceTech` compartilhada entre o gate e os casos de uso | Cada `PaymentsUseCase` criava a sua (com storage próprio). |
| Presenters não são recriados em `setMaxTimeShowPopup`/`setDialogStyle` | Todos usam o mesmo `PlugPag`. Recriar não tinha efeito e deixava as assinaturas antigas sem `dispose`. |
| `onDetachedFromEngine` libera todos os presenters (antes só o de pagamento) e as duas threads de background | Evita vazamento de assinaturas RxJava e threads. |
| Uma instância de `Gson` (`JsonResults`) | Era criada uma por evento. |
| Despachante único de eventos no Dart (`PagseguroEventDispatcher`) e helpers `_invokeBool`/`_invokeOrFalse`/`_paymentArguments` | Remove a duplicação dos dois `_callHandler` (Payment/Nfc) e dos blocos try/catch idênticos. |
| Gate de licença extraído para `ChannelRequestHandler` (sem Android) | Permite testar o gate por método na JVM. Comportamento idêntico ao anterior. |
| `PaymentGateway` (fachada do PlugPag), `EventSink`, `ResultMappers` | Isolam o SDK e o canal para testes com fakes. Sem mudança em produção. |
| Remoção de código morto: `transactions/*` (4 classes), `core/keyboardUtils`, `helpers/RetryAction`, eventos `onReadCard`/`onWriteCard`/`onAbort` | Sem referências. Os arquivos removidos aparecem no dry-run como "checked-in files ignored" até o commit. |
| `.pubignore` | Tira `doc/`, `local.properties`, keystores, `.DS_Store` e `*.hprof` do pacote publicado (antes ia `android/native/local.properties`). |
| Contador de dígitos da senha reinicia a cada transação | Uma transação interrompida deixava asteriscos "sobrando" na seguinte. |

## 5. Testes

| Suíte | Antes | Depois | Cobertura |
|---|---|---|---|
| JUnit (`android/native`) | 77 | 160 | Licença (existente); `ChannelRequestHandler` (gate para os 27 métodos de negócio, `LICENCE_ERROR` sem chamar o SDK, `notImplemented`, resposta única, plugin finalizado); `PaymentMethodRouter` (todos os 28 métodos com sucesso e erro, argumentos exatos, execução em background); `PaymentsUseCase` e `PaymentsPresenter` com `PaymentGateway` fake e RxJava síncrono (dados enviados ao SDK, aprovado/recusado, terminal não ativado, senha, abort, ativação, status, última transação, dados do usuário, parcelas, sem licença); `PaymentsFragment`/`NFCFragment` (nomes e argumentos dos eventos); `ResultMappers`/`ActionResult` (JSON lido pelo Dart, inteiros nulos); `PrintFileResolver`; estorno com `INVALID_ARGUMENT`; `onAbortedSuccessfully`; `Utils`. |
| Dart (`flutter test`) | 14 | 123 | Todos os métodos públicos de `Payment` e `Nfc` (argumentos exatos, sucesso, `PlatformException` e `LICENCE_ERROR`); sanitização de `userReference`; estorno com `INVALID_ARGUMENT`; todos os eventos para `PaymentHandler` e `NfcHandler`, convivência dos dois, nulos e eventos desconhecidos; `PagseguroResponseModel`, `PlugPagInstallment`, enums/extensões; `PagseguroPage`/`PagseguroController` (widget); `PrintRenderWidget` (widget, com `path_provider` simulado). |

Comandos: ver a seção "Testes" do README.

## 6. Pendências (decisão do dono / hardware)

1. **Validação em terminal real**: os fluxos com o SDK (pagamento, NFC, impressão, ativação, abort)
   foram testados com fakes. Falta rodar em uma maquininha (comportamento do PlugPagService,
   mensagens, tempos, resultado do `abort()`).
2. **Sem teste JVM** (dependem de Android ou do PlugPag real, Robolectric evitado):
   `MainThreadEventSink`, `AndroidAsyncExecutor`, `EncryptedLicenceStorage`, `PlugPagPaymentGateway`,
   `PagSeguroPayment` (construtor cria `PlugPag(Context)`), `PrintFileResolver.appDirectories`,
   `PrinterUsecase`, `NFCUseCase` e `NFCPresenter`.
3. **NFC: cada chamada ao canal cancela a assinatura NFC anterior** (`initializePresenters` chama
   `nfcPresenter.dispose()`), inclusive chamadas sem relação, como `beep`. Mantido por
   compatibilidade e documentado no README.
4. **Mensagem final da transação**: se o último evento do SDK foi de senha, a resposta final reaproveita
   o `eventCode` e mostra "VALOR/SENHA" em vez da mensagem do SDK (comportamento antigo, mantido).
5. **`PagseguroPage` usa `onPopInvoked`** (obsoleto). Trocar por `onPopInvokedWithResult` exige subir o
   mínimo do Flutter (hoje `>=3.0.0`).
6. **Commit dos arquivos removidos**: `android/native/src/main/java/br/com/jylabtech/pagseguro_payment_tech/`
   `transactions/TransactionsContract.java`, `TransactionsFragment.java`, `TransactionsPresenter.java`,
   `TransactionsUseCase.java`, `core/keyboardUtils.java` e `helpers/RetryAction.java`. Até o commit,
   o `dart pub publish --dry-run` mostra o aviso "6 checked-in files are ignored by a .gitignore".
