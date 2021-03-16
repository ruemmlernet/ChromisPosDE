/*
 * Copyright (c) 2020
 * cv cryptovision GmbH
 * Munscheidstr. 14
 * 45886 Gelsenkirchen
 * Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Java package for the TR-03151 Secure Element API by cryptovision (Java version)
 */
package com.cryptovision.SEAPI;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import com.cryptovision.SEAPI.exceptions.ErrorDescriptionSetByManufacturer;
import com.cryptovision.SEAPI.exceptions.ErrorIdNotFound;
import com.cryptovision.SEAPI.exceptions.ErrorRestoreFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSECommunicationFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSecureElementDisabled;
import com.cryptovision.SEAPI.exceptions.ErrorTSEResponseDataInvalid;
import com.cryptovision.SEAPI.exceptions.SEException;

/**
 * TR-03151 Secure Element API
 * <p>
 * Please refer to BSI specification for parameter and exceptions definition where not explicitly defined below.
 * <p>
 * Use {@link #getInstance(String)} to start.
 * {@link #close()} shall be used on application shutdown (latest), to close any transport internal resources.
 *
 * @author cv cryptovision GmbH
 * @version TR-03151 Version 1.0.1,<br>cv version 2.2
 */
public abstract class TSE {

	private static final String SE_API_VERSION_STRING = "cryptovision SE-API v2.3";
	private static final byte[] SE_API_VERSION = new byte[] { 2, 3 };

