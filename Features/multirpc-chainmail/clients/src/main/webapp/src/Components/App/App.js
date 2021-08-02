import React from 'react';

import {
  BrowserRouter as Router,
  Switch,
  Route
} from "react-router-dom";

import './App.css';

// import CreateSantaGame from '../CreateSantaGame/CreateSantaGame.js';
// import CheckSantaGame from '../CheckSantaGame/CheckSantaGame';
// import SantaGameCreated from '../SantaGameCreated/SantaGameCreated';
// import SantaCheckSent from '../SantaCheckSent/SantaCheckSent';
import CreateMessageApp from "../CreateMessageApp/CreateMessageApp";

function App(props) {

  return (
    <Router>
      <div>
        <Switch>
          <Route exact path="/" component={CreateMessageApp}/>
          {/*<Route exact path="/messages" component={CreateMessageApp}/>*/}
          {/*<Route exact path="/" component={CreateSantaGame}/>*/}
          {/*<Route path="/created" component={SantaGameCreated}/>*/}
          {/*<Route path="/create" component={CreateSantaGame}/>*/}
          {/*<Route path="/checked" component={SantaCheckSent}/>*/}
          {/*<Route path="/check" component={CheckSantaGame}/>          */}
        </Switch>
      </div>
    </Router>
  );
}

export default App;