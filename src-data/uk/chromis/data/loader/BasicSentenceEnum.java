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

package uk.chromis.data.loader;

import uk.chromis.basic.BasicException;

/**
 *
 * @author adrian
 */
public class BasicSentenceEnum implements SentenceEnum {
    
    BaseSentence sent;
    DataResultSet SRS;
    
    /** Creates a new instance of AbstractSentenceEnum
     * @param sent */
    public BasicSentenceEnum(BaseSentence sent) {
        this.sent = sent;
        this.SRS = null;
    }
    
    /**
     *
     * @throws BasicException
     */
    public void load() throws BasicException {
        load(null);
    }

    /**
     *
     * @param params
     * @throws BasicException
     */
    public void load(Object params) throws BasicException {
        SRS = sent.openExec(params);
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public Object getCurrent() throws BasicException {
        if (SRS == null) {
            throw new BasicException(LocalRes.getIntString("exception.nodataset"));
        } 
        
        return SRS.getCurrent();  
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    public boolean next() throws BasicException {
        if (SRS == null) {
            throw new BasicException(LocalRes.getIntString("exception.nodataset"));
        } 
        
        if (SRS.next()) {
            return true;  
        } else {
            SRS.close();
            SRS = null;
            sent.closeExec();
            return false;
        }
    }
}
