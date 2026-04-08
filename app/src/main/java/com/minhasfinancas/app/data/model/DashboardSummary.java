package com.minhasfinancas.app.data.model;

public class DashboardSummary {
    public double income;
    public double expense;

    public DashboardSummary(double income, double expense) {
        this.income = income;
        this.expense = expense;
    }

    public double getBalance() {
        return income - expense;
    }
}
