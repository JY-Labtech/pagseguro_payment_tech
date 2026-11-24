package br.com.jylabtech.pagseguro_payment_tech.licence;


import android.content.Context;

import java.util.Date;


/**
 * Fluxo de validação de licença conforme o contrato de licenças do jylabtech_core:
 *
 * <ul>
 *   <li>A primeira validação de cada processo (abertura do app) sempre consulta o servidor.</li>
 *   <li>Depois disso, com cache válido, usa o cache e consulta o servidor de novo a cada
 *       {@link LicenceConfig#REVALIDATE_INTERVAL_MS} (4h).</li>
 *   <li>Recusa definitiva do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não
 *       encontrada, aparelho desativado...) apaga o cache e bloqueia na hora.</li>
 *   <li>Falha de rede, timeout, 408/429/5xx ou corpo ilegível usa o cache válido (salvo há no
 *       máximo 72h); sem cache, bloqueia. Após uma falha, espera
 *       {@link LicenceConfig#RETRY_AFTER_FAILURE_MS} para tentar de novo, para não travar cada
 *       operação offline no timeout.</li>
 * </ul>
 *
 * Não chama nada de UI: {@link #validateLicence} pode (e deve) ser executado fora da main thread.
 */
public class CheckLicenceTech {

    /** Relógio injetável para testes. */
    public interface Clock {
        Date now();
    }

    private final LicenceStorage storage;
    private final LicenceRemote remote;
    private final Clock clock;

    // Estado deste processo (não persistido): reiniciar o app força nova consulta ao servidor.
    // volatile: validateExistsLicence lê sem lock (é chamado na main thread).
    private volatile boolean checkedOnlineInThisProcess;
    private volatile Date lastOnlineFailureAt;

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
     * Valida a licença (servidor ou cache, conforme as regras acima). Salva o cache novo somente
     * se a licença estiver válida e ativa. Faz acesso à rede: não chame na main thread.
     */
    public synchronized LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        if (licenceKey == null || licenceKey.trim().isEmpty()) {
            return LicenceResult.error("Licença não configurada. Informe a licenceKey em initPayment.");
        }
        String key = licenceKey.trim();
        String url = LicenceRules.resolveLicenceUrl(licenceInternalKey);
        Date now = clock.now();

        CheckLicenceEntity cached = readCacheSafely();
        if (cached != null && !LicenceRules.isCacheUsable(cached, key, url, now)) {
            deleteCacheSafely();
            cached = null;
        }
        if (cached != null && !LicenceRules.shouldRevalidate(cached, checkedOnlineInThisProcess, now)) {
            return LicenceResult.success(cached);
        }
        if (cached != null && recentlyFailedOnline(now)) {
            return LicenceResult.success(cached);
        }

        LicenceResult remoteResult;
        try {
            remoteResult = remote.check(url, key);
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return onlineFailure(cached, now, "Não foi possível validar a licença: " + detail);
        }

        if (remoteResult == null) {
            return onlineFailure(cached, now, "Não foi possível validar a licença.");
        }
        if (!remoteResult.isSuccess()) {
            if (remoteResult.getErrorMessage() == null || remoteResult.getErrorMessage().isEmpty()) {
                remoteResult.setErrorMessage("Licença não ativada.");
            }
            if (remoteResult.isRejectedByServer()) {
                return rejected(remoteResult);
            }
            return onlineFailure(cached, now, remoteResult.getErrorMessage());
        }

        CheckLicenceEntity entity = remoteResult.getLicenceEntity();
        if (!LicenceRules.isLicenceUsable(entity, now)) {
            // HTTP 200 com licença lida mas inutilizável: recusa definitiva.
            return rejected(LicenceResult.rejected(LicenceRules.describeUnusable(entity, now)));
        }

        entity.setLicenceInternalKey(url);
        entity.setSavedLocalDatabaseAt(now);
        try {
            storage.save(entity);
        } catch (Exception ignored) {
            // Falha ao salvar o cache não invalida a licença recém-validada.
        }
        checkedOnlineInThisProcess = true;
        lastOnlineFailureAt = null;
        return LicenceResult.success(entity);
    }

    /**
     * Confere apenas o cache local (sem rede). Usado como segunda barreira nas operações, na main
     * thread. Só devolve true quando {@link #validateLicence} também liberaria sem consultar o
     * servidor agora: cache válido E (já validado online neste processo há menos de 4h OU servidor
     * fora do ar há menos de 5 min). Cache inválido é apagado.
     * Não usa lock: não pode bloquear a main thread enquanto {@link #validateLicence} faz rede.
     */
    public boolean validateExistsLicence() {
        CheckLicenceEntity cached = readCacheSafely();
        if (cached == null) {
            return false;
        }
        Date now = clock.now();
        if (!LicenceRules.isCacheFresh(cached, now)) {
            deleteCacheSafely();
            return false;
        }
        return !LicenceRules.shouldRevalidate(cached, checkedOnlineInThisProcess, now)
                || recentlyFailedOnline(now);
    }

    private boolean recentlyFailedOnline(Date now) {
        Date failedAt = lastOnlineFailureAt;
        if (failedAt == null) {
            return false;
        }
        long elapsed = now.getTime() - failedAt.getTime();
        return elapsed >= 0 && elapsed < LicenceConfig.RETRY_AFTER_FAILURE_MS;
    }

    /** Servidor inacessível: usa o cache válido (até 72h) ou bloqueia se não houver. */
    private LicenceResult onlineFailure(CheckLicenceEntity cached, Date now, String message) {
        lastOnlineFailureAt = now;
        if (cached != null) {
            return LicenceResult.success(cached);
        }
        return LicenceResult.error(message);
    }

    /** Recusa definitiva: o cache não pode mais liberar o uso. */
    private LicenceResult rejected(LicenceResult result) {
        deleteCacheSafely();
        checkedOnlineInThisProcess = true;
        lastOnlineFailureAt = null;
        result.setRejectedByServer(true);
        return result;
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
