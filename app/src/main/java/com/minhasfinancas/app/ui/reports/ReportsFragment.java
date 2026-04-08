package com.minhasfinancas.app.ui.reports;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.minhasfinancas.app.BuildConfig;
import com.minhasfinancas.app.data.model.ReportFilter;
import com.minhasfinancas.app.data.model.ReportSummary;
import com.minhasfinancas.app.data.model.TransactionListItem;
import com.minhasfinancas.app.data.repository.FinanceRepository;
import com.minhasfinancas.app.databinding.FragmentReportsBinding;
import com.minhasfinancas.app.util.DateUtils;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportTableAdapter adapter;
    private String currentTypeFilter = "ALL";
    private String currentStatusFilter = "ALL";
    private final List<TransactionListItem> currentItems = new ArrayList<>();
    private ReportSummary currentSummary = new ReportSummary();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        adapter = new ReportTableAdapter();

        binding.recyclerReport.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReport.setAdapter(adapter);

        LocalDate today = LocalDate.now();
        binding.inputStartDate.setText(DateUtils.isoToDisplay(today.withDayOfMonth(1).toString()));
        binding.inputEndDate.setText(DateUtils.isoToDisplay(today.withDayOfMonth(today.lengthOfMonth()).toString()));

        binding.inputStartDate.setOnClickListener(v -> pickDate(true));
        binding.inputEndDate.setOnClickListener(v -> pickDate(false));
        binding.buttonTypeFilter.setOnClickListener(v -> showTypeMenu());
        binding.buttonStatusFilter.setOnClickListener(v -> showStatusMenu());
        binding.buttonGeneratePdf.setOnClickListener(v -> sharePdf());

        updateFilterLabels();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReport();
    }

    private void pickDate(boolean start) {
        String value = start ? textOf(binding.inputStartDate.getText()) : textOf(binding.inputEndDate.getText());
        LocalDate initial = DateUtils.parseDisplayOrIsoOrToday(value);
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    if (start) {
                        binding.inputStartDate.setText(DateUtils.isoToDisplay(selected.toString()));
                    } else {
                        binding.inputEndDate.setText(DateUtils.isoToDisplay(selected.toString()));
                    }
                    loadReport();
                },
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        );
        dialog.show();
    }

    private void showTypeMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.buttonTypeFilter);
        popupMenu.getMenu().add(0, 1, 0, "Todos");
        popupMenu.getMenu().add(0, 2, 1, "Receitas");
        popupMenu.getMenu().add(0, 3, 2, "Despesas");
        popupMenu.setOnMenuItemClickListener(this::onTypeSelected);
        popupMenu.show();
    }

    private boolean onTypeSelected(MenuItem item) {
        if (item.getItemId() == 2) {
            currentTypeFilter = "INCOME";
        } else if (item.getItemId() == 3) {
            currentTypeFilter = "EXPENSE";
        } else {
            currentTypeFilter = "ALL";
        }
        updateFilterLabels();
        loadReport();
        return true;
    }

    private void showStatusMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.buttonStatusFilter);
        popupMenu.getMenu().add(0, 1, 0, "Todos");
        popupMenu.getMenu().add(0, 2, 1, "Pago");
        popupMenu.getMenu().add(0, 3, 2, "A Pagar");
        popupMenu.getMenu().add(0, 4, 3, "Recebido");
        popupMenu.getMenu().add(0, 5, 4, "A Receber");
        popupMenu.setOnMenuItemClickListener(this::onStatusSelected);
        popupMenu.show();
    }

    private boolean onStatusSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 2) {
            currentStatusFilter = "EXPENSE_PAID";
        } else if (id == 3) {
            currentStatusFilter = "EXPENSE_PENDING";
        } else if (id == 4) {
            currentStatusFilter = "INCOME_PAID";
        } else if (id == 5) {
            currentStatusFilter = "INCOME_PENDING";
        } else {
            currentStatusFilter = "ALL";
        }
        updateFilterLabels();
        loadReport();
        return true;
    }

    private void updateFilterLabels() {
        if (binding == null) {
            return;
        }
        binding.textTypeFilter.setText("Tipo: " + typeLabel());
        binding.textStatusFilter.setText("Situação: " + statusLabel());
    }

    private void loadReport() {
        if (binding == null) {
            return;
        }

        String startDate = DateUtils.displayToIso(textOf(binding.inputStartDate.getText()));
        String endDate = DateUtils.displayToIso(textOf(binding.inputEndDate.getText()));
        if (startDate.isEmpty() || endDate.isEmpty()) {
            return;
        }

        ReportFilter filter = new ReportFilter();
        filter.startDate = startDate;
        filter.endDate = endDate;
        filter.type = currentTypeFilter;
        filter.status = currentStatusFilter;
        filter.categoryName = null;

        FinanceRepository repo = FinanceRepository.getInstance(requireContext());
        repo.generateRecurrencesForPeriod(filter.startDate, filter.endDate, () -> {
            repo.getTransactionsFiltered(filter, items -> {
                List<TransactionListItem> visibleItems = new ArrayList<>();
                for (TransactionListItem item : items) {
                    if ("EXPENSE".equals(item.type) || "INCOME".equals(item.type)) {
                        visibleItems.add(item);
                    }
                }
                currentItems.clear();
                currentItems.addAll(visibleItems);
                adapter.submitList(visibleItems);
                binding.textEmpty.setVisibility(visibleItems.isEmpty() ? View.VISIBLE : View.GONE);
                binding.textCount.setText(visibleItems.size() + " item(ns)");
            });
            repo.getReportSummary(filter, summary -> {
                currentSummary = summary;
                binding.textIncomeTotal.setText(DateUtils.currency(summary.income));
                binding.textExpenseTotal.setText(DateUtils.currency(summary.expense));
                binding.textBalanceTotal.setText("Saldo: " + DateUtils.currency(summary.getBalance()));
                binding.textBalanceTotal.setTextColor(Color.parseColor(summary.getBalance() >= 0 ? "#198754" : "#D9485F"));
            });
        });
    }

    private void sharePdf() {
        if (getContext() == null) {
            return;
        }
        if (currentItems.isEmpty()) {
            Toast.makeText(requireContext(), "Não há dados para compartilhar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File dir = new File(requireContext().getCacheDir(), "reports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File pdfFile = new File(dir, "relatorio-minhas-financas.pdf");
            createPdf(pdfFile);

            Uri uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Relatório Minhas Finanças");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartilhar PDF"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível gerar o PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createPdf(File outputFile) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setTextSize(12f);

        Paint textPaint = new Paint();
        textPaint.setTextSize(11f);

        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1f);

        int pageWidth = 595;
        int pageHeight = 842;
        int pageNumber = 1;
        int y = 40;

        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create());
        android.graphics.Canvas canvas = page.getCanvas();

        y = drawHeader(canvas, titlePaint, subtitlePaint, textPaint, linePaint, y);

        for (TransactionListItem item : currentItems) {
            if (y > pageHeight - 60) {
                document.finishPage(page);
                pageNumber++;
                page = document.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create());
                canvas = page.getCanvas();
                y = drawHeader(canvas, titlePaint, subtitlePaint, textPaint, linePaint, 40);
            }
            String description = item.description != null && !item.description.trim().isEmpty() ? item.description : item.title;
            canvas.drawText(trim(description, 34), 24, y, textPaint);
            canvas.drawText(("INCOME".equals(item.type) ? "+ " : "- ") + DateUtils.currency(item.amount), 320, y, textPaint);
            canvas.drawText(reportStatus(item), 470, y, textPaint);
            y += 20;
        }

        document.finishPage(page);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            document.writeTo(outputStream);
        } finally {
            document.close();
        }
    }

    private int drawHeader(android.graphics.Canvas canvas, Paint titlePaint, Paint subtitlePaint, Paint textPaint, Paint linePaint, int startY) {
        int y = startY;
        canvas.drawText("Relatório - Minhas Finanças", 24, y, titlePaint);
        y += 20;
        canvas.drawText("Período: " + textOf(binding.inputStartDate.getText()) + " até " + textOf(binding.inputEndDate.getText()), 24, y, subtitlePaint);
        y += 18;
        canvas.drawText("Tipo: " + typeLabel() + " | Situação: " + statusLabel(), 24, y, subtitlePaint);
        y += 24;
        canvas.drawText("Resumo de totais", 24, y, subtitlePaint);
        y += 18;
        canvas.drawText("Receitas: " + DateUtils.currency(currentSummary.income), 24, y, textPaint);
        canvas.drawText("Despesas: " + DateUtils.currency(currentSummary.expense), 220, y, textPaint);
        canvas.drawText("Saldo: " + DateUtils.currency(currentSummary.getBalance()), 420, y, textPaint);
        y += 24;
        canvas.drawText("Descrição", 24, y, textPaint);
        canvas.drawText("Valor", 320, y, textPaint);
        canvas.drawText("Situação", 470, y, textPaint);
        y += 6;
        canvas.drawLine(24, y, 560, y, linePaint);
        return y + 16;
    }

    private String trim(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }

    private String typeLabel() {
        if ("INCOME".equals(currentTypeFilter)) {
            return "Receitas";
        }
        if ("EXPENSE".equals(currentTypeFilter)) {
            return "Despesas";
        }
        return "Todos";
    }

    private String statusLabel() {
        switch (currentStatusFilter) {
            case "EXPENSE_PAID": return "Pago";
            case "EXPENSE_PENDING": return "A Pagar";
            case "INCOME_PAID": return "Recebido";
            case "INCOME_PENDING": return "A Receber";
            default: return "Todos";
        }
    }

    private String reportStatus(TransactionListItem item) {
        if ("INCOME".equals(item.type)) {
            return "PAID".equals(item.status) ? "Recebido" : "A receber";
        }
        return "PAID".equals(item.status) ? "Pago" : "A pagar";
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
