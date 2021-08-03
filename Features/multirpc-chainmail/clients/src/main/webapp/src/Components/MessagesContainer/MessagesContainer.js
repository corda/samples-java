import React, { useRef, useState, useEffect } from "react";
import { useHistory, withRouter } from "react-router-dom";
import axios from "axios";
import { BACKEND_URL, SEND_EMAIL } from "../CONSTANTS";

function MessageBox(props) {
  const [message, setMessage] = useState("");

  return (
    <div>
      <input
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Enter ChainMail Message"
      />
      {/*<button type="submit" disabled={!message}>*/}
      {/*<button type="submit">*/}
      {/*  🕊*/}
      {/*</button>*/}
      {/*<button onClick={()=> {setMessage(e.)}}>🕊</button>*/}
      <div
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
    <div>
      <p>From: {props.message.sender}</p>
      <p>{props.message.message}</p>
    </div>
  );
}

function MessagesContainer(props) {
  const [response, setResponse] = useState(null);
  const [messages, setMessages] = useState([]);
  const [counter, setCounter] = useState(0);

  console.log("INIT");

  const data = {
    requestingNode: "Alice",
  };
  //https://docs.corda.net/docs/corda-os/4.8/clientrpc.html#overview}

  useEffect(() => {
    const i = setInterval(() => {
      fetch(BACKEND_URL + "/messages", {
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
        method: "POST",
      })
        .then((res) => res.json())
        .then((res) => {
          const messages = res;
          console.log("Hello, can you hear me?");
          console.log(messages);
          if (messages !== null) {
            console.log("CONSOLE LOGGED FROM /messages");

            setMessages(messages);
          }
        });
    }, 2000);

    return () => {
      clearInterval(i);
    };
  }, []);

  function sendMessage(message) {
    console.log("SENDING MESSAGE");
    fetch(BACKEND_URL + "/messages/sendmessage", {
      headers: { "Content-Type": "application/json" },
  body: JSON.stringify({"message": message}),
      method: "POST",
    });
  }

  return (
    <>
      <main>
        {messages.map((message) => {
          return <Message message={message} />;
        })}
        <MessageBox onSubmit={sendMessage}></MessageBox>
      </main>
      {/*<form onSubmit={sendMessage}>*/}
      {/*    <input value={formValue} onChange={(e) => setFormValue(e.target.value)}/>*/}
      {/*    <button type="submit" disabled={!formValue}>🕊</button>*/}
      {/*</form>*/}
    </>
  );
}

export default withRouter(MessagesContainer);
