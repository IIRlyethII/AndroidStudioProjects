package com.ti3042.airmonitor.feature.monitoring

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ti3042.airmonitor.feature.monitoring.databinding.FragmentChartsBinding

/**
 * 📊 Fragment de Gráficos en Tiempo Real
 */
class ChartsFragment : Fragment() {
    
    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MonitoringViewModel by activityViewModels()
    private val tag = "ChartsFragment"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        
        Log.d(tag, "✅ ChartsFragment configurado")
    }
    
    private fun setupClickListeners() {
        with(binding) {
            btn1Hour.setOnClickListener { 
                Log.d(tag, "🕐 Cambiando a vista 1 hora")
            }
            btn6Hours.setOnClickListener { 
                Log.d(tag, "🕕 Cambiando a vista 6 horas")
            }
            btn24Hours.setOnClickListener { 
                Log.d(tag, "🕐 Cambiando a vista 24 horas")
            }
            btnWeek.setOnClickListener { 
                Log.d(tag, "📅 Cambiando a vista semana")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}