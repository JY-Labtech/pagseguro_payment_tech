// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

class PagseguroResponseModel {
  final String? cardBin;
  final String? cardBrand;
  final int? resultCode;
  final String? errorCode;
  final String? amount;
  final String? hostNSU;
  final String? nsu;
  final String? transactionID;
  final String? transactionCode;
  final String? date;
  final String? time;
  final String? terminalSerialNumber;
  final String? message;
  final String? errorMessage;
  final int? paymentType;

  PagseguroResponseModel({
    this.cardBin,
    this.cardBrand,
    this.resultCode,
    this.errorCode,
    this.amount,
    this.hostNSU,
    this.nsu,
    this.transactionID,
    this.transactionCode,
    this.date,
    this.time,
    this.terminalSerialNumber,
    this.message,
    this.errorMessage,
    this.paymentType,
  });

  bool get isTransactionValid {
    if (paymentType == 5) {
      return (resultCode == 0 && transactionCode != '');
    }
    return (resultCode == 0 && hostNSU != '' && nsu != '');
  }

  PagseguroResponseModel copyWith({
    String? cardBin,
    String? cardBrand,
    int? resultCode,
    String? errorCode,
    String? amount,
    String? hostNSU,
    String? nsu,
    String? transactionID,
    String? transactionCode,
    String? date,
    String? time,
    String? terminalSerialNumber,
    String? message,
    String? errorMessage,
    int? paymentType,
  }) {
    return PagseguroResponseModel(
      cardBin: cardBin ?? this.cardBin,
      cardBrand: cardBrand ?? this.cardBrand,
      resultCode: resultCode ?? this.resultCode,
      errorCode: errorCode ?? this.errorCode,
      amount: amount ?? this.amount,
      hostNSU: hostNSU ?? this.hostNSU,
      nsu: nsu ?? this.nsu,
      transactionID: transactionID ?? this.transactionID,
      transactionCode: transactionCode ?? this.transactionCode,
      date: date ?? this.date,
      time: time ?? this.time,
      terminalSerialNumber: terminalSerialNumber ?? this.terminalSerialNumber,
      message: message ?? this.message,
      errorMessage: errorMessage ?? this.errorMessage,
      paymentType: paymentType ?? this.paymentType,
    );
  }

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'cardBin': cardBin,
      'cardBrand': cardBrand,
      'resultCode': resultCode,
      'errorCode': errorCode,
      'amount': amount,
      'hostNSU': hostNSU,
      'nsu': nsu,
      'transactionID': transactionID,
      'transactionCode': transactionCode,
      'date': date,
      'time': time,
      'terminalSerialNumber': terminalSerialNumber,
      'message': message,
      'errorMessage': errorMessage,
      'paymentType': paymentType,
    };
  }

  /// Lê o JSON enviado pelo nativo em `onFinishedResponse` (ActionResult do plugin).
  ///
  /// Tolerante a tipos: números ou textos nos campos são aceitos (ex.: `amount` como `2590` ou
  /// `"2590"`), e campos ausentes/nulos ficam `null`. O nativo envia `transactionId`; a chave
  /// antiga `transactionID` continua aceita (antes só ela era lida e o id sempre vinha `null`).
  factory PagseguroResponseModel.fromMap(Map<String, dynamic> map) {
    return PagseguroResponseModel(
      cardBin: _asString(map['bin'] ?? map['cardBin']),
      cardBrand: _asString(map['cardBrand']),
      resultCode: _asInt(map['result'] ?? map['resultCode']),
      errorCode: _asString(map['errorCode']),
      amount: _asString(map['amount']),
      hostNSU: _asString(map['hostNsu'] ?? map['hostNSU']),
      nsu: _asString(map['nsu']),
      transactionID: _asString(map['transactionId'] ?? map['transactionID']),
      transactionCode: _asString(map['transactionCode']),
      date: _asString(map['date']),
      time: _asString(map['time']),
      terminalSerialNumber: _asString(map['terminalSerialNumber']),
      message: _asString(map['message']),
      errorMessage: _asString(map['errorMessage']),
      paymentType: _asInt(map['paymentType']),
    );
  }

  static String? _asString(dynamic value) => value?.toString();

  static int? _asInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    return int.tryParse(value.toString());
  }

  String toJson() => json.encode(toMap());

  factory PagseguroResponseModel.fromJson(String source) => PagseguroResponseModel.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() {
    return 'PagseguroResponseModel(cardBin: $cardBin, cardBrand: $cardBrand, resultCode: $resultCode, errorCode: $errorCode, amount: $amount, hostNSU: $hostNSU, nsu: $nsu, transactionID: $transactionID, transactionCode: $transactionCode, date: $date, time: $time, terminalSerialNumber: $terminalSerialNumber, message: $message, errorMessage: $errorMessage, paymentType: $paymentType)';
  }

  @override
  bool operator ==(covariant PagseguroResponseModel other) {
    if (identical(this, other)) return true;

    return other.cardBin == cardBin && other.cardBrand == cardBrand && other.resultCode == resultCode && other.errorCode == errorCode && other.amount == amount && other.hostNSU == hostNSU && other.nsu == nsu && other.transactionID == transactionID && other.transactionCode == transactionCode && other.date == date && other.time == time && other.terminalSerialNumber == terminalSerialNumber && other.message == message && other.errorMessage == errorMessage && other.paymentType == paymentType;
  }

  @override
  int get hashCode {
    return cardBin.hashCode ^ cardBrand.hashCode ^ resultCode.hashCode ^ errorCode.hashCode ^ amount.hashCode ^ hostNSU.hashCode ^ nsu.hashCode ^ transactionID.hashCode ^ transactionCode.hashCode ^ date.hashCode ^ time.hashCode ^ terminalSerialNumber.hashCode ^ message.hashCode ^ errorMessage.hashCode ^ paymentType.hashCode;
  }
}
