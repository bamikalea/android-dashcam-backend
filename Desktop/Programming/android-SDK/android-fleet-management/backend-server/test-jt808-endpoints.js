const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';
const DEVICE_ID = 'test-device-001';

async function testJT808Endpoints() {
  console.log('🧪 Testing JT808 Endpoints...\n');

  try {
    // Test 1: Register device
    console.log('1. Testing device registration...');
    const registerResponse = await axios.post(`${BASE_URL}/dashcams/register`, {
      deviceId: DEVICE_ID,
      model: 'Test Device',
      version: '1.0'
    });
    console.log('✅ Device registered successfully\n');

    // Test 2: JT808 Location endpoint
    console.log('2. Testing JT808 location endpoint...');
    const locationData = {
      latitude: 40.7128,
      longitude: -74.0060,
      altitude: 100,
      speed: 60,
      bearing: 90,
      warnBit: 0,
      statusBit: 1,
      timestamp: new Date().toISOString()
    };
    
    const locationResponse = await axios.post(`${BASE_URL}/dashcams/${DEVICE_ID}/jt808/location`, locationData);
    console.log('✅ JT808 location endpoint working');
    console.log('   Response:', locationResponse.data.message, '\n');

    // Test 3: JT808 Alert endpoint - Emergency
    console.log('3. Testing JT808 alert endpoint (Emergency)...');
    const emergencyAlert = {
      alertType: 'emergency',
      warnBit: 1, // Bit 0: Emergency alarm
      statusBit: 0,
      latitude: 40.7128,
      longitude: -74.0060,
      altitude: 100,
      speed: 0,
      description: 'Emergency alert test',
      timestamp: new Date().toISOString()
    };
    
    const emergencyResponse = await axios.post(`${BASE_URL}/dashcams/${DEVICE_ID}/jt808/alert`, emergencyAlert);
    console.log('✅ JT808 emergency alert endpoint working');
    console.log('   Alert ID:', emergencyResponse.data.alertId, '\n');

    // Test 4: JT808 Alert endpoint - Overspeed
    console.log('4. Testing JT808 alert endpoint (Overspeed)...');
    const overspeedAlert = {
      alertType: 'overspeed',
      warnBit: 2, // Bit 1: Overspeed alarm
      statusBit: 0,
      latitude: 40.7128,
      longitude: -74.0060,
      altitude: 100,
      speed: 120,
      description: 'Overspeed alert test',
      timestamp: new Date().toISOString()
    };
    
    const overspeedResponse = await axios.post(`${BASE_URL}/dashcams/${DEVICE_ID}/jt808/alert`, overspeedAlert);
    console.log('✅ JT808 overspeed alert endpoint working');
    console.log('   Alert ID:', overspeedResponse.data.alertId, '\n');

    // Test 5: Get JT808 data
    console.log('5. Testing JT808 data retrieval...');
    const jt808DataResponse = await axios.get(`${BASE_URL}/dashcams/${DEVICE_ID}/jt808`);
    console.log('✅ JT808 data retrieval working');
    console.log('   JT808 Enabled:', jt808DataResponse.data.jt808Enabled);
    console.log('   Data entries:', jt808DataResponse.data.data.length, '\n');

    // Test 6: Get device info
    console.log('6. Testing device info retrieval...');
    const deviceResponse = await axios.get(`${BASE_URL}/dashcams/${DEVICE_ID}`);
    console.log('✅ Device info retrieval working');
    console.log('   Device status:', deviceResponse.data.status);
    console.log('   JT808 Enabled:', deviceResponse.data.jt808Enabled);
    console.log('   Location:', deviceResponse.data.location ? 'Available' : 'Not available', '\n');

    console.log('🎉 All JT808 endpoint tests passed!');
    console.log('\n📋 Summary:');
    console.log('   - Device registration: ✅');
    console.log('   - JT808 location updates: ✅');
    console.log('   - JT808 emergency alerts: ✅');
    console.log('   - JT808 overspeed alerts: ✅');
    console.log('   - JT808 data retrieval: ✅');
    console.log('   - Device info retrieval: ✅');

  } catch (error) {
    console.error('❌ Test failed:', error.response ? error.response.data : error.message);
    console.error('Status:', error.response ? error.response.status : 'Unknown');
  }
}

// Run tests
testJT808Endpoints(); 