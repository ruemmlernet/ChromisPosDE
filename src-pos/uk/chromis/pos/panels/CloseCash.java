/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.chromis.pos.panels;

/**
 *
 * @author Medicus
 */
public class CloseCash {
    
    private int[] m_count = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int[] m_count_res = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public double[] m_cash = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500};
    private double m_sum;
    private double m_cashTotal = 0.0;
    
    public double m_dMoneyStart;
    public double m_dCashSales;

    public CloseCash() {

    }
    
    public void setCash(double value, int count) {
        for (int i=0; i<15; i++) {
            if (value == m_cash[i]) {
                m_count[i] = count;
            }
        }
    }
    
    public void setCashRes(double value, int count) {
        for (int i=0; i<15; i++) {
            if (value == m_cash[i]) {
                m_count_res[i] = count;
            }
        }
    }
    
    public int getCash(double value) {
        int count = 0;
        for (int i=0; i<15; i++) {
            if (value == m_cash[i]) {
                count = m_count[i];
            }
        }
        return count;
    }
    
    public int getCashRes(double value) {
        int count = 0;
        for (int i=0; i<15; i++) {
            if (value == m_cash[i]) {
                count = m_count_res[i];
            }
        }
        return count;
    }
    
    public void setCashTotal(double value) {
        m_cashTotal = value;
    }
    
    public void clearCash() {
        for (int i=0; i<15; i++) {
            m_count[i] = 0;
            m_count_res[i] = 0;
        }
    }
    
    public double getSummary() {
        m_sum = m_cashTotal;
        for (int i=0; i<15; i++) {
            m_sum = m_sum + ((m_count[i] + m_count_res[i]) * m_cash[i]);
        }
        return m_sum;
    }
    
    // Soll-End-Bestand
    public double getTarget() {
        return m_dMoneyStart + m_dCashSales;
    }
    
    public double getDifference() {
        // auf zwei Nachkommastellen runden
        return Math.round((getSummary() - getTarget()) * 100.0) / 100.0;
    }
    
}
