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
import java.util.ArrayList;
import java.util.Collections;

import org.apache.maven.buildcache.its.junit.IntegrationTest;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.Test;

import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_HIT;
import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_MISS;
import static org.apache.maven.buildcache.its.CacheITUtils.CACHE_SAVED;
import static org.apache.maven.buildcache.xml.CacheConfigImpl.CACHE_LOCATION_PROPERTY_NAME;

/**
 * Computing the cache key collects the dependency graph before Maven resolves the project's dependencies. A dependency
 * whose POM is only in a remote repository must still bring its transitive dependencies to the build, and the build
 * must still be cached.
 */
@IntegrationTest("src/test/projects/cold-local-repository")
class ColdLocalRepositoryTest {

    @Test
    void buildWithUndownloadedDependencyPomKeepsTransitiveDependenciesAndCaches(Verifier verifier) throws Exception {
        Path basedir = Paths.get(verifier.getBasedir());
        Path remoteRepository = basedir.resolveSibling("remote-repository");

        Verifier libs = createVerifier(verifier, basedir.resolve("libs"), "../../log-libs.txt");
        libs.setSystemProperty("altDeploymentRepository", "libs::" + remoteRepository.toUri());
        libs.executeGoals(Collections.singletonList("deploy"));
        libs.verifyErrorFreeLog();
        libs.deleteArtifacts("org.apache.maven.caching.test.cold-local-repository");

        Verifier app = createVerifier(verifier, basedir.resolve("app"), "../../log-app-cold.txt");
        app.setSystemProperty("libs.repository.url", remoteRepository.toUri().toString());
        app.setSystemProperty(
                CACHE_LOCATION_PROPERTY_NAME, basedir.resolveSibling("cache").toString());
        app.executeGoals(Collections.singletonList("verify"));
        app.verifyErrorFreeLog();
        app.verifyTextInLog(CACHE_MISS);
        app.verifyTextInLog(CACHE_SAVED);

        app.setLogFileName("../../log-app-warm.txt");
        app.executeGoals(Collections.singletonList("verify"));
        app.verifyErrorFreeLog();
        app.verifyTextInLog(CACHE_HIT);
    }

    private Verifier createVerifier(Verifier verifier, Path project, String logFileName) throws Exception {
        Verifier child = new Verifier(project.toString());
        child.setAutoclean(false);
        child.setLocalRepo(verifier.getLocalRepository());
        child.setCliOptions(new ArrayList<>(verifier.getCliOptions()));
        child.setSystemProperties(verifier.getSystemProperties());
        child.setLogFileName(logFileName);
        return child;
    }
}
