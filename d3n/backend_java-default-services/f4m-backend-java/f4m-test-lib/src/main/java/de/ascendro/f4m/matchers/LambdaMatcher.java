package de.ascendro.f4m.matchers;

import java.util.function.Function;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;

public class LambdaMatcher<T> extends CustomMatcher<T>{
	private final Function<T, Boolean> itemMatcher;
	
	public static <T> Matcher<T> matches(String description, Function<T, Boolean> itemMatcher){
		return new LambdaMatcher<T>(description, itemMatcher);
	}
	
	private LambdaMatcher(String description, Function<T, Boolean> itemMatcher) {
		super(description);
		this.itemMatcher = itemMatcher;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean matches(Object item) {
		return itemMatcher.apply((T)item);
	}
}
