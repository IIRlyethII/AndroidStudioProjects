package com.ti3042.airmonitor.bluetooth

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 🧪 Tests para RealBluetoothService
 * 
 * Verifica el funcionamiento del servicio real de Bluetooth
 * que se conecta al ESP32 físico
 */
class RealBluetoothServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var realBluetoothService: RealBluetoothService

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        realBluetoothService = RealBluetoothService(mockContext)
    }

    @After
    fun tearDown() {
        realBluetoothService.destroy()
    }

    @Test
    fun `test initial state`() {
        // Then
        assertFalse(realBluetoothService.isConnected())
        assertEquals(0, realBluetoothService.getConnectionAttempts())
        assertNull(realBluetoothService.getDeviceInfo())
        assertEquals(0, realBluetoothService.getQueuedCommandsCount())
    }

    @Test
    fun `test callback setup`() {
        // When
        realBluetoothService.setConnectionCallback(mockCallback)
        
        // Then - No exception thrown
        assertTrue(true)
    }

    @Test
    fun `test command queuing without connection`() = runTest {
        // Given
        realBluetoothService.setConnectionCallback(mockCallback)
        
        // When
        val result1 = realBluetoothService.sendCommand("GET_STATUS")
        val result2 = realBluetoothService.sendCommand("SET_LED_ON")
        
        // Then
        assertFalse(result1) // Should fail without connection
        assertFalse(result2) // Should fail without connection
    }

    @Test
    fun `test clear command queue`() {
        // Given
        realBluetoothService.sendCommand("TEST1")
        realBluetoothService.sendCommand("TEST2")
        
        // When
        realBluetoothService.clearCommandQueue()
        
        // Then
        assertEquals(0, realBluetoothService.getQueuedCommandsCount())
    }

    @Test
    fun `test destroy cleanup`() {
        // Given
        realBluetoothService.setConnectionCallback(mockCallback)
        realBluetoothService.sendCommand("TEST")
        
        // When
        realBluetoothService.destroy()
        
        // Then
        assertFalse(realBluetoothService.isConnected())
        assertEquals(0, realBluetoothService.getQueuedCommandsCount())
    }

    @Test
    fun `test getCurrentData returns null initially`() {
        // When
        val data = realBluetoothService.getCurrentData()
        
        // Then
        assertNull(data)
    }

    @Test
    fun `test connection without bluetooth adapter`() = runTest {
        // Given
        realBluetoothService.setConnectionCallback(mockCallback)
        
        // When
        realBluetoothService.connect()
        
        // Then
        verify(timeout = 1000) { 
            mockCallback.onError(match { it.contains("Bluetooth") }) 
        }
    }

    @Test
    fun `test connection with specific address`() = runTest {
        // Given
        val testAddress = "00:11:22:33:44:55"
        realBluetoothService.setConnectionCallback(mockCallback)
        
        // When
        realBluetoothService.connect(testAddress)
        
        // Then
        verify(timeout = 1000) { 
            mockCallback.onError(any()) // Will fail without real Bluetooth
        }
    }

    @Test
    fun `test disconnect when not connected`() {
        // When
        realBluetoothService.disconnect()
        
        // Then - No exception thrown
        assertFalse(realBluetoothService.isConnected())
    }
}