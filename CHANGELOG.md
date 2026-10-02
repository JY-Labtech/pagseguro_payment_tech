## 3.3.0

**Validação de licença mais rígida.**

- Validação de licença migrada para o novo servidor da JY Labtech (`https://portal-api.jylabtech.com.br/pos-payments/licence/check`), com o mesmo contrato da API anterior. As chaves de licença existentes continuam válidas.
- Atualiza o valor recomendado de `licenceInternalKey` (o valor antigo passa a ser ignorado e substituído pelo servidor oficial; veja abaixo).
- Licenças passam a ser adquiridas e gerenciadas em https://portal.jylabtech.com.br.
- README: nova seção de licenciamento (portal, planos `mensal`/`debug`, limite por dispositivo, cache offline de 7 dias, tabela de erros e troubleshooting) e correção de exemplos que não batiam com a API (`PaymentType.TYPE_CREDITO`, assinaturas de NFC, `PrintRenderWidget.print`, `PagseguroPaymentTech.I`).
- Exemplo: chaves lidas via `--dart-define` (`PAGSEGURO_LICENCE_KEY` / `PAGSEGURO_LICENCE_INTERNAL_KEY`), com o novo endereço como padrão.
- A URL de validação passa a ser travada no servidor oficial (`portal-api.jylabtech.com.br`, caminho `/pos-payments/licence/check...`, somente `https`). Qualquer outro `licenceInternalKey` (inclusive o endereço antigo do Cloud Run e servidores de terceiros) é ignorado e o servidor oficial é usado no lugar.
- `licenceInternalKey` agora é **opcional** em `initPayment`/`Payment.create` (padrão: servidor oficial). Não quebra quem já informa o valor.
- Corrigido: uma resposta HTTP 200 com licença vencida, inativa/suspensa ou cancelada era aceita. Agora só libera licença com `expiredAt` futuro e sem `isExpired`, `isInactive` ou `isCanceled` (novo campo lido). JSON inválido ou sem `expiredAt` também bloqueia, e nada é salvo no cache nesses casos.
- Cache offline só é usado se a licença estiver válida, tiver sido gravada há no máximo 7 dias (e não no futuro), for da mesma `licenceKey` e da mesma URL oficial; caso contrário é apagado e a licença é revalidada online.
- Toda operação de negócio (pagamentos, estorno, cancelamento, última transação, pinpad, impressão, NFC, dados do usuário, parcelas, beep, reboot, serial, estilo do dialog) valida a licença antes de chamar o SDK e, se inválida, falha com `PlatformException(code: 'LICENCE_ERROR')` sem executar nada. A validação roda em background (não trava mais a main thread) com timeouts de 15 s.
- Erros do servidor viram mensagens úteis (`message` do corpo ou `Erro ao validar licença (HTTP <código>)`).
- Corrigido: falha de licença no `initPayment` gerava `PlatformException` não tratada; agora o erro chega ao `PaymentHandler.onError` e o novo `payment.licenceReady` (`Future<bool>`) informa o resultado. `beep()` e `rebootDevice()` passam a aguardar o nativo e retornam `false` em erro.
- Testes: JUnit das regras de licença no módulo nativo (URL, regras da licença, cache, resposta HTTP, fluxo com rede/armazenamento simulados) e testes Dart do MethodChannel.
- Corrigido: `debitPayment` e os métodos NFC (`readNfc`, `writeNfc`, `reWriteNfc`, `refundNfc`, `formatNfc`, `debitNfc`) nunca completavam o `Future` (o nativo não respondia o MethodChannel). Agora completam com `true` quando a operação é disparada; o resultado continua chegando pelos callbacks do handler. **Mudança visível:** quem fazia `await` nesses métodos agora segue adiante.
- Corrigido: `printerfromFile`, `printerFile`, `printer` e `printerFilePath` sempre devolviam `false` (o nativo respondia `notImplemented` depois de disparar a impressão). Agora passam pela licença, executam a impressão e devolvem `true`; erro ao disparar devolve `false`.
- Todo método do canal passa a completar o result exatamente uma vez (roteamento centralizado em `PaymentMethodRouter` e guarda `SingleReplyResult`); exceções ao disparar a operação viram `PlatformException(code: 'ERROR')` e método desconhecido vira `notImplemented`.
- Argumentos NFC numéricos/bool (os parâmetros são `dynamic` no Dart) são convertidos para `String` no nativo em vez de falhar.
- README: exemplo de `printer()` corrigido (recebe caminho de arquivo) e explicação do retorno dos métodos.
- Testes: JUnit do roteamento do canal (resposta única e tipo esperado para todos os métodos) e testes Dart para `debitPayment`, NFC e `printer*`.
- `scripts/build_android_aar.sh`: ignora o `flutter.jar` no Jetifier (evitava estouro de memória no build). Binários em `android/libs` regenerados.
- Corrigido: `PaymentHandler.onTransactionSuccess` nunca era chamado (o nativo envia `onTransactionSuccess` e o Dart esperava `onTransactionSucess`; o evento caía em "NOT IMPLEMENTED"). As duas grafias são aceitas e `PaymentTypeHandler.ON_TRANSACTION_SUCCESS.method` passa a ser `onTransactionSuccess`.
- Corrigido: `Payment` e `Nfc` disputavam o mesmo MethodChannel e o último `initNfc`/`setHandler` passava a receber todos os eventos. Depois de `initNfc`, `onFinishedResponse` (licença, transação) e os eventos de pagamento eram perdidos; depois de `payment.setHandler`, os eventos `show*` do NFC eram perdidos. Um despachante único entrega `onFinishedResponse` ao `PaymentHandler`, `show*` ao `NfcHandler` e os eventos comuns ao último handler registrado (como antes). `nfc.closeMethodCallHandler()` só desliga o NFC e `initNfc` chamado de novo troca o handler (antes ignorava o novo).
- Corrigido: argumento nulo vindo do nativo (ex.: `onError` de uma exceção sem mensagem) lançava TypeError no Dart e o callback não era chamado. Agora chega como `''`.
- Corrigido: eventos enviados de threads de background (activeDialog com terminal não ativado, mensagens de arquivo não encontrado na impressão) eram rejeitados pelo Flutter ("@UiThread"). Todo evento nativo passa a ser postado na main thread (`MainThreadEventSink`).
- Corrigido: com o terminal não ativado, o pagamento podia terminar em `onError` com a mensagem de thread errada (ou NullPointerException) em vez de `activeDialog`; agora envia `activeDialog` + `disposeDialog` e não dispara `onTransactionSuccess`.
- Corrigido: falha do SDK em `abortTransaction` derrubava o app (assinatura RxJava sem tratamento de erro); agora vira `onError`. O abort também não sobrescreve mais a assinatura da transação em andamento.
- Corrigido: `Integer` nulo do SDK (`result`, `originalAmount`, `paymentType`) causava NullPointerException ao montar a resposta de estorno/última transação.
- Corrigido: em builds release com R8 (padrão do Flutter), os campos do JSON de `onFinishedResponse`/`writeToFile`/`onTransactionInfo` eram removidos (o Gson lê por reflexão). Regras de keep adicionadas em `consumer-rules.pro`.
- Corrigido: `PagseguroResponseModel.fromMap` lia `transactionID`, mas o nativo envia `transactionId` (o id sempre vinha `null`). Aceita as duas chaves, números ou textos nos campos, e `fromMap(toMap())` volta a funcionar.
- Corrigido: `initPayment`/`setPrintDialogMaxTime` descartavam o estilo definido em `setDialogstyle` (criavam um layout novo do dialog a cada chamada; a `PagseguroPage` chama `setHandler` ao abrir).
- Corrigido: `PaymentTypeHandler.SHOW_ERROR_REFUND_NFC.method` devolvia `null`.
- Corrigido: `PagseguroPage` ficava presa em "Aguarde..." quando o nativo devolvia erro ao iniciar o pagamento/abort/ativação (Future com erro sem tratamento); agora fecha com `resultCode: 9999999`. JSON inválido em `onFinishedResponse` é ignorado. Novo `PagseguroPageParams.amountInCents` (opcional) para não depender de `double`.
- Corrigido: `PrintRenderWidget` ficava aberto se a geração da imagem falhasse; agora sempre fecha. Em debug, aguarda o frame antes de capturar (evita a assertion `!debugNeedsPaint`).
- Otimização: `getSerialNumber`, `getUserData`, `calculateInstallments`, `beep`, `rebootDevice` e `printerfromFile` rodam em background no nativo (eram chamadas bloqueantes ao PlugPagService na main thread). `beep`/`rebootDevice` passam a devolver `false` quando o SDK falha (antes a falha era engolida).
- Otimização: `EncryptedSharedPreferences`/`MasterKey` criados uma vez por instância (antes a cada leitura/gravação do cache) e uma única `CheckLicenceTech` compartilhada entre o gate e a checagem local dos pagamentos; uma única instância de `Gson`; os presenters não são mais recriados ao mudar o tempo/estilo do dialog; `onDetachedFromEngine` libera as assinaturas de todos os presenters e as threads de background.
- Refatoração (sem mudança de comportamento): `ChannelRequestHandler` (gate de licença sem Android), `PaymentGateway` (fachada do PlugPag), `EventSink`, `AsyncExecutor` e `ResultMappers`, para testar o nativo na JVM. Removido código morto: `transactions/*` (4 classes), `core/keyboardUtils`, `helpers/RetryAction` e os eventos não usados `onReadCard`/`onWriteCard`/`onAbort` do `NFCFragment`. Extensões de `payment_types.dart` passam a ter tipo (`String`/`int`).
- **Mudança visível:** `userReference` passa a conter só letras e números (exigência do SDK): o plugin remove os outros caracteres, corta em 10 e usa `SMARTPOS` se nada sobrar. Antes só cortava em 10 e caracteres especiais faziam a transação falhar no SDK.
- **Mudança visível:** `refund` (`Payment` e `Nfc`) com `transactionCode`/`transactionId` nulos ou vazios lança `PlatformException(code: 'INVALID_ARGUMENT')` sem chamar o SDK (validação no Dart e no nativo). Antes os nulos chegavam ao SDK e o erro só aparecia por `onError`. Nova constante `Payment.invalidArgumentCode`.
- Corrigido: `onAbortedSuccessfully` nunca era enviado. Agora é enviado quando o SDK confirma o abort (`PlugPagAbortResult` = `RET_OK`).
- `printerFile(nome)` não usa mais `Environment.getExternalStorageDirectory()/Download` (obsoleto, sem acesso no Android 10+): o arquivo é procurado no armazenamento do app (caminho absoluto, `getExternalFilesDir(DIRECTORY_DOWNLOADS)`, `getExternalFilesDir(null)`, `getFilesDir()`, `getCacheDir()`). **Mudança visível:** arquivos na pasta pública Download deixam de ser encontrados; grave o arquivo no diretório do app (ex.: `getExternalStorageDirectory()` do path_provider) ou passe o caminho absoluto.
- README: documenta que qualquer chamada ao plugin durante uma operação NFC cancela o resultado pendente (comportamento mantido).
- `.pubignore` adicionado (cópia do `.gitignore` + `doc/`, `local.properties`, keystores, `.DS_Store`, `*.hprof`); `android/native/local.properties` deixa de ir para o pacote.
- Testes: 160 JUnit (antes 77) cobrindo gate de licença por método, roteamento de todos os métodos (sucesso, erro e background), casos de uso e presenter de pagamento com SDK fake, eventos enviados ao Dart mappers e busca do arquivo de impressão; 123 testes Dart (antes 14) cobrindo todos os métodos públicos (argumentos, sucesso, `PlatformException` e `LICENCE_ERROR`), eventos, modelos/enums, `PagseguroPage`/`PagseguroController` e `PrintRenderWidget`. Análise completa em `doc/ANALISE.md` (fora do pacote).

