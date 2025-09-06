package rankTable;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DisplayPanel extends Application {

    private TableView<Team> table1;
    private TableView<Team> table2;
    private double average = 0;
    private int redLineIndex = -1;

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        table1 = createTableView(false);
        table2 = createTableView(true);
        tabPane.getTabs().addAll(new Tab("Sıralama", table1), new Tab("Sıralama(Girov)", table2));
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox root = new VBox(tabPane);
        root.getStyleClass().add("root-pane");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("Qırmızı Xətt");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        new Thread(this::connectToServer).start();
    }

    private TableView<Team> createTableView(boolean isTab2) {
        TableView<Team> table = new TableView<>();

        TableColumn<Team, String> rankCol = new TableColumn<>("Sıra");
        rankCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= table.getItems().size()) setText(null);
                else setText(String.valueOf(getIndex() + 1));
            }
        });
        rankCol.setPrefWidth(60);
        rankCol.setMinWidth(60);
        rankCol.setMaxWidth(60);

        TableColumn<Team, String> nameCol = new TableColumn<>("Komandalar");
        TableColumn<Team, Integer> pointsCol = new TableColumn<>("Xal");
        TableColumn<Team, Integer> collateralCol = new TableColumn<>("Girov");

        if (!isTab2) {
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
            nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
            pointsCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
            table.getColumns().addAll(rankCol, nameCol, pointsCol);
        } else {
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
            collateralCol.setCellValueFactory(new PropertyValueFactory<>("collateral"));
            nameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.5));
            pointsCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
            collateralCol.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
            table.getColumns().addAll(rankCol, nameCol, pointsCol, collateralCol);
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(50);
        table.setItems(FXCollections.observableArrayList());

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("above-average", "below-average", "red-line");
                if (item == null || empty) return;

                int index = getIndex();
                if (!isTab2) {
                    if (item.getPoints() >= average) getStyleClass().add("above-average");
                    else getStyleClass().add("below-average");
                    if (index > 0 && table.getItems().get(index - 1).getPoints() >= average
                            && item.getPoints() < average)
                        getStyleClass().add("red-line");
                } else {
                    if (redLineIndex >= 0 && index == redLineIndex) getStyleClass().add("red-line");
                    if (redLineIndex == -1 || index < redLineIndex) getStyleClass().add("above-average");
                    else getStyleClass().add("below-average");
                }
            }
        });

        return table;
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5001);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject("DISPLAY");
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                @SuppressWarnings("unchecked")
                List<Team> teams = (List<Team>) in.readObject();
                Platform.runLater(() -> updateTables(teams));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateTables(List<Team> teams) {
        if (teams.isEmpty()) return;

        average = teams.stream().mapToInt(Team::getPoints).average().orElse(0);

        // Tab1 - xal sırasına görə
        List<Team> tab1List = new ArrayList<>(teams);
        tab1List.sort((a, b) -> b.getPoints() - a.getPoints());
        table1.getItems().setAll(tab1List);

        // Tab2 - başlanğıc sıra Tab1 ilə eyni, sort edilməyəcək
        List<Team> tab2List = new ArrayList<>();
        for (Team t : tab1List) {
            Team copy = new Team(t.getName(), t.getPoints());
            copy.setCollateral(t.getCollateral());
            tab2List.add(copy);
        }

        // Qırmızı xətt
        redLineIndex = -1;
        for (int i = 0; i < tab2List.size(); i++) {
            if (tab2List.get(i).getPoints() < average) { redLineIndex = i; break; }
        }

        if (redLineIndex >= 0) {
            for (int i = redLineIndex; i < tab2List.size(); i++) {
                Team t = tab2List.get(i);
                int collateral = t.getCollateral();
                int currentIndex = tab2List.indexOf(t);

                // Girov əməliyyatı - yalnız qırmızı xəttdən aşağı komandalar
                while (collateral > 0 && currentIndex > redLineIndex) {
                    tab2List.remove(currentIndex);
                    tab2List.add(currentIndex - 1, t);
                    currentIndex--;
                    collateral--;
                }

                // Qırmızı xətt limitinə çatıbsa, bir pillə aşağı
                if (collateral > 0 && currentIndex == redLineIndex) {
                    redLineIndex++;
                }
            }
        }

        table2.getItems().setAll(tab2List);
        table1.refresh();
        table2.refresh();
    }

    public static void main(String[] args) { launch(); }
}
