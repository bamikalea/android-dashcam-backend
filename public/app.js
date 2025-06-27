// Global variables
let dashcams = new Map();
let events = [];
let commands = [];
let mediaStream = null;
let audioStream = null;
let currentTab = 'images';

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    loadInitialData();
    setInterval(pollUpdates, 30000); // Poll every 30 seconds
    initializeMediaFeatures();
});

// Initialize media features
function initializeMediaFeatures() {
    // Set up volume control
    const volumeControl = document.getElementById('volumeControl');
    if (volumeControl) {
        volumeControl.addEventListener('input', function() {
            const video = document.getElementById('liveVideo');
            if (video) {
                video.volume = this.value / 100;
            }
        });
    }
    
    // Set up audio controls
    const enableMic = document.getElementById('enableMic');
    const enableSpeaker = document.getElementById('enableSpeaker');
    
    if (enableMic) {
        enableMic.addEventListener('change', function() {
            if (audioStream) {
                audioStream.getAudioTracks().forEach(track => {
                    track.enabled = this.checked;
                });
            }
        });
    }
    
    if (enableSpeaker) {
        enableSpeaker.addEventListener('change', function() {
            const video = document.getElementById('liveVideo');
            if (video) {
                video.muted = !this.checked;
            }
        });
    }
}

// Load initial data from server
async function loadInitialData() {
    try {
        // Load dashcams
        const dashcamsResponse = await fetch('/api/dashcams');
        const dashcamsData = await dashcamsResponse.json();
        dashcamsData.forEach(dashcam => {
            dashcams.set(dashcam.deviceId, dashcam);
        });
        updateDashcamList();
        
        // Load events
        const eventsResponse = await fetch('/api/events?limit=50');
        const eventsData = await eventsResponse.json();
        events = eventsData;
        updateEventHistory();
        
        // Load commands
        const commandsResponse = await fetch('/api/commands?limit=50');
        const commandsData = await commandsResponse.json();
        commands = commandsData;
        
        // Load media
        loadMediaData();
        
        updateStats();
    } catch (error) {
        console.error('Error loading initial data:', error);
        addLogEntry('Error loading initial data', 'event-system');
    }
}

// Load media data
async function loadMediaData() {
    try {
        const mediaResponse = await fetch('/api/media');
        const mediaData = await mediaResponse.json();
        
        if (mediaData.images) {
            updateImageGallery(mediaData.images);
        }
        if (mediaData.videos) {
            updateVideoList(mediaData.videos);
        }
        if (mediaData.audio) {
            updateAudioList(mediaData.audio);
        }
    } catch (error) {
        console.error('Error loading media data:', error);
    }
}

// Live Video Streaming Functions
function startLiveStream() {
    const deviceId = document.getElementById('selectedDashcam').value;
    if (!deviceId) {
        alert('Please select a dashcam first');
        return;
    }
    
    try {
        // Request live stream from dashcam
        sendCommand('start_live_stream');
        
        // Update UI
        document.getElementById('streamStatus').textContent = 'Connecting...';
        document.getElementById('streamUrl').textContent = `rtmp://${window.location.hostname}:1935/live/${deviceId}`;
        
        // Set up video element for streaming
        const video = document.getElementById('liveVideo');
        video.src = `/stream/${deviceId}`;
        video.play();
        
        addLogEntry(`Started live stream for ${deviceId}`, 'event-system');
    } catch (error) {
        console.error('Error starting live stream:', error);
        addLogEntry(`Error starting live stream: ${error.message}`, 'event-system');
    }
}

function stopLiveStream() {
    const deviceId = document.getElementById('selectedDashcam').value;
    if (!deviceId) {
        return;
    }
    
    try {
        // Stop live stream
        sendCommand('stop_live_stream');
        
        // Update UI
        document.getElementById('streamStatus').textContent = 'Offline';
        document.getElementById('streamUrl').textContent = 'Not connected';
        
        // Stop video
        const video = document.getElementById('liveVideo');
        video.pause();
        video.src = '';
        
        addLogEntry(`Stopped live stream for ${deviceId}`, 'event-system');
    } catch (error) {
        console.error('Error stopping live stream:', error);
        addLogEntry(`Error stopping live stream: ${error.message}`, 'event-system');
    }
}

