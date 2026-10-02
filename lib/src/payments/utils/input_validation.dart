import 'package:flutter/services.dart';
import 'package:pagseguro_payment_tech/src/licence/constants/licence_constants.dart' show USER_REFERENCE;

/// Código do erro para argumento obrigatório ausente/vazio (o nativo usa o mesmo código).
const String invalidArgumentCode = 'INVALID_ARGUMENT';

final RegExp _notAlphanumeric = RegExp(r'[^A-Za-z0-9]');

/// O SDK PlugPag aceita em `userReference` só letras e números, até 10 caracteres.
/// Remove o resto e corta em 10. Se nada sobrar, usa o padrão [USER_REFERENCE] ("SMARTPOS").
String sanitizeUserReference(String value) {
  final cleaned = value.replaceAll(_notAlphanumeric, '');
  if (cleaned.isEmpty) return USER_REFERENCE;
  return cleaned.length <= 10 ? cleaned : cleaned.substring(0, 10);
}

/// Estorno exige código e id da transação. Lança `PlatformException(INVALID_ARGUMENT)` sem
/// chamar o nativo (antes os nulos chegavam ao SDK e o erro só aparecia por onError).
void checkRefundArguments(String? transactionCode, String? transactionId) {
  if (transactionCode == null || transactionCode.trim().isEmpty || transactionId == null || transactionId.trim().isEmpty) {
    throw PlatformException(
      code: invalidArgumentCode,
      message: 'transactionCode e transactionId são obrigatórios para o estorno.',
    );
  }
}
