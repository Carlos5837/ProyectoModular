# Sistema de Asistencia Móvil para Personas con Discapacidad Visual

## Descripción

Proyecto de desarrollo de una aplicación móvil accesible que busca mejorar la autonomía y la seguridad de las personas con discapacidad visual mediante visión artificial y retroalimentación auditiva. La aplicación utilizará la cámara del dispositivo para reconocer objetos y obstáculos, leer textos impresos mediante reconocimiento óptico de caracteres (OCR) y comunicar la información relevante a través de síntesis de voz.

El sistema contempla una interfaz accesible operable mediante comandos de voz, procesamiento local para reducir la latencia y proteger la privacidad, y compatibilidad prevista con teléfonos inteligentes y, de manera complementaria, lentes inteligentes.

El proyecto se desarrolla en el marco de la asignatura **Laboratorio Abierto: Diseño**, en CUCEI, Universidad de Guadalajara.

## Objetivo general

Desarrollar un prototipo de aplicación móvil accesible para personas con discapacidad visual que permita reconocer objetos y obstáculos, interpretar textos impresos y proporcionar información del entorno mediante retroalimentación auditiva en tiempo real, con el propósito de favorecer la autonomía y facilitar la realización de actividades cotidianas.

## Equipo

* **Technical Mentor / Project Sponsor:** Luis Ernesto Ayala Hernández.
* **Project Leader:** Juan Carlos Monroy Becerra — líder técnico, supervisión de la arquitectura y desarrollo de la aplicación.
* **Integrantes:**

  * **Juan Carlos Monroy Becerra:** liderazgo técnico, arquitectura y supervisión del desarrollo.
  * **Oscar Octavio Gómez Vázquez:** desarrollo técnico principal, implementación de módulos e ingeniería de software.
  * **Elisa Andrés Molina:** documentación, investigación, integración y pruebas del sistema.

**Equipo académico:** A081 – 26A.

## Estado actual

* **Sprint / versión:** Sprint 3 — Consolidación de la construcción y transición hacia Pruebas.
* **Estado general:** Diseño conceptual, arquitectura técnica, alcance del prototipo, criterios de aceptación, metodología de validación y planificación de ejecución documentados.
* **Última actualización documental:** 15 de noviembre de 2026, correspondiente al Documento de Protocolo de Solución.

### Funcionalidades contempladas

* Reconocimiento de objetos y obstáculos mediante visión artificial.
* Lectura de textos impresos mediante OCR.
* Generación de retroalimentación auditiva mediante síntesis de voz.
* Interacción accesible mediante comandos de voz.
* Módulo de asistencia mediante un botón de acceso rápido para situaciones de emergencia.
* Procesamiento local de información para reducir la latencia y mejorar la privacidad.

## Arquitectura y diseño del programa

La aplicación está construida como un proyecto Android en Kotlin, con una
interfaz declarativa basada en Jetpack Compose. El punto de entrada es
`MainActivity`, que inicia el contenido de Compose mediante el composable
`ApoyoVisualApp`. En el estado actual del prototipo, la coordinación de la
interfaz, los permisos y los módulos de asistencia se concentra en
`MainActivity.kt`, mientras que los controles visuales repetibles se separan
en composables auxiliares como botones, acciones, interruptores y elementos de
ayuda.

### Organización de la aplicación

El diseño se organiza alrededor de un estado de pantalla y estados específicos
para cada función:

* **Presentación:** Jetpack Compose construye la pantalla activa y cambia entre
  Inicio, Leer texto, Identificar objetos, Orientación, Configuración y Ayuda.
* **Estado de interacción:** `remember` conserva la pantalla seleccionada, el
  texto reconocido, los resultados de objetos, el estado de la aplicación, la
  configuración visual y los parámetros de voz.
* **Acceso a capacidades del dispositivo:** los contratos de Activity Result
  gestionan la cámara y las solicitudes de permisos de cámara y ubicación.
* **Procesamiento local:** ML Kit procesa el OCR y TensorFlow Lite 2 ejecuta la
  detección de objetos usando modelos incluidos en los assets de la aplicación.
* **Retroalimentación:** Text To Speech comunica estados, resultados y errores
  sin depender de un servidor externo.

### Flujo general de datos

1. La persona selecciona una acción mediante un botón de alto contraste.
2. La aplicación valida el permiso requerido y solicita una captura o el
   acceso a la función correspondiente.
3. El resultado se procesa localmente en el módulo adecuado.
4. El estado de Compose se actualiza para mostrar el resultado y el estado de
   la operación.
5. El mismo resultado se envía al módulo Text To Speech para proporcionar
   retroalimentación auditiva.

Las operaciones que pueden tardar, como la detección de objetos, se ejecutan
fuera del hilo principal y regresan el resultado al hilo de interfaz. Esto
mantiene la interacción fluida y evita bloquear la navegación.

### Principios de diseño

* **Accesibilidad primero:** botones grandes, etiquetas descriptivas, texto
  ampliable, modo de alto contraste y confirmaciones por voz.
* **Interacción simple:** las funciones principales se exponen como acciones
  directas desde la pantalla de inicio y cada sección permite volver a ella.
