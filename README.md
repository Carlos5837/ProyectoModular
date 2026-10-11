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

* **Sprint / versión:** Sprint 1 — Plan de construcción.
* **Estado general:** Diseño conceptual, arquitectura técnica, alcance del prototipo, criterios de aceptación, metodología de validación y planificación de ejecución documentados.
* **Última actualización documental:** 10 de mayo de 2026, correspondiente al Documento de Protocolo de Solución.

### Funcionalidades contempladas

* Reconocimiento de objetos y obstáculos mediante visión artificial.
* Lectura de textos impresos mediante OCR.
* Generación de retroalimentación auditiva mediante síntesis de voz.
* Interacción accesible mediante comandos de voz.
* Módulo de asistencia mediante un botón de acceso rápido para situaciones de emergencia.
* Procesamiento local de información para reducir la latencia y mejorar la privacidad.

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
