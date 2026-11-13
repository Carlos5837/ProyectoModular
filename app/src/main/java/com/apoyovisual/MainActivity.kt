package com.apoyovisual

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.LANG_MISSING_DATA
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.location.LocationListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

// Paleta oscura con botones luminosos y texto de alto contraste.
private val Navy = Color(0xFF0B1423)       // Fondo principal
private val Panel = Color(0xFF17263A)      // Tarjetas y paneles
private val Accent = Color(0xFFFFD166)     // Botones dorados claros
private val White = Color(0xFFF8FAFC)      // Texto principal
private val Muted = Color(0xFFD5DFEA)      // Texto secundario
private val Black = Color(0xFF000000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApoyoVisualApp()
        }
    }
}

@Composable
fun ApoyoVisualApp() {
    val context = LocalContext.current

    var screen by remember { mutableStateOf("Inicio") }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var speechVolume by remember { mutableFloatStateOf(1.0f) }
    var largeText by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }

    var recognizedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Aplicación lista") }
    var objectResults by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("Ubicación no consultada") }
    var ttsReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf("") }

    val backgroundColor = if (highContrast) Black else Navy
    val cardColor = if (highContrast) Black else Panel
    val textColor = White
    val titleSize = if (largeText) 30.sp else 25.sp
    val bodySize = if (largeText) 21.sp else 17.sp

    // Motor de lectura de voz
    val tts = remember(context) {
        TextToSpeech(context.applicationContext) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Función para reproducir mensajes
    val speak: (String) -> Unit = { message ->

        if (message.isBlank()) {
            status = "No hay texto para reproducir."
        } else if (!ttsReady) {
            // Conserva el mensaje y lo reproduce cuando el motor termine de iniciar.
            pendingSpeech = message
            status = "El motor de voz todavía está iniciando. El mensaje se reproducirá en cuanto esté listo."
        } else {
            try {
                tts.stop()

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                tts.setAudioAttributes(audioAttributes)

                var languageResult = tts.setLanguage(Locale("es", "MX"))

                if (languageResult == LANG_MISSING_DATA ||
                    languageResult == LANG_NOT_SUPPORTED
                ) {
                    languageResult = tts.setLanguage(Locale("es"))
                }

                if (languageResult == LANG_MISSING_DATA ||
                    languageResult == LANG_NOT_SUPPORTED
                ) {
                    status = "No hay una voz en español instalada."
                } else {
                    tts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

                    val parameters = Bundle().apply {
                        putFloat(
                            TextToSpeech.Engine.KEY_PARAM_VOLUME,
                            speechVolume.coerceIn(0f, 1f)
                        )
                    }

                    val result = tts.speak(
                        message,
                        TextToSpeech.QUEUE_FLUSH,
                        parameters,
                        "apoyo_visual_message"
                    )

                    status = if (result == TextToSpeech.SUCCESS) {
                        "Reproduciendo mensaje."
                    } else {
                        "Error al iniciar la reproducción de voz."
                    }
                }

            } catch (e: Exception) {
                status = "Error de voz: ${e.message}"
            }
        }
    }

    // Reproduce el último mensaje pendiente cuando TextToSpeech termina de iniciar.
    LaunchedEffect(ttsReady) {
        if (ttsReady && pendingSpeech.isNotBlank()) {
            val message = pendingSpeech
            pendingSpeech = ""
            speak(message)
        }
    }

    // Cámara y reconocimiento de texto
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->

        if (bitmap == null) {
            status = "No se obtuvo ninguna imagen."
            speak(status)
        } else {
            try {
                status = "Analizando imagen..."

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )

                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val text = result.text

                        if (text.isNotBlank()) {
                            recognizedText = text
                            status = "Texto reconocido correctamente."
                            speak("El texto encontrado es: $text")
                        } else {
                            recognizedText = ""
                            status = "No se encontró texto en la imagen."
                            speak(status)
                        }
                    }
                    .addOnFailureListener { exception ->
                        status = "Error al reconocer texto: ${exception.message}"
                        speak(status)
                    }
                    .addOnCompleteListener {
                        recognizer.close()
                    }

            } catch (e: Exception) {
                status = "Error al procesar la imagen: ${e.message}"
                speak(status)
            }
        }
    }

    // Solicitud del permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            status = "Abriendo cámara..."
            cameraLauncher.launch(null)
        } else {
            status = "Debes permitir el acceso a la cámara."
            speak(status)
        }
    }

    // Cámara para identificación de objetos
    val objectCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->

        if (bitmap == null) {
            status = "No se obtuvo ninguna fotografía."
            speak(status)
        } else {
            status = "Analizando objetos..."
            objectResults = "Analizando fotografía..."

            detectarObjetos(context, bitmap) { resultado ->
                objectResults = resultado
                status = resultado
                speak(resultado)
            }
        }
    }

