/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.chromis.pos.tse;

import com.cryptovision.SEAPI.exceptions.SEException;
import com.cryptovision.SEAPI.exceptions.ErrorNoKey;
import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.TSE.LCS;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author Medicus
 */
public class TseFake {
    
    private static TseFake instance = null;
    private final File configFile;
    private final Properties m_propsconfig;
    private static String path;
    
    public TseFake(File configFile) {
        this.configFile = configFile;
        m_propsconfig = new Properties();
        load();
    }
    
    public static TseFake getInstance(Properties prop) throws IOException {
        path = prop.getProperty("path");
        if (instance == null) {
           instance = new TseFake(new File(prop.getProperty("path"), "TseTraining.properties"));
        }
        return instance;
    }
    
    public static String hexToString(byte[] data) throws IOException {
        return Hex.toHexString(data);
    }    
    
    private byte[] getRandomHex(int length) {
        byte[] hex = new byte[length];
        for (int i=0; i<length; i++) {
            hex[i] = (byte) ((Math.random())*256);
        }
        return hex;
    }

    class SortedProperties extends Properties {

        public Enumeration keys() {
            Enumeration keysEnum = super.keys();
            Vector<String> keyList = new Vector<String>();
            while (keysEnum.hasMoreElements()) {
                keyList.add((String) keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return keyList.elements();
        }
    }
    
    public void load() {
        try {
            InputStream in = new FileInputStream(configFile);
            if (in != null) {
                m_propsconfig.load(in);
                in.close();
            }
        } catch (IOException e) {
            loadDefault();
        }
    }

    private void loadDefault() {
        m_propsconfig.setProperty("pin.admin", "");
        m_propsconfig.setProperty("pin.timeadmin", "");
        m_propsconfig.setProperty("puk.admin", "");
        m_propsconfig.setProperty("puk.timeadmin", "");
        m_propsconfig.setProperty("login.admin", "0");
        m_propsconfig.setProperty("login.timeadmin", "0");
        m_propsconfig.setProperty("uniqueid", UUID.randomUUID().toString());
        m_propsconfig.setProperty("certificationid", UUID.randomUUID().toString());
        m_propsconfig.setProperty("serialnumber", Hex.toHexString(getRandomHex(32)));
        m_propsconfig.setProperty("publickey", Hex.toHexString(getRandomHex(64)));
        m_propsconfig.setProperty("signaturecounter", "0");
        Date m_dDate = new Date();
        long l = m_dDate.getTime() + 5*356*24*60*60*1000; // 5 Jahre
        m_propsconfig.setProperty("lifecycle", Long.toString(l));
        m_propsconfig.setProperty("lifecyclestate", LCS.notInitialized.toString());
        try {
            save();
        } catch (IOException ex) {
            Logger.getLogger(TseFake.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void save() throws FileNotFoundException, IOException {
        Set set = m_propsconfig.entrySet();
        SortedProperties sortedpropsconfig = new SortedProperties();
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            sortedpropsconfig.put(me.getKey().toString(), me.getValue().toString());
        }
        OutputStream out = new FileOutputStream(configFile);
        if (out != null) {
            sortedpropsconfig.store(out, "TseTraining Configuration file.");
            out.close();
        }
    }
    
    public void TseLog(String value) {
        Date zeitstempel = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        value = simpleDateFormat.format(zeitstempel).concat(": ").concat(value);
        
        FileWriter fWriter = null;
        try {
            fWriter = new FileWriter(path + "/TseTraining.log", /* append */ true);
            fWriter.append(value+"\n");
        } catch (IOException ex) {
            Logger.getLogger(TseFake.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (fWriter != null) {
                try {
                    fWriter.flush();
                    fWriter.close();
                } catch (IOException ex) {
                    Logger.getLogger(TseFake.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public void setProperty(String sKey, String sValue) {
        if (sValue == null) {
            m_propsconfig.remove(sKey);
        } else {
            m_propsconfig.setProperty(sKey, sValue);
        }
        try {
            save();
        } catch (IOException ex) {
            Logger.getLogger(TseFake.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean[] getPinStatus() throws IOException {
        boolean[] b = new boolean[2];
        b[0] = m_propsconfig.getProperty("pin.admin").equals("");
        b[1] = m_propsconfig.getProperty("pin.timeadmin").equals("");
        return b;
    }
    
    public String getFirmwareId() {
        return "fake0815";
    }
    
    public String getCertificationId() throws SEException {
        return m_propsconfig.getProperty("certificationid");
    }
    
    public byte[] getUniqueId() {
        return m_propsconfig.getProperty("uniqueid").getBytes();
    }
    
    public LCS getLifeCycleState() {
        if (LCS.notInitialized.toString().equals( m_propsconfig.getProperty("lifecyclestate"))) {
            return LCS.notInitialized;
        } else if (LCS.active.toString().equals( m_propsconfig.getProperty("lifecyclestate"))) {
            return LCS.active;
        } else if (LCS.deactivated.toString().equals( m_propsconfig.getProperty("lifecyclestate"))) {
            return LCS.deactivated;
        } else if (LCS.disabled.toString().equals( m_propsconfig.getProperty("lifecyclestate"))) {
            return LCS.disabled;
        } else if (LCS.noTime.toString().equals( m_propsconfig.getProperty("lifecyclestate"))) {
            return LCS.noTime;
        } else {
            return LCS.unknown;
        }
    }

    public void close() throws IOException {
        this.save();
    }
    
    public void initializePinValues(byte[] adminPin, byte[] adminPuk, byte[] timeAdminPin, byte[] timeAdminPuk) {
        setProperty("pin.admin", new String(adminPin));
        setProperty("pin.timeadmin", new String(timeAdminPin));
        setProperty("puk.admin", new String(adminPuk));
        setProperty("puk.timeadmin", new String(timeAdminPuk));
    }
    
    public void authenticateUser(String user, byte[] pin) throws SEException {
        if ((user.toLowerCase().equals("admin")) || (user.toLowerCase().equals("timeadmin"))) {
            if (m_propsconfig.getProperty("pin."+user.toLowerCase()).equals(new String(pin))) {
                this.getSignatureCounter("authenticateUser="+user.toLowerCase());
                setProperty("login."+user.toLowerCase(), "1");
            } else {
                throw new ErrorNoKey("User login failed!");
            }
        }
    }
    
    public class UnblockUserResult {
            public TSE.AuthenticationResult authenticationResult;	/**< result of the authentication */
    };
    
    public UnblockUserResult unblockUser(String user, byte[] puk, byte[] pin) throws SEException {
        UnblockUserResult uur = new UnblockUserResult();
        if ((user.toLowerCase().equals("admin")) || (user.toLowerCase().equals("timeadmin"))) {
            if (m_propsconfig.getProperty("puk."+user.toLowerCase()).equals(puk.toString())) {
                uur.authenticationResult = TSE.AuthenticationResult.ok;
                setProperty("pin."+user.toLowerCase(),new String(pin));
                this.getSignatureCounter("unblockUser="+user.toLowerCase());
            } else {
                uur.authenticationResult = TSE.AuthenticationResult.failed;
            }
        } else {
            uur.authenticationResult = TSE.AuthenticationResult.unknownUserId;
        }
        return uur;
    }
    
    public void initialize() throws SEException {
        if (m_propsconfig.getProperty("lifecyclestate").equals(LCS.notInitialized.toString())) {
            if (m_propsconfig.getProperty("login.admin","0").equals("1")) {
                setProperty("lifecyclestate", LCS.active.toString());
                this.getSignatureCounter("initialize");
            } else {
                throw new ErrorNoKey("Admin not logged in");
            }
        } else {
            throw new ErrorNoKey("TSE not in state 'uninitialized'");
        }
    }
    
    public void updateTime(long dummy) {  }
    
    public void logOut(String user) {
        if ((user.toLowerCase().equals("admin")) || (user.toLowerCase().equals("timeadmin"))) {
            setProperty("login."+user.toLowerCase(), "0");
            TseLog("logOut="+user.toLowerCase());
        }
    }
    
    public byte[] exportSerialNumbers() {
        //return Hex.decode(m_propsconfig.getProperty("publickey"));
        return m_propsconfig.getProperty("serialnumber").getBytes();
    }
    
    public String getPublicKey() {
        return m_propsconfig.getProperty("publickey");
    }
    
    public void mapERStoKey(String clientID, byte[] serialNumberKey) throws SEException {
        if (m_propsconfig.getProperty("lifecyclestate").equals(LCS.active.toString())) {
            if (m_propsconfig.getProperty("login.admin","0").equals("1")) {
                if (serialNumberKey == null) {
                    setProperty("till.serialnumber", "");
                    TseLog("mapERStoKey=[null]");
                } else {
                    setProperty("till.serialnumber", clientID);
                    TseLog("mapERStoKey="+clientID);
                }
            } else {
                throw new ErrorNoKey("Admin not logged in!");
            }
        } else {
            throw new ErrorNoKey("TSE not initialized!");
        }
    }

    private byte[] joinByteArray(byte[] a, byte[] b) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write( a );
            outputStream.write( b );
            return outputStream.toByteArray( );
        } catch (IOException ex) {
            Logger.getLogger(TseFake.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private byte[] asn1Object(String value) {
        byte[] t;
        byte[] c;
        c = new byte[6];
        c[0] = 30;
        c[1] = (byte) (value.length() + 4);
        c[2] = 30;
        c[3] = (byte) (value.length() + 2);
        c[4] = 4;
        c[5] = (byte) value.length();
        t = joinByteArray(c,value.getBytes());
        return t;
    }
            
    public byte[] getERSMappings() {
        return asn1Object(m_propsconfig.getProperty("till.serialnumber", ""));
    }
    
    public int getTimeSyncInterval() {
        return 1800;
    }
    
    public byte[] getSignatureAlgorithm() throws SEException {
        return asn1Object("0.4.0.127.0.7.1.1.4.1.3");
    }
    
    public byte[] exportPublicKey(byte[] serial) throws SEException {
        return m_propsconfig.getProperty("publickey").getBytes();
    }

    public void deactivateTSE() throws SEException {
        if (m_propsconfig.getProperty("login.admin","0").equals("1")) {
            if (m_propsconfig.getProperty("state","").equals("active")) {
                setProperty("lifecyclestate", LCS.deactivated.toString());
                TseLog("deactivateTSE");
            } else {
                throw new ErrorNoKey("TSE is not in state 'active'");
            }
        } else {
            throw new ErrorNoKey("Admin not logged in");
        }
    }
    
    public void activateTSE() throws SEException {
        if (m_propsconfig.getProperty("login.admin","0").equals("1")) {
            if (m_propsconfig.getProperty("state","").equals("deactivated")) {
                setProperty("lifecyclestate", LCS.active.toString());
                TseLog("activateTSE");
            } else {
                throw new ErrorNoKey("TSE is not in state 'deactivated'");
            }
        } else {
            throw new ErrorNoKey("Admin not logged in");
        }
    }
    
    private Long getSignatureCounter(String reason) {
        long sc = Long.valueOf(m_propsconfig.getProperty("signaturecounter","0"));
        sc++;
        setProperty("signaturecounter", Long.toString(sc));
        TseLog("signatureCounter ("+sc+"): "+reason);
        return sc;
    }
    
    private Long getTransactionNumber() {
        long sc = Long.valueOf(m_propsconfig.getProperty("transactionnumber","0"));
        sc++;
        setProperty("transactionnumber", Long.toString(sc));
        return sc;
    }
        
    public TSE.StartTransactionResult startTransaction(String kassenID, byte[] processData, String processType, byte[] additionalData) throws SEException {
        TSE.StartTransactionResult str = new TSE.StartTransactionResult();
        if (m_propsconfig.getProperty("lifecyclestate","").equals("active")) {
            if (m_propsconfig.getProperty("till.serialnumber","").equals(kassenID)) {
                str.logTime = System.currentTimeMillis()/1000;
                str.serialNumber =  Hex.decode( m_propsconfig.getProperty("serialnumber"));
                str.signatureValue = getRandomHex(64);
                str.transactionNumber = getTransactionNumber();
                str.signatureCounter = getSignatureCounter("startTransaction="+str.transactionNumber);
            } else {
                throw new ErrorNoKey("TSE not matched with till!");
            }
        } else {
            throw new ErrorNoKey("TSE not initialized!");
        }
        return str;
    }
    
    public TSE.UpdateTransactionResult updateTransaction(String kassenID, Long transactionNumber, byte[] processData, String processType) throws SEException {
        TSE.UpdateTransactionResult utr = new TSE.UpdateTransactionResult();
        if (m_propsconfig.getProperty("lifecyclestate","").equals("active")) {
            if (m_propsconfig.getProperty("till.serialnumber","").equals(kassenID)) {
                utr.logTime = System.currentTimeMillis()/1000;
                utr.serialNumber = m_propsconfig.getProperty("serialnumber").getBytes();
                utr.signatureCounter = getSignatureCounter("updateTransaction="+transactionNumber);
                utr.signatureValue = getRandomHex(64);
            } else {
                throw new ErrorNoKey("TSE not matched with till!");
            }
        } else {
            throw new ErrorNoKey("TSE not initialized!");
        }
        return utr;
    }
    
    public TSE.FinishTransactionResult finishTransaction(String kassenID, Long transactionNumber, byte[] processData, String processType, byte[] additionalData) throws SEException {
        TSE.FinishTransactionResult ftr = new TSE.FinishTransactionResult();
        if (m_propsconfig.getProperty("lifecyclestate","").equals("active")) {
            if (m_propsconfig.getProperty("till.serialnumber","").equals(kassenID)) {
                ftr.logTime = System.currentTimeMillis()/1000;
                ftr.serialNumber = Hex.decode(m_propsconfig.getProperty("serialnumber"));
                ftr.signatureCounter = getSignatureCounter("finishTransaction="+transactionNumber);
                ftr.signatureValue = getRandomHex(64);
            } else {
                throw new ErrorNoKey("TSE not matched with till!");
            }
        } else {
            throw new ErrorNoKey("TSE not initialized!");
        }
        return ftr;
    }

    public long[] getOpenTransactions() throws SEException {
        long[] x = new long[0];
        return x;
    }
    
    public void exportData(String kassenID, Long dummy, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long dummy2, String pfad) throws SEException {
        // nüx
    }
    
    public void deleteStoredDataUpTo(byte[] intSerialNumber, Long signatureCounter) throws SEException {
        // nüx
    }
    
}
