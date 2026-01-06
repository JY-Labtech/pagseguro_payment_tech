// ignore_for_file: public_member_api_docs, sort_constructors_first

class PlugPagInstallment {
  final int quantity;
  final int amount;
  final int total;

  PlugPagInstallment({
    required this.quantity,
    required this.amount,
    required this.total,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'quantity': quantity,
      'amount': amount,
      'total': total,
    };
  }

  factory PlugPagInstallment.fromMap(Map<String, dynamic> map) {
    return PlugPagInstallment(
      quantity: map['quantity'] as int? ?? 0,
      amount: map['amount'] as int? ?? 0,
      total: map['total'] as int? ?? 0,
    );
  }

  @override
  String toString() {
    return 'PlugPagInstallment(quantity: $quantity, amount: $amount, total: $total)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PlugPagInstallment && other.quantity == quantity && other.amount == amount && other.total == total;
  }

  @override
  int get hashCode {
    return quantity.hashCode ^ amount.hashCode ^ total.hashCode;
  }
}