* **Privacidad y baja latencia:** las imágenes y los modelos se procesan
  localmente; la captura se mantiene en memoria para su análisis y no se
  almacena de forma permanente.
* **Manejo explícito de estados:** la interfaz comunica cuando una operación
  está iniciando, procesando, finalizando o ha encontrado un error.
* **Diseño modular:** la captura, el OCR, la detección de objetos y la síntesis
  de voz tienen responsabilidades diferenciadas, aunque actualmente se
  coordinan desde la actividad principal.

## Módulos principales

### Captura y adquisición de imágenes

La aplicación utiliza el permiso `android.permission.CAMERA` y el contrato
`ActivityResultContracts.TakePicturePreview()` para solicitar una fotografía
directamente desde la cámara del dispositivo. La imagen se recibe como un
`Bitmap` en memoria y se utiliza inmediatamente para su análisis; este flujo no
guarda una copia permanente de la fotografía.

La captura se puede iniciar desde dos acciones de la interfaz:

* **Abrir cámara y leer:** envía el `Bitmap` al módulo de OCR.
* **Tomar fotografía:** envía el `Bitmap` al detector de objetos
  `efficientdet-lite2.tflite`, incluido en `app/src/main/assets/`.

Antes de abrir la cámara se comprueba el permiso. Si el usuario lo rechaza o
no se obtiene una imagen, la aplicación actualiza el estado y comunica el
problema mediante voz. El análisis de objetos se ejecuta en un hilo de trabajo
y publica el resultado en el hilo principal para no bloquear la interfaz.

### Reconocimiento óptico de caracteres (OCR)

El flujo de lectura convierte el `Bitmap` capturado en un `InputImage` con
rotación cero y crea un cliente de ML Kit mediante
`TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`. El
reconocedor procesa la imagen de forma asíncrona:

1. Se muestra el estado **Analizando imagen...**.
2. Si se detecta texto, se conserva en pantalla y se cambia el estado a
   **Texto reconocido correctamente.**
3. El texto encontrado se envía al módulo de voz para su lectura.
4. Si no se detecta texto, se informa al usuario y se limpia el resultado
   anterior.
5. Si ocurre un error, se muestra el mensaje correspondiente y también se
   comunica por voz.

El cliente de OCR se cierra al finalizar el procesamiento, tanto si la
operación termina correctamente como si falla.

### Implementación de Text To Speech

La retroalimentación auditiva se implementa con la clase Android
`android.speech.tts.TextToSpeech`, inicializada con el contexto de la
aplicación. Mientras el motor termina de iniciar, el último mensaje solicitado
se conserva como pendiente y se reproduce automáticamente cuando el motor está
listo.

La configuración del módulo es la siguiente:

* Idioma preferido: español de México (`es-MX`), con fallback a español
  (`es`) si no está disponible.
* Uso de audio: asistencia de accesibilidad y contenido de voz.
* Cola: `QUEUE_FLUSH`, por lo que un mensaje nuevo reemplaza la reproducción
  anterior.
* Velocidad configurable entre `0.5x` y `2.0x`.
* Volumen configurable entre `0 %` y `100 %`.

El módulo informa cuando no hay una voz en español instalada, cuando la
reproducción no puede iniciarse y cuando se produce un error. Al abandonar la
pantalla, se detiene la reproducción y se libera el motor con `shutdown()`.

### Criterios iniciales de aceptación

* Precisión objetivo del 70 % en el reconocimiento de objetos bajo condiciones normales de iluminación.
* Éxito mínimo del 80 % en pruebas de lectura de caracteres.
* Tiempo de respuesta objetivo de hasta 2 segundos entre la captura y la salida de audio.
* Operación de las funciones principales mediante una interfaz no visual.

## Estructura del repositorio

* `docs/`: documentación del proyecto, requerimientos, decisiones de diseño, arquitectura y reportes de los sprints.
* `src/`: código fuente de la aplicación y sus módulos.
* `tests/`: pruebas unitarias, pruebas de integración y validaciones funcionales.
* `assets/`: recursos gráficos, archivos de configuración y otros recursos necesarios para el desarrollo.
* `README.md`: descripción general, estado del proyecto e instrucciones de consulta.
* `.gitignore`: archivos y directorios que Git debe excluir del control de versiones.

## Cómo consultar o reproducir el proyecto

### Consulta de la documentación

1. Clonar el repositorio de GitHub o descargar sus archivos.
2. Abrir el proyecto en Visual Studio Code o en un editor compatible.
3. Consultar la documentación disponible en `docs/`, incluyendo la ficha de arranque, el diseño conceptual, la arquitectura técnica, el plan de construcción y el protocolo de solución.

### Reproducción del prototipo

## Versiones principales

* `design-baseline-v1.0`: versión de referencia del diseño y de las decisiones conceptuales del proyecto.
* `sprint-1-plan-baseline-v1.0`: versión de referencia de la planificación inicial del proyecto.
* `construction-handover-v1.0`: versión de referencia para la entrega de la documentación y los elementos correspondientes a la construcción del prototipo.

