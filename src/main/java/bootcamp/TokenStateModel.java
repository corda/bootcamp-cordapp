package bootcamp;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class TokenStateModel {
    private String issuer;
    private String owner;
    private int amount;

    public TokenStateModel(String issuer, String owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "\n" +
                "TokenStateModel{" + "\n" +
                "issuer= " + issuer + "\n" +
                ", owner= " + owner + "\n" +
                ", amount= " + amount + "\n" +
                '}';
    }
}
