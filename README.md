# ForkItSUTD

A mobile food tracking application built with Android that uses AI-powered meal recognition to help users log their food intake and track their nutrition goals.

## Overview

ForkItSUTD is an intelligent food tracking app designed to make logging meals effortless. Simply take a photo of your food using your device's camera or select from your gallery, and our integrated Google Gemini API automatically identifies the meal and its nutritional information. Track your eating habits and work towards your nutrition goals with an intuitive, user-friendly interface.

## Key Features

- 📸 **Camera & Gallery Integration** - Capture or upload photos of your meals
- 🤖 **AI-Powered Meal Recognition** - Google Gemini API automatically identifies meals and nutrition info
- 🍽️ **Custom Food Logging** - Add meals that the AI doesn't recognize or customize identified meals
- 👤 **User Authentication** - Secure login and signup functionality
- 🎯 **Goal Tracking** - Set and monitor your nutrition goals
- ☁️ **Cloud Sync** - Supabase backend for seamless data storage and synchronization across devices
- 📊 **Meal History** - View and manage your food log entries

## Tech Stack

- **Language**: Java
- **Platform**: Android
- **Backend**: Supabase (Authentication & Database)
- **AI Integration**: Google Gemini API
- **Build System**: Gradle

## Prerequisites

Before running ForkItSUTD, ensure you have the following installed:

- Android Studio (latest version)
- JDK 11 or higher
- Android SDK (API level 21 or higher)
- A Supabase project with configured authentication
- Google Gemini API key

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/perionsuck/ForkItSUTD.git
cd ForkItSUTD
```

### 2. Configure API Keys

Create a `local.properties` file in the project root:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
SUPABASE_URL=your_supabase_url_here
SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

### 3. Build the Project

```bash
./gradlew build
```

### 4. Run on an Emulator or Device

```bash
./gradlew installDebug
```

## Project Structure

```
ForkItSUTD/
├── app/                          # Main Android app module
│   ├── src/main/java/           # Java source code
│   ├── src/main/res/            # Resources (layouts, strings, drawables)
│   └── build.gradle             # App-level build configuration
├── gradle/                       # Gradle wrapper files
├── build.gradle                  # Project-level build configuration
├── settings.gradle               # Gradle settings
└── README.md                     # This file
```

## Recent Features

- Custom food button added to scan and log pages
- Gemini API integration for meal identification after photo capture
- Enhanced UI with improved user experience
- Supabase integration for user management and data persistence
- Goal-setting and tracking functionality

## Usage

1. **Sign Up / Login** - Create an account or log in with your credentials
2. **Scan Meal** - Open the scan page and take a photo of your meal
3. **Review Results** - The app automatically identifies the meal and shows nutritional info
4. **Log Entry** - Confirm or customize the meal details and log it
5. **Track Goals** - Set nutrition goals and monitor your progress
6. **View History** - Check your meal history anytime

## Contributing

We welcome contributions! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add: AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please follow the existing commit message format:
- `add:` for new features
- `fix:` for bug fixes
- `update:` for improvements
- `refactor:` for code restructuring

## License

This project is currently unlicensed. See your local guidelines for usage rights.

## Support

For issues, questions, or suggestions, please open an issue on the [GitHub Issues](https://github.com/perionsuck/ForkItSUTD/issues) page.

## Acknowledgments

- Built with assistance from Cursor AI
- Powered by Google Gemini API for meal recognition
- Backend infrastructure by Supabase