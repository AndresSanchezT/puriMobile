package com.andresDev.puriapp.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object DateFormatter {

    /**
     * Formatea una fecha ISO 8601 a formato legible
     * Entrada: "2025-11-18T18:59:14.241666"
     * Salida: "18/11/2025 6:59 PM"
     */
    fun formatearFechaHora(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Sin fecha"

        return try {
            // Para Android API 26+ (Java 8 time API)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val dateTime = LocalDateTime.parse(fechaISO, formatter)

                val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale("es", "PE"))
                dateTime.format(outputFormatter)
            } else {
                // Para versiones anteriores a Android API 26
                formatearFechaHoraLegacy(fechaISO)
            }
        } catch (e: Exception) {
            fechaISO // Si falla, mostrar la fecha original
        }
    }

    /**
     * Formatea solo la fecha sin hora
     * Entrada: "2025-11-18T18:59:14.241666"
     * Salida: "18/11/2025"
     */
    fun formatearSoloFecha(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Sin fecha"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val dateTime = LocalDateTime.parse(fechaISO, formatter)

                val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "PE"))
                dateTime.format(outputFormatter)
            } else {
                formatearSoloFechaLegacy(fechaISO)
            }
        } catch (e: Exception) {
            fechaISO
        }
    }

    /**
     * Formatea solo la hora
     * Entrada: "2025-11-18T18:59:14.241666"
     * Salida: "6:59 PM"
     */
    fun formatearSoloHora(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Sin hora"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val dateTime = LocalDateTime.parse(fechaISO, formatter)

                val outputFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "PE"))
                dateTime.format(outputFormatter)
            } else {
                formatearSoloHoraLegacy(fechaISO)
            }
        } catch (e: Exception) {
            fechaISO
        }
    }

    /**
     * Formato más descriptivo con día de la semana
     * Entrada: "2025-11-18T18:59:14.241666"
     * Salida: "Martes, 18 de noviembre de 2025 - 6:59 PM"
     */
    fun formatearFechaCompleta(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Sin fecha"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val dateTime = LocalDateTime.parse(fechaISO, formatter)

                val outputFormatter = DateTimeFormatter.ofPattern(
                    "EEEE, dd 'de' MMMM 'de' yyyy - h:mm a",
                    Locale("es", "PE")
                )
                dateTime.format(outputFormatter).capitalize()
            } else {
                formatearFechaCompletaLegacy(fechaISO)
            }
        } catch (e: Exception) {
            fechaISO
        }
    }

    // ============ MÉTODOS LEGACY PARA API < 26 ============

    private fun formatearFechaHoraLegacy(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale("es", "PE"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Intentar con formato sin microsegundos
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale("es", "PE"))
                val date = inputFormat.parse(fechaISO)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                fechaISO
            }
        }
    }

    private fun formatearSoloFechaLegacy(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
                val date = inputFormat.parse(fechaISO)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                fechaISO
            }
        }
    }

    private fun formatearSoloHoraLegacy(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale("es", "PE"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("h:mm a", Locale("es", "PE"))
                val date = inputFormat.parse(fechaISO)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                fechaISO
            }
        }
    }

    private fun formatearFechaCompletaLegacy(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy - h:mm a", Locale("es", "PE"))
            val date = inputFormat.parse(fechaISO)
            outputFormat.format(date ?: Date()).capitalize()
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy - h:mm a", Locale("es", "PE"))
                val date = inputFormat.parse(fechaISO)
                outputFormat.format(date ?: Date()).capitalize()
            } catch (e2: Exception) {
                fechaISO
            }
        }
    }
}