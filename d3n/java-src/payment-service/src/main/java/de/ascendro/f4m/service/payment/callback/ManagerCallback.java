package de.ascendro.f4m.service.payment.callback;

import java.util.function.Function;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ManagerCallback<T, J extends JsonMessageContent> extends WrapperCallback<T, J> {
	private Function<T, J> converter;
	
	public ManagerCallback(Callback<J> originalCallback, Function<T, J> converter) {
		super(originalCallback);
		this.converter = converter;
	}
	
	@Override
	protected void executeOnCompleted(T response) {
		J result = converter.apply(response);
		originalCallback.completed(result);
	}
}