# DeepBlue Rescue

API REST para administrar rescates de fauna marina, animales en rehabilitación y tratamientos. La aplicación expone los casos de rescate, los animales y sus tratamientos mediante controladores Spring MVC. El Service contiene las reglas del negocio, los Repository acceden a PostgreSQL y Flyway administra el esquema.

## Tecnologías

- Java 21
- Spring Boot 4.1.1 y Spring MVC
- Spring Data JPA e Hibernate
- PostgreSQL y Flyway
- Bean Validation
- MapStruct
- JUnit 5, Mockito, MockMvc y Testcontainers

## Requisitos previos

- JDK 21
- PostgreSQL en ejecución
- Docker disponible para los tests de integración que usan Testcontainers
- REST Client de Huachao Mao en VS Code, si se desea ejecutar el archivo `.http`

## Base de datos

La configuración predeterminada apunta a `localhost:5432/deepblue`, con usuario `postgres` y contraseña `postgres`. Para crear la base desde pgAdmin, conéctate a la base `postgres`, abre **Tools → Query Tool** y ejecuta:

```sql
CREATE DATABASE deepblue OWNER postgres;
```

Al iniciar la aplicación, Flyway aplica automáticamente las migraciones que crean el esquema. Hibernate valida el esquema y no lo genera (`ddl-auto: validate`).

Se pueden cambiar las credenciales y la base mediante las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`. Ejemplo en PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/deepblue"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
```

Las migraciones V1, V2 y V3 son versionadas. No edites una migración que ya fue aplicada a una base: crea una migración nueva. Si Flyway informa un checksum distinto, comprueba que estás usando la base prevista; no ejecutes `repair` sin verificar antes el esquema.

Las migraciones no insertan el caso, animal ni especialista del reto integrador. Las operaciones 1–6 del archivo `.http` requieren que esos datos ya existan en la base. La operación 7, que busca `AN-999`, puede probarse sin ellos.

## Ejecutar

Desde PowerShell, en la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

El proceso mantiene el servidor activo; no esperes `BUILD SUCCESS` mientras corre. Confirma el arranque buscando `Started DeepblueRescueApplication` o `Tomcat started on port 8080`. Para detenerlo, presiona `Ctrl+C` en esa terminal y confirma con `S` si PowerShell lo pregunta.

## API

| Método | Endpoint | Función | Respuesta normal |
| --- | --- | --- | --- |
| GET | `/api/rescue-cases/{caseCode}` | Consultar un caso por código | 200 |
| GET | `/api/rescue-cases?status=IN_REHABILITATION` | Buscar casos por estado | 200 |
| PATCH | `/api/rescue-cases/{caseCode}/status` | Cambiar el estado del caso | 200 |
| POST | `/api/treatments` | Registrar un tratamiento | 201 |
| GET | `/api/animals/{animalCode}` | Consultar un animal | 200 |
| GET | `/api/animals/in-rehabilitation` | Listar animales en rehabilitación | 200 |
| GET | `/api/animals/{animalCode}/treatments` | Listar tratamientos de un animal | 200 |
| GET | `/api/animals/{animalCode}/treatment-eligibility` | Consultar elegibilidad para tratamiento | 200 |

El PATCH recibe JSON como `{"status":"READY_FOR_RELEASE"}`. La validación de las peticiones de cambio de estado y registro de tratamiento usa Bean Validation.

### Elegibilidad para tratamiento

`GET /api/animals/{animalCode}/treatment-eligibility` devuelve el código y el resultado calculado por `AnimalService`:

```json
{
  "animalCode": "AN-2026-100",
  "eligible": true
}
```

Un código de animal inexistente produce `404`.

## Errores HTTP

`GlobalExceptionHandler` devuelve un `ErrorResponse` común:

```json
{
  "timestamp": "2026-10-07T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Animal not found: AN-999",
  "details": {}
}
```

| Estado | Situación |
| --- | --- |
| 400 Bad Request | Bean Validation, JSON inválido o parámetro de consulta inválido |
| 404 Not Found | Recurso inexistente (`ResourceNotFoundException`) |
| 409 Conflict | Regla del negocio incumplida (`BusinessRuleException`) |
| 500 Internal Server Error | Excepción inesperada; no se revelan detalles internos |

