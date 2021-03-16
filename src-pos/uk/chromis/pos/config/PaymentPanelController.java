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


import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;

import uk.chromis.custom.controls.LabeledComboBox;
import uk.chromis.custom.controls.LabeledToggleSwitch;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.payment.ConfigPaymentPanelEmpty;
import uk.chromis.pos.payment.PaymentConfiguration;

public class PaymentPanelController implements Initializable, BaseController {

    @FXML
    private Pane providerPane;
    @FXML
    private LabeledComboBox cardReader;
    @FXML
    private LabeledComboBox paymentGateway;
    @FXML
    private LabeledToggleSwitch paymentTest;

    private final Map<String, PaymentConfiguration> paymentsName = new HashMap<>();
    private PaymentConfiguration pc;

    public BooleanProperty dirty = new SimpleBooleanProperty();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        ObservableList<String> readers = FXCollections.observableArrayList("Not defined", "Generic", "Intelligent", "Keyboard");
        cardReader.addItemList(readers);

        dirty.bindBidirectional(cardReader.dirty);
        dirty.bindBidirectional(paymentGateway.dirty);
        dirty.bindBidirectional(paymentTest.dirty);

        cardReader.setLabel(AppLocal.getIntString("label.magcardreader"));
        cardReader.setItemWidth("label", 110.00);
        cardReader.setItemWidth("comboBox", 250.00);

        paymentGateway.setLabel(AppLocal.getIntString("label.paymentgateway"));
        paymentGateway.setItemWidth("label", 110.00);
        paymentGateway.setItemWidth("comboBox", 250.00);

        paymentTest.setText(AppLocal.getIntString("label.paymenttestmode"));

        paymentGateway.getComboBox().getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            pc = paymentsName.get(comboValue(paymentGateway.getSelected()));

            if (pc != null) {
                providerPane.getChildren().clear();
                providerPane.getChildren().add(pc.getFXComponent());

            }

        });

        // Payment Provider                
        initPayments("Not defined", new ConfigPaymentPanelEmpty());
        initPayments("external", new ConfigPaymentPanelEmpty());
        paymentGateway.setSelected("Not defined");
        
        load();
    }

    private void initPayments(String name, PaymentConfiguration pc) {
        paymentGateway.getComboBox().getItems().add(name);
        paymentsName.put(name, pc);
    }

    private String comboValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public void load() {
        cardReader.setSelected(AppConfig.getInstance().getProperty("payment.magcardreader"));
        paymentGateway.setSelected(AppConfig.getInstance().getProperty("payment.gateway"));
        paymentTest.setSelected(Boolean.parseBoolean(AppConfig.getInstance().getProperty("payment.testmode")));
        pc.loadProperties();
        dirty.setValue(false);
    }

    @Override
    public void save() {
        AppConfig.getInstance().setProperty("payment.magcardreader", comboValue(cardReader.getSelected()));
        AppConfig.getInstance().setProperty("payment.gateway", comboValue(paymentGateway.getSelected()));
        AppConfig.getInstance().setProperty("payment.testmode", Boolean.toString(paymentTest.isSelected()));
        pc.saveProperties();
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
