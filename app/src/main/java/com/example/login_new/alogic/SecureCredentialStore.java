package com.example.login_new.alogic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * 자동 로그인용 비밀번호를 Android Keystore 기반 암호화 저장소에 보관한다.
 * (기존에는 평문 SharedPreferences "loginPrefs"에 저장했다.)
 * 저장소를 열 수 없으면 비밀번호를 저장하지 않고, 자동 로그인은 동작하지 않는다.
 */
final class SecureCredentialStore {

    private static final String TAG = "SecureCredentialStore";
    private static final String FILE_NAME = "secure_login_prefs";
    private static final String KEY_PASSWORD = "UserPassword";

    private SecureCredentialStore() {
    }

    @Nullable
    private static SharedPreferences open(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "암호화 저장소를 열 수 없습니다.", e);
            return null;
        }
    }

    @Nullable
    static String getPassword(Context context) {
        SharedPreferences prefs = open(context);
        return prefs == null ? null : prefs.getString(KEY_PASSWORD, null);
    }

    static void putPassword(Context context, String password) {
        SharedPreferences prefs = open(context);
        if (prefs != null) {
            prefs.edit().putString(KEY_PASSWORD, password).apply();
        }
    }

    static void removePassword(Context context) {
        SharedPreferences prefs = open(context);
        if (prefs != null) {
            prefs.edit().remove(KEY_PASSWORD).apply();
        }
    }

    /**
     * 이전 버전이 평문으로 저장한 비밀번호가 있으면 암호화 저장소로 옮기고 평문 값을 지운다.
     */
    static void migrateLegacyPassword(Context context, SharedPreferences legacyPrefs, String legacyKey) {
        String legacy = legacyPrefs.getString(legacyKey, null);
        if (legacy == null) {
            return;
        }
        if (open(context) != null) {
            putPassword(context, legacy);
        }
        legacyPrefs.edit().remove(legacyKey).apply();
    }
}
