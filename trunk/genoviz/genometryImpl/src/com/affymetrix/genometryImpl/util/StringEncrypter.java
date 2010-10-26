package com.affymetrix.genometryImpl.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

public class StringEncrypter
{
	
	public static final String 	DESEDE_ENCRYPTION_SCHEME = "DESede";
	public static final String 	DES_ENCRYPTION_SCHEME = "DES";
	public static final String 	RC4_ENCRYPTION_SCHEME = "RC4";
	
	public static final String	DEFAULT_ENCRYPTION_KEY	= "anONSENsicalSet0FChars&Nums56@346";
	
	private KeySpec				keySpec;
	private SecretKeyFactory		keyFactory;
	private Cipher					cipher;
	
	private static final String	UNICODE_FORMAT			= "UTF8";

	public StringEncrypter(String encryptionScheme) throws EncryptionException {
		this(encryptionScheme, DEFAULT_ENCRYPTION_KEY);
	}

	public StringEncrypter(String encryptionScheme, String encryptionKey)
	        throws EncryptionException {

		if (encryptionKey == null)
			throw new IllegalArgumentException("encryption key was null");
		if (encryptionKey.trim().length() < 24)
			throw new IllegalArgumentException(
			        "encryption key was less than 24 characters");

		try {
			byte[] keyAsBytes = encryptionKey.getBytes(UNICODE_FORMAT);

			if (encryptionScheme.equals(DESEDE_ENCRYPTION_SCHEME)) {
				keySpec = new DESedeKeySpec(keyAsBytes);
			} else if (encryptionScheme.equals(DES_ENCRYPTION_SCHEME)) {
				keySpec = new DESKeySpec(keyAsBytes);
			} else {
				throw new IllegalArgumentException(
				        "Encryption scheme not supported: " + encryptionScheme);
			}

			keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
			cipher = Cipher.getInstance(encryptionScheme);

		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		} catch (UnsupportedEncodingException e) {
			throw new EncryptionException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (NoSuchPaddingException e) {
			throw new EncryptionException(e);
		}

	}

	public String encrypt(String unencryptedString) throws IllegalArgumentException  {
		if (unencryptedString == null || unencryptedString.trim().length() == 0)
			throw new IllegalArgumentException(
			        "unencrypted string was null or empty");

		try {
			SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			
			// encrypt
	        byte[] encryptedBytes = cipher.doFinal(unencryptedString.getBytes("UTF-8"));
	 
	        // convert encrypted bytes into a base64 encoded string
			Base64 encoder = new Base64();
	        String encryptedString = new String(encoder.encode(encryptedBytes), "UTF-8");// for store use, so must convert to string
	 
			
			return encryptedString;

		} catch (Exception e) {
			Logger.getLogger(StringEncrypter.class.getName()).log(Level.SEVERE, "Unable to encrypt password", e);
			return "";
		}
	}

	public String decrypt(String encryptedString) throws IllegalArgumentException {
		if (encryptedString == null || encryptedString.trim().length() <= 0)
			throw new IllegalArgumentException(
			        "encrypted string was null or empty");

		try {
			SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			// Convert the base64 encoded string into bytes
			Base64 encoder = new Base64();
			byte[] encryptedBytes = encoder.decode(encryptedString.getBytes("UTF-8"));
			
			//decrypt
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
			
			// Convert decrypted bytes into a String
	        String decryptedString = new String(decryptedBytes, "UTF-8");

			return decryptedString;
		} catch (Exception e) {
			Logger.getLogger(StringEncrypter.class.getName()).log(Level.SEVERE, "Unable to decrypt password", e);
			return "";
		}
	}


	public static class EncryptionException extends Exception {
		public EncryptionException(Throwable t) {
			super(t);
		}
	}
}