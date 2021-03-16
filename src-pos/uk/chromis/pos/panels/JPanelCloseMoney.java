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
package uk.chromis.pos.panels;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.awt.Color;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import uk.chromis.basic.BasicException;
import uk.chromis.data.gui.MessageInf;
import uk.chromis.data.gui.TableRendererBasic;
import uk.chromis.data.loader.Datas;
import uk.chromis.data.loader.SentenceExec;
import uk.chromis.data.loader.SerializerWriteBasic;
import uk.chromis.data.loader.Session;
import uk.chromis.data.loader.StaticSentence;
import uk.chromis.format.Formats;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.forms.AppUser;
import uk.chromis.pos.forms.AppView;
import uk.chromis.pos.forms.BeanFactoryApp;
import uk.chromis.pos.forms.BeanFactoryException;
import uk.chromis.pos.forms.DataLogicSales;
import uk.chromis.pos.forms.DataLogicSystem;
import uk.chromis.pos.forms.JPanelView;
import uk.chromis.pos.panels.PaymentsModel.PaymentsLine;
import uk.chromis.pos.panels.PaymentsModel.CashCountLine;
import uk.chromis.pos.printer.TicketParser;
import uk.chromis.pos.printer.TicketPrinterException;
import uk.chromis.pos.scripting.ScriptEngine;
import uk.chromis.pos.scripting.ScriptException;
import uk.chromis.pos.scripting.ScriptFactory;
import uk.chromis.pos.sync.DataLogicSync;

/**
 *
 * @author adrianromero
 */
public class JPanelCloseMoney extends JPanel implements JPanelView, BeanFactoryApp {

    private AppView m_App;
    private DataLogicSystem m_dlSystem;
    private DataLogicSales m_dlSales;
    private DataLogicSync m_dlSync;
    private PaymentsModel m_PaymentsToClose = null;
    private TicketParser m_TTP;
    private final DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private Session s;
    private Connection con;
    private Statement stmt;
    private Integer result;
    private String SQL;
    private ResultSet rs;
    private List<HourlySalesInfo> hSales;
    private CloseCash cc;

    private AppUser m_User;

    /**
     * Creates new form JPanelCloseMoney
     */
    public JPanelCloseMoney() {
        cc = new CloseCash();
        initComponents();
        initComponents2();
    }

