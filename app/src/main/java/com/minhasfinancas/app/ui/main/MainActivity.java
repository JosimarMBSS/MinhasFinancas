package com.minhasfinancas.app.ui.main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.minhasfinancas.app.R;
import com.minhasfinancas.app.databinding.ActivityMainBinding;
import com.minhasfinancas.app.ui.about.AboutFragment;
import com.minhasfinancas.app.ui.reports.ReportsFragment;
import com.minhasfinancas.app.ui.settings.SettingsInfoActivity;
import com.minhasfinancas.app.ui.transaction.TransactionFormActivity;
import com.minhasfinancas.app.ui.transaction.TransactionsFragment;
import com.minhasfinancas.app.util.DatabaseSeeder;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private int currentMenuId = R.id.nav_transactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.top_app_bar_on));
        Drawable overflowIcon = binding.toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setTint(ContextCompat.getColor(this, R.color.top_app_bar_on));
        }

        DatabaseSeeder.seed(this);

        binding.bottomNav.setOnItemSelectedListener(this);
        binding.fabAdd.setOnClickListener(v -> startActivity(new Intent(this, TransactionFormActivity.class)));

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_transactions);
        } else {
            updateFabVisibility();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addItem = menu.findItem(R.id.action_add);
        if (addItem != null) {
            addItem.setVisible(false);
        }
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null) {
            settingsItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, SettingsInfoActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        currentMenuId = item.getItemId();

        Fragment fragment;
        CharSequence title;

        if (item.getItemId() == R.id.nav_reports) {
            fragment = new ReportsFragment();
            title = getString(R.string.menu_reports);
        } else if (item.getItemId() == R.id.nav_about) {
            fragment = new AboutFragment();
            title = getString(R.string.menu_about);
        } else {
            fragment = new TransactionsFragment();
            title = getString(R.string.menu_transactions);
        }

        binding.toolbar.setTitle(title);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        updateFabVisibility();
        invalidateOptionsMenu();
        return true;
    }

    private void updateFabVisibility() {
        if (binding == null) {
            return;
        }
        binding.fabAdd.setVisibility(currentMenuId == R.id.nav_transactions ? View.VISIBLE : View.GONE);
    }
}
