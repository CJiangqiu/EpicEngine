# Epic Engine

A Minecraft mod that provides customizable interface features.

## ✅ Current Features
Main Menu Customization: Custom backgrounds, title images, button layouts, and text components
- Loading Screen Customization: Custom backgrounds, progress bars, tip texts, and percentage displays
- Visual Editor: WYSIWYG drag-and-drop interface editor
- Button Management: Selective show/hide functionality for main menu buttons
- Multi-language Support: English and Chinese interface support
- API Integration: Extensible architecture for other mods

  ---
Installation and Setup

Initial Installation

1. Download the Epic Engine mod file (.jar)
2. Place the mod file in your Minecraft mods folder
3. Launch the game - the mod will automatically create configuration files

File Structure

After first launch, configuration files will be created at:
.minecraft/config/epic_engine/
├── epic_engine-custom.toml     # Main configuration file
└── custom/                     # Custom resources folder
├── main_menu_layout.json   # Main menu layout configuration
├── loading_screen_layout.json # Loading screen layout configuration
└── textures/               # Texture files folder
├── background.png      # Main menu background
├── title.png          # Title image
├── loading.png        # Loading screen background
└── loading_progress_bar.png # Progress bar texture

  ---
Module 1: Configuration System

Basic Configuration

Edit the epic_engine-custom.toml file to enable features:

[general]
# Enable customization features                                                                                                                                                                                               
enable_customization = true

[main_menu]
# Enable main menu module                                                                                                                                                                                                     
main_menu_module_enabled = true                                                                                                                                                                                               
main_menu_background_enabled = true                                                                                                                                                                                           
main_menu_title_enabled = true                                                                                                                                                                                                
main_menu_buttons_enabled = true

      # File settings                                                                                                                                                                                                               
      main_menu_background_filename = "background.png"                                                                                                                                                                              
      main_menu_title_filename = "title.png"                                                                                                                                                                                        
      main_menu_button_prefix = "button_"                                                                                                                                                                                           

[loading_screen]
# Enable loading screen module                                                                                                                                                                                                
loading_screen_module_enabled = true                                                                                                                                                                                          
loading_screen_background_enabled = true                                                                                                                                                                                      
loading_screen_progress_enabled = true                                                                                                                                                                                        
loading_screen_tip_text_enabled = true                                                                                                                                                                                        
show_progress_percentage = true

      # File settings                                                                                                                                                                                                               
      loading_screen_background_filename = "loading.png"                                                                                                                                                                            
      loading_screen_progress_bar_filename = "loading_progress_bar.png"                                                                                                                                                             

[window]
# Window customization                                                                                                                                                                                                        
window_module_enabled = true                                                                                                                                                                                                  
window_title_enabled = true                                                                                                                                                                                                   
window_icon_enabled = true                                                                                                                                                                                                    
window_title_text = "My Custom Game"

Image Requirements

| Component                 | Recommended Size | Format  | Notes                              |
  |---------------------------|------------------|---------|------------------------------------|
| Main Menu Background      | 1920x1080        | PNG/JPG | Full screen coverage               |
| Loading Screen Background | 1920x1080        | PNG/JPG | Full screen coverage               |
| Title Image               | 512x128          | PNG     | Transparent background recommended |
| Progress Bar              | 400x32           | PNG     | Horizontal bar design              |
| Window Icons              | 16x16, 32x32     | PNG     | icon16.png, icon32.png             |

  ---
Module 2: Main Menu Customization

Layout Configuration (main_menu_layout.json)

{
"version": "1.0",
"created_time": "2024-01-01T00:00:00",
"last_modified": "2024-01-01T00:00:00",
"screen_resolution": {
"width": 1920,
"height": 1080                                                                                                                                                                                                                
},
"background": {
"texture_name": "background.png"                                                                                                                                                                                              
},
"title_image": {
"texture_name": "title.png",
"scale": 1.0,
"position": {
"x": 500,
"y": 100,
"width": -1,
"height": -1                                                                                                                                                                                                                
}
},
"buttons": [
{
"id": "singleplayer_button",
"button_index": 1,
"enabled": true,
"properties": {
"custom_text": "Single Player",
"texture_name": "",
"text_color": "#FFFFFF",
"texture_scale": 1.0,
"show_text_over_texture": true                                                                                                                                                                                            
},
"position": {
"x": 860,
"y": 318,
"width": 200,
"height": 20                                                                                                                                                                                                              
}
}
],
"custom_texts": [
{
"id": "welcome_text",
"properties": {
"text": "Welcome to My Server!",
"color": "#FFFF00",
"font_scale": 1.5,
"shadow": true                                                                                                                                                                                                            
},
"position": {
"x": 100,
"y": 50,
"width": -1,
"height": -1                                                                                                                                                                                                              
}
}
]
}

