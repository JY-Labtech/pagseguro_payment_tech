package br.com.jylabtech.pagseguro_payment_tech.licence;


import android.content.Context;

import java.util.Date;


/**
 * Fluxo de validação de licença: cache offline válido primeiro, depois o servidor oficial.
 * Não chama nada de UI: pode (e deve) ser executado fora da main thread.
 */
public class CheckLicenceTech {

    /** Relógio injetável para testes. */
    public interface Clock {
        Date now();
    }

    private final LicenceStorage storage;
    private final LicenceRemote remote;
    private final Clock clock;

    public CheckLicenceTech(Context context) {
        this(new EncryptedLicenceStorage(context), new LicenceProvider(context), new Clock() {
            @Override
            public Date now() {
                return new Date();
            }
        });
    }

    public CheckLicenceTech(LicenceStorage storage, LicenceRemote remote, Clock clock) {
        this.storage = storage;
        this.remote = remote;
        this.clock = clock;
    }

    /**
     * Valida a licença: usa o cache se ainda for válido; senão consulta o servidor
     * e salva o cache novo somente se a licença estiver válida e ativa.
     */
    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        if (licenceKey == null || licenceKey.trim().isEmpty()) {
            return LicenceResult.error("Licença não configurada. Informe a licenceKey em initPayment.");
        }
        String key = licenceKey.trim();
        String url = LicenceRules.resolveLicenceUrl(licenceInternalKey);
        Date now = clock.now();

        CheckLicenceEntity cached = readCacheSafely();
        if (cached != null) {
            if (LicenceRules.isCacheUsable(cached, key, url, now)) {
                return LicenceResult.success(cached);
            }
            deleteCacheSafely();
        }

        LicenceResult remoteResult;
        try {
            remoteResult = remote.check(url, key);
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return LicenceResult.error("Não foi possível validar a licença: " + detail);
        }

        if (remoteResult == null) {
            return LicenceResult.error("Não foi possível validar a licença.");
        }
        if (!remoteResult.isSuccess()) {
            if (remoteResult.getErrorMessage() == null || remoteResult.getErrorMessage().isEmpty()) {
                remoteResult.setErrorMessage("Licença não ativada.");
            }
            return remoteResult;
        }

        CheckLicenceEntity entity = remoteResult.getLicenceEntity();
        if (!LicenceRules.isLicenceUsable(entity, now)) {
            return LicenceResult.error(LicenceRules.describeUnusable(entity, now));
        }

        entity.setLicenceInternalKey(url);
        entity.setSavedLocalDatabaseAt(now);
        try {
            storage.save(entity);
        } catch (Exception ignored) {
            // Falha ao salvar o cache não invalida a licença recém-validada.
        }
        return LicenceResult.success(entity);
    }

    /**
     * Confere apenas o cache local (sem rede). Usado como segunda barreira nas operações.
     */
    public boolean validateExistsLicence() {
        CheckLicenceEntity cached = readCacheSafely();
        if (cached == null) {
            return false;
        }
        if (LicenceRules.isCacheFresh(cached, clock.now())) {
            return true;
        }
        deleteCacheSafely();
        return false;
    }

    private CheckLicenceEntity readCacheSafely() {
        try {
            return storage.read();
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteCacheSafely() {
        try {
            storage.delete();
        } catch (Exception ignored) {
        }
    }
}