## 3.2.2

- Configura tempo padrão de 30 segundos no dialog de impressão da via do cliente via `PlugPagCustomPrinterLayout.setMaxTimeShowPopup()`
- Adiciona parâmetro `maxTimeShowPopUp` em `PagseguroPaymentTech.I.initPayment()` (padrão: 30)
- Adiciona método `payment.setPrintDialogMaxTime()` para alterar o tempo em runtime
- Documentação atualizada no README com exemplos de uso

## 3.2.1

- Atualiza plugpagservice para 1.33.0

## 3.1.1

- Adiciona método `calculateInstallments` para calcular valores de parcelas
- Cria modelo `PlugPagInstallment` com campos `quantity`, `amount` e `total`
- Adiciona enum `InstallmentType` com valores `A_VISTA`, `PARC_VENDEDOR` e `PARC_COMPRADOR`
- Implementa integração nativa com SDK PagSeguro para cálculo de parcelas
- Método disponível em `PagseguroPaymentTech.I.payment.calculateInstallments(saleValue, installmentType)`
- Atualiza Gradle de 8.12 para 8.13
- Atualiza Android Gradle Plugin (AGP) de 8.12.1 para 8.13.1

## 3.0.2

- Atualiza Gradle de 8.12 para 8.13
- Atualiza Android Gradle Plugin (AGP) de 8.12.1 para 8.13.1

