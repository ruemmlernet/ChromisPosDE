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
package uk.chromis.pos.ticket;

import com.cryptovision.SEAPI.TSE.FinishTransactionResult;
import com.cryptovision.SEAPI.TSE.StartTransactionResult;
import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.bouncycastle.util.encoders.Hex;
import uk.chromis.basic.BasicException;
import uk.chromis.data.loader.DataRead;
import uk.chromis.data.loader.LocalRes;
import uk.chromis.data.loader.SerializableRead;
import uk.chromis.format.Formats;
import uk.chromis.pos.customers.CustomerInfoExt;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.DataLogicSales;
import uk.chromis.pos.payment.PaymentInfo;
import uk.chromis.pos.payment.PaymentInfoMagcard;
import uk.chromis.pos.tse.TseProcessData;
import uk.chromis.pos.util.StringUtils;

/**
 *
 * @author adrianromero
 */
public final class TicketInfo implements SerializableRead, Externalizable {

    private static final long serialVersionUID = 2765650092387265178L;
    private static final DateFormat m_dateformat = new SimpleDateFormat("HH:mm:ss");

    private String m_sHost;
    private String m_sId;
    private TicketType tickettype;
    private int m_iTicketId;
    private int m_iPickupId;
    private java.util.Date m_dDate;
    private Properties attributes;
    private UserInfo m_User;
    private Double multiply;
    private CustomerInfoExt m_Customer;
    private String m_sActiveCash;
    private List<TicketLineInfo> m_aLines;
    private List<PaymentInfo> payments;
    private List<TicketTaxInfo> taxes;
    private CouponSet m_CouponLines;
    private final String m_sResponse;
    private String loyaltyCardNumber;
    private Boolean oldTicket;
    private Boolean m_sharedticket;
    private UserInfo m_sharedticketUser;
    private String m_nosc;
    private String layawayCustomerName = "";
    private String tseTimeFormat;
    private String tseHashAlgorythmus;
    private String tseStatus;
    private String tsePublicKey;
    private String tillSerialNumber;
    private StartTransactionResult str;
    private FinishTransactionResult ftr;
    private String abrechnungskreis;
    private Boolean autoTseTransactions = true;

    private static String Hostname;

    public static void setHostname(String name) {
        Hostname = name;
    }

    public static String getHostname() {
        return Hostname;
    }

    /**
     * Creates new TicketModel
     */
    public TicketInfo() {
        m_sId = UUID.randomUUID().toString();
        tickettype = TicketType.NORMAL;
        m_iTicketId = 0; // incrementamos
        m_dDate = new Date();
        attributes = new Properties();
        m_User = null;
        m_Customer = null;
        m_sActiveCash = null;
        m_aLines = new ArrayList<>();
        m_CouponLines = new CouponSet();

        payments = new ArrayList<>();
        taxes = null;
        m_sResponse = null;
        oldTicket = false;
        multiply = 0.0;
        m_sharedticket = false;
        m_nosc = "0";
        
        str = null;
        ftr = null;
        abrechnungskreis = "";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(m_sId);
        out.writeInt(tickettype.id);
        out.writeInt(m_iTicketId);
        out.writeObject(m_Customer);
        out.writeObject(m_dDate);
        out.writeObject(m_User);
        out.writeObject(attributes);
        out.writeObject(m_aLines);
        out.writeObject(m_CouponLines);
        out.writeObject(m_nosc);
        out.writeObject(str);
        out.writeObject(ftr);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        m_sId = (String) in.readObject();
        tickettype = TicketType.get(in.readInt());
        m_iTicketId = in.readInt();
        m_Customer = (CustomerInfoExt) in.readObject();
        m_dDate = (Date) in.readObject();
        m_User = (UserInfo) in.readObject();
        attributes = (Properties) in.readObject();
        m_aLines = (List<TicketLineInfo>) in.readObject();
        m_CouponLines = (CouponSet) in.readObject();
        m_nosc = (String) in.readObject();
        try {
            str = (StartTransactionResult) in.readObject();
            ftr = (FinishTransactionResult) in.readObject();
        } catch (IOException e) {
        }
        m_sActiveCash = null;
        payments = new ArrayList<>();
        taxes = null;
        m_sharedticketUser = m_User;
    }

