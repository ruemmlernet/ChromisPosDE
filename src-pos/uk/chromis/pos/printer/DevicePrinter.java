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

package uk.chromis.pos.printer;

import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 *   
 */
public interface DevicePrinter {
    
    public static final int SIZE_0 = 0;
    public static final int SIZE_1 = 1;
    public static final int SIZE_2 = 2;
    public static final int SIZE_3 = 3;
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_UNDERLINE = 2;
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;
    public static final int ALIGN_CENTER = 2;
    public static final String BARCODE_EAN13 = "EAN13";
    public static final String BARCODE_EAN8 = "EAN8";
    public static final String BARCODE_CODE128 = "CODE128";
    public static final String BARCODE_CODE39 = "CODE39";
    public static final String BARCODE_UPCA = "UPC-A";
    public static final String BARCODE_UPCE = "UPC-E";    
    public static final String POSITION_BOTTOM = "bottom";
    public static final String POSITION_NONE = "none";
    public String getPrinterName();
    public String getPrinterDescription();
    public JComponent getPrinterComponent();

    
    public void reset();
    
    // INTERFAZ PRINTER

    /**
     *
     */
    public void beginReceipt();
    public void printImage(BufferedImage image);
    public void printLogo(Byte iNumber);
    public Boolean printBarCode(String type, String position, String code);
    public void beginLine(int iTextSize);
    public void printText(int iStyle, String sText);
    public void endLine();
    public void endReceipt();   
    public void openDrawer();    

}
