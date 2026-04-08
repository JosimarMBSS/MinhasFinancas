package com.minhasfinancas.app.data.model;

public class ReportSummary {
    public int count;
    public double income;
    public double expense;

    public double getBalance() {
        return income - expense;
    }
}