// Audio Communication Functions
async function startAudio() {
    const deviceId = document.getElementById('selectedDashcam').value;
    if (!deviceId) {
        alert('Please select a dashcam first');
        return;
    }
    
    try {
        // Request microphone access
        audioStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        
        // Start audio communication
        sendCommand('start_audio');
        
        // Update UI
        document.getElementById('audioStatus').textContent = 'Audio: On';
        document.getElementById('enableMic').checked = true;
        
        addLogEntry(`Started audio communication for ${deviceId}`, 'event-system');
    } catch (error) {
        console.error('Error starting audio:', error);
        addLogEntry(`Error starting audio: ${error.message}`, 'event-system');
    }
}

function stopAudio() {
    const deviceId = document.getElementById('selectedDashcam').value;
    if (!deviceId) {
        return;
    }
    
    try {
        // Stop audio communication
        sendCommand('stop_audio');
        
        // Stop audio stream
        if (audioStream) {
            audioStream.getTracks().forEach(track => track.stop());
            audioStream = null;
        }
        
        // Update UI
        document.getElementById('audioStatus').textContent = 'Audio: Off';
        document.getElementById('enableMic').checked = false;
        
        addLogEntry(`Stopped audio communication for ${deviceId}`, 'event-system');
    } catch (error) {
        console.error('Error stopping audio:', error);
        addLogEntry(`Error stopping audio: ${error.message}`, 'event-system');
    }
}

// Media Management Functions
function showTab(tabName) {
    // Hide all tab contents
    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));
    
    // Remove active class from all tab buttons
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => button.classList.remove('active'));
    
    // Show selected tab
    document.getElementById(tabName + 'Tab').classList.add('active');
    event.target.classList.add('active');
    
    currentTab = tabName;
}

function refreshImages() {
    loadMediaData();
    addLogEntry('Refreshed image gallery', 'event-system');
}

function refreshVideos() {
    loadMediaData();
    addLogEntry('Refreshed video list', 'event-system');
}

function refreshRecordings() {
    loadMediaData();
    addLogEntry('Refreshed audio recordings', 'event-system');
}

function updateImageGallery(images) {
    const gallery = document.getElementById('imageGallery');
    
    if (!images || images.length === 0) {
        gallery.innerHTML = '<div class="text-gray-500 text-center py-8">No images captured yet</div>';
        return;
    }
    
    let html = '';
    images.forEach(image => {
        html += `
            <div class="image-item cursor-pointer" onclick="openImageModal('${image.url}', '${image.filename}')">
                <img src="${image.thumbnail || image.url}" alt="${image.filename}" loading="lazy">
                <div class="p-3">
                    <p class="text-sm font-medium text-gray-900">${image.filename}</p>
                    <p class="text-xs text-gray-500">${new Date(image.timestamp).toLocaleString()}</p>
                </div>
            </div>
        `;
    });
    
    gallery.innerHTML = html;
}

function updateVideoList(videos) {
    const videoList = document.getElementById('videoList');
    
    if (!videos || videos.length === 0) {
        videoList.innerHTML = '<div class="text-gray-500 text-center py-8">No videos recorded yet</div>';
        return;
    }
    
    let html = '';
    videos.forEach(video => {
        html += `
            <div class="media-item">
                <div class="flex-shrink-0 mr-3">
                    <i class="fas fa-video text-blue-600 text-xl"></i>
                </div>
                <div class="flex-1">
                    <p class="font-medium text-gray-900">${video.filename}</p>
                    <p class="text-sm text-gray-500">${video.duration} • ${new Date(video.timestamp).toLocaleString()}</p>
                </div>
                <div class="flex space-x-2">
                    <button onclick="playVideo('${video.url}')" class="btn-primary text-sm">
                        <i class="fas fa-play mr-1"></i>Play
                    </button>
                    <button onclick="downloadVideo('${video.url}', '${video.filename}')" class="btn-success text-sm">
                        <i class="fas fa-download mr-1"></i>Download
                    </button>
                </div>
            </div>
        `;
    });
    
    videoList.innerHTML = html;
}

