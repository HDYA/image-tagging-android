# image-tagging-android
An app to tag phone images with customized searchable tags

## Features

This Android application provides:

### 1. Settings View
- Select a target directory from local storage
- Persist selected directory across app restarts  
- Export all labeled images to CSV file
- Configure time threshold for grouping files
- Toggle date-based grouping

### 2. Label Management View
- Display user-defined labels sorted alphabetically
- Support for Unicode characters (Chinese, etc.)
- Add, edit, and delete labels
- Batch operations for label management

### 3. Gallery View (Default)
- Display images and videos from selected directory
- Thumbnail view with file information
- Searchable label assignment interface
- Group files by date/time with configurable threshold
- Persistent labeling across app restarts

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room for local persistence
- **Navigation**: Navigation Compose
- **Image Loading**: Coil
- **Permissions**: Accompanist Permissions
- **File Operations**: ExifInterface for EXIF data

## Project Structure

```
app/src/main/java/com/hdya/imagetagging/
├── data/           # Database entities, DAOs, and repositories
├── ui/             # Compose UI screens and components
│   ├── gallery/    # Gallery view with image listing and labeling
│   ├── labels/     # Label management interface
│   ├── settings/   # Settings and configuration
│   └── theme/      # UI theme and styling
└── utils/          # Utility classes for file operations and CSV export
```

## Getting Started

### Prerequisites

- **Android Studio**: Download and install [Android Studio](https://developer.android.com/studio) (Arctic Fox or newer recommended)
- **JDK**: Java Development Kit 11 or higher
- **Android SDK**: SDK API Level 34 (automatically managed by Android Studio)
- **Minimum Android Version**: API Level 26 (Android 8.0)

### Setting up the Project in Android Studio

1. **Clone the Repository**
   ```bash
   git clone https://github.com/HDYA/image-tagging-android.git
   cd image-tagging-android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository folder and select it
   - Click "OK"

3. **SDK Setup**
   - Android Studio will automatically prompt to install missing SDK components
   - Accept the installation of required SDK platforms and build tools
   - Ensure the following are installed:
     - Android SDK Platform 34
     - Android SDK Build-Tools 34.0.0
     - Android SDK Platform-Tools

4. **Gradle Sync**
   - Android Studio will automatically start Gradle sync
   - If prompted, click "Sync Now" to download dependencies
   - Wait for the sync to complete (this may take a few minutes on first setup)

### Building and Running

#### Running on Device/Emulator

1. **Set up a Device**
   - **Physical Device**: Enable Developer Options and USB Debugging
   - **Emulator**: Create an AVD with API Level 26+ using AVD Manager

2. **Run the App**
   - Click the "Run" button (▶️) in Android Studio, or
   - Use the shortcut `Shift + F10`
   - Select your target device when prompted

#### Building APK Files

##### Debug APK (for testing)
```bash
# Command line
./gradlew assembleDebug

# The APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

Or in Android Studio:
1. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Once completed, click **locate** to find the APK file

##### Release APK (for distribution)
```bash
# Command line
./gradlew assembleRelease

# The APK will be generated at:
# app/build/outputs/apk/release/app-release.apk
```

Or in Android Studio:
1. Go to **Build** → **Generate Signed Bundle / APK**
2. Select **APK** and click **Next**
3. Either create a new keystore or use an existing one
4. Fill in the keystore information and click **Next**
5. Select **release** build variant and click **Finish**

### Gradle Commands

- **Clean project**: `./gradlew clean`
- **Build project**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Install debug APK**: `./gradlew installDebug`

### Troubleshooting

- **Gradle sync fails**: Try **File** → **Invalidate Caches and Restart**
- **Missing dependencies**: Ensure you have a stable internet connection for downloading dependencies
- **Build errors**: Check that you're using the correct JDK version (11+)

### Permissions Required

The app requires the following permissions:
- Storage permissions for reading media files
- Write permission for CSV export
- These will be requested at runtime when needed