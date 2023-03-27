import 'bootstrap/dist/css/bootstrap.min.css';
import React from "react";
import axios from 'axios';

import Container from 'react-bootstrap/Container'
import { Row } from 'react-bootstrap';
import { Col } from 'react-bootstrap';
import { BrowserRouter as Router, Switch, Route, Link} from "react-router-dom";

class retrieveToken extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tokenId:'',
      reEmail:'',
      peerNodes:[{
        label: "C=CN,L=Beijing,O=AsiaEast",
        storageNode: "C=CN,L=Beijing,O=AsiaEast",
      },
      {
        label: "C=US,L=New York,O=USEast3",
        storageNodeue: "C=US,L=New York,O=USEast3",
      },
      {
        label: "C=US,L=San Diego,O=USWest1",
        storageNode: "C=US,L=San Diego,O=USWest1",
      },],
      endPoint:'10050',
      storageNode:'BankA',
      callback:''};

      this.handleChange = this.handleChange.bind(this);
      this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentDidMount(){
      axios.get('http://localhost:10050/peers', {"headers": {"content-type": "application/json",}})
       .then(
         response => {
          //  console.log(response.data)
           const peerList = response.data.peers
           console.log(peerList)
           const nodeList = []
           peerList.map((node)=>(
            nodeList.push({label:node,storageNode:node})
           ))
           console.log(nodeList)
           console.log("--")
           console.log(this.state.peerNodes)
           this.setState({
             peerNodes:nodeList
          });
          }
         );

    }

    handleChange(event) {
      this.setState({ [event.target.name]: event.target.value });
    }

    handleSubmit(event) {
      // console.log('A token was submitted: \nSender: '+ this.state.seEmail+'\nReceiver: '+this.state.reEmail+'\nMessage'+this.state.message);
      console.log(this.state.storageNode);
      if (this.state.storageNode == 'C=CN,L=Beijing,O=AsiaEast'){
        console.log('10051')
        this.setState({endPoint:'10051'}, this.queryToken);
      }else if(this.state.storageNode == 'C=US,L=New York,O=USEast3'){
        console.log('10052')
        this.setState({endPoint : '10052'}, this.queryToken);
      }else if(this.state.storageNode == 'C=US,L=San Diego,O=USWest1'){
        console.log('10053')
        this.setState({endPoint : '10053'}, this.queryToken);
      }
      event.preventDefault();
    }

    queryToken = () => {
      const queryInfo = JSON.stringify({ 
        "tokenId": this.state.tokenId,
        "recipientEmail": this.state.reEmail        
       });
       console.log(queryInfo);
       console.log(`http://localhost:${this.state.endPoint}/retrieve`)    
       axios.post(`http://localhost:${this.state.endPoint}/retrieve`, queryInfo, {"headers": {"content-type": "application/json",}})
       .then(
         response => {
           console.log(response.data)
           this.setState({callback:response.data})
          }
         );
    }

    render(){
      return (
        <div className="retrieveToken">
          <Container>
            <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="200" className="App-logo"/>
            <Row>
              <Col md="auto"><h1>CorDapp Token-To-Friend</h1></Col>
              <Col md={{offset:2}}><Link to="/" ><button className="btn btn-primary" >Create a Token</button></Link> <button className="btn btn-primary disabled" >Retrieve a Token</button></Col>
            </Row>
            <p>In this example CorDapp, you will be able to issue your friend a token that carries a secret message.
              You will receive the information of the tokenID and where the token is stored. Your friend would need these
              information to retrieve the token and reveal the secret message.</p>
              <p>If you are retrieving a token, please click the Retrieve Token button above to continue. ↗️ </p>
          </Container>
          <Container>
            <h4>Retrieve the Secret Message: </h4>
            <form onSubmit={this.handleSubmit}>
              <div className="form-group">
                <label>
                  Storage Node
                </label>
                <select value={this.state.storageNode} name="storageNode" onChange={this.handleChange} className="form-control">
                  {this.state.peerNodes.map((option) => (
                    <option value={option.storageNode}>{option.label}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Token ID</label>
                <input
                  type="text"  className="form-control" name="tokenId" onChange={this.handleChange} />
              </div>
              <div className="form-group">
                <label>
                  Recipient's Email Address
                </label>
                <input
                  type="text"  className="form-control" name="reEmail" onChange={this.handleChange} />
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
                <h4>Here is your query result: </h4>
                <p>{this.state.callback}</p>
              </div>
                  }
          </Container>


        </div>
      );
    }
  }

  export default retrieveToken;
