package com.minhasfinancas.app.ui.about;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.minhasfinancas.app.BuildConfig;
import com.minhasfinancas.app.databinding.FragmentAboutBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), this::exportToUri);
        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::importFromUri);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.textVersion.setText("Versão " + BuildConfig.VERSION_NAME);
        binding.buttonExportData.setOnClickListener(v -> exportLauncher.launch("minhas-financas-backup.db"));
        binding.buttonImportData.setOnClickListener(v -> importLauncher.launch(new String[]{"*/*"}));
        return binding.getRoot();
    }

    private void exportToUri(Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }
        try {
            File dbFile = requireContext().getDatabasePath("minhas_financas.db");
            if (!dbFile.exists()) {
                Toast.makeText(requireContext(), "Nenhum banco encontrado para exportar.", Toast.LENGTH_SHORT).show();
                return;
            }
            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                copy(in, out);
            }
            Toast.makeText(requireContext(), "Dados exportados com sucesso.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível exportar os dados.", Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromUri(Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }
        try {
            File dbFile = requireContext().getDatabasePath("minhas_financas.db");
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dbFile, false)) {
                copy(in, out);
            }
            Toast.makeText(requireContext(), "Dados importados. Feche e abra o app para recarregar.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível importar os dados.", Toast.LENGTH_SHORT).show();
        }
    }

    private void copy(InputStream in, OutputStream out) throws Exception {
        if (in == null || out == null) {
            throw new IllegalStateException("Fluxo inválido");
        }
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
