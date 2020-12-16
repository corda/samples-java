import 'bootstrap/dist/css/bootstrap.min.css';
import React from "react";
import axios from 'axios';

import '../css/App.css';
import retrieveToken from './retrieveToken';
import Container from 'react-bootstrap/Container'
import { Row } from 'react-bootstrap';
import { Col } from 'react-bootstrap';
import { BrowserRouter as Router, Switch, Route, Link} from "react-router-dom";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      seEmail:'',
      reEmail:'',
      message:'',
      callback:''};

      this.handleChange = this.handleChange.bind(this);
      this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(event) {
      this.setState({ [event.target.name]: event.target.value });
    }

    handleSubmit(event) {
      console.log('A token was submitted: \nSender: '+ this.state.seEmail+'\nReceiver: '+this.state.reEmail+'\nMessage'+this.state.message);
      const tokenInfo = JSON.stringify({
        "senderEmail": this.state.seEmail,
        "recipientEmail": this.state.reEmail,
        "secretMessage": this.state.message
      });
      console.log(tokenInfo);

      axios.post('http://localhost:10050/createToken', tokenInfo, {"headers": {"content-type": "application/json",}})
      .then(
        response => {
          console.log(response.data)
          this.setState({
            callback: response.data
          });
          //  alert('You have just created a token for your friend! He will be able to collect the secret message using the Token Id and Storage Node information.'+response.data);
        }
      );

      event.preventDefault();
    }

    render(){
      return (
        <div className="App">
          <Container>
            <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="200" className="App-logo"/>
            <Row>
              <Col md="auto"><h1>CorDapp Token-To-Friend</h1></Col>
              <Col md={{offset:2}}><button className="btn btn-primary disabled" >Create a Token</button> <Link to="/retrieveToken" ><button className="btn btn-primary" >Retrieve a Token</button></Link></Col>
            </Row>
            <p>In this example CorDapp, you will be able to issue your friend a token that carries a secret message.
              You will receive the information of the tokenID and where the token is stored. Your friend would need these
              information to retrieve the token and reveal the secret message.</p>
            <p>If you are retrieving a token, please click the Retrieve Token button above to continue. ↗️ </p>
          </Container>
          <Container>
            <br />
            <h4>Send a token to a friend: </h4>
            <form onSubmit={this.handleSubmit}>
              <div className="form-group">
                <label>
                  Your Email Address
                </label>
                <input
                  type="text"  className="form-control" name="seEmail" onChange={this.handleChange} />
                <small
                  id="emailHelp"
                  className="form-text text-muted">
                  We'll never share your email with anyone else.
                </small>
              </div>
              <div className="form-group">
                <label>
                  Recipient's Email Address
                </label>
                <input
                  type="text"  className="form-control" name="reEmail" onChange={this.handleChange} />
              </div>
              <div>
                <p>You can attache a secret message to this token and deliver to your friend. </p>
                <input
                  type="text"  className="form-control" name="message" onChange={this.handleChange} />
              </div>
              <br />
              <div className="row">
                <div className="col-10" />
                <div className="col">
                  <button type="submit" className="btn btn-primary">Submit</button>
                </div>
              </div>
            </form>
          </Container>
          <Container>
            {this.state.callback != '' &&
              <div>
                <h4>Here is your Token Information: </h4>
                <p>{this.state.callback}</p>
              </div>
            }
          </Container>





        </div>
      );
    }
  }

  export default App;
