## samples-java/token-cordapps

This folder features five sample projects, each of them demonstrates different features of corda.  

### [Bike Market](./bikemarket): 
This sample Cordapp demonstrate some simple flows related to the token SDK.

### [Token Market](./tokenmarket): 
This CorDapp servers a basic example to create, issue and perform a DvP (Delivery vs Payment) of an Evolvable NonFungible token in Corda utilizing the TokenSDK.

### [Fungible House Token](./fungiblehousetoken): 
This cordapp servers as a basic example to create, issue, move fungible tokens in Corda utilizing the TokenSDK.

### [Stock Pay Dividend](./stockpaydividend): 
This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of EvolvableToken which represents stock. You will find the StockState extends from EvolvableToken which allows the stock details(eg. announcing dividends) to be updated without affecting the parties who own the stock.