function updateAudioList(audioFiles) {
    const audioList = document.getElementById('audioList');
    
    if (!audioFiles || audioFiles.length === 0) {
        audioList.innerHTML = '<div class="text-gray-500 text-center py-8">No audio recordings yet</div>';
        return;
    }
    
    let html = '';
    audioFiles.forEach(audio => {
        html += `
            <div class="media-item">
                <div class="flex-shrink-0 mr-3">
                    <i class="fas fa-microphone text-green-600 text-xl"></i>
                </div>
                <div class="flex-1">
                    <p class="font-medium text-gray-900">${audio.filename}</p>
                    <p class="text-sm text-gray-500">${audio.duration} • ${new Date(audio.timestamp).toLocaleString()}</p>
                </div>
                <div class="flex space-x-2">
                    <button onclick="playAudio('${audio.url}')" class="btn-primary text-sm">
                        <i class="fas fa-play mr-1"></i>Play
                    </button>
                    <button onclick="downloadAudio('${audio.url}', '${audio.filename}')" class="btn-success text-sm">
                        <i class="fas fa-download mr-1"></i>Download
                    </button>
                </div>
            </div>
        `;
    });
    
    audioList.innerHTML = html;
}

function addImageToGallery(imageData) {
    const gallery = document.getElementById('imageGallery');
    const noImagesDiv = gallery.querySelector('.text-gray-500');
    
    if (noImagesDiv) {
        noImagesDiv.remove();
    }
    
    const imageItem = document.createElement('div');
    imageItem.className = 'image-item cursor-pointer';
    imageItem.onclick = () => openImageModal(imageData.url, imageData.filename);
    
    imageItem.innerHTML = `
        <img src="${imageData.thumbnail || imageData.url}" alt="${imageData.filename}" loading="lazy">
        <div class="p-3">
            <p class="text-sm font-medium text-gray-900">${imageData.filename}</p>
            <p class="text-xs text-gray-500">${new Date(imageData.timestamp).toLocaleString()}</p>
        </div>
    `;
    
    gallery.insertBefore(imageItem, gallery.firstChild);
}

// Image Modal Functions
function openImageModal(imageUrl, filename) {
    document.getElementById('modalImage').src = imageUrl;
    document.getElementById('modalImage').alt = filename;
    document.getElementById('imageModal').classList.remove('hidden');
}

function closeImageModal() {
    document.getElementById('imageModal').classList.add('hidden');
}

