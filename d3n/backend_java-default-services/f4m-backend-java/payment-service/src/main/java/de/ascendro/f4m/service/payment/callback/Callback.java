package de.ascendro.f4m.service.payment.callback;

public interface Callback<R> {
	public void completed(R response);
	public void failed(Throwable throwable);
}
