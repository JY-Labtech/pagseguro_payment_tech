import 'package:flutter/material.dart';

class PagseguroStyle {
  final Color backgroundColor;
  final Color textColor;
  final Color buttonColor;
  final Color buttonTextColor;
  final IconData? icon;
  final Color iconColor;

  const PagseguroStyle({
    this.backgroundColor = const Color.fromARGB(255, 55, 169, 100),
    this.textColor = Colors.white,
    this.buttonColor = const Color.fromARGB(255, 51, 128, 82),
    this.buttonTextColor = Colors.white,
    this.icon,
    this.iconColor = Colors.white,
  });
}
