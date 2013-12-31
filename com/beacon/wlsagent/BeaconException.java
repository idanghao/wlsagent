package com.beacon.wlsagent;

public class BeaconException extends RuntimeException {

	public BeaconException() {
	}

	public BeaconException(String message, Throwable cause) {
		super(message, cause);
	}

	public BeaconException(String message) {
		super(message);
	}

	public BeaconException(Throwable cause) {
		super(cause);
	}
}
