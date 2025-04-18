/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink.compact.changelog;

import org.apache.paimon.flink.FlinkConnectorOptions;
import org.apache.paimon.flink.sink.Committable;
import org.apache.paimon.options.MemorySize;
import org.apache.paimon.options.Options;
import org.apache.paimon.table.FileStoreTable;
import org.apache.paimon.utils.ThreadPoolUtils;

import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.types.Either;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Receive and process the {@link ChangelogCompactTask}s emitted by {@link
 * ChangelogCompactCoordinateOperator}.
 */
public class ChangelogCompactWorkerOperator extends AbstractStreamOperator<Committable>
        implements OneInputStreamOperator<Either<Committable, ChangelogCompactTask>, Committable> {

    private final FileStoreTable table;

    private transient ExecutorService executor;
    private transient MemorySize bufferSize;

    public ChangelogCompactWorkerOperator(FileStoreTable table) {
        this.table = table;
    }

    @Override
    public void open() throws Exception {
        Options options = new Options(table.options());
        int numThreads =
                options.getOptional(FlinkConnectorOptions.CHANGELOG_PRECOMMIT_COMPACT_THREAD_NUM)
                        .orElse(Runtime.getRuntime().availableProcessors());
        executor =
                ThreadPoolUtils.createCachedThreadPool(
                        numThreads, "changelog-compact-async-read-bytes");
        bufferSize = options.get(FlinkConnectorOptions.CHANGELOG_PRECOMMIT_COMPACT_BUFFER_SIZE);
        LOG.info(
                "Creating {} threads and a buffer of {} bytes for changelog compaction.",
                numThreads,
                bufferSize.getBytes());
    }

    @Override
    public void processElement(StreamRecord<Either<Committable, ChangelogCompactTask>> record)
            throws Exception {
        if (record.getValue().isLeft()) {
            output.collect(new StreamRecord<>(record.getValue().left()));
        } else {
            ChangelogCompactTask task = record.getValue().right();
            List<Committable> committables = task.doCompact(table, executor, bufferSize);
            committables.forEach(committable -> output.collect(new StreamRecord<>(committable)));
        }
    }
}
