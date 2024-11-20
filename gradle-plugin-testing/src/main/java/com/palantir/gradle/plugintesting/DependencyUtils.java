/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.plugintesting;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DependencyUtils {
    private static final Logger log = LoggerFactory.getLogger(DependencyUtils.class);

    private DependencyUtils() {}

    /**
     * Get dependency string for the given artifact.  The format differs between external dependencies (e.g. jars) and
     * projects in the same source root.  The format matches how dependencies are represented in reports generated from
     * the core gradle dependencies task and in build.gradle files, except the version is omitted.
     */
    public static String getDependencyName(ResolvedArtifact artifact) {
        final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
        return isProjectDependency(artifact)
                ? String.format(
                        "project :%s",
                        ((ProjectComponentIdentifier) artifact.getId().getComponentIdentifier())
                                .getProjectPath()
                                .substring(1))
                : getJarDepName(
                        id.getGroup(), id.getName(), Optional.empty(), Optional.ofNullable(artifact.getClassifier()));
    }

    public static String getDependencyName(ResolvedDependency dependency) {
        Set<ResolvedArtifact> artifacts = dependency.getModuleArtifacts();
        // use the first artifact - if artifact is empty (which it should never be for normal dependencies),
        // use the module id from the dep itself
        if (!artifacts.isEmpty()) {
            return getDependencyName(artifacts.iterator().next());
        } else {
            return getJarDepName(
                    dependency.getModuleGroup(), dependency.getModuleName(), Optional.empty(), Optional.empty());
        }
    }

    public static String getDependencyName(ModuleDependency dependency) {
        if (isProjectDependency(dependency)) {
            return String.format(
                    "project :%s",
                    ((ProjectDependency) dependency)
                            .getDependencyProject()
                            .getPath()
                            .substring(1));
        } else {
            Optional<String> maybeClassifier =
                    dependency.getArtifacts().stream().findFirst().map(DependencyArtifact::getClassifier);
            return getJarDepName(dependency.getGroup(), dependency.getName(), Optional.empty(), maybeClassifier);
        }
    }

    /**
     * Generate dependency id in the standard way except for version - group:name[:version][:classifier].
     */
    public static String getJarDepName(
            String group, String name, Optional<String> version, Optional<String> classifier) {
        String sep = classifier.isPresent() ? ":" : "";
        return group + ":" + name + version.map(v -> ":" + v).orElse(sep)
                + classifier.map(s -> ":" + s).orElse("");
    }

    /**
     * Returns dependencies declared directly in the given configurations.
     */
    public static Set<String> getDirectDependencyNames(Configuration config) {
        return getDependencyNames(config, false);
    }

    /**
     * Returns module dependencies declared in the given configurations, optionally including those declared by parent
     * configurations.  Does not include transitive dependencies.
     */
    public static Set<String> getDependencyNames(Configuration config, boolean includeParents) {
        DependencySet dependencies = includeParents ? config.getAllDependencies() : config.getDependencies();
        return dependencies.stream()
                .filter(ModuleDependency.class::isInstance)
                .map(ModuleDependency.class::cast)
                .map(DependencyUtils::getDependencyName)
                .collect(Collectors.toSet());
    }

    /**
     * Return true if the dependency is from a project in the current build rather than an external jar.
     */
    public static boolean isProjectDependency(ModuleDependency dependency) {
        return dependency instanceof ProjectDependency;
    }

    /**
     * Return true if the dependency is from a project in the current build rather than an external jar.
     */
    public static boolean isProjectDependency(ResolvedDependency dependency) {
        return isProjectDependency(getDependencyName(dependency));
    }

    /**
     * Return true if the resolved artifact is derived from a project in the current build rather than an
     * external jar.
     */
    public static boolean isProjectDependency(ResolvedArtifact artifact) {
        return artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier;
    }

    /**
     * We have a few different string representations of project dependency names.
     */
    public static boolean isProjectDependency(String artifactName) {
        return artifactName.startsWith("project :")
                || artifactName.startsWith(":")
                || artifactName.startsWith("project (')")
                // a colon in the name (when the name doesn't start with one of the above) indicates it is a
                // jar dependency e.g. group:id
                || !artifactName.contains(":");
    }

    /**
     * Find dependency with given name somewhere in the set of dependencies for the configuration if it exists.
     * @return A ResolvedDependency that can be queried for transitive dependencies and such
     */
    public static Optional<ResolvedDependency> findResolvedDependency(Configuration config, String name) {
        return findResolvedDependency(config.getResolvedConfiguration().getFirstLevelModuleDependencies(), name);
    }

    public static Optional<ResolvedDependency> findResolvedDependency(Set<ResolvedDependency> deps, String name) {
        Optional<ResolvedDependency> maybeResult =
                deps.stream().filter(d -> name.equals(getDependencyName(d))).findFirst();

        if (maybeResult.isPresent()) {
            return maybeResult;
        }

        return deps.stream()
                .map(ResolvedDependency::getChildren)
                .map(subDeps -> findResolvedDependency(subDeps, name))
                .filter(Optional::isPresent)
                .findFirst()
                .orElseGet(Optional::empty);
    }
}
