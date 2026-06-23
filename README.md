# KMAhoot!

Kahoot-подібний квіз-застосунок із самостійно реалізованим Java-бекендом і React-фронтендом.

## Стек

| Частина | Технології |
|---|---|
| Бекенд | Java 21, Maven, SQLite (`sqlite-jdbc`) |
| Фронтенд | React 19, TypeScript, Vite, Tailwind CSS 4 |
| Транспорт | Власний бінарний протокол поверх WebSocket (браузер) і TCP (окремий клієнт) |
| API | HTTP (стандартний `com.sun.net.httpserver`) |

## Архітектура

```
Браузер
 ├── WebSocket :9092  →  SessionDispatcher  →  GameService
 └── HTTP      :8090  →  HttpApiServer      →  QuizDAO / UserDAO

Окремий клієнт
 └── TCP       :9090  →  SessionDispatcher  →  GameService
                                               ↓
                                            SQLite (kahoot.db)
```

Бекенд не використовує жодних фреймворків — тільки JDK і `sqlite-jdbc`.

## Запуск

### Бекенд

```bash
# від кореня проєкту
mvn compile
mvn exec:java
```

Сервер запуститься на трьох портах:

| Порт | Призначення |
|---|---|
| `9090` | TCP (власний клієнт) |
| `9092` | WebSocket (браузер) |
| `8090` | HTTP REST API |

Порти можна перевизначити через змінні середовища `TCP_PORT`, `WS_PORT`, `HTTP_PORT`.

### Фронтенд

```bash
cd frontend
npm install       # один раз
npm run dev       # http://localhost:5173
```

### Змінні середовища фронтенду (`.env`)

```
VITE_WS_URL=ws://localhost:9092
VITE_API_URL=http://localhost:8090
```

За замовчуванням вже вказано ці значення, файл `.env` потрібен лише при зміні портів.

## Демо-дані

При першому запуску бекенд автоматично створює:

| Логін | Пароль | Роль |
|---|---|---|
| `admin` | `admin` | Адміністратор |
| `player` | `player` | Гравець |

А також три демо-вікторини: **Мережеві протоколи**, **Основи програмування**, **Бази даних**.

## Ігровий процес

1. Ведучий (будь-який авторизований користувач) відкриває дашборд → вибирає вікторину → **Запустити**.
2. Гравці переходять на головну сторінку, вводять PIN і нікнейм.
3. Ведучий натискає **Почати гру** — кожен гравець проходить питання у власному темпі.
4. За швидку правильну відповідь нараховується бонус до базових 1000 очок.
5. Ведучий бачить таблицю лідерів у реальному часі; по натисканню **Завершити гру** всі гравці отримують повідомлення про завершення сесії.

## Структура проєкту

```
├── src/main/java/kahoot/
│   ├── db/          — DAO-шар (Quiz, User, GameHistory)
│   ├── game/        — ігрова логіка (GameSession, GameService, ScoringEngine)
│   ├── model/       — Quiz, Question, Answer, User
│   ├── net/         — TCP-сервер, ConnectionRegistry, SessionDispatcher
│   ├── protocol/    — бінарний протокол (Packet, Message, CRC-16, шифр)
│   └── web/         — WebSocket-сервер, HttpApiServer
└── frontend/src/
    ├── net/         — gameClient (WS), api (HTTP), протокол
    └── pages/       — Auth, Dashboard, Lobby, Game, HostView, QuizEditor
```

## Збірка та тести

```bash
mvn test          # JUnit-тести бекенду
mvn package       # зібрати JAR у target/

npm run build     # зібрати фронтенд у frontend/dist/
npm run lint      # ESLint
```
