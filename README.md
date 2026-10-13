# To Do (Do) List

A full-stack to-do app with friends, chat and shared tasks. Add your own tasks, send tasks to your friends, chat with them, and get notified when something happens — all with two switchable art styles and a little confetti when you finish something.

- **Frontend:** Nuxt 4 (Vue 3) in [`frontend/`](frontend)
- **Backend:** Spring Boot 3 (Java 21) REST API in [`backend/`](backend)
- **Database:** MySQL

## Features

**Layout**
- A sidebar with every section (My Board, Friends Tasks, Friends, Community, Settings), notification badges, the Global Clicker and your account. On smaller screens it shrinks to icons.
- A quick-add bar at the top of the board: type a title and press Enter (or press **N** anywhere to jump to it). The **⋯** button adds a description, priority, category and due date.
- Click any task to open its **detail panel**: edit the title, description, priority, category and due date (changes save automatically), manage its steps, complete or delete it. **Esc** closes it.

**Tasks**
- Tasks have a description, due date, priority (🔴 High / 🟡 Medium / 🟢 Low) and category
- Filter by priority or category, and sort by newest, oldest, due date, name or priority
- **Steps (checklists):** break a task into steps in its detail panel. The board shows the progress (e.g. ☑ 2/5), and finishing the last step gives a little confetti. Double-click a step to rename it.
- Due-date warnings: orange **Due today** and red **Overdue** labels
- Completing a task moves it to a **Done** tab (with an **Undo** button for 5 seconds), and the board shows how many tasks you finished this week

**Friends**
- Send, accept, decline and cancel friend requests; remove friends
- Chat with friends, with **Sent ✓ / Seen ✓✓** read receipts
- **Friends Tasks:** send tasks to a friend's list, see the tasks friends sent you, and get notified when a friend finishes a task you sent them

**Community Tasks**
- A shared wall where anyone can post a task (with description, priority and category)
- Any other user can complete it; the first one to finish it gets the credit (you can't complete your own posts)
- Remove your own open posts, see who finished the ones you posted, and get notified when it happens
- Completed tasks stay on the wall for 7 days

**Notifications**
- Red badges for friend requests, unread messages, new tasks from friends, completed tasks and finished community posts, updated every few seconds

**Look and feel**
- Two art styles, switchable in Settings: **Glass** (dark, neon, frosted panels) and **Neo-Brutalism** (bold outlines, hard shadows, bright colors)
- Accent color picker, animated particle backgrounds, and confetti / neon sparks when you complete a task
- A shared **Global Clicker** that every user can push up together

**Account**
- Sign up, log in, change username or password (passwords are stored hashed with BCrypt)
- Each browser tab remembers its own logged-in user, so you can test with two accounts side by side

## Project structure

```
ToDoDoList/
├── frontend/            Nuxt app
│   ├── app/pages/       Pages: landing, login, sign-up, board, friends, friends tasks, community, settings
│   ├── app/layouts/     "app" layout with the sidebar (used after logging in), default layout without it
│   ├── app/components/  Sidebar, particle background, notification badge
│   ├── app/composables/ Login state, settings, notifications, confetti, task helpers
│   ├── app/plugins/     Particles, settings and notification loaders
│   └── app/assets/css/  Neo-Brutalism theme
└── backend/             Spring Boot API
    └── src/main/java/org/example/
        ├── *Controller.java   REST endpoints (auth, tasks, community, friends, notifications, counter)
        ├── *Repository.java   Database queries (Spring Data JPA)
        └── Task, Subtask, CommunityTask, User, Friendship, Message, Counter   Database tables
```

## Requirements

- [Node.js](https://nodejs.org/) 20 or newer
- [Java](https://adoptium.net/) 21
- [Maven](https://maven.apache.org/) (or open `backend/` in IntelliJ IDEA, which includes it)
- [MySQL](https://dev.mysql.com/downloads/) 8 running on `localhost:3306`

## Getting started

### 1. Create the database

In MySQL, create an empty database. The tables are created automatically when the backend starts.

```sql
CREATE DATABASE FullStackProj;
```

The backend connects as user `root` with an empty password. If your MySQL setup is different, change these lines in [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/FullStackProj?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

Or open the `backend` folder in IntelliJ IDEA and run `Main`. The API starts on **http://localhost:8080**. Wait for the line `Started Main in ... seconds`.

> After changing backend code, stop and restart it — a running backend does not pick up new code.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:3000**, click **Let's Go**, and sign up.

To try the friends features, sign up a second account in another tab or browser, add each other as friends, and send a task or a message.

## API overview

All endpoints are under `http://localhost:8080/api`.

| Area | Method and path | What it does |
|---|---|---|
| Auth | `POST /auth/register`, `POST /auth/login` | Create an account, log in |
| | `PUT /auth/username`, `PUT /auth/password` | Change username or password |
| | `GET /auth/users` | List usernames (for suggestions) |
| Tasks | `GET /tasks/{user}`, `POST /tasks/{user}` | A user's tasks, add a task |
| | `PUT /tasks/{id}`, `PUT /tasks/{id}/done`, `DELETE /tasks/{id}` | Edit, complete / reopen, delete |
| | `POST /tasks/{id}/subtasks` | Add a step to a task |
| | `PUT /tasks/subtasks/{id}`, `DELETE /tasks/subtasks/{id}` | Tick / rename a step, delete a step |
| | `POST /tasks/send/{friend}`, `GET /tasks/sent/{user}` | Send a task to a friend, tasks you sent |
| | `POST /tasks/seen/{user}`, `POST /tasks/completions-seen/{user}` | Mark notifications as seen |
| Friends | `GET /friends/{user}` | Friends, incoming and outgoing requests |
| | `POST /friends/request`, `POST /friends/requests/{id}/accept`, `DELETE /friends/requests/{id}` | Send, accept, decline / cancel a request |
| | `DELETE /friends/{user}/{friend}` | Remove a friend |
| | `GET`/`POST /friends/{user}/messages/{friend}` | Read and send chat messages |
| Other | `GET /notifications/{user}` | All badge counts at once |
| | `GET /counter`, `POST /counter/increment` | The Global Clicker |

## Known limitations

This is a learning project and is meant to run locally:

- **No login tokens yet.** The API trusts the username the browser sends, so someone calling the API directly could act as another user. Adding JWT authentication is the next step before putting it online.
- The backend address `http://localhost:8080` is written directly in the frontend pages.
- The layout is designed for desktop screens.
