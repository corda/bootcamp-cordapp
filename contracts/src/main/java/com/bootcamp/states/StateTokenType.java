package com.bootcamp.states;

import com.bootcamp.contracts.StateTokenTypeContract;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import java.util.Objects;


/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
@BelongsToContract(StateTokenTypeContract.class)
public class StateTokenType extends EvolvableTokenType{

    private final String importantInformationThatMayChange;
    private final Party maintainer;
    private final UniqueIdentifier uniqueIdentifier;
    private final int fractionDigits;

    public StateTokenType(String importantInformationThatMayChange, Party maintainer, UniqueIdentifier uniqueIdentifier, int fractionDigits) {
        this.importantInformationThatMayChange = importantInformationThatMayChange;
        this.maintainer = maintainer;
        this.uniqueIdentifier = uniqueIdentifier;
        this.fractionDigits = fractionDigits;
    }

    public String getImportantInformationThatMayChange() { return importantInformationThatMayChange; }

    @Override
    public int getFractionDigits() { return this.fractionDigits; }

    @NotNull
    @Override
    public List<Party> getMaintainers() { return Arrays.asList(maintainer); }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return this.uniqueIdentifier; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateTokenType that = (StateTokenType) o;
        return getFractionDigits() == that.getFractionDigits() &&
                getImportantInformationThatMayChange().equals(that.getImportantInformationThatMayChange()) &&
                getMaintainers().equals(that.getMaintainers()) &&
                getLinearId().equals(that.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getImportantInformationThatMayChange(), getMaintainers(), getLinearId(), getFractionDigits());
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<StateTokenType> toPointer(){
        LinearPointer<StateTokenType> linearPointer = new LinearPointer<>(uniqueIdentifier, StateTokenType.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}