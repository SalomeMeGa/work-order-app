package com.example.signatureandpdfapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import com.itextpdf.text.pdf.PdfPTable
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.content.Intent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import com.itextpdf.text.Font

class MainActivity : AppCompatActivity() {

    private lateinit var signaturePad: SignaturePad
    private lateinit var buttonSavePDF: Button
    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextTicketNumber: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var signaturePadSecond: SignaturePad
    private lateinit var editTextStartDate: EditText
    private lateinit var editTextStartTime: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var editTextEndTime: EditText
    private lateinit var spinnerServiceConcluded: Spinner
    private lateinit var spinnerServiceType: Spinner
    private lateinit var descripcion_servicio: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        signaturePad = findViewById(R.id.signature_pad)
        signaturePadSecond = findViewById(R.id.signature_pad_second)
        buttonSavePDF = findViewById(R.id.buttonSavePDF)
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextTicketNumber = findViewById(R.id.editTextTicketNumber)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        editTextStartTime = findViewById(R.id.editTextStartTime)
        editTextEndDate = findViewById(R.id.editTextEndDate)
        editTextEndTime = findViewById(R.id.editTextEndTime)
        spinnerServiceConcluded = findViewById(R.id.spinnerServiceConcluded)
        spinnerServiceType = findViewById(R.id.spinnerServiceType)
        descripcion_servicio = findViewById(R.id.descripcion_servicio)


        // Configurar el Spinner con las prioridades
        val priorities = arrayOf("Baja", "Media", "Alta")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        // Configurar selección de fecha y hora
        editTextStartDate.setOnClickListener { showDatePickerDialog(editTextStartDate) }
        editTextStartTime.setOnClickListener { showTimePickerDialog(editTextStartTime) }
        editTextEndDate.setOnClickListener { showDatePickerDialog(editTextEndDate) }
        editTextEndTime.setOnClickListener { showTimePickerDialog(editTextEndTime) }

