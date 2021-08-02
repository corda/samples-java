const PROTOCOL = 'http://'
const PORT = '10056' // note that this is the port of the santaServer! (from clients build.gradle: '--server.port=10056')
const HOSTNAME = 'localhost'
// const SEND_EMAIL = false // set this to true if you've configured sendgrid to send emails on the backend
const SEND_EMAIL = true// set this to true if you've configured sendgrid to send emails on the backend


const BACKEND_URL = PROTOCOL + HOSTNAME + ':' + PORT;

export { BACKEND_URL, SEND_EMAIL }; 
