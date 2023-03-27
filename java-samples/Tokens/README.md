## Tokens CorDapp Samples

This folder features [TokenSDK](https://training.corda.net/libraries/tokens-sdk/) sample projects.

### [Fungible House Token](./fungiblehousetoken):
This CorDapp serves as a basic example to demonstrate how to tokenize an asset into [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the TokenSDK. 

### [Bike Market](./bikemarket):
This sample CorDapp demonstrates all four out-of-box TokenSDK flows (Create, Issue, Move, and Redeem flows). The CorDapp demonstrate how to tokenize an asset as non-fungible tokens and excute a few transacting procedures. 
<p align="center">
  <img src="./bikemarket/diagram/pic1.png" alt="Corda" width="500">
</p>

### [Dollar To House Token](./dollartohousetoken):
This CorDapp demonstrate a DvP (Delivery vs Payment) of an [Evolvable](https://training.corda.net/libraries/tokens-sdk/#evolvabletokentype), [NonFungible](https://training.corda.net/libraries/tokens-sdk/#nonfungibletoken) house token in Corda utilizing the TokenSDK. 

### [Stock Pay Dividend](./stockpaydividend):
This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of [EvolvableToken](https://training.corda.net/libraries/tokens-sdk/#evolvabletokentype) which represents stock. You will find the StockState extends from EvolvableToken which allows the stock details(eg. announcing dividends) to be updated without affecting the parties who own the stock.  

### [Spaceships-javaAPIs](./spaceships-javaAPIs):
This CorDapp contains examples using new [Java APIs](https://medium.com/corda/corda-tokens-made-easy-with-new-java-apis-83095693d72) introduced in Token SDK 1.2 - It makes of use of utility functions and the new Java Token Builders. The lastes Java APIs were indeed later implmented for all of the samples above as well. 

### [Token To Friend](./tokentofriend):
This CorDapp showcase a simple token issuance use case. You can attach a secret message with the token and your friend can retrive it with the credential that you inputed when creating the token. This cordapp come with a nice looking front end using Reactjs. Feel free to give a try on the app!
<p align="center">
  <img src="./tokentofriend/diagram.png" alt="Corda" width="600">
</p>
