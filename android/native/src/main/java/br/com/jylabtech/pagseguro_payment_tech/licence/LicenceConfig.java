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

    /**
     * Validade máxima do cache offline sem nova consulta bem-sucedida ao servidor (contrato de
     * licenças do jylabtech_core: no máximo 72h; igual ao TTL do token assinado).
     */
    public static final long CACHE_MAX_AGE_MS = 72L * 60L * 60L * 1000L;

    /** Com cache válido, consulta o servidor de novo depois deste intervalo (em uso). */
    public static final long REVALIDATE_INTERVAL_MS = 4L * 60L * 60L * 1000L;

    /** Depois de uma falha de rede, espera este tempo antes de tentar o servidor de novo (usa o cache). */
    public static final long RETRY_AFTER_FAILURE_MS = 5L * 60L * 1000L;

    /** Motivo do core para cliente bloqueado/banido (bloqueio total imediato). */
    public static final String REASON_CUSTOMER_BLOCKED = "customer_blocked";

    /** Mensagem exibida quando o core informa {@link #REASON_CUSTOMER_BLOCKED}. */
    public static final String CUSTOMER_BLOCKED_MESSAGE = "Cliente bloqueado. Entre em contato com a JY Labtech.";

    /** Tolerância para relógio do aparelho adiantado/atrasado em relação ao cache. */
    public static final long CLOCK_SKEW_TOLERANCE_MS = 5L * 60L * 1000L;

    /** Código de erro devolvido ao Flutter quando a licença é inválida. */
    public static final String LICENCE_ERROR_CODE = "LICENCE_ERROR";

    /** Código de resultado de sucesso. */
    public static final int RESULT_SUCCESS = 0;

    /** Código de resultado de erro. */
    public static final int RESULT_ERROR = 999999;
}
