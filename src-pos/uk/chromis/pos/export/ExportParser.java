/*
**    Chromis POS DE - The New Face of Open Source POS for Germany
**
**    Juergen Ruemmler IT-Solutions, Huenxe, Germany
**    Copyright (c) 2020 
**    https://www.ruemmler.net
**
**    This file is part of Chromis POS DE Version V0.96.0
**
**    Chromis POS DE is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS DE is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS DE.  If not, see <http://www.gnu.org/licenses/>
 */
package uk.chromis.pos.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.chromis.data.loader.Session;

/**
 *
 * @author Medicus
 */
public class ExportParser extends DefaultHandler {
    
    private StringBuilder text;    
    private static SAXParser m_sp = null;    
    
    private static final String CSV_COLUMNDELIMITER = ";";
    private static final String CSV_LINEDELIMITER = "\r\n";
    private static final String XML_LINEDELIMITER = "\r\n";
    
    private String[] inputKeyName;
    private String[] inputKeyType;
    private int inputKeyCount;
    private String[] xPfad;
    private String xmlPath;
    private String[] xmlColumn;
    private int xmlColumnCount;
    private int xPfadPos;
    private String efFormat;
    private String efFilename;
    private boolean efCreateWithoutContent;
    private String efSql;
    private Connection con;
    private Session s;
    private Statement stmt;
    private String SQL;
    private ResultSet rs;
    private String mPfad;
    private String message;
    private String[] mParams;
    private PrintWriter pwIndex;
    private String indexPuffer;
    private String schemaFile;
    private String schemaRessource;
    
    /**
     * Creates a new instance of ExportParser
     *
     * @param sess Database Session
     */
    public ExportParser(Session sess) {
        s = sess;
        xPfad = new String[20];
        xPfadPos = -1;
    }

    private void joinMessage(String msg) {
        if (msg.length() > 0) {
            if (message.length() > 0) {
                message = message.concat("\r\n\r\n").concat(msg);
            } else {
                message = msg;
            }
        }
    }
    
    /**
     *
     * @param sIn zu verarbeitende Ressource (xml)
     */
    public String doExport(String pfad, String sIn, String[] params) {
        mPfad = pfad;
        message = "";
        mParams = params;
        if ((!mPfad.endsWith("\"")) && (!mPfad.endsWith("/")) ) {
            mPfad = mPfad.concat("/");
        }
        
        String res = getResource(sIn);
        if (res.equals("")) {
            joinMessage("Kein Template ("+sIn+") für Export");
        } else {
            doExport(new StringReader(res));
        }
        
        return message;
    }

