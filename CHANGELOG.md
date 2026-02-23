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
