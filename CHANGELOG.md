## 2.0.0+1

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