// Permiso de cámara para identificación de objetos
    val objectCameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                objectCameraLauncher.launch(null)
            } else {
                status = "Debes permitir el acceso a la cámara."
                speak(status)
            }
        }

    // Solicitud de permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            locationText = getLocationText(context)
            status = locationText
            speak(locationText)
        } else {
            locationText = "No se concedió el permiso de ubicación."
            status = locationText
            speak(locationText)
        }
    }

    // Consulta de ubicación
    val requestLocation = {
        val fineGranted = context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = context.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            locationText = getLocationText(context)
            status = locationText
            speak(locationText)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = backgroundColor,
            surface = cardColor,
            onPrimary = Navy,
            onBackground = White,
            onSurface = White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (screen != "Inicio") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { screen = "Inicio" }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = White
                            )
                        }

                        Text(
                            text = screen,
                            color = White,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (screen) {
                    "Inicio" -> {
                        Text(
                            text = "APOYO VISUAL",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Herramientas de apoyo para personas con discapacidad visual",
                            fontSize = bodySize,
                            color = White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(25.dp))

                        BigButton(
                            text = "Leer texto",
                            icon = Icons.Filled.RecordVoiceOver
                        ) {
                            screen = "Leer texto"
                            speak("Sección de lectura de texto")
                        }

                        BigButton(
                            text = "Identificar objetos",
                            icon = Icons.Filled.Search
                        ) {
                            screen = "Identificar objetos"
                            speak("Sección de identificación de objetos")
                        }

                        BigButton(
                            text = "Orientación y movilidad",
                            icon = Icons.Filled.Explore
                        ) {
                            screen = "Orientación"
                            speak("Sección de orientación y movilidad")
                        }

                        BigButton(
                            text = "Configuración",
                            icon = Icons.Filled.Settings
                        ) {
                            screen = "Configuración"
                            speak("Sección de configuración")
                        }

                        BigButton(
                            text = "Ayuda",
                            icon = Icons.Filled.Help
                        ) {
                            screen = "Ayuda"
                            speak("Sección de ayuda")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = status,
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )
                    }

                    "Leer texto" -> {
                        Text(
                            text = "Lectura de texto",
                            fontSize = titleSize,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Toma una fotografía de un documento, letrero u objeto con texto. La aplicación intentará reconocerlo y leerlo en voz alta.",
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        BigButton(
                            text = "Abrir cámara y leer",
                            icon = Icons.Filled.CameraAlt
                        ) {
                            val granted = context.checkSelfPermission(
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (granted) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        }

                        BigButton(
                            text = "Repetir lectura",
                            icon = Icons.Filled.VolumeUp
                        ) {
                            if (recognizedText.isNotBlank()) {
                                speak(recognizedText)
                            } else {
                                speak("Todavía no hay texto reconocido.")
                            }
                        }

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Texto reconocido:",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = bodySize
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = cardColor
                            )
                        ) {
                            Text(
                                text = recognizedText.ifBlank {
                                    "Aquí aparecerá el texto detectado por la cámara."
                                },
                                color = White,
                                fontSize = bodySize,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = status,
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )
                    }
                    "Identificar objetos" -> {

                        Text(
                            text = "Identificación de objetos",
                            fontSize = titleSize,
                            color = White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Toma una fotografía para que Apoyo Visual intente reconocer los objetos que aparecen en ella.",
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        BigButton(
                            text = "Tomar fotografía",
                            icon = Icons.Filled.CameraAlt
                        ) {
                            val granted = context.checkSelfPermission(
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (granted) {
                                objectCameraLauncher.launch(null)
                            } else {
                                objectCameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        }

                        BigButton(
                            text = "Repetir descripción",
                            icon = Icons.Filled.VolumeUp
                        ) {
                            if (objectResults.isNotBlank() &&
                                objectResults != "Analizando fotografía..."
                            ) {
                                speak(objectResults)
                            } else {
                                speak("Todavía no hay objetos identificados.")
                            }
                        }

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Resultado:",
                            color = White,
                            fontSize = bodySize,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = cardColor
                            )
                        ) {
                            Text(
                                text = objectResults.ifBlank {
                                    "Aquí aparecerán los objetos identificados."
                                },
                                color = White,
                                fontSize = bodySize,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = status,
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )
                    }

                    "Orientación" -> {
                        Text(
                            text = "Orientación y movilidad",
                            fontSize = titleSize,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Consulta la última ubicación disponible del dispositivo.",
                            color = White,
                            fontSize = bodySize,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        BigButton(
                            text = "Consultar mi ubicación",
                            icon = Icons.Filled.LocationOn
                        ) {
                            requestLocation()
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = cardColor
                            )
                        ) {
                            Text(
                                text = locationText,
                                color = White,
                                fontSize = bodySize,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = "Nota: esta versión muestra la última ubicación conocida. Todavía no proporciona navegación paso a paso ni detección de obstáculos.",
                            color = White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    "Configuración" -> {
                        Text(
                            text = "Personaliza la aplicación",
                            color = White,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Velocidad de voz: ${"%.1f".format(speechRate)}",
                            color = White,
                            fontSize = bodySize
                        )

                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.5f..2.0f
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Volumen de voz: ${(speechVolume * 100).toInt()}%",
                            color = White,
                            fontSize = bodySize
                        )

                        Slider(
                            value = speechVolume,
                            onValueChange = { speechVolume = it },
                            valueRange = 0f..1f
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SettingSwitch(
                            title = "Texto grande",
                            description = "Aumenta el tamaño de los textos.",
                            checked = largeText,
                            onCheckedChange = { largeText = it }
                        )

                        SettingSwitch(
                            title = "Alto contraste",
                            description = "Utiliza un fondo oscuro para mejorar el contraste.",
                            checked = highContrast,
                            onCheckedChange = { highContrast = it }
                        )

                        BigButton(
                            text = "Probar voz",
                            icon = Icons.Filled.VolumeUp
                        ) {
                            speak("Esta es una prueba de lectura de Apoyo Visual.")
                        }
                    }

                    "Ayuda" -> {
                        Text(
                            text = "Centro de ayuda",
                            color = White,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        HelpItem(
                            title = "Leer texto",
                            description = "Permite tomar una fotografía y reconocer las palabras que aparecen en ella."
                        )

                        HelpItem(
                            title = "Orientación",
                            description = "Consulta la última ubicación conocida del teléfono. Necesita permiso de ubicación."
                        )

                        HelpItem(
                            title = "Configuración",
                            description = "Permite modificar la velocidad y el volumen de la voz, activar texto grande y alto contraste."
                        )

                        HelpItem(
                            title = "Accesibilidad",
                            description = "Los controles tienen etiquetas para facilitar su identificación. Puedes complementar su uso con TalkBack."
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        BigButton(
                            text = "Escuchar ayuda",
                            icon = Icons.Filled.VolumeUp
                        ) {
                            speak(
                                "Bienvenido a Apoyo Visual. " +
                                        "Puedes leer texto utilizando la cámara, " +
                                        "consultar tu ubicación y modificar las opciones de voz."
                            )
                        }
                    }
                }
            }
        }
    }
}

// Botón reutilizable
@Composable
fun BigButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Navy
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(25.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Acción accesible reutilizable
@Composable
fun AccessibleAction(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = description,
                color = White,
                fontSize = 16.sp
            )

            TextButton(onClick = onClick) {
                Text("Abrir", color = Accent)
            }
        }
    }
}

// Interruptor reutilizable
@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    color = White,
                    fontSize = 14.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Navy,
                    checkedTrackColor = Accent,
                    checkedBorderColor = Accent,
                    uncheckedThumbColor = Muted,
                    uncheckedTrackColor = Panel,
                    uncheckedBorderColor = Muted
                )
            )
        }
    }
}

