# City Game Server

Серверная часть игры "Города" с использованием RMI over HTTP.

## Деплой на Railway

1. Создайте аккаунт на [Railway](https://railway.app/)
2. Установите Railway CLI:
   ```bash
   npm i -g @railway/cli
   ```
3. Войдите в аккаунт:
   ```bash
   railway login
   ```
4. Инициализируйте проект:
   ```bash
   railway init
   ```
5. Задеплойте приложение:
   ```bash
   railway up
   ```

После успешного деплоя Railway предоставит URL вашего приложения. Этот URL будет использоваться как `RAILWAY_STATIC_URL` для RMI сервера.

## Локальный запуск

1. Соберите проект:
   ```bash
   mvn clean package
   ```
2. Запустите сервер:
   ```bash
   java -jar target/city-game-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Примечания

- Сервер использует RMI over HTTP для обхода ограничений на нестандартные порты
- По умолчанию используется порт 8080
- Для работы в облаке необходимо настроить переменную окружения `RAILWAY_STATIC_URL` 