        // Configurar el Spinner de "¿Se concluyó el servicio?" con opciones
        val serviceConcludedOptions = arrayOf("Sí", "No")
        val serviceConcludedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceConcludedOptions)
        serviceConcludedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServiceConcluded.adapter = serviceConcludedAdapter

        // Configurar el Spinner de Tipo de atención" con opciones
        val serviceTypeOptions = arrayOf("Correctivo", "Instalación","Retiro","Reemplazo")
        val serviceTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceTypeOptions)
        serviceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerServiceType.adapter = serviceTypeAdapter


        // Solicitar permisos de almacenamiento en tiempo de ejecución
        requestStoragePermissions()

        // Botón para generar PDF
        buttonSavePDF.setOnClickListener {
            val ticketNumber = editTextTicketNumber.text.toString()
            val name = editTextName.text.toString()
            val email = editTextEmail.text.toString()
            val startDate = editTextStartDate.text.toString()
            val startTime = editTextStartTime.text.toString()
            val endDate = editTextEndDate.text.toString()
            val endTime = editTextEndTime.text.toString()
            val signatureBitmap = signaturePad.signatureBitmap
            val signatureBitmapSecond = signaturePadSecond.signatureBitmap
            val selectedPriority = spinnerPriority.selectedItem.toString()
            val selectedService = spinnerServiceConcluded.selectedItem.toString()
            val selectedType = spinnerServiceType.selectedItem.toString()
            val descripcion = descripcion_servicio.text.toString()


            if (signaturePad.isEmpty || signaturePadSecond.isEmpty) {
                Log.e("Firma", "Una o ambas firmas están vacías")
                Toast.makeText(this, "Por favor, firme ambos campos antes de guardar el PDF.", Toast.LENGTH_SHORT).show()
            } else {

                try {
                    savePdfToMediaStore(ticketNumber, name, email, selectedPriority,startDate, startTime, endDate, endTime, signatureBitmap, signatureBitmapSecond, selectedService, selectedType, descripcion)
                    Toast.makeText(this, "PDF guardado exitosamente", Toast.LENGTH_SHORT).show()

                    // Volver a la pantalla de login
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish() // Finaliza la MainActivity


                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Error", "Error al guardar el PDF: ${e.message}")
                    Toast.makeText(this, "Error al guardar el PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Solicitar permisos de almacenamiento en tiempo de ejecución
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    // Guardar el PDF con los datos del usuario y la firma en MediaStore
    private fun savePdfToMediaStore(ticketNumber: String, name: String, email: String, selectedPriority: String, startDate: String, startTime: String, endDate: String, endTime: String, signature: Bitmap, secondSignature: Bitmap, selectedService: String, selectedType: String, descripcion: String) {
        val document = Document()

        // Preparar contenido para guardar en MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Orden_de_Servicio.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MyPDFs")
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {

            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                PdfWriter.getInstance(document, outputStream)
                document.open()
                // Agregar el texto de encabezado

                val boldFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)
                val boldFont_ticket = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
                val headerText = """
                    Netjer Networks México S.A. de C.V.
                    Tordo 22 Oficina A, Col. Tacubaya, Alc. Miguel Hidalgo, C.P. 11870, CDMX
                    Tel: +52 (55) 1054-1184 Fax: +52 (55) 1054-1185
                    """.trimIndent()
                val headerParagraph = Paragraph(headerText).apply {
                    alignment = Paragraph.ALIGN_CENTER
                    spacingAfter = 5f  // Espacio después del encabezado
                }
                document.add(headerParagraph)

                // Agregar un espacio entre el encabezado y el resto del contenido
                document.add(Paragraph("\n")) // Añadir una línea en blanco

                // Crear parrafo adicional
                val additionalText = "Formato Universal de Servicio"
                val additionalParagraph = Paragraph(additionalText, boldFont).apply {
                    alignment = Paragraph.ALIGN_CENTER  // Alineación del párrafo adicional
                    spacingAfter = 15f  // Espacio después del párrafo adicional
                }
                // Agregar el párrafo adicional al documento
                document.add(additionalParagraph)

                // Agregar datos al PDF utilizando los valores ingresados por el usuario

                val ticketParagraph = Paragraph("Número de Ticket: $ticketNumber",boldFont_ticket).apply {
                    alignment = Paragraph.ALIGN_RIGHT
                }
                document.add(ticketParagraph)

                val informationText = "Datos del cliente"
                val informationParagraph = Paragraph(informationText,boldFont_ticket).apply {
                    alignment = Paragraph.ALIGN_CENTER  // Alineación del párrafo adicional
                    spacingAfter = 15f  // Espacio después del párrafo adicional
                }
                // Agregar el párrafo adicional al documento
                document.add(informationParagraph)

                // Agregar datos del cliente en una tabla
                val table = PdfPTable(2)
                table.widthPercentage = 100f // ancho completo

                // Encabezados de la tabla (opcional)
                table.addCell("Dato")
                table.addCell("Descripción")

                 // Agregar filas con los datos del cliente
                table.addCell("Nombre del Cliente")
                table.addCell("Alberto Mendoza Garcia")

                table.addCell("Correo")
                table.addCell(email)

                table.addCell("Nombre de la Empresa")
                table.addCell("Netjer Networks S.A. de C.V.")

                table.addCell("Colonia")
                table.addCell(name)

                table.addCell("Número de Teléfono")
                table.addCell("+52 (55) 1054-1184")

                table.addCell("Dirección")
                table.addCell("Calle Tordo 22, CDMX")
                // Agregar la tabla al documento
                document.add(table)

                // Agregar un espacio entre el encabezado y el resto del contenido
                document.add(Paragraph("\n")) // Añadir una línea en blanco

                // Agregar prioridad
                val priorityParagraph = Paragraph("Prioridad del Servicio: $selectedPriority").apply {
                    alignment = Paragraph.ALIGN_LEFT
                    spacingAfter = 5f
                }
                document.add(priorityParagraph)

                 // Agregar Fecha y Hora de Inicio y Término después de la Prioridad
                document.add(Paragraph("Fecha y Hora de Inicio: $startDate $startTime").apply { spacingAfter = 5f })
                document.add(Paragraph("Fecha y Hora de Término: $endDate $endTime").apply { spacingAfter = 5f })

                 // Agregar si el servicio se concluyo
                val serviceParagraph = Paragraph("El servicio fue concluido?: $selectedService").apply {
                    alignment = Paragraph.ALIGN_LEFT
                    spacingAfter = 5f
                }
                document.add(serviceParagraph)

                val typeParagraph = Paragraph("Tipo de atención: $selectedType").apply {
                    alignment = Paragraph.ALIGN_LEFT
                    spacingAfter = 5f

                }
                document.add(typeParagraph)

                val informationDevice = "Datos del dispositivo"
                val deviceParagraph = Paragraph(informationDevice,boldFont_ticket).apply {
                    alignment = Paragraph.ALIGN_CENTER  // Alineación del párrafo adicional
                    spacingAfter = 15f  // Espacio después del párrafo adicional
                }
                // Agregar el párrafo adicional al documento
                document.add(deviceParagraph)

                // Agregar una segunda tabla con los datos de los dispositivos
                val deviceTable = PdfPTable(2)
                deviceTable.widthPercentage = 100f // ancho completo

                // Encabezados de la segunda tabla
                deviceTable.addCell("Campo")
                deviceTable.addCell("Descripción")

                // Datos ficticios para la tabla de dispositivos
                deviceTable.addCell("Tipo de dispositivo")
                deviceTable.addCell("Swicth de red")

                deviceTable.addCell("Número de serie")
                deviceTable.addCell("NA863LM32")

                deviceTable.addCell("Marca")
                deviceTable.addCell("Extreme Networks")

                deviceTable.addCell("Modelo")
                deviceTable.addCell("Summint X440-G2-48t-10GE4")

                deviceTable.addCell("Inicio de soporte")
                deviceTable.addCell("01-01-2024")

                deviceTable.addCell("Fin de soporte")
                deviceTable.addCell("01-01-2025")

                deviceTable.addCell("Descripción")
                deviceTable.addCell("Switch de red con 48 puertos 10GE y capacidades avanzadas de capa 3")
                // Agregar la segunda tabla al documento
                document.add(deviceTable)

                // Agregar un espacio entre el encabezado y el resto del contenido
                document.add(Paragraph("\n")) // Añadir una línea en blanco

                val informationcomment = "Descripción de la falla de Servicio"
                val commentParagraph = Paragraph(informationcomment,boldFont_ticket).apply {
                    alignment = Paragraph.ALIGN_LEFT  // Alineación del párrafo adicional
                    spacingAfter = 15f  // Espacio después del párrafo adicional
                }
                // Agregar el párrafo adicional al documento
                document.add(commentParagraph)

                // Agregar una tabla con una sola columna y una sola fila
                val singleCellTable = PdfPTable(1)
                singleCellTable.widthPercentage = 100f // ancho completo

                // Texto ficticio para la única celda de la tabla
                singleCellTable.addCell("El switch marca Extreme Networks ha perdido conexión. No responde a pruebas de red, afectando la conectividad en el área asignada.")

                // Agregar la tabla al documento
                document.add(singleCellTable)

                document.add(Paragraph("\n")) // Añadir una línea en blanco

                val descriptioncomment = "Descripción del servicio realizado"
                val descripctionParagraph = Paragraph(descriptioncomment,boldFont_ticket).apply {
                    alignment = Paragraph.ALIGN_LEFT  // Alineación del párrafo adicional
                    spacingAfter = 15f  // Espacio después del párrafo adicional
                }
                // Agregar el párrafo adicional al documento
                document.add(descripctionParagraph)

                // Agregar una tabla con una sola columna y una sola fila
                val descriptionCellTable = PdfPTable(1)
                descriptionCellTable.widthPercentage = 100f // ancho completo

                // Texto ficticio para la única celda de la tabla
                descriptionCellTable.addCell(descripcion)

                // Agregar la tabla al documento
                document.add(descriptionCellTable)

                document.add(Paragraph("\n")) // Añadir una línea en blanco



                // Convertir y agregar la firma al PDF
                val signatureFile = convertBitmapToImageFile(signature)
                Log.i("Firma", "Firma guardada en ${signatureFile.absolutePath}")
                val image = Image.getInstance(signatureFile.absolutePath)
                image.scaleToFit(150f, 150f)  // Ajustar tamaño de la firma
                document.add(image)

                // Agregar el texto "Firma de cliente" debajo de la firma
                val clientSignatureLabel = Paragraph("Firma de Ingeniero").apply {
                    alignment = Paragraph.ALIGN_LEFT // Alineación centrada
                    spacingBefore = 5f // Espacio antes del texto
                }
                document.add(clientSignatureLabel)


                // Convertir y agregar la segunda firma al PDF
                val secondSignatureFile = convertBitmapToImageFile(secondSignature)
                Log.i("Firma", "Firma 2 guardada en ${secondSignatureFile.absolutePath}")
                val secondImage = Image.getInstance(secondSignatureFile.absolutePath)
                secondImage.scaleToFit(150f, 150f)  // Ajustar tamaño de la segunda firma
                document.add(secondImage)

                // Agregar el texto "Firma de testigo" debajo de la segunda firma
                val witnessSignatureLabel = Paragraph("Firma de Cliente").apply {
                    alignment = Paragraph.ALIGN_LEFT // Alineación izquierda
                    spacingBefore = 5f // Espacio antes del texto
                }
                document.add(witnessSignatureLabel)


                document.close()
                Log.i("PDF", "PDF generado exitosamente en MediaStore")

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Error", "Error al crear el PDF: ${e.message}")
            }
        } ?: Log.e("Error", "No se pudo crear el URI en MediaStore")
    }

    // Convertir el Bitmap de la firma en un archivo de imagen temporal
    private fun convertBitmapToImageFile(signature: Bitmap): File {
        val tempFile = File.createTempFile("signature", ".png", cacheDir)

        try {
            val outputStream = FileOutputStream(tempFile)
            signature.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Error", "Error al guardar la imagen: ${e.message}")
        }

        return tempFile
    }

    // Manejar la respuesta de los permisos solicitados
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Permiso denegado. No se puede guardar el PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            editText.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day).show()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            editText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true).show()
    }


}