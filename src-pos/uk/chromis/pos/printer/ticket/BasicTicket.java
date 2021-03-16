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

package uk.chromis.pos.printer.ticket;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


public abstract class BasicTicket implements PrintItem {

    protected java.util.List<PrintItem> m_aCommands;
    protected PrintItemLine pil;
    protected int m_iBodyHeight;
   
       /** Creates a new instance of AbstractTicket */
       public BasicTicket() {
           m_aCommands = new ArrayList<>();
           pil = null;
           m_iBodyHeight = 0;
       }

    protected abstract Font getBaseFont();
    protected abstract int getFontHeight();
    protected abstract double getImageScale();

    @Override
       public int getHeight() {
          return m_iBodyHeight;
       }

    /**
     *
     * @param g2d
     * @param x
     * @param y
     * @param width
     */
    @Override
       public void draw(Graphics2D g2d, int x, int y, int width) {

           int currenty = y;
           for (PrintItem pi : m_aCommands) {
               pi.draw(g2d, x, currenty, width);
               currenty += pi.getHeight();
           }
       }

    /**
     *
     * @return
     */
    public java.util.List<PrintItem> getCommands() {
          return m_aCommands;
       }

       // INTERFAZ PRINTER 2

    /**
     *
     * @param image
     */
           public void printImage(BufferedImage image) {

           PrintItem pi = new PrintItemImage(image, getImageScale());
           m_aCommands.add(pi);
           m_iBodyHeight += pi.getHeight();
       }

    /**
     *
     * @param type
     * @param position
     * @param code
     */
    public void printBarCode(String type, String position, String code) {
           PrintItem pi = new PrintItemBarcode(type, position, code, getImageScale());
           m_aCommands.add(pi);
           m_iBodyHeight += pi.getHeight();
       }

    /**
     *
     * @param iTextSize
     */
    public void beginLine(int iTextSize) {
           pil = new PrintItemLine(iTextSize, getBaseFont(), getFontHeight());
       }

    /**
     *
     * @param iStyle
     * @param sText
     */
    public void printText(int iStyle, String sText) {
           if (pil != null) {
               pil.addText(iStyle, sText);
           }
       }

    /**
     *
     */
    public void endLine() {
           if (pil != null) {
               m_aCommands.add(pil);
               m_iBodyHeight += pil.getHeight();
               pil = null;
           }
       }
 }