<p align="center">
    <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda Token SDK

## Reminder

This project is open source under an Apache 2.0 licence. That means you
can submit PRs to fix bugs and add new features if they are not currently
available.

## What is the token SDK?

The tokens SDK exists to make it easy for CorDapp developers to create
CorDapps which use tokens. Functionality is provided to create token types,
then issue, move and redeem tokens of a particular type.

The tokens SDK comprises three CorDapp JARs:

1. Contracts which contains the base types, states and contracts
2. Workflows which contains flows for issuing, moving and redeeming tokens
   as well as utilities for the above operations.
3. Money which contains token type definitions for various currencies

The token SDK is intended to replace the "finance module" from the core
Corda repository.

For more details behind the token SDK's design, see
[here](design/design.md).

## How to use the SDK?

### Using the bootcamp cordapp.

By far the easiest way to get started with the tokens SDK is to use the
`tokens-template` which is a branch on the java version of the "CorDapp
template". You can obtain it with the following commands:

You can get started to use tokens-sdk by cloning this bootcamp-cordapp. 

    git clone https://github.com/corda/bootcamp-cordapp.git
    cd bootcamp-cordapp
    git checkout token_bootcamp

Once you have cloned the repository, you should open it with IntelliJ. This
will give you an example repo with the token SDK dependencies already
included and some example code which should illustrate you how to use token SDK.
You can `deployNodes` to create three nodes:

    ./gradlew clean deployNodes
    ./build/nodes/runnodes

You can issue some currency tokens from `PartyA` to `PartyB` from Party A's
shell with the following command:

    start CreateAndIssueFixedToken currencyCode: GBP, amount: 100, recipient: PartyB

To check the issued Fungible tokens run the below command on PartyB's terminal

      run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.FungibleToken

Create evolvable token type on the ledger on PartyA's terminal

    start CreateEvolvableToken importantInformationThatMayChange : random

This will create a linear state of type StateTokenType in A's vault

Get the uuid of the StateTokenType from PartyA's terminal by hitting below command.

    run vaultQuery contractStateType : com.bootcamp.states.StateTokenType

Issue tokens off the created ExampleEvolvableTokenType from PartyA s terminal to PartyB

    start IssueEvolvableToken tokenId : 79332247-61d2-4d00-bb1f-d62416cf4920 , recipient : PartyB

To check the issued NFT run the below command on PartyB's terminal

      run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.NonFungibleToken

### Adding token SDK dependencies to an existing CorDapp

First, add a variable for the tokens SDK version you wish to use:

    buildscript {
        ext {
            tokens_release_version = '1.2'
            tokens_release_group = 'com.r3.corda.lib.tokens'
        }
    }

Second, you must add the tokens development artifactory repository to the
list of repositories for your project:

    repositories {
        maven { url 'https://ci-artifactory.corda.r3cev.com/artifactory/corda-lib' }
    }

Now, you can add the tokens SDK dependencies to the `dependencies` block
in each module of your CorDapp. For contract modules add:

    cordaCompile "$tokens_release_group:tokens-contracts:$tokens_release_version"

In your workflow `build.gradle` add:

    cordaCompile "$tokens_release_group:tokens-workflows:$tokens_release_version"

If you want to use the `deployNodes` task, you will need to add the
following dependencies to your root `build.gradle` file:

    cordapp "$tokens_release_group:tokens-contracts:$tokens_release_version"
    cordapp "$tokens_release_group:tokens-workflows:$tokens_release_version"

These should also be added to the `deployNodes` task with the following syntax:

    nodeDefaults {
        projectCordapp {
            deploy = false
        }
        cordapp("$tokens_release_group:tokens-contracts:$tokens_release_version")
        cordapp("$tokens_release_group:tokens-workflows:$tokens_release_version")
    }

### Installing the token SDK binaries

If you wish to build the token SDK from source then do the following to
publish binaries to your local maven repository:

    git clone http://github.com/corda/token-sdk
    cd token-sdk
    ./gradlew clean install