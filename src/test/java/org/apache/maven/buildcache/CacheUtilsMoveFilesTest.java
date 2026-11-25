/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.buildcache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CacheUtilsMoveFilesTest {

    @Test
    void movesFilesUnderTarget(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("module/target/classes");
        Files.createDirectories(source.resolve("com/example/empty"));
        Files.write(source.resolve("Top.class"), "top".getBytes(StandardCharsets.UTF_8));
        Files.write(source.resolve("com/example/Nested.class"), "nested".getBytes(StandardCharsets.UTF_8));
        Path target = tempDir.resolve("staging/module/target/classes");

        CacheUtils.moveFiles(source, target);

        assertThat(target.resolve("Top.class")).hasContent("top");
        assertThat(target.resolve("com/example/Nested.class")).hasContent("nested");
        assertThat(target.resolve("com/example/empty")).isEmptyDirectory();
        assertThat(source).doesNotExist();
        assertThat(source.getParent()).isEmptyDirectory();
    }

    @Test
    void movesNoDirectory(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("module/target/classes");
        Files.createDirectories(source.resolve("com/example"));
        Files.write(source.resolve("com/example/Nested.class"), "nested".getBytes(StandardCharsets.UTF_8));
        Object sourceKey = fileKey(source);
        Object nestedKey = fileKey(source.resolve("com/example"));
        assumeTrue(sourceKey != null, "The file system exposes no file keys");
        Path target = tempDir.resolve("staging/module/target/classes");

        CacheUtils.moveFiles(source, target);

        assertThat(fileKey(target)).isNotEqualTo(sourceKey);
        assertThat(fileKey(target.resolve("com/example"))).isNotEqualTo(nestedKey);
    }

    private Object fileKey(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class).fileKey();
    }
}
