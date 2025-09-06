package rankTable;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class InputPanel extends Application {
    private ObjectOutputStream out;
    private final ObservableList<Team> teamsList = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        try {
            Socket socket = new Socket("localhost", 5001);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject("INPUT");
            out.flush();
        } catch (Exception e) { e.printStackTrace(); }

        // --- Komanda əlavə etmək üçün sahələr ---
        TextField teamNameField = new TextField();
        teamNameField.setPromptText("Yeni komanda adı");

        TextField pointsField = new TextField();
        pointsField.setPromptText("Xal");

        Button submitBtn = new Button("Əlavə et");
        submitBtn.setOnAction(e -> {
            try {
                String name = teamNameField.getText();
                int points = Integer.parseInt(pointsField.getText());
                Team t = new Team(name, points);
                out.writeObject(t);
                out.flush();

                teamsList.add(new Team(name, points));
                teamsList.sort((a, b) -> b.getPoints() - a.getPoints());

                teamNameField.clear();
                pointsField.clear();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // --- Komandaları göstərmək üçün ListView ---
        ListView<Team> listView = new ListView<>(teamsList);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " - " + item.getPoints() + " - " + item.getCollateral());
            }
        });
        listView.setPrefHeight(200);

        // --- Xalları yeniləmək ---
        TextField editPointsField = new TextField();
        editPointsField.setPromptText("Yeni xal");
        Button updateBtn = new Button("Yenilə");
        updateBtn.setOnAction(e -> {
            Team selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    int newPoints = Integer.parseInt(editPointsField.getText());
                    Team updated = new Team(selected.getName(), newPoints);
                    updated.setCollateral(selected.getCollateral());
                    out.writeObject(updated);
                    out.flush();

                    selected.setPoints(newPoints);
                    listView.refresh();
                    teamsList.sort((a, b) -> b.getPoints() - a.getPoints());
                    editPointsField.clear();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        // --- Girov əlavə etmək ---
        TextField collateralField = new TextField();
        collateralField.setPromptText("Yeni Girov");
        Button addCollateralBtn = new Button("Əlavə et");
        addCollateralBtn.setOnAction(e -> {
            Team selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    int collateral = Integer.parseInt(collateralField.getText());
                    Team updated = new Team(selected.getName(), selected.getPoints());
                    updated.setCollateral(collateral);
                    out.writeObject(updated);
                    out.flush();

                    selected.setCollateral(collateral);
                    listView.refresh();
                    collateralField.clear();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        // --- Komandani Sil ---
        Button deleteBtn = new Button("Komandani Sil");
        deleteBtn.setOnAction(e -> {
            Team selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    // Serverə silmə əmri göndəririk
                    out.writeObject("DELETE:" + selected.getName());
                    out.flush();

                    // Lokal siyahıdan silirik
                    teamsList.remove(selected);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


        // --- Növbəti tur butonu ---
        Button nextRoundBtn = new Button("Növbəti");
        nextRoundBtn.setOnAction(e -> {
            try {
                File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File file = new File(downloadsDir, "Siralama.txt");

                boolean fileExists = file.exists();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    if (!fileExists) writer.write("");
                    writer.newLine(); writer.newLine(); writer.newLine(); writer.newLine();
                    writer.write("sira --- komanda --- xal --- girov");
                    writer.newLine();

                    // Tab2 sırası (girov nəticəsi ilə hazır sıralama)
                    List<Team> tab2Order = new ArrayList<>(teamsList);

                    // Qırmızı xətt mövqeyi
                    double avg = teamsList.stream().mapToInt(Team::getPoints).average().orElse(0);
                    int redLineIndex = -1;
                    for (int i = 0; i < tab2Order.size(); i++) {
                        if (tab2Order.get(i).getPoints() < avg) { redLineIndex = i; break; }
                    }

                    // Qırmızı xəttdən yuxarı keçən və aşağı qalan komandalar
                    List<Team> aboveRedLine = new ArrayList<>();
                    List<Team> belowRedLine = new ArrayList<>();
                    for (int i = 0; i < tab2Order.size(); i++) {
                        if (redLineIndex >= 0 && i < redLineIndex) aboveRedLine.add(tab2Order.get(i));
                        else belowRedLine.add(tab2Order.get(i));
                    }

                    // Fayla yaz: əvvəl qırmızı xəttdən yuxarı keçən komandalar
                    int n=1;
                    for (Team t : aboveRedLine) {
                        writer.write(n+"    ---    "+t.getName() + "    ---    " + t.getPoints() + "    ---    " + t.getCollateral());
                        writer.newLine();
                    }

                    // Qırmızı xətt
                    writer.write("________________________");
                    writer.newLine();

                    // Aşağı qalanlar

                    for (Team t : belowRedLine) {
                        writer.write(n+"    ---    "+t.getName() + "    ---    " + t.getPoints() + "    ---    " + t.getCollateral());
                        writer.newLine();
                        n++;
                    } n=1;

                    // 4 boş sətir
                    writer.newLine(); writer.newLine(); writer.newLine(); writer.newLine();
                }

                // Tab1 və Tab2 xalları və girovları sıfırlamaq
                for (Team t : teamsList) {
                    t.setPoints(0);
                    t.setCollateral(0);
                    out.writeObject(t);
                }
                out.flush();

            } catch (Exception ex) { ex.printStackTrace(); }
        });


        HBox addBox = new HBox(10, teamNameField, pointsField, submitBtn);
        HBox editBox = new HBox(10, editPointsField, updateBtn);
        HBox collateralBox = new HBox(10, collateralField, addCollateralBtn);
        HBox deleteBox = new HBox(10, deleteBtn);
        HBox nextRoundBox = new HBox(10, nextRoundBtn);

        VBox layout = new VBox(15, addBox, listView, editBox, collateralBox, deleteBox, nextRoundBox);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("root-pane");

        Scene scene = new Scene(layout, 600, 450);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());



        stage.setTitle("Input Panel");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(); }
}
