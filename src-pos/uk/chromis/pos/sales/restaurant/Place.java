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

package uk.chromis.pos.sales.restaurant;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import uk.chromis.basic.BasicException;
import uk.chromis.data.gui.NullIcon;
import uk.chromis.data.loader.DataRead;
import uk.chromis.data.loader.SerializableRead;

/**
 *
 *
 */
public class Place implements SerializableRead, java.io.Serializable {

    private static final long serialVersionUID = 8652254694281L;
    private static final Icon ICO_OCU = new ImageIcon(Place.class.getResource("/uk/chromis/images/edit_group.png"));
    private static final Icon ICO_FRE = new NullIcon(22, 22);

    private String m_sId;
    private String m_sName;
    private int m_ix;
    private int m_iy;
    private int m_diffx;
    private int m_diffy;
    private String m_sfloor;
    private String m_customer;
    private String m_waiter;
    private String m_ticketId;
    private Boolean m_tableMoved;
    private Boolean m_changed = false;

    private boolean m_bPeople;
    private JButton m_btn;

    /**
     * Creates a new instance of TablePlace
     */
    public Place() {
    }

    /**
     *
     * @param dr
     * @throws BasicException
     */
    @Override
    public void readValues(DataRead dr) throws BasicException {
        m_sId = dr.getString(1);
        m_sName = dr.getString(2);
        m_ix = dr.getInt(3).intValue();
        m_iy = dr.getInt(4).intValue();
        m_sfloor = dr.getString(5);
        m_customer = dr.getString(6);
        m_waiter = dr.getString(7);
        m_ticketId = dr.getString(8);
        m_tableMoved = dr.getBoolean(9);

        m_bPeople = false;
        m_btn = new JButton();

        m_btn.setFocusPainted(false);
        m_btn.setFocusable(false);
        m_btn.setRequestFocusEnabled(false);
        m_btn.setHorizontalTextPosition(SwingConstants.CENTER);
        m_btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        m_btn.setIcon(ICO_FRE);
        m_btn.setText(m_sName);
        m_btn.setMargin(new Insets(2, 5, 2, 5));

        m_diffx=0;
        m_diffy=0;
    }


    public String getId() {
        return m_sId;
    }

    public String getTicketID(){
        return m_ticketId;
    }
    public String getName() {
        return m_sName;
    }

    public int getX() {
        return m_ix;
    }

    public int getY() {
        return m_iy;
    }

    public void setX(int x) {
        this.m_ix = x;
    }

    public void setY(int y) {
        this.m_iy = y;
    }

    public int getDiffX() {
        return m_diffx;
    }

    public int getDiffY() {
        return m_diffy;
    }

    public void setDiffX(int x) {
        this.m_diffx = x;
    }

    public void setDiffY(int y) {
        this.m_diffy = y;
    }
    
    public Boolean getChanged() {
        return m_changed;
    }

    public void setChanged(Boolean changed) {
        this.m_changed = changed;
    }    
    
    
    public String getFloor() {
        return m_sfloor;
    }

    public JButton getButton() {
        return m_btn;
    }

    public String getCustomer() {
        return m_customer;
    }

    public String getWaiter() {
        return m_waiter;
    }

    public boolean hasPeople() {
        return m_bPeople;
    }

    public void setPeople(boolean bValue) {
        m_bPeople = bValue;
        m_btn.setIcon(bValue ? ICO_OCU : ICO_FRE);
    }

    public void setButtonBounds() {
        Dimension d = m_btn.getPreferredSize();
        m_btn.setPreferredSize(new Dimension(d.width + 30, d.height + 15));
        d = m_btn.getPreferredSize();
        m_btn.setBounds(m_ix - d.width / 2, m_iy - d.height / 2, d.width, d.height);
    }

    public void setButtonText(String btnText) {
        m_btn.setText(btnText);
    }

}
