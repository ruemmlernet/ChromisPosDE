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

package uk.chromis.pos.epm;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import uk.chromis.basic.BasicException;
import uk.chromis.data.loader.DataRead;
import uk.chromis.data.loader.Datas;
import uk.chromis.data.loader.PreparedSentence;
import uk.chromis.data.loader.SentenceExec;
import uk.chromis.data.loader.SentenceExecTransaction;
import uk.chromis.data.loader.SerializerRead;
import uk.chromis.data.loader.SerializerWriteBasicExt;
import uk.chromis.data.model.Field;
import uk.chromis.data.model.Row;
import uk.chromis.data.user.EditorRecord;
import uk.chromis.data.user.ListProviderCreator;
import uk.chromis.data.user.SaveProvider;
import uk.chromis.format.Formats;
import uk.chromis.pos.admin.JParamsSites;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.panels.JPanelTable2;
import uk.chromis.pos.sync.DataLogicSync;


public class BreaksPanel extends JPanelTable2 {

    private BreaksView jeditor;
    private JParamsSites m_paramsSite;
    private DataLogicSync dlSync;

    public BreaksPanel() {
    }

    @Override
    protected void init() {

        // DataLogicPresenceManagement dlPresenceManagement = (DataLogicPresenceManagement) app.getBean("uk.chromis.pos.epm.DataLogicPresenceManagement");
        dlSync = (DataLogicSync) app.getBean("uk.chromis.pos.sync.DataLogicSync");


        m_paramsSite = new JParamsSites();
        m_paramsSite.setVisible(dlSync.isCentral());
        m_paramsSite.init(app);
        m_paramsSite.addActionListener(new BreaksPanel.ReloadActionListener());

        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field("NAME", Datas.STRING, Formats.STRING, true, true, true),
                new Field("NOTES", Datas.STRING, Formats.STRING),
                new Field("VISIBLE", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("SITEGUID", Datas.STRING, Formats.STRING)
        );

        lpr = new ListProviderCreator(new PreparedSentence(app.getSession(),
                "SELECT ID, NAME, NOTES, VISIBLE, SITEGUID "
                + "FROM BREAKS "
                + "WHERE SITEGUID = ? "
                + "ORDER BY LOWER (NAME)",
                new SerializerWriteBasicExt(new Datas[]{Datas.OBJECT, Datas.STRING}, new int[]{1}),
                new BreaksPanel.BreaksSerializerRead()
        ),
                m_paramsSite);

        SentenceExec updatesent = new SentenceExecTransaction(app.getSession()) {
            @Override
            public int execInTransaction(Object params) throws BasicException {
                Object[] values = (Object[]) params;
                if (values[4] == null) {
                    // INSERT
                    values[4] = m_paramsSite.getSelectKey();
                    return new PreparedSentence(app.getSession(), "INSERT INTO BREAKS (ID, NAME, NOTES, VISIBLE, SITEGUID) VALUES (?, ?, ?, ?, ?)", new SerializerWriteBasicExt(row.getDatas(), new int[]{0, 1, 2, 3, 4})).exec(params);
                } else {
                    // UPDATE
                    return new PreparedSentence(app.getSession(), "UPDATE BREAKS SET NAME = ?, NOTES = ?, VISIBLE = ? WHERE ID = ? AND SITEGUID = ? ", new SerializerWriteBasicExt(row.getDatas(), new int[]{1, 2, 3, 0, 4})).exec(params);
                }
            }
        };

        SentenceExec insertsent = new SentenceExecTransaction(app.getSession()) {
            @Override
            public int execInTransaction(Object params) throws BasicException {
                Object[] values = (Object[]) params;
                // INSERT
                values[0] = UUID.randomUUID().toString();
                values[4] = m_paramsSite.getSelectKey();
                return new PreparedSentence(app.getSession(), "INSERT INTO BREAKS (ID, NAME, NOTES, VISIBLE, SITEGUID) VALUES (?, ?, ?, ?, ?)", new SerializerWriteBasicExt(row.getDatas(), new int[]{0, 1, 2, 3, 4})).exec(params);
            }
        };

        SentenceExec deletesent = new SentenceExecTransaction(app.getSession()) {
            @Override
            public int execInTransaction(Object params) throws BasicException {
                Object[] values = (Object[]) params;
                values[4] = m_paramsSite.getSelectKey();
                return new PreparedSentence(app.getSession(), "DELETE FROM BREAKS WHERE ID = ? AND SITEGUID = ? ", new SerializerWriteBasicExt(row.getDatas(), new int[]{0, 4})).exec(params);
            }
        };

        spr = new SaveProvider(updatesent, insertsent, deletesent);
        jeditor = new BreaksView(app, dirty, dlSync.getSiteGuid());
    }

    @Override
    public void activate() throws BasicException {
        m_paramsSite.activate();
        jeditor.activate();
        super.activate();
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Breaks");
    }

    @Override
    public Component getFilter() {
        return m_paramsSite.getComponent();
    }

    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }

    private class ReloadActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                BreaksPanel.this.bd.actionLoad();
            } catch (BasicException w) {
            }
        }
    }

    private class BreaksSerializerRead implements SerializerRead {

        @Override
        public Object readValues(DataRead dr) throws BasicException {
            return new Object[]{
                dr.getString(1),
                dr.getString(2),
                dr.getString(3),
                dr.getBoolean(4),
                dr.getString(5),
                ((Object[]) m_paramsSite.createValue())[1]};
        }
    }

}
