package br.com.jylabtech.pagseguro_payment_tech.licence;

/**
 * Armazenamento local (cache offline) da última licença validada.
 */
public interface LicenceStorage {
    CheckLicenceEntity read();

    void save(CheckLicenceEntity entity);

    void delete();
}
