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

package uk.chromis.pos.tse;

import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.TSE.FinishTransactionResult;
import com.cryptovision.SEAPI.TSE.StartTransactionResult;
import com.cryptovision.SEAPI.TSE.UnblockUserResult;
import com.cryptovision.SEAPI.TSE.UpdateTransactionResult;
import com.cryptovision.SEAPI.exceptions.SEException;
import com.cryptovision.SEAPI.exceptions.ErrorSeApiDeactivated;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;

public class TseInfoCryptovision extends TseInfo {

    private TSE m_TSE;
    
    private Properties props;
    
    private String m_adminPin = "";
    private String m_timeadminPin = "";
    private String m_timeFormat = "unixTime";
    private Long m_tseLastTimeSet = 0L;
    private Integer m_tseTimeSyncInterval = 0;
    private Boolean m_active = true;
    
    public TseInfoCryptovision(String sPath) throws SEException {
        if (sPath == null) sPath = "";
        props = new Properties();

        // transport=MSCJava10
        // transport=MscJna
        props.setProperty("transport", "MscJna");
        // path=F:/
        // path=/media/user/cv TSE
        props.setProperty("path", sPath);
        // TSE communication timeout, seconds (default: 5)
        props.setProperty("timeout", "5");
        // delay after direct IO operations, milliseconds (default: 0)
        props.setProperty("delay", "0");
        // size of direct IO read/write operations. 512 on most Windows platforms, 2048 on some Linux. Smaller is faster, increase on Exception. (default: 512)
        props.setProperty("alignment", "1024");
        
        try {
            m_TSE = TSE.getInstance(props);
        } catch (IOException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        TseSerialNumber = readSerialNumber();
    }
    
    public void setAdminPin(String pin) {
        m_adminPin = pin;
    }
    
    public void setTimeadminPin(String pin) {
        m_timeadminPin = pin;
    }
    
    public boolean getActiveState() {
        return m_active;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String getName() {
        return "cryptovision";
    }

    /**
     *
     * @return
     */
    @Override
    public String getConfigCheck() {
        String result = "";
        try {
            boolean[] states = m_TSE.getPinStatus();
            if(!states[0]) {
                result = "TSE already in use!";
            } else {
                result = "TSE not in use!";
            }
            result = result.concat(System.lineSeparator());
            result = "Firmware: " + m_TSE.getFirmwareId();
            result = result.concat(System.lineSeparator());
            result = result.concat("Certification ID: "+m_TSE.getCertificationId());
            result = result.concat(System.lineSeparator());
            result = result.concat("TSE-ID: "+ TseInfo.hexToString(m_TSE.getUniqueId()));
            result = result.concat(System.lineSeparator());
            result = result.concat("Life Cycle State: "+m_TSE.getLifeCycleState().toString());
        } catch (SEException | IOException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     *
     */
    @Override
    public void close() {
        try {
            m_TSE.close();
        } catch (IOException | SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     *
     * @return
     */
    @Override
    public boolean TseInUse() {
        boolean result = false;
        if (m_TSE == null) {
            return false;
        }
        try {
            boolean[] states = m_TSE.getPinStatus();
            if(!states[0]) {
                result = true;
            }
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    /**
     *
     */
    @Override
    public void initializeTSE(byte[] adminPin, byte[] adminPuk, byte[] timeAdminPin, byte[] timeAdminPuk) {
        try {
            m_TSE.initializePinValues(adminPin, adminPuk, timeAdminPin, timeAdminPuk);
            m_TSE.authenticateUser("Admin", adminPin);
            m_TSE.initialize();
            OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);
            m_TSE.updateTime(utc.toEpochSecond());
            m_TSE.logOut("Admin");
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String readSerialNumber() {
        String sn = "";
        try {
            byte[] serNum = m_TSE.exportSerialNumbers();
            sn = Hex.encodeHexString( serNum ).substring(12, 76) ;
        } catch (ErrorSeApiDeactivated ex) {
            // com.cryptovision.SEAPI.exceptions.ErrorSeApiDeactivated
            m_active = false;
            sn = "";
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sn;
    }
    
    private byte[] intSerialNumber() {
        byte[] serial = "".getBytes();
        try {
            byte[] data = m_TSE.exportSerialNumbers();
            serial = Arrays.copyOfRange(data, 6, 6+32);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return serial;
    }
    
    @Override
    public String getSerialNumber() {
        if (TseSerialNumber.equals("")) {
            TseSerialNumber = readSerialNumber();
        }
        return TseSerialNumber;
    }
    
    @Override
    public String mapERStoKey(String kassenID) {
        String msg = "";
        try {
            m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
            m_TSE.updateTime(System.currentTimeMillis()/1000);
            m_TSE.mapERStoKey(kassenID, intSerialNumber());
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            msg = "Kasse mit TSE verbinden ist gescheitert";
        } finally {
            try {            
                m_TSE.logOut("Admin");
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return msg;
    }
    
    @Override
    public String unmapERS(String kassenID) {
        String msg = "";
        try {
            m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
            m_TSE.mapERStoKey(kassenID, null);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            msg = "Kasse von TSE trennen ist gescheitert";
        } finally {
            try {            
                m_TSE.logOut("Admin");
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return msg;
    }
    
    @Override
    public byte[] getERSMappings() {
        byte[] ba = "".getBytes();
        
        try {
            ba = m_TSE.getERSMappings();
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ba;
    }
    
    public String checkAccounts() {
        String res = "";
        try {
            m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            res = "Admin-PIN ist nicht korrekt. Bitte prÃ¼fen!";
        } finally {
            try {
                m_TSE.logOut("Admin");
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            m_TSE.authenticateUser("TimeAdmin", m_timeadminPin.getBytes());
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            if (!res.equals("")) res = res + (char)13 + (char)10;
            res = res.concat("TimeAdmin-PIN ist nicht korrekt. Bitte prÃ¼fen!");
        } finally {
            try {
                m_TSE.logOut("TimeAdmin");
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return res;
    }
    
    public String setTime() {
        String ret = "";
        if (m_tseTimeSyncInterval == 0) {
            try {
                m_tseTimeSyncInterval = m_TSE.getTimeSyncInterval();
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
                ret = ex.getMessage();
            }
        }
        // nur wenn mehr als [m_tseTimeSyncInterval] Sekunden seit dem letzten mal vergangen sind
        if (m_tseLastTimeSet < ((System.currentTimeMillis()/1000) - m_tseTimeSyncInterval) ) { 
            try {
                m_TSE.authenticateUser("TimeAdmin", m_timeadminPin.getBytes());
                m_TSE.updateTime(System.currentTimeMillis()/1000);
            } catch (SEException | NullPointerException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
                ret = ex.toString();
            }
            try {
                m_TSE.logOut("TimeAdmin");
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
                ret = "Fehler: TSE-TimeAdmin ist nicht angemeldet!";
            }
            m_tseLastTimeSet = System.currentTimeMillis()/1000;
        }
        return ret;
    }
    
    private StartTransactionResult intStartTransaction(String kassenID, String processData, String processType, String additionalData) {
        StartTransactionResult res = null;
        setTime();
        try {
            res = m_TSE.startTransaction(kassenID, processData.getBytes(), processType, additionalData.getBytes());
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    @Override
    public StartTransactionResult startTransaction(String kassenID, String processData, String processType, String additionalData) {
        
        StartTransactionResult str = intStartTransaction(kassenID, processData, processType, additionalData);
        if (str == null) {
            // versuchÂ´s noch einmal, Sam
            // gemÃ¤ÃŸ "BSI TR-03153 - Kapitel 7.1 Fehlerbehandlung" muss das Aufzeichnungssystem jeden Schritt wiederholen, wenn dieser fehlschlÃ¤gt
            str = intStartTransaction(kassenID, processData, processType, additionalData);
        }
        return str;
    }

    private UpdateTransactionResult intUpdateTransaction(String kassenID, long transactionNumber, String processData, String processType) {
        UpdateTransactionResult res = null;
        setTime();
        try {
            res = m_TSE.updateTransaction(kassenID, transactionNumber, processData.getBytes(), processType);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    @Override
    public UpdateTransactionResult updateTransaction(String kassenID, long transactionNumber, String processData, String processType) {
        
        UpdateTransactionResult utr = intUpdateTransaction(kassenID, transactionNumber, processData, processType);
        if (utr == null) {
            // versuchÂ´s noch einmal, Sam
            // gemÃ¤ÃŸ "BSI TR-03153 - Kapitel 7.1 Fehlerbehandlung" muss das Aufzeichnungssystem jeden Schritt wiederholen, wenn dieser fehlschlÃ¤gt
            utr = intUpdateTransaction(kassenID, transactionNumber, processData, processType);
        }
        
        return utr;
    }
    
    private FinishTransactionResult intFinishTransaction(String kassenID, long transactionNumber, String processData, String processType, String additionalData) {
        FinishTransactionResult res = null;
        setTime();
        try {
            res = m_TSE.finishTransaction(kassenID, transactionNumber, processData.getBytes(), processType, additionalData.getBytes());
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    @Override
    public FinishTransactionResult finishTransaction(String kassenID, long transactionNumber, String processData, String processType, String additionalData) {
        FinishTransactionResult ftr = intFinishTransaction(kassenID, transactionNumber, processData, processType, additionalData);
        if (ftr == null) {
            // versuchÂ´s noch einmal, Sam
            // gemÃ¤ÃŸ "BSI TR-03153 - Kapitel 7.1 Fehlerbehandlung" muss das Aufzeichnungssystem jeden Schritt wiederholen, wenn dieser fehlschlÃ¤gt
            ftr = intFinishTransaction(kassenID, transactionNumber, processData, processType, additionalData);
        }
        return ftr;
    }
    
    
    @Override
    public String getSignaturAlgorithmus() {
        byte[] ret;
        String res = "";
        try {
            ret = m_TSE.getSignatureAlgorithm();
            res = TseInfo.asn1ToString(ret);
        } catch (SEException | IOException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (res.contains("0.4.0.127.0.7.1.1.4.1.3")) {
            return "ecdsa-plain-SHA256";
        } else if (res.contains("0.4.0.127.0.7.1.1.4.1.4")) {
            return "ecdsa-plain-SHA384";
        } else if (res.contains("0.4.0.127.0.7.1.1.4.1.5")) {
            return "ecdsa-plain-SHA512";
        } else {
            return "";
        }
    }
    
    @Override
    public String getPublicKey() {
        byte[] ret = "".getBytes();
        
        try {
            byte[] data = m_TSE.exportSerialNumbers();
            byte[] serial = Arrays.copyOfRange(data, 6, 6+32);
            ret = m_TSE.exportPublicKey(serial);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Hex.encodeHexString(ret);
    }
    
    @Override
    public String getCertificationId() {
        String ret = "";
        
        try {
            ret = m_TSE.getCertificationId();
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    @Override
    public String getProcesstypeKassenbeleg() {
        return PROCESSTYPE_KASSENBELEG;
    }
    
    @Override
    public String getProcesstypeBestellung() {
        return PROCESSTYPE_BESTELLUNG;
    }
    
    @Override
    public String getTimeFormat() {
        return m_timeFormat;
    }
    
    @Override
    public long[] getOpenTransactions() {
        try {
            return m_TSE.getOpenTransactions();
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    @Override
    public String unblockUser(String user, byte[] puk, byte[] pin) {
        UnblockUserResult uur = null;
        try {
            uur = m_TSE.unblockUser(user, puk, pin);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (uur != null) {
            if (null == uur.authenticationResult) {
                return "Unbekannter Fehler";
            } else switch (uur.authenticationResult) {
                case ok:
                    return "User ist entsperrt";
                case unknownUserId:
                    return "Unbekannter User";
                case error:
                    return "Fehler";
                case failed:
                    return "Entsperren gescheitert";
                case pinIsBlocked:
                    return "PIN ist gesperrt";
                default:
                    return "Unbekannter Fehler";
            }
        } else {
            return "Unbekannter Fehler";
        }
    }
    
    @Override
    public void exportData(String kassenID, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, String pfad) {
        try {
            //        m_TSE.exportData(kassenID, transactionNumber, startTransactionNumber, endTransactionNumber, startDate, endDate, maximumNumberRecords, fileName);
            m_TSE.exportData(kassenID, null, startTransactionNumber, endTransactionNumber, startDate, endDate, null, pfad);
        } catch (SEException | IOException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
//    public void exportMoreData() {
//        m_TSE.exportMoreData(serialNumberKey, previousSignatureCounter, maximumNumberRecords, stream);
//    }
    
    @Override
    public Boolean deleteStoredDataUpTo(Long signatureCounter) {
        Boolean res = false;
        try {
            m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
            m_TSE.deleteStoredDataUpTo(intSerialNumber() , signatureCounter);
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                m_TSE.logOut("Admin");
                res = true;
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return res;
    }
    
    @Override
    public boolean deactivateTse() {
        Boolean res = false;
        if (setTime().equals("")) {
            try {
                m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
                m_TSE.deactivateTSE();
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    m_TSE.logOut("Admin");
                    res = true;
                } catch (SEException ex) {
                    Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
                }
                m_active = false;
            }
        }
        return res;
    }

    @Override
    public boolean activateTse() {
        boolean res = false;
        try {
            m_TSE.authenticateUser("Admin", m_adminPin.getBytes());
            m_TSE.activateTSE();
        } catch (SEException ex) {
            Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                m_TSE.logOut("Admin");
                res = true;
            } catch (SEException ex) {
                Logger.getLogger(TseInfoCryptovision.class.getName()).log(Level.SEVERE, null, ex);
            }
            m_active = true;
        }
        return res;
    }
}
