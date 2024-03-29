/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.agent.rt.internal.output;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jacoco.agent.rt.internal.IExceptionLogger;
import org.jacoco.core.runtime.AgentOptions;
import org.jacoco.core.runtime.RuntimeData;

/**
 * Output that connects to a TCP port. This controller uses the following agent
 * options:
 * <ul>
 * <li>address</li>
 * <li>port</li>
 * </ul>
 */
public class TcpClientOutput implements IAgentOutput {

	private final IExceptionLogger logger;

	private TcpConnection connection;

	private Thread worker;

	private ScheduledExecutorService executor;

	/**
	 * New controller instance.
	 *
	 * @param logger
	 *            logger to use in case of exceptions is spawned threads
	 */
	public TcpClientOutput(final IExceptionLogger logger) {
		this.logger = logger;
	}

	public void startup(final AgentOptions options, final RuntimeData data)
			throws IOException {
		// keep alive
		executor = Executors.newSingleThreadScheduledExecutor();
		worker = new Thread(new Runnable() {
			public void run() {
				connectToServer(options, data);
				System.out.println("====> connect completed");
			}
		});
		worker.setName(getClass().getName());
		worker.setDaemon(true);
		worker.start();
		// heart beat
		startHeartBeat(options.getHeartBeatInterval());
	}

	public void shutdown() throws Exception {
		connection.close();
		// 关闭 executor
		executor.shutdown();
		worker.join();
	}

	public void writeExecutionData(final String traceId, final boolean reset)
			throws IOException {
		connection.writeExecutionData(traceId, reset);
	}

	/**
	 * Open a socket based on the given configuration.
	 *
	 * @param options
	 *            address and port configuration
	 * @return opened socket
	 * @throws IOException
	 */
	protected Socket createSocket(final AgentOptions options)
			throws IOException {
		return tryConnect(options.getAddress(), options.getPort(),
				options.getRetryCount(), options.getRetryDelay());
	}

	private Socket tryConnect(final String address, final int port,
			final int retryCount, final int retryDelay) throws IOException {
		int count = 0;
		while (true) {
			try {
				return new Socket(address, port);
			} catch (final IOException e) {
				if (retryCount > 0 && ++count > retryCount) {
					System.err.println(
							"====> try connect timeout, retry count is : "
									+ retryCount);
					throw e;
				}
				sleep(retryDelay);
			}
		}
	}

	private void sleep(final int retryDelay) throws InterruptedIOException {
		try {
			Thread.sleep(retryDelay);
		} catch (final InterruptedException e) {
			throw new InterruptedIOException();
		}
	}

	protected void startHeartBeat(int interval) {
		System.out.println("====> init heart beat,interval is : " + interval);
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (connection == null || connection.isClosed()) {
					System.err.println(
							"====> heart beat failed, connection is not init or has been closed.");
					return;
				}
				try {
					connection.visitSessionInfo();
				} catch (Exception e) {
					System.err.println("====> heart beat failed, error is:"
							+ e.getMessage());
					logger.logExeption(e);
				}
			}
		}, 0, interval, TimeUnit.SECONDS);
	}

	protected void connectToServer(final AgentOptions options,
			final RuntimeData data) {
		System.out.println("====> connect to server");
		// reconnect
		try {
			Socket socket = createSocket(options);
			connection = new TcpConnection(socket, data);
			connection.init();
			connection.run();
			if (options.getKeepAlive() && connection.isClosed()) {
				throw new Exception("connect is closed");
			}
		} catch (Exception ex) {
			System.err.println("====> connect to server error, error is:"
					+ ex.getMessage());
			logger.logExeption(ex);
			connectToServer(options, data);
		}
	}

}
