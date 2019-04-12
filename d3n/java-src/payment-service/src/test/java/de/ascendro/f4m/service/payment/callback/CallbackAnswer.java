package de.ascendro.f4m.service.payment.callback;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CallbackAnswer<V> implements Answer<Void> {
	private V response;

	public CallbackAnswer(V response) {
		this.response = response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void answer(InvocationOnMock invocation) throws Throwable {
		Object[] args = invocation.getArguments();
		boolean callbackFound = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Callback) {
				((Callback<V>) args[i]).completed(response);
				callbackFound = true;
			}
		}
		if (callbackFound) {
			return null;
		} else {
			throw new RuntimeException("Callback method not found");
		}
	}

}
