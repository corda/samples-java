const PROTOCOL = 'http://'
const PORT = '10052' // note that this is the port of Alice's server! (from clients build.gradle: '--server.port=10052')
const HOSTNAME = 'localhost'

const BACKEND_URL = PROTOCOL + HOSTNAME + ':' + PORT;

export {BACKEND_URL};
