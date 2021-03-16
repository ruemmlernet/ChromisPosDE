/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c) 2015-2018
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


package uk.chromis.pos.admin;

import java.awt.Component;
import java.util.List;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import uk.chromis.basic.BasicException;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.sync.DataLogicSync;
import uk.chromis.pos.sync.SitesInfo;
import uk.chromis.data.loader.SessionFactory;

/**
 *
 * @author John
 */
public class RolesAddNewEntry extends javax.swing.JDialog {

    private DataLogicAdmin dlAdmin;
    private DataLogicSync dlSync;
    private static Boolean result = false;
    private static String guid;

    /**
     * Creates new form NewJDialog
     */
    public RolesAddNewEntry(java.awt.Frame parent, Boolean modal) {

        super(parent, modal);
        initComponents();
        dlSync = new DataLogicSync();
        dlSync.init(SessionFactory.getInstance().getSession());
        if (!dlSync.isCentral()) {
            jButtonAddToAll.setVisible(false);
        }
    }

    public RolesAddNewEntry(java.awt.Dialog parent, Boolean modal) {
        super(parent, modal);
        initComponents();
        dlSync = new DataLogicSync();
        dlSync.init(SessionFactory.getInstance().getSession());
        if (!dlSync.isCentral()) {
            jButtonAddToAll.setVisible(false);
        }
    }

    public static boolean showDialog(Component parent, String siteGuid) {

        guid = siteGuid;
        Window window = getWindow(parent);
        RolesAddNewEntry mydialog;

        if (window instanceof Frame) {
            mydialog = new RolesAddNewEntry((Frame) window, true);
        } else {
            mydialog = new RolesAddNewEntry((Dialog) window, true);
        }

        //  mydialog.pack();
        mydialog.setLocationRelativeTo(parent);

        mydialog.setVisible(true);
        return (result);
    }

    protected static Window getWindow(Component parent) {
        if (parent == null) {
            return new JFrame();
        } else if (parent instanceof Frame || parent instanceof Dialog) {
            return (Window) parent;
        } else {
            return getWindow(parent.getParent());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonCancel = new javax.swing.JButton();
        m_jEntryDisplayedName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        m_jEntryClassName = new javax.swing.JTextField();
        m_jEntrySection = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButtonAdd = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        m_jEntryDescription = new javax.swing.JTextArea();
        jButtonAddToAll = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        setTitle(bundle.getString("label.addnewentry")); // NOI18N

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        m_jEntryDisplayedName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                m_jEntryDisplayedNameFocusLost(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setText(bundle.getString("label.entrysection")); // NOI18N

        m_jEntryClassName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                m_jEntryClassNameFocusLost(evt);
            }
        });

        m_jEntrySection.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                m_jEntrySectionFocusLost(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel4.setText(bundle.getString("label.entrydescription")); // NOI18N

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setText(bundle.getString("label.entryclassname")); // NOI18N

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel2.setText(bundle.getString("label.entrydisplayname")); // NOI18N

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        m_jEntryDescription.setColumns(20);
        m_jEntryDescription.setRows(5);
        jScrollPane1.setViewportView(m_jEntryDescription);

        jButtonAddToAll.setText("Add To All Sites");
        jButtonAddToAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddToAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 177, Short.MAX_VALUE)
                        .addComponent(jButtonCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAddToAll, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(m_jEntryDisplayedName, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(m_jEntrySection, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(m_jEntryClassName, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 481, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 481, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jEntrySection, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jEntryDisplayedName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jEntryClassName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonAddToAll, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        result = false;
        dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed

        String className = m_jEntryClassName.getText().replace(" ", "");
        String section = m_jEntrySection.getText();
        String displayName = m_jEntryDisplayedName.getText();
        String description = m_jEntryDescription.getText();

        if (className.equals("") || section.equals("") || displayName.equals("")) {
            JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.addnewentryerror"), "Notice", JOptionPane.INFORMATION_MESSAGE);

        } else {

            try {
                dlAdmin = new DataLogicAdmin();
                dlAdmin.init(SessionFactory.getInstance().getSession());
                dlAdmin.insertEntry(
                        new Object[]{className, section, displayName, description, guid});

            } catch (BasicException ex) {
            }
            result = true;
            dispose();
        }

    }//GEN-LAST:event_jButtonAddActionPerformed

    private void m_jEntrySectionFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_m_jEntrySectionFocusLost
        String section = m_jEntrySection.getText().trim();
        m_jEntrySection.setText(section);
    }//GEN-LAST:event_m_jEntrySectionFocusLost

    private void m_jEntryDisplayedNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_m_jEntryDisplayedNameFocusLost
        String section = m_jEntryDisplayedName.getText().trim();
        m_jEntryDisplayedName.setText(section);
    }//GEN-LAST:event_m_jEntryDisplayedNameFocusLost

    private void m_jEntryClassNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_m_jEntryClassNameFocusLost
        String section = m_jEntryClassName.getText().trim();
        m_jEntryClassName.setText(section);
    }//GEN-LAST:event_m_jEntryClassNameFocusLost

    private void jButtonAddToAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToAllActionPerformed
        String className = m_jEntryClassName.getText().replace(" ", "");
        String section = m_jEntrySection.getText();
        String displayName = m_jEntryDisplayedName.getText();
        String description = m_jEntryDescription.getText();

        if (className.equals("") || section.equals("") || displayName.equals("")) {
            JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.addnewentryerror"), "Notice", JOptionPane.INFORMATION_MESSAGE);

        } else {

            try {

                dlAdmin = new DataLogicAdmin();
                dlAdmin.init(SessionFactory.getInstance().getSession());

                List<SitesInfo> a = dlSync.getSitesList().list();
                if (dlSync.getCentralGuid().equals(dlSync.getSiteGuid())) {
                    SitesInfo tempSite = new SitesInfo(dlSync.getCentralGuid(), dlSync.getCentralName());
                    a.add(0, tempSite);
                }

                for (SitesInfo site : a) {
                    dlAdmin.insertEntry(
                            new Object[]{className, section, displayName, description, site.getGuid()});
                }
            } catch (BasicException ex) {
            }
            result = true;
            dispose();
        }
    }//GEN-LAST:event_jButtonAddToAllActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonAddToAll;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField m_jEntryClassName;
    private javax.swing.JTextArea m_jEntryDescription;
    private javax.swing.JTextField m_jEntryDisplayedName;
    private javax.swing.JTextField m_jEntrySection;
    // End of variables declaration//GEN-END:variables
}
