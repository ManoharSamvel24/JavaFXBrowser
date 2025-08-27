package javafxbrowser;

import com.jfoenix.controls.JFXToggleButton;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsController {

    @FXML
    private JFXToggleButton themeToggle;
    
    @FXML
    private VBox background;
    
    @FXML
    private Label lightLabel;
    
    @FXML
    private Label darkLabel;

    private Scene mainScene;

    // Called by the main controller after loading settings
    public void setMainScene(Scene scene) {
        this.mainScene = scene;

        //  Detect current theme and set toggle state
        if (mainScene.getStylesheets().stream().anyMatch(s -> s.contains("dark.css"))) {
            themeToggle.setSelected(true);
        }
        else {
            themeToggle.setSelected(false);
            
        }

        //  Listener for theme toggle
        themeToggle.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            if (mainScene != null) {
                // Remove whichever theme is active
                mainScene.getStylesheets().removeIf(s -> s.contains("light.css") || s.contains("dark.css"));
                // Add new one
                if (isSelected) {
                    mainScene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());
                    background.setStyle("-fx-background-color: #3D3D3D;");
                    lightLabel.setStyle("-fx-text-fill: #E8E8E8;");
                    darkLabel.setStyle("-fx-text-fill: #E8E8E8");
                } 
                else {
                    mainScene.getStylesheets().add(getClass().getResource("light.css").toExternalForm());
                    background.setStyle("-fx-background-color: #ffffff;");
                    lightLabel.setStyle("-fx-text-fill: #121212;");
                    darkLabel.setStyle("-fx-text-fill: #121212");
                }
            }
        });
    }
}
