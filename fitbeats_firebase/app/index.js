import document from "document";
import { HeartRateSensor } from "heart-rate";
import * as messaging from "messaging";

let hrmData = document.getElementById("hrm-data");

let hrm = new HeartRateSensor();

hrm.start();

messaging.peerSocket.onopen = function() {
  // Ready to send messages
}

// Listen for the onerror event
messaging.peerSocket.onerror = function(err) {
  // Handle any errors
  console.log("Connection error: " + err.code + " - " + err.message);
}

function refreshData() {
  let data = {
    hrm: {
      heartRate: hrm.heartRate ? hrm.heartRate : 0
    }
  };

  hrmData.text = JSON.stringify(data.hrm);
  
  //console.log(hrmData.text);
  
  var data = {
    title: JSON.stringify(data.hrm),
    isTest: true,
    records: [1, 2, 3, 4]
  }

  if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
    // Send the data to peer as a message
    messaging.peerSocket.send(data);
  }
  
}

refreshData();
setInterval(refreshData, 1000);
