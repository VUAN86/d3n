package de.ascendro.f4m.service.integration.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.rules.ErrorCollector;
import org.junit.runners.model.MultipleFailureException;

import de.ascendro.f4m.service.exception.F4MException;

/**
 * Advanced ErrorCollector, which allows clearing collected error, if necessary for badly engineered test.
 */
public class ClearableErrorCollector extends ErrorCollector {
    private List<Throwable> errors = new ArrayList<>();
    private F4MException[] expectedErrors;

    @Override
    protected void verify() throws Throwable {
    	if(ArrayUtils.isEmpty(expectedErrors)){
    		MultipleFailureException.assertEmpty(errors);
    	}else {
    		verifyIfOnlyExpected();
    	}
    }

	private void verifyIfOnlyExpected() throws Exception {
		final List<Throwable> actualErrors = new ArrayList<>();
		for (Throwable error : errors) {
			if (error instanceof F4MException) {
				final F4MException receivedF4MException = (F4MException) error;
				final boolean receivedExpected = Stream.of(expectedErrors)
						.filter(e -> e.getClass().equals(receivedF4MException.getClass()))
						.filter(e -> e.getCode().equals(receivedF4MException.getCode()))
						.filter(e -> e.getType().equals(receivedF4MException.getType()))
						.findAny()
						.isPresent();
				if (!receivedExpected) {
					actualErrors.add(error);
				}
			} else {
				actualErrors.add(error);
			}
		}
		MultipleFailureException.assertEmpty(actualErrors);
	}

    /**
     * Adds a Throwable to the table.  Execution continues, but the test will fail at the end.
     */
    @Override
	public void addError(Throwable error) {
        errors.add(error);
    }

    public void clearCollectedErrors() {
    	errors.clear();
    }
    
    public void setExpectedErrors(F4MException...expectedErrors){
    	this.expectedErrors = expectedErrors;
    }
}
