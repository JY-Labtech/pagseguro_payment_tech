# Exemplo `pagseguro_payment_tech`

Aplicativo demonstrativo que exercita pagamentos, NFC e impressão usando o plugin.

## Como testar

1. Garanta que os binários nativos (`android/libs/pagseguro_payment_tech-release.aar` e `pagseguro_payment_tech-native.jar`) estejam atualizados.
2. Conecte um Smart POS com o **PlugPagService** instalado.
3. Rode o app definindo as chaves de licença via `--dart-define`:
   ```bash
   flutter run \
     --dart-define=PAGSEGURO_LICENCE_KEY=SUA_LICENCE_KEY \
     --dart-define=PAGSEGURO_LICENCE_INTERNAL_KEY=aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=
   ```
   A licença é adquirida em https://portal.jylabtech.com.br (o plano `debug`, com 2 aparelhos, é concedido como cortesia no cadastro). Se `PAGSEGURO_LICENCE_INTERNAL_KEY` não for informado, o exemplo usa o novo endereço de validação por padrão.

## Código relevante

- `lib/payment/payment_page.dart`: fluxo principal de pagamento e impressora.
- `lib/payment/payment_controller.dart`: implementação de `PaymentHandler`.
- `lib/payment/nfc_page.dart`: operações NFC (ler, gravar, debitar, estornar).

> **Dica:** para executar testes automatizados ou pipelines, injete as chaves através de variáveis de ambiente seguras e repasse-as para o comando `flutter run`/`flutter test` via `--dart-define`.