    /**
     *
     * @param dr
     * @throws BasicException
     */
    @Override
    public void readValues(DataRead dr) throws BasicException {
        m_sId = dr.getString(1);
        tickettype = TicketType.get(dr.getInt(2));
        m_iTicketId = dr.getInt(3);
        m_dDate = dr.getTimestamp(4);
        m_sActiveCash = dr.getString(5);
        try {
            byte[] img = dr.getBytes(6);
            if (img != null) {
                attributes.loadFromXML(new ByteArrayInputStream(img));
            }
        } catch (IOException e) {
        }
        m_User = new UserInfo(dr.getString(7), dr.getString(8));
        m_Customer = new CustomerInfoExt(dr.getString(9));
        m_aLines = new ArrayList<>();
        try {
            m_CouponLines = (CouponSet) dr.getObject(10);
        } catch (BasicException e) {
            // Ignore error - may be editing a ticket saved pre-coupon support  
            m_CouponLines = new CouponSet();
        }
        payments = new ArrayList<>();
        taxes = null;
        m_sharedticketUser = m_User;
    }

    public String getLayawayCustomer() {
        return layawayCustomerName;
    }

    public void setLayawayCustomer(String customerName) {
        this.layawayCustomerName = customerName;
    }

    /**
     *
     * @return
     */
    public TicketInfo copyTicket() {
        TicketInfo t = new TicketInfo();

        t.tickettype = tickettype;
        t.m_iTicketId = m_iTicketId;
        t.m_dDate = m_dDate;
        t.m_sActiveCash = m_sActiveCash;
        t.attributes = (Properties) attributes.clone();
        t.m_User = m_User;
        t.m_Customer = m_Customer;

        t.m_aLines = new ArrayList<>();
        for (TicketLineInfo l : m_aLines) {
            t.m_aLines.add(l.copyTicketLine());
        }

        t.m_CouponLines = new CouponSet();
        t.m_CouponLines.copyAll(m_CouponLines);

        t.str = str;
        t.ftr = ftr;

        t.refreshLines();

        t.payments = new LinkedList<>();
        for (PaymentInfo p : payments) {
            t.payments.add(p.copyPayment());
        }
        t.oldTicket = oldTicket;
        t.m_nosc = m_nosc;
        // taxes are not copied, must be calculated again.
        return t;
    }

    public Double getDiscount() {
        Double discount = null;
        if (m_Customer != null) {
            discount = m_Customer.getDiscount();
        }
        if (discount == null) {
            discount = 0.0;
        }

        return discount;
    }

    private Double applyDiscount(Double value) {
        if (value != null && value > 0.0) {
            value = value - (value * getDiscount());
        }
        return value;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return m_sId;
    }

    /**
     *
     * @return
     */
    public TicketType getTicketType() {
        return tickettype;
    }

    /**
     *
     * @param tickettype
     */
    public void setTicketType(final TicketType _tickettype) {
        this.tickettype = _tickettype;
    }

    /**
     *
     * @return
     */
    public int getTicketId() {
        return m_iTicketId;
    }

    /**
     *
     * @param iTicketId
     */
    public void setTicketId(int iTicketId) {
        m_iTicketId = iTicketId;
        // refreshLines();
    }
    
    /**
     * 
     * 
     */
    public void setTseData(long logTimeStart, long logTimeEnd, String serialNumber, 
                           long signatureCounter, String signatureValue, long transactionNumber,
                           String timeFormat, String hashAlgorythmus, String status,
                           String publicKey) {
        if (str == null) {
            str = new StartTransactionResult();
        }
        if (ftr == null) {
            ftr = new FinishTransactionResult();
        }
        str.logTime = logTimeStart;
        ftr.logTime = logTimeEnd;
        tillSerialNumber = serialNumber;
        ftr.signatureCounter = signatureCounter;
        ftr.signatureValue = Hex.decode(signatureValue);
//        ftr.signatureValue = signatureValue.getBytes();
        str.transactionNumber = transactionNumber;
        tseTimeFormat = timeFormat;
        tseHashAlgorythmus = hashAlgorythmus;
        tseStatus = status;
        tsePublicKey = publicKey;
    }
    
