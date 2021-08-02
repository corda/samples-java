import React, { useState, useEffect } from 'react';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import Link from '@material-ui/core/Link';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import { useHistory, withRouter } from 'react-router-dom';
import axios from 'axios';

import secret_corda from '../img/secret_corda.png';
import { BACKEND_URL, SEND_EMAIL } from '../CONSTANTS.js';

// function validateEmail(email) {
//   const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
//   return re.test(String(email).toLowerCase());
// }

function Copyright() {
  return (
    <Typography variant="body2" color="textSecondary" align="center">
      {'Copyright © '}
      <Link color="inherit" href="https://material-ui.com/">
        R3
      </Link>{' '}
      {new Date().getFullYear()}
      {'.'}
    </Typography>
  );
}

const useStyles = makeStyles((theme) => ({
  paper: {
    marginTop: theme.spacing(8),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  avatar: {
    margin: theme.spacing(1),
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: '100%', // Fix IE 11 issue.
    marginTop: theme.spacing(3),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  },
}));

function CreateSantaGame(props) {
  
  // state variables and related functions 
  const [name1, setName1] = useState("david");
  const [name2, setName2] = useState("peter");
  const [name3, setName3] = useState("mary");
  
  const name1Change = e => setName1(e.target.value);
  const name2Change = e => setName2(e.target.value);
  const name3Change = e => setName3(e.target.value);

  const [email1, setEmail1] = useState("david@corda.net");
  const [email2, setEmail2] = useState("peter@corda.net");
  const [email3, setEmail3] = useState("mary@corda.net");

  const email1Change = e => setEmail1(e.target.value);
  const email2Change = e => setEmail2(e.target.value);
  const email3Change = e => setEmail3(e.target.value);
  
  let playerNames = [name1, name2, name3];
  let playerEmails = [email1, email2, email3];

  const classes = useStyles();
  const history = useHistory();

  const [response, setResponse] = useState(null);

  function buttonHandler(e) {
    e.preventDefault();

    console.log(playerNames);
    console.log(playerEmails);

    const data = { 
      playerNames: playerNames, 
      playerEmails: playerEmails,
      sendEmail: SEND_EMAIL
     }

    axios.post(
      BACKEND_URL + '/games',
      data,
      { headers: { 'Content-Type': 'application/json' } }
    ).then(res => {
      const gameId = res.data.gameId; 

      if (gameId !== null) {
        const secret_msg = "❄️ The elves 🧝‍♂️ have set aside some space " + gameId + " in santa's workshop! ❄️";
        console.log(secret_msg);
      
        if (!SEND_EMAIL) {
          alert(secret_msg); 
        }
        
        setResponse(res);
      }
      
    });
    
  }

  // note for anyone curious how this useEffect binds to the response
  // https://stackoverflow.com/questions/63603966/react-api-call-with-axios-how-to-bind-an-onclick-event-with-an-api-call

  useEffect(() => {
    if (response !== null) {
      let path = "/created";
      history.push(path);
    }
  }, [response]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Container component="main" maxWidth="sm">
    <CssBaseline />
    <div className={classes.paper}>

      <img src={secret_corda} alt="corda logo with santa hat"/>

      <Typography component="h1" variant="h2">
        Corda Secret Santa!
      </Typography>
      
          <form className={classes.form} id="createSantaForm" noValidate>
            
            <Grid container spacing={2}>
            
              <Grid item xs={12} sm={6}>
                <TextField
                  autoComplete="fname"
                  name="firstName"
                  variant="outlined"
                  required
                  fullWidth
                  id="firstName1"
                  label="First Name"
                  placeholder={name1}
                  onChange={name1Change}
                  error={name1 === ""}
                  helperText={name1 === "" ? 'Empty field!' : ' '}
                  autoFocus
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  variant="outlined"
                  required
                  fullWidth
                  id="email1"
                  label="Email Address"
                  name="email"
                  autoComplete="email"
                  type="email"
                  placeholder={email1}
                  onChange={email1Change}
                  error={email1 === ""} // TODO use validateEmail in the future
                  helperText={email1 === "" ? 'Empty field!' : ' '}
                />
              </Grid>


              <Grid item xs={12} sm={6}>
                <TextField
                  autoComplete="fname"
                  name="firstName"
                  variant="outlined"
                  required
                  fullWidth
                  id="firstName2"
                  label="First Name"
                  placeholder={name2}
                  onChange={name2Change}
                  error={name2 === ""}
                  helperText={name2 === "" ? 'Empty field!' : ' '}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  variant="outlined"
                  required
                  fullWidth
                  id="email2"
                  label="Email Address"
                  name="email"
                  autoComplete="email"
                  type="email"
                  placeholder={email2}
                  onChange={email2Change}
                  error={email2 === ""}
                  helperText={email2 === "" ? 'Empty field!' : ' '}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  autoComplete="fname"
                  name="firstName"
                  variant="outlined"
                  required
                  fullWidth
                  id="firstName3"
                  label="First Name"
                  placeholder={name3}
                  onChange={name3Change}
                  error={name3 === ""}
                  helperText={name3 === "" ? 'Empty field!' : ' '}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  variant="outlined"
                  required
                  fullWidth
                  id="email3"
                  label="Email Address"
                  name="email"
                  autoComplete="email"
                  type="email"
                  placeholder={email3}
                  onChange={email3Change}
                  error={email3 === ""}
                  helperText={email3 === "" ? 'Empty field!' : ' '}
                />
              </Grid>

              <Grid item xs={12}>
                <FormControlLabel
                  control={<Checkbox value="allowExtraEmails" color="primary"/>}
                  label="We want to receive 🦌 reindeer, 🎁  marketing promotions and 🍭 candy via email."
                />

                <br/> 
                <br/> 

              </Grid>
            </Grid>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              onClick={buttonHandler}
            >
              Create
            </Button>

            <Grid container justify="flex-end">
              <Grid item>
                <br/> 
                <Link href="/check" variant="body2">
                  {"Looking up a game? Check a Santa 🎅  ID."}
                </Link>
              </Grid>
            </Grid>
          </form>

        </div>
        <Box mt={5}>
          <Copyright />
        </Box>
      </Container>
  );
}

export default withRouter(CreateSantaGame);