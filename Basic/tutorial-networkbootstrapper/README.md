# Network Bootstrapper Tutorial

In this tutorial, we will walk through the steps of bootstrapping a Corda Network using the Corda Network Bootstrapper. This tutorial consists of 3 node Config files and 2 shell scripts to ease up the manual copying of the files and folders.

Due to the size cap of the GitHub uploaded file, you will need to manually download the Corda Network Bootstrapper (to this directory). The download link is at [here](https://software.r3.com/ui/native/corda-releases/net/corda/corda-tools-network-bootstrapper)

## Deploy a local Corda Network via the bootstrapper
With the Corda Network Bootstrapper downloaded to this directory, you can simply call: (with the version name of the bootstrapper that you downloaded)
```
java -jar corda-tools-network-bootstrapper-4.9.jar
```
This command will generate the node folders that corresponds with each node config file. You should expect some folder structure like this:
```
. 
├── network_Bootstrapper.jar    
├── Notary_node.conf            
├── PA_node.conf            
├── PB_node.conf            
├── cordapp-a.jar    
├── cordapp-b.jar
├── checkpoints_xxxxxx.log       //Logs of bootstrapping
├── diagnostic-xxxxxxx.log       //Logs of bootstrapping
├── node-xxxxx.log               //Logs of bootstrapping
├── Notary                       //Notary's folder
├── PA                          //PA's folder
└── PB                          //PB's folder
```
Next, you will need to go into each node folder and start the node.
```
cd PA
java -jar corda.jar 
```
Then, you can go into the PB folder and Notary folder to do the same steps, and you will have a Corda network running on your local machine.

## Deploy a Corda Network onto VMs via the bootstrapper

When deploying a Corda network to remote VMs, you would need to do an additional step before bootstrap the node folders. You would need to go into the node.conf file and add the VM address. For example: (this is not the full config file, you still need other fields.)
```
//remote VM address
p2pAddress="13.71.147.131:10007"
rpcSettings {
    address="0.0.0.0:10008"
    adminAddress="0.0.0.0:10108"
    standAloneBroker=false
    useSsl=false
}
```
After change the p2pAddress, you can now perform the bootstrapping with
```
java -jar corda-tools-network-bootstrapper-4.9.jar
```
Next, we need to drop the node folder to the remote VMs. I simply use the `scp` command. For example:
```
scp -r ./PartyA user@13.71.147.131:./
```
The steps for starting the node would be the same with doing so on a local machine. 
