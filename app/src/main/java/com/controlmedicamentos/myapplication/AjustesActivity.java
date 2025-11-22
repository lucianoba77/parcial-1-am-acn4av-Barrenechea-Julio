package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.R;

public class AjustesActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etTelefono, etEdad;
    private TextInputLayout tilNombre, tilEmail, tilTelefono, tilEdad;
    private Switch switchNotificaciones, switchVibracion, switchSonido;
    private SeekBar seekBarVolumen, seekBarRepeticiones;
    private TextView tvVolumen, tvRepeticiones, tvDiasAntelacion;
    private MaterialButton btnGuardar, btnDiasAntelacion, btnLogout, btnEliminarCuenta;
    private MaterialButton btnNavHome, btnNavNuevaMedicina, btnNavBotiquin, btnNavAjustes;

    private SharedPreferences preferences;
    private int diasAntelacionStock = 3;
    
    private com.controlmedicamentos.myapplication.services.AuthService authService;
    private com.controlmedicamentos.myapplication.services.FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ocultar ActionBar/Toolbar para que no muestre el título duplicado
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_ajustes);

        // Inicializar servicios primero
        authService = new com.controlmedicamentos.myapplication.services.AuthService();
        firebaseService = new com.controlmedicamentos.myapplication.services.FirebaseService();
        
        inicializarVistas();
        cargarDatosUsuario(); // Cargar desde Firebase
        cargarPreferencias(); // Cargar configuraciones locales
        configurarListeners();
    }

    private void inicializarVistas() {
        // Campos de usuario
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etEdad = findViewById(R.id.etEdad);
        tilNombre = findViewById(R.id.tilNombre);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefono = findViewById(R.id.tilTelefono);
        tilEdad = findViewById(R.id.tilEdad);

        // Switches de configuración
        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        switchVibracion = findViewById(R.id.switchVibracion);
        switchSonido = findViewById(R.id.switchSonido);

        // SeekBars
        seekBarVolumen = findViewById(R.id.seekBarVolumen);
        seekBarRepeticiones = findViewById(R.id.seekBarRepeticiones);

        // TextViews
        tvVolumen = findViewById(R.id.tvVolumen);
        tvRepeticiones = findViewById(R.id.tvRepeticiones);
        tvDiasAntelacion = findViewById(R.id.tvDiasAntelacion);

        // Botones
        btnGuardar = findViewById(R.id.btnGuardar);
        btnDiasAntelacion = findViewById(R.id.btnDiasAntelacion);
        btnLogout = findViewById(R.id.btnLogout);
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        
        // Botones de navegación
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavNuevaMedicina = findViewById(R.id.btnNavNuevaMedicina);
        btnNavBotiquin = findViewById(R.id.btnNavBotiquin);
        btnNavAjustes = findViewById(R.id.btnNavAjustes);

        // SharedPreferences
        preferences = getSharedPreferences("ControlMedicamentos", MODE_PRIVATE);
    }

    /**
     * Carga los datos del usuario desde Firebase
     */
    private void cargarDatosUsuario() {
        firebaseService.obtenerUsuarioActual(new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.controlmedicamentos.myapplication.models.Usuario) {
                    com.controlmedicamentos.myapplication.models.Usuario usuario = 
                        (com.controlmedicamentos.myapplication.models.Usuario) result;
                    
                    // Precargar datos del usuario en el formulario
                    if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
                        etNombre.setText(usuario.getNombre());
                    }
                    
                    // Obtener email de Firebase Auth (más confiable)
                    com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
                    if (currentUser != null && currentUser.getEmail() != null) {
                        etEmail.setText(currentUser.getEmail());
                    } else if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
                        etEmail.setText(usuario.getEmail());
                    }
                    
                    // Precargar teléfono y edad si están disponibles
                    if (usuario.getTelefono() != null && !usuario.getTelefono().isEmpty()) {
                        etTelefono.setText(usuario.getTelefono());
                    }
                    if (usuario.getEdad() > 0) {
                        etEdad.setText(String.valueOf(usuario.getEdad()));
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                // Si hay error, intentar cargar desde Firebase Auth
                com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
                if (currentUser != null) {
                    if (currentUser.getDisplayName() != null) {
                        etNombre.setText(currentUser.getDisplayName());
                    }
                    if (currentUser.getEmail() != null) {
                        etEmail.setText(currentUser.getEmail());
                    }
                }
            }
        });
    }

    private void cargarPreferencias() {
        // Cargar configuraciones de notificaciones (no datos del usuario)
        switchNotificaciones.setChecked(preferences.getBoolean("notificaciones", true));
        switchVibracion.setChecked(preferences.getBoolean("vibracion", true));
        switchSonido.setChecked(preferences.getBoolean("sonido", true));

        // Cargar configuraciones de volumen y repeticiones
        int volumen = preferences.getInt("volumen", 70);
        int repeticiones = preferences.getInt("repeticiones", 3);
        diasAntelacionStock = preferences.getInt("dias_antelacion_stock", 7); // Por defecto 7 días

        seekBarVolumen.setProgress(volumen);
        seekBarRepeticiones.setProgress(repeticiones);

        actualizarTextos();
    }

    private void actualizarTextos() {
        tvVolumen.setText("Volumen: " + seekBarVolumen.getProgress() + "%");
        tvRepeticiones.setText("Repeticiones: " + seekBarRepeticiones.getProgress());
        tvDiasAntelacion.setText("Días de antelación: " + diasAntelacionStock);
    }

    private void configurarListeners() {
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarConfiguracion();
            }
        });

        btnDiasAntelacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoDiasAntelacion();
            }
        });
        
        // Navegación inferior
        btnNavHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(AjustesActivity.this, MainActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        
        btnNavNuevaMedicina.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(AjustesActivity.this, NuevaMedicinaActivity.class);
                startActivity(intent);
            }
        });
        
        btnNavBotiquin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(AjustesActivity.this, BotiquinActivity.class);
                startActivity(intent);
                finish();
            }
        });
        
        btnNavAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ya estamos en ajustes, no hacer nada
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarSesion();
            }
        });

        btnEliminarCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoEliminarCuenta();
            }
        });

        seekBarVolumen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumen.setText("Volumen: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarRepeticiones.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRepeticiones.setText("Repeticiones: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void guardarConfiguracion() {
        // Validar que los campos requeridos estén completos
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String edadStr = etEdad.getText().toString().trim();
        
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("El email es requerido y debe ser válido");
            return;
        }
        
        int edad = 0;
        if (!edadStr.isEmpty()) {
            try {
                edad = Integer.parseInt(edadStr);
            } catch (NumberFormatException e) {
                tilEdad.setError("La edad debe ser un número válido");
                return;
            }
        }
        
        // Guardar datos del usuario en Firebase
        com.controlmedicamentos.myapplication.models.Usuario usuario = 
            new com.controlmedicamentos.myapplication.models.Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setTelefono(telefono.isEmpty() ? null : telefono);
        usuario.setEdad(edad);
        
        firebaseService.guardarUsuario(usuario, new com.controlmedicamentos.myapplication.services.FirebaseService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                // Guardar configuraciones locales en SharedPreferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("notificaciones", switchNotificaciones.isChecked());
                editor.putBoolean("vibracion", switchVibracion.isChecked());
                editor.putBoolean("sonido", switchSonido.isChecked());
                editor.putInt("volumen", seekBarVolumen.getProgress());
                editor.putInt("repeticiones", seekBarRepeticiones.getProgress());
                editor.putInt("dias_antelacion_stock", diasAntelacionStock);
                editor.apply();
                
                Toast.makeText(AjustesActivity.this, "Configuración guardada exitosamente", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(AjustesActivity.this, 
                    "Error al guardar datos del usuario: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDialogoDiasAntelacion() {
        String[] opciones = {"1 día", "2 días", "3 días", "5 días", "7 días"};
        int[] valores = {1, 2, 3, 5, 7};

        new AlertDialog.Builder(this)
                .setTitle("Días de antelación para stock bajo")
                .setSingleChoiceItems(opciones, getIndiceDiasAntelacion(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        diasAntelacionStock = valores[which];
                        actualizarTextos();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int getIndiceDiasAntelacion() {
        switch (diasAntelacionStock) {
            case 1: return 0;
            case 2: return 1;
            case 3: return 2;
            case 5: return 3;
            case 7: return 4;
            default: return 2;
        }
    }


    private void cerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Cerrar Sesión", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        authService.logout();
                        // Redirigir a LoginActivity
                        android.content.Intent intent = new android.content.Intent(AjustesActivity.this, LoginActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEliminarCuenta() {
        // Crear diálogo para ingresar credenciales
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_eliminar_cuenta, null);
        TextInputEditText etEmailEliminar = dialogView.findViewById(R.id.etEmailEliminar);
        TextInputEditText etPasswordEliminar = dialogView.findViewById(R.id.etPasswordEliminar);
        
        // Prellenar email si está disponible
        com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            etEmailEliminar.setText(currentUser.getEmail());
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta Permanentemente")
                .setMessage("⚠️ Esta acción es permanente y no se puede deshacer. Se eliminarán:\n\n" +
                        "• Tu cuenta de usuario\n" +
                        "• Todos tus medicamentos\n" +
                        "• Todos tus asistentes\n" +
                        "• Todos tus registros e historial")
                .setView(dialogView)
                .setPositiveButton("Eliminar Cuenta", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = etEmailEliminar.getText().toString().trim();
                        String password = etPasswordEliminar.getText().toString();
                        
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(AjustesActivity.this, "Por favor completa todos los campos", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        eliminarCuenta(email, password);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuenta(String email, String password) {
        // Verificar si es usuario de Google
        com.google.firebase.auth.FirebaseUser currentUser = authService.getCurrentUser();
        boolean esGoogle = false;
        if (currentUser != null) {
            for (com.google.firebase.auth.UserInfo provider : currentUser.getProviderData()) {
                if ("google.com".equals(provider.getProviderId())) {
                    esGoogle = true;
                    break;
                }
            }
        }
        
        // Implementar eliminación de cuenta
        // Por ahora mostrar mensaje de que está en desarrollo
        Toast.makeText(this, "Funcionalidad de eliminar cuenta en desarrollo. Se implementará próximamente.", Toast.LENGTH_LONG).show();
        
        // TODO: Implementar eliminación completa de cuenta
        // 1. Reautenticar usuario
        // 2. Eliminar todos los medicamentos
        // 3. Eliminar todos los asistentes
        // 4. Eliminar documento de usuario en Firestore
        // 5. Eliminar usuario de Firebase Auth
        // 6. Redirigir a LoginActivity
    }
}
