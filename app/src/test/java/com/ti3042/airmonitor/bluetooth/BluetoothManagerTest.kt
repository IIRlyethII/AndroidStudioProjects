package com.ti3042.airmonitor.bluetooth

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 🧪 Tests para BluetoothManager
 * 
 * Verifica el funcionamiento del manager que controla
 * los servicios Mock y Real de Bluetooth
 */
class BluetoothManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var bluetoothManager: BluetoothManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        
        // Limpiar instancia singleton
        BluetoothManager.clearInstance()
    }

    @After
    fun tearDown() {
        BluetoothManager.clearInstance()
    }

    @Test
    fun `test initialization with simulation mode`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        
        // When
        manager.initialize(mockContext, useSimulation = true)
        
        // Then
        assertNotNull(manager.getCurrentService())
        assertTrue(manager.isSimulationMode())
    }

    @Test
    fun `test initialization with real mode`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        
        // When
        manager.initialize(mockContext, useSimulation = false)
        
        // Then
        assertNotNull(manager.getCurrentService())
        assertFalse(manager.isSimulationMode())
    }

    @Test
    fun `test switch between modes`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = true)
        assertTrue(manager.isSimulationMode())
        
        // When
        manager.switchToMode(mockContext, useSimulation = false)
        
        // Then
        assertFalse(manager.isSimulationMode())
    }

    @Test
    fun `test connection callback delegation`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = true)
        
        // When
        manager.setConnectionCallback(mockCallback)
        manager.connect()
        
        // Then
        verify(timeout = 1000) { mockCallback.onConnectionStateChanged(true) }
    }

    @Test
    fun `test data callback delegation`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = true)
        manager.setConnectionCallback(mockCallback)
        
        // When
        manager.connect()
        
        // Then
        verify(timeout = 3000, atLeast = 1) { 
            mockCallback.onDataReceived(any()) 
        }
    }

    @Test
    fun `test command sending`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = true)
        manager.connect()
        
        // When
        val result = manager.sendCommand("TEST")
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test device info in real mode`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = false)
        
        // When
        val deviceInfo = manager.getDeviceInfo()
        
        // Then
        // En modo real sin dispositivo conectado debería ser null
        assertNull(deviceInfo)
    }

    @Test
    fun `test singleton behavior`() = runTest {
        // Given
        val manager1 = BluetoothManager.getInstance()
        val manager2 = BluetoothManager.getInstance()
        
        // Then
        assertSame(manager1, manager2)
    }

    @Test
    fun `test cleanup on destroy`() = runTest {
        // Given
        val manager = BluetoothManager.getInstance()
        manager.initialize(mockContext, useSimulation = true)
        
        // When
        manager.destroy()
        
        // Then
        assertNull(manager.getCurrentService())
    }
}