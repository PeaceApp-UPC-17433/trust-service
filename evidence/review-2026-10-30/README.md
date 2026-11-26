# Verificación de trust-service

Ejecución local real del 30 de octubre de 2026, Java 21. Las métricas se extrajeron de los XML de Surefire y del CSV de JaCoCo; no son valores de ejemplo.

- Pruebas: 13; fallos: 0; errores: 0.
- Cobertura global de líneas: 72.93 %.
- Cobertura de líneas del paquete application: 100.0 %.
- Control bloqueante: 70 % de líneas del paquete application, que contiene las reglas de negocio. No es un umbral global ni de ramas.
- Contenedor: construcción multietapa, PostgreSQL 16, salud healthy/UP y UID 100; detalles en container-verification.json.

Reproducir con `mvn -B -ntp clean verify`, seguido de `docker compose up --build --wait --wait-timeout 180 -d`. Los reportes completos se generan en target/surefire-reports y target/site/jacoco. El workflow adjunta esos directorios como artefactos durante 30 días. La configuración Compose incluida utiliza credenciales exclusivas del entorno local de desarrollo.

test-verification.json incluye nombres de casos y una huella SHA-256 de pom.xml y del código Java usado. container-verification.json corresponde al árbol de trabajo de esta PR. No acredita un despliegue cloud ni pruebas funcionales end-to-end.
