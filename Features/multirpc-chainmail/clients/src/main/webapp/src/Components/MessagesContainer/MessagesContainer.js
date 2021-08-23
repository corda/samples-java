import React, {useState, useEffect} from "react";
import "./MessagesContainer.css";
import {withRouter} from "react-router-dom";
import {BACKEND_URL} from "../CONSTANTS";
import chain_mail_logo from "../img/ChainMailLogo1.png";

function MessageInputBox(props) {
    const [message, setMessage] = useState("");

    return (
        <div className="send-form">
            <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Enter ChainMail Message"
                onKeyPress={(e) => {
                    if (e.key === 'Enter') {
                        props.onSubmit(message);
                        setMessage("");
                    }
                }
                }
            />
            <div
                className="send-button"
                onClick={() => {
                    props.onSubmit(message);
                    setMessage("");
                }}
            >
                🕊
            </div>
        </div>
    );
}

function Message(props) {
    return (
        <div className="message-item">
            <table>
                <td className="td-from">
                    <p className="message-from">Node: {props.message.sender} |</p>
                </td>
                <td className="td-content">
                    <p className="message-content">{props.message.message}</p>
                </td>
            </table>
        </div>
    );
}

function MessagesApp(props) {
    const [messages, setMessages] = useState([]);
    console.log("INIT");

    useEffect(() => {
        const i = setInterval(() => {
            fetch(BACKEND_URL + "/messages", {
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({requestingNode: "Alice"}),
                method: "POST",
            })
                .then((res) => res.json())
                .then((res) => {
                    const messages = res;
                    console.log(messages);
                    if (messages !== null) {
                        setMessages(messages);
                    }
                });
        }, 2000);

        return () => {
            clearInterval(i);
        };
    }, []);

    function sendMessage(message) {
        fetch(BACKEND_URL + "/messages/sendmessage", {
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({message: message}),
            method: "POST",
        });
    }

    return (
        <>
            <div className="App">
                <h1>
                    <img className="logo" src={chain_mail_logo}/>
                </h1>
                <main className="message-container">
                    {messages.map((message) => {
                        return <Message message={message}/>;
                    })}
                </main>
                <MessageInputBox onSubmit={sendMessage}/>
            </div>
        </>
    );
}

export default withRouter(MessagesApp);
