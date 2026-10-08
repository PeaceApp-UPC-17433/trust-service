# trust-service

BC-03 *Trust & Moderation* de PeaceApp: detección de duplicados por proximidad
espacio-temporal, clasificación de reportes falsos, cola de moderación, decisiones
y apelaciones. Implementa US-14 a US-17 del informe (ver `PeaceApp-Report`,
secciones 1.3.4 y 2.4).

## Requisitos

- Java 21, Maven 3.9+
- Docker (opcional, para levantar junto a Postgres)

## Ejecutar en local

```bash
mvn spring-boot:run
```

Usa H2 en memoria por defecto. Con Postgres real: `docker compose up --build`.
El servicio queda en `http://localhost:8083`. Swagger UI en `/swagger-ui.html`.

Requiere un JWT emitido por `identity-service` en el header `Authorization: Bearer <token>`
(mismo `JWT_SECRET` en los tres servicios — ver limitaciones abajo).

## Endpoints

| Método | Ruta | Historia | Descripción |
| --- | --- | --- | --- |
| POST | /api/v1/trust/evaluate | US-14, US-15 | Evaluar un incidente recién reportado |
| GET | /api/v1/trust/cases/{id} | — | Detalle de un caso |
| GET | /api/v1/trust/moderation-queue | US-16 | Cola de moderación (rol MODERADOR/ADMINISTRADOR) |
| POST | /api/v1/trust/moderation-queue/{id}/decision | US-16 | Aprobar o rechazar un caso |
| POST | /api/v1/trust/cases/{id}/appeal | US-17 | Apelar un caso rechazado (autor, dentro de 7 días) |

## Limitaciones conocidas (pendientes de diagramas/ADR)

- **No hay modelo de clasificación real.** `falsenessScore` es una heurística
  provisional (ausencia de descripción/evidencia y reputación negativa del autor);
  debe reemplazarse por el modelo mencionado en la sección 1.1.4 del informe.
- **Integración sincrónica, no por eventos.** `incident-service` debería invocar
  `/api/v1/trust/evaluate` justo después de crear el reporte; esta llamada aún no
  está implementada en `incident-service` y el broker de eventos de CA-01 (para
  publicar `ReportCleared`, `DuplicateLinked`, etc.) tampoco está definido.
- La validación del JWT usa un secreto HMAC compartido por configuración, como
  sustituto temporal de la propagación de claims por el API Gateway (CA-04).
- La detección de duplicados compara contra todos los casos del mismo tipo en la
  ventana de tiempo; falta paginar/indexar geoespacialmente para escalar (RNF-10).