function downloadImage() {
    const imageUrl = document.getElementById('modalImage').src;
    const filename = document.getElementById('modalImage').alt;
    
    const link = document.createElement('a');
    link.href = imageUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// Media Playback Functions
function playVideo(videoUrl) {
    // Create a modal or use existing video player
    const video = document.getElementById('liveVideo');
    video.src = videoUrl;
    video.play();
    addLogEntry(`Playing video: ${videoUrl}`, 'event-system');
}

function playAudio(audioUrl) {
    const audio = new Audio(audioUrl);
    audio.play();
    addLogEntry(`Playing audio: ${audioUrl}`, 'event-system');
}

function downloadVideo(videoUrl, filename) {
    const link = document.createElement('a');
    link.href = videoUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    addLogEntry(`Downloaded video: ${filename}`, 'event-system');
}

function downloadAudio(audioUrl, filename) {
    const link = document.createElement('a');
    link.href = audioUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    addLogEntry(`Downloaded audio: ${filename}`, 'event-system');
}

// Handle media updates from server
function handleMediaUpdate(data) {
    switch (data.type) {
        case 'image':
            addImageToGallery(data);
            break;
        case 'video':
            refreshVideos();
            break;
        case 'audio':
            refreshRecordings();
            break;
    }
}

// Update server status indicator
function updateServerStatus(online) {
    const statusElement = document.getElementById('serverStatus');
    if (online) {
        statusElement.innerHTML = '<span class="status-online"><i class="fas fa-circle"></i> Server Online</span>';
    } else {
        statusElement.innerHTML = '<span class="status-offline"><i class="fas fa-circle"></i> Server Offline</span>';
    }
}

// Update dashcam status
function updateDashcamStatus(data) {
    const dashcam = dashcams.get(data.deviceId);
    if (dashcam) {
        dashcam.status = data.status;
        dashcam.lastSeen = data.timestamp;
    } else {
        // New dashcam
        dashcams.set(data.deviceId, {
            deviceId: data.deviceId,
            status: data.status,
            lastSeen: data.timestamp,
            location: null,
            events: []
        });
    }
    
    updateDashcamList();
    updateStats();
}

// Update dashcam location
function updateDashcamLocation(data) {
    const dashcam = dashcams.get(data.deviceId);
    if (dashcam) {
        dashcam.location = data.location;
        dashcam.lastSeen = data.timestamp;
        updateDashcamList();
    }
}

// Update dashcam list display
function updateDashcamList() {
    const container = document.getElementById('dashcamList');
    const select = document.getElementById('selectedDashcam');
    
    if (dashcams.size === 0) {
        container.innerHTML = '<p class="text-gray-500 text-center py-8">No dashcams connected</p>';
        select.innerHTML = '<option value="">Choose a dashcam...</option>';
        return;
    }
    
    // Update dashcam list
    let html = '';
    let selectHtml = '<option value="">Choose a dashcam...</option>';
    
    dashcams.forEach((dashcam, deviceId) => {
        const statusClass = dashcam.status === 'online' ? 'status-online' : 'status-offline';
        const lastSeen = new Date(dashcam.lastSeen).toLocaleTimeString();
        
        html += `
            <div class="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                <div>
                    <div class="flex items-center">
                        <span class="${statusClass} mr-2"><i class="fas fa-circle"></i></span>
                        <span class="font-medium">${deviceId}</span>
                    </div>
                    <div class="text-sm text-gray-500">
                        Last seen: ${lastSeen}
                    </div>
                    ${dashcam.location ? `
                        <div class="text-sm text-gray-500">
                            Location: ${dashcam.location.latitude.toFixed(4)}, ${dashcam.location.longitude.toFixed(4)}
                        </div>
                    ` : ''}
                </div>
                <div class="text-right">
                    <span class="text-xs px-2 py-1 rounded-full ${dashcam.status === 'online' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                        ${dashcam.status}
                    </span>
                </div>
            </div>
        `;
        
        selectHtml += `<option value="${deviceId}">${deviceId} (${dashcam.status})</option>`;
    });
    
    container.innerHTML = html;
    select.innerHTML = selectHtml;
}

// Send command to dashcam
async function sendCommand(command) {
    const deviceId = document.getElementById('selectedDashcam').value;
    
    if (!deviceId) {
        alert('Please select a dashcam first');
        return;
    }
    
    try {
        const response = await fetch(`/api/dashcams/${deviceId}/command`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                command: command,
                parameters: {}
            })
        });
        
        const result = await response.json();
        
        if (response.ok) {
            addLogEntry(`Command sent: ${command} to ${deviceId}`, 'event-system');
            commands.push({
                commandId: result.commandId,
                deviceId: deviceId,
                command: command,
                timestamp: new Date(),
                status: 'sent'
            });
            updateStats();
        } else {
            addLogEntry(`Command failed: ${result.error}`, 'event-system');
        }
    } catch (error) {
        console.error('Error sending command:', error);
        addLogEntry(`Error sending command: ${error.message}`, 'event-system');
    }
}

// Add event to history
function addEvent(data) {
    events.unshift({
        deviceId: data.deviceId,
        eventType: data.eventType,
        eventData: data.eventData,
        timestamp: data.timestamp
    });
    
    // Keep only last 100 events
    if (events.length > 100) {
        events = events.slice(0, 100);
    }
    
    updateEventHistory();
}

// Add command response
function addCommandResponse(data) {
    const command = commands.find(cmd => cmd.commandId === data.commandId);
    if (command) {
        command.response = data.response;
        command.success = data.success;
        command.status = data.success ? 'completed' : 'failed';
    }
}

