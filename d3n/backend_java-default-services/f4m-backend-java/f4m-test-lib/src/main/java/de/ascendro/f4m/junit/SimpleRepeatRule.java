package de.ascendro.f4m.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit test rule for repeatedly running 'unstable' tests.<br/>
 * Usage - add rule declaration to test class:
 * <code>
 * 	@Rule public SimpleRepeatRule repeatRule = new SimpleRepeatRule(10);
 * </code>
 * 
 */
public class SimpleRepeatRule implements TestRule {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRepeatRule.SimpleRepeatStatement.class);
	private int repeatCount;

	public SimpleRepeatRule(int repeatCount) {
		this.repeatCount = repeatCount;

	}

	private class SimpleRepeatStatement extends Statement {
		
		private final Statement statement;
		private Description description;

		private SimpleRepeatStatement(Statement statement, Description description) {
			this.statement = statement;
			this.description = description;
		}

		@Override
		public void evaluate() throws Throwable {
			for (int i = 0; i < repeatCount; i++) {
				LOGGER.debug("Running {}. iteration of test '{}'", i, description.getDisplayName());
				statement.evaluate();
			}
		}
	}

	@Override
	public Statement apply(Statement statement, Description description) {
		return new SimpleRepeatStatement(statement, description);
	}
}