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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;

import com.cryptovision.SEAPI.TSE.TSEOutputStream;
import com.cryptovision.SEAPI.exceptions.ErrorStreamWrite;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid;
import com.cryptovision.SEAPI.exceptions.SEException;

public abstract class MSCTransport extends Transport {

	/**  */
	protected static final byte[] FILE_HEADER = new byte[] { 0x41, 0x64, 0x56, 0x61, 0x6e, 0x63, 0x45, 0x44, 0x20, 0x53, 0x65, 0x43, 0x75, 0x52, 0x65, 0x20, 0x53, 0x44, 0x2f, 0x4d, 0x4d, 0x43, 0x20, 0x43, 0x41, 0x72, 0x64, 0x01, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xAF };

	protected static final int TSE_CMD_BUFF_SIZE = Short.MAX_VALUE;

	protected int IO_DELAY;
	protected File io;
	final int SIZE;

	protected MSCTransport() {
		SIZE = 8*1024;
	}

	public MSCTransport(Properties props) throws FileNotFoundException {
		io = new File(props.getProperty("path")+"/TSE-IO.bin");
		if(io.exists())
			SIZE = (int) io.length();
		else
			throw new FileNotFoundException(io.getAbsolutePath());

		IO_DELAY = props.containsKey("delay") ? Integer.parseInt(props.getProperty("delay")) : 0;
	}

	public void minDelay(int i) {
		if(IO_DELAY < i)
			IO_DELAY = i;
	}

	protected void write(ByteBuffer bb) throws IOException {
		RandomAccessFile file = new RandomAccessFile(io, "rws");
		file.write(bb.array(), 0, ((Buffer)bb).limit());
		file.getFD().sync();
		file.close();
	}

	protected void send(byte[] adata) throws IOException, ErrorTSECommandDataInvalid {
		if(adata.length >= SIZE -32-2-2)
			throw new ErrorTSECommandDataInvalid("command data length exceeded");

		ByteBuffer bb = bb(32+4+adata.length);
		bb.put(FILE_HEADER);
		bb.putShort((short) adata.length);
		bb.putShort((short) 0);
		bb.put(adata);
		((Buffer)bb).flip();

//		System.out.println("MSC > "+new String(Hex.encode(slice(bb, 32, 64))));

		write(bb);
	}

	abstract protected short receive(ByteBuffer bb, int count, int timeout) throws IOException;

	protected ByteBuffer bb(int size) {
		return ByteBuffer.allocate(size);
	}

	protected byte[] slice(ByteBuffer bb, int start, int len) {
		return Arrays.copyOfRange(bb.array(), start, start+len);
	}

	private short transmitAndReceive(byte[] adata, ByteBuffer resp) throws SEException, IOException {
//		System.out.println("MSC > "+new String(Hex.encode(adata)));
		send(adata);
		try { Thread.sleep(IO_DELAY); } catch (InterruptedException e) {}
		short result = receive(resp, resp.remaining(), IO_TIMEOUT);
//		System.out.println("MSC < "+new String(Hex.encode(slice(resp, resp.position(), resp.remaining()))));
		return result;
	}

	private int receiveToStream(ByteBuffer resp, long size, OutputStream stream) throws IOException, SEException {
		int offset = 0;
		while(offset < size) {
			if(!resp.hasRemaining()) {
				((Buffer)resp).clear();

				byte[] adata2 = new byte[] { (byte) 0xC5 };
//				System.out.println("MSC > "+new String(Hex.encode(adata2)));
				send(adata2);
				try { Thread.sleep(IO_DELAY); } catch (InterruptedException e) {}

				short len = receive(resp, resp.remaining(), IO_TIMEOUT);
//				System.out.println("MSC < "+new String(Hex.encode(resp.array(), 0, resp.limit())));
				if(!resp.hasRemaining() && size-offset > 2)
					return len;

				((Buffer)resp).position(((Buffer)resp).position()-2);
			}

			int len = (int) Math.min(resp.remaining(), size-offset);
			offset += len;
			try {
				stream.write(slice(resp, ((Buffer)resp).position(), len));
			} catch (IOException e) {
				if(offset < size) {
					byte[] adata2 = new byte[] { (byte) 0xC4 };
//					System.out.println("MSC > "+new String(Hex.encode(adata2)));
					send(adata2);
					try { Thread.sleep(IO_DELAY); } catch (InterruptedException ex) {}
					receive(resp, resp.remaining(), IO_TIMEOUT);
				}
				throw new ErrorStreamWrite(e);
			}
			((Buffer)resp).position(((Buffer)resp).position() + len);
		}

		return offset;
	}

	@Override
	short transmit(byte[] adata, byte[] response) throws IOException, SEException {

		ByteBuffer resp = bb(SIZE);
		short result = transmitAndReceive(adata, resp);

		if(result <= 0)
			return (short) result;
		else if(result == resp.remaining()) {
			resp.get(response, 0, result);
			return result;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream(response.length);
		int size = receiveToStream(resp, result, stream);
		System.arraycopy(stream.toByteArray(), 0, response, 0, stream.size());

		return (short) size;
	}

	@Override
	short transmitAndWrite(byte[] adata, OutputStream stream) throws IOException, SEException {

		ByteBuffer resp = bb(SIZE);
		short result = transmitAndReceive(adata, resp);

		if(result != (short) 0x9000 || resp.remaining() < 8)
			if(result < 0)
				return (short) result;
			else
				return (short) 0x8002;

		long size = resp.getLong();

		if(stream instanceof TSEOutputStream)
			((TSEOutputStream) stream).total(size+1024);

		receiveToStream(resp, size, stream);

		return 0;
	}

	@Override
	public void close() throws IOException {
		// nop
	}
}
