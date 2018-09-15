import { outbox } from "file-transfer";
import firebase from "firebase";
import * as messaging from "messaging";

var data_hr = 0;

messaging.peerSocket.onmessage = function(evt) {
  // Output the message to the console
  data_hr = JSON.stringify(evt.data);
  data_hr = JSON.parse(data_hr);
  data_hr = data_hr.title;
  data_hr = JSON.parse(data_hr);
  console.log("fds " + data_hr);
  sendFile();
}

function sendFile() {
  console.log('firebase', firebase);
  var config = {
    apiKey: "AIzaSyDqbZ0OgHAIUVXJOJTnhL9Q18z8mWvjyVE",
    authDomain: "fitbeat-549ab.firebaseapp.com",
    databaseURL: "https://fitbeat-549ab.firebaseio.com",
    projectId: "fitbeat-549ab",
    storageBucket: "fitbeat-549ab.appspot.com",
    messagingSenderId: "519794952011"
  };

  try {
    firebase.initializeApp(config);
    } catch (err) {
      // we skip the "already exists" message which is
      // not an actual error when we're hot-reloading
      if (!/already exists/.test(err.message)) {
        console.error('Firebase initialization error', err.stack)
      }
    }


  const db = firebase.database();
  const ref = db.ref('bpm');

  db.ref('bpm_test').update({ message: ' guys' })
  db.ref('bpm').update({ message: data_hr })

  ref.on('value', snapshot => {
    const bpm = snapshot.val();

    outbox.enqueue("alphabits.txt", { bpm });
  });

  // console.log("Sending file...");
  // let data = new Uint8Array(26);
  // for (let counter = 0; counter < data.length; counter++) {
  //   data[counter] = "a".charCodeAt(0) + counter;
  // }
  // outbox.enqueue("alphabits.txt", data);
}

//setTimeout(sendFile, 2000);