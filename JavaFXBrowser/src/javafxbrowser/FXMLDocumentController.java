package javafxbrowser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FXMLDocumentController implements Initializable {

    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    @FXML
    private ProgressBar progressBar;

    private WebEngine engine;
    private WebHistory history;

    private String homePage;

    private double zoom;

    // File path for plain text history
    private static final String HISTORY_FILE = "history.txt";

    // Class to hold history entry for persistent storage
    public static class HistoryEntry {
        public String url;
        public String visitedDate;

        public HistoryEntry(String url, String visitedDate) {
            this.url = url;
            this.visitedDate = visitedDate;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        engine = webView.getEngine();
        zoom = 1;
        homePage = "www.google.com";
        textField.setText(homePage);
        setupLoadingIndicator();
        loadPage();
        // Scene not available yet — run later
        Platform.runLater(() -> {
            if (textField.getScene() != null) {
                textField.getScene().getStylesheets().add(
                        getClass().getResource("light.css").toExternalForm()
                );
            }
        });
    }

    public void loadPage() {
        String url = textField.getText().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        engine.load(url);
        
        // Save to persistent history after loading
        saveHistoryEntry(url, LocalDateTime.now().toString());
    }

    public void refreshPage() {
        engine.reload();
    }

    public void zoomIn() {
        zoom += 0.25;
        webView.setZoom(zoom);
    }

    public void zoomOut() {
        zoom -= 0.25;
        webView.setZoom(zoom);
    }

    // Save a single history entry to the file (appends)
    private void saveHistoryEntry(String url, String timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
            writer.write(url + "|" + timestamp);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load all history entries from file
    private List<HistoryEntry> loadHistoryFromFile() {
        List<HistoryEntry> historyList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    historyList.add(new HistoryEntry(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return historyList;
    }

    // Overwrite the history file with new list (used on deletion / clear)
    private void saveAllHistoryToFile(List<HistoryEntry> historyList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, false))) {
            for (HistoryEntry entry : historyList) {
                writer.write(entry.url + "|" + entry.visitedDate);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayHistory() {
        List<HistoryEntry> savedHistory = loadHistoryFromFile();

        Stage historyStage = new Stage();
        historyStage.setTitle("Browsing History");
        historyStage.initModality(Modality.APPLICATION_MODAL);
        historyStage.setWidth(600);
        historyStage.setHeight(400);

        TableView<HistoryEntry> table = new TableView<>();

        // URL column
        TableColumn<HistoryEntry, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().url));
        urlCol.setPrefWidth(400);

        // Date column
        TableColumn<HistoryEntry, String> dateCol = new TableColumn<>("Last Visited");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().visitedDate));
        dateCol.setPrefWidth(180);

        table.getColumns().addAll(urlCol, dateCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getItems().setAll(savedHistory);

        // Double click to open page in main browser
        table.setRowFactory(tv -> {
            TableRow<HistoryEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    HistoryEntry entry = row.getItem();
                    if (entry != null) {
                        textField.setText(entry.url);
                        loadPage();
                        historyStage.close();
                    }
                }
            });
            return row;
        });

        // Delete and Clear buttons
        HBox buttonRow = new HBox(10);
        Button deleteBtn = new Button("Delete Selected");
        Button clearBtn = new Button("Clear All");

        deleteBtn.setOnAction(e -> {
            HistoryEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                table.getItems().remove(selected);
                saveAllHistoryToFile(new ArrayList<>(table.getItems()));
            }
        });

        clearBtn.setOnAction(e -> {
            table.getItems().clear();
            saveAllHistoryToFile(new ArrayList<>());
        });

        buttonRow.getChildren().addAll(deleteBtn, clearBtn);

        VBox layout = new VBox(10, table, buttonRow);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout);
        historyStage.setScene(scene);
        historyStage.showAndWait();
    }

    public void back() {
        history = engine.getHistory();
        ObservableList<WebHistory.Entry> entries = history.getEntries();
        history.go(-1);
        textField.setText(entries.get(history.getCurrentIndex()).getUrl());
    }

    public void forward() {
        history = engine.getHistory();
        ObservableList<WebHistory.Entry> entries = history.getEntries();
        history.go(1);
        textField.setText(entries.get(history.getCurrentIndex()).getUrl());
    }

    public void setupLoadingIndicator() {
        engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case RUNNING:
                    progressBar.setVisible(true);
                    break;
                case SUCCEEDED:
                case FAILED:
                case CANCELLED:
                    progressBar.setVisible(false);
                    break;
                default:
                    break;
            }
        });
        // Bind progress
        progressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
    }

    public void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsController = loader.getController();
            settingsController.setMainScene(textField.getScene()); // pass your main scene
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setScene(new Scene(root));
            settingsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
