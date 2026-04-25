package TextEditor;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

public class Main extends Application{

    private File currentFile = null;
    private boolean modified = false;
    private Charset currentCharset = StandardCharsets.UTF_8;
    private TextArea textArea;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Текстовая область
        textArea = new TextArea();
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            modified = true;
        });
        root.setCenter(textArea);

        // Панель инструментов с кнопками
        ToolBar toolBar = new ToolBar();
        Button btnNew = new Button("Новый");
        Button btnOpen = new Button("Открыть");
        Button btnSave = new Button("Сохранить");

        btnNew.setOnAction(e -> newFile());

        btnOpen.setOnAction(e -> openFile());

        btnSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (currentFile == null) {
                    saveFileAs();
                } else {
                    writeFile(currentFile, currentCharset);
                }
            }
        });

        toolBar.getItems().addAll(btnNew, btnOpen, btnSave);

        root.setTop(toolBar);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Мой текстовый редактор");
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (modified) {
                event.consume(); // предотвращаем немедленное закрытие
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Выход");
                alert.setHeaderText("Есть несохранённые изменения");
                alert.setContentText("Сохранить изменения перед выходом?");
                ButtonType buttonSave = new ButtonType("Сохранить и выйти");
                ButtonType buttonDiscard = new ButtonType("Выйти без сохранения");
                ButtonType buttonCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == buttonSave) {
                        if (currentFile == null) {
                            saveFileAs();
                        } else {
                            writeFile(currentFile, currentCharset);
                        }
                        if (!modified) { // если сохранение прошло успешно, закрываем
                            primaryStage.close();
                        }
                    } else if (result.get() == buttonDiscard) {
                        primaryStage.close();
                    }
                    // при отмене ничего не делаем, окно остаётся открытым
                }
            }
        });

    }

    private void newFile() {
        if (modified) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Сохранение");
            alert.setHeaderText("Есть несохранённые изменения");
            alert.setContentText("Хотите сохранить изменения перед созданием нового файла?");
            ButtonType buttonSave = new ButtonType("Сохранить");
            ButtonType buttonDiscard = new ButtonType("Не сохранять");
            ButtonType buttonCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == buttonSave) {
                    if (currentFile == null) {
                        saveFileAs();
                    } else {
                        writeFile(currentFile, currentCharset);
                    }
                    if (modified) return; // если сохранение не удалось или отменено, прерываем создание нового
                } else if (result.get() == buttonDiscard) {
                    // ничего не делаем, просто продолжаем
                } else {
                    return; // отмена
                }
            }
        }
        // Очищаем текстовое поле и сбрасываем состояние
        textArea.clear();
        currentFile = null;
        modified = false;
        // Можно сбросить кодировку на UTF-8 или оставить прежнюю
        currentCharset = StandardCharsets.UTF_8;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void saveFileAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл как");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile == null) return;

        // Выбор кодировки
        ChoiceDialog<String> encodingDialog = new ChoiceDialog<>("UTF-8", "UTF-8", "UTF-16", "ASCII");
        encodingDialog.setTitle("Выбор кодировки");
        encodingDialog.setHeaderText("Выберите кодировку для сохранения");
        encodingDialog.setContentText("Кодировка:");
        Optional<String> encodingResult = encodingDialog.showAndWait();
        if (encodingResult.isEmpty()) return;
        String charsetName = encodingResult.get();
        Charset charset = Charset.forName(charsetName.equals("ASCII") ? "US-ASCII" : charsetName);

        writeFile(selectedFile, charset);
        currentFile = selectedFile;
        currentCharset = charset;
    }

    private void writeFile(File file, Charset charset) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset))) {
            writer.write(textArea.getText());
            modified = false;
        } catch (IOException e) {
            showError("Ошибка при записи файла", e.getMessage());
        }
    }

    private void openFile() {
        // Проверка на несохранённые изменения аналогично newFile (можно вынести в отдельный метод)
        if (modified) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Сохранение");
            alert.setHeaderText("Есть несохранённые изменения");
            alert.setContentText("Хотите сохранить изменения перед открытием другого файла?");
            ButtonType buttonSave = new ButtonType("Сохранить");
            ButtonType buttonDiscard = new ButtonType("Не сохранять");
            ButtonType buttonCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == buttonSave) {
                    if (currentFile == null) {
                        saveFileAs();
                    } else {
                        writeFile(currentFile, currentCharset);
                    }
                    if (modified) return;
                } else if (result.get() == buttonDiscard) {
                    // продолжить
                } else {
                    return;
                }
            }
        }

        // Диалог выбора файла
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Открыть текстовый файл");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt", "*.java", "*.xml", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile == null) return;

        // Диалог выбора кодировки
        ChoiceDialog<String> encodingDialog = new ChoiceDialog<>("UTF-8", "UTF-8", "UTF-16", "ASCII");
        encodingDialog.setTitle("Выбор кодировки");
        encodingDialog.setHeaderText("Выберите кодировку файла");
        encodingDialog.setContentText("Кодировка:");
        Optional<String> encodingResult = encodingDialog.showAndWait();
        if (encodingResult.isEmpty()) return; // отмена
        String charsetName = encodingResult.get();
        Charset charset = Charset.forName(charsetName.equals("ASCII") ? "US-ASCII" : charsetName);

        // Чтение файла
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), charset))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            textArea.setText(content.toString());
            currentFile = selectedFile;
            currentCharset = charset;
            modified = false;
        } catch (IOException e) {
            showError("Ошибка при чтении файла", e.getMessage());
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
