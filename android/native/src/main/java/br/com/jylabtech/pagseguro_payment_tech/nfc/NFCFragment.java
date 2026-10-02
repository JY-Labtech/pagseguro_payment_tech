package br.com.jylabtech.pagseguro_payment_tech.nfc;

import java.util.ArrayList;
import java.util.List;
import io.flutter.plugin.common.MethodChannel;
import br.com.jylabtech.pagseguro_payment_tech.core.EventSink;
import br.com.jylabtech.pagseguro_payment_tech.core.MainThreadEventSink;
import br.com.jylabtech.pagseguro_payment_tech.helpers.Utils;
import br.com.jylabtech.pagseguro_payment_tech.user.UserData;


/**
 * Eventos NFC enviados ao {@code NfcHandler} do Dart, sempre pela main thread.
 */
public class NFCFragment implements NFCContract {

    private final EventSink sink;

    public NFCFragment(MethodChannel channel) {
        this(new MainThreadEventSink(channel));
    }

    public NFCFragment(EventSink sink) {
        this.sink = sink;
    }

    //METHODS SUCCESS

    static final String ON_SUCCESS = "showSuccess";
    static final String ON_SUCCESS_WRITE = "showSuccessWrite";
    static final String ON_SUCCESS_RE_WRITE = "showSuccessReWrite";
    static final String ON_SUCCESS_REFUND_NFC = "showSuccessRefundNfc";
    static final String ON_SUCCESS_FORMAT = "showSuccessFormat";
    static final String ON_SUCCESS_DEBIT_NFC = "showSuccessDebitNfc";

    //METHODS ERROR
    static final String ON_ERROR = "onError";
    static final String ON_ERROR_READ = "showErrorRead";
    static final String ON_ERROR_WRITE = "showErrorWrite";
    static final String ON_ERROR_RE_WRITE = "showErrorReWrite";
    static final String ON_ERROR_FORMAT = "showErrorFormat";
    static final String ON_ERROR_DEBIT_NFC = "showErrorDebitNfc";
    static final String ON_ERROR_REFUND_NFC = "showErrorRefundNfc";



    //Success methods
    @Override
    public void showSuccess(UserData result) {

        final List<String> results = new ArrayList<>();
        results.add(Utils.removeAsterisco(result.getValue()));
        results.add(Utils.removeAsterisco(result.getName()));
        results.add(Utils.removeAsterisco(result.getCpf()));
        results.add(Utils.removeAsterisco(result.getNumberTag()));
        results.add(Utils.removeAsterisco(result.getCellPhone()));
        results.add(Utils.removeAsterisco(result.getActive()));
        results.add(Utils.removeAsterisco(result.getOpenValue()));

        sink.send(ON_SUCCESS, results);
    }

    @Override
    public void showSuccessWrite(Object result) {
        sink.send(ON_SUCCESS_WRITE, result);
    }

    @Override
    public void showSuccessReWrite(Object result) {
        sink.send(ON_SUCCESS_RE_WRITE, result);
    }

    @Override
    public void showSuccessFormat(Object result) {
        sink.send(ON_SUCCESS_FORMAT, result);
    }

    @Override
    public void showSuccessDebitNfc(Object result) {
        sink.send(ON_SUCCESS_DEBIT_NFC, result);
    }

    @Override
    public void showSuccessRefundNfc(Object result) {
        sink.send(ON_SUCCESS_REFUND_NFC, result);
    }

    //Error methods
    @Override
    public void showErrorRead(String message) {
        sink.send(ON_ERROR_READ, message);

    }

    @Override
    public void showErrorWrite(String message) {
        sink.send(ON_ERROR_WRITE, message);
    }

    @Override
    public void showErrorReWrite(String message) {
        sink.send(ON_ERROR_RE_WRITE, message);
    }

    @Override
    public void showErrorRefundNfc(String message) {
        sink.send(ON_ERROR_REFUND_NFC, message);
    }

    @Override
    public void showErrorFormat(String message) {
        sink.send(ON_ERROR_FORMAT, message);
    }

    @Override
    public void showErrorDebitNfc(String message) {
        sink.send(ON_ERROR_DEBIT_NFC, message);
    }

    @Override
    public void showError(String message) {
        sink.send(ON_ERROR,message);

    }



}