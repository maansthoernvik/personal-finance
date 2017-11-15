package com.sarah.expensecontrol;

/**
 * Klass för att representera en Expense i applikationen, klassfälten ska överensstämma med
 * kontraktet för databasen, ExpenseControlContract.
 */

public class Expense {
    private long id;
    private String name;
    private int amount;
    private long timestamp;
    private boolean recurring;
    private String category;
    private String pictureUri;

    /**
     * Tom constructor
     */

    public Expense() {}

    /**
     * Standard constructor
     * @param id id
     * @param name name
     * @param amount amount
     * @param timestamp timestamp
     * @param recurring recurring?
     * @param category category
     * @param pictureUri pictureUri
     */

    public Expense(long id, String name, int amount, long timestamp, boolean recurring,
                   String category, String pictureUri) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.timestamp = timestamp;
        this.recurring = recurring;
        this.category = category;
        this.pictureUri = pictureUri;
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

    /**
     * Getter för recurring
     * @return recurring
     */

    public boolean isRecurring() {
        return recurring;
    }

    /**
     * Setter för recurring
     * @param recurring att sätta
     */

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    /**
     * Getter för category
     * @return category
     */

    public String getCategory() {
        return category;
    }

    /**
     * Setter för category
     * @param category att sätta
     */

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Getter för pictureUri
     * @return pictureUri
     */

    public String getPictureUri() {
        return pictureUri;
    }

    /**
     * Setter för pictureUri
     * @param pictureUri att sätta
     */

    public void setPictureUri(String pictureUri) {
        this.pictureUri = pictureUri;
    }
}