    /**
     *
     * @param in
     */
    private void doExport(Reader in) {
        try {
            if (m_sp == null) {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                m_sp = spf.newSAXParser();
            }
            m_sp.parse(new InputSource(in), this);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String getResource(String res) {
        String ret = "";
        try {        
            con = s.getConnection();
            stmt = (Statement) con.createStatement();
            SQL = "SELECT content FROM resources where name='"+res+"'";
            rs = stmt.executeQuery(SQL);
            rs.first();
            Blob bob = rs.getBlob("CONTENT");
            try {
                ret = new String(bob.getBytes(1, (int)bob.length()), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            rs = null;
            con = null;
        } catch (SQLException ex) {
            Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    private boolean doExportFile() {
        setParams();
        switch (efFormat.toLowerCase()) {
            case "csv":
                return doExportFileCSV();
        }
        return false;
    }
    
    private void doExportSchema(String sIn, String efFile) {
        String res = getResource(sIn);
        if (!res.equals("")) {
            // Datei zum Schreiben öffnen
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mPfad + efFile), StandardCharsets.UTF_8), true );
            } catch (IOException ex) {
                Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (pw != null) {
                pw.print(res + CSV_LINEDELIMITER);
                // Datei schliessen
                pw.flush();
                pw.close();
            }
        }
    }

    
    private void setParams() {
        efSql = efSql.replace("&gt;", ">");
        efSql = efSql.replace("&lt;", "<");
        
        for (int i=0; i<inputKeyCount; i++) {
            efSql = efSql.replace(":".concat(inputKeyName[i]), "'".concat(mParams[i]).concat("'"));
        }
    }
    
    private boolean doExportFileCSV() {
        Integer result = 0;
        String line;
        String value;
        try {        
            con = s.getConnection();
            stmt = (Statement) con.createStatement();
            rs = stmt.executeQuery(efSql);
            
            while (rs.next()) {
                result++;
                if (result > 1) break;
            }
            if ((result == 0) && (efCreateWithoutContent == false)) {
                return false;
            }
            
            // Datei zum Schreiben öffnen
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mPfad + efFilename), StandardCharsets.UTF_8), true );
            } catch (IOException ex) {
                Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (pw != null) {
                rs.first();
                
                // CSV-Header schreiben
                ResultSetMetaData rsmd = rs.getMetaData();
                Integer cc = rsmd.getColumnCount();
                line = "";
                for (int i=1; i<=xmlColumnCount; i++) {
                    if (i == 1) {
                        line = xmlColumn[i];
                    } else {
                        line = line.concat(CSV_COLUMNDELIMITER).concat(xmlColumn[i]);
                    }
                }
                pw.print(line + CSV_LINEDELIMITER);

                if (result > 0) {
                    // Records
                    do {
                        line = "";
                        for (int i=1; i<=cc; i++) {
                            value = rs.getString(i);
                            value = (value == null) ? "" : value;
                            if (i == 1) {
                                line = value;
                            } else {
                                line = line.concat(CSV_COLUMNDELIMITER).concat(value);
                            }
                        }
                        pw.print(line + CSV_LINEDELIMITER);
                    } while (rs.next());
                }
                
                // CSV-Datei schliessen
                pw.flush();
                pw.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
            joinMessage("SQL: "+ex.getMessage());
        }
        return true;
    }
    
    private void openIndexFile() {
        // Datei zum Schreiben öffnen
        pwIndex = null;
        try {
            pwIndex = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mPfad + "index.xml"), StandardCharsets.UTF_8), true );
        } catch (IOException ex) {
            Logger.getLogger(ExportParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeIndexFile(String value) {
        if (pwIndex != null) {
            pwIndex.print(value);
        }
    }
    
    private void writeIndexFilePuffer() {
        if (pwIndex != null) {
            pwIndex.print(indexPuffer);
        }
    }
    
    private void pufferIndexFile(String value) {
        if (pwIndex != null) {
            indexPuffer = indexPuffer.concat(value);
        }
    }
    
    private void closeIndexFile() {
        if (pwIndex != null) {
            pwIndex.flush();
            pwIndex.close();
        }
    }
    
    private void incXmlPath(String element) {
        xPfadPos++;
        xPfad[xPfadPos] = element;
        xmlPath = getXmlPath();
    }
    
    private void decXmlPath() {
        xPfadPos--;
        xmlPath = getXmlPath();
    }
    
    private String getXmlPath() {
        String path = "";
        for (int i=0; i <= xPfadPos; i++) {
            if (!path.equals("")) path = path.concat(".");
            path = path.concat(xPfad[i]);
        }
        return path;
    }
    
    private String getXmlPathSpaces(int x) {
        String ret = "";
        for (int i=1; i<=x; i++) {
            ret = ret.concat("    ");
        }
        return ret;
    }
        
    @Override
    public void startDocument() throws SAXException {
        // inicalizo las variables pertinentes
        inputKeyName = new String[10];
        inputKeyType = new String[10];
        inputKeyCount = 0;
        xPfadPos = -1;
        
        // Erstelle index.xml
        openIndexFile();
        writeIndexFile("<?xml version=\"1.0\" encoding=\"utf-8\"?>"+XML_LINEDELIMITER);
    }

    @Override
    public void endDocument() throws SAXException {
        writeIndexFile(getXmlPathSpaces(1)+"</Media>"+XML_LINEDELIMITER);
        writeIndexFile("</DataSet>"+XML_LINEDELIMITER);
        closeIndexFile();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        incXmlPath(qName);
        switch (xmlPath.toLowerCase()) {
            case "export.inputkeys.inputkey":
                if (inputKeyCount < inputKeyName.length-1 ) {
                    inputKeyCount++;
                }
                break;
            case "export.inputkeys.inputkey.name":
            case "export.inputkeys.inputkey.type":
            case "export.exportfiles.exportfile.format":
            case "export.exportfiles.exportfile.createfilewithoutcontent":
            case "export.exportfiles.exportfile.sql":
            case "export.exportschema.file":
            case "export.exportschema.ressource":
                text = new StringBuilder();
                break;
            case "export.exportfiles.exportfile":
                efFormat = "";
                efFilename = "";
                efCreateWithoutContent = false;
                efSql = "";
                break;
            // index.xml
            case "export.index_xml_header.doctype":
                text = new StringBuilder();
                break;
            case "export.index_xml_header.dataset":
                writeIndexFile("<DataSet>"+XML_LINEDELIMITER);
                break;
            case "export.index_xml_header.dataset.version":
                text = new StringBuilder();
                writeIndexFile(getXmlPathSpaces(1)+"<Version>");
                break;
            case "export.index_xml_header.dataset.datasupplier":
            case "export.index_xml_header.dataset.media":
                writeIndexFile(getXmlPathSpaces(1)+"<"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.index_xml_header.dataset.datasupplier.name":
            case "export.index_xml_header.dataset.datasupplier.location":
            case "export.index_xml_header.dataset.datasupplier.comment":
            case "export.index_xml_header.dataset.media.name":
                text = new StringBuilder();
                writeIndexFile(getXmlPathSpaces(2)+"<"+qName+">");
                break;
            case "export.exportfiles.exportfile.table":
                indexPuffer = "";
                pufferIndexFile(getXmlPathSpaces(2)+"<Table>"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.range":
            case "export.exportfiles.exportfile.table.variablelength":
                pufferIndexFile(getXmlPathSpaces(3)+"<"+qName+">"+XML_LINEDELIMITER);
                xmlColumn = new String[99];
                xmlColumnCount = 0;
                break;
            case "export.exportfiles.exportfile.table.url":
            case "export.exportfiles.exportfile.table.name":
            case "export.exportfiles.exportfile.table.description":
            case "export.exportfiles.exportfile.table.decimalsymbol":
            case "export.exportfiles.exportfile.table.digitgroupingsymbol":
                text = new StringBuilder();
                pufferIndexFile(getXmlPathSpaces(3)+"<"+qName+">");
                break;
            case "export.exportfiles.exportfile.table.range.from":
            case "export.exportfiles.exportfile.table.variablelength.columndelimiter":
            case "export.exportfiles.exportfile.table.variablelength.recorddelimiter":
            case "export.exportfiles.exportfile.table.variablelength.textencapsulator":
                text = new StringBuilder();
                pufferIndexFile(getXmlPathSpaces(4)+"<"+qName+">");
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn":
                pufferIndexFile(getXmlPathSpaces(4)+"<"+qName+">");
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.name":
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.description":
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.maxlength":
                text = new StringBuilder();
                pufferIndexFile("<"+qName+">");
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (xmlPath.toLowerCase()) {
            case "export.inputkeys.inputkey.name":
                inputKeyName[inputKeyCount-1] = text.toString();
                break;
            case "export.inputkeys.inputkey.type":
                inputKeyType[inputKeyCount-1] = text.toString();
                break;
            case "export.exportfiles.exportfile.format":
                efFormat = text.toString().toLowerCase();
                break;
            case "export.exportfiles.exportfile.createfilewithoutcontent":
                efCreateWithoutContent = text.toString().toLowerCase().equals("true");
                break;
            case "export.exportfiles.exportfile.sql":
                efSql = text.toString();
                break;
            case "export.exportfiles.exportfile":
                if (doExportFile()) {
                    writeIndexFilePuffer();
                }
                break;
            // index.xml
            case "export.index_xml_header.doctype":
                writeIndexFile("<!DOCTYPE "+text.toString()+">"+XML_LINEDELIMITER);
                break;
            case "export.index_xml_header.dataset.version":
            case "export.index_xml_header.dataset.datasupplier.name":
            case "export.index_xml_header.dataset.datasupplier.location":
            case "export.index_xml_header.dataset.datasupplier.comment":
            case "export.index_xml_header.dataset.media.name":
                writeIndexFile(text.toString()+"</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.name":
            case "export.exportfiles.exportfile.table.description":
            case "export.exportfiles.exportfile.table.decimalsymbol":
            case "export.exportfiles.exportfile.table.digitgroupingsymbol":
            case "export.exportfiles.exportfile.table.range.from":
            case "export.exportfiles.exportfile.table.variablelength.columndelimiter":
            case "export.exportfiles.exportfile.table.variablelength.recorddelimiter":
            case "export.exportfiles.exportfile.table.variablelength.textencapsulator":
                pufferIndexFile(text.toString()+"</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.index_xml_header.dataset.datasupplier":
                writeIndexFile(getXmlPathSpaces(1)+"</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table":
                pufferIndexFile(getXmlPathSpaces(2)+"</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.url":
                efFilename = text.toString();
                pufferIndexFile(text.toString()+"</URL>"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.utf8":
                pufferIndexFile(getXmlPathSpaces(3)+"<UTF8/>"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.alphanumeric":
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.numeric":
                pufferIndexFile("<"+qName+"/>");
                break;
            case "export.exportfiles.exportfile.table.range":
            case "export.exportfiles.exportfile.table.variablelength":
                pufferIndexFile(getXmlPathSpaces(3)+"</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn":
                pufferIndexFile("</"+qName+">"+XML_LINEDELIMITER);
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.name":
                pufferIndexFile(text.toString()+"</"+qName+">");
                xmlColumnCount++;
                xmlColumn[xmlColumnCount] = text.toString();
                break;
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.description":
            case "export.exportfiles.exportfile.table.variablelength.variablecolumn.maxlength":
                pufferIndexFile(text.toString()+"</"+qName+">");
                break;
            case "export.exportschema.file":
                schemaFile = text.toString();
                break;
            case "export.exportschema.ressource":
                schemaRessource = text.toString();
                break;
            case "export.exportschema":
                doExportSchema(schemaRessource, schemaFile);
                break;
        }
        decXmlPath();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (text != null) {
            text.append(ch, start, length);
        }
    }
    
}
