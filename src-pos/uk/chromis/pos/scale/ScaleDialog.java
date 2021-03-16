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


package uk.chromis.pos.scale;

import java.awt.Component;
import javax.swing.ImageIcon;
import uk.chromis.beans.JNumberDialog;
import uk.chromis.pos.forms.AppLocal;

/**
 *
 * @author adrian
 */
public class ScaleDialog implements Scale {

    private Component parent;

    /**
     *
     * @param parent
     */
    public ScaleDialog(Component parent) {
        this.parent = parent;
    }

    /**
     *
     * @return
     * @throws ScaleException
     */
    @Override
    public Double readWeight() throws ScaleException {
        
        // Set title for grams Kilos, ounzes, pounds, ...
        return JNumberDialog.showEditNumber(parent, AppLocal.getIntString("label.scale"), AppLocal.getIntString("label.scaleinput"), new ImageIcon(ScaleDialog.class.getResource("/uk/chromis/images/ark2.png")));
    }
}