    /**
     *
     * @param app
     * @throws BeanFactoryException
     */
    @Override
    public void init(AppView app) throws BeanFactoryException {

        m_App = app;
        m_dlSystem = (DataLogicSystem) m_App.getBean("uk.chromis.pos.forms.DataLogicSystem");
        m_dlSales = (DataLogicSales) m_App.getBean("uk.chromis.pos.forms.DataLogicSales");
        m_dlSync = (DataLogicSync) m_App.getBean("uk.chromis.pos.sync.DataLogicSync");
        m_TTP = new TicketParser(m_App.getDeviceTicket(), m_dlSystem);

        m_jTicketTable.setDefaultRenderer(Object.class, new TableRendererBasic(
                new Formats[]{new FormatsPayment(), Formats.CURRENCY}));
        m_jTicketTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        m_jScrollTableTicket.getVerticalScrollBar().setPreferredSize(new Dimension(25, 25));
        m_jTicketTable.getTableHeader().setReorderingAllowed(false);
        m_jTicketTable.setRowHeight(25);
        m_jTicketTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//        m_jsalestable.setDefaultRenderer(Object.class, new TableRendererBasic(
//                new Formats[]{Formats.STRING, Formats.CURRENCY, Formats.CURRENCY, Formats.CURRENCY}));
//        m_jsalestable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        m_jScrollSales.getVerticalScrollBar().setPreferredSize(new Dimension(25, 25));
//        m_jsalestable.getTableHeader().setReorderingAllowed(false);
//        m_jsalestable.setRowHeight(25);
//        m_jsalestable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        m_jCloseCash.setVisible(!m_dlSync.isCentral());
        m_jBookCash.setVisible(!m_dlSync.isCentral());

        jPanelTop.setVisible(false);
        if (AppConfig.getInstance().getBoolean("screen.600800")) {
            jPanelTop.setVisible(true);
            jPanelBottom.setVisible(false);
        } else {
            jPanelTop.setVisible(false);
            jPanelBottom.setVisible(true);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public Object getBean() {
        return this;
    }

    /**
     *
     * @return
     */
    @Override
    public JComponent getComponent() {
        return this;
    }

    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.CloseTPV");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        loadData();
    }

    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {
        // Ich muss die Deaktivierung abbrechen dürfen   
        return true;
    }

    private void loadData() throws BasicException {

        // Reset
        m_jSequence.setText(null);
        m_jMinDate.setText(null);
        m_jMaxDate.setText(null);
        m_jPrintCash.setEnabled(false);
        m_jCloseCash.setEnabled(false);
        m_jBookCash.setEnabled(false);

        m_jPrintCashTop.setEnabled(false);
        m_jCloseCashTop.setEnabled(false);

        m_jCash0.setText(null);
        m_jCash1.setText(null);
        m_jCash2.setText(null);
        m_jCash3.setText(null);

        // LoadData
        m_PaymentsToClose = PaymentsModel.loadInstance(m_App);
        
        jTextField1.setText(null);
        jTextField2.setText(null);
        jTextField3.setText(null);
        jTextField4.setText(null);
        jTextField5.setText(null);
        jTextField6.setText(null);
        jTextField7.setText(null);
        jTextField8.setText(null);
        jTextField9.setText(null);
        jTextField10.setText(null);
        jTextField11.setText(null);
        jTextField12.setText(null);
        jTextField13.setText(null);
        jTextField14.setText(null);
        jTextField15.setText(null);
        jTextField16.setText(null);
        jTextField17.setText(null);
        jTextField18.setText(null);
        jTextField19.setText(null);
        jTextField20.setText(null);
        jTextField21.setText(null);
        jTextField22.setText(null);
        jTextField23.setText(null);
        jTextField24.setText(null);
        jTextField25.setText(null);
        jTextField26.setText(null);
        jTextField27.setText(null);
        jTextField28.setText(null);
        jTextField29.setText(null);
        jTextField30.setText(null);
        
        List<CashCountLine> linec = m_PaymentsToClose.getCashCountLines();
        cc.clearCash();
        for (int i = 0; i < linec.size(); i++) {
            cc.setCash(linec.get(i).getValue(), linec.get(i).getCount());
            cc.setCashRes(linec.get(i).getValue(), linec.get(i).getCountRes());
        }
        if (cc.getCash(0.01) != 0) jTextField1.setText(String.valueOf(cc.getCash(0.01)));
        if (cc.getCash(0.02) != 0) jTextField2.setText(String.valueOf(cc.getCash(0.02)));
        if (cc.getCash(0.05) != 0) jTextField3.setText(String.valueOf(cc.getCash(0.05)));
        if (cc.getCash(0.10) != 0) jTextField4.setText(String.valueOf(cc.getCash(0.10)));
        if (cc.getCash(0.20) != 0) jTextField5.setText(String.valueOf(cc.getCash(0.20)));
        if (cc.getCash(0.50) != 0) jTextField6.setText(String.valueOf(cc.getCash(0.50)));
        if (cc.getCash(1) != 0) jTextField7.setText(String.valueOf(cc.getCash(1)));
        if (cc.getCash(2) != 0) jTextField8.setText(String.valueOf(cc.getCash(2)));
        if (cc.getCash(5) != 0) jTextField9.setText(String.valueOf(cc.getCash(5)));
        if (cc.getCash(10) != 0) jTextField10.setText(String.valueOf(cc.getCash(10)));
        if (cc.getCash(20) != 0) jTextField11.setText(String.valueOf(cc.getCash(20)));
        if (cc.getCash(50) != 0) jTextField12.setText(String.valueOf(cc.getCash(50)));
        if (cc.getCash(100) != 0) jTextField13.setText(String.valueOf(cc.getCash(100)));
        if (cc.getCash(200) != 0) jTextField14.setText(String.valueOf(cc.getCash(200)));
        if (cc.getCash(500) != 0) jTextField15.setText(String.valueOf(cc.getCash(500)));

        if (cc.getCashRes(0.01) != 0) jTextField16.setText(String.valueOf(cc.getCashRes(0.01)));
        if (cc.getCashRes(0.02) != 0) jTextField17.setText(String.valueOf(cc.getCashRes(0.02)));
        if (cc.getCashRes(0.05) != 0) jTextField18.setText(String.valueOf(cc.getCashRes(0.05)));
        if (cc.getCashRes(0.10) != 0) jTextField19.setText(String.valueOf(cc.getCashRes(0.10)));
        if (cc.getCashRes(0.20) != 0) jTextField20.setText(String.valueOf(cc.getCashRes(0.20)));
        if (cc.getCashRes(0.50) != 0) jTextField21.setText(String.valueOf(cc.getCashRes(0.50)));
        if (cc.getCashRes(1) != 0) jTextField22.setText(String.valueOf(cc.getCashRes(1)));
        if (cc.getCashRes(2) != 0) jTextField23.setText(String.valueOf(cc.getCashRes(2)));
        if (cc.getCashRes(5) != 0) jTextField24.setText(String.valueOf(cc.getCashRes(5)));
        if (cc.getCashRes(10) != 0) jTextField25.setText(String.valueOf(cc.getCashRes(10)));
        if (cc.getCashRes(20) != 0) jTextField26.setText(String.valueOf(cc.getCashRes(20)));
        if (cc.getCashRes(50) != 0) jTextField27.setText(String.valueOf(cc.getCashRes(50)));
        if (cc.getCashRes(100) != 0) jTextField28.setText(String.valueOf(cc.getCashRes(100)));
        if (cc.getCashRes(200) != 0) jTextField29.setText(String.valueOf(cc.getCashRes(200)));
        if (cc.getCashRes(500) != 0) jTextField30.setText(String.valueOf(cc.getCashRes(500)));

        m_jSalesSubtotal.setText(null);
        m_jSalesTaxes.setText(null);
        m_jSalesTotal.setText(null);

        m_jTicketTable.setModel(new DefaultTableModel());

        // Populate Data
        m_jSequence.setText(m_PaymentsToClose.printSequence());
        m_jMinDate.setText(m_PaymentsToClose.printDateStart());
        m_jMaxDate.setText(m_PaymentsToClose.printDateEnd());

        if (m_PaymentsToClose.getPayments() != 0 || m_PaymentsToClose.getSales() != 0) {

            m_jPrintCash.setEnabled(true);
            m_jCloseCash.setEnabled(true);
            m_jBookCash.setEnabled(true);

            m_jPrintCashTop.setEnabled(true);
            m_jCloseCashTop.setEnabled(true);

            cc.m_dMoneyStart = m_PaymentsToClose.getMoneyStart();
            
            List<PaymentsLine> line = m_PaymentsToClose.getPaymentLines();
            String[] scash = new String[]{"cash","cashrefund","cashin","cashout"};
            List<String> lcash = Arrays.asList(scash);
            cc.m_dCashSales = 0.0;
            for (int i = 0; i < line.size(); i++) {
                if (lcash.contains(line.get(i).getType())) {
                    cc.m_dCashSales = cc.m_dCashSales + line.get(i).getValue();
                }
            }
            
            m_jCash0.setText(Formats.CURRENCY.formatValue(cc.m_dMoneyStart));
            m_jCash1.setText(Formats.CURRENCY.formatValue(cc.m_dCashSales));
            m_jCash2.setText(Formats.CURRENCY.formatValue(cc.getTarget()));
            m_jCash3.setText(Formats.CURRENCY.formatValue(cc.getDifference()));

            m_jSalesSubtotal.setText(m_PaymentsToClose.printSalesBase());
            m_jSalesTaxes.setText(m_PaymentsToClose.printSalesTaxes());
            m_jSalesTotal.setText(m_PaymentsToClose.printSalesTotal());
        }

        m_jTicketTable.setModel(m_PaymentsToClose.getPaymentsModel());

        TableColumnModel jColumns = m_jTicketTable.getColumnModel();
        jColumns.getColumn(0).setPreferredWidth(177);
        jColumns.getColumn(0).setResizable(false);
        jColumns.getColumn(1).setPreferredWidth(80);
        jColumns.getColumn(1).setResizable(false);

//        m_jsalestable.setModel(m_PaymentsToClose.getSalesModel());

//        jColumns = m_jsalestable.getColumnModel();
//        jColumns.getColumn(0).setPreferredWidth(108);
//        jColumns.getColumn(0).setResizable(false);
//        jColumns.getColumn(1).setPreferredWidth(75);
//        jColumns.getColumn(1).setResizable(false);
//        jColumns.getColumn(2).setPreferredWidth(75);
//        jColumns.getColumn(2).setResizable(false);

// read number of no cash drawer activations
        s = m_App.getSession();

        String sdbmanager = m_dlSystem.getDBVersion();

        try {
            result = 0;
            con = s.getConnection();
            if ("PostgreSQL".equals(sdbmanager)) {
                SQL = "SELECT * FROM DRAWEROPENED WHERE TICKETID = 'No Sale' AND OPENDATE > " + "'" + m_PaymentsToClose.printDateStart() + "'";
            } else {
                SQL = "SELECT * FROM DRAWEROPENED WHERE TICKETID = 'No Sale' AND OPENDATE > {fn TIMESTAMP('" + m_PaymentsToClose.getDateStartDerby() + "')}";
            }

            stmt = (Statement) con.createStatement();
            rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                result++;
            }
            rs = null;
            con = null;
            s = null;
        } catch (SQLException e) {
            System.out.println("error = " + e);
        }
        m_jNoCashSales.setText(result.toString());

        // read the hourly sales
        if ("PostgreSQL".equals(sdbmanager)) {
            hSales = m_dlSystem.getHourlySalesPostgreSQL(m_App.getActiveCashIndex());
        } else if ("Derby".equals(sdbmanager)) {
            hSales = m_dlSystem.getHourlySalesDerby(m_App.getActiveCashIndex());
        } else {
            hSales = m_dlSystem.getHourlySalesMySQL(m_App.getActiveCashIndex());
        }

    }