    public long getTseTransactionNumber() {
        return str.transactionNumber;
    }
    
    public String printTseTransactionNumber() {
        return String.valueOf(getTseTransactionNumber());
}
    
    public long getTseLogTimeStart() {
        return str.logTime;
    }
    
    public String printTseLogTimeStart() {
        if (str != null) {
            return TseProcessData.dateFormat(str.logTime * 1000);
        } else {
            return "";
        }
    }
    
    public long getTseLogTimeEnd() {
        return ftr.logTime;
    }
    
    public String printTseLogTimeEnd() {
        if (ftr != null) {
            return TseProcessData.dateFormat(ftr.logTime * 1000);
        } else {
            return "";
        }
    }
    
    public String getTseSerialNumber() {
        return Hex.toHexString(str.serialNumber);
    }
    
    public String printTillSerialNumber() {
        return tillSerialNumber;
    }
    
    public String printTseSerialNumber() {
        return getTseSerialNumber();
    }
    
    public String printTseSerialNumber(int vStart) {
        if (!getTseSerialNumber().equals("")) {
            return getTseSerialNumber().substring(vStart-1);
        } else {
            return "";
        }
    }
    
    public String printTseSerialNumber(int vStart, int vEnd) {
        if (!getTseSerialNumber().equals("")) {
            return getTseSerialNumber().substring(vStart-1, vEnd);
        } else {
            return "";
        }
    }
    
    public long getTseSignatureCounter() {
        return ftr.signatureCounter;
    }
    
    public String printTseSignatureCounter() {
        return String.valueOf(getTseSignatureCounter());
    }
    
    public String getTseSignatureValue() {
        return Hex.toHexString(ftr.signatureValue == null ? str.signatureValue : ftr.signatureValue);
    }
    
    public String printTseSignatureValue() {
        return getTseSignatureValue();
    }
    
    public String printTseSignatureValue(int vStart) {
        if (!getTseSignatureValue().equals("")) {
            return getTseSignatureValue().substring(vStart-1);
        } else {
            return "";
        }
    }
    
    public String printTseSignatureValue(int vStart, int vEnd) {
        if (!getTseSignatureValue().equals("")) {
            return getTseSignatureValue().substring(vStart-1, vEnd);
        } else {
            return "";
        }
    }
    
    public void setAutoTseTransactions(Boolean b, DataLogicSales dlSales) {
        autoTseTransactions = b;
        
        if (autoTseTransactions) {
            if (str != null) {
                if (getLinesCount() < 1) {
                    abortTseTransaction(dlSales);
                }
            }
        }
        
    }
    
    public String getTseTimeFormat() {
        return tseTimeFormat;
    }
    
    public String printTseTimeFormat() {
        return getTseTimeFormat();
    }
    
    public String getTseHashAlgorythmus() {
        return tseHashAlgorythmus;
    }
    
    public String printTseHashAlgorythmus() {
        return getTseHashAlgorythmus();
    }
    
    public String getTseStatus() {
        return tseStatus;
    }
    
    public String printTseStatus() {
        return getTseStatus();
    }
    
    public String getTsePublicKey() {
        return tsePublicKey;
    }
    
    public String printTsePublicKey() {
        return getTsePublicKey();
    }
    