## Probar

Compilar el proyecto:

```powershell
.\mvnw.cmd clean compile
```

Ejecutar solo los tests de controladores:

```powershell
.\mvnw.cmd "-Dtest=AnimalControllerTest,RescueCaseControllerTest,TreatmentControllerTest" test
```

Ejecutar la suite completa:

```powershell
.\mvnw.cmd clean test
```

Los tests MVC usan `@WebMvcTest`, `@MockitoBean` y `MockMvc`; no necesitan PostgreSQL. Los tests de persistencia usan Testcontainers y necesitan Docker.

## Reto HTTP

El archivo [`reto-integrador.http`](src/test/java/com/deepblue/rescue/controller/reto-integrador.http) contiene siete peticiones del escenario. Instala la extensión REST Client, inicia la aplicación, abre el archivo y pulsa **Send Request** sobre cada operación, en orden. Para repetir el flujo, restablece el caso a `IN_REHABILITATION` antes de la operación 4. El paso 90 (`GET /api/animals/AN-999`) debe devolver `404 Not Found`.

## Respuestas del laboratorio

### Punto 21: validación de entrada o regla del negocio

- `animalCode` vacío: DTO/Bean Validation.
- `status == null`: DTO/Bean Validation.
- Animal inexistente: Service.
- Especialista inactivo: Service.
- Descripción con más de 500 caracteres: DTO/Bean Validation.
- Caso en estado `RELEASED`: Service.
- Transición `ADMITTED → RELEASED`: Service.

### Puntos 92–94: diseño REST

- `GET /api/animals/AN-001` comunica mejor el recurso que `GET /api/getAnimal?id=1`: usa el nombre del recurso y su identificador en la ruta.
- `PATCH /api/rescue-cases/RES-001/status` expresa una modificación parcial; no reemplaza todo el caso.
- `/api/animals/AN-001/treatments` expresa la relación de pertenencia entre animal y tratamientos. `/api/treatments?animal=AN-001` expresa una búsqueda filtrada. Ambos diseños son válidos.

### Punto 95–99: responsabilidades y anti-patrones

- El Controller no usa Repository directamente, no aplica reglas de negocio y no devuelve entidades JPA; delega al Service y usa DTOs.
- Los errores se transforman en un único `@RestControllerAdvice`, no mediante `try/catch` repetidos en cada endpoint.
- Se distinguen los estados `200`, `201`, `400`, `404`, `409` y `500` según el resultado.

### Punto 100: asignación de responsabilidades

| Necesidad | Capa |
| --- | --- |
| Recibir JSON y devolver `201` | Controller |
| Verificar `@NotBlank` | DTO Validation |
| Buscar un animal y comprobar que el especialista esté activo | Service |
| Ejecutar una query | Repository |
| Convertir una excepción de negocio en `409` | ControllerAdvice |
| Abrir la transacción | Service |

### Puntos 101–104: pruebas por capa

- Repository: persistencia real con PostgreSQL/Testcontainers.
- Service: reglas de negocio con repositorios mockeados mediante Mockito.
- Controller: contrato HTTP con el Controller real, Service mock, `@WebMvcTest` y MockMvc.
- Cada test de Controller verifica rutas, método HTTP, validación, código de respuesta, JSON y delegación; no prueba internamente cómo el Service consulta la base.

### Punto 105: Service → Controller → test

