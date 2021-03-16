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

package uk.chromis.pos.printer.javapos;

import jpos.JposException;
import jpos.LineDisplay;
import jpos.LineDisplayConst;
import uk.chromis.pos.printer.DeviceDisplay;
import uk.chromis.pos.printer.DeviceDisplayBase;
import uk.chromis.pos.printer.DeviceDisplayImpl;
import uk.chromis.pos.printer.TicketPrinterException;
 
/**
 *
 *   
 */
public class DeviceDisplayJavaPOS implements DeviceDisplay, DeviceDisplayImpl {
    
    private String m_sName;
    private LineDisplay m_ld;
    
    private DeviceDisplayBase m_displaylines;
    
    /** Creates a new instance of DeviceDisplayJavaPOS
     * @param sDeviceName
     * @throws uk.chromis.pos.printer.TicketPrinterException */
    public DeviceDisplayJavaPOS(String sDeviceName) throws TicketPrinterException {
        m_sName = sDeviceName;
        
        m_ld = new LineDisplay();
        try {       
            m_ld.open(m_sName);
            m_ld.claim(10000);
            m_ld.setDeviceEnabled(true);
        } catch (JposException e) {
            throw new TicketPrinterException(e.getMessage(), e);
        }

        m_displaylines = new DeviceDisplayBase(this);
   }
    
    /**
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return m_sName;
    }    

    /**
     *
     * @return
     */
    @Override
    public String getDisplayDescription() {
        return null;
    }      

    /**
     *
     * @return
     */
    @Override
    public javax.swing.JComponent getDisplayComponent() {
        return null;
    }
    
    /**
     *
     * @param animation
     * @param sLine1
     * @param sLine2
     */
    @Override
    public void writeVisor(int animation, String sLine1, String sLine2) {
        m_displaylines.writeVisor(animation, sLine1, sLine2);
    }

    /**
     *
     * @param sLine1
     * @param sLine2
     */
    @Override
    public void writeVisor(String sLine1, String sLine2) {        
        m_displaylines.writeVisor(sLine1, sLine2);
    }
     
    /**
     *
     */
    @Override
    public void clearVisor() {
        m_displaylines.clearVisor();
    }
    
    /**
     *
     */
    @Override
    public void repaintLines() {
        try {
            m_ld.displayTextAt(0, 0, m_displaylines.getLine1(), LineDisplayConst.DISP_DT_NORMAL);
            m_ld.displayTextAt(1, 0, m_displaylines.getLine2(), LineDisplayConst.DISP_DT_NORMAL);
        } catch (JposException e) {
        }
    }
    
    @Override
    public void finalize() throws Throwable {
   
        m_ld.setDeviceEnabled(false);
        m_ld.release();
        m_ld.close();
        
        super.finalize();
    }
}
