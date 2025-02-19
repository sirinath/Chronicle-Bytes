/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static net.openhft.chronicle.bytes.MappedBytes.mappedBytes;
import static net.openhft.chronicle.bytes.MappedFile.mappedFile;
import static org.junit.Assert.assertEquals;

public class MappedMemoryTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MappedMemoryTest.class);
    private static final long SHIFT = 27L;
    private static long BLOCK_SIZE = 1L << SHIFT;

    // on i7-3970X ~ 3.3 ns
    @Test
    public void testRawMemoryMapped() throws IOException {
        for (int t = 0; t < 5; t++) {
            File tempFile = File.createTempFile("chronicle", "q");
            try {

                long startTime = System.nanoTime();
                MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize());
                MappedBytesStore bytesStore = mappedFile.acquireByteStore(1);
                long address = bytesStore.address;

                for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                    OS.memory().writeLong(address + i, i);
                }
                for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                    OS.memory().writeLong(address + i, i);
                }

                bytesStore.release();
                mappedFile.close();
                assertEquals(mappedFile.referenceCounts(), 0, mappedFile.refCount());
                LOG.info("With RawMemory,\t\t time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                tempFile.delete();
            }
        }
    }

    // on i7-3970X ~ 6.9 ns
    @Test
    public void withMappedNativeBytesTest() throws IOException {

        for (int t = 0; t < 5; t++) {
            File tempFile = File.createTempFile("chronicle", "q");
            try {

                long startTime = System.nanoTime();
                final Bytes bytes = mappedBytes(tempFile, BLOCK_SIZE / 2);
                bytes.writeLong(1, 1);
                for (long i = 0; i < BLOCK_SIZE; i += 8) {
                    bytes.writeLong(i);
                }
                bytes.release();
                assertEquals(0, bytes.refCount());
                LOG.info("With MappedNativeBytes,\t avg time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                tempFile.delete();
            }
        }
    }

    // on i7-3970X ~ 6.0 ns
    @Test
    public void withRawNativeBytesTess() throws IOException {

        for (int t = 0; t < 5; t++) {
            File tempFile = File.createTempFile("chronicle", "q");
            try {

                MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize());
                long startTime = System.nanoTime();
                Bytes bytes = mappedFile.acquireBytesForWrite(1);
                for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                    bytes.writeLong(i);
                }
                bytes.release();

                bytes = mappedFile.acquireBytesForWrite(BLOCK_SIZE / 2 + 1);
                for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                    bytes.writeLong(i);
                }
                bytes.release();

                mappedFile.close();
                assertEquals(mappedFile.referenceCounts(), 0, mappedFile.refCount());
                LOG.info("With NativeBytes,\t\t time= " + 80 * (System.nanoTime() - startTime) / BLOCK_SIZE / 10.0 + " ns, number of longs written=" + BLOCK_SIZE / 8);
            } finally {
                tempFile.delete();
            }
        }
    }

    @Test
    public void mappedMemoryTest() throws IOException {

        File tempFile = File.createTempFile("chronicle", "q");
        try {

            final Bytes bytes = mappedBytes(tempFile, OS.pageSize());
            char[] chars = new char[OS.pageSize() * 11];
            Arrays.fill(chars, '.');
            chars[chars.length - 1] = '*';
            bytes.writeUTFΔ(new String(chars));
            String text = "hello this is some very long text";
            bytes.writeUTFΔ(text);

            bytes.readUTFΔ();
            assertEquals(text, bytes.readUTFΔ());
            bytes.release();
            assertEquals(0, bytes.refCount());
        } finally {
            tempFile.delete();
        }
    }
}

