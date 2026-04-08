package com.minhasfinancas.app.data.entity;

public class RecurrenceEntity {
    public long id;
    public String title;
    public String type;
    public double amount;
    public long categoryId;
    public String frequency;
    public int intervalValue;
    public String startDate;
    public String endDate;
    public Integer maxOccurrences;
    public boolean isActive;
    public String accountName;
    public String notes;
    public long createdAt;
}
