import React from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import Link from '@material-ui/core/Link';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import secret_corda from '../img/secret_corda.png';

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

export default function SantaCheckSent(props) {
  const classes = useStyles();

  return (
    <Container component="main" maxWidth="sm">
    <CssBaseline />
    <div className={classes.paper}>

      <img src={secret_corda}/>

      <Typography component="h1" variant="h2">
      Help is on the sleigh!
      </Typography>

      <br/> 

      <Typography component="h6" variant="h6">
        Everyone forgets sometimes. Not to worry! The elves have sent a reminder to your email (check your spam chimney 📨!)! 

        Make sure you don't tell anyone who the elves assigned you! 🤫 

        Good luck! 🎄 
      </Typography>

      <Box mt={5}>
        
      </Box>

      <Grid container justify="flex-end">
        <Grid item>
          <Link href="/create" variant="body2">
            Don't have a game? Make one 🎁 
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
