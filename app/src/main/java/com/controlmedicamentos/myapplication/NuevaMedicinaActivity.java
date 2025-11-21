package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.utils.DatosPrueba;
import java.util.Calendar;

public class NuevaMedicinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etAfeccion, etDetalles;
    private TextInputLayout tilNombre, tilAfeccion;
    private MaterialButton btnGuardar, btnCancelar, btnSeleccionarColor, btnFechaVencimiento, btnCancelarAccion;
    private MaterialButton btnSeleccionarHora;
    private android.widget.Spinner spinnerPresentacion;
    private TextInputEditText etTomasDiarias, etStockInicial, etDiasTratamiento;
    private TextInputLayout tilTomasDiarias, tilStockInicial, tilDiasTratamiento;

    private int colorSeleccionado = R.color.medicamento_azul;
    private Calendar fechaVencimiento = null;
    private String horaSeleccionada = "08:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_medicina);

        inicializarVistas();
        configurarSpinner();
        configurarListeners();
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etAfeccion = findViewById(R.id.etAfeccion);
        etDetalles = findViewById(R.id.etDetalles);
        tilNombre = findViewById(R.id.tilNombre);
        tilAfeccion = findViewById(R.id.tilAfeccion);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelarAccion = findViewById(R.id.btnCancelarAccion);
        btnSeleccionarColor = findViewById(R.id.btnSeleccionarColor);
        btnFechaVencimiento = findViewById(R.id.btnFechaVencimiento);
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora);

        spinnerPresentacion = findViewById(R.id.spinnerPresentacion);
        etTomasDiarias = findViewById(R.id.etTomasDiarias);
        etStockInicial = findViewById(R.id.etStockInicial);
        etDiasTratamiento = findViewById(R.id.etDiasTratamiento);

        tilTomasDiarias = findViewById(R.id.tilTomasDiarias);
        tilStockInicial = findViewById(R.id.tilStockInicial);
        tilDiasTratamiento = findViewById(R.id.tilDiasTratamiento);
    }

    private void configurarSpinner() {
        String[] presentaciones = {
                "Comprimidos", "Cápsulas", "Jarabe", "Crema",
                "Pomada", "Spray nasal", "Inyección", "Gotas"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presentaciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresentacion.setAdapter(adapter);
    }

    private void configurarListeners() {
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarMedicamento();
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCancelarAccion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSeleccionarColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorColor();
            }
        });

        btnSeleccionarHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorHora();
            }
        });

        btnFechaVencimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorFecha();
            }
        });
    }
    private void configurarSpinner() {
        String[] presentaciones = {
                "Comprimidos", "Cápsulas", "Jarabe", "Crema",
                "Pomada", "Spray nasal", "Inyección", "Gotas", "Parche"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, presentaciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresentacion.setAdapter(adapter);

        // Listener para cambiar el hint según la presentación
        spinnerPresentacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String presentacion = presentaciones[position];
                actualizarHintStock(presentacion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void actualizarHintStock(String presentacion) {
        switch (presentacion) {
            case "Comprimidos":
            case "Cápsulas":
                tilStockInicial.setHint("Cantidad de comprimidos");
                etStockInicial.setHint("30");
                break;
            case "Jarabe":
            case "Inyección":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint("15");
                break;
            case "Crema":
            case "Pomada":
            case "Parche":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint("20");
                break;
            case "Spray nasal":
            case "Gotas":
                tilStockInicial.setHint("Días estimados de duración");
                etStockInicial.setHint("10");
                break;
        }
    }
    private void guardarMedicamento() {
        if (validarFormulario()) {
            Medicamento medicamento = crearMedicamento();
            DatosPrueba.agregarMedicamento(medicamento);

            Toast.makeText(this, "Medicamento guardado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;

        if (TextUtils.isEmpty(etNombre.getText())) {
            tilNombre.setError("El nombre es requerido");
            valido = false;
        } else {
            tilNombre.setError(null);
        }

        if (TextUtils.isEmpty(etAfeccion.getText())) {
            tilAfeccion.setError("La afección es requerida");
            valido = false;
        } else {
            tilAfeccion.setError(null);
        }

        if (TextUtils.isEmpty(etTomasDiarias.getText())) {
            tilTomasDiarias.setError("Las tomas diarias son requeridas");
            valido = false;
        } else {
            tilTomasDiarias.setError(null);
        }

        if (TextUtils.isEmpty(btnSeleccionarHora.getText())) {
            Toast.makeText(this, "Debe seleccionar una hora", Toast.LENGTH_SHORT).show();
            valido = false;
        }

        if (TextUtils.isEmpty(etStockInicial.getText())) {
            tilStockInicial.setError("El stock inicial es requerido");
            valido = false;
        } else {
            tilStockInicial.setError(null);
        }

        return valido;
    }

    private Medicamento crearMedicamento() {
        String nombre = etNombre.getText().toString();
        String afeccion = etAfeccion.getText().toString();
        String detalles = etDetalles.getText().toString();
        String presentacion = spinnerPresentacion.getSelectedItem().toString();
        int tomasDiarias = Integer.parseInt(etTomasDiarias.getText().toString());
        String horarioPrimeraToma = horaSeleccionada;
        int stockInicial = Integer.parseInt(etStockInicial.getText().toString());
        int diasTratamiento = Integer.parseInt(etDiasTratamiento.getText().toString());

        String id = String.valueOf(System.currentTimeMillis());

        Medicamento medicamento = new Medicamento(
                id, nombre, presentacion, tomasDiarias, horarioPrimeraToma,
                afeccion, stockInicial, colorSeleccionado, diasTratamiento
        );

        medicamento.setDetalles(detalles);

        if (fechaVencimiento != null) {
            medicamento.setFechaVencimiento(fechaVencimiento.getTime());
        }

        return medicamento;
    }

    private void mostrarSelectorColor() {
        String[] colores = {"Azul", "Verde", "Rojo", "Naranja", "Morado", "Amarillo"};
        int[] valoresColores = {
                R.color.medicamento_azul,
                R.color.medicamento_verde,
                R.color.medicamento_rojo,
                R.color.medicamento_naranja,
                R.color.medicamento_morado,
                R.color.medicamento_amarillo
        };

        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Color")
                .setItems(colores, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        colorSeleccionado = valoresColores[which];
                        btnSeleccionarColor.setBackgroundColor(getResources().getColor(colorSeleccionado));
                        btnSeleccionarColor.setText(colores[which]);
                    }
                })
                .show();
    }

    private void mostrarSelectorHora() {
        String[] partes = horaSeleccionada.split(":");
        int hora = Integer.parseInt(partes[0]);
        int minuto = Integer.parseInt(partes[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        horaSeleccionada = String.format("%02d:%02d", hourOfDay, minute);
                        btnSeleccionarHora.setText(horaSeleccionada);
                    }
                }, hora, minuto, true);

        timePickerDialog.show();
    }

    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        fechaVencimiento = Calendar.getInstance();
                        fechaVencimiento.set(year, month, dayOfMonth);
                        btnFechaVencimiento.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
}