// Update event history table
function updateEventHistory() {
    const tbody = document.getElementById('eventTableBody');
    
    if (events.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="px-6 py-4 text-center text-gray-500">No events recorded</td></tr>';
        return;
    }
    
    let html = '';
    events.forEach(event => {
        const time = new Date(event.timestamp).toLocaleString();
        const eventClass = getEventClass(event.eventType);
        
        html += `
            <tr class="${eventClass}">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${time}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${event.deviceId}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${event.eventType}</td>
                <td class="px-6 py-4 text-sm text-gray-500">${JSON.stringify(event.eventData)}</td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

// Get CSS class for event type
function getEventClass(eventType) {
    switch (eventType) {
        case 'motion_detected':
            return 'event-motion';
        case 'collision_detected':
            return 'event-collision';
        case 'location_update':
            return 'event-location';
        default:
            return 'event-system';
    }
}

// Add log entry
function addLogEntry(message, type = 'event-system') {
    const container = document.getElementById('logContainer');
    const time = new Date().toLocaleTimeString();
    
    const logEntry = document.createElement('div');
    logEntry.className = `log-entry ${type}`;
    logEntry.innerHTML = `[${time}] ${message}`;
    
    container.insertBefore(logEntry, container.firstChild);
    
    // Keep only last 50 log entries
    while (container.children.length > 50) {
        container.removeChild(container.lastChild);
    }
}

// Update statistics
function updateStats() {
    const totalDashcams = dashcams.size;
    const onlineDashcams = Array.from(dashcams.values()).filter(d => d.status === 'online').length;
    const eventsToday = events.filter(e => {
        const eventDate = new Date(e.timestamp).toDateString();
        const today = new Date().toDateString();
        return eventDate === today;
    }).length;
    const commandsSent = commands.filter(c => c.status === 'sent' || c.status === 'completed').length;
    
    document.getElementById('totalDashcams').textContent = totalDashcams;
    document.getElementById('onlineDashcams').textContent = onlineDashcams;
    document.getElementById('eventsToday').textContent = eventsToday;
    document.getElementById('commandsSent').textContent = commandsSent;
    document.getElementById('dashcamCount').textContent = onlineDashcams;
}

// Utility function to format time
function formatTime(date) {
    return new Date(date).toLocaleString();
}

// Export functions for global access
window.sendCommand = sendCommand;
window.startLiveStream = startLiveStream;
window.stopLiveStream = stopLiveStream;
window.startAudio = startAudio;
window.stopAudio = stopAudio;
window.showTab = showTab;
window.refreshImages = refreshImages;
window.refreshVideos = refreshVideos;
window.refreshRecordings = refreshRecordings;
window.openImageModal = openImageModal;
window.closeImageModal = closeImageModal;
window.downloadImage = downloadImage;
window.playVideo = playVideo;
window.playAudio = playAudio;
window.downloadVideo = downloadVideo;
window.downloadAudio = downloadAudio;

async function pollUpdates() {
    try {
        // Poll dashcams
        const dashcamsResponse = await fetch('/api/dashcams');
        const dashcamsData = await dashcamsResponse.json();
        dashcams.clear();
        dashcamsData.forEach(dashcam => {
            dashcams.set(dashcam.deviceId, dashcam);
        });
        updateDashcamList();

        // Poll events
        const eventsResponse = await fetch('/api/events?limit=50');
        const eventsData = await eventsResponse.json();
        events = eventsData;
        updateEventHistory();

        // Poll commands
        const commandsResponse = await fetch('/api/commands?limit=50');
        const commandsData = await commandsResponse.json();
        commands = commandsData;
        updateStats();

        // Optionally poll media updates if needed
        // loadMediaData();

        // Update server status
        updateServerStatus(true);
    } catch (error) {
        updateServerStatus(false);
        addLogEntry('Polling error: ' + error.message, 'event-system');
    }
}

// --- Location Command Integration ---
function requestLocation(deviceId) {
    if (!deviceId) {
        alert('Please select a dashcam first');
        return;
    }
    if (window.socket) {
        window.socket.emit('get_location', { deviceId });
    } else {
        alert('Socket not connected');
    }
}

// Listen for location_response from server
if (typeof io !== 'undefined') {
    window.socket = io();
    window.socket.on('location_response', function(data) {
        if (data && data.deviceId && data.location) {
            const el = document.getElementById('location-result');
            if (el) {
                el.innerText = `Device ${data.deviceId} location: Lat ${data.location.latitude}, Lng ${data.location.longitude}`;
            } else {
                alert(`Device ${data.deviceId} location: Lat ${data.location.latitude}, Lng ${data.location.longitude}`);
            }
        }
    });
} 