## 3.0.1

- Atualiza classe de style para pagseguroPage

## 3.0.0

- Encapsula toda a implementação Android em um módulo nativo (AAR privado).
- Novo script `scripts/build_android_aar.sh` para gerar os binários (`.aar` + `<plugin>-native.jar`).
- `initPayment` volta a exigir `licenceKey` e `licenceInternalKey` informadas em tempo de execução (não usar mais `local.properties`).
- Documentação atualizada com o novo fluxo e orientações de uso das chaves.

## 2.1.0

- Update plugin to use gradle 8.12 (deprecated, use 8.13+)
- Update Wrapper to version 1.30.52

## 2.0.1

- Configure PagseguroPage to automatize payment flow

## 2.0.0+2

    - Configure new methods setDialogStyle and getUserdata

⚠️ Attention: In this version, there is an important change in the way payments are initialized.

🛠️ What changed:

Breaking Change: The initPayment method no longer requires the licenceKey to be passed as a parameter.

It is now mandatory to define the following fields in local.properties:

licenceKey → main licence key

licenceInternalKey → internal licence key

This means that any code that previously passed licenceKey directly to initPayment will need to be updated.

## 1.1.2

- Add method wipe licence local database

## 1.1.1

- Add method wipe licence local database

## 1.1.0+3

- Replace instance() to I

## 1.1.0+2

- Update checkLicence version

## 1.1.0+1

- Add method setHandler to NFC

## 1.1.0

- Fix checkLicence
- Update Wrapper to version 1.30.51

## 1.0.6

- First release
- Update dart sdk

## 1.0.5

- First release
- Update dart sdk

## 1.0.2

- First release

## 1.0.1

- First release

## 1.0.0

- Primeiro release do Plugin.
