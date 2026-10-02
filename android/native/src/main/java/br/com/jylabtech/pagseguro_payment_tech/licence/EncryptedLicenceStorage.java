package br.com.jylabtech.pagseguro_payment_tech.licence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Cache offline da licença em EncryptedSharedPreferences.
 * Mantém o mesmo nome de prefs e chave das versões anteriores.
 */
public class EncryptedLicenceStorage implements LicenceStorage {

    static final String PREFS_NAME = "licence_prefs_encrypted";
    static final String KEY_JSON = "licence_entity";

    private final Context context;
    /**
     * Criar MasterKey + EncryptedSharedPreferences custa caro (Keystore + Tink); antes isso era
     * refeito a cada leitura/gravação. Agora é criado uma vez por instância, sob demanda.
     */
    private SharedPreferences cachedPrefs;

    public EncryptedLicenceStorage(Context context) {
        this.context = context;
    }

    private synchronized SharedPreferences prefs() throws Exception {
        if (cachedPrefs == null) {
            cachedPrefs = createPrefs();
        }
        return cachedPrefs;
    }

    private SharedPreferences createPrefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    @Override
    public CheckLicenceEntity read() {
        try {
            String savedJson = prefs().getString(KEY_JSON, null);
            if (savedJson != null) {
                return CheckLicenceEntity.fromJson(savedJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void save(CheckLicenceEntity entity) {
        try {
            String json = entity.toJson();
            if (json != null) {
                prefs().edit().putString(KEY_JSON, json).apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete() {
        try {
            prefs().edit().remove(KEY_JSON).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
