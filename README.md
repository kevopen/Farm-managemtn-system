# Farm Management System

A modern, web-based Farm Management System built with Vaadin and Firebase integration. This application helps farmers and agricultural businesses manage their farm operations efficiently.

## Features

- User Authentication with Firebase
- Dashboard with farm overview
- Inventory management for:
  - Livestock
  - Crops
  - Equipment
  - Supplies
- Task scheduling and management
- Financial tracking and reporting
- Weather integration
- Mobile-responsive design

## Technology Stack

- Frontend: Vaadin Flow (Java)
- Backend: Spring Boot
- Database: Firebase Realtime Database
- Authentication: Firebase Auth
- Build Tool: Maven
- Java Version: 17 or later

## Prerequisites

Before you begin, ensure you have the following installed:
- JDK 17 or later
- Node.js 18 or later
- Maven 3.8+
- A Firebase project with Realtime Database enabled

## Setup

1. Clone the repository:
```bash
git clone https://github.com/kevopen/Farm-managemtn-system.git
cd farm-management-system
```

2. Firebase Configuration:
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Generate a new private key for service account from Project Settings > Service Accounts
   - Save the generated JSON file as `firebase-service-account.json` in `src/main/resources/`
   - Create `application.properties` in `src/main/resources/` with your Firebase configuration

Example `application.properties`:
```properties
firebase.database.url=your-database-url
firebase.storage.bucket=your-storage-bucket
```

3. Install dependencies and build:
```bash
mvn clean install
```

## Running the Application

1. Start the development server:
```bash
mvn spring-boot:run
```

2. Open your browser and navigate to:
```
http://localhost:8080
```

## Demo Data

The application comes with demo data for testing purposes. When you first run the application, sample data will be automatically populated, including:
- Demo user accounts
- Sample farm inventory
- Example tasks and schedules
- Test financial records

Demo login credentials:
- Email: admin@farmer.com
- Password: 123456

## Security Note

This project includes sensitive configuration files that are not tracked in version control:
- Firebase service account JSON
- Application properties with configuration
- Environment variables

Make sure to:
1. Never commit sensitive files to version control
2. Keep your Firebase credentials secure
3. Use environment variables for production deployments

## Development

The project uses standard Maven project structure:
- `src/main/java`: Java source files
- `src/main/resources`: Configuration files
- `frontend/`: Vaadin frontend resources

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

- Built with [Vaadin](https://vaadin.com/)
- Firebase integration for real-time data management
- Original template from Vaadin start.vaadin.com