| Método del Service | Endpoint/controlador | Test representativo |
| --- | --- | --- |
| `RescueCaseService.findByCode` | GET `/api/rescue-cases/{caseCode}` | `shouldReturnRescueCaseByCode` |
| `RescueCaseService.findByStatus` | GET `/api/rescue-cases?status=...` | `shouldReturnCasesByStatus` |
| `RescueCaseService.changeStatus` | PATCH `/api/rescue-cases/{caseCode}/status` | `shouldChangeRescueCaseStatus` |
| `TreatmentService.register` | POST `/api/treatments` | `shouldCreateTreatment` |
| `TreatmentService.findByAnimalCode` | GET `/api/animals/{animalCode}/treatments` | `shouldReturnAnimalTreatments` |
| `AnimalService.findByCode` | GET `/api/animals/{animalCode}` | `shouldReturnAnimalByCode` |
| `AnimalService.findAnimalsInRehabilitation` | GET `/api/animals/in-rehabilitation` | `shouldReturnAnimalsInRehabilitation` |
| `AnimalService.canReceiveTreatment` | GET `/api/animals/{animalCode}/treatment-eligibility` | `shouldReturnTreatmentEligibility` |

### Punto 106: excepción y estado HTTP

- DTO inválido → 400, `MethodArgumentNotValidException`.
- JSON inválido → 400, `HttpMessageNotReadableException`.
- Parámetro de consulta inválido → 400, `MethodArgumentTypeMismatchException`.
- Recurso inexistente → 404, `ResourceNotFoundException`.
- Regla de negocio → 409, `BusinessRuleException`.
- Error inesperado → 500, `Exception`.

### Punto 107: casos mínimos de prueba

Los tests cubren: GET de caso existente e inexistente; búsqueda por estado válido e inválido; PATCH válido, inválido y transición no permitida; POST de tratamiento válido, inválido, con recurso inexistente y con conflicto; las cuatro consultas de Animal; animal inexistente; enum inválido en JSON; y excepción inesperada 500.

### Punto 113: preguntas de sustentación

1. El Controller recibe la petición HTTP, obtiene parámetros/body, delega y define la respuesta HTTP.
2. El Controller traduce HTTP; el Service decide si la operación está permitida por el negocio.
3. El acceso directo al Repository acopla la capa HTTP a persistencia y salta las reglas del Service.
4. `@RestController` combina `@Controller` y `@ResponseBody`; los retornos se serializan como respuesta.
5. `@RequestMapping` define una ruta base común para los endpoints.
6. `@PathVariable` obtiene un segmento de la ruta; `@RequestParam`, un parámetro de consulta.
7. `@RequestBody` convierte el cuerpo JSON al objeto Java indicado.
8. `@Valid` activa las restricciones Bean Validation del objeto recibido.
9. La validación comprueba la forma del input; el Service comprueba reglas de negocio.
10. `GET` consulta recursos sin modificar el estado.
11. `POST` crea un recurso.
12. `PATCH` modifica parcialmente un recurso.
13. `200` indica que la petición tuvo éxito.
14. `201` indica que se creó un recurso.
15. `400` indica una petición inválida.
16. `404` indica que el recurso no existe.
17. `409` indica un conflicto con una regla del negocio.
18. `500` indica un error inesperado del servidor.
19. `ResponseEntity` permite controlar estado, headers y cuerpo de respuesta.
20. `@RestControllerAdvice` centraliza el manejo de excepciones de los controladores.
21. Un `ErrorResponse` común ofrece un contrato uniforme a los clientes.
22. `details` agrega información específica, por ejemplo errores por campo.
23. `MethodArgumentNotValidException` representa input que incumple constraints; `BusinessRuleException`, una operación no permitida por negocio.
24. `@WebMvcTest` prueba el slice MVC: rutas, binding, validación y serialización.
25. El Service mock aísla el contrato HTTP de las reglas y la persistencia.
26. MockMvc simula peticiones HTTP sin iniciar un servidor real.
27. No se necesita PostgreSQL porque el Service se sustituye por un mock.
28. `verify(..., never())` confirma que una entrada inválida no llegó al Service.
29. Las transacciones pertenecen al Service.
30. El Service decide si una transición de estado es válida.

## Arquitectura final

```text
HTTP → Controller → DTO/validación → Service/reglas de negocio → Repository → PostgreSQL
                         ↑                                      ↓
HTTP ← ResponseEntity ← Response DTO ←──────────────────────────┘
```

El Controller traduce HTTP hacia la aplicación. El Service implementa las reglas del negocio. El Repository accede a los datos. `GlobalExceptionHandler` transforma las excepciones en respuestas HTTP consistentes.