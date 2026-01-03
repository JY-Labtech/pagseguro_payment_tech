import 'package:flutter/material.dart';
import 'package:pagseguro_payment_tech/src/payments/utils/payment_types.dart';

import '../presentation.dart';

class PagseguroOptions {
  final String activationCode;
  final bool isPrinterEstablishment;

  PagseguroOptions({required this.activationCode, required this.isPrinterEstablishment});
}

class PagseguroPageParams {
  final double amount;
  final int? parcels;
  final PaymentTypeCredit? creditType;
  final PagseguroPaymentType type;
  final PagseguroOptions options;
  final PagseguroStyle? style;

  PagseguroPageParams({
    required this.amount,
    this.parcels,
    this.creditType,
    required this.type,
    required this.options,
    this.style,
  });
}

class PagseguroPage extends StatefulWidget {
  const PagseguroPage({
    Key? key,
    required this.params,
  }) : super(key: key);
  final PagseguroPageParams params;

  @override
  State<PagseguroPage> createState() => _PagseguroPageState();
}

class _PagseguroPageState extends State<PagseguroPage> {
  late PagseguroController _controller;
  late PagseguroStyle _style;
  PagseguroHandler? _pagseguroHandler;
  String message = '';

  @override
  void initState() {
    _controller = PagseguroController();
    _style = widget.params.style ?? const PagseguroStyle();

    message = 'Iniciando fluxo de pagamento.\nAguarde...';

    _pagseguroHandler = PagseguroHandler(
      onMessageMonitor: (value) => setMessageFlow(value),
      onTransaction: (method, transaction) => setOnFinishedResponse(method: method, message: transaction),
    );

    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _controller.init(
        handler: _pagseguroHandler,
        params: widget.params,
        options: widget.params.options,
      ),
    );
    super.initState();
  }

  void setMessageFlow(String value) async {
    _controller.onMessageMonitor(value);
    message = value;
    _set();
  }

  void setOnFinishedResponse({
    required String method,
    required String message,
  }) async {
    _controller.onTransactionMonitor(
      method: method,
      message: message,
      onPopResult: (value) => _popWithResult(value),
    );
    await Future.delayed(const Duration(milliseconds: 500));
    _set();
  }

  void _set() {
    if (mounted) {
      setState(() {});
    }
  }

  void _popWithResult(PagseguroResponseModel? result) {
    if (mounted) {
      Navigator.of(context).pop(result);
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (_, __) {},
      child: Scaffold(
        backgroundColor: _style.backgroundColor,
        body: SizedBox(
          child: Column(
            children: [
              Expanded(
                child: Container(
                  height: size.height,
                  width: double.infinity,
                  color: _style.backgroundColor,
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Center(
                        child: Text(
                          message,
                          style: TextStyle(fontSize: 20, color: _style.textColor),
                          textAlign: TextAlign.center,
                        ),
                      ),
                      Center(
                        child: Icon(
                          _style.icon ?? Icons.credit_card,
                          size: 80,
                          color: _style.iconColor,
                        ),
                      ),
                      Center(
                          child: CircularProgressIndicator(
                              valueColor: AlwaysStoppedAnimation<Color>(
                        _style.iconColor,
                      ))),
                      Center(
                        child: Padding(
                          padding: const EdgeInsets.only(top: 20),
                          child: Text(
                            'Pagseguro\nAguarde...',
                            style: TextStyle(fontSize: 16, color: _style.buttonTextColor),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              Visibility(
                visible: _controller.isVisibilityCancelButton,
                child: Container(
                  padding: const EdgeInsets.all(16),
                  color: _style.backgroundColor,
                  child: ElevatedButton(
                    onPressed: () {
                      _controller.onAbortCurrentTransaction();
                    },
                    child: Padding(
                      padding: const EdgeInsetsGeometry.symmetric(horizontal: 20),
                      child: Text('Cancelar Pagamento', style: TextStyle(color: _style.buttonTextColor)),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _style.buttonColor,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
