/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jctools.queues;

import static org.jctools.queues.CircularArrayOffsetCalculator.allocate;

import org.jctools.util.Pow2;

public class SpscChunkedArrayQueue<E> extends BaseSpscLinkedArrayQueue<E> {

    public SpscChunkedArrayQueue(final int capacity) {
        this(Math.max(8, Pow2.roundToPowerOfTwo(capacity / 8)), capacity);
    }
    @SuppressWarnings("unchecked")
    public SpscChunkedArrayQueue(final int chunkSize, final int capacity) {
        if (capacity < 16) {
            throw new IllegalArgumentException("Max capacity must be 4 or more");
        }
        // minimal chunk size of eight makes sure minimal lookahead step is 2
        if (chunkSize < 8) {
            throw new IllegalArgumentException("Chunk size must be 2 or more");
        }

        maxQueueCapacity = Pow2.roundToPowerOfTwo(capacity);
        int chunkCapacity = Pow2.roundToPowerOfTwo(chunkSize);
        if (chunkCapacity >= maxQueueCapacity) {
            throw new IllegalArgumentException(
                    "Initial capacity cannot exceed maximum capacity(both rounded up to a power of 2)");
        }

        long mask = chunkCapacity - 1;
        // need extra element to point at next array
        E[] buffer = allocate(chunkCapacity+1);
        producerBuffer = buffer;
        producerMask = mask;
        consumerBuffer = buffer;
        consumerMask = mask;
        producerBufferLimit = mask - 1; // we know it's all empty to start with
        producerQueueLimit = maxQueueCapacity;
        soProducerIndex(0L);// serves as a StoreStore barrier to support correct publication
    }

    protected void linkNewBuffer(final E[] oldBuffer, final long currIndex, final long offset, final E e,
            final long mask) {
        // allocate new buffer of same length
        final E[] newBuffer = allocate((int)(mask + 2));
        producerBuffer = newBuffer;

        linkOldToNew(currIndex, oldBuffer, offset, newBuffer, offset, e);
    }

    @Override
    protected final boolean isBounded() {
        return true;
    }
}
