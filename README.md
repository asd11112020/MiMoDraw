# MiMoDraw - Professional Drawing App

A feature-rich drawing application for Android, comparable to professional tools like Huawei Canva.

## Features

### Drawing Tools
- **Pen** - Basic pen tool with smooth strokes
- **Brush** - Soft brush with pressure sensitivity
- **Calligraphy** - Chinese calligraphy brush effect
- **Spray** - Airbrush/spray paint effect
- **Eraser** - Erase parts of your drawing
- **Blur** - Blur tool for softening edges
- **Fill Bucket** - Fill areas with color
- **Eyedropper** - Pick colors from canvas
- **Text Tool** - Add text with custom fonts

### Shape Tools
- Line
- Rectangle
- Circle/Ellipse
- Triangle
- Rounded Rectangle
- Star
- Heart
- Arrow

### Advanced Features
- **Layers System** - Multiple layers with visibility toggle
- **Undo/Redo** - Full history support
- **Zoom & Pan** - Pinch to zoom, drag to pan
- **Symmetry Drawing** - Vertical, Horizontal, Quadrant, Radial symmetry
- **Grid Overlay** - Configurable grid for precise drawing
- **Snap to Grid** - Auto-align to grid points
- **Color Picker** - 30+ preset colors
- **Opacity Control** - Adjust brush transparency
- **Brush Size** - 1-100px adjustable size
- **Filters** - Blur, Sharpen, Brightness, Contrast, Saturation, etc.
- **Text Editor** - Custom fonts, bold, italic, size control
- **Export to Gallery** - Save as PNG to device gallery

### Canvas Options
- Custom canvas sizes (Social Media, Story, YouTube, A4, etc.)
- Custom background colors
- Multiple layer support

## Building the APK

### Prerequisites
- Android Studio (Arctic Fox or newer)
- JDK 17 or newer
- Android SDK 34

### Build Steps

1. Open the project in Android Studio:
   ```
   File → Open → Select MiMoDraw folder
   ```

2. Wait for Gradle sync to complete

3. Build the APK:
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. The APK will be at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Command Line Build

```bash
cd MiMoDraw
chmod +x gradlew
./gradlew assembleDebug
```

## Project Structure

```
MiMoDraw/
├── app/
│   ├── src/main/
│   │   ├── java/com/mimo/draw/
│   │   │   ├── MainActivity.kt          # Main activity
│   │   │   ├── DrawingModels.kt          # Data models
│   │   │   ├── DrawingViewModel.kt       # ViewModel
│   │   │   ├── DrawingCanvas.kt          # Canvas composable
│   │   │   └── ToolBar.kt               # UI components
│   │   ├── res/
│   │   │   ├── drawable/                 # Vector icons
│   │   │   ├── values/                   # Strings, themes
│   │   │   └── mipmap-*/                 # App icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## License

MIT License
