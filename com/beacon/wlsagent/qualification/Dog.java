package com.beacon.wlsagent.qualification;

import de.schlichtherle.license.*;

import java.util.Date;
import java.util.prefs.Preferences;

import com.beacon.wlsagent.BeaconException;

public class Dog {
	public Dog() {
	}

	/** The product id of your software */
	private static final String PRODUCT_ID = "WEBLOGICMON"; // CUSTOMIZE

	/**
	 * The subject for the license manager and also the alias of the private key
	 * entry in the keystore.
	 */
	private static final String SUBJECT = "fusionspykey"; // CUSTOMIZE

	/** The resource name of your private keystore file. */
	private static final String KEYSTORE_RESOURCE = "publicCerts.store"; // CUSTOMIZE

	/** The password for the keystore. */
	private static final String KEYSTORE_STORE_PWD = "UyCAIUDoHh27PYWXYUZ9"; // CUSTOMIZE

	/** The password to encrypt the generated license key file. */
	private static final String CIPHER_KEY_PWD = "UyCAIUDoHh27PYWXYUZ9"; // CUSTOMIZE

	private LicenseContent lc;

	private LicenseManager manager = new LicenseManager(
			new DefaultLicenseParam(SUBJECT, Preferences
					.userNodeForPackage(Dog.class), new DefaultKeyStoreParam(
					Dog.class, // CUSTOMIZE
					KEYSTORE_RESOURCE, SUBJECT, KEYSTORE_STORE_PWD, null),//
					new DefaultCipherParam(CIPHER_KEY_PWD)));

	private void install() {
		try {
			manager.install(new java.io.File("license.beacon"));
		} catch (Exception ex) {
			System.out.println("License install failed due to: \n"
					+ ex.getMessage());
			System.exit(-1);
		}
	}

	public boolean presume() throws BeaconException {
		this.install();
		boolean isValid = false;
		try {
			this.lc = manager.verify();
			System.out.println("VALIDATION PASS! \nAUTHORIZATION MESSAGE :\n "
					+ lc.getInfo());
			isValid = true;
			Date expDate = lc.getNotAfter();
			if (expDate != null) {
				System.out.println("This license will expire after: "
						+ lc.getNotAfter());
			}
			return isValid;
		} catch (Exception ex) {
			throw new BeaconException("License validation failed due to:\n"
					+ ex.getMessage());
		}
	}

	public void silentPresume() throws Exception {
		this.install();
		manager.verify();
		this.lc = manager.verify();		
	}

	public String getIp() {
		if (this.lc != null) {
			return this.lc.getIp();
		} else {
			return "";
		}
	}

}