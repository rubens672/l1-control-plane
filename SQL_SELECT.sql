--seleziona quantità per device
SELECT deviceId,
count(deviceId) count,
min(DATE(timestamp, "Europe/Rome")) min,
max(DATE(timestamp, "Europe/Rome")) max  
FROM `mosqhealthagent.telemetry.frigate_stats`
group by deviceId; 