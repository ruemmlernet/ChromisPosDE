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

package uk.chromis.pos.reports;

import java.util.HashMap;
import java.util.Map;
import uk.chromis.pos.forms.AppLocal;

/**
 *
 *   
 */
public class ReportFieldsArray implements ReportFields {
    
    private Map m_keys = null;
    
    /** Creates a new instance of ReportFieldsArray
     * @param afields */
    public ReportFieldsArray(String[] afields) {
               
        // Creo el mapa de claves
        m_keys = new HashMap();
        for (int i = 0; i < afields.length; i++) {
            m_keys.put(afields[i], new Integer(i));
        }
    }
    
    /**
     *
     * @param record
     * @param field
     * @return
     * @throws ReportException
     */
    public Object getField(Object record, String field) throws ReportException {
        
        Integer i = (Integer) m_keys.get(field);
        if (i == null) {
            throw new ReportException(AppLocal.getIntString("exception.unavailablefield", new Object[] {field}));
        } else {
            Object[] arecord = (Object[]) record;
            if (arecord == null || i.intValue() < 0 || i.intValue() >= arecord.length) {
                throw new ReportException(AppLocal.getIntString("exception.unavailablefields"));
            } else {
                return arecord[i.intValue()];
            }
        }        
    }
}
