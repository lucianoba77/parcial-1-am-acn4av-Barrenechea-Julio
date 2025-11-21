package com.controlmedicamentos.myapplication.services;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;

/**
 * Servicio para manejar la autenticación con Firebase
 */
public class AuthService {
    private static final String TAG = "AuthService";
    private FirebaseAuth mAuth;

    public AuthService() {
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Obtiene el usuario actual autenticado
     * @return FirebaseUser si hay usuario autenticado, null en caso contrario
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Verifica si hay un usuario autenticado
     * @return true si hay usuario autenticado, false en caso contrario
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Registra un nuevo usuario con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Usuario registrado exitosamente");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.e(TAG, "Error al registrar usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Usuario autenticado exitosamente");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.e(TAG, "Error al autenticar usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Envía un email para recuperar la contraseña
     * @param email Email del usuario
     * @param callback Callback para manejar el resultado
     */
    public void resetPassword(String email, AuthCallback callback) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email de recuperación enviado");
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error al enviar email de recuperación", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Cierra la sesión del usuario actual
     */
    public void logout() {
        mAuth.signOut();
        Log.d(TAG, "Usuario cerró sesión");
    }

    /**
     * Interfaz para callbacks de autenticación
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception exception);
    }
}

