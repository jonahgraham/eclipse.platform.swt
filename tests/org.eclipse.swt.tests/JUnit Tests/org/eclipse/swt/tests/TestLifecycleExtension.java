package org.eclipse.swt.tests;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.tests.junit.SwtTestUtil;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestLifecycleExtension
		implements BeforeAllCallback, AfterAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
		System.out.println("=============================================================");
		System.out.println(">>> Starting all: " + context.getTestClass().map(Class::getName).orElse("UnknownClass"));
		report();
		System.out.println("--------------------------------------------------------------");
	}

	@Override
	public void afterAll(ExtensionContext context) {
		System.out.println("--------------------------------------------------------------");
		System.out.println("<<< Finished all: " + context.getTestClass().map(Class::getName).orElse("UnknownClass"));
		report();
		System.out.println("=============================================================");

	}

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		System.out.println("=============================================================");
		System.out.println(">>> Starting test: " + context.getTestClass().map(Class::getName).orElse("UnknownClass")
				+ "." + context.getDisplayName());
		report();
		System.out.println("--------------------------------------------------------------");
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		System.out.println("--------------------------------------------------------------");
		System.out.println("<<< Finished test: " + context.getTestClass().map(Class::getName).orElse("UnknownClass")
				+ "." + context.getDisplayName());
		report();
		System.out.println("=============================================================");
	}

	private void report() {
		try {
			System.out.println("free -mh");
			Process free = new ProcessBuilder("free", "-mh").inheritIO().start();
			printProcessOutput(free);
		} catch (IOException e1) {
			System.out.println("Exception while trying to get free memory info");
		}
		try {
			System.out.println("ps aux");
			Process free = new ProcessBuilder("ps", "aux").inheritIO().start();
			printProcessOutput(free);
		} catch (IOException e) {
			System.out.println("Exception while trying to get ps aux info");
		}
		long totalMemory = Runtime.getRuntime().totalMemory(); // Total memory allocated to the JVM
		long freeMemory = Runtime.getRuntime().freeMemory(); // Free memory within the JVM
		long usedMemory = totalMemory - freeMemory; // Used memory within the JVM
		System.out.println("Total Heap Memory: " + totalMemory / (1024 * 1024) + " MB");
		System.out.println("Free Heap Memory: " + freeMemory / (1024 * 1024) + " MB");
		System.out.println("Used Heap Memory: " + usedMemory / (1024 * 1024) + " MB");

		if (SwtTestUtil.isGTK) {
			List<String> paths = new ArrayList<>();
			Path fd = Paths.get("/proc/self/fd/");
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fd)) {
				directoryStream.forEach(path -> {
					String resolvedPath = resolveSymLink(path);
					if (isTestRelatedFileDescriptor(resolvedPath)) {
						paths.add(resolvedPath);
					}
				});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Failed to get file descriptor(s)" + paths.size());
				e.printStackTrace(System.out);
			}
			Collections.sort(paths);
			System.out.println(" File descriptors == Count" + paths.size());
			paths.forEach(System.out::println);
		}
	}

	private void printProcessOutput(Process process) throws IOException {
		String result = new String(process.getInputStream().readAllBytes());
		String err = new String(process.getErrorStream().readAllBytes());
		System.out.println(result);
		if (!err.isBlank()) {
			System.out.println("stderr:");
			System.out.println(err);
		}
	}

	private static boolean isTestRelatedFileDescriptor(String fileDescriptorPath) {
		// Do not consider file descriptors of Maven artifacts that are currently opened
		// by other Maven plugins executed in parallel build (such as parallel
		// compilation of the swt.tools bundle etc.)
		return fileDescriptorPath != null && !fileDescriptorPath.contains(".m2") && !fileDescriptorPath.endsWith(".jar")
				&& !fileDescriptorPath.contains("target/classes");
	}

	private static String resolveSymLink(Path path) {
		try {
			return Files.isSymbolicLink(path) ? Files.readSymbolicLink(path).toString() : path.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
