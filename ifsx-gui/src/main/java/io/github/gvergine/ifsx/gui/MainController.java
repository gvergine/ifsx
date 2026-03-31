package io.github.gvergine.ifsx.gui;

import io.github.gvergine.ifsx.core.extract.IfsExtractor;
import io.github.gvergine.ifsx.core.hooks.HookRunner;
import io.github.gvergine.ifsx.core.model.*;
import io.github.gvergine.ifsx.core.pack.IfsPacker;
import io.github.gvergine.ifsx.core.parser.DumpIfsParser;
import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MainController {

    // Tab pane
    @FXML private TabPane tabPane;
    @FXML private Tab inspectTab;

    // ── Extract tab ──
    @FXML private TextField extractIfsField;
    @FXML private TextField extractDirField;
    @FXML private Button extractBtn;
    @FXML private VBox extractProgressArea;
    @FXML private ProgressBar extractProgressBar;
    @FXML private Label extractStatusLabel;
    @FXML private VBox extractLogPane;
    @FXML private TextArea extractLogArea;
    @FXML private VBox preExtractHooksPane;
    @FXML private VBox preExtractHooksList;
    @FXML private VBox postExtractHooksPane;
    @FXML private VBox postExtractHooksList;

    // ── Pack tab ──
    @FXML private TextField packDirField;
    @FXML private TextField packIfsField;
    @FXML private Button packBtn;
    @FXML private VBox packProgressArea;
    @FXML private ProgressBar packProgressBar;
    @FXML private Label packStatusLabel;
    @FXML private VBox packLogPane;
    @FXML private TextArea packLogArea;
    @FXML private VBox prePackHooksPane;
    @FXML private VBox prePackHooksList;
    @FXML private VBox postPackHooksPane;
    @FXML private VBox postPackHooksList;

    // ── Inspect tab ──
    @FXML private TreeView<IfsEntryItem> treeView;
    @FXML private SplitPane splitPane;
    @FXML private Label detailTitle;
    @FXML private Label detailType;
    @FXML private Label detailPath;
    @FXML private Label detailSize;
    @FXML private Label detailOffset;
    @FXML private Label detailMode;
    @FXML private Label detailUidGid;
    @FXML private Label detailInode;
    @FXML private Label targetLabel;
    @FXML private Label detailTarget;
    @FXML private Label elfLabel;
    @FXML private Label detailElf;
    @FXML private TitledPane scriptPane;
    @FXML private TextArea scriptArea;

    // Status bar
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // Enable Extract button only when both fields have content
        extractIfsField.textProperty().addListener((obs, old, val) -> updateExtractBtn());
        extractDirField.textProperty().addListener((obs, old, val) -> updateExtractBtn());

        // Enable Pack button only when both fields have content
        packDirField.textProperty().addListener((obs, old, val) -> updatePackBtn());
        packIfsField.textProperty().addListener((obs, old, val) -> updatePackBtn());

        // Drag-and-drop: IFS file onto the Extract source field
        extractIfsField.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        extractIfsField.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File f = db.getFiles().get(0);
                if (f.isFile()) {
                    extractIfsField.setText(f.getAbsolutePath());
                    if (extractDirField.getText().isBlank()) {
                        extractDirField.setText(
                            new File(f.getParentFile(), stripExtension(f.getName()) + "_extracted")
                                .getAbsolutePath());
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        // Drag-and-drop: directory onto the Pack source field
        packDirField.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && db.getFiles().get(0).isDirectory())
                event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        packDirField.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File dir = db.getFiles().get(0);
                if (dir.isDirectory()) {
                    packDirField.setText(dir.getAbsolutePath());
                    if (packIfsField.getText().isBlank()) {
                        packIfsField.setText(
                            new File(dir.getParentFile(), dir.getName() + ".ifs")
                                .getAbsolutePath());
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        // Drag-and-drop on Inspect tree view
        treeView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        treeView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) openIfs(db.getFiles().get(0));
            event.setDropCompleted(true);
            event.consume();
        });

        // Inspect tab tree selection
        treeView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getValue() != null)
                    showEntryDetails(newVal.getValue().entry());
            });

        // Populate hook checkboxes from ~/.ifsx
        loadHooks(HookRunner.PRE_EXTRACT,  preExtractHooksList,  preExtractHooksPane);
        loadHooks(HookRunner.POST_EXTRACT, postExtractHooksList, postExtractHooksPane);
        loadHooks(HookRunner.PRE_PACK,     prePackHooksList,     prePackHooksPane);
        loadHooks(HookRunner.POST_PACK,    postPackHooksList,    postPackHooksPane);
    }

    // ─────────────────────────── Extract tab ───────────────────────────

    @FXML
    private void onBrowseExtractIfs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select IFS Image");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("IFS Images", "*.ifs", "*.bin", "*.image"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(extractIfsField.getScene().getWindow());
        if (file != null) {
            extractIfsField.setText(file.getAbsolutePath());
            if (extractDirField.getText().isBlank()) {
                extractDirField.setText(
                    new File(file.getParentFile(), stripExtension(file.getName()) + "_extracted")
                        .getAbsolutePath());
            }
        }
    }

    @FXML
    private void onBrowseExtractDir() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Output Directory");
        File dir = dc.showDialog(extractDirField.getScene().getWindow());
        if (dir != null) extractDirField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onExtract() {
        Path ifsPath  = Path.of(extractIfsField.getText().trim());
        Path outputDir = Path.of(extractDirField.getText().trim());

        // Capture selected hooks on the FX thread before the background task starts
        List<String> preHooks  = selectedHooks(preExtractHooksList);
        List<String> postHooks = selectedHooks(postExtractHooksList);

        extractBtn.setDisable(true);
        setVisible(extractProgressArea, true);
        setVisible(extractLogPane, true);
        extractLogArea.clear();
        extractProgressBar.setProgress(-1);
        extractStatusLabel.setText("Extracting...");

        Task<IfsImage> task = new Task<>() {
            @Override
            protected IfsImage call() throws Exception {
                Consumer<String> log = line ->
                    Platform.runLater(() -> extractLogArea.appendText(line + "\n"));
                HookRunner hooks = new HookRunner();
                hooks.runHooks(HookRunner.PRE_EXTRACT, preHooks, ifsPath, outputDir, log);
                IfsImage image = new IfsExtractor().extract(ifsPath, outputDir, log);
                hooks.runHooks(HookRunner.POST_EXTRACT, postHooks, ifsPath, outputDir, log);
                return image;
            }
        };
        task.setOnSucceeded(e -> {
            IfsImage result = task.getValue();
            extractProgressBar.setProgress(1.0);
            extractStatusLabel.setText(
                "Done — " + result.getEntries().size() + " entries extracted to " + outputDir);
            extractBtn.setDisable(false);
            statusLabel.setText("Extracted: " + ifsPath.getFileName());
        });
        task.setOnFailed(e -> {
            extractProgressBar.setProgress(0);
            extractStatusLabel.setText("Failed: " + task.getException().getMessage());
            extractBtn.setDisable(false);
            statusLabel.setText("Extraction failed");
        });
        daemon(task);
    }

    private void updateExtractBtn() {
        extractBtn.setDisable(
            extractIfsField.getText().isBlank() || extractDirField.getText().isBlank());
    }

    // ─────────────────────────── Pack tab ───────────────────────────

    @FXML
    private void onBrowsePackDir() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Extracted Directory (must contain _ifsx.build)");
        File dir = dc.showDialog(packDirField.getScene().getWindow());
        if (dir != null) {
            packDirField.setText(dir.getAbsolutePath());
            if (packIfsField.getText().isBlank()) {
                packIfsField.setText(
                    new File(dir.getParentFile(), dir.getName() + ".ifs").getAbsolutePath());
            }
        }
    }

    @FXML
    private void onBrowsePackIfs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save IFS Image As");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("IFS Images", "*.ifs"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        if (!packDirField.getText().isBlank()) {
            File dir = new File(packDirField.getText());
            if (dir.getParentFile() != null) fc.setInitialDirectory(dir.getParentFile());
            fc.setInitialFileName(dir.getName() + ".ifs");
        }
        File file = fc.showSaveDialog(packIfsField.getScene().getWindow());
        if (file != null) packIfsField.setText(file.getAbsolutePath());
    }

    @FXML
    private void onPack() {
        Path sourceDir = Path.of(packDirField.getText().trim());
        Path outputIfs = Path.of(packIfsField.getText().trim());

        // Capture selected hooks on the FX thread before the background task starts
        List<String> preHooks  = selectedHooks(prePackHooksList);
        List<String> postHooks = selectedHooks(postPackHooksList);

        packBtn.setDisable(true);
        setVisible(packProgressArea, true);
        setVisible(packLogPane, true);
        packLogArea.clear();
        packProgressBar.setProgress(-1);
        packStatusLabel.setText("Packing...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Consumer<String> log = line ->
                    Platform.runLater(() -> packLogArea.appendText(line + "\n"));
                HookRunner hooks = new HookRunner();
                hooks.runHooks(HookRunner.PRE_PACK, preHooks, outputIfs, sourceDir, log);
                new IfsPacker().pack(sourceDir, outputIfs, log);
                hooks.runHooks(HookRunner.POST_PACK, postHooks, outputIfs, sourceDir, log);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            packProgressBar.setProgress(1.0);
            packStatusLabel.setText("Done — IFS image created: " + outputIfs);
            packBtn.setDisable(false);
            statusLabel.setText("Packed: " + outputIfs.getFileName());
        });
        task.setOnFailed(e -> {
            packProgressBar.setProgress(0);
            packStatusLabel.setText("Failed: " + task.getException().getMessage());
            packBtn.setDisable(false);
            statusLabel.setText("Pack failed");
        });
        daemon(task);
    }

    private void updatePackBtn() {
        packBtn.setDisable(
            packDirField.getText().isBlank() || packIfsField.getText().isBlank());
    }

    // ─────────────────────────── Inspect tab ───────────────────────────

    @FXML
    private void onOpenIfs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open IFS Image");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("IFS Images", "*.ifs", "*.bin", "*.image"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(treeView.getScene().getWindow());
        if (file != null) openIfs(file);
    }

    private void openIfs(File file) {
        tabPane.getSelectionModel().select(inspectTab);
        statusLabel.setText("Loading " + file.getName() + "...");
        detailTitle.setText("Loading...");

        new Thread(() -> {
            try {
                SdpToolExecutor exec = new SdpToolExecutor();
                String verbose = exec.runDumpIfsVerbose(file.toPath());
                IfsImage image = new DumpIfsParser().parse(verbose);
                Platform.runLater(() -> {
                    buildTree(image, file.getName());
                    statusLabel.setText("Loaded: " + file.getAbsolutePath()
                        + "  |  " + image.getEntries().size() + " entries");
                    detailTitle.setText("Select an entry");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading IFS");
                    detailTitle.setText("Load failed");
                    showError("Failed to load IFS: " + e.getMessage());
                });
            }
        }).start();
    }

    private void buildTree(IfsImage image, String fileName) {
        TreeItem<IfsEntryItem> root = new TreeItem<>(new IfsEntryItem(fileName, null));
        root.setExpanded(true);
        Map<String, TreeItem<IfsEntryItem>> dirNodes = new HashMap<>();
        dirNodes.put("", root);
        for (IfsEntry entry : image.getEntries()) {
            String path = entry.getPath();
            if (path == null) continue;
            String displayName = (entry instanceof IfsSymlinkEntry sym)
                ? path + " -> " + sym.getTarget() : path;
            TreeItem<IfsEntryItem> item = new TreeItem<>(new IfsEntryItem(displayName, entry));
            String parentPath = "";
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) parentPath = path.substring(0, lastSlash);
            dirNodes.getOrDefault(parentPath, root).getChildren().add(item);
            if (entry instanceof IfsDirectoryEntry) {
                dirNodes.put(path, item);
                item.setExpanded(true);
            }
        }
        treeView.setRoot(root);
    }

    private void showEntryDetails(IfsEntry entry) {
        if (entry == null) {
            detailTitle.setText("Image root");
            clearDetails();
            return;
        }
        detailTitle.setText(entry.getPath());
        detailType.setText(entry.getType().name());
        detailPath.setText(entry.getPath());
        long bytes = entry.getSizeBytes();
        detailSize.setText(bytes >= 0 ? formatSize(bytes) : "-");
        detailOffset.setText(entry.getOffset() != null ? "0x" + entry.getOffset() : "-");
        FileAttributes a = entry.getAttributes();
        if (a != null) {
            detailMode.setText(a.getMode());
            detailUidGid.setText(a.getUid() + " / " + a.getGid());
            detailInode.setText(String.valueOf(a.getIno()));
        } else {
            detailMode.setText("-");
            detailUidGid.setText("-");
            detailInode.setText("-");
        }
        boolean isSym = entry instanceof IfsSymlinkEntry;
        setVisible(targetLabel, isSym); setVisible(detailTarget, isSym);
        if (isSym) detailTarget.setText(((IfsSymlinkEntry) entry).getTarget());

        boolean isElf = entry instanceof IfsFileEntry fe && fe.isElf();
        setVisible(elfLabel, isElf); setVisible(detailElf, isElf);
        if (isElf) detailElf.setText(((IfsFileEntry) entry).getElfInfo());

        boolean isScript = entry instanceof IfsScriptEntry;
        setVisible(scriptPane, isScript);
        if (isScript) scriptArea.setText(((IfsScriptEntry) entry).getScriptContent());
    }

    private void clearDetails() {
        detailType.setText("-"); detailPath.setText("-");
        detailSize.setText("-"); detailOffset.setText("-");
        detailMode.setText("-"); detailUidGid.setText("-"); detailInode.setText("-");
        setVisible(targetLabel, false); setVisible(detailTarget, false);
        setVisible(elfLabel, false);    setVisible(detailElf, false);
        setVisible(scriptPane, false);
    }

    // ─────────────────────────── Help ───────────────────────────

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About IFSX");
        alert.initOwner(tabPane.getScene().getWindow());
        alert.setHeaderText("IFSX");
        String version = getClass().getPackage().getImplementationVersion();
        alert.setContentText(
            "IFS Extract / Repack Tool\n"
            + (version != null ? "Version " + version + "\n" : "")
            + "github.com/gvergine/ifsx\n"
            + "Apache License 2.0");
        var iconStream = getClass().getResourceAsStream("ifsx.png");
        if (iconStream != null) {
            ImageView iv = new ImageView(new Image(iconStream));
            iv.setFitWidth(64);
            iv.setFitHeight(64);
            iv.setPreserveRatio(true);
            alert.setGraphic(iv);
        }
        alert.showAndWait();
    }

    @FXML
    private void onExit() { Platform.exit(); }

    // ─────────────────────────── Hook helpers ───────────────────────────

    /** Populate a hooks VBox with CheckBoxes; reveal the pane if any hooks exist. */
    private static void loadHooks(String phase, VBox listContainer, VBox pane) {
        List<String> names = HookRunner.availableHooks(phase);
        if (names.isEmpty()) return;
        for (String name : names) {
            listContainer.getChildren().add(new CheckBox(name));
        }
        setVisible(pane, true);
    }

    /** Return the names of all checked hooks in the given list container. */
    private static List<String> selectedHooks(VBox listContainer) {
        return listContainer.getChildren().stream()
            .filter(n -> n instanceof CheckBox cb && cb.isSelected())
            .map(n -> ((CheckBox) n).getText())
            .toList();
    }

    // ─────────────────────────── Helpers ───────────────────────────

    /** Set both visible and managed together (managed=false collapses the node). */
    private static void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private static void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setContentText(msg);
        a.showAndWait();
    }
}
