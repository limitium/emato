# Email Parser Application

A web application for parsing and processing email files (.eml and .msg) with trade information.

## Prerequisites

- Node.js (v18 or higher)
- Java 17 or higher
- Gradle 8.x
- Git

## Project Structure

    email-parser/
    ├── frontend/          # Vue.js frontend application
    │   ├── public/        # Static assets
    │   ├── src/           # Source code
    │   │   ├── assets/    # Images, fonts, etc.
    │   │   ├── components/# Vue components
    │   │   ├── router/    # Vue Router configuration
    │   │   ├── services/  # API services
    │   │   ├── store/     # Vuex store
    │   │   ├── views/     # Page components
    │   │   ├── App.vue    # Root component
    │   │   └── main.js    # Entry point
    │   ├── scripts/       # Build scripts
    │   ├── .env           # Environment variables
    │   └── package.json   # Dependencies and scripts
    ├── backend/           # Spring Boot backend application
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── java/  # Java source code
    │   │   │   └── resources/ # Configuration files
    │   │   └── test/      # Test files
    │   ├── build.gradle   # Gradle configuration
    │   └── gradlew        # Gradle wrapper
    └── README.md


## Frontend Setup

Navigate to the frontend directory:

    cd frontend

Install dependencies:

    npm install

Create a `.env` file in the frontend directory:

    VITE_API_BASE_URL=http://localhost:8080
    VITE_APP_TITLE=Email Parser

### Development

Start the development server:

    npm run dev

The application will be available at `http://localhost:5173`

### Production Build

Build the application:

    npm run build

The build process will:
1. Automatically set the version from package.json
2. Include the git commit hash in the version
3. Set the build date
4. Generate production-ready files in the `dist` directory

To preview the production build:

    npm run preview

## Backend Setup

Navigate to the backend directory:

    cd backend

### Development

Start the Spring Boot application:

    ./gradlew bootRun

The API will be available at `http://localhost:8080`

### Production Build

Build the JAR file:

    ./gradlew build

The built JAR will be available in `build/libs/`

Run the production JAR:

    java -jar build/libs/email-parser-1.0.0.jar

## API Documentation

### Email Parsing Endpoints

#### Parse Email

    POST /api/parse
    Content-Type: application/json

    {
      "body": "email content",
      "fileType": "eml" | "msg"
    }

#### Get All Emails

    GET /api/emails

#### Get Email Details

    GET /api/email/{id}

#### Update Email

    PUT /api/email/{id}
    Content-Type: application/json

    {
      "id": number,
      "subject": string,
      "fromEmail": string,
      "body": string,
      "trades": [
        {
          "id": number,
          "emailId": number,
          "isSuccess": boolean,
          "errorMessage": string,
          "clientWay": "Buy" | "Sell",
          "currency": string,
          "isinCode": string,
          "quantity": number,
          "price": number,
          "tradeDate": string,
          "settlementDate": string
        }
      ],
      "sent": boolean,
      "createdAt": string,
      "modifiedAt": string
    }

#### Delete Email

    DELETE /api/email/{id}

#### Get Analytics

    GET /api/analytics

## Development Guidelines

### Version Control

The project uses Git for version control. The version number is automatically generated during build and includes:
- Version from package.json
- Git commit hash
- Build date

Example version: `1.0.0-a1b2c3d` (where a1b2c3d is the git commit hash)

### Code Style

Frontend:
- Vue.js Single File Components
- ESLint for code linting
- Prettier for code formatting

Backend:
- Google Java Style Guide
- Checkstyle for code style verification

### Testing

Frontend:

    npm run test

Backend:

    ./gradlew test

## Deployment

### Development Environment

1. Build the frontend:

    cd frontend
    npm run build

2. Build the backend:

    cd backend
    ./gradlew build

3. Deploy the backend JAR file:

    java -jar backend/build/libs/email-parser-1.0.0.jar --spring.profiles.active=dev

4. Deploy the frontend dist directory to your web server

### Production Environment

1. Build the frontend with production settings:

    cd frontend
    npm run build

2. Build the backend:

    cd backend
    ./gradlew build

3. Deploy the backend JAR file:

    java -jar backend/build/libs/email-parser-1.0.0.jar --spring.profiles.active=prod

4. Deploy the frontend dist directory to your production web server

### Docker Deployment

1. Build the Docker image:

    ./gradlew jib

2. Run the Docker container:

    docker run -p 8080:8080 email-parser

## Environment Variables

### Frontend

- `VITE_API_BASE_URL`: Backend API URL
- `VITE_APP_TITLE`: Application title
- `VITE_APP_VERSION`: Automatically set during build
- `VITE_APP_BUILD_DATE`: Automatically set during build

### Backend

- `SERVER_PORT`: API server port (default: 8080)
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (dev, test, prod)
- `LOGGING_LEVEL_ROOT`: Logging level
- `AI_LAB_URL`: URL for the AI Lab service
- `DATABASE_URL`: Database connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

## Acknowledgments

- [Vue.js](https://vuejs.org/) - Frontend framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Backend framework
- [Apache POI](https://poi.apache.org/) - Used for parsing MSG files
- [Bootstrap](https://getbootstrap.com/) - CSS framework
- [Font Awesome](https://fontawesome.com/) - Icons

