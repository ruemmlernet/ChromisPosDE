/*
 * Copyright (c) 2019
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

package com.cryptovision.SEAPI;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Properties;

import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid;
import com.cryptovision.SEAPI.exceptions.ErrorUpdateTimeFailed;
import com.cryptovision.SEAPI.exceptions.GetInstanceException;
import com.cryptovision.SEAPI.exceptions.SEException;
import com.cryptovision.SEAPI.transport.Transport;
import com.cryptovision.SEAPI.transport.Transport.Command;
import com.cryptovision.SEAPI.transport.Transport.ConfigData;
import com.cryptovision.SEAPI.transport.Transport.KeyData;
import com.cryptovision.SEAPI.transport.Transport.Status;

/**
 * @author "Markus Ethen <markus.ethen@cryptovision.com>"
 */
public class TSEConnector extends TSE {

	private static final byte[] VERSION = new byte[] { 2, 3, 1 };
	private static final String VERSION_STRING = "cryptovision Java SE-API v2.3.1";
	private static final long UINT32_MAX = (long) 0xFFFFFFFFL;

	final Transport transport;
	final String TSE_VERSION;
	final byte[] TSE_SERIAL;

	TSEConnector(Transport t) throws SEException {
		transport = t;
		TSE_VERSION = null;
		TSE_SERIAL = null;
	}

	TSEConnector(Properties props) throws SEException {
		try {
			transport = Transport.getInstance(props);
		} catch (FileNotFoundException e) {
			throw new GetInstanceException(e);
		} catch (IOException e) {
			throw new GetInstanceException(e);
		}
		try {
			transport.flush();
			Object[] result = transport.send(Command.Start);
			TSE_VERSION = (String) result[0];
			if(TSE_VERSION.contains("f44a9e") || TSE_VERSION.contains("aee640")
			|| TSE_VERSION.contains("52376")
			|| TSE_VERSION.contains("462076") || TSE_VERSION.contains("056817") || TSE_VERSION.contains("379178") 
			|| TSE_VERSION.contains("430527") || TSE_VERSION.contains("966416") || TSE_VERSION.contains("021148") || TSE_VERSION.contains("932479"))
				throw new GetInstanceException(VERSION_STRING + " should be used with certified TSE or latest Engineering Samples firmware only");
			TSE_SERIAL = (byte[]) result[1];
		} catch (SEException e) {
			try {
				transport.close();
			} catch (IOException e1) {
				throw new GetInstanceException(e);
			}
			throw e;
		} catch (IOException e) {
			try {
				transport.close();
			} catch (IOException e1) { }
			throw new GetInstanceException(e);
		}
	}

	@Override
	public String getImplementationVersionString() {
		return VERSION_STRING;
	}

	@Override
	public byte[] getImplementationVersion() {
		return VERSION;
	}

	@Override
	public byte[] getUniqueId() {
		return Arrays.copyOf(TSE_SERIAL, TSE_SERIAL.length);
	}

	@Override
	public String getFirmwareId() throws SEException {
		return TSE_VERSION;
	}

