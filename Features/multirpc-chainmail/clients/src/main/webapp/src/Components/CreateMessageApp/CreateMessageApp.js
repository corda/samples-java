import React, {useRef, useState, useEffect} from 'react';
import {useHistory, withRouter} from 'react-router-dom';
import axios from 'axios';
import {BACKEND_URL, SEND_EMAIL} from "../CONSTANTS";

// interval timer for 1hz refresh
function useInterval(callback, delay) {
    const savedCallback = useRef();

    // Remember the latest callback.
    useEffect(() => {
        savedCallback.current = callback;
    }, [callback]);

    // Set up the interval.
    useEffect(() => {
        function tick() {
            savedCallback.current();
        }
        if (delay !== null) {
            let id = setInterval(tick, delay);
            return () => clearInterval(id);
        }
    }, [delay]);
}
function CreateMessageApp(props) {

    const [response, setResponse] = useState(null);
    const [messages, setMessages] = useState(null);
    const [counter, setCounter] = useState(0);

    useInterval(() => {
        setCounter(counter + 1);
    }, 1000);

    console.log("INIT")

    const data = {
        requestingNode: "Alice"
    }
        //https://docs.corda.net/docs/corda-os/4.8/clientrpc.html#overview}

    useEffect(() => {
        // axios.get(
        axios.post(
            BACKEND_URL + '/messages',
            data,
            {headers: {'Content-Type': 'application/json'}}
        ).then(res => {
            // const messages = res.data.messages;
            const messages = res.data.messages;
            // const messages = res.data.messages;

            // if (gameId !== null) {
            console.log("Hello, can you hear me?")
            console.log(messages)
            if (messages !== null) {
                const secret_msg = "CONSOLE LOGGED FROM /messages";
                console.log(secret_msg);

                // if (!SEND_EMAIL) {
                //     alert(secret_msg);
                // }

                setResponse(res);
            }
        });
    }, [counter])

    return (
        <p>Hello? {messages}{response}</p>
    )
}
export default withRouter(CreateMessageApp);
