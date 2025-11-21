package com.controlmedicamentos.myapplication.services;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.models.Toma;
import com.controlmedicamentos.myapplication.models.Usuario;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para manejar operaciones CRUD con Firebase Firestore
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseFirestore db;
    private AuthService authService;

    // Nombres de colecciones
    private static final String COLLECTION_USUARIOS = "usuarios";
    private static final String COLLECTION_MEDICAMENTOS = "medicamentos";
    private static final String COLLECTION_TOMAS = "tomas";
    private static final String COLLECTION_CONFIGURACIONES = "configuraciones";

    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
        authService = new AuthService();
    }

    // ==================== USUARIOS ====================

    /**
     * Guarda o actualiza un usuario en Firestore
     */
    public void guardarUsuario(Usuario usuario, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> usuarioMap = usuarioToMap(usuario);
        usuarioMap.put("fechaActualizacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_USUARIOS)
            .document(firebaseUser.getUid())
            .set(usuarioMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Usuario guardado exitosamente");
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar usuario", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Obtiene el usuario actual desde Firestore
     */
    public void obtenerUsuarioActual(FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_USUARIOS)
            .document(firebaseUser.getUid())
            .get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Usuario usuario = mapToUsuario(document);
                            if (callback != null) {
                                callback.onSuccess(usuario);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(new Exception("Usuario no encontrado"));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al obtener usuario", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    // ==================== MEDICAMENTOS ====================

    /**
     * Guarda un nuevo medicamento en Firestore
     */
    public void guardarMedicamento(Medicamento medicamento, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> medicamentoMap = medicamentoToMap(medicamento);
        medicamentoMap.put("usuarioId", firebaseUser.getUid());
        medicamentoMap.put("fechaCreacion", com.google.firebase.Timestamp.now());
        medicamentoMap.put("fechaActualizacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_MEDICAMENTOS)
            .add(medicamentoMap)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Medicamento guardado con ID: " + documentReference.getId());
                    medicamento.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess(medicamento);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Actualiza un medicamento existente
     */
    public void actualizarMedicamento(Medicamento medicamento, FirestoreCallback callback) {
        Map<String, Object> medicamentoMap = medicamentoToMap(medicamento);
        medicamentoMap.put("fechaActualizacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamento.getId())
            .update(medicamentoMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Medicamento actualizado exitosamente");
                    if (callback != null) {
                        callback.onSuccess(medicamento);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al actualizar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Elimina un medicamento
     */
    public void eliminarMedicamento(String medicamentoId, FirestoreCallback callback) {
        db.collection(COLLECTION_MEDICAMENTOS)
            .document(medicamentoId)
            .delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Medicamento eliminado exitosamente");
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al eliminar medicamento", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * Obtiene todos los medicamentos del usuario actual
     */
    public void obtenerMedicamentos(FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("usuarioId", firebaseUser.getUid())
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Medicamento> medicamentos = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Medicamento medicamento = mapToMedicamento(document);
                            medicamentos.add(medicamento);
                        }
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Log.e(TAG, "Error al obtener medicamentos", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    /**
     * Obtiene medicamentos activos del usuario actual
     */
    public void obtenerMedicamentosActivos(FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("usuarioId", firebaseUser.getUid())
            .whereEqualTo("activo", true)
            .whereEqualTo("pausado", false)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Medicamento> medicamentos = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Medicamento medicamento = mapToMedicamento(document);
                            medicamentos.add(medicamento);
                        }
                        if (callback != null) {
                            callback.onSuccess(medicamentos);
                        }
                    } else {
                        Log.e(TAG, "Error al obtener medicamentos activos", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }

    // ==================== TOMAS ====================

    /**
     * Guarda una toma en Firestore
     */
    public void guardarToma(Toma toma, FirestoreCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return;
        }

        Map<String, Object> tomaMap = tomaToMap(toma);
        tomaMap.put("usuarioId", firebaseUser.getUid());
        tomaMap.put("fechaCreacion", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_TOMAS)
            .add(tomaMap)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Toma guardada con ID: " + documentReference.getId());
                    toma.setId(documentReference.getId());
                    if (callback != null) {
                        callback.onSuccess(toma);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error al guardar toma", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    // ==================== LISTENERS EN TIEMPO REAL ====================

    /**
     * Agrega un listener para cambios en tiempo real de medicamentos
     */
    public com.google.firebase.firestore.ListenerRegistration agregarListenerMedicamentos(
            FirestoreListCallback callback) {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser == null) {
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado"));
            }
            return null;
        }

        return db.collection(COLLECTION_MEDICAMENTOS)
            .whereEqualTo("usuarioId", firebaseUser.getUid())
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error en listener de medicamentos", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                    return;
                }

                if (snapshot != null) {
                    List<Medicamento> medicamentos = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Medicamento medicamento = mapToMedicamento(document);
                        medicamentos.add(medicamento);
                    }
                    if (callback != null) {
                        callback.onSuccess(medicamentos);
                    }
                }
            });
    }

    // ==================== CONVERSIÓN DE OBJETOS ====================

    private Map<String, Object> usuarioToMap(Usuario usuario) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre", usuario.getNombre());
        map.put("email", usuario.getEmail());
        map.put("telefono", usuario.getTelefono());
        map.put("edad", usuario.getEdad());
        map.put("medicamentosIds", usuario.getMedicamentosIds());
        return map;
    }

    private Usuario mapToUsuario(DocumentSnapshot document) {
        Usuario usuario = new Usuario();
        usuario.setId(document.getId());
        usuario.setNombre(document.getString("nombre"));
        usuario.setEmail(document.getString("email"));
        usuario.setTelefono(document.getString("telefono"));
        if (document.get("edad") != null) {
            usuario.setEdad(document.getLong("edad").intValue());
        }
        if (document.get("medicamentosIds") != null) {
            usuario.setMedicamentosIds((List<String>) document.get("medicamentosIds"));
        }
        return usuario;
    }

    private Map<String, Object> medicamentoToMap(Medicamento medicamento) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre", medicamento.getNombre());
        map.put("presentacion", medicamento.getPresentacion());
        map.put("tomasDiarias", medicamento.getTomasDiarias());
        map.put("horarioPrimeraToma", medicamento.getHorarioPrimeraToma());
        map.put("afeccion", medicamento.getAfeccion());
        map.put("stockInicial", medicamento.getStockInicial());
        map.put("stockActual", medicamento.getStockActual());
        map.put("color", medicamento.getColor());
        map.put("diasTratamiento", medicamento.getDiasTratamiento());
        map.put("activo", medicamento.isActivo());
        map.put("pausado", medicamento.isPausado());
        map.put("detalles", medicamento.getDetalles());
        map.put("horariosTomas", medicamento.getHorariosTomas());
        if (medicamento.getFechaVencimiento() != null) {
            map.put("fechaVencimiento", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(medicamento.getFechaVencimiento().getTime())));
        }
        if (medicamento.getFechaInicioTratamiento() != null) {
            map.put("fechaInicioTratamiento", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(medicamento.getFechaInicioTratamiento().getTime())));
        }
        map.put("tipoStock", medicamento.getTipoStock().name());
        map.put("diasEstimadosDuracion", medicamento.getDiasEstimadosDuracion());
        map.put("diasRestantesDuracion", medicamento.getDiasRestantesDuracion());
        return map;
    }

    private Medicamento mapToMedicamento(DocumentSnapshot document) {
        Medicamento medicamento = new Medicamento();
        medicamento.setId(document.getId());
        medicamento.setNombre(document.getString("nombre"));
        medicamento.setPresentacion(document.getString("presentacion"));
        if (document.get("tomasDiarias") != null) {
            medicamento.setTomasDiarias(document.getLong("tomasDiarias").intValue());
        }
        medicamento.setHorarioPrimeraToma(document.getString("horarioPrimeraToma"));
        medicamento.setAfeccion(document.getString("afeccion"));
        if (document.get("stockInicial") != null) {
            medicamento.setStockInicial(document.getLong("stockInicial").intValue());
        }
        if (document.get("stockActual") != null) {
            medicamento.setStockActual(document.getLong("stockActual").intValue());
        }
        if (document.get("color") != null) {
            medicamento.setColor(document.getLong("color").intValue());
        }
        if (document.get("diasTratamiento") != null) {
            medicamento.setDiasTratamiento(document.getLong("diasTratamiento").intValue());
        }
        medicamento.setActivo(document.getBoolean("activo") != null ? document.getBoolean("activo") : true);
        medicamento.setPausado(document.getBoolean("pausado") != null ? document.getBoolean("pausado") : false);
        medicamento.setDetalles(document.getString("detalles"));
        if (document.get("horariosTomas") != null) {
            medicamento.setHorariosTomas((List<String>) document.get("horariosTomas"));
        }
        if (document.getTimestamp("fechaVencimiento") != null) {
            medicamento.setFechaVencimiento(document.getTimestamp("fechaVencimiento").toDate());
        }
        if (document.getTimestamp("fechaInicioTratamiento") != null) {
            medicamento.setFechaInicioTratamiento(document.getTimestamp("fechaInicioTratamiento").toDate());
        }
        if (document.getString("tipoStock") != null) {
            medicamento.setTipoStock(Medicamento.TipoStock.valueOf(document.getString("tipoStock")));
        }
        if (document.get("diasEstimadosDuracion") != null) {
            medicamento.setDiasEstimadosDuracion(document.getLong("diasEstimadosDuracion").intValue());
        }
        if (document.get("diasRestantesDuracion") != null) {
            medicamento.setDiasRestantesDuracion(document.getLong("diasRestantesDuracion").intValue());
        }
        return medicamento;
    }

    private Map<String, Object> tomaToMap(Toma toma) {
        Map<String, Object> map = new HashMap<>();
        map.put("medicamentoId", toma.getMedicamentoId());
        if (toma.getFechaHoraProgramada() != null) {
            map.put("fechaHoraProgramada", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(toma.getFechaHoraProgramada().getTime())));
        }
        if (toma.getFechaHoraTomada() != null) {
            map.put("fechaHoraTomada", new com.google.firebase.Timestamp(
                new java.sql.Timestamp(toma.getFechaHoraTomada().getTime())));
        }
        map.put("estado", toma.getEstado().name());
        map.put("observaciones", toma.getObservaciones());
        return map;
    }

    // ==================== INTERFACES ====================

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onError(Exception exception);
    }

    public interface FirestoreListCallback {
        void onSuccess(List<?> result);
        void onError(Exception exception);
    }
}

