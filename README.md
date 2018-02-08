![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Bootcamp CorDapp

This project is the template we will use to define a complete CorDapp during 
today's bootcamp. Our CorDapp will allow a token to be issued onto the ledger.

We'll take a test-driven approach. You'll know your CorDapp is working 
when it passes both sets of tests defined in `src/test/kotlin/bootcamp`.

## Links to useful resources

This project contains two sets of example state and contract implementations:

* `src/main/kotlin/examples/ExampleStates.kt`
* `src/main/kotlin/examples/ExampleContract.kt`

There are also several web resources that you will likely find useful for this 
bootcamp:

* Key Concepts docs (`docs.corda.net/key-concepts.html`)
* API docs (`docs.corda.net/api-index.html`)
* Cheat sheet (`docs.corda.net/cheat-sheet.html`)
* Sample CorDapps (`www.corda.net/samples`)
* Stack Overflow (`www.stackoverflow.com/questions/tagged/corda`)

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
    |   - recipient   |
    |   - amount      |
    |                 |
    -------------------

### The TokenContract

Contracts govern how states evolve over time. Our contract, TokenContract, 
will define how TokenStates evolve. It will impose the following constraints 
on transactions involving TokenStates:

    -------------------------------------------------------------------------------------
    |                                                                                   |
    |    - - - - - - - - - -                                     -------------------    |
    |                                              ▲             |                 |    |
    |    |                 |                       | -►          |   TokenState    |    |
    |            NO             -------------------     -►       |                 |    |
    |    |                 |    |      Issue command       -►    |   - issuer      |    |
    |          INPUTS           |     signed by issuer     -►    |   - recipient   |    |
    |    |                 |    -------------------     -►       |   - amount > 0  |    |
    |                                              | -►          |                 |    |
    |    - - - - - - - - - -                       ▼             -------------------    |
    |                                                                                   |
    -------------------------------------------------------------------------------------

              No inputs             One issue command,                One output,
                                 issuer is a required signer       amount is positive

### The TokenFlow

Flows automate the process of updating the ledger. Our flow, TokenFlow, is 
already defined, and automates the following steps:

            Issuer                Recipient                Notary
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
