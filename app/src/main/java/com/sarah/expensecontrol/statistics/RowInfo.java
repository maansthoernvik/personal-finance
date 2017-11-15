package com.sarah.expensecontrol.statistics;

import java.util.ArrayList;

/**
 * Klass som ska hålla information om en rad i statistiköversikten, id, datum och totaler för just
 * den raden.
 */

public class RowInfo {
    private int rowId;
    private long rowDate;
    private ArrayList<Integer> totals;

    /**
     * Standard constructor, totals måste fyllas i senare.
     * @param rowId id för raden
     * @param rowDate datum som raden håller
     */

    public RowInfo(int rowId, long rowDate) {
        this.rowId = rowId;
        this.rowDate = rowDate;
        this.totals = new ArrayList<>();
    }

    /**
     * Getter för rowId.
     * @return rowId
     */

    public int getRowId() {
        return rowId;
    }

    /**
     * Setter för rowId.
     * @param rowId att skriva över med
     */

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    /**
     * Getter för rowDate.
     * @return rowDate
     */

    public long getRowDate() {
        return rowDate;
    }

    /**
     * Setter för rowDate
     * @param rowDate att skriva över med
     */

    public void setRowDate(long rowDate) {
        this.rowDate = rowDate;
    }

    /**
     * Hämta totalen i den specificerade indexet.
     * @param index att hämta från
     * @return totalen för det givna indexet
     */

    public int getTotal(int index) {
        return totals.get(index);
    }

    /**
     * Lägger till totals till this.totals.
     * @param totals att lägga till instansens totals
     */

    public void addToTotals(Integer... totals) {
        for (Integer total : totals) {
            this.totals.add(total);
        }
    }

    /**
     * Getter för totals.
     * @return totals
     */

    public ArrayList<Integer> getTotals() {
        return totals;
    }

    /**
     * Setter för totals.
     * @param totals att skriva över med
     */

    public void setTotals(ArrayList<Integer> totals) {
        this.totals = totals;
    }
}
