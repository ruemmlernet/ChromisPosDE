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

package uk.chromis.pos.printer.escpos;

import uk.chromis.pos.printer.DeviceTicket;

/**
 *
 * @author adrianromero
 */
public class DeviceDisplaySurePOS extends DeviceDisplaySerial {
    
    private UnicodeTranslator trans;
    
    /**
     *
     * @param display
     */
    public DeviceDisplaySurePOS(PrinterWritter display) { 
        trans = new UnicodeTranslatorSurePOS();
        init(display);                
    }
   
    /**
     *
     */
    @Override
    public void initVisor() {
        display.write(new byte[]{0x00, 0x01}); // IBM Mode
        display.write(new byte[]{0x02}); // Set the code page
        display.write(trans.getCodeTable());
        display.write(new byte[]{0x11}); // HIDE CURSOR
        display.write(new byte[]{0x14}); // HIDE CURSOR
        display.write(new byte[]{0x10, 0x00}); // VISOR HOME
        display.flush();
    }

    /**
     *
     */
    @Override
    public void repaintLines() {
        display.write(new byte[]{0x10, 0x00}); // VISOR HOME
        display.write(trans.transString(DeviceTicket.alignLeft(m_displaylines.getLine1(), 20)));
        display.write(new byte[]{0x10, 0x14});
        display.write(trans.transString(DeviceTicket.alignLeft(m_displaylines.getLine2(), 20)));        
        display.flush();
    }
}
