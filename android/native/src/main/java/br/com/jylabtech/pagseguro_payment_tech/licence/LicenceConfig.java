package br.com.jylabtech.pagseguro_payment_tech.licence;

/**
 * Configuração fixa da validação de licença.
 * A URL de validação é travada no servidor oficial da JY Labtech (jylabtech_core).
 */
public final class LicenceConfig {

    private LicenceConfig() {}

    /** Endpoint oficial de validação de licença (jylabtech_core). */
    public static final String LICENCE_CHECK_URL = "https://portal-api.jylabtech.com.br/pos-payments/licence/check";

    /** Único host aceito para validação de licença. */
    public static final String LICENCE_HOST = "portal-api.jylabtech.com.br";

    /** Prefixo de path aceito (inclui as variantes /invoice e /subscription). */
    public static final String LICENCE_PATH_PREFIX = "/pos-payments/licence/check";

    /** Timeout de conexão com o servidor de licença. */
    public static final int CONNECT_TIMEOUT_MS = 15000;

    /** Timeout de leitura da resposta do servidor de licença. */
    public static final int READ_TIMEOUT_MS = 15000;

    /** Validade máxima do cache offline da licença. */
    public static final long CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;

    /** Tolerância para relógio do aparelho adiantado/atrasado em relação ao cache. */
    public static final long CLOCK_SKEW_TOLERANCE_MS = 5L * 60L * 1000L;

    /** Código de erro devolvido ao Flutter quando a licença é inválida. */
    public static final String LICENCE_ERROR_CODE = "LICENCE_ERROR";

    /** Código de resultado de sucesso. */
    public static final int RESULT_SUCCESS = 0;

    /** Código de resultado de erro. */
    public static final int RESULT_ERROR = 999999;
}
