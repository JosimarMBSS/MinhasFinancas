package com.minhasfinancas.app.data.entity;

public class TransactionEntity {
    public long id;
    public String title;
    public String description;
    public double amount;
    public String type;
    public long categoryId;
    public String transactionDate;
    public String dueDate;
    public String paidDate;
    public String status;
    public Long recurrenceId;
    public Long accountId;
    public Long destinationAccountId;
    public String accountName;
    public String notes;
    public long createdAt;
    public long updatedAt;
}
