package com.ti3042.airmonitor.feature.monitoring

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ti3042.airmonitor.feature.monitoring.databinding.FragmentHistoryBinding
import com.ti3042.airmonitor.feature.monitoring.databinding.FragmentAnalyticsBinding
import com.ti3042.airmonitor.feature.monitoring.databinding.FragmentReportsBinding
import kotlinx.coroutines.launch

/**
 * 📊 Fragment de Historial de Datos
 */
class HistoryFragment : Fragment() {
    
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringViewModel by activityViewModels()
    private lateinit var historyAdapter: HistoryAdapter
    
    private val tag = "HistoryFragment"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        
        Log.d(tag, "✅ HistoryFragment configurado")
    }
    
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.recyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Datos simulados para el historial
            val sampleData = listOf(
                "📊 14:30 - 850 PPM - 22.5°C - 65%",
                "📊 14:25 - 820 PPM - 22.3°C - 64%",
                "📊 14:20 - 890 PPM - 22.7°C - 66%"
            )
            historyAdapter.submitList(sampleData)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 🧪 Fragment de Análisis Estadístico
 */
class AnalyticsFragment : Fragment() {
    
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringViewModel by activityViewModels()
    private val tag = "AnalyticsFragment"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        
        Log.d(tag, "✅ AnalyticsFragment configurado")
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Datos simulados para analytics
            updateAnalytics()
        }
    }
    
    private fun updateAnalytics() {
        with(binding) {
            tvWeeklyAverage.text = "875.5 PPM"
            tvWeeklyTrend.text = "↗️ +5.2%"
            tvCriticalHours.text = "12h"
            tvBestHour.text = "06:00"
            tvWorstHour.text = "14:30"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 📋 Fragment de Reportes
 */
class ReportsFragment : Fragment() {
    
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringViewModel by activityViewModels()
    private val tag = "ReportsFragment"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        
        Log.d(tag, "✅ ReportsFragment configurado")
    }
    
    private fun setupClickListeners() {
        with(binding) {
            btnExportPdf.setOnClickListener {
                viewModel.exportReport("PDF")
            }
            
            btnExportExcel.setOnClickListener {
                viewModel.exportReport("EXCEL")
            }
            
            btnExportCsv.setOnClickListener {
                viewModel.exportReport("CSV")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}