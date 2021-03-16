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

package uk.chromis.data.user;
import uk.chromis.basic.BasicException;
import uk.chromis.data.loader.SentenceExec;
import uk.chromis.data.loader.TableDefinition;

/**
 *
 *   
 */
public class SaveProvider {
    
    /**
     *
     */
    protected SentenceExec m_sentupdate;

    /**
     *
     */
    protected SentenceExec m_sentinsert;

    /**
     *
     */
    protected SentenceExec m_sentdelete;
    
    /** Creates a new instance of SavePrSentence
     * @param sentupdate
     * @param sentdelete
     * @param sentinsert */
    public SaveProvider(SentenceExec sentupdate, SentenceExec sentinsert, SentenceExec sentdelete) {
        m_sentupdate = sentupdate;
        m_sentinsert = sentinsert;
        m_sentdelete = sentdelete;
    }

    /**
     *
     * @param table
     */
    public SaveProvider(TableDefinition table) {
        m_sentupdate = table.getUpdateSentence();
        m_sentdelete = table.getDeleteSentence();
        m_sentinsert = table.getInsertSentence();
    }

    /**
     *
     * @param table
     * @param fields
     */
    public SaveProvider(TableDefinition table, int[] fields) {
        m_sentupdate = table.getUpdateSentence(fields);
        m_sentdelete = table.getDeleteSentence();
        m_sentinsert = table.getInsertSentence(fields);
    }
    
    /**
     *
     * @return
     */
    public boolean canDelete() {
        return m_sentdelete != null;      
    }

    /**
     *
     * @param value
     * @return
     * @throws BasicException
     */
    public int deleteData(Object value) throws BasicException {
        return m_sentdelete.exec(value);
    }
    
    /**
     *
     * @return
     */
    public boolean canInsert() {
        return m_sentinsert != null;          
    }
    
    /**
     *
     * @param value
     * @return
     * @throws BasicException
     */
    public int insertData(Object value) throws BasicException {
        return m_sentinsert.exec(value);
    }
    
    /**
     *
     * @return
     */
    public boolean canUpdate() {
        return m_sentupdate != null;      
    }

    /**
     *
     * @param value
     * @return
     * @throws BasicException
     */
    public int updateData(Object value) throws BasicException {
        return m_sentupdate.exec(value);
    }
}
