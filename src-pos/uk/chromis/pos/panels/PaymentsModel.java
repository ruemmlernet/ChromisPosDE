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

package uk.chromis.pos.panels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import uk.chromis.basic.BasicException;
import uk.chromis.data.loader.DataRead;
import uk.chromis.data.loader.Datas;
import uk.chromis.data.loader.SerializableRead;
import uk.chromis.data.loader.SerializerReadBasic;
import uk.chromis.data.loader.SerializerReadClass;
import uk.chromis.data.loader.SerializerWriteString;
import uk.chromis.data.loader.StaticSentence;
import uk.chromis.format.Formats;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.forms.AppView;
import uk.chromis.pos.forms.DataLogicSystem;
import uk.chromis.pos.util.StringUtils;

public class PaymentsModel {

    private String m_sHost;
    private int m_iSeq;
    private Date m_dDateStart;
    private Date m_dDateEnd;
    private Date rDate;
    private Double m_dMoneyStart;
    private Double m_dMoneyEnd;
    private Double m_dMoneyDifference;

    private Integer m_iPayments;
    private Integer m_iCashCounts;
    private Double m_dPaymentsTotal;
    private java.util.List<PaymentsLine> m_lpayments;
    private java.util.List<CashCountLine> m_lcashCount;
//    private java.util.List<CashCountLine> m_lcashCountRes;

    private Integer m_iCategorySalesRows;
    private Double m_dCategorySalesTotalUnits;
    private Double m_dCategorySalesTotal;
    private java.util.List<CategorySalesLine> m_lcategorysales;

    private Integer m_iProductSalesRows;
    private Double m_dProductSalesTotalUnits;
    private Double m_dProductSalesTotal;
    private java.util.List<ProductSalesLine> m_lproductsales;
    private java.util.List<RemovedProductLines> m_lremovedlines;

    private final static String[] PAYMENTHEADERS = {"Label.Payment", "label.Money"};

    private Integer m_iSales;
    private Double m_dSalesBase;
    private Double m_dSalesTaxes;
    private Double m_dSalesTaxNet;
    private java.util.List<SalesLine> m_lsales;

    private final static String[] SALEHEADERS = {"label.taxcategory", "label.totaltax", "label.totalnet"};
    private DataLogicSystem dlSystem;

    private PaymentsModel() {
    }

    /**
     *
     * @return
     */
    public static PaymentsModel emptyInstance() {

        PaymentsModel p = new PaymentsModel();

        p.m_iPayments = 0;
        p.m_iCashCounts = 0;
        p.m_dPaymentsTotal = 0.0;
        p.m_lpayments = new ArrayList<>();
        p.m_lcashCount = new ArrayList<>();
        p.m_iCategorySalesRows = 0;
        p.m_dCategorySalesTotalUnits = 0.0;
        p.m_dCategorySalesTotal = 0.0;
        p.m_lcategorysales = new ArrayList<>();
        p.m_iSales = null;
        p.m_dSalesBase = null;
        p.m_dSalesTaxes = null;
        p.m_dSalesTaxNet = null;
        p.m_dMoneyStart = 0.0;
        p.m_dMoneyEnd = 0.0;
        p.m_dMoneyDifference = 0.0;

        p.m_iProductSalesRows = 0;
        p.m_dProductSalesTotalUnits = 0.0;
        p.m_dProductSalesTotal = 0.0;
        p.m_lproductsales = new ArrayList<>();
        // end

        p.m_lremovedlines = new ArrayList<>();

        p.m_lsales = new ArrayList<>();

        return p;
    }

