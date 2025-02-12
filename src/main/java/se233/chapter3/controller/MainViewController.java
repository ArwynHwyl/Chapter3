package se233.chapter3.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import se233.chapter3.Launcher;
import se233.chapter3.model.FileFreq;
import se233.chapter3.model.PdfDocument;
import javafx.scene.control.Button;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainViewController {
    LinkedHashMap<String ,List<FileFreq>> uniqueSets;
    @FXML
    private ListView<String> inputListView;
    @FXML
    private Button startButton;
    @FXML
    private ListView listView;
    @FXML
    private void initialize() {
        inputListView.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            final  boolean isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".pdf");
            if (db.hasFiles()&&isAccepted) {
                event.acceptTransferModes(TransferMode.COPY);
            } else  {
                event.consume();
            }
        });
        inputListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                String filePath;
                int total_files = db.getFiles().size();
                for(int i = 0; i < total_files; i++) {
                        File file = db.getFiles().get(i);
                        filePath = file.getAbsolutePath();
                        inputListView.getItems().add(filePath);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        startButton.setOnAction(event-> {
            ExecutorService executor= Executors.newFixedThreadPool(4);
            final ExecutorCompletionService<Map<String,FileFreq>> completionService=new
                    ExecutorCompletionService<>(executor);
            List<String> inputListViewItems = inputListView.getItems();
            int total_files = inputListViewItems.size();
            Map<String, FileFreq>[] wordMap = new Map[total_files];
            for (int i = 0; i < total_files; i++) {
                try {
                    String filePath = inputListViewItems.get(i);
                    PdfDocument p = new PdfDocument(filePath);
                    completionService.submit(new WordCountMapTask(p));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < total_files; i++) {
                try {
                    Future<Map<String,FileFreq>> future = completionService.take();
                    wordMap[i] = future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                WordCountReduceTask merger = new WordCountReduceTask(wordMap);
                Future<LinkedHashMap<String, List<FileFreq>>>future= executor.submit(
                        merger);
                uniqueSets = future.get();
                listView.getItems().addAll(uniqueSets.keySet());
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }

        });
        listView.setOnMouseClicked(event-> {
            List<FileFreq> listOfLinks = uniqueSets.get(listView.getSelectionModel().
                    getSelectedItem());
            ListView<FileFreq> popupListView = new ListView<>();
            LinkedHashMap<FileFreq,String> lookupTable = new LinkedHashMap<>();
            for (int i=0 ; i<listOfLinks.size() ; i++) {
                lookupTable.put(listOfLinks.get(i),listOfLinks.get(i).getPath());
                popupListView.getItems().add(listOfLinks.get(i));
            }
            popupListView.setPrefHeight(popupListView.getItems().size() * 28);
            popupListView.setOnMouseClicked(innerEvent-> {
                Launcher.hs.showDocument("file:///"+lookupTable.get(popupListView.
                        getSelectionModel().getSelectedItem()));
                popupListView.getScene().getWindow().hide();
            });
            Popup popup = new Popup();
            popup.getContent().add(popupListView);
            popup.show(Launcher.primaryStage);
        });
    }
}
