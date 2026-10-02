package br.com.jylabtech.pagseguro_payment_tech.licence;

/**
 * Cliente de rede que consulta o servidor de licença.
 * Deve ser chamado fora da main thread.
 */
public interface LicenceRemote {
    LicenceResult check(String url, String licenceKey) throws Exception;
}