    private void printPayments(String report) {

        String sresource = m_dlSystem.getResourceAsXML(report);
        if (sresource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
            msg.show(this);
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("payments", m_PaymentsToClose);
                script.put("nosales", result.toString());
                script.put("hourlysales", hSales);
                script.put("ticket",m_dlSales);
                m_TTP.printTicket(script.eval(sresource).toString());
            } catch (ScriptException | TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(this);
            }
        }
    }

    private class FormatsPayment extends Formats {

        @Override
        protected String formatValueInt(Object value) {
            return AppLocal.getIntString("transpayment." + (String) value);
        }

        @Override
        protected Object parseValueInt(String value) throws ParseException {
            return value;
        }

        @Override
        public int getAlignment() {
            return javax.swing.SwingConstants.LEFT;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        m_jSequence = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        m_jMinDate = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        m_jMaxDate = new javax.swing.JTextField();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jPanel5 = new javax.swing.JPanel();
        m_jScrollTableTicket = new javax.swing.JScrollPane();
        m_jTicketTable = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jTextField16 = new javax.swing.JTextField();
        jTextField17 = new javax.swing.JTextField();
        jTextField18 = new javax.swing.JTextField();
        jTextField20 = new javax.swing.JTextField();
        jTextField21 = new javax.swing.JTextField();
        jTextField22 = new javax.swing.JTextField();
        jTextField19 = new javax.swing.JTextField();
        jTextField23 = new javax.swing.JTextField();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jTextField14 = new javax.swing.JTextField();
        jTextField15 = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jTextField24 = new javax.swing.JTextField();
        jTextField25 = new javax.swing.JTextField();
        jTextField26 = new javax.swing.JTextField();
        jTextField27 = new javax.swing.JTextField();
        jTextField28 = new javax.swing.JTextField();
        jTextField29 = new javax.swing.JTextField();
        jTextField30 = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        m_jSalesSubtotal = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        m_jSalesTaxes = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        m_jSalesTotal = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        m_jCash1 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        m_jNoCashSales = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel30 = new javax.swing.JLabel();
        m_jCash0 = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        m_jCash2 = new javax.swing.JTextField();
        m_jCash3 = new javax.swing.JTextField();
        jPanelTop = new javax.swing.JPanel();
        m_jCloseCashTop = new javax.swing.JButton();
        m_jPrintCashTop = new javax.swing.JButton();
        jPanelBottom = new javax.swing.JPanel();
        m_jCloseCash = new javax.swing.JButton();
        m_jPrintCash = new javax.swing.JButton();
        m_jBookCash = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, AppLocal.getIntString("label.datestitle"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14))); // NOI18N

        jLabel11.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel11.setText(AppLocal.getIntString("label.sequence")); // NOI18N

        m_jSequence.setEditable(false);
        m_jSequence.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jSequence.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText(AppLocal.getIntString("Label.StartDate")); // NOI18N

        m_jMinDate.setEditable(false);
        m_jMinDate.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jMinDate.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText(AppLocal.getIntString("Label.EndDate")); // NOI18N

        m_jMaxDate.setEditable(false);
        m_jMaxDate.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jMaxDate.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jSequence, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jMinDate, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jMaxDate, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jSequence, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jMinDate, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jMaxDate, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(filler1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, AppLocal.getIntString("label.paymentstitle"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14))); // NOI18N

        m_jScrollTableTicket.setBorder(null);
        m_jScrollTableTicket.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jScrollTableTicket.setMinimumSize(new java.awt.Dimension(350, 140));
        m_jScrollTableTicket.setPreferredSize(new java.awt.Dimension(350, 140));

        m_jTicketTable.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jTicketTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        m_jTicketTable.setFocusable(false);
        m_jTicketTable.setIntercellSpacing(new java.awt.Dimension(0, 1));
        m_jTicketTable.setRequestFocusEnabled(false);
        m_jTicketTable.setShowVerticalLines(false);
        m_jScrollTableTicket.setViewportView(m_jTicketTable);

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel10.setText("Cash count");

        jLabel20.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel20.setText("coins");

        jLabel9.setText("0,01");

        jLabel13.setText("0,02");

        jLabel14.setText("0,05");

        jLabel15.setText("0,10");

        jLabel16.setText("0,20");

        jLabel17.setText("0,50");

        jLabel18.setText("1,00");

        jLabel19.setText("2,00");

        jTextField16.setEditable(false);
        jTextField16.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField16.setFocusable(false);
        jTextField16.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField16FocusLost(evt);
            }
        });

        jTextField17.setEditable(false);
        jTextField17.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField17.setFocusable(false);
        jTextField17.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField17FocusLost(evt);
            }
        });

        jTextField18.setEditable(false);
        jTextField18.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField18.setFocusable(false);
        jTextField18.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField18FocusLost(evt);
            }
        });

        jTextField20.setEditable(false);
        jTextField20.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField20.setFocusable(false);
        jTextField20.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField20FocusLost(evt);
            }
        });

        jTextField21.setEditable(false);
        jTextField21.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField21.setFocusable(false);
        jTextField21.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField21FocusLost(evt);
            }
        });

        jTextField22.setEditable(false);
        jTextField22.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField22.setFocusable(false);
        jTextField22.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField22FocusLost(evt);
            }
        });

        jTextField19.setEditable(false);
        jTextField19.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField19.setFocusable(false);
        jTextField19.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField19FocusLost(evt);
            }
        });

        jTextField23.setEditable(false);
        jTextField23.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField23.setFocusable(false);
        jTextField23.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField23FocusLost(evt);
            }
        });

        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField1FocusLost(evt);
            }
        });

        jTextField2.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField2FocusLost(evt);
            }
        });

        jTextField3.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField3FocusLost(evt);
            }
        });

        jTextField4.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField4.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField4FocusLost(evt);
            }
        });

        jTextField5.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField5.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField5FocusLost(evt);
            }
        });

        jTextField6.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField6.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField6FocusLost(evt);
            }
        });

        jTextField7.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField7.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField7FocusLost(evt);
            }
        });

        jTextField8.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField8.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField8FocusLost(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField23, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField22, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField18, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jTextField16, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jTextField17, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField20, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField21, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel20)
                .addGap(44, 44, 44))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextField16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jTextField17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jTextField18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jTextField20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jTextField21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jTextField22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jTextField23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel22.setText("5");

        jTextField9.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField9.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField9FocusLost(evt);
            }
        });

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel23.setText("10");

        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel24.setText("20");

        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel25.setText("50");

        jLabel26.setText("100");

        jLabel27.setText("200");

        jLabel28.setText("500");

        jTextField10.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField10.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField10FocusLost(evt);
            }
        });

        jTextField11.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField11.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField11FocusLost(evt);
            }
        });

        jTextField12.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField12.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField12FocusLost(evt);
            }
        });

        jTextField13.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField13.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField13FocusLost(evt);
            }
        });

        jTextField14.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField14.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField14FocusLost(evt);
            }
        });

        jTextField15.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField15.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField15FocusLost(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel21.setText("notes");

        jTextField24.setEditable(false);
        jTextField24.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField24.setFocusable(false);
        jTextField24.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField24FocusLost(evt);
            }
        });

        jTextField25.setEditable(false);
        jTextField25.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField25.setFocusable(false);
        jTextField25.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField25FocusLost(evt);
            }
        });

        jTextField26.setEditable(false);
        jTextField26.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField26.setFocusable(false);
        jTextField26.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField26FocusLost(evt);
            }
        });

        jTextField27.setEditable(false);
        jTextField27.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField27.setFocusable(false);
        jTextField27.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField27FocusLost(evt);
            }
        });

        jTextField28.setEditable(false);
        jTextField28.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField28.setFocusable(false);
        jTextField28.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField28FocusLost(evt);
            }
        });

        jTextField29.setEditable(false);
        jTextField29.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField29.setFocusable(false);
        jTextField29.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField29FocusLost(evt);
            }
        });

        jTextField30.setEditable(false);
        jTextField30.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField30.setFocusable(false);
        jTextField30.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField30FocusLost(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField30, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField15, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField24, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField25, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField26, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField27, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField28, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jTextField29, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField14, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(29, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel21)
                .addGap(52, 52, 52))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(162, 162, 162)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField30, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(jTextField24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel23)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel24)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel27)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel28)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jCheckBox1.setText("edit stored");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel10)
                        .addGap(63, 63, 63)
                        .addComponent(jCheckBox1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jCheckBox1))
                .addGap(1, 1, 1)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(248, Short.MAX_VALUE))
        );

        jLabel6.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel6.setText(AppLocal.getIntString("label.totalnet")); // NOI18N

        m_jSalesSubtotal.setEditable(false);
        m_jSalesSubtotal.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jSalesSubtotal.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel12.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel12.setText(AppLocal.getIntString("label.taxes")); // NOI18N

        m_jSalesTaxes.setEditable(false);
        m_jSalesTaxes.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jSalesTaxes.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel7.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel7.setText(AppLocal.getIntString("label.total")); // NOI18N

        m_jSalesTotal.setEditable(false);
        m_jSalesTotal.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jSalesTotal.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        m_jSalesTotal.setPreferredSize(new java.awt.Dimension(6, 21));

        jLabel4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel4.setText(AppLocal.getIntString("label.Money")); // NOI18N

        m_jCash1.setEditable(false);
        m_jCash1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCash1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel8.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText(AppLocal.getIntString("label.nocashsales1"));

        m_jNoCashSales.setEditable(false);
        m_jNoCashSales.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jNoCashSales.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel30.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel30.setText(AppLocal.getIntString("label.Opening"));

        m_jCash0.setEditable(false);
        m_jCash0.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCash0.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel31.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel31.setText(AppLocal.getIntString("label.nocashsales2"));

        jLabel5.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel5.setText(AppLocal.getIntString("label.Closing"));

        jLabel32.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel32.setText(AppLocal.getIntString("label.Difference"));

        m_jCash2.setEditable(false);
        m_jCash2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCash2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        m_jCash3.setEditable(false);
        m_jCash3.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        m_jCash3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jLabel31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(39, 39, 39)
                        .addComponent(m_jNoCashSales, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator1)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel30, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jCash3, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_jCash2, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_jSalesTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jCash0, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(m_jCash1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_jSalesTaxes, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_jSalesSubtotal, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(29, 29, 29))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jSalesSubtotal, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jSalesTaxes, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jSalesTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jCash0, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jCash1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(m_jCash2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(m_jCash3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(m_jNoCashSales, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(m_jScrollTableTicket, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jScrollTableTicket, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(26, 26, 26))
        );

        jPanelTop.setEnabled(false);

        m_jCloseCashTop.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCloseCashTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/chromis/images/calculator.png"))); // NOI18N
        m_jCloseCashTop.setText(AppLocal.getIntString("Button.CloseCash")); // NOI18N
        m_jCloseCashTop.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jCloseCashTop.setIconTextGap(2);
        m_jCloseCashTop.setInheritsPopupMenu(true);
        m_jCloseCashTop.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jCloseCashTop.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jCloseCashTop.setPreferredSize(new java.awt.Dimension(85, 33));
        m_jCloseCashTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseCashTopActionPerformed(evt);
            }
        });

        m_jPrintCashTop.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPrintCashTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/chromis/images/printer.png"))); // NOI18N
        m_jPrintCashTop.setText(AppLocal.getIntString("Button.PrintCash")); // NOI18N
        m_jPrintCashTop.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jPrintCashTop.setIconTextGap(2);
        m_jPrintCashTop.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jPrintCashTop.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jPrintCashTop.setPreferredSize(new java.awt.Dimension(85, 33));
        m_jPrintCashTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jPrintCashTopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTopLayout = new javax.swing.GroupLayout(jPanelTop);
        jPanelTop.setLayout(jPanelTopLayout);
        jPanelTopLayout.setHorizontalGroup(
            jPanelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTopLayout.createSequentialGroup()
                .addComponent(m_jPrintCashTop, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jCloseCashTop, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanelTopLayout.setVerticalGroup(
            jPanelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTopLayout.createSequentialGroup()
                .addGroup(jPanelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m_jPrintCashTop, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jCloseCashTop, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        m_jCloseCash.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jCloseCash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/chromis/images/calculator.png"))); // NOI18N
        m_jCloseCash.setText(AppLocal.getIntString("Button.CloseCash")); // NOI18N
        m_jCloseCash.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jCloseCash.setIconTextGap(2);
        m_jCloseCash.setInheritsPopupMenu(true);
        m_jCloseCash.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jCloseCash.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jCloseCash.setPreferredSize(new java.awt.Dimension(85, 33));
        m_jCloseCash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseCashActionPerformed(evt);
            }
        });

        m_jPrintCash.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPrintCash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/chromis/images/printer.png"))); // NOI18N
        m_jPrintCash.setText(AppLocal.getIntString("Button.PrintCash")); // NOI18N
        m_jPrintCash.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jPrintCash.setIconTextGap(2);
        m_jPrintCash.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jPrintCash.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jPrintCash.setPreferredSize(new java.awt.Dimension(85, 33));
        m_jPrintCash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jPrintCashActionPerformed(evt);
            }
        });

        m_jBookCash.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jBookCash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/uk/chromis/images/cashdrawer.png"))); // NOI18N
        m_jBookCash.setText(AppLocal.getIntString("Button.BookCash"));
        m_jBookCash.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        m_jBookCash.setIconTextGap(2);
        m_jBookCash.setInheritsPopupMenu(true);
        m_jBookCash.setMaximumSize(new java.awt.Dimension(85, 33));
        m_jBookCash.setMinimumSize(new java.awt.Dimension(85, 33));
        m_jBookCash.setPreferredSize(new java.awt.Dimension(85, 33));
        m_jBookCash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jBookCashActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelBottomLayout = new javax.swing.GroupLayout(jPanelBottom);
        jPanelBottom.setLayout(jPanelBottomLayout);
        jPanelBottomLayout.setHorizontalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(m_jPrintCash, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(m_jBookCash, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m_jCloseCash, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelBottomLayout.setVerticalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(m_jCloseCash, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(m_jPrintCash, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(m_jBookCash, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(148, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jPanelTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanelBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        add(jPanel1, java.awt.BorderLayout.LINE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void initComponents2() {
        jLabel10.setText(AppLocal.getIntString("label.cashcount"));
        jLabel20.setText(AppLocal.getIntString("label.coins"));
        jLabel21.setText(AppLocal.getIntString("label.notes2"));
        jCheckBox1.setText(AppLocal.getIntString("label.editstored"));
    }
    
    
    private void m_jPrintCashTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jPrintCashTopActionPerformed

        // print report
        printPayments("Printer.PartialCash");
    }//GEN-LAST:event_m_jPrintCashTopActionPerformed

    private void m_jCloseCashTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jCloseCashTopActionPerformed
        CloseCashActionPerformed(evt);
    }//GEN-LAST:event_m_jCloseCashTopActionPerformed

    private void m_jCloseCashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jCloseCashActionPerformed
        CloseCashActionPerformed(evt);
    }//GEN-LAST:event_m_jCloseCashActionPerformed
    
    private void CloseCashActionPerformed(java.awt.event.ActionEvent evt) {                                                

        int res = JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.wannaclosecash"), AppLocal.getIntString("message.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {

            Date dNow = new Date();
            
            try {
                // Wir schließen die Box, wenn das Schließen noch aussteht.
                if (m_App.getActiveCashDateEnd() == null) {
                    new StaticSentence(m_App.getSession(), "UPDATE CLOSEDCASH SET DATEEND = ?, NOSALES = ?, MONEYEND = ?, MONEYDIFFERENCE = ? WHERE HOST = ? AND MONEY = ?", new SerializerWriteBasic(new Datas[]{Datas.TIMESTAMP, Datas.INT, Datas.DOUBLE, Datas.DOUBLE, Datas.STRING, Datas.STRING}))
                            .exec(new Object[]{dNow, result, cc.getTarget(), cc.getDifference(), m_App.getProperties().getHost(), m_App.getActiveCashIndex()});
                }

                m_PaymentsToClose.setMoneyEnd(cc.getTarget());
                m_PaymentsToClose.setMoneyDifference(cc.getDifference());
                
                // CashCount speichern
                Object[] oc = new Object[4];
                oc[0] = m_App.getActiveCashIndex();
                new StaticSentence(m_App.getSession(), "DELETE FROM CLOSEDCASH_COUNT WHERE MONEY = ? ", new SerializerWriteBasic(new Datas[]{Datas.STRING})).exec(oc);
                m_PaymentsToClose.clearCashCountLines();
                for (int i = 0; i < 15; i++) {
                    oc[1] = cc.m_cash[i];
                    oc[2] = cc.getCash(cc.m_cash[i]);
                    oc[3] = cc.getCashRes(cc.m_cash[i]);
                    new StaticSentence(m_App.getSession(), "INSERT INTO CLOSEDCASH_COUNT (MONEY, VALUE, COUNT, COUNT_RES) VALUES (?, ?, ?, ?)", new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.DOUBLE, Datas.INT, Datas.INT})).exec(oc);
                    m_PaymentsToClose.setCashCountLine(cc.m_cash[i], cc.getCash(cc.m_cash[i]), cc.getCashRes(cc.m_cash[i]));
                }
                
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                // Wir erstellen eine neue Box
                m_App.setActiveCash(UUID.randomUUID().toString(), m_App.getActiveCashSequence() + 1, dNow, null);

                // Wir erstellen die aktive Box
                m_dlSystem.execInsertCash(
                        new Object[]{m_App.getActiveCashIndex(), m_App.getProperties().getHost(), m_App.getActiveCashSequence(), m_App.getActiveCashDateStart(), m_App.getActiveCashDateEnd(), cc.getTarget(), 0.0, 0.0, AppConfig.getTerminalSerial()});

                m_dlSystem.execDrawerOpened(
                        new Object[]{UUID.randomUUID().toString(), m_App.getAppUserView().getUser().getName(), "Close Cash"});

                // Wir legen das Enddatum fest
                m_PaymentsToClose.setDateEnd(dNow);

                // print report
                printPayments("Printer.CloseCash");

                // Wir zeigen die Nachricht
                JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.closecashok"), AppLocal.getIntString("message.title"), JOptionPane.INFORMATION_MESSAGE);
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotclosecash"), e);
                msg.show(this);
            }

            try {
                loadData();
            } catch (BasicException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("label.noticketstoclose"), e);
                msg.show(this);
            }
        }
    }    
    
    private void m_jPrintCashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jPrintCashActionPerformed
        // print report
        printPayments("Printer.PartialCash");
    }//GEN-LAST:event_m_jPrintCashActionPerformed

    public static boolean isInteger(String str) { 
        try {  
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){  
            return false;  
        }  
    }

    public static boolean isDouble(String str) { 
        try {  
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){  
            return false;  
        }  
    }
    
    private void TextFieldFocusLost(javax.swing.JTextField tf, double value) {
        if (isInteger(tf.getText())) {
            cc.setCash(value, Integer.parseInt(tf.getText().trim()));
        } else {
            cc.setCash(value, 0);
        }
        if (tf.getText().trim().equals("") || isInteger(tf.getText())) {
            tf.setBackground(Color.white);
        } else {
            tf.setBackground(Color.red);
        }
        checkCashCountFields();        
        m_jCash3.setText(Formats.CURRENCY.formatValue(cc.getDifference()));
    }

    private void TextFieldResFocusLost(javax.swing.JTextField tf, double value) {
        if (isInteger(tf.getText())) {
            cc.setCashRes(value, Integer.parseInt(tf.getText().trim()));
        } else {
            cc.setCashRes(value, 0);
        }
        if (tf.getText().trim().equals("") || isInteger(tf.getText())) {
            tf.setBackground(Color.white);
        } else {
            tf.setBackground(Color.red);
        }
        checkCashCountFields();        
        m_jCash3.setText(Formats.CURRENCY.formatValue(cc.getDifference()));
    }

    private void jTextField1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField1FocusLost
        TextFieldFocusLost(jTextField1, 0.01);
    }//GEN-LAST:event_jTextField1FocusLost

    private void jTextField2FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField2FocusLost
        TextFieldFocusLost(jTextField2, 0.02);
    }//GEN-LAST:event_jTextField2FocusLost

    private void jTextField3FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField3FocusLost
        TextFieldFocusLost(jTextField3, 0.05);
    }//GEN-LAST:event_jTextField3FocusLost

    private void jTextField4FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField4FocusLost
        TextFieldFocusLost(jTextField4, 0.1);
    }//GEN-LAST:event_jTextField4FocusLost

    private void jTextField5FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField5FocusLost
        TextFieldFocusLost(jTextField5, 0.2);
    }//GEN-LAST:event_jTextField5FocusLost

    private void jTextField6FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField6FocusLost
        TextFieldFocusLost(jTextField6, 0.5);
    }//GEN-LAST:event_jTextField6FocusLost

    private void jTextField7FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField7FocusLost
        TextFieldFocusLost(jTextField7, 1);
    }//GEN-LAST:event_jTextField7FocusLost

    private void jTextField8FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField8FocusLost
        TextFieldFocusLost(jTextField8, 2);
    }//GEN-LAST:event_jTextField8FocusLost

    private void m_jBookCashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jBookCashActionPerformed

        int res = JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.wannabookdifference"), AppLocal.getIntString("message.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {

            try {

                // Differenz ausbuchen
                if (cc.getDifference() != 0.0) {
                    Object[] payment = new Object[9];
                    payment[0] = UUID.randomUUID().toString();
                    payment[1] = m_App.getActiveCashIndex();
                    payment[2] = new Date();
                    payment[3] = UUID.randomUUID().toString();
                    if (cc.getDifference() > 0) {
                        payment[4] = "cashin";
                    } else {
                        payment[4] = "cashout";
                    }
                    payment[5] = cc.getDifference();
                    payment[6] = "Kassendifferenz";
                    payment[7] = "DifferenzSollIst";
                    payment[8] = m_App.getAppUserView().getUser().getId();
                    SentenceExec ex = m_dlSales.getPaymentMovementInsert();
                    ex.exec(payment);
                }

                // CashCount speichern
                Object[] oc = new Object[3];
                oc[0] = m_App.getActiveCashIndex();
                new StaticSentence(m_App.getSession(), "DELETE FROM CLOSEDCASH_COUNT WHERE MONEY = ? ", new SerializerWriteBasic(new Datas[]{Datas.STRING})).exec(oc);
                for (int i=0; i<15; i++) {
                    oc[1] = cc.m_cash[i];
                    oc[2] = cc.getCash(cc.m_cash[i]);
                    new StaticSentence(m_App.getSession(), "INSERT INTO CLOSEDCASH_COUNT (MONEY, VALUE, COUNT) VALUES (?, ?, ?)", new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.DOUBLE, Datas.INT})).exec(oc);
                }

                loadData();

            } catch (BasicException ex1) {
                Logger.getLogger(JPanelCloseMoney.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }//GEN-LAST:event_m_jBookCashActionPerformed

    private void jTextField16FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField16FocusLost
        TextFieldResFocusLost(jTextField16, 0.01);
    }//GEN-LAST:event_jTextField16FocusLost

    private void jTextField17FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField17FocusLost
        TextFieldResFocusLost(jTextField17, 0.02);
    }//GEN-LAST:event_jTextField17FocusLost

    private void jTextField18FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField18FocusLost
        TextFieldResFocusLost(jTextField18, 0.05);
    }//GEN-LAST:event_jTextField18FocusLost

    private void jTextField20FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField20FocusLost
        TextFieldResFocusLost(jTextField20, 0.2);
    }//GEN-LAST:event_jTextField20FocusLost

    private void jTextField21FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField21FocusLost
        TextFieldResFocusLost(jTextField21, 0.5);
    }//GEN-LAST:event_jTextField21FocusLost

    private void jTextField22FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField22FocusLost
        TextFieldResFocusLost(jTextField22, 1.0);
    }//GEN-LAST:event_jTextField22FocusLost

    private void jTextField19FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField19FocusLost
        TextFieldResFocusLost(jTextField19, 0.1);
    }//GEN-LAST:event_jTextField19FocusLost

    private void jTextField23FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField23FocusLost
        TextFieldResFocusLost(jTextField23, 2.0);
    }//GEN-LAST:event_jTextField23FocusLost

    private void jTextField30FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField30FocusLost
        TextFieldResFocusLost(jTextField30, 500.0);
    }//GEN-LAST:event_jTextField30FocusLost

    private void jTextField29FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField29FocusLost
        TextFieldResFocusLost(jTextField29, 200.0);
    }//GEN-LAST:event_jTextField29FocusLost

    private void jTextField28FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField28FocusLost
        TextFieldResFocusLost(jTextField28, 100.0);
    }//GEN-LAST:event_jTextField28FocusLost

    private void jTextField27FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField27FocusLost
        TextFieldResFocusLost(jTextField27, 50.0);
    }//GEN-LAST:event_jTextField27FocusLost

    private void jTextField26FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField26FocusLost
        TextFieldResFocusLost(jTextField26, 20.0);
    }//GEN-LAST:event_jTextField26FocusLost

    private void jTextField25FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField25FocusLost
        TextFieldResFocusLost(jTextField25, 10.0);
    }//GEN-LAST:event_jTextField25FocusLost

    private void jTextField24FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField24FocusLost
        TextFieldResFocusLost(jTextField24, 5.0);
    }//GEN-LAST:event_jTextField24FocusLost

    private void jTextField15FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField15FocusLost
        TextFieldFocusLost(jTextField15, 500);
    }//GEN-LAST:event_jTextField15FocusLost

    private void jTextField14FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField14FocusLost
        TextFieldFocusLost(jTextField14, 200);
    }//GEN-LAST:event_jTextField14FocusLost

    private void jTextField13FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField13FocusLost
        TextFieldFocusLost(jTextField13, 100);
    }//GEN-LAST:event_jTextField13FocusLost

    private void jTextField12FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField12FocusLost
        TextFieldFocusLost(jTextField12, 50);
    }//GEN-LAST:event_jTextField12FocusLost

    private void jTextField11FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField11FocusLost
        TextFieldFocusLost(jTextField11, 20);
    }//GEN-LAST:event_jTextField11FocusLost

    private void jTextField10FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField10FocusLost
        TextFieldFocusLost(jTextField10, 10);
    }//GEN-LAST:event_jTextField10FocusLost

    private void jTextField9FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField9FocusLost
        TextFieldFocusLost(jTextField9, 5);
    }//GEN-LAST:event_jTextField9FocusLost

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
        if (jCheckBox1.isSelected()) {
            jTextField1.setEditable(false);
            jTextField1.setFocusable(false);
            jTextField2.setEditable(false);
            jTextField2.setFocusable(false);
            jTextField3.setEditable(false);
            jTextField3.setFocusable(false);
            jTextField4.setEditable(false);
            jTextField4.setFocusable(false);
            jTextField5.setEditable(false);
            jTextField5.setFocusable(false);
            jTextField6.setEditable(false);
            jTextField6.setFocusable(false);
            jTextField7.setEditable(false);
            jTextField7.setFocusable(false);
            jTextField8.setEditable(false);
            jTextField8.setFocusable(false);
            jTextField9.setEditable(false);
            jTextField9.setFocusable(false);
            jTextField10.setEditable(false);
            jTextField10.setFocusable(false);
            jTextField11.setEditable(false);
            jTextField11.setFocusable(false);
            jTextField12.setEditable(false);
            jTextField12.setFocusable(false);
            jTextField13.setEditable(false);
            jTextField13.setFocusable(false);
            jTextField14.setEditable(false);
            jTextField14.setFocusable(false);
            jTextField15.setEditable(false);
            jTextField15.setFocusable(false);
            
            jTextField16.setEditable(true);
            jTextField16.setFocusable(true);
            jTextField17.setEditable(true);
            jTextField17.setFocusable(true);
            jTextField18.setEditable(true);
            jTextField18.setFocusable(true);
            jTextField19.setEditable(true);
            jTextField19.setFocusable(true);
            jTextField20.setEditable(true);
            jTextField20.setFocusable(true);
            jTextField21.setEditable(true);
            jTextField21.setFocusable(true);
            jTextField22.setEditable(true);
            jTextField22.setFocusable(true);
            jTextField23.setEditable(true);
            jTextField23.setFocusable(true);
            jTextField24.setEditable(true);
            jTextField24.setFocusable(true);
            jTextField25.setEditable(true);
            jTextField25.setFocusable(true);
            jTextField26.setEditable(true);
            jTextField26.setFocusable(true);
            jTextField27.setEditable(true);
            jTextField27.setFocusable(true);
            jTextField28.setEditable(true);
            jTextField28.setFocusable(true);
            jTextField29.setEditable(true);
            jTextField29.setFocusable(true);
            jTextField30.setEditable(true);
            jTextField30.setFocusable(true);
        } else {
            jTextField1.setEditable(true);
            jTextField1.setFocusable(true);
            jTextField2.setEditable(true);
            jTextField2.setFocusable(true);
            jTextField3.setEditable(true);
            jTextField3.setFocusable(true);
            jTextField4.setEditable(true);
            jTextField4.setFocusable(true);
            jTextField5.setEditable(true);
            jTextField5.setFocusable(true);
            jTextField6.setEditable(true);
            jTextField6.setFocusable(true);
            jTextField7.setEditable(true);
            jTextField7.setFocusable(true);
            jTextField8.setEditable(true);
            jTextField8.setFocusable(true);
            jTextField9.setEditable(true);
            jTextField9.setFocusable(true);
            jTextField10.setEditable(true);
            jTextField10.setFocusable(true);
            jTextField11.setEditable(true);
            jTextField11.setFocusable(true);
            jTextField12.setEditable(true);
            jTextField12.setFocusable(true);
            jTextField13.setEditable(true);
            jTextField13.setFocusable(true);
            jTextField14.setEditable(true);
            jTextField14.setFocusable(true);
            jTextField15.setEditable(true);
            jTextField15.setFocusable(true);
            
            jTextField16.setEditable(false);
            jTextField16.setFocusable(false);
            jTextField17.setEditable(false);
            jTextField17.setFocusable(false);
            jTextField18.setEditable(false);
            jTextField18.setFocusable(false);
            jTextField19.setEditable(false);
            jTextField19.setFocusable(false);
            jTextField20.setEditable(false);
            jTextField20.setFocusable(false);
            jTextField21.setEditable(false);
            jTextField21.setFocusable(false);
            jTextField22.setEditable(false);
            jTextField22.setFocusable(false);
            jTextField23.setEditable(false);
            jTextField23.setFocusable(false);
            jTextField24.setEditable(false);
            jTextField24.setFocusable(false);
            jTextField25.setEditable(false);
            jTextField25.setFocusable(false);
            jTextField26.setEditable(false);
            jTextField26.setFocusable(false);
            jTextField27.setEditable(false);
            jTextField27.setFocusable(false);
            jTextField28.setEditable(false);
            jTextField28.setFocusable(false);
            jTextField29.setEditable(false);
            jTextField29.setFocusable(false);
            jTextField30.setEditable(false);
            jTextField30.setFocusable(false);
        }
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private boolean checkCashCountFieldsFilled() {
        boolean bo = false;
        
        if (!jTextField1.getText().trim().equals("")) bo = true;
        if (!jTextField2.getText().trim().equals("")) bo = true;
        if (!jTextField3.getText().trim().equals("")) bo = true;
        if (!jTextField4.getText().trim().equals("")) bo = true;
        if (!jTextField5.getText().trim().equals("")) bo = true;
        if (!jTextField6.getText().trim().equals("")) bo = true;
        if (!jTextField7.getText().trim().equals("")) bo = true;
        if (!jTextField8.getText().trim().equals("")) bo = true;
        if (!jTextField9.getText().trim().equals("")) bo = true;
        if (!jTextField10.getText().trim().equals("")) bo = true;
        if (!jTextField11.getText().trim().equals("")) bo = true;
        if (!jTextField12.getText().trim().equals("")) bo = true;
        if (!jTextField13.getText().trim().equals("")) bo = true;
        if (!jTextField14.getText().trim().equals("")) bo = true;
        if (!jTextField15.getText().trim().equals("")) bo = true;
        
        return bo;
    }
    
    private void checkCashCountFields() {
        if (checkCashCountFieldsFilled() == true) {
            m_jCash3.setText(Formats.CURRENCY.formatValue(cc.getDifference()));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelTop;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField20;
    private javax.swing.JTextField jTextField21;
    private javax.swing.JTextField jTextField22;
    private javax.swing.JTextField jTextField23;
    private javax.swing.JTextField jTextField24;
    private javax.swing.JTextField jTextField25;
    private javax.swing.JTextField jTextField26;
    private javax.swing.JTextField jTextField27;
    private javax.swing.JTextField jTextField28;
    private javax.swing.JTextField jTextField29;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField30;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JButton m_jBookCash;
    private javax.swing.JTextField m_jCash0;
    private javax.swing.JTextField m_jCash1;
    private javax.swing.JTextField m_jCash2;
    private javax.swing.JTextField m_jCash3;
    private javax.swing.JButton m_jCloseCash;
    private javax.swing.JButton m_jCloseCashTop;
    private javax.swing.JTextField m_jMaxDate;
    private javax.swing.JTextField m_jMinDate;
    private javax.swing.JTextField m_jNoCashSales;
    private javax.swing.JButton m_jPrintCash;
    private javax.swing.JButton m_jPrintCashTop;
    private javax.swing.JTextField m_jSalesSubtotal;
    private javax.swing.JTextField m_jSalesTaxes;
    private javax.swing.JTextField m_jSalesTotal;
    private javax.swing.JScrollPane m_jScrollTableTicket;
    private javax.swing.JTextField m_jSequence;
    private javax.swing.JTable m_jTicketTable;
    // End of variables declaration//GEN-END:variables

}
