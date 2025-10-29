/*******************************************************************************
 * Copyright (c) 2025 Kichwa Coders Canada, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.swt.tests;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public final class ProccessStats {
	private static final boolean EXCLUDE_JARS_CLASSES = false;

	private ProccessStats() {
	}

	/**
	 * Immutable! Use {@link StatsBuilder} to make a new one
	 */
	public static class Stats {
		public final long usedSystemMemory;
		/**
		 * Map of OS system thread names -> number of threads with that name. Most
		 * threads have unique names, but this allows for multiple threads to have the
		 * same name.
		 */
		public final Map<String, Integer> systemThreads;
		public final long usedVmMemory;
		/**
		 * Map of vm thread names -> number of threads with that name. Most threads have
		 * unique names, but this allows for multiple threads to have the same name.
		 */
		public final Map<String, Integer> vmThreads;
		/**
		 * Map of file names -> number of times they are open. Most files will be open
		 * only once, but some items, such as video card may be multiple times.
		 */
		public final Map<String, Integer> openFiles;

		private Stats(long usedSystemMemory, Map<String, Integer> systemThreads, long usedVmMemory,
				Map<String, Integer> vmThreads, Map<String, Integer> openFiles) {
			this.usedSystemMemory = usedSystemMemory;
			this.systemThreads = Collections.unmodifiableMap(systemThreads);
			this.usedVmMemory = usedVmMemory;
			this.vmThreads = Collections.unmodifiableMap(vmThreads);
			this.openFiles = Collections.unmodifiableMap(openFiles);
		}

		@Override
		public String toString() {
			String osResources = String.format("UsedMem=%,d bytes (%.2f MB), Threads=%d\n%s", usedSystemMemory,
					asMB(usedSystemMemory), total(systemThreads), toString(systemThreads));
			String vmResources = String.format("UsedVM=%,d bytes (%.2f MB), VM Threads=%d\n%s", usedVmMemory,
					asMB(usedVmMemory), total(vmThreads), toString(vmThreads));
			int totalOpen = ProccessStats.Stats.total(openFiles);
			String fileSummary = String.format("Open files%s: (count: %d, unique count: %d)\n%s",
					EXCLUDE_JARS_CLASSES ? " (excluding .jar/.class files)" : "", totalOpen, openFiles.size(),
					toString(openFiles));
			return String.format("""
					ProccessStats
					%s
					%s
					%s
					""", osResources, vmResources, fileSummary);
		}

		private String toString(Map<String, Integer> counts) {
			return counts.entrySet().stream().map(e -> "  " + e.getKey() + " x " + e.getValue())
					.collect(Collectors.joining(System.lineSeparator()));
		}

		public static int total(Map<String, Integer> counts) {
			return counts.values().stream().mapToInt(Integer::intValue).sum();
		}

		private double asMB(long bytes) {
			return bytes / 1024.0 / 1024.0;
		}

		public Stats subtract(Stats other) {
			long usedSystemMemory = this.usedSystemMemory - other.usedSystemMemory;
			Map<String, Integer> systemThreads = subtract(this.systemThreads, other.systemThreads);
			long usedVmMemory = this.usedVmMemory - other.usedVmMemory;
			Map<String, Integer> vmThreads = subtract(this.vmThreads, other.vmThreads);
			Map<String, Integer> openFiles = subtract(this.openFiles, other.openFiles);

			return new Stats(usedSystemMemory, systemThreads, usedVmMemory, vmThreads, openFiles);
		}

		private Map<String, Integer> subtract(Map<String, Integer> left, Map<String, Integer> right) {
			Map<String, Integer> result = new HashMap<>(left);
			right.entrySet().forEach(rightEntry -> {
				String name = rightEntry.getKey();
				int otherCount = rightEntry.getValue();
				Integer count = result.getOrDefault(name, 0);
				result.put(name, count - otherCount);
			});
			result.entrySet().removeIf(entry -> entry.getValue() == 0);
			return result;
		}
	}

	public static class StatsBuilder {
		public long usedSystemMemory = 0;
		public Map<String, Integer> systemThreads = new HashMap<>();
		public long usedVmMemory = 0;
		public Map<String, Integer> vmThreads = new HashMap<>();
		public Map<String, Integer> openFiles = new HashMap<>();

		public Stats build() {
			return new Stats(usedSystemMemory, Collections.unmodifiableMap(new HashMap<>(systemThreads)), usedVmMemory,
					Collections.unmodifiableMap(new HashMap<>(vmThreads)),
					Collections.unmodifiableMap(new HashMap<>(openFiles)));
		}

		public StatsBuilder usedSystemMemory(long usedSystemMemory) {
			this.usedSystemMemory = usedSystemMemory;
			return this;
		}

		public StatsBuilder systemThreads(Map<String, Integer> systemThreads) {
			this.systemThreads = systemThreads;
			return this;
		}

		public StatsBuilder usedVmMemory(long usedVmMemory) {
			this.usedVmMemory = usedVmMemory;
			return this;
		}

		public StatsBuilder vmThreads(Map<String, Integer> vmThreads) {
			this.vmThreads = vmThreads;
			return this;
		}

		public StatsBuilder openFiles(Map<String, Integer> openFiles) {
			this.openFiles = openFiles;
			return this;
		}
	}

	public static Stats collect() {
		System.gc();
		runFinalization();

		long usedSystemMemory = getUsedSystemMemory();
		Map<String, Integer> systemThreads = getCounts(getSystemThreadNames());
		long usedVmMemory = getUsedVmMemory();
		Map<String, Integer> vmThreads = getCounts(getVmThreadNames());
		Map<String, Integer> openFiles = getCounts(getOpenedDescriptors());
		return new Stats(usedSystemMemory, systemThreads, usedVmMemory, vmThreads, openFiles);
	}

	// TODO why does @SuppressWarnings make its own warning?
	@SuppressWarnings("removal")
	private static void runFinalization() {
		System.runFinalization();
	}

	private static List<String> getVmThreadNames() {
		return Thread.getAllStackTraces().keySet().stream().map(Thread::getName).collect(Collectors.toList());
	}

	private static long getUsedVmMemory() {
		long totalVmMemory = Runtime.getRuntime().totalMemory();
		long freeVmMemory = Runtime.getRuntime().freeMemory();
		long usedVmMemory = totalVmMemory - freeVmMemory;
		return usedVmMemory;
	}

	private static long getUsedSystemMemory() {
		try (var lines = Files.lines(Path.of("/proc/self/status"))) {
			for (String line : (Iterable<String>) lines::iterator) {
				if (line.startsWith("VmRSS:")) {
					long kb = Long.parseLong(line.replaceAll("\\D+", ""));
					// always in kB? Looks like it from task_mmu.c sources
					return kb * 1024L;
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to collect ProcSelfStats");
			e.printStackTrace();
		}
		return -1;
	}

	@SuppressWarnings("unused")
	private static int getSystemThreadCount() {
		try (var lines = Files.lines(Path.of("/proc/self/status"))) {
			for (String line : (Iterable<String>) lines::iterator) {
				if (line.startsWith("Threads:")) {
					return Integer.parseInt(line.replaceAll("\\D+", ""));
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to collect ProcSelfStats");
			e.printStackTrace();
		}
		return -1;
	}

	private static List<String> getSystemThreadNames() {
		List<String> names = new ArrayList<>();
		Path taskDir = Paths.get("/proc/self/task");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir)) {
			for (Path tidPath : stream) {
				try {
					Path commFile = tidPath.resolve("comm");
					String name = Files.readString(commFile).trim();
					names.add(name);
				} catch (IOException e) {
					// thread might have exited; skip it
				}
			}
		} catch (IOException e1) {
			System.err.println("Failed to collect ProcSelfStats");
			e1.printStackTrace();
		}

		return names;
	}

	/**
	 * Return map of file names -> number of times they are open. Most files will be
	 * open only once, but some items, such as video card may be multiple times.
	 */
	private static Map<String, Integer> getCounts(List<String> openFiles) {
		Map<String, Integer> fileCounts = new TreeMap<>();
		for (String s : openFiles) {
			fileCounts.merge(s, 1, Integer::sum);
		}
		return fileCounts;
	}

	private static List<String> getOpenedDescriptors() {
		List<String> paths = new ArrayList<>();
		Path fd = Paths.get("/proc/self/fd/");
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fd)) {
			directoryStream.forEach(path -> {
				String resolvedPath = resolveSymLink(path);
				if (isTestRelatedFileDescriptor(resolvedPath)) {
					paths.add(resolvedPath);
				}
			});
		} catch (IOException e1) {
			System.err.println("Failed to collect open file descriptors");
			e1.printStackTrace();
		}
		Collections.sort(paths);
		return paths;
	}

	private static boolean isTestRelatedFileDescriptor(String fileDescriptorPath) {
		// Do not consider file descriptors of Maven artifacts that are currently opened
		// by other Maven plugins executed in parallel build (such as parallel
		// compilation of the swt.tools bundle etc.)
		if (fileDescriptorPath == null) {
			return false;
		}
		if (!EXCLUDE_JARS_CLASSES) {
			return true;
		}
		return !fileDescriptorPath.endsWith(".jar") && !fileDescriptorPath.endsWith(".class");
	}

	private static String resolveSymLink(Path path) {
		try {
			return Files.isSymbolicLink(path) ? Files.readSymbolicLink(path).toString() : path.toString();
		} catch (IOException e) {
			System.err.println("Failed to convert fd symlink to real file");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Simple manual test for class to check it works.
	 *
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		makeAThread();
		Stats s = collect();
		System.out.println(s);
		makeAThread();
		Path tempFilePath = Files.createTempFile("myTempFile", ".txt");
		try (var outputStream = new FileOutputStream(tempFilePath.toFile())) {
			Stats after = collect();
			System.out.println(after);
			System.out.println(after.subtract(s));
		}
	}

	private static void makeAThread() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Thread thread = new Thread("samename") {
			@Override
			public void run() {
				countDownLatch.countDown();
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
		countDownLatch.await();
	}
}