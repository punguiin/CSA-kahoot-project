# Kahoot

A Kahoot-style quiz application with a plain-Java backend and a React frontend.

- **Backend** — Java 21, no frameworks (JDK only + explicitly added libraries), built with Maven.
- **Frontend** — React + TypeScript, built with Vite. Lives in [`frontend/`](frontend/).

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **Node.js 22+** and **npm 10+** (for the frontend)

## Backend setup

Run from the project root:

```bash
mvn compile        # compile sources
mvn test           # run tests
mvn package        # build the artifact into target/
```

Run the app:

```bash
java -cp target/classes Main
```

## Frontend setup

Run from the `frontend/` directory:

```bash
cd frontend
npm install        # install dependencies (first time only)
npm run dev        # start the Vite dev server
```

Other useful commands:

```bash
npm run build      # type-check and build for production (into frontend/dist/)
npm run preview    # serve the production build locally
npm run lint       # run ESLint
```
