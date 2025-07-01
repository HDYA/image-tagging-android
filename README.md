# image-tagging-android
An app to tag phone images with customized searchable tags

## Features

This Android application provides advanced image and video tagging capabilities with performance optimizations for large media collections:

### 1. Settings View
- **Directory Selection**: Choose and persist target directory from local storage
- **CSV Export**: Display labeled images in CSV format with copy-to-clipboard functionality  
- **Grouping Configuration**: Toggle date-based grouping and configure time thresholds
- **Date Type Selection**: Choose between EXIF capture time, file creation time, or modification time for grouping
- **Sorting Options**: Sort files by name or date, in ascending or descending order
- **Path Filtering**: CSV export shows only files from the currently selected directory

### 2. Label Management View
- **Unicode Support**: Full support for Chinese characters and other Unicode symbols
- **CRUD Operations**: Add, edit, and delete custom labels with alphabetical sorting
- **Clipboard Import**: Import labels from clipboard (multi-line text with each line as a tag)
- **Filter Labels**: Text field to filter and search through existing labels
- **Smart Deletion**: "Clear All" removes only unused labels, preserving labels assigned to files

### 3. Gallery View (Default Launch Screen)
- **High-Performance Media Discovery**: Efficiently handles directories with 15k+ files using pagination
- **Thumbnail Display**: Shows thumbnails for supported formats (JPG/JPEG, PNG, GIF, BMP, WebP, RAW formats)
- **File Format Support**: Displays placeholders for unsupported formats instead of loading thumbnails
- **Advanced Label Assignment**: 
  - Searchable dropdown with Chinese Pinyin support (full and initial pinyin)
  - Shows 10 most recently used labels for quick access
  - Scrollable tag selection dialog for large tag lists
- **Group Operations**: Click group headers to assign labels to all files in a time-based group
- **Image Preview**: Click thumbnails for fullscreen image preview or external video player
- **Smart Navigation**: "Jump to Next Unlabeled" button to quickly find untagged files
- **Performance Optimizations**:
  - Pagination with 100-file batches for smooth scrolling
  - Auto-loading next page when scrolling near end
  - Cached grouping information and file labels
  - Progress indicators for loading operations

## Technical Architecture

### Modern Android Stack
- **Language**: Kotlin with coroutines for async operations
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room with proper relationship management and caching
- **Navigation**: Navigation Compose for screen transitions
- **Image Loading**: Coil for efficient thumbnail loading
- **Data Storage**: DataStore for preferences persistence
- **File Operations**: ExifInterface for metadata reading
- **Performance**: Optimized pagination and caching systems

### Advanced Features
- **Chinese Pinyin Search**: Enhanced character mapping for comprehensive Chinese text search
- **Memory Management**: Efficient label and file metadata caching
- **File Safety**: Read-only operations - never modifies original media files
- **Responsive UI**: Adapts to different screen sizes and handles large datasets
- **Error Handling**: Robust error handling with graceful degradation

## Project Structure

```
app/src/main/java/com/hdya/imagetagging/
├── data/           # Database entities, DAOs, repositories with relationship management
├── ui/             # Compose UI screens and components
│   ├── gallery/    # High-performance gallery with pagination and caching
│   ├── labels/     # Label management with filtering and bulk operations
│   ├── settings/   # Configuration with advanced sorting and export options
│   └── theme/      # Material Design 3 theming
└── utils/          # File operations, CSV export, Pinyin search utilities
```

### Key Components
- **Database Layer**: Room entities with proper relationships and caching
- **Performance**: Pagination system handling 15k+ files efficiently  
- **Search**: Enhanced Chinese Pinyin support with comprehensive character mapping
- **UI Components**: Scrollable dialogs, progress indicators, responsive layouts
- **File Safety**: Read-only operations with metadata extraction only

## Performance Features

- **Pagination**: Loads files in 100-item batches for smooth scrolling
- **Caching**: Memory-based caching for labels, file metadata, and grouping information
- **Lazy Loading**: Thumbnails loaded only for supported formats when needed
- **Smart Refresh**: Incremental label updates without full reload
- **Progress Feedback**: Loading indicators and status messages for better UX

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