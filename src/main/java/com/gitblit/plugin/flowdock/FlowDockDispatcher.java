/*
 * Copyright 2014 gitblit.com.
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
package com.gitblit.plugin.flowdock;

import java.io.IOException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import ro.fortsoft.pf4j.Extension;

import com.gitblit.manager.IRuntimeManager;
import com.gitblit.models.UserModel;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.transport.ssh.commands.CommandMetaData;
import com.gitblit.transport.ssh.commands.DispatchCommand;
import com.gitblit.transport.ssh.commands.SshCommand;
import com.gitblit.transport.ssh.commands.UsageExample;
import com.gitblit.transport.ssh.commands.UsageExamples;
import com.gitblit.utils.StringUtils;

@Extension
@CommandMetaData(name = "flowdock", description = "FlowDock commands")
public class FlowDockDispatcher extends DispatchCommand {

	@Override
	protected void setup() {
		boolean canAdmin = getContext().getClient().getUser().canAdmin();
		if (canAdmin) {
			register(TestCommand.class);
			register(MessageCommand.class);
		}
	}

	@CommandMetaData(name = "test", description = "Post a test message")
	@UsageExamples(examples = {
			@UsageExample(syntax = "${cmd}", description = "Posts a test message to the default flow"),
			@UsageExample(syntax = "${cmd} myFlow", description = "Posts a test message to myFlow")
	})
	public static class TestCommand extends SshCommand {

		@Argument(index = 0, metaVar = "FLOW", usage = "Destination Flow for message")
		String flow;

		/**
		 * Post a test message
		 */
		@Override
		public void run() throws Failure {
			UserModel user = getContext().getClient().getUser();

		    MessagePayload payload = new MessagePayload();
		    payload.subject("Test message from Gitblit");
		    payload.content("Test message sent from Gitblit");
		    payload.from(user);

		    if (!StringUtils.isEmpty(flow)) {
		    	payload.setFlow(flow);
		    }

			try {
				IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
				FlowDock.init(runtimeManager);
				FlowDock.instance().send(payload);
			} catch (IOException e) {
			    throw new Failure(1, e.getMessage(), e);
			}
		}
	}

	@CommandMetaData(name = "send", aliases = { "post" }, description = "Asynchronously post a message")
	@UsageExamples(examples = {
			@UsageExample(syntax = "${cmd} -m \"'this is a test'\"", description = "Asynchronously posts a message to the default flow"),
			@UsageExample(syntax = "${cmd} myFlow -m \"'this is a test'\"", description = "Asynchronously posts a message to myFlow")
	})
	public static class MessageCommand extends SshCommand {

		@Argument(index = 0, metaVar = "FLOW", usage = "Destination Flow for message")
		String flow;

		@Option(name = "--message", aliases = {"-m" }, metaVar = "-|MESSAGE", required = true)
		String message;

		/**
		 * Post a message
		 */
		@Override
		public void run() throws Failure {
			UserModel user = getContext().getClient().getUser();

		    MessagePayload payload = new MessagePayload();
		    payload.subject(message);
		    payload.content(message);
		    payload.from(user);

		    if (!StringUtils.isEmpty(flow)) {
		    	payload.setFlow(flow);
		    }

			IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
			FlowDock.init(runtimeManager);
		    FlowDock.instance().sendAsync(payload);
		}
	}
}