Component Functions

Title Image Component

- texture_name: Image file name in textures folder
- scale: Scaling factor (1.0 = original size)
- position: X, Y coordinates on screen

Button Components

- id: Button identifier (singleplayer, multiplayer, options, quit, etc.)
- enabled: Show/hide button (true/false)
- custom_text: Override button text
- text_color: Hex color code for text
- position: Button location and size

Custom Text Components

- text: Display text content
- color: Hex color code
- font_scale: Text size multiplier
- shadow: Enable text shadow effect

  ---
Module 3: Loading Screen Customization

Layout Configuration (loading_screen_layout.json)

{
"version": "1.0",
"screen_resolution": {
"width": 1920,
"height": 1080                                                                                                                                                                                                                
},
"progress_bar": {
"enabled": true,
"show_background": true,
"background_color": "#404040",
"background_alpha": 255,
"position": {
"x": 760,
"y": 524,
"width": 400,
"height": 32                                                                                                                                                                                                                
}
},
"percentage_text": {
"enabled": true,
"properties": {
"format": "%.0f%%",
"color": "#FFFFFF",
"font_scale": 1.0,
"shadow": true                                                                                                                                                                                                              
},
"position": {
"x": 960,
"y": 500                                                                                                                                                                                                                    
}
},
"tip_text": {
"enabled": true,
"properties": {
"color": "#FFFFFF",
"font_scale": 1.0,
"shadow": true,
"tip_texts": [
"Loading world data...",
"Preparing resources...",
"Initializing game..."                                                                                                                                                                                                    
],
"display_interval": 3000                                                                                                                                                                                                    
},
"position": {
"x": 960,
"y": 580                                                                                                                                                                                                                    
}
},
"custom_texts": [
{
"id": "loading_message",
"properties": {
"text": "Please wait while loading...",
"color": "#CCCCCC",
"font_scale": 0.8,
"shadow": false                                                                                                                                                                                                           
},
"position": {
"x": 960,
"y": 620                                                                                                                                                                                                                  
}
}
]
}

Component Functions

Progress Bar Component

- enabled: Show/hide progress bar
- show_background: Display background behind progress
- background_color: Hex color for background
- background_alpha: Background transparency (0-255)
- position: Progress bar location and size

Percentage Text Component

- format: Printf-style format string (%.0f%% for whole numbers)
- color: Text color in hex format
- font_scale: Text size multiplier
- shadow: Enable drop shadow

Tip Text Component

- tip_texts: Array of rotating tip messages
- display_interval: Time between tips (milliseconds)
- color: Text color
- font_scale: Text size

  ---
Module 4: Visual Editor System

Accessing the Editor

1. Launch the game and enter the main menu
2. Press L key to enter edit mode
3. Use the toolbar at the top to navigate

Editor Controls

| Action          | Control                   | Description                                 |
  |-----------------|---------------------------|---------------------------------------------|
| Move Components | Left Click + Drag         | Drag any component to new position          |
| Toggle Buttons  | Middle Click              | Enable/disable main menu buttons            |
| Stop Dragging   | Right Click               | Cancel current drag operation               |
| Save Layout     | Click "Save Layout"       | Save current configuration                  |
| Switch Pages    | Click "◀ Prev" / "Next ▶" | Toggle between Main Menu and Loading Screen |
| Exit Editor     | Press L key               | Return to normal mode                       |

Page Navigation

- Main Menu Page: Edit title, buttons, and custom texts
- Loading Screen Page: Edit progress bar, percentage, and tip texts

Visual Feedback

- Normal Components: Green border when hovered
- Selected Components: Blue border when dragging
- Disabled Buttons: Red border with semi-transparent overlay
- Component Info: Position and size display on hover

  ---
Module 5: Button Management System

Available Buttons

The system automatically detects and manages these vanilla buttons:
- singleplayer: Single Player Game
- multiplayer: Multiplayer Game
- realms: Minecraft Realms
- options: Options/Settings
- quit: Quit Game
- mods: Mod List
- language: Language Settings
- accessibility: Accessibility Options

