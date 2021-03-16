/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.chromis.pos.tse;

import java.util.Arrays;

/**
 *
 * @author Medicus
 */
public class asn1obj {
    
    public static byte[] getTerminal(byte[] bIn, int nr) {
        
        int count = 1;
        int b4 = 0;
        int pos = 3;
        byte[] terminal = "".getBytes();
        
        while (nr >= count) {
            
            if (pos < bIn.length) {
                b4 = bIn[pos];
                if (nr == count) {
                    terminal = Arrays.copyOfRange(bIn, pos+1, pos+1+b4);
                }
            }
            count++;
            pos = pos + b4 + 2;
        }
        return terminal;
    }
    
    public static String getTerminalID(byte[] bIn, int nr) {
        
        String s2 = "";
        byte[] t = getTerminal(bIn, nr);
        if (t.length > 2) {
            int b2 = t[1];
            byte[] t2 = Arrays.copyOfRange(t, 2, 2 + b2);
            for (int i=0; i< t2.length; i++) {
                s2 = s2 + Character.toString((char) t2[i]);
            }
        }
        return s2;
    }
    
    
}

