# attachzip -- attachment demo [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/attachment-sendfile)

This Cordapp shows how to upload and download an [attachment](https://training.corda.net/corda-details/attachments/) via flow.


## Concepts

In this Cordapp, there are two parties:
* Seller: sends an invoice (with attachment) to Buyer
* Buyer: receive the the invoice and be able to download the attached zip file to their local machine


### States

You'll want to take a quick look at [InvoiceState.java](./contracts/src/main/java/net/corda/examples/sendfile/states/InvoiceState.java#L21)

```java
public InvoiceState(String invoiceAttachmentID, List<AbstractParty> participants) {
    this.invoiceAttachmentID = invoiceAttachmentID;
    this.participants = participants;
}
```


### Flows

There are two flows [`sendAttachment`](./workflows/src/main/java/net/corda/examples/sendfile/flows/SendAttachment.java#L47-L67) and [`downloadAttachment`](./workflows/src/main/java/net/corda/examples/sendfile/flows/DownloadAttachment.java#L47-L66).

The flow logic is the following:

* `sendAttachment`: send and sync the attachment between parties
  1. Uploading attachment from local
  2. Attaching the accachmentID to the transaction
  3. Storing the attached file into attachment service at the counterparty's node (Automatically check if it already exists or not. If it does, do nothing; if not, download the attached file from the conterparty.)

* `downloadAttchment`: save the attachment file from node's serviceHub to local
  1. signing the attachment service in the node to download the file via attachmentID

![alt text](./graph.png)


## Usage

### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions.

Once all three nodes are started up, in Seller's node shell, run:
```
flow start SendAttachment receiver: Buyer
```
After this call, we already finished
1. uploading a zip file to Seller's node
2. sending the zip file to Buyer's node

Now, lets move to Buyer's node shell, and run:
```
flow start DownloadAttachment sender: Seller, path: file.zip
```
This command is telling the node to retrieve attachment from the transaction that is sent by `Seller`and download it as `file.zip` at the node root direction （⚠️ attachZIP/build/nodes/Buyer)



## Notes:

* This uploaded file is hardcoded into the flow.
* The transaction retrieving is also hardcoded to retrieve the first state that being stored in the vault.

