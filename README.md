La Marcha del Caballo Loco
Juego de lógica del recorrido del caballo – Android / Kotlin

La Marcha Caballo Loco es un juego de lógica para Android inspirado en el clásico problema del recorrido del caballo de ajedrez.
El objetivo es visitar todas las casillas del tablero utilizando únicamente movimientos válidos del caballo, optimizando movimientos, gestionando bonus y superando niveles con restricciones crecientes.

Proyecto desarrollado como práctica personal para profundizar en lógica de juegos, manejo de estado, UI dinámica y desarrollo Android nativo con Kotlin.

-- Características principales

Tablero 8x8 generado dinámicamente

Validación completa de movimientos del caballo

Sistema de bonus por casillas especiales

Indicador visual de movimientos disponibles

Temporizador de partida

Barra de progreso animada

Progresión de niveles (reducción de movimientos permitidos)

Integración de anuncios nativos (Google Mobile Ads)

Captura de pantalla de la partida

Función para compartir el juego

--Lógica del juego

El tablero se gestiona mediante una matriz de estados

Cada celda puede encontrarse vacía, visitada, como opción de movimiento o como bonus

Los movimientos válidos se calculan dinámicamente según la posición actual del caballo

El juego finaliza por:

Falta de movimientos disponibles

Tiempo agotado

Recorrido completo del tablero

-- Tecnologías utilizadas

Kotlin

Android SDK

XML (View System)

Handlers / Runnables (temporizador)

ValueAnimator (animaciones de progreso)

Canvas & Bitmap (captura de pantalla)

Google Mobile Ads (Native Ads)

-- Capturas de pantalla

(Pendiente de agregar imágenes del juego en ejecución)

-- Estado del proyecto

Proyecto personal en desarrollo, utilizado como ejercicio práctico para consolidar conocimientos en programación Android con Kotlin.
El foco principal está en la lógica del juego, el manejo de estado y la interacción con la interfaz de usuario.

-- Licencia

Este proyecto se publica bajo la licencia MIT.
Ver el archivo LICENSE para más detalles.

Desarrollado por Dami Pica
Proyecto personal de aprendizaje y práctica en desarrollo de software.
