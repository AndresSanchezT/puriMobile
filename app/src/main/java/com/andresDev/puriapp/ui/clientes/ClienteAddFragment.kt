package com.andresDev.puriapp.ui.clientes

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.andresDev.puriapp.data.model.Cliente
import com.andresDev.puriapp.databinding.FragmentClienteAddBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ClienteAddFragment : Fragment() {

    private val clienteViewModel: ClienteViewModel by viewModels()
    private var _binding: FragmentClienteAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PERMISOS (respuesta)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

            val readImagesGranted =
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        && permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true)

            when {
                cameraGranted -> openCamera()
                readImagesGranted -> openGalleryLegacy()
                else -> Toast.makeText(requireContext(), "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }

        // CÁMARA
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUri != null) {
                binding.ivLogoCliente.setImageURI(imageUri)
            }
        }

        // PHOTO PICKER (Android 13+) - NO REQUIERE PERMISOS
        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.ivLogoCliente.setImageURI(uri)
            }
        }

        // GALERÍA LEGACY (Android 12 y menores)
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.ivLogoCliente.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCambiarImagen.setOnClickListener {
            showImagePickerOptions()
        }
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnAnadirCliente.setOnClickListener {
            registrarCliente()
        }

        binding.btnLimpiarCampos.setOnClickListener {
            limpiarFormulario()
        }
    }

    private fun registrarCliente() {
        // Validar campos
        val nombreNegocio = binding.etNombreNegocio.text.toString().trim()
        val nombresApellidos = binding.etNombresApellidos.text.toString().trim()
        val direccion = binding.etDireccion.text.toString().trim()
        val referencia = binding.etReferencia.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()

        // Validaciones
        if (nombreNegocio.isEmpty()) {
            binding.etNombreNegocio.error = "Campo requerido"
            return
        }

        if (nombresApellidos.isEmpty()) {
            binding.etNombresApellidos.error = "Campo requerido"
            return
        }

        if (direccion.isEmpty()) {
            binding.etDireccion.error = "Campo requerido"
            return
        }

//        if (telefono.isEmpty()) {
//            binding.etTelefono.error = "Campo requerido"
//            return
//        }

        if (telefono.length < 9) {
            binding.etTelefono.error = "Teléfono inválido"
            return
        }


        // Obtener fecha y hora actual en formato ISO 8601
        val fechaActual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            // Para versiones anteriores a Android 8
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
        }
        // Crear objeto Cliente con los campos que acepta el backend
        val nuevoCliente = Cliente(
            id = null, // El backend lo genera
            nombreContacto = nombresApellidos,
            nombreNegocio = nombreNegocio,
            direccion = direccion,
            referencia = referencia.ifEmpty { null },
            estado = "", // El backend lo genera
            telefono = telefono,
            fechaRegistro = fechaActual, // El backend lo genera
            fechaActualizacion = null, // El backend lo genera
            latitud = null, // Opcional por ahora
            longitud = null, // Opcional por ahora
            tieneCredito = false // Valor por defecto
        )

        // Deshabilitar botón mientras se procesa
        binding.btnAnadirCliente.isEnabled = false

        // Llamar al ViewModel para registrar
        clienteViewModel.registrarCliente(nuevoCliente)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            clienteViewModel.registroClienteState.collect { state ->
                when (state) {
                    is RegistroClienteState.Idle -> {
                        // Estado inicial
                    }
                    is RegistroClienteState.Loading -> {
                        // Mostrar loading
                        binding.btnAnadirCliente.isEnabled = false
                        Toast.makeText(requireContext(), "Registrando cliente...", Toast.LENGTH_SHORT).show()
                    }
                    is RegistroClienteState.Success -> {
                        // Éxito
                        binding.btnAnadirCliente.isEnabled = true
                        Toast.makeText(requireContext(), "Cliente registrado exitosamente", Toast.LENGTH_LONG).show()
                        limpiarFormulario()
                        // Opcionalmente navegar atrás
                         findNavController().navigateUp()
                    }
                    is RegistroClienteState.Error -> {
                        // Error
                        binding.btnAnadirCliente.isEnabled = true
                        Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun limpiarFormulario() {
        binding.etNombreNegocio.text?.clear()
        binding.etNombresApellidos.text?.clear()
        binding.etDireccion.text?.clear()
        binding.etReferencia.text?.clear()
        binding.etTelefono.text?.clear()

        // Limpiar errores
        binding.etNombreNegocio.error = null
        binding.etNombresApellidos.error = null
        binding.etDireccion.error = null
        binding.etTelefono.error = null
    }


    // Selección de opción
    private fun showImagePickerOptions() {
        val opciones = arrayOf("Abrir Cámara", "Abrir Galería")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar opción")
            .setItems(opciones) { _, index ->
                when (index) {
                    0 -> requestCameraPermission()
                    1 -> openGallery()
                }
            }
            .show()
    }

    // Permiso para cámara
    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    // Abrir galería (sin permisos en Android 13+)
    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa Photo Picker (NO requiere permisos)
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // Android 12 y menores necesitan permiso
            requestGalleryPermission()
        }
    }

    // Permiso para galería (solo Android 12 y menores)
    private fun requestGalleryPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "temp_image.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        imageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun openGalleryLegacy() {
        galleryLauncher.launch("image/*")
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Estados para el registro
sealed class RegistroClienteState {
    object Idle : RegistroClienteState()
    object Loading : RegistroClienteState()
    data class Success(val cliente: Cliente) : RegistroClienteState()
    data class Error(val message: String) : RegistroClienteState()
}