Button States

- Enabled (default): Button appears and functions normally
- Disabled: Button is hidden but configuration is preserved

Selective Button Display

To show only specific buttons (e.g., Single Player, Multiplayer, Quit):
1. Enter edit mode with L key
2. Middle-click unwanted buttons to disable them
3. Disabled buttons will show red overlay
4. Click "Save Layout" to persist changes
5. Exit edit mode

  ---
Module 6: Tip Text System

Configuration in TOML

[loading_screen.tip_text]
tip_texts = [
"epic_engine.tip.building_world",
"epic_engine.tip.loading_resources",
"epic_engine.tip.preparing_game"                                                                                                                                                                                          
]
tip_display_interval = 3000                                                                                                                                                                                                   
tip_random_order = true

Language File Integration

Create custom tips in language files:

English (en_us.json):
{
"epic_engine.tip.building_world": "Building world terrain...",
"epic_engine.tip.loading_resources": "Loading game resources...",
"epic_engine.tip.preparing_game": "Preparing game environment...",
"epic_engine.tip.custom_1": "Tip: Press F3 for debug information",
"epic_engine.tip.custom_2": "Tip: Use F11 for fullscreen mode"                                                                                                                                                                  
}

Chinese (zh_cn.json):
{
"epic_engine.tip.building_world": "正在构建世界地形...",
"epic_engine.tip.loading_resources": "正在加载游戏资源...",
"epic_engine.tip.preparing_game": "正在准备游戏环境..."                                                                                                                                                                         
}

  ---
Module 7: Developer Integration

For Modpack Authors

Include default configurations in your mod resources:
your_mod/src/main/resources/assets/epic_engine/custom/
├── main_menu_layout.json
├── loading_screen_layout.json
└── textures/
├── background.png
├── title.png
├── loading.png
└── loading_progress_bar.png

These files will be automatically copied to the user's config folder on first launch (only if they don't already exist).

API for Other Mods

Implement custom main menu components:

// Register your component provider                                                                                                                                                                                               
@EventBusSubscriber(modid = "your_mod", bus = EventBusSubscriber.Bus.MOD)                                                                                                                                                         
public class YourModComponents {

      @SubscribeEvent                                                                                                                                                                                                               
      public static void registerComponents(FMLCommonSetupEvent event) {
          MainMenuAPIRegistry.registerProvider(new YourComponentProvider());
      }
}

// Implement the provider interface                                                                                                                                                                                               
public class YourComponentProvider implements IMainMenuComponentProvider {

      @Override                                                                                                                                                                                                                     
      public String getModId() {
          return "your_mod";
      }

      @Override                                                                                                                                                                                                                     
      public IExternalButton createButton(MainMenuLayoutData layoutData) {
          return new YourCustomButton();
      }

      @Override                                                                                                                                                                                                                     
      public List<IExternalText> createTexts(MainMenuLayoutData layoutData) {
          return Arrays.asList(new YourCustomText());
      }
}

  ---
Troubleshooting Guide

Common Issues and Solutions

Images Not Displaying

- Check file format: Use PNG or JPG format
- Verify filename: Ensure filename matches configuration exactly
- Check file path: Confirm files are in the correct textures folder
- Test file integrity: Try opening images in an image viewer

Layout Not Applying

- Restart game: Some changes require a game restart
- Validate JSON: Use a JSON validator to check file syntax
- Check permissions: Ensure config folder is writable
- Reset to defaults: Delete layout files to regenerate defaults

Editor Not Working

- Key conflicts: Check if other mods use the L key
- Screen resolution: Ensure game is running in supported resolution
- Mod conflicts: Temporarily disable other UI mods for testing

Performance Issues

- Optimize images: Compress large texture files
- Reduce components: Limit number of custom text elements
- Update hardware: Ensure adequate graphics capabilities

Best Practices

Image Optimization

- Use appropriate resolutions for each component
- Compress images without significant quality loss
- Use PNG for images requiring transparency
- Keep file sizes reasonable for better loading times

Layout Design

- Maintain consistent visual hierarchy
- Ensure text remains readable across different resolutions
- Test layouts on different screen sizes
- Use contrasting colors for better visibility

Configuration Management

- Back up working configurations before making changes
- Use version control for complex setups
- Document custom configurations for team projects
- Regular testing after updates