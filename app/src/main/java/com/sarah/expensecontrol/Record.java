package com.sarah.expensecontrol;

/**
 * Klass för att representera records i applikationen. Ska överensstämma med kontraktet mot
 * databasen, ExpenseControlContract, dock så är _ID överflöding för denna klass.
 */

public class Record {
    private String type;
    private int amount;
    private long timestamp;

    /**
     * Tom constructor
     */

    public Record() {}

    /**
     * Standard constructor
     * @param type type
     * @param amount amount
     * @param timestamp timestamp
     */

    public Record(String type, int amount, long timestamp) {
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    /**
     * Getter för type
     * @return type
     */

    public String getType() {
        return type;
    }

    /**
     * Setter för type
     * @param type att sätta
     */

    public void setType(String type) {
        this.type = type;
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
     * Getter för timestamp
     * @return timestamp
     */

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Setter för timestamp
     * @param timestamp att sätta
     */

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
