package com.teslicek.txoptimizations;

import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class FastFilePackResources extends AbstractPackMetadataResources implements PackResources {

    public static final Logger LOGGER = LogUtils.getLogger();

    private String[] sortedPaths = new String[0];
    private Map<String, Set<String>> namespaces = new HashMap<>();
    private ZipFile zipFile;
    private final List<String> prefixStack;
    private final Set<String> overlays;
    private volatile boolean fileTreeIndexed;

    public FastFilePackResources(PackLocationInfo packLocationInfo, ZipFile zipFile, List<String> overlays) {
        super(packLocationInfo);
        this.zipFile = zipFile;
        this.overlays = new HashSet<>(overlays);
        this.prefixStack = new ArrayList<>(overlays.size() + 1);

        for (int i = overlays.size() - 1; i >= 0; i--)
            this.prefixStack.add(overlays.get(i) + "/");

        this.prefixStack.add("");
    }

    private void ensureFileTree() {
        if (this.fileTreeIndexed)
            return;

        synchronized (this) {
            if (this.fileTreeIndexed)
                return;

            if (this.zipFile == null) {
                this.fileTreeIndexed = true;

                return;
            }

            ArrayList<String> paths = new ArrayList<>(8192);
            Enumeration<? extends ZipEntry> entries = this.zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String path = entry.getName();

                if (entry.isDirectory())
                    path = path.substring(0, path.length() - 1);

                this.indexNamespace(path);
                paths.add(path);
            }

            this.sortedPaths = paths.toArray(new String[0]);

            Arrays.sort(this.sortedPaths);

            this.fileTreeIndexed = true;
        }
    }

    private void indexNamespace(String path) {
        int s0 = path.indexOf('/');

        if (s0 < 1)
            return;

        String first = path.substring(0, s0);
        boolean firstIsOverlay = this.overlays.contains(first);

        String dirFolder;
        int nsLeft;
        int nsRight;

        if (firstIsOverlay) {
            int s1 = path.indexOf('/', s0 + 1);

            if (s1 <= s0 + 1)
                return;

            dirFolder = path.substring(s0 + 1, s1);
            int s2 = path.indexOf('/', s1 + 1);

            if (s2 <= s1 + 1)
                return;

            nsLeft = s1 + 1;
            nsRight = s2;
        } else {
            dirFolder = first;
            int s1 = path.indexOf('/', s0 + 1);

            if (s1 <= s0 + 1)
                return;

            nsLeft = s0 + 1;
            nsRight = s1;
        }

        String namespace = path.substring(nsLeft, nsRight);

        if (!Identifier.isValidNamespace(namespace)) {
            LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", namespace, this.zipFile);
            return;
        }

        this.namespaces.computeIfAbsent(dirFolder, s -> new HashSet<>()).add(namespace);
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getRootResource(String... parts) {
        return this.getResource(String.join("/", parts));
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getResource(PackType packType, Identifier resourceLocation) {
        for (String prefix : this.prefixStack) {
            IoSupplier<InputStream> supplier = this.getResource(prefix + packType.getDirectory() + "/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());

            if (supplier != null)
                return supplier;
        }

        return null;
    }

    @Nullable
    private IoSupplier<InputStream> getResource(String path) {
        if (this.zipFile == null)
            return null;

        ZipEntry entry = this.zipFile.getEntry(path);

        if (entry == null)
            return null;

        return IoSupplier.create(this.zipFile, entry);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        this.ensureFileTree();
        String packDir = packType.getDirectory();
        String[] paths = this.sortedPaths;

        if (paths.length == 0)
            return;

        if (this.prefixStack.size() == 1) {
            String namespacePrefix = this.prefixStack.get(0) + packDir + "/" + namespace + "/";
            String dirPrefix = namespacePrefix + path + "/";
            String endExclusive = dirPrefix + Character.MAX_VALUE;

            this.emitRangePaths(paths, dirPrefix, endExclusive, namespacePrefix, namespace, resourceOutput);
            return;
        }

        HashMap<Identifier, IoSupplier<InputStream>> scratch = HashMap.newHashMap(2048);

        for (String overlayPrefix : this.prefixStack) {
            String namespacePrefix = overlayPrefix + packDir + "/" + namespace + "/";
            String dirPrefix = namespacePrefix + path + "/";
            String endExclusive = dirPrefix + Character.MAX_VALUE;

            int lo = lowerBound(paths, dirPrefix);
            int hi = lowerBound(paths, endExclusive);

            for (int q = lo; q < hi; q++) {
                String filePath = paths[q];
                String rlPath = filePath.substring(namespacePrefix.length());
                Identifier location = Identifier.tryBuild(namespace, rlPath);

                if (location == null) {
                    LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, rlPath);
                    continue;
                }

                scratch.putIfAbsent(location, this.getResource(filePath));
            }
        }

        scratch.forEach(resourceOutput);
    }

    private void emitRangePaths(String[] paths, String dirPrefix, String endExclusive, String namespacePrefix, String namespace, ResourceOutput resourceOutput) {
        int lo = lowerBound(paths, dirPrefix);
        int hi = lowerBound(paths, endExclusive);
        ZipFile zf = this.zipFile;
        int nsPrefixLen = namespacePrefix.length();

        for (int q = lo; q < hi; q++) {
            String filePath = paths[q];
            String rlPath = filePath.substring(nsPrefixLen);
            Identifier location = Identifier.tryBuild(namespace, rlPath);

            if (location == null) {
                LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, rlPath);
                continue;
            }

            ZipEntry entry = zf.getEntry(filePath);

            if (entry == null)
                continue;

            resourceOutput.accept(location, IoSupplier.create(zf, entry));
        }
    }

    static int lowerBound(String[] sorted, String key) {
        int lo = 0;
        int hi = sorted.length;

        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            String mv = sorted[mid];

            if (mv.compareTo(key) < 0)
                lo = mid + 1;
            else
                hi = mid;
        }

        return lo;
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        this.ensureFileTree();

        Map<String, Set<String>> map = this.namespaces;

        if (map == null)
            return Set.of();

        return map.getOrDefault(packType.getDirectory(), Collections.emptySet());
    }

    @Override
    public void close() {
        if (this.zipFile == null)
            return;

        IOUtils.closeQuietly(this.zipFile);
        this.zipFile = null;
        this.namespaces = null;
        this.sortedPaths = new String[0];
        this.fileTreeIndexed = false;
    }
}
