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

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Properties;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

interface Jna extends Library {
	Jna lib = (Jna) Native.loadLibrary("se-msc-io_"+Platform.RESOURCE_PREFIX, Jna.class);

	int mscOpen(String fileName, IntByReference handle);
	int mscClose(int handle);
	int mscWrite(int handle, byte[] data, int length, short le);
	int mscRead(int handle, byte[] response, IntByReference responseLength, int timeout, int readdelay);
}

public class MscJnaTransport extends MSCTransport {

	private static final int JnaErrorIO             = 0x1009;
	private static final int JnaErrorTSETimeout     = 0x100A;
	private static final int JnaErrorBufferTooSmall = 0x100B;

	private final int msc_handle;
	
	public MscJnaTransport(Properties props) throws IOException {
		super(props);

 		if(!io.exists())
			throw new IOException(io.getAbsolutePath()+" not available");

		int result;
		IntByReference handle = new IntByReference();
		if((result = Jna.lib.mscOpen(io.getAbsolutePath(), handle)) != 0) {
			switch(result) {
			case JnaErrorIO:             throw new IOException("ErrorIO");
			case JnaErrorTSETimeout:     throw new IOException("ErrorTSETimeout");
			default:                     throw new IOException("JNA result: "+result);
			}
		}
		msc_handle = handle.getValue();
	}

	@Override
	protected void write(ByteBuffer bb) throws IOException {
		throw new Error("unreachable");
	}

	protected short receive(ByteBuffer bb, int count, int timeout) throws IOException {
		int result;
		IntByReference responseLength = new IntByReference(((Buffer)bb).capacity());
//		long start = System.currentTimeMillis();
		if((result = Jna.lib.mscRead(msc_handle, bb.array(), responseLength, timeout, IO_DELAY)) != 0) {
			switch(result) {
			case JnaErrorBufferTooSmall: throw new IOException("ErrorBufferTooSmall");
			case JnaErrorIO:             throw new IOException("ErrorIO");
			case JnaErrorTSETimeout:     throw new IOException("ErrorTSETimeout");
			default:                     throw new IOException("JNA result: "+result);
			}
		}
//		if(System.currentTimeMillis() - start > 5000)
//			System.err.println("receive");

		((Buffer)bb).position(responseLength.getValue());
		((Buffer)bb).flip();

		if(responseLength.getValue() == 1) {
			byte b = bb.get();
			bb.clear();
			bb.putShort((short) 0x801C);
			bb.put(b);
			bb.flip();
		} else if(responseLength.getValue() == 0) {
			bb.clear();
			bb.putShort((short) 0x801C);
			bb.flip();
		}

		short len = bb.getShort();
		return len;
	}

	@Override
	protected void send(byte[] adata) throws IOException {
		int result;
//		long start = System.currentTimeMillis();
		if((result = Jna.lib.mscWrite(msc_handle, adata, adata.length, (short) 0)) != 0) {
			switch(result) {
			case JnaErrorIO:             throw new IOException("ErrorIO");
			case JnaErrorBufferTooSmall: throw new IOException("ErrorBufferTooSmall");
			default:                     throw new IOException("JNA result: "+result);
			}
		}
//		if(System.currentTimeMillis() - start > 1000)
//			System.err.println("send");
	}

	/* (non-Javadoc)
	 * @see com.cryptovision.SEAPI.transport.MSCTransport#close()
	 */
	@Override
	public void close() throws IOException {
		super.close();

		int result;
		if((result = Jna.lib.mscClose(msc_handle)) != 0) {
			switch(result) {
			case JnaErrorIO:             throw new IOException("ErrorIO");
			default:                     throw new IOException("JNA result: "+result);
			}
		}
	}
}
