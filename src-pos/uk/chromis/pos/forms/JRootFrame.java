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
package uk.chromis.pos.forms;

import java.awt.BorderLayout;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import uk.chromis.pos.instance.AppMessage;
import uk.chromis.pos.instance.InstanceManager;
import uk.chromis.pos.util.OSValidator;

/**
 *
 * @author adrianromero
 */
public class JRootFrame extends javax.swing.JFrame implements AppMessage {

    // Gestor de que haya solo una instancia corriendo en cada maquina.
    private InstanceManager m_instmanager = null;

    private JRootApp m_rootapp;
    private AppProperties m_props;
    private OSValidator m_OS;

    /**
     * Creates new form JRootFrame
     */
    public JRootFrame() {
        initComponents();
    }

    /**
     *
     * @param props
     */
    public void initFrame(AppProperties props) {
        m_OS = new OSValidator();
        m_props = props;
        m_rootapp = new JRootApp();
        int result = m_rootapp.initApp(m_props);
        if (result == m_rootapp.INIT_SUCCESS) {
            if ("true".equals(AppConfig.getInstance().getProperty("machine.uniqueinstance"))) {
                try {
                    m_instmanager = new InstanceManager(this);
                } catch (RemoteException | AlreadyBoundException e) {
                }
            }
            add(m_rootapp, BorderLayout.CENTER);
            try {
                this.setIconImage(ImageIO.read(JRootFrame.class.getResourceAsStream("/uk/chromis/fixedimages/smllogo.png")));
            } catch (IOException e) {
            }
            setTitle(AppLocal.APP_NAME + " - V" + AppLocal.APP_VERSION + AppLocal.APP_DEMO);
            pack();
            setLocationRelativeTo(null);
            // this.setResizable(false);         
            setVisible(true);
        } else {
        }

        /* this is now redundant should never get to this stage new dbmanager class handles checking
        if (result == m_rootapp.INIT_FAIL_CONFIG) {           
            new JFrmConfig(props).setVisible(true); // Show the configuration window.
        }
         */
    }
  
    /**
     *
     * @throws RemoteException
     */
    @Override
    public void restoreWindow() throws RemoteException {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (getExtendedState() == JFrame.ICONIFIED) {
                    setExtendedState(JFrame.NORMAL);
                }
                requestFocus();
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        m_rootapp.tseBackup();
//        m_rootapp.tryToClose();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.exit(0);
    }//GEN-LAST:event_formWindowClosed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}