// Elemento de ayuda reutilizable
@Composable
fun HelpItem(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = description,
                color = White,
                fontSize = 16.sp
            )
        }
    }
}

// Obtiene la última ubicación conocida
fun getLocationText(context: Context): String {
    val fineGranted = context.checkSelfPermission(
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = context.checkSelfPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
        return "No se tiene permiso para consultar la ubicación."
    }

    return try {
        val locationManager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val providers = locationManager.getProviders(true)

        var bestLocation: Location? = null

        for (provider in providers) {
            val hasPermission =
                if (provider == LocationManager.GPS_PROVIDER) {
                    fineGranted
                } else {
                    fineGranted || coarseGranted
                }

            if (!hasPermission) continue

            val location = locationManager.getLastKnownLocation(provider)

            if (location != null) {
                if (bestLocation == null ||
                    location.accuracy < bestLocation.accuracy
                ) {
                    bestLocation = location
                }
            }
        }

        if (bestLocation != null) {
            "Tu última ubicación conocida es: " +
                    "latitud ${"%.5f".format(bestLocation.latitude)}, " +
                    "longitud ${"%.5f".format(bestLocation.longitude)}."
        } else {
            "No hay una ubicación conocida disponible. Activa el GPS e inténtalo nuevamente."
        }

    } catch (e: SecurityException) {
        "No se pudo acceder a la ubicación. Revisa los permisos."
    } catch (e: Exception) {
        "Error al consultar la ubicación: ${e.message}"
    }
}

