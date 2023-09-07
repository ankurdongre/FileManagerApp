import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileManagerApp extends Application {

    private static FileManagerController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Manager");

        FileManagerView view = new FileManagerView();
        controller = new FileManagerController(view);

        Scene scene = new Scene(view.getView(), 800, 600);

        primaryStage.setScene(scene);
        primaryStage.show();

        controller.displayFiles(System.getProperty("user.home"));
    }

    public static class FileManagerView {
        private ListView<String> listView;
        private TextField pathField;
        private Button backButton;
        private Button openButton;
        private Button copyButton;
        private Button moveButton;
        private Button deleteButton;
        private Button pasteButton;
        private Button createFolderButton;
        private Button renameButton;
        private VBox controls;
        private BorderPane root;
        private Label diskInfoLabel;

        public FileManagerView() {
            listView = new ListView<>();
            pathField = new TextField();

            backButton = new Button("Back");
            openButton = new Button("Open");
            copyButton = new Button("Copy");
            moveButton = new Button("Move");
            deleteButton = new Button("Delete");
            pasteButton = new Button("Paste");
            createFolderButton = new Button("Create Folder");
            renameButton = new Button("Rename");

            controls = new VBox(10);
            root = new BorderPane();

            diskInfoLabel = new Label();
            HBox topRightPane = new HBox(10, diskInfoLabel);
            root.setTop(topRightPane);

            pathField.setOnMouseClicked(event -> listView.getSelectionModel().clearSelection());

            controls.getChildren().addAll(pathField, backButton, openButton, copyButton, moveButton, deleteButton, pasteButton, createFolderButton, renameButton);
            root.setLeft(controls);
            root.setCenter(listView);
        }

        public ListView<String> getListView() { return listView; }
        public TextField getPathField() { return pathField; }
        public Button getBackButton() { return backButton; }
        public Button getOpenButton() { return openButton; }
        public Button getCopyButton() { return copyButton; }
        public Button getMoveButton() { return moveButton; }
        public Button getDeleteButton() { return deleteButton; }
        public Button getPasteButton() { return pasteButton; }
        public Button getCreateFolderButton() { return createFolderButton; }
        public Button getRenameButton() { return renameButton; }
        public VBox getControls() { return controls; }
        public BorderPane getView() { return root; }
        public Label getDiskInfoLabel() { return diskInfoLabel; }
    }

    public static class FileManagerController {
        private FileManagerView view;
        private String copiedFilePath;
        private String sourceFilePath;

        public FileManagerController(FileManagerView view) {
            this.view = view;
            view.getBackButton().setOnAction(event -> goBack());
            view.getOpenButton().setOnAction(event -> openSelectedFile());
            view.getCopyButton().setOnAction(event -> copySelectedFile());
            view.getMoveButton().setOnAction(event -> moveSelectedFile());
            view.getDeleteButton().setOnAction(event -> deleteSelectedFile());
            view.getPasteButton().setOnAction(event -> pasteFile(view.getPathField().getText()));
            view.getCreateFolderButton().setOnAction(event -> createFolder(view.getPathField().getText()));
            view.getRenameButton().setOnAction(event -> renameSelectedFile());
            view.getListView().setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        File selectedFile = new File(view.getPathField().getText(), selectedItem);
                        if (selectedFile.isDirectory()) displayFiles(selectedFile.getAbsolutePath());
                        else openSelectedFile();
                    }
                }
            });
            displayThisPC();
        }

        private void displayThisPC() {
            File[] roots = File.listRoots();
            view.getListView().getItems().clear();
            view.getDiskInfoLabel().setText("");
            HBox diskButtons = new HBox(10);
            for (File root : roots) {
                String diskInfo = String.format("%s (%s free of %s)",
                        root.getPath(), formatSize(root.getFreeSpace()), formatSize(root.getTotalSpace()));
                Button diskButton = new Button(diskInfo);
                diskButton.setOnAction(event -> displayFiles(root.getPath()));
                diskButtons.getChildren().add(diskButton);
            }
            view.getDiskInfoLabel().setGraphic(diskButtons);
            view.getPathField().setText("This PC");
            view.getBackButton().setDisable(true);
        }

        private String formatSize(long bytes) {
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int unitIndex = 0;
            double size = bytes;
            while (size >= 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }
            return String.format("%.2f %s", size, units[unitIndex]);
        }

        public void displayFiles(String path) {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
                view.getListView().getItems().clear();
                view.getListView().getSelectionModel().clearSelection();
                view.getPathField().setText(path);
                view.getBackButton().setDisable(directory.getParentFile() == null);
                File[] files = directory.listFiles();
                if (files != null) for (File file : files) view.getListView().getItems().add(file.getName());
            } else {
                view.getListView().getItems().clear();
                view.getListView().getItems().add("Invalid directory");
            }
            view.getListView().getSelectionModel().clearSelection();
        }

        private void goBack() {
            String currentPath = view.getPathField().getText();
            File currentDirectory = new File(currentPath);
            File parentDirectory = currentDirectory.getParentFile();
            if (parentDirectory != null) displayFiles(parentDirectory.getAbsolutePath());
            else if (currentPath.equals("This PC")) displayThisPC();
            else if (currentDirectory.isDirectory() && currentPath.length() == 3 && currentPath.endsWith(":\\")) displayThisPC();
        }

        private void openSelectedFile() {
            String inputPath = view.getPathField().getText().trim();
            String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                File selectedPath = new File(inputPath, selectedItem);
                if (selectedPath.exists() && selectedPath.isDirectory()) controller.displayFiles(selectedPath.getAbsolutePath());
                else if (selectedPath.exists() && selectedPath.isFile()) openFile(selectedPath);
                else System.out.println("Selected file/folder does not exist.");
            } else {
                File inputPathFile = new File(inputPath);
                if (inputPathFile.exists() && inputPathFile.isDirectory()) controller.displayFiles(inputPathFile.getAbsolutePath());
                else System.out.println("No item selected or invalid path.");
            }
        }

        private void openFile(File file) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void copySelectedFile() {
            String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                copiedFilePath = new File(view.getPathField().getText(), selectedItem).getAbsolutePath();
                sourceFilePath = copiedFilePath;
            }
        }

        private void moveSelectedFile() {
            String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                sourceFilePath = new File(view.getPathField().getText(), selectedItem).getAbsolutePath();
                copiedFilePath = null;
            }
        }

        private void pasteFile(String destinationPath) {
            if (sourceFilePath != null) {
                File sourceFile = new File(sourceFilePath);
                File destinationDir = new File(destinationPath);
                if (sourceFile.exists() && destinationDir.exists() && destinationDir.isDirectory()) {
                    File destinationFile = new File(destinationDir, sourceFile.getName());
                    try {
                        if (copiedFilePath != null) {
                            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            displayFiles(view.getPathField().getText());
                        } else {
                            Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            displayFiles(destinationDir.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                copiedFilePath = null;
                sourceFilePath = null;
            }
        }

        private void deleteSelectedFile() {
            String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                File selectedFile = new File(view.getPathField().getText(), selectedItem);
                if (selectedFile.exists()) {
                    if (selectedFile.isDirectory()) deleteDirectory(selectedFile);
                    else selectedFile.delete();
                    displayFiles(view.getPathField().getText());
                }
            }
        }

        private void deleteDirectory(File directory) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) deleteDirectory(file);
                    else file.delete();
                }
            }
            directory.delete();
        }

        private void createFolder(String parentPath) {
            TextInputDialog dialog = new TextInputDialog("New Folder");
            dialog.setTitle("Create New Folder");
            dialog.setHeaderText("Enter folder name:");
            dialog.setContentText("Folder Name:");
            dialog.showAndWait().ifPresent(folderName -> {
                File newFolder = new File(parentPath, folderName);
                if (!newFolder.exists()) {
                    newFolder.mkdir();
                    displayFiles(parentPath);
                }
            });
        }

        private void renameSelectedFile() {
            String selectedItem = view.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TextInputDialog dialog = new TextInputDialog(selectedItem);
                dialog.setTitle("Rename File");
                dialog.setHeaderText("Enter new name:");
                dialog.setContentText("New Name:");
                dialog.showAndWait().ifPresent(newName -> {
                    File selectedFile = new File(view.getPathField().getText(), selectedItem);
                    File newFile = new File(view.getPathField().getText(), newName);
                    if (selectedFile.exists()) {
                        selectedFile.renameTo(newFile);
                        displayFiles(view.getPathField().getText());
                    }
                });
            }
        }
    }
}
