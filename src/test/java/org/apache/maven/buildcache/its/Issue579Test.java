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
package org.apache.maven.buildcache.its;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.buildcache.its.junit.IntegrationTest;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.Test;

import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_HIT;
import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_MISS;
import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_SAVED;
import static org.apache.maven.buildcache.its.CacheITUtils.assertNotInLog;
import static org.apache.maven.buildcache.its.CacheITUtils.replaceInFile;
import static org.apache.maven.buildcache.xml.CacheConfigImpl.CACHE_LOCATION_PROPERTY_NAME;

/**
 * Reproducer for <a href="https://github.com/apache/maven-build-cache-extension/issues/579">Issue #579</a>:
 * changing a managed version of a transitive dependency does not invalidate the cache.
 */
@IntegrationTest("src/test/projects/mbuildcache-579")
class Issue579Test {

    private static final String MANAGED_HAMCREST_CORE_1_3 =
            "<artifactId>hamcrest-core</artifactId>\n        <version>1.3</version>";
    private static final String MANAGED_HAMCREST_CORE_1_2 =
            "<artifactId>hamcrest-core</artifactId>\n        <version>1.2</version>";

    @Test
    void changingManagedVersionOfTransitiveDependencyInvalidatesCache(Verifier verifier) throws Exception {
        Path pom = setUp(verifier);

        build(verifier, "../log-1-hamcrest-core-1.3.txt");
        verifier.verifyTextInLog(CACHE_SAVED);

        build(verifier, "../log-2-hamcrest-core-1.3-unchanged.txt");
        verifier.verifyTextInLog(CACHE_HIT);

        replaceInFile(pom, MANAGED_HAMCREST_CORE_1_3, MANAGED_HAMCREST_CORE_1_2);
        build(verifier, "../log-3-hamcrest-core-1.2.txt");
        assertNotInLog(verifier, CACHE_HIT);
        verifier.verifyTextInLog(CACHE_MISS);
        verifier.verifyTextInLog(CACHE_SAVED);
    }

    @Test
    void changingManagedVersionOfUnusedDependencyKeepsCache(Verifier verifier) throws Exception {
        Path pom = setUp(verifier);

        build(verifier, "../log-1-hamcrest-library-1.3.txt");
        verifier.verifyTextInLog(CACHE_SAVED);

        replaceInFile(
                pom,
                "<artifactId>hamcrest-library</artifactId>\n        <version>1.3</version>",
                "<artifactId>hamcrest-library</artifactId>\n        <version>1.2</version>");
        build(verifier, "../log-2-hamcrest-library-1.2.txt");
        verifier.verifyTextInLog(CACHE_HIT);
    }

    private Path setUp(Verifier verifier) {
        Path basedir = Paths.get(verifier.getBasedir());
        verifier.setAutoclean(false);
        verifier.setSystemProperty(
                CACHE_LOCATION_PROPERTY_NAME, basedir.resolveSibling("cache").toString());
        return basedir.resolve("pom.xml");
    }

    private void build(Verifier verifier, String logFileName) throws Exception {
        verifier.setLogFileName(logFileName);
        verifier.executeGoals(Collections.singletonList("verify"));
        verifier.verifyErrorFreeLog();
    }
}
