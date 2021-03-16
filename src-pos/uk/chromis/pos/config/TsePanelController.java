/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c)2015-2016
**    http://www.chromis.co.uk
**
**    This file is part of Chromis POS Version V0.60.2 beta
**
**    Chromis POS is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS.  If not, see <http://www.gnu.org/licenses/>
**
**
 */
package uk.chromis.pos.config;

import com.cryptovision.SEAPI.exceptions.SEException;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import uk.chromis.custom.controls.LabeledComboBox;
import uk.chromis.custom.controls.LabeledTextField;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.tse.TseInfo;
import uk.chromis.pos.tse.TseInfoCryptovision;
import uk.chromis.pos.tse.TseInfoCryptovisionTest;

/**
 * FXML Controller class
 *
 * @author John
 */
public class TsePanelController implements Initializable, BaseController {

    @FXML
    private LabeledComboBox tseDetails;
    @FXML
    private LabeledTextField tseLib;
    @FXML
    private Button btnLibrary;

    protected BooleanProperty dirty = new SimpleBooleanProperty();

    private Image image;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dirty.bindBidirectional(tseDetails.dirty);
        ObservableList<String> tseEngines = FXCollections.observableArrayList("", "Cryptovision", "Training");
        tseDetails.setLabel("TSE Engine");
        tseDetails.setWidthSizes(120.0, 400.0);
        tseDetails.addItemList(tseEngines);

        dirty.bindBidirectional(tseLib.dirty);
        tseLib.setLabel(AppLocal.getIntString("label.tselib"));
        tseLib.setWidthSizes(120.0, 400.0);
        tseLib.disableProperty().set(true);

        tseDetails.getComboBox().getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            String dirname = System.getProperty("dirname.path");
            dirname = dirname == null ? "./" : dirname;
        });

        tseDetails.getComboBox().getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
//            switch (tseDetails.getSelected().toString()) {
            switch (newValue.toString()) {
                case "Cryptovision":
                    tseLib.disableProperty().set(false);
                    break;
                case "Training":
                    tseLib.disableProperty().set(false);
                    break;
                default:
                    tseLib.disableProperty().set(true);
                    break;
            }
        });
        
        load();

        // add the icons to the open file button
        image = new Image(getClass().getResourceAsStream("/uk/chromis/images/fileopen.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(18);
        imageView.setFitWidth(18);
        btnLibrary.setGraphic(imageView);
    }

    public void handleTestConnection() {
        try {
            String res;
            if ("Cryptovision".equals(tseDetails.getSelected())) {
                TseInfo tse = new TseInfoCryptovision(tseLib.getText());
                res = tse.getConfigCheck();
                tse.close();
            } else if ("Training".equals(tseDetails.getSelected())) {
                TseInfo tse = new TseInfoCryptovisionTest(tseLib.getText());
                res = tse.getConfigCheck();
                tse.close();
            } else {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("TSE Check");
            if ("".equals(res)) {
                alert.setHeaderText("No Information from TSE");
            } else {
                alert.setHeaderText("Information from TSE");
            }
            alert.setContentText(res);
            ButtonType buttonExit = new ButtonType("Exit");
            alert.getButtonTypes().setAll(buttonExit);
            Optional<ButtonType> result = alert.showAndWait();
        } catch (SEException ex) {
            Logger.getLogger(TsePanelController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void handleSelectLibFile() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        if (AppConfig.getInstance().getProperty("tse.directory") != null) {
            dirChooser.setInitialDirectory(new File(AppConfig.getInstance().getProperty("tse.directory")));
        }
        File selectedDirectory = dirChooser.showDialog(null);
        if (selectedDirectory != null) {
            tseLib.setText(selectedDirectory.getAbsolutePath().toString());
        }
    }

    private String comboValue(Object value) {
        return value == null ? "" : value.toString();
    }

    public void load() {
        if (AppConfig.getInstance().getProperty("tse.engine") != null) {
            tseDetails.setSelected(AppConfig.getInstance().getProperty("tse.engine"));
        } else {
            tseDetails.setSelected("");
        }

        if ("Cryptovision".equals(tseDetails.getSelected())) {
            tseLib.setText(AppConfig.getInstance().getProperty("tse.directory"));
        } else if ("Training".equals(tseDetails.getSelected())) {
            tseLib.setText(AppConfig.getInstance().getProperty("tse.directory"));
        } else {
            tseLib.setText("");
        }

        dirty.setValue(false);
    }

    @Override
    public void save() {
        AppConfig.getInstance().setProperty("tse.engine", comboValue(tseDetails.getSelected()));
        if ("Cryptovision".equals(tseDetails.getSelected())) {
            AppConfig.getInstance().setProperty("tse.directory", tseLib.getText());
        } else if ("Training".equals(tseDetails.getSelected())) {
            AppConfig.getInstance().setProperty("tse.directory", tseLib.getText());
        } else {
            AppConfig.getInstance().setProperty("tse.directory", "");
        }
        dirty.setValue(false);
    }

    @Override
    public Boolean isDirty() {
        return dirty.getValue();
    }

    @Override
    public void setDirty(Boolean value) {
        dirty.setValue(value);
    }

}
