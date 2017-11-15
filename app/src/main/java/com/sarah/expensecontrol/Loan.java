package com.sarah.expensecontrol;

import java.io.Serializable;

/**
 * Klass för att representera lån i applikationen. Ska överensstämma med kontraktet mot databasen,
 * ExpenseControlContract.
 */

public class Loan implements Serializable {
    private long id;
    private String name;
    private int amount;
    private double interest;
    private int amortization;

    /**
     * Tom contstructor
     */

    public Loan() {}

    /**
     *
     * @param amount amount
     * @param interest interest
     * @param amortization amortization
     */

    public Loan(int amount, double interest, int amortization) {
        this.amount = amount;
        this.interest = interest;
        this.amortization = amortization;
    }

    /**
     *
     * @param id id
     * @param amount amount
     * @param interest interest
     * @param amortization amortization
     */

    public Loan(long id, int amount, double interest, int amortization) {
        this.id = id;
        this.amount = amount;
        this.interest = interest;
        this.amortization = amortization;
    }

    /**
     * Standard Constructor
     * @param id id
     * @param name name
     * @param amount amount
     * @param interest interest
     * @param amortization amortization
     */

    public Loan(long id, String name, int amount, double interest, int amortization) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.interest = interest;
        this.amortization = amortization;
    }

    /**
     * Getter för id
     * @return id
     */

    public long getId() {
        return id;
    }

    /**
     * Setter för id
     * @param id att sätta
     */

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Getter för name
     * @return name
     */

    public String getName() {
        return name;
    }

    /**
     * Setter för name
     * @param name att sätta
     */

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter för amount
     * @return amount
     */

    public int getAmount() {
        return amount;
    }

    /**
     * Setter för amount
     * @param amount att sätta
     */

    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Getter för interest
     * @return interest
     */

    public double getInterest() {
        return interest;
    }

    /**
     * Setter för interest
     * @param interest att sätta
     */

    public void setInterest(double interest) {
        this.interest = interest;
    }

    /**
     * Getter för amortization
     * @return amortization
     */

    public int getAmortization() {
        return amortization;
    }

    /**
     * Setter för amortization
     * @param amortization att sätta
     */

    public void setAmortization(int amortization) {
        this.amortization = amortization;
    }
}
