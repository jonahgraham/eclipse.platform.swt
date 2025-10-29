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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.swt.tests.ProccessStats.Stats;
import org.eclipse.swt.tests.ProccessStats.StatsBuilder;
import org.eclipse.swt.tests.junit.SwtTestUtil;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.MultipleFailuresError;

public class CheckForLeaksExtension
		implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
	private static final boolean VERBOSE = false;
	/**
	 * Collecting all the system information is a non-trivial operation, therefore
	 * unless you are actively diagnosing a leak we only collect stats at the
	 * beginning and end of a test class (beforeAll/afterAll). Set to true to
	 * collect stats on each test (beforeEach/afterEach)
	 */
	private static final boolean COLLECT_ON_EACH_TEST = true;

	/**
	 * The first time we collected stats - we could assign this at init, but we
	 * really don't mind usage the JUnit uses, so start after that.
	 */
	private static Stats initialStats;
	private Stats beforeAllStats;
	private Stats beforeEachStats;
	private boolean initialStatsResetOnFirstBrowserTest = false;

	@Override
	public void beforeAll(ExtensionContext context) {
		Stats stats = ProccessStats.collect();
		beforeAllStats = stats;
		if (initialStats == null) {
			initialStats = stats;
		}
		report(stats, context, "beforeAll", true);
	}

	@Override
	public void afterAll(ExtensionContext context) throws InterruptedException {
//		int i = 0;
//		while (i++ < 100000) {
//			Stats stats = ProccessStats.collect();
//			System.out.println(stats);
//			Thread.sleep(2500);
//		}
		Stats stats = ProccessStats.collect();
		report(stats, context, "afterAll", true);
//		assertTollerableGrowth(context, beforeAllStats, stats);
//		assertTollerableGrowth(firstEverStats, stats);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		initialStats = ProccessStats.collect();
//		if (COLLECT_ON_EACH_TEST) {
//			Stats stats = ProccessStats.collect();
//			beforeEachStats = stats;
//			report(stats, context, "beforeEach", false);
//		}
	}

	@Override
	public void afterEach(ExtensionContext context) throws InterruptedException {
		assertTollerableGrowth(context);
//		if (COLLECT_ON_EACH_TEST) {
//			Stats stats = ProccessStats.collect();
//			report(stats, context, "afterEach", false);
//			assertTollerableGrowth(context);
////		assertTollerableGrowth(firstEverStats, stats);
//		}
	}

	@SuppressWarnings("unused")
	private void assert0Growth(Stats before, Stats after) throws MultipleFailuresError {
		Stats diff = after.subtract(before);
		assertAll("Process Stats had growth\ndiff:\n" + diff.toString() + "\nabsolute:\n" + after, //
				() -> assertTrue(diff.usedSystemMemory <= 0, "usedSystemMemory"), //
				() -> assertTrue(Stats.total(diff.systemThreads) <= 0, "systemThreadCount"), //
				() -> assertTrue(diff.usedVmMemory <= 0, "usedVmMemory"), //
				() -> assertTrue(Stats.total(diff.vmThreads) <= 0, "vmThreadCount"),
				() -> assertTrue(Stats.total(diff.openFiles) <= 0, "totalOpenFiles") //
		);
	}

	private String createFailReport(Stats diff, Stats diffFromInitial, Stats before, Stats after) {
		if (true) {
			return String.format("""
					Process Stats had unexpected growth
					DIFF:
					%s
					DIFF FROM INITIAL:
					%s
					BEFORE:
					%s
					AFTER:
					%s
					""", diff, diffFromInitial, before, after);
		}
		return String.format("""
				Process Stats had unexpected growth
				DIFF:
				%s
					DIFF FROM INITIAL:
					%s
				""", diff, diffFromInitial);
	}

	private void assertTollerableGrowth(ExtensionContext context) throws InterruptedException {
//		if (!initialStatsResetOnFirstBrowserTest) {
//			boolean isBrowserTest = context.getTestInstance()
//					.filter(c -> {
//						boolean instance = c instanceof Test_org_eclipse_swt_browser_Browser;
//						return instance;
//					}).isPresent();
//			if (isBrowserTest) {
//				initialStatsResetOnFirstBrowserTest = true;
//				initialStats = after;
//				return;
//			}
//		}

		boolean allOk = allOk(context);
		if (!allOk) {
			Display current = Display.getCurrent();
			if (current == null) {
				// Display is gone, that means it better have cleaned up fully before being
				// fully disposed
			} else {
				// We have a display still, we may need to spin the loop to allow OS level stuff
				// to clear up
				SwtTestUtil.processEvents(1_000, () ->  this.allOk(null));
			}
			// Fail this test, but reset initalStats for the next test to run
			initialStats = ProccessStats.collect();

		}
		assertTrue(allOk);

	}

	private boolean allOk(ExtensionContext context) {
		Stats after = ProccessStats.collect();
		Stats diff = after.subtract(initialStats);

		// allow 100% memory growth limited (at least 1MiB though)
		boolean usedSytemMemoryOk = /*
									 * diff.usedSystemMemory <= before.usedSystemMemory ||
									 */diff.usedSystemMemory < 100_000;

		// allow 100% memory growth limited (at least 1MiB though)
		boolean usedVmMemoryOk = // diff.usedVmMemory <= before.usedVmMemory || diff.usedVmMemory < 1_000_000;
				diff.usedVmMemory < 100_000;

		// ideally we wouldn't allow any new system threads that are unexpected, for now
		// allow up to 25 system threads, TODO make an allowlist for system threads
		// (like "C2 Compiler Thread", "GC Thread...")
		boolean systemThreadCountOk = // Stats.total(diff.systemThreads) <= 0 ||
				Stats.total(diff.systemThreads) <= 0;
		// ideally we wouldn't allow any new vm threads that are unexpected, for now
		// allow up to 25 vm threads, TODO make an allowlist for vm threads (like
		// "Java2D Disposer" and "SWTResourceTracker")
		boolean vmThreadCountOk = // Stats.total(diff.vmThreads) <= 0 ||
				Stats.total(diff.vmThreads) <= 0;
		// ideally we wouldn't allow any new file handles that are unexpected, for now
		// allow up to 25 such file handles threads, TODO make an allowlist for file
		// handles threads (like
		// "/dev/random")
		boolean openFileCountOk = // Stats.total(diff.openFiles) <= 0 ||
				Stats.total(diff.openFiles) <= 0;

		boolean allOk = Arrays
				.asList(usedSytemMemoryOk, usedVmMemoryOk, systemThreadCountOk, vmThreadCountOk, openFileCountOk)
				.stream().allMatch(Boolean::booleanValue);
		if (!allOk && context != null) {
			String testName = context.getTestClass().map(Class::getName).orElse("UnknownClass");
//			if (!all) {
				testName += "." + context.getDisplayName();
//			}
			System.out.println(">>> Diff report for " + testName);

			System.out.println(diff);
		}
		return allOk;
	}

	private static final Map<Pattern, Integer> allowedThreads = Map.of(//
			Pattern.compile("C.*CompilerThr.*"), Integer.MAX_VALUE, //
			Pattern.compile("Common-Cleaner"), Integer.MAX_VALUE, //
			Pattern.compile("Finalizer"), Integer.MAX_VALUE, //
			Pattern.compile("G1.*"), Integer.MAX_VALUE, //
			Pattern.compile("GC Thread.*"), Integer.MAX_VALUE, //
			Pattern.compile("JDWP.*"), Integer.MAX_VALUE, //
			Pattern.compile("RMI.*"), Integer.MAX_VALUE, //
			Pattern.compile("Reference Handler.*"), Integer.MAX_VALUE, //
			Pattern.compile("Signal Dispatcher.*"), Integer.MAX_VALUE //
	);

	private Stats filter(Stats stats) {
		HashMap<String, Integer> systemThreads = filterThreads(stats.systemThreads);
		return new StatsBuilder().systemThreads(systemThreads).build();
	}

	private HashMap<String, Integer> filterThreads(Map<String, Integer> threads) {
		HashMap<String, Integer> filteredThreads = new HashMap<>(threads);
		filteredThreads.entrySet().removeIf(entry -> {
			String name = entry.getKey();
			int count = entry.getValue();
			for (var e : allowedThreads.entrySet()) {
				if (e.getKey().matcher(name).matches()) {
					if (count < e.getValue()) {
						return true;
					}
				}
			}
			return false;
		});
		return filteredThreads;
	}

	private void report(Stats stats, ExtensionContext context, String stage, boolean all) {
		if (VERBOSE) {
			System.out.println("=============================================================");
			String testName = context.getTestClass().map(Class::getName).orElse("UnknownClass");
			if (!all) {
				testName += "." + context.getDisplayName();
			}
			System.out.println(">>> TestLifecycleExtension." + stage + ": " + testName);
			System.gc();
			System.out.println(stats);
			System.out.println("=============================================================");
		}
	}
}
