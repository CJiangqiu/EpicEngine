# Epic Engine: Rise of the Adventurers

A Minecraft mod that enhances terrain generation with majestic mountains and provides customizable interface features.

## ✅ Current Features

### 🏔️ Enhanced Terrain Generation
- **Enhanced Mountain Heights**: Increases the height difference between peaks and ground in vanilla mountains
- **Continuous Mountain Ranges**: Uses Perlin noise algorithms to create more majestic and continuous mountain landscapes
- **Configurable World Height**: Customize maximum and minimum world heights
- **Mountain Biome Focus**: Specifically enhances mountain biomes while preserving other terrain types

### 🎨 Interface Customization
- **Custom Main Menu**: Personalize background images and title graphics
- **Custom Window Title**: Set your own game window title

## 🚧 Upcoming Features

### 🌾 Terrain Enhancement
- **Plains Flattening**: Super-flat plains terrain generation (code ready, testing in progress)

### 🎨 Interface Features
- **Custom Loading Screen**: Personalized loading backgrounds and progress bars (config ready)
- **Menu Copyright Text**: Custom copyright display (config ready)
- **Button Text Customization**: Personalize menu button text (config ready)

### 🎬 Multimedia
- **Video Playback System**: Intro videos for world entry (config system complete)

---

## 📖 Quick Tutorial

### Installation
1. Install Minecraft Forge
2. Place the mod file in your `mods` folder
3. Launch the game - configuration files will be auto-generated

### Using Current Features

#### Mountain Terrain
- Create a new world to experience enhanced mountain generation
- Adjust settings in `config/epic_engine-world.toml`:

```toml
maxMountainHeight = 512
enableWorldModifications = true
```

#### Custom Interface
1. Navigate to `config/epic_engine/textures/`
2. Replace files:
    - `background.png` - Main menu background (1920x1080 recommended)
    - `title.png` - Title image (1024x256 with transparency recommended)
3. Edit `config/epic_engine-custom.toml`:

```toml
enableCustomization = true
customWindowTitle = "Your Custom Title"
```

### Configuration Files
- `epic_engine-world.toml` - Terrain generation settings
- `epic_engine-custom.toml` - Interface customization settings

### Resource Folder Structure

The mod creates these folders in `config/epic_engine/`:

- **textures/** - Image files (background.png, title.png)
- **videos/** - Video files (planned feature)
- **sounds/** - Audio files (planned feature)

---

## 🎯 Tips
- Use new worlds for best terrain experience
- Keep custom images reasonably sized for performance
- Backup your configuration files before major updates

## 🤝 Contributing
Issues and pull requests are welcome! Help us make Epic Engine even better.