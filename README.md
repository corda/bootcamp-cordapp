<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Bootcamp CorDapp [<img src="https://raw.githubusercontent.com/corda/samples-java/master/webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/bootcamp-cordapp)

This project is the template we will use as a basis for developing a complete CorDapp
during today's bootcamp. Our CorDapp will allow the issuance of tokens onto the ledger.

We'll develop the CorDapp using a test-driven approach. At each stage, you'll know your
CorDapp is working once it passes both sets of tests defined in `src/test/java/bootcamp`.

## Set up

1. Download and install a JDK 8 JVM (minimum supported version 8u131)
2. Download and install IntelliJ Community Edition (supported versions: prior than 2021)
3. Download the bootcamp-cordapp repository:

       git clone https://github.com/corda/bootcamp-cordapp

4. Open IntelliJ load the project

## What we'll be building

Our CorDapp will have three parts:

### The TokenState

States define shared facts on the ledger. Our state, TokenState, will define a
token. It will have the following structure:

    -------------------
    |                 |
    |   TokenState    |
    |                 |
    |   - issuer      |
    |   - owner       |
    |   - amount      |
    |                 |
    -------------------

### The TokenContract

Contracts govern how states evolve over time. Our contract, TokenContract,
will define how TokenStates evolve. It will only allow the following type of
TokenState transaction:

    -------------------------------------------------------------------------------------
    |                                                                                   |
    |    - - - - - - - - - -                                     -------------------    |
    |                                              ▲             |                 |    |
    |    |                 |                       | -►          |   TokenState    |    |
    |            NO             -------------------     -►       |                 |    |
    |    |                 |    |      Issue command       -►    |   - issuer      |    |
    |          INPUTS           |     signed by issuer     -►    |   - owner       |    |
    |    |                 |    -------------------     -►       |   - amount > 0  |    |
    |                                              | -►          |                 |    |
    |    - - - - - - - - - -                       ▼             -------------------    |
    |                                                                                   |
    -------------------------------------------------------------------------------------

              No inputs             One issue command,                One output,
                                 issuer is a required signer       amount is positive

To do so, TokenContract will impose the following constraints on transactions
involving TokenStates:

* The transaction has no input states
* The transaction has one output state
* The transaction has one command
* The output state is a TokenState
* The output state has a positive amount
* The command is an Issue command
* The command lists the TokenState's issuer as a required signer

### The TokenIssueFlow

Flows automate the process of updating the ledger. Our flow, TokenIssueFlow, will
automate the following steps:

            Issuer                  Owner                  Notary
              |                       |                       |
       Chooses a notary
              |                       |                       |
        Starts building
         a transaction                |                       |
              |
        Adds the output               |                       |
          TokenState
              |                       |                       |
           Adds the
         Issue command                |                       |
              |
         Verifies the                 |                       |
          transaction
              |                       |                       |
          Signs the
         transaction                  |                       |
              |
              |----------------------------------------------►|
              |                       |                       |
                                                         Notarises the
              |                       |                   transaction
                                                              |
              |◀----------------------------------------------|
              |                       |                       |
         Records the
         transaction                  |                       |
              |
              |----------------------►|                       |
                                      |
              |                  Records the                  |
                                 transaction
              |                       |                       |
              ▼                       ▼                       ▼

## Running our CorDapp

Normally, you'd interact with a CorDapp via a client or webserver. So we can
focus on our CorDapp, we'll be running it via the node shell instead.

Once you've finished the CorDapp's code, run it with the following steps:

* Build a test network of nodes by opening a terminal window at the root of
  your project and running the following command:

  * Windows:   `gradlew.bat deployNodes`
  * Linux/Mac:     `./gradlew deployNodes`

* Start the nodes by running the following command:

  * Windows:   `build\nodes\runnodes.bat`
  * Linux/Mac: `./build/nodes/runnodes`



* Build and Start the nodes using docker:
  * Windows:   `gradlew.bat prepareDockerNodes`
  * Linux/Mac:     `./gradlew prepareDockerNodes`
  * `ACCEPT_LICENSE=Y | docker-compose -f ./build/nodes/docker-compose.yml up`


* Open the nodes are started, go to the terminal of Party A (not the notary!)
  and run the following command to issue 99 tokens to Party B:

  `flow start TokenIssueFlowInitiator owner: PartyB, amount: 99`

* You can now see the tokens in the vaults of Party A and Party B (but not
  Party C!) by running the following command in their respective terminals:

  `run vaultQuery contractStateType: com.bootcamp.states.TokenState`