	/**
	 * @param configFileName name of configuration file, either relative or absolute. For documentation of file content, please see @ref TLConfigFile
	 * @return {@link TSE} instance.
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 */
	public static TSE getInstance(String configFileName) throws SEException, FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileReader(configFileName));
		props.setProperty("filename", configFileName);

		return getInstance(props);
	}

	/**
	 * @param props Java Properties as you would define them in a config file @ref TLConfigFile
	 * @return {@link TSE} instance.
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 */
	public static TSE getInstance(Properties props) throws SEException, FileNotFoundException, IOException {
		try {
			String connector = props.getProperty("connector");
			if(connector == null)
				connector = "com.cryptovision.SEAPI.TSEConnector";

			return (TSE) Class.forName(connector).getDeclaredConstructor(Properties.class).newInstance(props);
		} catch (InvocationTargetException e) {
			if(e.getTargetException() instanceof RuntimeException)
				throw (RuntimeException) e.getTargetException();
			else if(e.getTargetException() instanceof SEException)
				throw (SEException) e.getTargetException();
			else
				throw new Error(e.getTargetException());
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * @return API version as String
	 */
	public static String getApiVersionString() {
		return SE_API_VERSION_STRING;
	}

	/**
	 * @return API major/minor version as byte[2]
	 */
	public static byte[] getApiVersion() {
		return SE_API_VERSION;
	}

	/**
	 * @return implementation version as String
	 */
	public abstract String getImplementationVersionString();

	/**
	 * @return implementation version as byte[3] where first two bytes match API version
	 */
	public abstract byte[] getImplementationVersion();

	/**
	 * @return some identifier guaranteed to be unambiguous for every cryptovision TSE
	 * @since 2.1
	 */
	public abstract byte[] getUniqueId();

	/**
	 * @return certification ID as assigned by BSI ("BSI-K-TR-0374-2019" for cryptovision TSE)
	 * @since 2.1
	 */
	public abstract String getCertificationId() throws SEException;

	/**
	 * @return some firmware identifier
	 * @since 2.1
	 */
	public abstract String getFirmwareId() throws SEException;

	/**
	 * TSE Life Cycle State
	 */
	public enum LCS {
		unknown, 		/**< undefined life cycle state	*/
		notInitialized,	/**< {@link TSE#initialize() initialize()} not called yet */
		noTime,			/**< time not set */
		active,			/**< ready to sign transactions */
		deactivated,	/**< after {@link TSE#deactivateTSE() deactivateTSE()} */
		disabled		/**< after {@link TSE#disableSecureElement() disableSecureElement()} */
		;				//	KEEP on separate line to mollify Doxygen

		public static LCS from(byte value) throws ErrorTSEResponseDataInvalid {
			for(LCS v : values())
				if(v.ordinal() == value)
					return v;
			throw new ErrorTSEResponseDataInvalid();
		}
	};

	/**
	 * @return current life cycle state
	 * @throws ErrorSECommunicationFailed
	 * @since 2.0
	 */
	public abstract LCS getLifeCycleState() throws SEException;

	/**
	 * @return "PIN in transport state" per Admin PIN, Admin PUK, TimeAdmin PIN, TimeAdmin PUK
	 * @throws ErrorSECommunicationFailed
	 * @since 0.97
	 */
	public abstract boolean[] getPinStatus() throws SEException;

	/**
	 * Set PIN/PUK values if in transport state.
	 *
	 * @note  The length of either PIN must be exactly  8 bytes!
	 * @note  The length of either PUK must be exactly 10 bytes!
	 *
	 * @param adminPIN 8 bytes or <code>null</code> to not touch it
	 * @param adminPUK 10 bytes or <code>null</code> to not touch it
	 * @param timePIN 8 bytes or <code>null</code> to not touch it
	 * @param timePUK 10 bytes or <code>null</code> to not touch it
	 * @throws ErrorTSECommandDataInvalid
	 * @throws ErrorSECommunicationFailed
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStorageFailure
	 * @since 0.97
	 */
	public abstract void initializePinValues(byte[] adminPIN, byte[] adminPUK, byte[] timePIN, byte[] timePUK) throws SEException;

	/**
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorDescriptionNotSetByManufacturer
	 * @throws ErrorDescriptionSetByManufacturer
	 */
	public abstract void initialize() throws SEException;
	/**
	 * @param  description	Textual description for this TSE. Pass <code>null</code> as the cryptovision TSE does not support (re-) naming the TSE.
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorDescriptionNotSetByManufacturer
	 * @throws ErrorDescriptionSetByManufacturer
	 * @since 2.0
	 */
	public final void initialize(String description) throws SEException {
		if(description != null)
			throw new ErrorDescriptionSetByManufacturer();
		initialize();
	}

	/**
	 * This command can be used to temporarily deactivate the TSE.
	 * It is meant to be used as protection of an entirely personalized TSE (i.e. with PINs assigned and ERSs mapped) during transport.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiDeactivated
	 * @since 0.97
	 */
	public abstract void deactivateTSE() throws SEException;

	/**
	 * This command works identical to the {@link #initialize()} command.
	 * It is meant to enable an entirely personalized TSE (i.e. with PINs assigned and ERSs mapped) after final installation in the ERS.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiNotDeactivated
	 * @since 0.97
	 */
	public abstract void activateTSE() throws SEException;

	/**
	 * Map the serial number of an ERS to a signature key.
	 * <p>
	 * This assigns an existing private key to a client (cash register).<br>
	 * Method can also be used to delete such a mapping, but see {@link #unmapERS(String)}.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * <li>Requires time set.
	 * <li>Pass <code>null</code> for <code>serialNumberKey</code> to delete a mapping.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 * @param	serialNumberKey	ID of the key to be mapped (SHA256 hash value of the public key) or<br><code>null</code> to remove a client-mapping
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorNoSuchKey unknown <code>serialNumberKey</code>
	 * @throws ErrorSECommunicationFailed with <code>serialNumberKey == null</code>: clientId not mapped
	 * @throws ErrorERSalreadyMapped
	 */
	public abstract void mapERStoKey(String clientId, byte[] serialNumberKey) throws SEException;

	/**
	 * Unmap the serial number of an ERS.
	 * <p>
	 * This deletes a client (cash register) to private key mapping.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSECommunicationFailed clientId not mapped
	 * @since 2.3
	 */
	public abstract void unmapERS(String clientId) throws SEException;
	
	/**
	 * @param	unixTime	new time to set in UnixTime format
	 *
	 * @throws ErrorUpdateTimeFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public abstract void updateTime(long unixTime) throws SEException;

	/**
	 * @throws ErrorDisableSecureElementFailed
	 * @throws ErrorTimeNotSet
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public abstract void disableSecureElement() throws SEException;

	/**
	 * maximum size of <code>processData</code>.
	 * @since 0.99+
	 * @deprecated see {@link #MAX_SIZE_TRANSPORT_LAYER}
	 */
	public static final int MAX_LEN_PROCESS_DATA    = 8096;
	/**
	 * the actual limitation is on the overall size of the TSE command
	 * @since 2.0
	 */
	public static final int MAX_SIZE_TRANSPORT_LAYER = 8192;

	/**
	 * common fields in xxxTransaction return values
	 */
	static class TransactionResult {
		public long logTime;			/**< time logged by the CSP */
		public byte[] serialNumber;		/**< signature key serial number */
		public long signatureCounter;	/**< signature counter */
		public byte[] signatureValue;   /**< signature */
	}

	/**
	 * {@link TSE#startTransaction() startTransaction()} return value
	 */
	public static class StartTransactionResult extends TransactionResult {
		public long transactionNumber;	/**< transaction number assigned */
	}

	/**
	 * @throws ErrorStartTransactionFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract StartTransactionResult startTransaction(String clientId, byte[] processData, String processType, byte[] additionalData) throws SEException;

	/**
	 * {@link TSE#updateTransaction() updateTransaction()} return value
	 */
	public static class UpdateTransactionResult extends TransactionResult { }
	/**
	 * @throws ErrorUpdateTransactionFailed
	 * @throws ErrorLogMessageRetrievalFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorNoTransaction
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract UpdateTransactionResult updateTransaction(String clientId, long transactionNumber, byte[] processData, String processType) throws SEException;

	/**
	 * {@link TSE#finishTransaction() finishTransaction()} return value
	 */
	public static class FinishTransactionResult extends TransactionResult { }
	/**
	 * @throws ErrorFinishTransactionFailed
	 * @throws ErrorNoTransaction missing in BSI API
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract FinishTransactionResult finishTransaction(String clientId, long transactionNumber, byte[] processData, String processType, byte[] additionalData) throws SEException;

	/**
	 * @return list of dangling ("open") transactions from the SE.
	 *
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @since 0.97
	 */
	public abstract long[] getOpenTransactions() throws SEException;

	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract byte[] exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords) throws SEException, IOException;
	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param 	fileName 				Export data written to disk. File must not yet exist.
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, String fileName) throws SEException, IOException;
	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param	stream					Export data is written to this instance.
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, OutputStream stream) throws SEException, IOException;
	/**
	 * Use -1 (transactionNumber) / 0 and Long.MAX_VALUE (for long parameters) to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws IOException
	 * @deprecated Use {@link #exportData(String, Long, Long, Long, Long, Long, Long)} instead.
	 */
	public final byte[] exportData(String clientId, long transactionNumber, long startTransactionNumber, long endTransactionNumber, long startDate, long endDate, long maximumNumberRecords) throws SEException, IOException {
		Long transactionNumberL      = transactionNumber;
		Long startTransactionNumberL = startTransactionNumber;
		Long endTransactionNumberL   = endTransactionNumber;
		Long startDateL              = startDate;
		Long endDateL                = endDate;
		Long maximumNumberRecordsL   = maximumNumberRecords;
		if(transactionNumber == Long.MAX_VALUE)      transactionNumberL = null;
		if(startTransactionNumber == 0
		|| startTransactionNumber == Long.MAX_VALUE) startTransactionNumberL = null;
		if(endTransactionNumber == Long.MAX_VALUE)   endTransactionNumberL = null;
		if(startDate == 0
		|| startDate == Long.MAX_VALUE)              startDateL = null;
		if(endDate == Long.MAX_VALUE)                endDateL = null;
		if(maximumNumberRecords == Long.MAX_VALUE)   maximumNumberRecordsL = null;
		return exportData(clientId, transactionNumberL, startTransactionNumberL, endTransactionNumberL, startDateL, endDateL, maximumNumberRecordsL);
	}

	/**
	 * OuputStream extension to receive total size of exported data.
	 *
	 * Implement {@link TSEOutputStream} to receive a {@link #total(long)} callback call
	 * from {@link #exportData(String, Long, Long, Long, Long, Long, Long, OutputStream)} and
	 * {@link #exportMoreData(byte[], Long, Long, OutputStream)}.
	 * @since 2.1
	 */
	public abstract static class TSEOutputStream extends OutputStream {
		/** default implementation saves total number of bytes */
		protected long size;
		/**
		 * Called by export methods to provide total number of bytes in this export.
		 *
		 * @param size
		 */
		public void total(long size) {
			// default implementation: save value
			this.size = size;
		}
	}
	/**
	 * Continue data export, e.g. immediately after last seen log entry.
	 *
	 * @param 	serialNumberKey          serial of key used in previous log entry.
	 * @param 	previousSignatureCounter last seen signature counter
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param	stream					Export data is written to this instance.
	 *
	 * @throws ErrorNoSuchKey
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @since 2.0
	 */
	public abstract void exportMoreData(byte[] serialNumberKey, Long previousSignatureCounter, Long maximumNumberRecords, OutputStream stream) throws SEException;

	/**
	 * Delete oldest data.
	 *
	 * @param 	serialNumberKey			serial of key
	 * @param 	signatureCounter		highest signature counter to delete
	 *
	 * @throws ErrorNoSuchKey
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @since 2.1
	 */
	public abstract void deleteStoredDataUpTo(byte[] serialNumberKey, Long signatureCounter) throws SEException;

	/**
	 * @throws ErrorExportCertFailed
	 * @throws ErrorSeApiNotInitialized
	 * @since 0.97
	 */
	public abstract byte[] exportCertificates() throws SEException;
	/**
	 * @deprecated typo in method name compared to TR-03151, use {@link #exportCertificates()}
	 */
	public final byte[] exportCertificate() throws SEException { return exportCertificates(); }

	/**
	 * @param serialNumberKey
	 * @return certificate expiration date encoded as unix time
	 * @note expiration date is X.509 "notAfter" field +1
	 * @since 2.0
	 */
	public abstract long getCertificateExpirationDate(byte[] serialNumberKey) throws SEException;

	/**
	 * @return ASN.1 encoded sequence of mappings ERS to serial number
	 * @since 2.0
	 */
	public abstract byte[] getERSMappings() throws SEException;

	/**
	 * @throws ErrorRestoreFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public final void restoreFromBackup(byte[] restoreData) throws SEException {
		throw new ErrorRestoreFailed("unimplemented");
	}

	/**
	 * @throws ErrorNoLogMessage
	 * @throws ErrorReadingLogMessage
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract byte[] readLogMessage() throws SEException;

	/**
	 * @throws ErrorExportSerialNumbersFailed
	 * @throws ErrorSeApiNotInitialized
	 */
	public abstract byte[] exportSerialNumbers() throws SEException;

	/**
	 * @throws ErrorGetMaxNumberOfClientsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getMaxNumberOfClients() throws SEException;

	/**
	 * @throws ErrorGetCurrentNumberOfClientsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getCurrentNumberOfClients() throws SEException;

	/**
	 * @throws ErrorGetMaxNumberTransactionsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getMaxNumberOfTransactions() throws SEException;

	/**
	 * @throws ErrorGetCurrentNumberOfTransactionsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getCurrentNumberOfTransactions() throws SEException;

	/**
	 * @return current transaction counter (last used value)
	 * @since 2.0
	 */
	public abstract long getTransactionCounter() throws SEException;

	/**
	 * @return size of log memory in bytes.
	 * @since 2.0
	 */
	public abstract long getTotalLogMemory() throws SEException;

	/**
	 * @return remaining log memory in bytes.
	 * @since 2.0
	 */
	public abstract long getAvailableLogMemory() throws SEException;

	/**
	 * @return For values below 100, typical data retention is more than 10 years. Bigger values indicate shorter data retention, but at least 1 year.
	 * @since 2.0
	 */
	public abstract int getWearIndicator() throws SEException;

	/**
	 * @return current signature counter (last used value) for key
	 * @since 2.0
	 */
	public abstract long getSignatureCounter(byte[] serialNumberKey) throws SEException;

	/**
	 * @throws ErrorSeApiNotInitialized
	 * @since 2.0
	 */
	public abstract byte[] exportPublicKey(byte[] serialNumberKey) throws SEException;

	/**
	 * List of update variants.
	 *
	 * @note Only signed updates are supported.
	 */
	public enum UpdateVariants {
		signed, 			/**< UpdateTransaction returns signature */
		unsigned, 			/**< UpdateTransaction does not return a signature */
		signedAndUnsigned	/**< Both signed and unsigned are supported */
		;					//	KEEP on separate line to mollify Doxygen

		public static UpdateVariants from(byte value) throws ErrorTSEResponseDataInvalid {
			for(UpdateVariants v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * @throws ErrorGetSupportedUpdateVariantsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract UpdateVariants getSupportedTransactionUpdateVariants() throws SEException;

	/**
	 * Time sync variants according to spec.
	 *
	 * @note cryptovision TSE always uses {@link #unixTime}
	 */
	public enum SyncVariants {
		noInput,			/**< */
		utcTime, 			/**< UTC time */
		generalizedTime, 	/**< generalized time */
		unixTime			/**< unix time */
		;					//	KEEP on separate line to mollify Doxygen

		public static SyncVariants from(byte value) throws ErrorTSEResponseDataInvalid {
			for(SyncVariants v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * @return {@link SyncVariants#unixTime}
	 * @since 2.0
	 */
	public abstract SyncVariants getTimeSyncVariant() throws SEException;

	/**
	 * @return ASN.1 encoded signature algorithm as encoded into signed data
	 * @since 2.0
	 */
	public abstract byte[] getSignatureAlgorithm() throws SEException;

	/**
	 * @return proposed update interval for the CSP time base (number of seconds)
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 * @since 2.0
	 */
	public abstract int getTimeSyncInterval() throws SEException;

	/**
	 * @return proposed update interval for the CSP time base (number of seconds)
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 * @since 0.97
	 * @deprecated see {@link #getTimeSyncInterval()}
	 */
	public final int getTimeUpdateInterval() throws SEException {
		return getTimeSyncInterval();
	}

	/**
	 * @throws ErrorDeleteStoredDataFailed
	 * @throws ErrorUnexportedStoredData
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @note (only) TR0-03153 v1.0.1 Table 6 calls it "deleteSecuredData".
	 */
	public abstract void deleteStoredData() throws SEException;

	/**
	 * Possible authentication results
	 */
	public enum AuthenticationResult {
		ok,				/**< no error */
		failed,			/**< authentication failed */
		pinIsBlocked,	/**< the PIN is blocked */
		unknownUserId,	/**< the userId is unknown */
		error 			/**< some other error */
		;				//	KEEP on separate line to mollify Doxygen

		public static AuthenticationResult from(byte value) throws SEException {
			for(AuthenticationResult v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * {@link TSE#authenticateUser() authenticateUser()} return value
	 */
	public class AuthenticateUserResult {
		public AuthenticationResult authenticationResult;	/**< result of the authentication */
		public short remainingRetries;						/**< remaining retries of the PIN */
	};
	/**
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract AuthenticateUserResult authenticateUser(String userId, byte[] pin) throws SEException;


	/**
	 * Log out a user.
	 * @deprecated see {@link #logOut(String)}
	 *
	 * @throws ErrorUserIdNotManaged
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorUserIdNotAuthenticated -> we use ErrorUserNotAuthorized instead
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled

	 */
	public final void logout(String userId) throws SEException {
		logout(userId);
	}

	/**
	 * Log out a user.
	 *
	 * @throws ErrorUserIdNotManaged
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorUserIdNotAuthenticated -> we use ErrorUserNotAuthorized instead
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract void logOut(String userId) throws SEException;

	/**
	 * {@link TSE#unblockUser() unblockUser()} return value
	 */
	public class UnblockUserResult {
		public AuthenticationResult authenticationResult;	/**< result of the authentication */
	};

	/**
	 * @throws ErrorUserIdNotManaged missing in BSI API
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorTSECommandDataInvalid
	 */
	public abstract UnblockUserResult unblockUser(String userId, byte[] puk, byte[] newPin) throws SEException;

	/**
	 * shut down the transport layer.
	 */
	public abstract void close() throws IOException, SEException;
}
