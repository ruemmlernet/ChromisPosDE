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

package uk.chromis.data.gui;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import uk.chromis.data.loader.QBFCompareEnum;

/**
 *
 * @author adrian
 */
public class ListQBFModelNumber extends AbstractListModel implements ComboBoxModel {

    private Object[] m_items;
    private Object m_sel;

    /**
     * Creates a new instance of ListQBFModelNumber
     *
     * @param items
     */
//    public ListQBFModelNumber() {
//    private ListQBFModelNumber(Object... items) {
    public ListQBFModelNumber(Object... items) {
        m_items = items;
        m_sel = m_items[0];
    }

//    m_items = new Object[] {
    /**
     *
     * @return
     */
    public static ListQBFModelNumber getMandatoryString() {
        return new ListQBFModelNumber(
                QBFCompareEnum.COMP_NONE,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_RE,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getMandatoryNumber() {
        return new ListQBFModelNumber(
                QBFCompareEnum.COMP_NONE,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getNonMandatoryString() {
        return new ListQBFModelNumber(
                QBFCompareEnum.COMP_NONE,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_RE,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS,
                QBFCompareEnum.COMP_ISNULL,
                QBFCompareEnum.COMP_ISNOTNULL
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getNonMandatoryProduct() {
        return new ListQBFModelNumber(
                null,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_RE,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getNonMandatoryPrice() {
        return new ListQBFModelNumber(
                null,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_LESSOREQUALS,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_GREATEROREQUALS
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getNonMandatoryNumber() {
        return new ListQBFModelNumber(
                QBFCompareEnum.COMP_NONE,
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS,
                QBFCompareEnum.COMP_ISNULL,
                QBFCompareEnum.COMP_ISNOTNULL
        );
    }

    /**
     *
     * @return
     */
    public static ListQBFModelNumber getOverrideMandatoryNumber() {
        return new ListQBFModelNumber(
                QBFCompareEnum.COMP_EQUALS,
                QBFCompareEnum.COMP_DISTINCT,
                QBFCompareEnum.COMP_GREATER,
                QBFCompareEnum.COMP_LESS,
                QBFCompareEnum.COMP_GREATEROREQUALS,
                QBFCompareEnum.COMP_LESSOREQUALS
        );
    }

    @Override
    public Object getElementAt(int index) {

        return m_items[index];
    }

    @Override
    public int getSize() {
        return m_items.length;
    }

    @Override
    public Object getSelectedItem() {
        return m_sel;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        m_sel = anItem;
    }
}
