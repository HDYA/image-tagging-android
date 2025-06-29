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

## Requirements

- Android SDK 26 (Android 8.0) or higher
- Storage permissions for reading media files
- Write permission for CSV export

## Building

This project uses Gradle with the Android Gradle Plugin. Ensure you have:
- Android SDK installed
- Android Build Tools
- Kotlin compiler

Run `./gradlew build` to build the project.