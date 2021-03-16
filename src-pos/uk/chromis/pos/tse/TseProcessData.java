/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.chromis.pos.tse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Medicus
 */
public class TseProcessData {
    
    public static String vorgangsTyp;
    public static Double bruttoAllg;
    public static Double bruttoErm;
    public static Double bruttoDurchschn3;
    public static Double bruttoDurchschn1;
    public static Double bruttoNull;
    public static Double zahlungBarEUR;
    public static Double zahlungUnbarEUR;

    public static void clear() {
        vorgangsTyp = "";
        bruttoAllg = 0.0;
        bruttoErm = 0.0;
        bruttoDurchschn3 = 0.0;
        bruttoDurchschn1 = 0.0;
        bruttoNull = 0.0;
        zahlungBarEUR = 0.0;
        zahlungUnbarEUR = 0.0;
    }
    
    public static String getProcessData() {
        
        String pData;
        
        pData = vorgangsTyp + "^";
        pData = pData + numberFormat(bruttoAllg) + "_";
        pData = pData + numberFormat(bruttoErm) + "_";
        pData = pData + numberFormat(bruttoDurchschn3) + "_";
        pData = pData + numberFormat(bruttoDurchschn1) + "_";
        pData = pData + numberFormat(bruttoNull) + "^";
        
        if (zahlungBarEUR != 0) {
            pData = pData + numberFormat(zahlungBarEUR) + ":Bar";
        }
        
        if (zahlungUnbarEUR != 0) {
            if (zahlungBarEUR != 0) pData = pData + "_";
            pData = pData + numberFormat(zahlungUnbarEUR) + ":Unbar";
        }
        
        if ((zahlungBarEUR == 0) && (zahlungUnbarEUR == 0)) {
            pData = pData + "0:Bar";
        }
        
        return pData;
        
    }
    
    public static String numberFormat(Double num) {
        
        // Dezimalseparator einstellen
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setDecimalSeparator('.');
        // Anzahl Nachkommastellen                
        DecimalFormat df = new DecimalFormat("0.00");
        df.setDecimalFormatSymbols(s);
        String s3 = df.format(num);        
        
        return s3;
    }
    
    public static boolean checkSum() {
        Double s1 = bruttoAllg + bruttoErm + bruttoDurchschn3 + bruttoDurchschn1 + bruttoNull - zahlungBarEUR - zahlungUnbarEUR;
        Double s2 = Math.abs(bruttoAllg) + Math.abs(bruttoErm) + Math.abs(bruttoDurchschn3) + Math.abs(bruttoDurchschn1) + Math.abs(bruttoNull);
        return (s1 == 0) && (s2 > 0);
    }
    
    public static String dateFormat(Long num) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        return sdf.format(num);
    }
    
}
