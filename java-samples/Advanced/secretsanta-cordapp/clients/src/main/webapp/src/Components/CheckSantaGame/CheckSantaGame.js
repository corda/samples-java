import React, { useState, useEffect } from 'react';
import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Link from '@material-ui/core/Link';
import Paper from '@material-ui/core/Paper';
import Box from '@material-ui/core/Box';
import Grid from '@material-ui/core/Grid';
import LockOutlinedIcon from '@material-ui/icons/LockOutlined';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import { BACKEND_URL, SEND_EMAIL } from '../CONSTANTS.js';
import { useHistory, withRouter } from 'react-router-dom';
import axios from 'axios';

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
  root: {
    height: '100vh',
  },
  image: {
    backgroundImage: 'url(https://source.unsplash.com/random)',
    backgroundRepeat: 'no-repeat',
    backgroundColor:
      theme.palette.type === 'light' ? theme.palette.grey[50] : theme.palette.grey[900],
    backgroundSize: 'cover',
    backgroundPosition: 'center',
  },
  paper: {
    margin: theme.spacing(8, 4),
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
    marginTop: theme.spacing(1),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  },
}));


function CheckSantaGame(props) {

  const classes = useStyles();
  const history = useHistory();

  const [response, setResponse] = useState(null);
  
  // state variables and related functions 
  const [gameId, setGameId] = useState("12345678");
  const [name, setName] = useState("david");
  
  const gameIdChange = e => setGameId(e.target.value);
  const nameChange = e => setName(e.target.value);

  const buttonHandler = (e) => {
    e.preventDefault(); 
    
    console.log(name);
    console.log(gameId);

    const data = { 
      name: name.trim(),
      gameId: gameId.trim(), 
      sendEmail: SEND_EMAIL, 
     }

    axios.post(
      BACKEND_URL + '/games/check',
      data,
      { 
        headers: { 'Content-Type': 'application/json' } 
      }
    ).then(res => {
      console.log("RESPONSE: ", res.data); 

      const target = res.data.target; 

      if (target !== null) {
        const secret_msg = "🧝‍♂️  - Psst! It's " + target + "!";
        console.log(secret_msg)

        if (!SEND_EMAIL) {
          alert(secret_msg);  
        }
        setResponse(res);
      }
      
    });
  }

  useEffect(() => {
    if (response !== null) {
      let path = "/checked";
      history.push(path);
    }
  }, [response]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Grid container component="main" className={classes.root}>
      <CssBaseline />
      <Grid item xs={false} sm={4} md={7} className={classes.image} />
      <Grid item xs={12} sm={8} md={5} component={Paper} elevation={6} square>
        <div className={classes.paper}>
          <Avatar className={classes.avatar}>
            <LockOutlinedIcon />
          </Avatar>
          <Typography component="h1" variant="h2">
            Check Santa Details
          </Typography>
          <form
            className={classes.form}
            noValidate>

            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              id="gameId"
              label="Santa Session ID"
              name="gameId"
              autoComplete="gameId"

              placeholder={gameId}
              onChange={gameIdChange}
              error={gameId === ""}
              helperText={gameId === "" ? 'Empty field!' : ' '}

              autoFocus
            />

            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="name"
              label="First Name"
              type="text"
              id="firstName"
              autoComplete=""

              placeholder={name}
              onChange={nameChange}
              error={name === ""}
              helperText={name === "" ? 'Empty field!' : ' '}
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              onClick={ buttonHandler }
            >
              Check!
            </Button>

            <br/> 

            <Grid container>
              <Grid item xs>
                {/* <Link href="#" variant="body2">
                  Forgot password?
                </Link> */}
              </Grid>
              <Grid item>
                <br/>
                <Link href="/create" variant="body2">
                  {"Don't have a game? Make one 🎁 "}
                </Link>

              </Grid>
            </Grid>
            <Box mt={5}>
              <Copyright />
            </Box>
          </form>
        </div>
      </Grid>
    </Grid>
  );
}

export default withRouter(CheckSantaGame);