fun detectarObjetos(
    context: Context,
    bitmap: Bitmap,
    onResult: (String) -> Unit
) {
    Thread {
        var detector: ObjectDetector? = null

        try {
            detector = ObjectDetector.createFromFile(
                context.applicationContext,
                "efficientdet-lite2.tflite"
            )

            val argbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
                bitmap
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
            val imagen = TensorImage.fromBitmap(argbBitmap)
            val detecciones = detector.detect(imagen)

            val categorias = detecciones
                .flatMap { it.categories }
                .sortedByDescending { it.score }

            // Registrar todas las categorías para diagnosticar errores.
            categorias.forEach { categoria ->
                android.util.Log.d(
                    "ApoyoVisual",
                    "Categoría: ${categoria.label}, " +
                            "Confianza: ${(categoria.score * 100).toInt()}%"
                )
            }

            val mejores = categorias
                .filter { it.score >= 0.55f }
                .filter { it.score >= 0.35f }
                .distinctBy { it.label }
                .take(5)

            val resultado = when {
                mejores.isEmpty() -> {
                    "No pude identificar objetos. " +
                            "Intenta tomar otra fotografía."
                }

                else -> {
                    mejores.joinToString(
                        separator = ". ",
                        prefix = "Esto es lo que pude reconocer: ",
                        postfix = "."
                    ) { categoria ->
                        val nombre = traducirObjeto(categoria.label)
                        val confianza = (categoria.score * 100).toInt()

                        if (categoria.score >= 0.65f) {
                            "$nombre, con una confianza del $confianza por ciento"
                        } else {
                            "Posiblemente $nombre, con una confianza del $confianza por ciento"
                        }
                    }
                }
            }

            Handler(Looper.getMainLooper()).post {
                onResult(resultado)
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "ApoyoVisual",
                "Error al detectar objetos",
                e
            )

            Handler(Looper.getMainLooper()).post {
                onResult("No fue posible identificar los objetos.")
            }
        } finally {
            detector?.close()
        }
    }.start()
}


fun traducirObjeto(label: String): String {
    return when (label.lowercase(Locale.ROOT)) {
        "person" -> "persona"
        "bicycle" -> "bicicleta"
        "car" -> "automóvil"
        "motorcycle" -> "motocicleta"
        "airplane" -> "avión"
        "bus" -> "autobús"
        "train" -> "tren"
        "truck" -> "camión"
        "boat" -> "barco"
        "traffic light" -> "semáforo"
        "bird" -> "pájaro"
        "cat" -> "gato"
        "dog" -> "perro"
        "horse" -> "caballo"
        "backpack" -> "mochila"
        "umbrella" -> "paraguas"
        "handbag" -> "bolso"
        "suitcase" -> "maleta"
        "bottle" -> "botella"
        "cup" -> "taza"
        "fork" -> "tenedor"
        "knife" -> "cuchillo"
        "spoon" -> "cuchara"
        "bowl" -> "tazón"
        "banana" -> "plátano"
        "apple" -> "manzana"
        "orange" -> "naranja"
        "pizza" -> "pizza"
        "donut" -> "dona"
        "cake" -> "pastel"
        "chair" -> "silla"
        "couch" -> "sofá"
        "potted plant" -> "planta en maceta"
        "bed" -> "cama"
        "dining table" -> "mesa"
        "tv" -> "televisor"
        "laptop" -> "computadora portátil"
        "mouse" -> "ratón"
        "remote" -> "control remoto"
        "keyboard" -> "teclado"
        "cell phone" -> "teléfono celular"
        "microwave" -> "microondas"
        "oven" -> "horno"
        "sink" -> "fregadero"
        "refrigerator" -> "refrigerador"
        "book" -> "libro"
        "clock" -> "reloj"
        "vase" -> "jarrón"
        "scissors" -> "tijeras"
        "toothbrush" -> "cepillo de dientes"
        else -> label
    }
}