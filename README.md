# Bank Cards API

REST API для управления банковскими картами с ролями **ADMIN** и **USER**,  
аутентификацией через **JWT** и документированием через **Swagger UI**.

Repository:  
https://github.com/ktmn21/Bank_rest

---

# Функционал

## Аутентификация
- Регистрация пользователей
- Логин через JWT
- Получение текущего пользователя

---

## Роли

### ADMIN
- создаёт карты
- блокирует карты
- активирует карты
- удаляет карты
- управляет пользователями
- видит все карты

### USER
- управляет своими картами
- может инициировать переводы
- может запрашивать блокировку карты

---

# Операции с картами

- Создание карты администратором
- Просмотр всех карт (**ADMIN**)
- Просмотр своих карт (**USER**)
- Блокировка карты (**ADMIN**)
- Активация карты (**ADMIN**)
- Удаление карты (**ADMIN**)
- Запрос на блокировку своей карты (**USER**)

---

# Управление пользователями

Доступно только для **ADMIN**

- создание пользователя
- получение списка пользователей
- получение пользователя по ID
- изменение роли пользователя
- удаление пользователя

---

# Переводы

Пользователь может выполнять переводы **между своими картами**.

---

# Стек технологий

- Java 17
- Spring Boot 3.x
- Spring Security 6 + JWT
- Spring Data JPA (Hibernate)
- PostgreSQL
- Maven
- Swagger / springdoc-openapi
- Docker

---

# Запуск проекта

## Клонирование репозитория

```bash
git clone https://github.com/ktmn21/Bank_rest.git
cd Bank_rest
```

---

## Настройка базы данных

В `application.yml` или `application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/bankcards
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
```

---

## Запуск приложения

```bash
mvn clean spring-boot:run
```

---

## Swagger UI

После запуска:

```
http://localhost:8080/swagger-ui/index.html
```

---

# Запуск через Docker

Если добавлен **Dockerfile**:

```bash
mvn clean package -DskipTests
docker build -t bank-cards-api .
docker run -p 8080:8080 --name bank-cards-api bank-cards-api
```

Swagger будет доступен по адресу:

```
http://localhost:8080/swagger-ui/index.html
```

---

# Аутентификация и JWT

Базовый URL:

```
/api/auth
```

---

# Регистрация

### POST `/api/auth/register`

### Request Body

```json
{
  "username": "user1",
  "email": "user1@example.com",
  "password": "password123"
}
```

### Response

```
200 OK
User registered successfully
```

или сообщение об ошибке, если **username/email уже заняты**.

---

# Логин

### POST `/api/auth/login`

### Request Body

```json
{
  "username": "user1",
  "password": "password123"
}
```

### Response

```json
{
  "token": "<JWT_TOKEN>",
  "username": "user1"
}
```

---

# Текущий пользователь

### GET `/api/auth/me`

Требует **Bearer Token**

### Response

```json
{
  "id": 1,
  "username": "user1",
  "email": "user1@example.com",
  "role": "USER",
  "createdAt": "2026-03-10T17:26:57.792236"
}
```

---

# Использование JWT в Swagger

1. Получить токен через `/api/auth/login`
2. В Swagger нажать **Authorize**
3. Вставить:

```
Bearer <JWT_TOKEN>
```

4. Нажать **Authorize**

После этого все защищённые эндпоинты будут работать.

---

# Управление пользователями (ADMIN)

Базовый URL:

```
/api/users
```

Все эндпоинты требуют **ADMIN**.

---

## Создать пользователя

### POST `/api/users`

Query параметры:

```
role (optional) = USER | ADMIN
```

### Body

```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}
```

### Response

```
201 Created
```

---

## Получить всех пользователей

### GET `/api/users`

Поддерживает **pagination**

```
page
size
sort
```

---

## Получить пользователя по ID

### GET `/api/users/{id}`

Response:

```
200 OK
```

или

```
404 Not Found
```

---

## Обновить роль пользователя

### PATCH `/api/users/{id}/role`

Query параметр:

```
role = ADMIN | USER
```

---

## Удалить пользователя

### DELETE `/api/users/{id}`

Response

```
204 No Content
```

---

# Управление картами

Базовый URL:

```
/api/cards
```

---

# ADMIN — создать карту

### POST `/api/cards`

```json
{
  "cardNumber": "1234567812345678",
  "ownerId": 1,
  "expiryDate": "2028-12-31",
  "initialBalance": 0.00
}
```

Response

```
201 Created
```

---

# ADMIN — получить все карты

### GET `/api/cards`

Поддерживает:

```
page
size
sort
```

---

# USER — получить свои карты

### GET `/api/cards/my`

Query параметры:

```
status (optional)
page
size
sort
```

Возвращает **только карты текущего пользователя**.

---

# ADMIN — заблокировать карту

### PATCH `/api/cards/{id}/block`

Response

```
200 OK
```

---

# ADMIN — активировать карту

### PATCH `/api/cards/{id}/activate`

Response

```
200 OK
```

---

# USER — запросить блокировку карты

### PATCH `/api/cards/{id}/request-block`

Использует `@AuthenticationPrincipal` для проверки владельца карты.

---

# ADMIN — удалить карту

### DELETE `/api/cards/{id}`

Response

```
204 No Content
```

---

# Переводы между картами

Базовый URL:

```
/api/transfers
```

---

# Создать перевод

### POST `/api/transfers`

### Body

```json
{
  "fromCardId": 1,
  "toCardId": 2,
  "amount": 100.00
}
```

Контроллер проверяет владельца карты через:

```
@AuthenticationPrincipal
```

### Response

```
Transfer successful
```

Логика перевода реализована в **TransferService.transfer**.

---

# Автор

**Kutman Mukarapov**

GitHub:  
https://github.com/ktmn21
