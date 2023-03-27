import React from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import Link from '@material-ui/core/Link';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';

import secret_corda from '../img/secret_corda.png';
import santa_flying from '../img/reindeer-flying.png';


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

export default function SantaGameCreated(props) {
  const classes = useStyles();

  return (
    <Container component="main" maxWidth="sm">
    <CssBaseline />
    <div className={classes.paper}>

      <img src={secret_corda}/>

      <Typography component="h1" variant="h2">
      The Game is on!
      </Typography>

      <br/> 

      <Typography component="h6" variant="h6">
        The elves have sent the assignments to each of the participants so your friends know where to send their gifts 🎁! (Check your spam chimney!)
        
        Make sure you don't tell anyone who the elves assigned you! 🤫 For the best results you should wait to open your gifts altogether and guess who the elves picked to get each gift.
        
        <br/> 
        <br/> 

        Merry Christmas from all of us at R3. 🎄
      </Typography>

      <Box mt={5}>
        <img src={santa_flying}/>
      </Box>

      <Grid container justify="flex-end">
        <Grid item>
          <Link href="/check" variant="body2">
            {"Looking up a game? Check a Santa 🎅  ID."}
          </Link>
        </Grid>
      </Grid>  

    </div>

    <Box mt={5}>
      <Copyright />
    </Box>
    </Container>
  );
  
}