    public String getTseQRCode() {
        fillTseProcessData();
        String s = "V0;";
        s = s + AppConfig.getTerminalSerial()+ ";" + 
                "Kassenbeleg-V1" + ";" + 
                TseProcessData.getProcessData() + ";" +
                Long.toString(str.transactionNumber) + ";" +
                Long.toString(ftr.signatureCounter) + ";" +
                TseProcessData.dateFormat(str.logTime * 1000) + ";" +
                TseProcessData.dateFormat(ftr.logTime * 1000) + ";" +
                tseHashAlgorythmus + ";" +
                tseTimeFormat + ";" +
                getTseSignatureValue() + ";" +
                tsePublicKey;
        return s;
    }
    
    public void fillTseProcessData() {
        TseProcessData.clear();
        TseProcessData.vorgangsTyp = "Beleg";

        if (taxes != null) {
            for (TicketTaxInfo tax : taxes) {
                switch (tax.getTaxInfo().getTseTaxCat()) {
                    case 1:
                        TseProcessData.bruttoAllg += tax.getTotal();
                        break;
                    case 2:
                        TseProcessData.bruttoErm += tax.getTotal();
                        break;
                    case 3:
                        TseProcessData.bruttoDurchschn3 += tax.getTotal();
                        break;
                    case 4:
                        TseProcessData.bruttoDurchschn1 += tax.getTotal();
                        break;
                    case 5:
                        TseProcessData.bruttoNull += tax.getTotal();
                        break;
                }
            }
        } else {
            TseProcessData.bruttoNull = this.getTotal();
        }
    
        for (PaymentInfo pay : payments) {
            if ((pay.getName().equals("cash")) || (pay.getName().equals("cashout"))) {
                TseProcessData.zahlungBarEUR += pay.getTotal();
            } else {
                TseProcessData.zahlungUnbarEUR += pay.getTotal();
            }
        }
    }
    
    public void setTillSerialNumber(String v) {
        tillSerialNumber = v;
    }
    
    public boolean mitTse() {
        if (AppConfig.getTseStatus().equals("")) {
            if (AppConfig.getTse() != null) {
                return true;
            }
        }
        return false;
    }
    
    public void setStartTransactionResult(StartTransactionResult r) {
        str = r;
    }
    
    public StartTransactionResult getStartTransactionResult() {
        return str;
    }
    
    public void clearStartTransactionResult() {
        str = null;
    }
    
    public boolean startTseTransaction() {
        if ((str == null) && (getLinesCount()>0)) {
            startTseTransactionForce();
        }
        return true;
    }
    
