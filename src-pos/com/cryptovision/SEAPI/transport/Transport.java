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

package com.cryptovision.SEAPI.transport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;

import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.exceptions.ErrorCertificateExpired;
import com.cryptovision.SEAPI.exceptions.ErrorFinishTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorIdNotFound;
import com.cryptovision.SEAPI.exceptions.ErrorNoDataAvailable;
import com.cryptovision.SEAPI.exceptions.ErrorNoERS;
import com.cryptovision.SEAPI.exceptions.ErrorNoKey;
import com.cryptovision.SEAPI.exceptions.ErrorNoStartup;
import com.cryptovision.SEAPI.exceptions.ErrorNoStorage;
import com.cryptovision.SEAPI.exceptions.ErrorNoTransaction;
import com.cryptovision.SEAPI.exceptions.ErrorParameterMismatch;
import com.cryptovision.SEAPI.exceptions.ErrorRetrieveLogMessageFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSECommunicationFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSeApiNotDeactivated;
import com.cryptovision.SEAPI.exceptions.ErrorSeApiNotInitialized;
import com.cryptovision.SEAPI.exceptions.ErrorSeApiDeactivated;
import com.cryptovision.SEAPI.exceptions.ErrorSecureElementDisabled;
import com.cryptovision.SEAPI.exceptions.ErrorSigningSystemOperationDataFailed;
import com.cryptovision.SEAPI.exceptions.ErrorStartTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorStorageFailure;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommunicationError;
import com.cryptovision.SEAPI.exceptions.ErrorTSEResponseDataInvalid;
import com.cryptovision.SEAPI.exceptions.ErrorTimeNotSet;
import com.cryptovision.SEAPI.exceptions.ErrorTooManyRecords;
import com.cryptovision.SEAPI.exceptions.ErrorTransactionNumberNotFound;
import com.cryptovision.SEAPI.exceptions.ErrorTransport;
import com.cryptovision.SEAPI.exceptions.ErrorUnexportedStoredData;
import com.cryptovision.SEAPI.exceptions.ErrorUpdateTimeFailed;
import com.cryptovision.SEAPI.exceptions.ErrorUpdateTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorUserIdNotManaged;
import com.cryptovision.SEAPI.exceptions.ErrorUserNotAuthenticated;
import com.cryptovision.SEAPI.exceptions.ErrorUserNotAuthorized;
import com.cryptovision.SEAPI.exceptions.GetInstanceException;
import com.cryptovision.SEAPI.exceptions.SEException;

public abstract class Transport {

	protected int IO_TIMEOUT = 5000; // 5 sec default...

	/**
	 * @deprecated for internal / legacy use only.
	 */
	public static Transport getInstance(String configFileName) throws FileNotFoundException, IOException, GetInstanceException {
		Properties props = new Properties();
		props.load(new FileReader(configFileName));
		props.setProperty("filename", configFileName);

		return getInstance(props);
	}

	public static Transport getInstance(Properties props) throws FileNotFoundException, IOException, GetInstanceException {
		String transport = props.getProperty("transport");

		String filename = props.getProperty("filename");
		String location = filename == null ? "" : " in "+filename;
		String absLocation = filename == null ? "" : " in "+new File(filename).getAbsolutePath();

		if(transport == null)
			throw new GetInstanceException("no transport configured" + location);

		Transport t;
		try {
			Class<?> clazz = Class.forName("com.cryptovision.SEAPI.transport." + transport + "Transport");
			t = (Transport) clazz.getConstructors()[0].newInstance(props);
		} catch (ClassNotFoundException e) {
			throw new GetInstanceException("no such transport: "+transport);
		} catch (InvocationTargetException e) {
			Throwable c = e.getCause();
			if(c != null)
				if(c instanceof FileNotFoundException)
					throw new GetInstanceException("wrong path"+absLocation+" or device not available", (Exception) c);
				else if(c instanceof Exception)
					throw new GetInstanceException((Exception) c);
			throw new GetInstanceException(e);
		} catch (Exception e) {
			throw new GetInstanceException(e);
		}

		if(props.containsKey("timeout"))
			t.IO_TIMEOUT = Integer.parseInt(props.getProperty("timeout"))*1000;

		return t;
	}

	public enum Command {
		Start(0),

		GetPinStates(1),
		InitializePins(2),

		AuthenticateUser(3),
		UnblockUser(4),
		Logout(5),

		Initialize(6),
		UpdateTime(7),
		GetSerialNumbers(8),
		MapERStoKey(9),
		GetERSMappings(23),

		StartTransaction(10),
		UpdateTransaction(11),
		FinishTransaction(12),

		ExportData(13),
		ExportMoreData(22),
		GetCertificates(14),
		ReadLogMessage(15),
		Erase(16),
		DeleteUpTo(27),

