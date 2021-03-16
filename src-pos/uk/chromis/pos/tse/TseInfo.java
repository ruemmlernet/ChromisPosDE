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
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.util.ASN1Dump;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;



public abstract class TseInfo {
    
    public String TseSerialNumber;
    
    protected static final String PROCESSTYPE_KASSENBELEG = "Kassenbeleg-V1";
    protected static final String PROCESSTYPE_BESTELLUNG  = "Bestellung-V1";

    
    public TseInfo() {
//        dlSync = (DataLogicSync) app.getBean("uk.chromis.pos.sync.DataLogicSync");
    }
    
    public abstract String getProcesstypeKassenbeleg();
    
    public abstract String getProcesstypeBestellung();
    
    public abstract String getTimeFormat();
    
    public static String hexToString(byte[] data) throws IOException {
        return Hex.toHexString(data);
    }    
    
    public static void asn1Dump(byte[] data, PrintStream out) throws IOException {
        ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(data));
        out.println(ASN1Dump.dumpAsString(bIn.readObject(), true));
        bIn.close();
    }
    
    public static String asn1ToString(byte[] data) throws IOException {
        ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(data));
        String ret = ASN1Dump.dumpAsString(bIn.readObject(), true);
        bIn.close();
        return ret;
    }
    
    public static void updateTse(byte[] adminPin, byte[] adminPuk, byte[] timeAdminPin, byte[] timeAdminPuk, byte[] sernum) {
//        # nach DataLogicAdmin ...
    }
    
    public abstract String getName();

    public abstract String getConfigCheck();

    public abstract void close();
    
    public abstract boolean TseInUse();
    
    public abstract void initializeTSE(byte[] adminPin, byte[] adminPuk, byte[] timeAdminPin, byte[] timeAdminPuk);
    
    public abstract String mapERStoKey(String KassenID);
    
    public abstract String getSerialNumber();
    
    public abstract String unmapERS(String kassenID);
        
    public abstract byte[] getERSMappings();
        
    public abstract TSE.StartTransactionResult startTransaction(String kassenID, String processData, String processType, String additionalData);
    
    public abstract TSE.UpdateTransactionResult updateTransaction(String kassenID, long transactionNumber, String processData, String processType);
    
    public abstract TSE.FinishTransactionResult finishTransaction(String kassenID, long transactionNumber, String processData, String processType, String additionalData);
    
    public abstract String getSignaturAlgorithmus();
    
    public abstract String getPublicKey();
    
    public abstract String getCertificationId();
    
    public abstract long[] getOpenTransactions();
    
    public abstract String unblockUser(String user, byte[] puk, byte[] pin);
    
    public abstract void exportData(String kassenID, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, String pfad);
    
    public abstract Boolean deleteStoredDataUpTo(Long signatureCounter);
    
    public abstract boolean deactivateTse();

    public abstract boolean activateTse();
    
    public abstract boolean getActiveState();
    
    public abstract void setAdminPin(String pin);
    
    public abstract void setTimeadminPin(String pin);
    
    public abstract String setTime();
}

