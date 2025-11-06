package com.ti3042.airmonitor.feature.monitoring

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.ti3042.airmonitor.feature.monitoring.databinding.ActivityMonitoringBinding

/**
 * 📊 Sistema de Monitoreo y Análisis
 * 
 * Responsabilidades:
 * - Análisis histórico de datos
 * - Gráficos en tiempo real
 * - Reportes de calidad del aire
 * - Análisis de tendencias
 * - Exportación de reportes
 */
class MonitoringActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMonitoringBinding
    private val viewModel: MonitoringViewModel by viewModels {
        MonitoringViewModelFactory()
    }
    
    private val tag = "MonitoringActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "📊 Creating MonitoringActivity")
        
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPager()
        
        // Cargar datos iniciales
        viewModel.loadMonitoringData()
        
        Log.d(tag, "✅ MonitoringActivity configurado correctamente")
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "📊 Monitoreo y Análisis"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupViewPager() {
        val adapter = MonitoringPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "📈 Gráficos"
                1 -> "📊 Historial" 
                2 -> "🧪 Análisis"
                3 -> "📋 Reportes"
                else -> "Tab $position"
            }
            tab.setIcon(when (position) {
                0 -> android.R.drawable.ic_menu_gallery
                1 -> android.R.drawable.ic_menu_recent_history
                2 -> android.R.drawable.ic_menu_info_details
                3 -> android.R.drawable.ic_menu_save
                else -> android.R.drawable.ic_menu_info_details
            })
        }.attach()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    /**
     * 📱 Adapter para las pestañas del monitoreo
     */
    private class MonitoringPagerAdapter(activity: MonitoringActivity) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = 4
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChartsFragment()
                1 -> HistoryFragment()
                2 -> AnalyticsFragment() 
                3 -> ReportsFragment()
                else -> ChartsFragment()
            }
        }
    }
}