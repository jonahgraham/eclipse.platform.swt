package org.eclipse.swt.tests;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestLifecycleExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
		System.out.println("=============================================================");
        System.out.println(">>> Starting test: " + context.getDisplayName());
		report();
		System.out.println("--------------------------------------------------------------");
    }
    @Override
    public void afterTestExecution(ExtensionContext context) {
		System.out.println("--------------------------------------------------------------");
        System.out.println("<<< Finished test: " + context.getDisplayName());
		report();
		System.out.println("=============================================================");
    }

	private void report() {
		try {
			new ProcessBuilder("free", "-mh").inheritIO().start().waitFor(1, TimeUnit.SECONDS);
		} catch (InterruptedException | IOException e1) {
			System.out.println("Exception while trying to get free memory info");
		}
		try {
			new ProcessBuilder("ps", "aux").inheritIO().start().waitFor(1, TimeUnit.SECONDS);
		} catch (InterruptedException | IOException e) {
			System.out.println("Exception while trying to get ps aux info");
		}
		long totalMemory = Runtime.getRuntime().totalMemory(); // Total memory allocated to the JVM
		long freeMemory = Runtime.getRuntime().freeMemory(); // Free memory within the JVM
		long usedMemory = totalMemory - freeMemory; // Used memory within the JVM
		System.out.println("Total Heap Memory: " + totalMemory / (1024 * 1024) + " MB");
		System.out.println("Free Heap Memory: " + freeMemory / (1024 * 1024) + " MB");
		System.out.println("Used Heap Memory: " + usedMemory / (1024 * 1024) + " MB");
	}

}
