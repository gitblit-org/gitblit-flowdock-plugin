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

import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.Version;

import com.gitblit.extensions.GitblitPlugin;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.servlet.GitblitContext;

public class Plugin extends GitblitPlugin {

	public static final String SETTING_DEFAULT_FLOW = "flowdock.defaultFlow";

	public static final String SETTING_DEFAULT_TOKEN = "flowdock.defaultToken";

	public static final String SETTING_FIXED_COMMIT_TAGS = "flowdock.fixedCommitTags";

	public static final String SETTING_FIXED_TICKET_TAGS = "flowdock.fixedTicketTags";

	public static final String SETTING_FLOW_TOKEN = "flowdock.%s.token";

	public static final String SETTING_USE_PROJECT_FLOWS = "flowdock.useProjectFlows";

	public static final String SETTING_POST_PERSONAL_REPOS = "flowdock.postPersonalRepos";

	public static final String SETTING_POST_TICKETS = "flowdock.postTickets";

	public static final String SETTING_POST_TICKET_COMMENTS = "flowdock.postTicketComments";

	public static final String SETTING_POST_BRANCHES = "flowdock.postBranches";

	public static final String SETTING_POST_TAGS = "flowdock.postTags";

	public Plugin(PluginWrapper wrapper) {
		super(wrapper);

		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		FlowDock.init(runtimeManager);
	}

	@Override
	public void start() {
		log.debug("{} STARTED.", getWrapper().getPluginId());
	}

	@Override
	public void stop() {
		FlowDock.instance().stop();
		log.debug("{} STOPPED.", getWrapper().getPluginId());
	}

	@Override
	public void onInstall() {
		log.debug("{} INSTALLED.", getWrapper().getPluginId());
	}

	@Override
	public void onUpgrade(Version oldVersion) {
		log.debug("{} UPGRADED from {}.", getWrapper().getPluginId(), oldVersion);
	}

	@Override
	public void onUninstall() {
		log.debug("{} UNINSTALLED.", getWrapper().getPluginId());
	}
}
