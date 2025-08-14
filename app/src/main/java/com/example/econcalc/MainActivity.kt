package com.example.econcalc

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)

        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }

        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(InvestmentFragment())
            navigationView.setCheckedItem(R.id.nav_investment)
        }
    }

    private fun selectDrawerItem(menuItem: MenuItem) {
        val fragment: Fragment = when (menuItem.itemId) {
            R.id.nav_investment -> InvestmentFragment()
            R.id.nav_financial_planning -> FinancialPlanningFragment()
            R.id.nav_income -> IncomeFragment()
            R.id.nav_taxes -> TaxesFragment()
            R.id.nav_business -> BusinessFragment()
            R.id.nav_advanced -> AdvancedFragment()
            else -> InvestmentFragment()
        }

        replaceFragment(fragment)

        drawer.closeDrawer(GravityCompat.START)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}