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

package uk.chromis.pos.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.codabar.CodabarBean;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.impl.int2of5.Interleaved2Of5Bean;
import org.krysalis.barcode4j.impl.postnet.POSTNETBean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.impl.upcean.EAN8Bean;
import org.krysalis.barcode4j.impl.upcean.UPCABean;
import org.krysalis.barcode4j.impl.upcean.UPCEBean;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

/**
 *
 * @author adrian
 */
public class BarcodeImage {

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeCodabar(String value) {
        AbstractBarcodeBean barcode = new CodabarBean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeCode39(String value) {
        AbstractBarcodeBean barcode = new Code39Bean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeInterleaved2Of5(String value) {
        AbstractBarcodeBean barcode = new Interleaved2Of5Bean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodePOSTNET(String value) {
        AbstractBarcodeBean barcode = new POSTNETBean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeUPCA(String value) {
        AbstractBarcodeBean barcode = new UPCABean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeUPCE(String value) {
        AbstractBarcodeBean barcode = new UPCEBean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeEAN13(String value) {
        AbstractBarcodeBean barcode = new EAN13Bean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcodeEAN8(String value) {
        AbstractBarcodeBean barcode = new EAN8Bean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    /**
     *
     * @param value
     * @return
     */
    public static Image getBarcode128(String value) {
        AbstractBarcodeBean barcode = new Code128Bean();
        barcode.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
        return getBarcode(value, barcode);
    }

    public static Image badBarcode() {
        return getBarcodeImage();
    }

    private static Image getBarcodeImage() {
        GetBarImage image = new GetBarImage();        
        return image.getImage();
    }

    private static Image getBarcode(String value, AbstractBarcodeBean barcode) {

        barcode.setModuleWidth(1.0);
        barcode.setBarHeight(40.0);
        barcode.setFontSize(10.0);
        barcode.setQuietZone(10.0);
        barcode.doQuietZone(true);
        BarcodeDimension dim = barcode.calcDimensions(value);
        int width = (int) dim.getWidth(0) + 20;
        int height = (int) dim.getHeight(0);

        BufferedImage imgtext = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgtext.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.BLACK);

        try {
            barcode.generateBarcode(new Java2DCanvasProvider(g2d, 0), value);
        } catch (IllegalArgumentException e) {
            g2d.drawRect(0, 0, width - 1, height - 1);
            g2d.drawString(value, 2, height - 3);
        }

        g2d.dispose();

        return imgtext;
    }

    private static class GetBarImage {

        private GetBarImage() {

        }

        private BufferedImage getImage() {
            BufferedImage imgtext = null;
            try {
                imgtext = ImageIO.read(getClass().getResourceAsStream("/uk/chromis/fixedimages/invalid1.png"));
            } catch (IOException ex) {
                Logger.getLogger(BarcodeImage.class.getName()).log(Level.SEVERE, null, ex);
            }
            return imgtext;
        }
    }
}