		GetConfigData(17),
		GetStatus(18),
		GetKeyData(24),
		GetWearIndicator(25),

		Deactivate(19),
		Activate(20),
		Disable(21),


		Shutdown(255);

		public final short value;

		private Command(int value) {
			this.value = (short) value;
		}
	}

	public enum ConfigData {
		Version(0),
		SignatureAlgorithm(1),
		SupportedUpdateVariants(2),
		MaxKeys(3),
		MaxClients(4),
		MaxTransactions(5),
		TimeUpdateInterval(6),
		SupportedTimeFormats(7),
		CertificationId(8);

		public final short value;
		private ConfigData(int value) {
			this.value = (short) value;
		}
	}

	public enum Status {
		NumClients(0),
		NumTransactions(1),
		OpenTransactions(2),
		TransactionCounter(3),
		LifeCycleState(4),
		TotalMemory(5),
		AvailableMemory(6);

		public final short value;
		private Status(int value) {
			this.value = (short) value;
		}
	}

	public enum KeyData {
		SignatureCounter(0),
		ExpirationDate(1),
		PublicKey(2);

		public final short value;
		private KeyData(int value) {
			this.value = (short) value;
		}
	}

	protected static final byte BYTE = 1;
	protected static final byte BYTE_ARRAY = 2;
	protected static final byte SHORT = 3;
	protected static final byte STRING = 4;
	protected static final byte LONG_ARRAY = 5;

	private static final int TSE_MAX_SIZE_STRING     = 0x200;

	final byte[] encode(Command cmd, Object[] params) throws SEException {
		ByteBuffer o = ByteBuffer.allocate(TSE.MAX_SIZE_TRANSPORT_LAYER);
		o.put((byte) 0x5C);
		o.put((byte) 'T');
		o.putShort(cmd.value);
		o.putShort((short) 0); // filled in later

		for(int i = 0; i < params.length; i++) {
			Object val = params[i];

			if(val == null) {
				// TSE will reject if this parameter is not a byte array... null only allowed for byte arrays
				o.put(BYTE_ARRAY);
				o.putShort((short) 0);
				continue;
			}

			Class<? extends Object> type = val.getClass();

			if(Byte.TYPE.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
				o.put(BYTE);
				o.putShort((short) 1);
				o.put((Byte) val);
			} else if(byte[].class.isAssignableFrom(type)) {
				o.put(BYTE_ARRAY);
				byte[] v = (byte[]) val;
				if(v.length > TSE.MAX_SIZE_TRANSPORT_LAYER)
					throw new ErrorTSECommandDataInvalid("parameter length exceeded");
				o.putShort((short) v.length);
				o.put(v);
			} else if(Short.TYPE.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
				o.put(SHORT);
				o.putShort((short) 2);
				o.putShort((Short) val);
			} else if(Long.TYPE.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
				o.put(BYTE_ARRAY);
				if((Long) val == -1)
					o.putShort((short) 0);
				else {
					byte[] bytes = new BigInteger(Long.toHexString((Long) val), 16).toByteArray();
					o.putShort((short) bytes.length);
					o.put(bytes);
				}
			} else if(String.class.isAssignableFrom(type)) {
				o.put(STRING);
				byte[] data = ((String) val).getBytes();
				if(data.length > TSE_MAX_SIZE_STRING)
					throw new ErrorTSECommandDataInvalid("parameter length exceeded");
				o.putShort((short) data.length);
				o.put(data);
			} else if(ConfigData.class.isAssignableFrom(type)) {
				o.put(SHORT);
				o.putShort((short) 2);
				o.putShort(((ConfigData) val).value);
			} else if(Status.class.isAssignableFrom(type)) {
				o.put(SHORT);
				o.putShort((short) 2);
				o.putShort(((Status) val).value);
			} else if(KeyData.class.isAssignableFrom(type)) {
				o.put(SHORT);
				o.putShort((short) 2);
				o.putShort(((KeyData) val).value);
			} else
				throw new ErrorTSECommandDataInvalid("parameter type unimplemented");
		}

		o.putShort(4, (short) (((Buffer)o).position()-6));
		((Buffer)o).flip();
		byte[] adata = new byte[((Buffer)o).limit()];
		o.get(adata);

		return adata;
	}

