import React, { useRef, useState, useEffect } from "react";
import "./MessagesContainer.css";
import { useHistory, withRouter } from "react-router-dom";
import { BACKEND_URL, SEND_EMAIL } from "../CONSTANTS";

function MessageInputBox(props) {
  const [message, setMessage] = useState("");

  return (
    <div className="send-form">
      <input
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Enter ChainMail Message"
      />
      <div
        className="sendButton"
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
      <p className="message-from">From: {props.message.sender}</p>
      <p>{props.message.message}</p>
    </div>
  );
}

function MessagesApp(props) {
  const [messages, setMessages] = useState([]);
  console.log("INIT");

useEffect(() => {
    const i = setInterval(() => {
      fetch(BACKEND_URL + "/messages", {
        headers: { "Content-Type": "application/json" },
          body: "hi",
        // body: JSON.stringify(data),
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
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: message }),
      method: "POST",
    });
  }

  return (
    <>
      <div className="App">
        <h1>⛓💌</h1>
        <main>
          {messages.map((message) => {
            return <Message message={message} />;
          })}
          <MessageInputBox onSubmit={sendMessage}></MessageInputBox>
        </main>
      </div>
    </>
  );
}

export default withRouter(MessagesApp);
