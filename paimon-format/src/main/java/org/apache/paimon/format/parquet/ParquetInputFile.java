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

package org.apache.paimon.format.parquet;

import org.apache.paimon.fs.FileIO;
import org.apache.paimon.fs.Path;

import org.apache.parquet.io.InputFile;

import java.io.IOException;

/** A {@link InputFile} for paimon. */
public class ParquetInputFile implements InputFile {

    private final FileIO fileIO;
    private final Path path;
    private final long length;

    public static ParquetInputFile fromPath(FileIO fileIO, Path path, long length)
            throws IOException {
        return new ParquetInputFile(fileIO, path, length);
    }

    private ParquetInputFile(FileIO fileIO, Path path, long length) {
        this.fileIO = fileIO;
        this.path = path;
        this.length = length;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public ParquetInputStream newStream() throws IOException {
        return new ParquetInputStream(fileIO.newInputStream(path));
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