	final void checkResult(short result) throws SEException {
		if(result >= 0)
			return;

		switch (result) {
		case (short) 0x8000: throw new ErrorSECommunicationFailed();
		case (short) 0x8001: throw new ErrorTSECommandDataInvalid();
		case (short) 0x8002: throw new ErrorTSEResponseDataInvalid();
		case (short) 0x8003: throw new ErrorSigningSystemOperationDataFailed();
		case (short) 0x8004: throw new ErrorRetrieveLogMessageFailed();
		case (short) 0x8005: throw new ErrorStorageFailure();
		case (short) 0x8006: throw new ErrorSecureElementDisabled();
		case (short) 0x8007: throw new ErrorUserNotAuthorized();
		case (short) 0x8008: throw new ErrorUserNotAuthenticated();
		case (short) 0x8009: throw new ErrorSeApiNotInitialized();
		case (short) 0x800A: throw new ErrorUpdateTimeFailed();
		case (short) 0x800B: throw new ErrorUserIdNotManaged();
		case (short) 0x800C: throw new ErrorStartTransactionFailed();
		case (short) 0x800D: throw new ErrorCertificateExpired();
		case (short) 0x800E: throw new ErrorNoTransaction();
		case (short) 0x800F: throw new ErrorUpdateTransactionFailed();
		case (short) 0x8010: throw new ErrorFinishTransactionFailed();
		case (short) 0x8011: throw new ErrorTimeNotSet();
		case (short) 0x8012: throw new ErrorNoERS();
		case (short) 0x8013: throw new ErrorNoKey();
		case (short) 0x8014: throw new ErrorSeApiNotDeactivated();
		case (short) 0x8015: throw new ErrorNoDataAvailable();
		case (short) 0x8016: throw new ErrorTooManyRecords();
		case (short) 0x8017: throw new ErrorUnexportedStoredData();
		case (short) 0x8018: throw new ErrorParameterMismatch();
		case (short) 0x8019: throw new ErrorIdNotFound();
		case (short) 0x801A: throw new ErrorTransactionNumberNotFound();
		case (short) 0x801B: throw new ErrorSeApiDeactivated();
		case (short) 0x801C: throw new ErrorTransport();
		case (short) 0x801D: throw new ErrorNoStartup();
		case (short) 0x801E: throw new ErrorNoStorage();

		case (short) 0x9000: return; // export data
		default:             throw new ErrorSigningSystemOperationDataFailed("result code unimplemented: "+String.format("0x%02X", result));
		}
	}

	final Object[] decode(int length, byte[] response) throws SEException {
		LinkedList<Object> result = new LinkedList<Object>();

		int pos = 0;
		while(pos < length) {
			byte type = response[pos++];
			short len = (short) (((response[pos++] & 0xFF) << 8) + (response[pos++] & 0xFF));
			switch(type) {
			case BYTE:
				if(len != 1)
					throw new ErrorSigningSystemOperationDataFailed("wrong length");
				result.add((Byte) response[pos++]);
				break;
			case BYTE_ARRAY:
				result.add(Arrays.copyOfRange(response, pos, pos+len));
				pos += len;
				break;
			case SHORT:
				if(len != 2)
					throw new ErrorSigningSystemOperationDataFailed("wrong length");
				result.add((Short) ((short) (((response[pos++] & 0xFF) << 8) + (response[pos++] & 0xFF))));
				break;
			case STRING:
				result.add(new String(Arrays.copyOfRange(response, pos, pos+len)));
				pos += len;
				break;
			case LONG_ARRAY:
				if(len % 4 != 0)
					throw new ErrorSigningSystemOperationDataFailed("wrong length");
				ByteBuffer buffer = ByteBuffer.allocate(len);
				buffer.put(response, pos, len);
				pos += len;
				((Buffer)buffer).flip();
				long[] longs = new long[len/4];
				for(int i = 0; i < longs.length; i++)
					longs[i] = 0xFFFFFFFF & buffer.getInt();
				result.add(longs);
				break;
			default:
				throw new ErrorTSEResponseDataInvalid();
			}
		}

		if(pos != length)
			throw new ErrorTSEResponseDataInvalid();

		return result.toArray(new Object[result.size()]);
	}

	final public Object[] send(Command cmd, Object... params) throws SEException {
		try {
			byte[] adata = encode(cmd, params);
			byte[] response = new byte[0xFFFF+2];

			short result = transmit(adata, response);
			checkResult(result);

			return decode(result, response);
		} catch (SEException e) {
			throw e;
		} catch (Exception e) {
			throw new ErrorTSECommunicationError(e);
		}
	}

	final public void sendAndWrite(Command cmd, OutputStream stream, Object... params) throws SEException {
		try {
			byte[] adata = encode(cmd, params);

			short result = transmitAndWrite(adata, stream);
			checkResult(result);
			stream.write(new byte[1024]);
		} catch (SEException e) {
			throw e;
		} catch (Exception e) {
			throw new ErrorTSECommunicationError(e);
		}
	}

	public void flush() throws IOException {}

	abstract short transmitAndWrite(byte[] adata, OutputStream stream) throws IOException, SEException;
	abstract short transmit(byte[] adata, byte[] response) throws IOException, SEException;

	public abstract void close() throws IOException;
}