    public boolean startTseTransactionForce() {
        if (mitTse()) {
            fillTseProcessData();
            tillSerialNumber = AppConfig.getTerminalSerial();
            str = AppConfig.getTse().startTransaction(AppConfig.getTerminalSerial(), 
                                                         TseProcessData.getProcessData(), 
                                                         AppConfig.getTse().getProcesstypeKassenbeleg(), 
                                                         "");
            if (str == null) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }
    
    public boolean getTseStarted() {
        if (str == null) {
            return false;
        } else if (str.transactionNumber == 0) {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean finishTseTransaction(DataLogicSales dlSales) {
        if (getLinesCount() == 0) {
            if (str != null) {
                return abortTseTransaction(dlSales);
            }
            return true;
        }
        
        if (str == null) {
            startTseTransaction();
        }
        
        return finishTseTransactionForce(dlSales);
    }
    
    public boolean finishTseTransactionForce(DataLogicSales dlSales) {
        if ((str != null) && mitTse()) {
            fillTseProcessData();
            tillSerialNumber = AppConfig.getTerminalSerial();
            ftr = AppConfig.getTse().finishTransaction(AppConfig.getTerminalSerial(), 
                                                       str.transactionNumber, 
                                                       TseProcessData.getProcessData(), 
                                                       AppConfig.getTse().getProcesstypeKassenbeleg(),
                                                       "");
        }
        if (ftr != null) {
            tseTimeFormat = AppConfig.getTse().getTimeFormat();
            tseHashAlgorythmus = AppConfig.getTse().getSignaturAlgorithmus();
            tseStatus = AppConfig.getTse().getCertificationId();
            tsePublicKey = AppConfig.getTse().getPublicKey();
            return true;
        } else {
            setTseData(System.currentTimeMillis()/1000,
                       System.currentTimeMillis()/1000,
                       "",  0,
                       "",  0,
                       "",  "",
                       "TSE-Ausfall",   "");
            return false;
        }
    }
    
    public boolean abortTseTransaction(DataLogicSales dlSales) {
        Boolean result = true;
        if (str != null) {
            if (str.transactionNumber > 0) {
                TseProcessData.clear();
                TseProcessData.vorgangsTyp = "AVBelegabbruch";
                ftr = AppConfig.getTse().finishTransaction(AppConfig.getTerminalSerial(), 
                                                           str.transactionNumber, 
                                                           TseProcessData.getProcessData(), 
                                                           AppConfig.getTse().getProcesstypeKassenbeleg(),
                                                           "");
            }
        }
        if (ftr != null) {
            tseTimeFormat = AppConfig.getTse().getTimeFormat();
            tseHashAlgorythmus = AppConfig.getTse().getSignaturAlgorithmus();
            tseStatus = AppConfig.getTse().getCertificationId();
            tsePublicKey = AppConfig.getTse().getPublicKey();
        } else {
            setTseData(System.currentTimeMillis()/1000, 
                       System.currentTimeMillis()/1000, 
                       "",    0, 
                       "",    0,
                       "",    "",
                       "TSE-Ausfall",
                       "");
            result = false;
        }
        try {
            dlSales.saveTicketTseFailed(this);
        } catch (BasicException ex) {
            Logger.getLogger(TicketInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
                                
                                
        m_sId = UUID.randomUUID().toString();
        str = null;
        ftr = null;
        return result;
    }
    
    /**
     *
     * @param iTicketId
     */
    public void setPickupId(int iTicketId) {
        m_iPickupId = iTicketId;
    }

    /**
     *
     * @return
     */
    public int getPickupId() {
        return m_iPickupId;
    }

    /**
     *
     * @param info
     * @return
     */
    public String getName(Object info) {
        StringBuilder name = new StringBuilder();
        
        if (m_User != null) {
            name.append(m_User.getName());
            name.append(" - ");
        }

        if (m_iPickupId > 0) {
            name.append(" ");
            name.append(m_iPickupId);
        }

        if (info == null) {
            if (m_iTicketId == 0) {
                name.append("(").append(m_dateformat.format(m_dDate)).append("-").append(Long.toString(m_dDate.getTime() % 1000)).append(")");
            } else {
                name.append(Integer.toString(m_iTicketId));
            }
        } else {
            name.append(info.toString());

        }
        if (getCustomerId() != null) {
            name.append(" - ");
            name.append(m_Customer.toString());
//            Double discount = getDiscount();
//            if (discount > 0.0) {
//                name.append(" -");
//                name.append(Formats.PERCENT.formatValue(discount));
//            }
        }
        return name.toString();
    }

    public String getName(Object info, String pickupID) {
        StringBuilder name = new StringBuilder();

        // if (m_User != null) {
        name.append(pickupID);
        name.append(" - ");
        // }

        if (info == null) {
            if (m_iTicketId == 0) {
                name.append("(").append(m_dateformat.format(m_dDate)).append(" ").append(Long.toString(m_dDate.getTime() % 1000)).append(")");
            } else {
                name.append(Integer.toString(m_iTicketId));
            }
        } else {
            name.append(info.toString());

        }
        if (getCustomerId() != null) {
            name.append(" - ");
            name.append(m_Customer.toString());
            Double discount = getDiscount();
            if (discount > 0.0) {
                name.append(" -");
                name.append(Formats.PERCENT.formatValue(discount));
            }
        }
        return name.toString();
    }

    public String getName() {
        if (layawayCustomerName.equals("")) {
            return getName(null);
        } else {
            return layawayCustomerName;
        }
    }

    public java.util.Date getDate() {
        return m_dDate;
    }

    public void setDate(java.util.Date dDate) {
        m_dDate = dDate;
    }

    public String getNoSC() {
        return m_nosc;
    }

    public void setNoSC(String value) {
        m_nosc = value;
    }

    public UserInfo getUser() {
        return m_User;
    }

    public String getUserName() {
        return m_User.getName();
    }

    public UserInfo getSharedTicketUser() {
        return m_sharedticketUser;
    }

    public void setUser(UserInfo value) {
        m_User = value;
        attributes.setProperty("user.name", m_User.getName());
    }

    public CustomerInfoExt getCustomer() {
        return m_Customer;
    }
    
    public void setAbrechnungskreis(String v) {
        abrechnungskreis = v;
    }
    
    public String getAbrechnungskreis() {
        return abrechnungskreis;
    }

    public void setCustomer(CustomerInfoExt value) {
        if (value == null) return;
        m_Customer = value;
        if (m_Customer.getName() != null) attributes.setProperty("customer.name", m_Customer.getName());
        if (m_Customer.getId() != null) attributes.setProperty("customer.id", m_Customer.getId());
        attributes.setProperty("customer.type", "");
        attributes.setProperty("customer.street", m_Customer.getAddress()+" "+m_Customer.getAddress2());
        if (m_Customer.getPostal() != null) attributes.setProperty("customer.postal", m_Customer.getPostal());
        if (m_Customer.getCity() != null) attributes.setProperty("customer.city", m_Customer.getCity());
        if (m_Customer.getCountry() != null) attributes.setProperty("customer.country", m_Customer.getCountry());
        if (m_Customer.getTaxid() != null) attributes.setProperty("customer.taxid", m_Customer.getTaxid());
    }

    public String getCustomerId() {
        if (m_Customer == null) {
            return null;
        } else {
            return m_Customer.getId();
        }
    }

    public Boolean hasCustomer() {
        if (m_Customer == null) {
            return false;
        } else {
            return true;
        }
    }
    
    public String getTransactionID() {
        return (getPayments().size() > 0)
                ? (getPayments().get(getPayments().size() - 1)).getTransactionID()
                : StringUtils.getCardNumber(); //random transaction ID
    }

    public String getReturnMessage() {
        return ((getPayments().get(getPayments().size() - 1)) instanceof PaymentInfoMagcard)
                ? ((PaymentInfoMagcard) (getPayments().get(getPayments().size() - 1))).getReturnMessage()
                : LocalRes.getIntString("Button.OK");
    }

    public void setActiveCash(String value) {
        m_sActiveCash = value;
    }

    public String getActiveCash() {
        return m_sActiveCash;
    }

    public String getProperty(String key) {
        return attributes.getProperty(key);
    }

    public String getProperty(String key, String defaultvalue) {
        return attributes.getProperty(key, defaultvalue);
    }

    public void setProperty(String key, String value) {
        attributes.setProperty(key, value);
    }

    public Properties getProperties() {
        return attributes;
    }

    public TicketLineInfo getLine(int index) {
        return m_aLines.get(index);
    }

    public void addLine(TicketLineInfo oLine) {
        oLine.setTicket(m_sId, m_aLines.size());
        m_aLines.add(oLine);
        if (autoTseTransactions) {
            startTseTransaction();
        }
    }

    public void addCouponLine(String id, int line, String text) {
        m_CouponLines.add(id, line, text);
    }

    public void removeCouponLine(String id, int line) {
        m_CouponLines.remove(id, line);
    }

    public void removeCoupon(String id) {
        if (id == null) {
            // Remove all coupons
            m_CouponLines.clear();
        } else {
            m_CouponLines.remove(id);
        }
    }

    public int checkAndAddLine(TicketLineInfo oLine, boolean flag) {
        // returns index of product in the ticket list or -1 if new product
        if (m_aLines.size() == 0 || !flag) {
            oLine.setTicket(m_sId, m_aLines.size());
            m_aLines.add(oLine);
            if (autoTseTransactions) {
                startTseTransaction();
            }
            return -1;
        } else {
            int size = m_aLines.size();
            for (int i = 0; i < size; i++) {
                TicketLineInfo temp = m_aLines.get(i);
                if ((temp.getProductID().equals(oLine.getProductID())) && oLine.getProductAttSetId() == null) {
                    m_aLines.get(i).setMultiply(m_aLines.get(i).getMultiply() + oLine.getMultiply());
                    return i;
                }
            }
            oLine.setTicket(m_sId, m_aLines.size());
            m_aLines.add(oLine);
            if (autoTseTransactions) {
                startTseTransaction();
            }
            return -1;
        }
    }

    public void insertLine(int index, TicketLineInfo oLine) {
        m_aLines.add(index, oLine);
        refreshLines();
        if (autoTseTransactions) {
            startTseTransaction();
        }
    }

    public void setLine(int index, TicketLineInfo oLine) {
        oLine.setTicket(m_sId, index);
        m_aLines.set(index, oLine);
        if (autoTseTransactions) {
            startTseTransaction();
        }
    }

    public void removeLine(int index, DataLogicSales dlSales) {
        m_aLines.remove(index);
        refreshLines();
        if (getLinesCount() < 1) {
            if (autoTseTransactions) {
                abortTseTransaction(dlSales);
            }
        }
    }

    private void refreshLines() {
        for (int i = 0; i < m_aLines.size(); i++) {
            getLine(i).setTicket(m_sId, i);
        }
        if (autoTseTransactions) {
            startTseTransaction();
        }
    }

    public int getLinesCount() {
        return m_aLines.size();
    }

    public double getArticlesCount() {
        double dArticles = 0.0;
        TicketLineInfo oLine;

        for (Iterator<TicketLineInfo> i = m_aLines.iterator(); i.hasNext();) {
            oLine = i.next();
            dArticles += oLine.getMultiply();
        }
        return dArticles;
    }

    public double getSubTotal() {
        double sum = 0.0;
        for (TicketLineInfo line : m_aLines) {
            sum += line.getSubValue();
        }
        return sum;
    }

    public double getTax() {
        double sum = 0.0;
        if (hasTaxesCalculated()) {
            for (TicketTaxInfo tax : taxes) {
                sum += tax.getTax(); // Taxes are already rounded...
            }
        } else {
            for (TicketLineInfo line : m_aLines) {
                sum += line.getTax();
            }
        }
        return sum;
    }

    public double getTotal() {
        return getSubTotal() + getTax();
    }

    public double getTotalPaid() {
        double sum = 0.0;
        for (PaymentInfo p : payments) {
            if (!"debtpaid".equals(p.getName())) {
                sum += p.getTotal();
            }
        }
        return sum;
    }

    public double getTotalChange() {
        double sum = 0.0;
        for (PaymentInfo p : payments) {
            if (!"debtpaid".equals(p.getName())) {
                sum += p.getChange();
            }
        }
        return sum;
    }

    public double getChange() {
        return getTotalChange();
    }

    public double getTotalTendered() {
        double sum = 0.0;
        for (PaymentInfo p : payments) {
            if (!"debtpaid".equals(p.getName())) {
                sum += p.getTendered();
            }
        }
        return sum;
    }

    public double getTendered() {
        return getTotalTendered();
    }

    public List<String> getCouponLines() {
        return m_CouponLines.getCouponLines();
    }

    public List<TicketLineInfo> getLines() {
        return m_aLines;
    }

    public void setLines(List<TicketLineInfo> l) {
        m_aLines = l;
    }

    public List<PaymentInfo> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentInfo> l) {
        payments = l;
    }

    public void resetPayments() {
        payments = new ArrayList<>();
    }

    public List<TicketTaxInfo> getTaxes() {
        return taxes;
    }

    public boolean hasTaxesCalculated() {
        return taxes != null;
    }

    public void setTaxes(List<TicketTaxInfo> l) {
        taxes = l;
    }

    public void resetTaxes() {
        taxes = null;
    }

    public TicketTaxInfo getTaxLine(TaxInfo tax) {
        for (TicketTaxInfo taxline : taxes) {
            if (tax.getId().equals(taxline.getTaxInfo().getId())) {
                return taxline;
            }
        }
        return new TicketTaxInfo(tax);
    }

    public TicketTaxInfo[] getTaxLines() {
        Map<String, TicketTaxInfo> m = new HashMap<>();

        TicketLineInfo oLine;
        for (Iterator<TicketLineInfo> i = m_aLines.iterator(); i.hasNext();) {
            oLine = i.next();

            TicketTaxInfo t = m.get(oLine.getTaxInfo().getId());
            if (t == null) {
                t = new TicketTaxInfo(oLine.getTaxInfo());
                m.put(t.getTaxInfo().getId(), t);
            }
            t.add(oLine.getSubValue());
        }
        Collection<TicketTaxInfo> avalues = m.values();
        return avalues.toArray(new TicketTaxInfo[avalues.size()]);
    }

    public String printId() {

        String receiptSize = (AppConfig.getInstance().getProperty("till.receiptsize"));
        String receiptPrefix = (AppConfig.getInstance().getProperty("till.receiptprefix"));

        if (m_iTicketId > 0) {
            String tmpTicketId = Integer.toString(m_iTicketId);
            if (receiptSize == null || (Integer.parseInt(receiptSize) <= tmpTicketId.length())) {
                if (receiptPrefix != null) {
                    tmpTicketId = receiptPrefix + tmpTicketId;
                }
                return tmpTicketId;
            }
            while (tmpTicketId.length() < Integer.parseInt(receiptSize)) {
                tmpTicketId = "0" + tmpTicketId;
            }
            if (receiptPrefix != null) {
                tmpTicketId = receiptPrefix + tmpTicketId;
            }
            return tmpTicketId;
        } else {
            return "";
        }
    }

    public String printDate() {
        return Formats.TIMESTAMP.formatValue(m_dDate);
    }

    public String printUser() {
        return m_User == null ? "" : m_User.getName();
    }

    public String getHost() {
        return m_sHost;
    }

    public String printHost() {
        return StringUtils.encodeXML(m_sHost);
    }

    public void clearCardNumber() {
        loyaltyCardNumber = null;
    }

    public void setLoyaltyCardNumber(String cardNumber) {
        loyaltyCardNumber = cardNumber;
    }

    public String getLoyaltyCardNumber() {
        return (loyaltyCardNumber);
    }

    public String printCustomer() {
        return m_Customer == null ? "" : m_Customer.getName();
    }

    public String printArticlesCount() {
        return Formats.DOUBLE.formatValue(getArticlesCount());
    }

    public String printSubTotal() {
        return Formats.CURRENCY.formatValue(getSubTotal());
    }

    public String printTax() {
        return Formats.CURRENCY.formatValue(getTax());
    }

    public String printTotal() {
        return Formats.CURRENCY.formatValue(getTotal());
    }

    public String printTotalPaid() {
        return Formats.CURRENCY.formatValue(getTotalPaid());
    }

    public String printTendered() {
        return Formats.CURRENCY.formatValue(getTendered());
    }

    public String printOriginalUser() {
        if (getSharedTicketUser() == null) {
            return "";
        }
        return getSharedTicketUser().getName();
    }

    public String printChange() {
        return Formats.CURRENCY.formatValue(getTendered() - getTotal());
    }

    public String VoucherReturned() {
        return Formats.CURRENCY.formatValue(getTotalPaid() - getTotal());
    }

    public boolean getOldTicket() {
        return (oldTicket);
    }

    public void setSharedTicket(Boolean shared) {
        m_sharedticket = shared;
    }

    public boolean isSharedTicket() {
        return (m_sharedticket);
    }

    public void setdDate(java.util.Date m_date) {
        m_dDate = m_date;
    }

    public java.util.Date getdDate() {
        return m_dDate;
    }

    public void setOldTicket(Boolean otState) {
        oldTicket = otState;
    }

}