    /**
     *
     * @param app
     * @return
     * @throws BasicException
     */
    public static PaymentsModel loadInstance(AppView app) throws BasicException {

        PaymentsModel p = new PaymentsModel();

        // Propiedades globales
        p.m_sHost = app.getProperties().getHost();
        p.m_iSeq = app.getActiveCashSequence();
        p.m_dDateStart = app.getActiveCashDateStart();
        p.m_dDateEnd = null;

        Object[] valcategorysales = (Object[]) new StaticSentence(app.getSession(), 
                  "SELECT COUNT(*), SUM(TICKETLINES.UNITS), SUM((TICKETLINES.PRICE + TICKETLINES.PRICE * TAXES.RATE ) * TICKETLINES.UNITS) "
                + "FROM TICKETLINES, TICKETS, RECEIPTS, TAXES "
                + "WHERE TICKETLINES.TICKET = TICKETS.ID AND TICKETS.ID = RECEIPTS.ID AND TICKETLINES.TAXID = TAXES.ID "
                + "AND TICKETLINES.PRODUCT IS NOT NULL AND RECEIPTS.MONEY = ? "
                + "GROUP BY RECEIPTS.MONEY", SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.INT, Datas.DOUBLE, Datas.DOUBLE}))
                .find(app.getActiveCashIndex());

        if (valcategorysales == null) {
            p.m_iCategorySalesRows = 0;
            p.m_dCategorySalesTotalUnits = 0.0;
            p.m_dCategorySalesTotal = 0.0;
        } else {
            p.m_iCategorySalesRows = (Integer) valcategorysales[0];
            p.m_dCategorySalesTotalUnits = (Double) valcategorysales[1];
            p.m_dCategorySalesTotal = (Double) valcategorysales[2];
        }

        // MoneyStart, -End, -Difference / Anfangsbestand Kasse, Endbestand und Differenz
        p.m_dMoneyEnd = 0.0;
        p.m_dMoneyDifference = 0.0;

        Object[] valcategorysales2 = (Object[]) new StaticSentence(app.getSession(), 
                  "SELECT MONEYSTART FROM CLOSEDCASH WHERE MONEY = ? ",
                SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.DOUBLE}))
                .find(app.getActiveCashIndex());

        if (valcategorysales2 != null) {
            p.m_dMoneyStart = (Double) valcategorysales2[0];
        } else {
            p.m_dMoneyStart = 0.0;
        }

        List categorys = new StaticSentence(app.getSession(), "SELECT a.NAME, sum(c.UNITS), sum(c.UNITS * (c.PRICE + (c.PRICE * d.RATE))) "
                + "FROM CATEGORIES as a "
                + "LEFT JOIN PRODUCTS as b on a.id = b.CATEGORY "
                + "LEFT JOIN TICKETLINES as c on b.id = c.PRODUCT "
                + "LEFT JOIN TAXES as d on c.TAXID = d.ID "
                + "LEFT JOIN RECEIPTS as e on c.TICKET = e.ID "
                + "WHERE e.MONEY = ? "
                + "GROUP BY a.NAME", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.CategorySalesLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
                .list(app.getActiveCashIndex());

        if (categorys == null) {
            p.m_lcategorysales = new ArrayList();
        } else {
            p.m_lcategorysales = categorys;
        }

        // Payments
        Object[] valtickets = (Object[]) new StaticSentence(app.getSession(), "SELECT COUNT(*), SUM(PAYMENTS.TOTAL) "
                + "FROM PAYMENTS, RECEIPTS "
                + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.TOTAL<>0 AND RECEIPTS.MONEY = ?", SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.INT, Datas.DOUBLE}))
                .find(app.getActiveCashIndex());

        if (valtickets == null) {
            p.m_iPayments = 0;
            p.m_dPaymentsTotal = 0.0;
        } else {
            p.m_iPayments = (Integer) valtickets[0];
            p.m_dPaymentsTotal = (Double) valtickets[1];
        }

        List l = new StaticSentence(app.getSession(), "SELECT PAYMENTS.PAYMENT, SUM(PAYMENTS.TOTAL), PAYMENTS.NOTES "
                + "FROM PAYMENTS, RECEIPTS "
                + "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND PAYMENTS.TOTAL<>0 AND RECEIPTS.MONEY = ? "
                + "GROUP BY PAYMENTS.PAYMENT, PAYMENTS.NOTES", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.PaymentsLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
                .list(app.getActiveCashIndex());

        if (l == null) {
            p.m_lpayments = new ArrayList();
        } else {
            p.m_lpayments = l;
        }

        // CashCount
        List ccl = new StaticSentence(app.getSession(), "SELECT coalesce(cc1.value, cc2.value) value, coalesce(cc1.count, 0) ccount, coalesce(cc1.count_res, cc2.count_res) ccount_res " +
                                                        "FROM (select c0.money m0, c1.money m1 " +
                                                        "      from closedcash c1, closedcash c2, (select ? money) c0 " +
                                                        "      where c1.host = c2.host " +
                                                        "        and c1.HOSTSEQUENCE = c2.hostsequence-1 " +
                                                        "        and c2.money = c0.money) cc0 " +
                                                        "LEFT JOIN CLOSEDCASH_COUNT cc1 on cc1.money = cc0.m0 " +
                                                        "LEFT JOIN CLOSEDCASH_COUNT cc2 on cc2.money = cc0.m1 and (cc1.value = cc2.value or cc1.value is null)",
                SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.CashCountLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
                .list(app.getActiveCashIndex());

        if (ccl == null) {
            p.m_lcashCount = new ArrayList();
            p.m_iCashCounts = 0;
        } else {
            p.m_lcashCount = ccl;
            p.m_iCashCounts = 1;
        }        
        
        // Sales
        Object[] recsales = (Object[]) new StaticSentence(app.getSession(),
                "SELECT COUNT(DISTINCT RECEIPTS.ID), SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) "
                + "FROM RECEIPTS, TICKETLINES WHERE RECEIPTS.ID = TICKETLINES.TICKET AND RECEIPTS.MONEY = ?",
                SerializerWriteString.INSTANCE,
                new SerializerReadBasic(new Datas[]{Datas.INT, Datas.DOUBLE}))
                .find(app.getActiveCashIndex());
        if (recsales == null) {
            p.m_iSales = null;
            p.m_dSalesBase = null;
        } else {
            p.m_iSales = (Integer) recsales[0];
            p.m_dSalesBase = (Double) recsales[1];
        }

        // Taxes
        Object[] rectaxes = (Object[]) new StaticSentence(app.getSession(),
                "SELECT SUM(TAXLINES.AMOUNT), SUM(TAXLINES.BASE) "
                + "FROM RECEIPTS, TAXLINES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND RECEIPTS.MONEY = ?", SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.DOUBLE, Datas.DOUBLE}))
                .find(app.getActiveCashIndex());
        if (rectaxes == null) {
            p.m_dSalesTaxes = null;
            p.m_dSalesTaxNet = null;
        } else {
            p.m_dSalesTaxes = (Double) rectaxes[0];
            p.m_dSalesTaxNet = (Double) rectaxes[1];
        }

        try {
            List<SalesLine> asales = new StaticSentence(app.getSession(),
                    "SELECT TAXCATEGORIES.NAME, SUM(NEWTAXLINES.AMOUNT), SUM(NEWTAXLINES.BASE), SUM(NEWTAXLINES.BASE + NEWTAXLINES.AMOUNT) "
                    + "FROM RECEIPTS, "
                    + "(SELECT TAXLINES.ID,RECEIPT,TAXID,BASE,SUM(AMOUNT) as AMOUNT "
                    + " FROM TAXLINES,TAXES where TAXLINES.TAXID=TAXES.ID and PARENTID is not null GROUP BY RECEIPT, PARENTID "
                    + " union "
                    + " SELECT TAXLINES.ID, RECEIPT, TAXID, BASE, AMOUNT as AMOUNT "
                    + " FROM TAXLINES,TAXES "
                    + " where TAXLINES.TAXID=TAXES.ID and PARENTID is null) NEWTAXLINES, "
                    + " TAXES, TAXCATEGORIES "
                    + " WHERE RECEIPTS.ID = NEWTAXLINES.RECEIPT AND NEWTAXLINES.TAXID = TAXES.ID AND TAXES.CATEGORY = TAXCATEGORIES.ID"
                    + " AND RECEIPTS.MONEY = ?"
                    + " GROUP BY TAXCATEGORIES.NAME ", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.SalesLine.class))
                    .list(app.getActiveCashIndex());
            if (asales == null) {
                p.m_lsales = new ArrayList<>();
            } else {
                p.m_lsales = asales;
            }
        } catch (BasicException e) {
            List<SalesLine> asales = new StaticSentence(app.getSession(),
                    "SELECT TAXCATEGORIES.NAME, SUM(TAXLINES.AMOUNT), SUM(TAXLINES.BASE), SUM(TAXLINES.BASE + TAXLINES.AMOUNT) "
                    + "FROM RECEIPTS, TAXLINES, TAXES, TAXCATEGORIES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND TAXLINES.TAXID = TAXES.ID AND TAXES.CATEGORY = TAXCATEGORIES.ID "
                    + "AND RECEIPTS.MONEY = ?"
                    + "GROUP BY TAXCATEGORIES.NAME", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.SalesLine.class))
                    .list(app.getActiveCashIndex());
            if (asales == null) {
                p.m_lsales = new ArrayList<>();
            } else {
                p.m_lsales = asales;
            }
        }

        SimpleDateFormat ndf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDateFormatted = ndf.format(app.getActiveCashDateStart());
        List removedLines = new StaticSentence(app.getSession(), "SELECT LINEREMOVED.NAME, LINEREMOVED.TICKETID, LINEREMOVED.PRODUCTNAME, SUM(LINEREMOVED.UNITS) AS TOTAL_UNITS  "
                + "FROM LINEREMOVED "
                + "WHERE LINEREMOVED.REMOVEDDATE > ? "
                + "GROUP BY LINEREMOVED.NAME, LINEREMOVED.TICKETID, LINEREMOVED.PRODUCTNAME", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.RemovedProductLines.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
                .list(startDateFormatted);

        if (removedLines == null) {
            p.m_lremovedlines = new ArrayList();
        } else {
            p.m_lremovedlines = removedLines;
        }

        // Product Sales
        Object[] valproductsales = (Object[]) new StaticSentence(app.getSession(), "SELECT COUNT(*), SUM(TICKETLINES.UNITS), SUM((TICKETLINES.PRICE + TICKETLINES.PRICE * TAXES.RATE ) * TICKETLINES.UNITS) "
                + "FROM TICKETLINES, TICKETS, RECEIPTS, TAXES "
                + "WHERE TICKETLINES.TICKET = TICKETS.ID AND TICKETS.ID = RECEIPTS.ID AND TICKETLINES.TAXID = TAXES.ID AND TICKETLINES.PRODUCT IS NOT NULL AND RECEIPTS.MONEY = ? "
                + "GROUP BY RECEIPTS.MONEY", SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.INT, Datas.DOUBLE, Datas.DOUBLE}))
                .find(app.getActiveCashIndex());

        if (valproductsales == null) {
            p.m_iProductSalesRows = 0;
            p.m_dProductSalesTotalUnits = 0.0;
            p.m_dProductSalesTotal = 0.0;
        } else {
            p.m_iProductSalesRows = (Integer) valproductsales[0];
            p.m_dProductSalesTotalUnits = (Double) valproductsales[1];
            p.m_dProductSalesTotal = (Double) valproductsales[2];
        }

        List products = new StaticSentence(app.getSession(), "SELECT PRODUCTS.NAME, SUM(TICKETLINES.UNITS), TICKETLINES.PRICE, TAXES.RATE "
                + "FROM TICKETLINES, TICKETS, RECEIPTS, PRODUCTS, TAXES "
                + "WHERE TICKETLINES.PRODUCT = PRODUCTS.ID AND TICKETLINES.TICKET = TICKETS.ID AND TICKETS.ID = RECEIPTS.ID AND TICKETLINES.TAXID = TAXES.ID AND RECEIPTS.MONEY = ? "
                + "GROUP BY PRODUCTS.NAME, TICKETLINES.PRICE, TAXES.RATE", SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentsModel.ProductSalesLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
                .list(app.getActiveCashIndex());

        if (products == null) {
            p.m_lproductsales = new ArrayList();
        } else {
            p.m_lproductsales = products;
        }

        return p;
    }

    /**
     *
     * @return
     */
    public int getPayments() {
        return m_iPayments;
    }

    /**
     *
     * @return
     */
    public int getCashCounts() {
        return m_iCashCounts;
    }

    /**
     *
     * @return
     */
    public double getTotal() {
        return m_dPaymentsTotal;
    }

    /**
     *
     * @return
     */
    public String getHost() {
        return m_sHost;
    }

    /**
     *
     * @return
     */
    public int getSequence() {
        return m_iSeq;
    }

    /**
     *
     * @return
     */
    public Date getDateStart() {
        return m_dDateStart;
    }

    /**
     *
     * @param dValue
     */
    public void setDateEnd(Date dValue) {
        m_dDateEnd = dValue;
    }

    /**
     *
     * @return
     */
    public Date getDateEnd() {
        return m_dDateEnd;
    }

    /**
     *
     * @return
     */
    public String getDateStartDerby() {
        SimpleDateFormat ndf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ndf.format(m_dDateStart);
    }

    /**
     * 
     * @return
     */
    public Double getMoneyStart() {
        return m_dMoneyStart;
    }
    
    public Double getMoneyEnd() {
        return m_dMoneyEnd;
    }
    
    public Double getMoneyDifference() {
        return m_dMoneyDifference;
    }
            
    public Double getMoneyCash() {
        return m_dMoneyEnd + m_dMoneyDifference;
    }
    
    public Double getPaymentsTotal() {
        return m_dPaymentsTotal;
    }
    
    public void setMoneyEnd(double value) {
        m_dMoneyEnd = value;
    }
    
    public void setMoneyDifference(double value) {
        m_dMoneyDifference = value;
    }
    
    public void clearCashCountLines() {
        m_lcashCount = new ArrayList();
    }
    
    public void setCashCountLine(double value, int count, int countRes) {
        CashCountLine ccl = new CashCountLine();
        ccl.m_Count = count;
        ccl.m_Value = value;
        ccl.m_CountRes = countRes;
        
        m_lcashCount.add(ccl);
        m_iCashCounts = 1;
    }
    
    public void setCashCountResLine(double value, int count) {
        CashCountLine ccl = new CashCountLine();
        ccl.m_Count = count;
        ccl.m_Value = value;
        
        m_lcashCount.add(ccl);
        m_iCashCounts = 1;
    }
    
    /**
     *
     * @return
     */
    public String printHost() {
//        return m_sHost;
        return StringUtils.encodeXML(m_sHost);
    }

    /**
     *
     * @return
     */
    public String printSequence() {
        return Formats.INT.formatValue(m_iSeq);
    }

    /**
     *
     * @return
     */
    public String printDateStart() {
        return Formats.TIMESTAMP.formatValue(m_dDateStart);
    }

    /**
     *
     * @return
     */
    public String printDateEnd() {
        return Formats.TIMESTAMP.formatValue(m_dDateEnd);
    }

    /**
     *
     * @return
     */
    public String printPayments() {
        return Formats.INT.formatValue(m_iPayments);
    }

    /**
     *
     * @return
     */
    public String printPaymentsTotal() {
        return Formats.CURRENCY.formatValue(m_dPaymentsTotal);
    }
    
    // Anfangsbestand
    public String printMoneyStart() {
        return Formats.CURRENCY.formatValue(m_dMoneyStart);
    }
    
    // Soll-Endbestand
    public String printMoneyEnd() {
        return Formats.CURRENCY.formatValue(m_dMoneyEnd);
    }
    
    // Differenz zwischen soll und ist
    public String printMoneyDifference() {
        return Formats.CURRENCY.formatValue(m_dMoneyDifference);
    }
    
    // tatsächlicher Endbestand
    public String printMoneyCash() {
        return Formats.CURRENCY.formatValue(m_dMoneyEnd + m_dMoneyDifference);
    }

    // Soll-Bargeld-Änderung
    public String printPaymentsCash() {
        return Formats.CURRENCY.formatValue(m_dMoneyEnd - m_dMoneyStart);
    }
    
    /**
     *
     * @return
     */
    public List<PaymentsLine> getPaymentLines() {
        return m_lpayments;
    }
    
    /**
     * 
     * @return
     */
    public List<CashCountLine> getCashCountLines() {
        return m_lcashCount;
    }

    /**
     *
     * @return
     */
    public int getSales() {
        return m_iSales == null ? 0 : m_iSales;
    }

    /**
     *
     * @return
     */
    public String printSales() {
        return Formats.INT.formatValue(m_iSales);
    }

    /**
     *
     * @return
     */
    public String printSalesBase() {
        return Formats.CURRENCY.formatValue(m_dSalesBase);
    }

    /**
     *
     * @return
     */
    public String printSalesTaxes() {
        return Formats.CURRENCY.formatValue(m_dSalesTaxes);
    }

    /**
     *
     * @return
     */
    public String printSalesTotal() {
        return Formats.CURRENCY.formatValue((m_dSalesBase == null || m_dSalesTaxes == null)
                ? null
                : m_dSalesBase + m_dSalesTaxes);
    }

    /**
     *
     * @return
     */
    public List<SalesLine> getSaleLines() {
        return m_lsales;
    }

    /**
     *
     * @return
     */
    public double getCategorySalesRows() {
        return m_iCategorySalesRows;
    }

    /**
     *
     * @return
     */
    public String printCategorySalesRows() {
        return Formats.INT.formatValue(m_iCategorySalesRows);
    }

    /**
     *
     * @return
     */
    public double getCategorySalesTotalUnits() {
        return m_dCategorySalesTotalUnits;
    }

    /**
     *
     * @return
     */
    public String printCategorySalesTotalUnits() {
        return Formats.DOUBLE.formatValue(m_dCategorySalesTotalUnits);
    }

    /**
     *
     * @return
     */
    public double getCategorySalesTotal() {
        return m_dCategorySalesTotal;
    }

    /**
     *
     * @return
     */
    public String printCategorySalesTotal() {
        return Formats.CURRENCY.formatValue(m_dCategorySalesTotal);
    }

    /**
     *
     * @return
     */
    public List<CategorySalesLine> getCategorySalesLines() {
        return m_lcategorysales;
    }
