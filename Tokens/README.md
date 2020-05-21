## samples-java/token-cordapps

This folder features tokenSDK sample projects.

### [bike market](./bikemarket):
This sample Cordapp demonstrate some simple flows related to the token SDK.

### [dollar to Token DvP](./dollartohousetoken):
This CorDapp serves as a basic example to create, issue and perform a DvP (Delivery vs Payment) of an Evolvable NonFungible token in Corda utilizing the TokenSDK.

### [fungible house token](./fungiblehousetoken):
This Cordapp serves as a basic example to create, issue, move fungible tokens in Corda utilizing the TokenSDK.

### [stock pay dividend](./stockpaydividend):
This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of EvolvableToken which represents stock. You will find the StockState extends from EvolvableToken which allows the stock details(eg. announcing dividends) to be updated without affecting the parties who own the stock.

### [spaceships-javaAPIs](./spaceships-javaAPIs):
This CorDapp contains examples using new Java APIs introduced in Token SDK 1.2 - It makes of use of utility functions and the new Java Token Builders.