	@Override
	public String getCertificationId() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.CertificationId);
		return (String) data[0];
	}

	/**
	 * @return the transport
	 */
	public Transport getTransport() {
		return transport;
	}

	@Override
	public void close() throws IOException, SEException {
		try {
			transport.send(Command.Shutdown);
		} finally {
			transport.close();
		}
	}

	static byte[] long2array(long l, int length) {
		byte[] val = BigInteger.valueOf(l).toByteArray();
		byte[] data = new byte[length];
		System.arraycopy(val, 0, data, data.length-val.length, val.length);
		return data;
	}

	@Override
	public AuthenticateUserResult authenticateUser(String userId, byte[] pin) throws SEException {
		Object[] data = transport.send(Command.AuthenticateUser, userId, pin);
		AuthenticateUserResult result = new AuthenticateUserResult();
		result.authenticationResult = AuthenticationResult.from((Byte) data[0]);
		result.remainingRetries = (Short) data[1];
		return result;
	}

	@Override
	public LCS getLifeCycleState() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.LifeCycleState);
		return LCS.from(((byte[]) data[0])[0]);
	}

	@Override
	public boolean[] getPinStatus() throws SEException {
		boolean[] result = new boolean[4];
		Object[] data = transport.send(Command.GetPinStates);
		byte[] bytes = (byte[]) data[0];
		for(int i = 0; i < result.length; i++)
			result[i] = bytes[i] != 0;
		return result;
	}

	@Override
	public void initializePinValues(byte[] adminPIN, byte[] adminPUK, byte[] timePIN, byte[] timePUK)
			throws SEException {
		transport.send(Command.InitializePins, adminPUK, adminPIN, timePUK, timePIN);
	}

	@Override
	public void initialize() throws SEException {
		transport.send(Command.Initialize);
	}

	@Override
	public void deactivateTSE() throws SEException {
		transport.send(Command.Deactivate);
	}

	@Override
	public void activateTSE() throws SEException {
		transport.send(Command.Activate);
	}

	@Override
	public void updateTime(long unixTime) throws SEException {
		if(unixTime > 4765129200L) // 01.01.2121
			throw new ErrorUpdateTimeFailed("SE-API sanity check failed");
		byte[] time = BigInteger.valueOf(unixTime).toByteArray();
		transport.send(Command.UpdateTime, time);
	}

	@Override
	public void mapERStoKey(String clientId, byte[] serialNumberKey) throws SEException {
		if(clientId.getBytes().length > 30)
			throw new ErrorTSECommandDataInvalid();
		transport.send(Command.MapERStoKey, clientId, serialNumberKey);
	}

	@Override
	public void unmapERS(String clientId) throws SEException {
		if(clientId.getBytes().length > 30)
			throw new ErrorTSECommandDataInvalid();
		transport.send(Command.MapERStoKey, clientId, null);
	}
	
	@Override
	public StartTransactionResult startTransaction(String clientId, byte[] processData, String processType,
			byte[] additionalData) throws SEException {
		Object[] data = transport.send(Command.StartTransaction, clientId, processData, processType, additionalData);
		StartTransactionResult result = new StartTransactionResult();
		result.transactionNumber = new BigInteger(1, (byte[]) data[0]).longValue();
		result.signatureCounter = new BigInteger(1, (byte[]) data[1]).longValue();
		result.logTime = new BigInteger(1, (byte[]) data[2]).longValue();
		result.signatureValue = (byte[]) data[3];
		result.serialNumber = (byte[]) data[4];
		return result;
	}

	@Override
	public UpdateTransactionResult updateTransaction(String clientId, long transactionNumber, byte[] processData,
			String processType) throws SEException {
		Object[] data = transport.send(Command.UpdateTransaction, BigInteger.valueOf(transactionNumber).toByteArray(), clientId, processData, processType);
		UpdateTransactionResult result = new UpdateTransactionResult();
		result.signatureCounter = new BigInteger(1, (byte[]) data[0]).longValue();
		result.logTime = new BigInteger(1, (byte[]) data[1]).longValue();
		result.signatureValue = (byte[]) data[2];
		result.serialNumber = (byte[]) data[3];
		return result;
	}

	@Override
	public FinishTransactionResult finishTransaction(String clientId, long transactionNumber, byte[] processData,
			String processType, byte[] additionalData) throws SEException {
		Object[] data = transport.send(Command.FinishTransaction, BigInteger.valueOf(transactionNumber).toByteArray(), clientId, processData, processType, additionalData);
		FinishTransactionResult result = new FinishTransactionResult();
		result.signatureCounter = new BigInteger(1, (byte[]) data[0]).longValue();
		result.logTime = new BigInteger(1, (byte[]) data[1]).longValue();
		result.signatureValue = (byte[]) data[2];
		result.serialNumber = (byte[]) data[3];
		return result;
	}

	@Override
	public byte[] exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords) throws SEException, IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			if(clientId == null)               clientId = "";
			if(transactionNumber == null)      transactionNumber = (long) -1;
			if(startTransactionNumber == null) startTransactionNumber = (long) 0;
			if(endTransactionNumber == null)   endTransactionNumber = UINT32_MAX;
			if(startDate == null)              startDate = (long) 0;
			if(endDate == null)                endDate = UINT32_MAX;
			if(maximumNumberRecords == null)   maximumNumberRecords = UINT32_MAX;
			transport.sendAndWrite(Command.ExportData, stream, clientId, transactionNumber, startTransactionNumber, endTransactionNumber, startDate, endDate, maximumNumberRecords);
		} finally {
			stream.close();
		}
		return stream.toByteArray();
	}

	@Override
	public void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, String fileName) throws SEException, IOException {
		FileOutputStream stream = new FileOutputStream(fileName);
		try {
			if(clientId == null)               clientId = "";
			if(transactionNumber == null)      transactionNumber = (long) -1;
			if(startTransactionNumber == null) startTransactionNumber = (long) 0;
			if(endTransactionNumber == null)   endTransactionNumber = UINT32_MAX;
			if(startDate == null)              startDate = (long) 0;
			if(endDate == null)                endDate = UINT32_MAX;
			if(maximumNumberRecords == null)   maximumNumberRecords = UINT32_MAX;
			transport.sendAndWrite(Command.ExportData, stream, clientId, transactionNumber, startTransactionNumber, endTransactionNumber, startDate, endDate, maximumNumberRecords);
		} finally {
			stream.close();
		}
	}

	@Override
	public void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, OutputStream stream) throws SEException {
		if(clientId == null)               clientId = "";
		if(transactionNumber == null)      transactionNumber = (long) -1;
		if(startTransactionNumber == null) startTransactionNumber = (long) 0;
		if(endTransactionNumber == null)   endTransactionNumber = UINT32_MAX;
		if(startDate == null)              startDate = (long) 0;
		if(endDate == null)                endDate = UINT32_MAX;
		if(maximumNumberRecords == null)   maximumNumberRecords = UINT32_MAX;
		transport.sendAndWrite(Command.ExportData, stream, clientId, transactionNumber, startTransactionNumber, endTransactionNumber, startDate, endDate, maximumNumberRecords);
	}

	@Override
	public void exportMoreData(byte[] serialNumberKey, Long previousSignatureCounter, Long maximumNumberRecords, OutputStream stream) throws SEException {
		transport.sendAndWrite(Command.ExportMoreData, stream, serialNumberKey, previousSignatureCounter, maximumNumberRecords);
	}

	/* (non-Javadoc)
	 * @see com.cryptovision.SEAPI.TSE#deleteStoredDataUpTo(byte[], java.lang.Long)
	 */
	@Override
	public void deleteStoredDataUpTo(byte[] serialNumberKey, Long signatureCounter) throws SEException {
		transport.send(Command.DeleteUpTo, serialNumberKey, signatureCounter);
	}

	@Override
	public void logOut(String userId) throws SEException {
		transport.send(Command.Logout, userId);
	}

	@Override
	public void disableSecureElement() throws SEException {
		transport.send(Command.Disable);
	}

	@Override
	public byte[] exportCertificates() throws SEException {
		Object[] data = transport.send(Command.GetCertificates);
		byte[] tar = (byte[]) data[0];
		return Arrays.copyOfRange(tar, 0, tar.length+2*512);
	}

	@Override
	public long getCertificateExpirationDate(byte[] serialNumberKey) throws SEException {
		Object[] data = transport.send(Command.GetKeyData, KeyData.ExpirationDate, serialNumberKey);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public byte[] getERSMappings() throws SEException {
		Object[] data = transport.send(Command.GetERSMappings );
		return (byte[]) data[0];
	}

	@Override
	public byte[] readLogMessage() throws SEException {
		Object[] data = transport.send(Command.ReadLogMessage);
		return (byte[]) data[0];
	}

	@Override
	public byte[] exportSerialNumbers() throws SEException {
		Object[] data = transport.send(Command.GetSerialNumbers);
		return (byte[]) data[0];
	}

	@Override
	public int getTimeSyncInterval() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.TimeUpdateInterval);
		return new BigInteger(1, (byte[]) data[0]).intValue();
	}

	@Override
	public long getMaxNumberOfClients() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.MaxClients);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getCurrentNumberOfClients() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.NumClients);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getMaxNumberOfTransactions() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.MaxTransactions);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getCurrentNumberOfTransactions() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.NumTransactions);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getTransactionCounter() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.TransactionCounter);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getTotalLogMemory() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.TotalMemory);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public long getAvailableLogMemory() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.AvailableMemory);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public int getWearIndicator() throws SEException {
		Object[] data = transport.send(Command.GetWearIndicator);
		return (Short) data[0];
	}

	@Override
	public long getSignatureCounter(byte[] serialNumberKey) throws SEException {
		Object[] data = transport.send(Command.GetKeyData, KeyData.SignatureCounter, serialNumberKey);
		return new BigInteger(1, (byte[]) data[0]).longValue();
	}

	@Override
	public byte[] exportPublicKey(byte[] serialNumberKey) throws SEException {
		Object[] data = transport.send(Command.GetKeyData, KeyData.PublicKey, serialNumberKey);
		byte[] puk = (byte[]) data[0];
		if(puk.length == 254) puk = Arrays.copyOfRange(puk, 188, 188+65);
		return puk;
	}

	@Override
	public long[] getOpenTransactions() throws SEException {
		Object[] data = transport.send(Command.GetStatus, Status.OpenTransactions);
		return (long[]) data[0];
	}

	@Override
	public UpdateVariants getSupportedTransactionUpdateVariants() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.SupportedUpdateVariants);
		return UpdateVariants.from(((byte[]) data[0])[0]);
	}

	@Override
	public SyncVariants getTimeSyncVariant() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.SupportedTimeFormats);
		return SyncVariants.from(((byte[]) data[0])[0]);
	}

	@Override
	public byte[] getSignatureAlgorithm() throws SEException {
		Object[] data = transport.send(Command.GetConfigData, ConfigData.SignatureAlgorithm);
		return ((byte[]) data[0]);
	}

	@Override
	public void deleteStoredData() throws SEException {
		transport.send(Command.Erase);
	}

	@Override
	public UnblockUserResult unblockUser(String userId, byte[] puk, byte[] newPin) throws SEException {
		Object[] data = transport.send(Command.UnblockUser, userId, puk, newPin);
		UnblockUserResult result = new UnblockUserResult();
		result.authenticationResult = AuthenticationResult.from((Byte) data[0]);
		return result;
	}
}