// end

    /**
     *
     * @return
     */
    public double getProductSalesRows() {
        return m_iProductSalesRows;
    }

    /**
     *
     * @return
     */
    public String printProductSalesRows() {
        return Formats.INT.formatValue(m_iProductSalesRows);
    }

    /**
     *
     * @return
     */
    public double getProductSalesTotalUnits() {
        return m_dProductSalesTotalUnits;
    }

    /**
     *
     * @return
     */
    public String printProductSalesTotalUnits() {
        return Formats.DOUBLE.formatValue(m_dProductSalesTotalUnits);
    }

    /**
     *
     * @return
     */
    public double getProductSalesTotal() {
        return m_dProductSalesTotal;
    }

    /**
     *
     * @return
     */
    public String printProductSalesTotal() {
        return Formats.CURRENCY.formatValue(m_dProductSalesTotal);
    }

    /**
     *
     * @return
     */
    public List<ProductSalesLine> getProductSalesLines() {
        return m_lproductsales;
    }
    // end

    /**
     *
     * @return
     */
    public List<RemovedProductLines> getRemovedProductLines() {
        return m_lremovedlines;
    }

    /**
     *
     * @return
     */
    public AbstractTableModel getPaymentsModel() {
        return new AbstractTableModel() {
            @Override
            public String getColumnName(int column) {
                return AppLocal.getIntString(PAYMENTHEADERS[column]);
            }

            @Override
            public int getRowCount() {
                return m_lpayments.size();
            }

            @Override
            public int getColumnCount() {
                return PAYMENTHEADERS.length;
            }

            @Override
            public Object getValueAt(int row, int column) {
                PaymentsLine l = m_lpayments.get(row);
                switch (column) {
                    case 0:
                        return l.getType();
                    case 1:
                        return l.getValue();
                    default:
                        return null;
                }
            }
        };
    }

    /**
     *
     */
    public static class CategorySalesLine implements SerializableRead {

        private String m_CategoryName;
        private Double m_CategoryUnits;
        private Double m_CategorySum;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_CategoryName = dr.getString(1);
            m_CategoryUnits = dr.getDouble(2);
            m_CategorySum = dr.getDouble(3);
        }

        /**
         *
         * @return
         */
        public String printCategoryName() {
            //return m_CategoryName;
            return StringUtils.encodeXML(m_CategoryName);
        }

        /**
         *
         * @return
         */
        public String printCategoryUnits() {
            return Formats.DOUBLE.formatValue(m_CategoryUnits);
        }

        /**
         *
         * @return
         */
        public Double getCategoryUnits() {
            return m_CategoryUnits;
        }

        /**
         *
         * @return
         */
        public String printCategorySum() {
            return Formats.CURRENCY.formatValue(m_CategorySum);
        }

        /**
         *
         * @return
         */
        public Double getCategorySum() {
            return m_CategorySum;
        }
    }

    /**
     *
     */
    public static class RemovedProductLines implements SerializableRead {

        private String m_Name;
        private String m_TicketId;
        private String m_ProductName;
        private Double m_TotalUnits;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_Name = dr.getString(1);
            m_TicketId = dr.getString(2);
            m_ProductName = dr.getString(3);
            m_TotalUnits = dr.getDouble(4);
        }

        /**
         *
         * @return
         */
        public String printWorkerName() {
            return StringUtils.encodeXML(m_Name);
        }

        /**
         *
         * @return
         */
        public String printTicketId() {
            return StringUtils.encodeXML(m_TicketId);
        }

        /**
         *
         * @return
         */
        public String printProductName() {
            return StringUtils.encodeXML(m_ProductName);
        }

        /**
         *
         * @return
         */
        public String printTotalUnits() {
            return Formats.DOUBLE.formatValue(m_TotalUnits);
        }

    }

    /**
     *
     */
    public static class ProductSalesLine implements SerializableRead {

        private String m_ProductName;
        private Double m_ProductUnits;
        private Double m_ProductPrice;
        private Double m_TaxRate;
        private Double m_ProductPriceTax;
        private Double m_ProductPriceNet;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_ProductName = dr.getString(1);
            m_ProductUnits = dr.getDouble(2);
            m_ProductPrice = dr.getDouble(3);
            m_TaxRate = dr.getDouble(4);

            m_ProductPriceTax = m_ProductPrice + m_ProductPrice * m_TaxRate;
            m_ProductPriceNet = m_ProductPrice * m_TaxRate;
        }

        /**
         *
         * @return
         */
        public String printProductName() {
            return StringUtils.encodeXML(m_ProductName);
        }

        /**
         *
         * @return
         */
        public String printProductUnits() {
            return Formats.DOUBLE.formatValue(m_ProductUnits);
        }

        /**
         *
         * @return
         */
        public Double getProductUnits() {
            return m_ProductUnits;
        }

        /**
         *
         * @return
         */
        public String printProductPrice() {
            return Formats.CURRENCY.formatValue(m_ProductPrice);
        }

        /**
         *
         * @return
         */
        public Double getProductPrice() {
            return m_ProductPrice;
        }

        /**
         *
         * @return
         */
        public String printTaxRate() {
            return Formats.PERCENT.formatValue(m_TaxRate);
        }

        /**
         *
         * @return
         */
        public Double getTaxRate() {
            return m_TaxRate;
        }

        /**
         *
         * @return
         */
        public String printProductPriceTax() {
            return Formats.CURRENCY.formatValue(m_ProductPriceTax);
        }

        /**
         *
         * @return
         */
        public String printProductSubValue() {
            return Formats.CURRENCY.formatValue(m_ProductPriceTax * m_ProductUnits);
        }

        /**
         * @return
         */
        public String printProductPriceNet() {
            return Formats.CURRENCY.formatValue(m_ProductPrice * m_ProductUnits);
        }

    }
    // end

    /**
     *
     */
    public static class SalesLine implements SerializableRead {

        private String m_SalesTaxName;
        private Double m_SalesTaxes;
        private Double m_SalesTaxNet;
        private Double m_SalesTaxGross;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_SalesTaxName = dr.getString(1);
            m_SalesTaxes = dr.getDouble(2);
            m_SalesTaxNet = dr.getDouble(3);
            m_SalesTaxGross = dr.getDouble(4);
        }

        /**
         *
         * @return
         */
        public String printTaxName() {
            // return m_SalesTaxName;
            return StringUtils.encodeXML(m_SalesTaxName);
        }

        /**
         *
         * @return
         */
        public String printTaxes() {
            return Formats.CURRENCY.formatValue(m_SalesTaxes);
        }

        /**
         * @return
         */
        public String printTaxNet() {
            return Formats.CURRENCY.formatValue(m_SalesTaxNet);
        }

        /**
         * @return
         */
        public String printTaxGross() {
            return Formats.CURRENCY.formatValue(m_SalesTaxes + m_SalesTaxNet);
        }

        /**
         *
         * @return
         */
        public String getTaxName() {
            return m_SalesTaxName;
        }

        /**
         *
         * @return
         */
        public Double getTaxes() {
            return m_SalesTaxes;
        }

        /**
         * @return
         */
        public Double getTaxNet() {
            return m_SalesTaxNet;
        }

        /**
         * @return
         */
        public Double getTaxGross() {
            return m_SalesTaxGross;
        }

    }

    /**
     *
     * @return
     */
    public AbstractTableModel getSalesModel() {
        return new AbstractTableModel() {
            @Override
            public String getColumnName(int column) {
                return AppLocal.getIntString(SALEHEADERS[column]);
            }

            @Override
            public int getRowCount() {
                return m_lsales.size();
            }

            @Override
            public int getColumnCount() {
                return SALEHEADERS.length;
            }

            @Override
            public Object getValueAt(int row, int column) {
                SalesLine l = m_lsales.get(row);
                switch (column) {
                    case 0:
                        return l.getTaxName();
                    case 1:
                        return l.getTaxes();
                    case 2:
                        return l.getTaxNet();
                    default:
                        return null;
                }
            }
        };
    }

    /**
     *
     */
    public static class PaymentsLine implements SerializableRead {

        private String m_PaymentType;
        private Double m_PaymentValue;
        private String s_PaymentReason;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_PaymentType = dr.getString(1);
            m_PaymentValue = dr.getDouble(2);
            s_PaymentReason = dr.getString(3) == null ? "" : dr.getString(3);
        }

        /**
         *
         * @return
         */
        public String printType() {
            return AppLocal.getIntString("transpayment." + m_PaymentType);
        }

        /**
         *
         * @return
         */
        public String getType() {
            return m_PaymentType;
        }

        /**
         *
         * @return
         */
        public String printValue() {
            return Formats.CURRENCY.formatValue(m_PaymentValue);
        }

        /**
         *
         * @return
         */
        public Double getValue() {
            return m_PaymentValue;
        }

        /**
         *
         * @return
         */
        public String printReason() {
            return StringUtils.encodeXML(s_PaymentReason);
        }

        /**
         *
         * @return
         */
        public String getReason() {
            return s_PaymentReason;
        }
    }
    
    
    /**
     *
     */
    public static class CashCountLine implements SerializableRead {

        private double m_Value;
        private int m_Count;
        private int m_CountRes;

        /**
         *
         * @param dr
         * @throws BasicException
         */
        @Override
        public void readValues(DataRead dr) throws BasicException {
            m_Value = dr.getDouble(1);
            m_Count = dr.getInt(2);
            m_CountRes = dr.getInt(3);
        }

        /**
         *
         * @return
         */
        public String printValue() {
            return Formats.CURRENCY.formatValue(m_Value);
        }

        /**
         *
         * @return
         */
        public double getValue() {
            return m_Value;
        }

        /**
         *
         * @return
         */
        public String printCount() {
            return Formats.INT.formatValue(m_Count);
        }

        /**
         *
         * @return
         */
        public String printCountRes() {
            return Formats.INT.formatValue(m_CountRes);
        }

        /**
         *
         * @return
         */
        public String printCountS() {
            return Formats.INT.formatValue(m_Count + m_CountRes);
        }

        /**
         *
         * @return
         */
        public int getCount() {
            return m_Count;
        }

        /**
         *
         * @return
         */
        public int getCountRes() {
            return m_CountRes;
        }

        /**
         *
         * @return
         */
        public String printCountedValue() {
            return Formats.CURRENCY.formatValue(m_Value * (m_Count + m_CountRes));
        }

        /**
         *
         * @return
         */
        public Double getCountedValue() {
            return m_Value * m_Count;
        }
    }
    
    
}
