# Exemplo `pagseguro_payment_tech`

Aplicativo demonstrativo que exercita pagamentos (débito, crédito à vista e parcelado, voucher e
PIX), ativação do terminal, cancelamento, última transação, estorno e impressão usando o plugin.

## Como rodar

1. Conecte uma maquininha PagSeguro Smart com o **PlugPagService** instalado e atualizado.
2. Na raiz do plugin, garanta que os binários nativos (`android/libs/pagseguro_payment_tech-release.aar`
   e `pagseguro_payment_tech-native.jar`) estejam atualizados (`sh scripts/build_android_aar.sh`
   depois de mudar `android/native`).
3. Rode:

   ```bash
   cd example
   flutter pub get
   flutter run
   ```

Não há chave, licença nem `--dart-define` para configurar.

## Código relevante

- `lib/payment/payment_page.dart`: inicialização (`PagseguroPaymentTech.I.initPayment`), botões de
  pagamento, cancelamento, última transação, estorno e impressão.
- `lib/payment/payment_controller.dart`: implementação de `PaymentHandler` (recebe os eventos do SDK).
