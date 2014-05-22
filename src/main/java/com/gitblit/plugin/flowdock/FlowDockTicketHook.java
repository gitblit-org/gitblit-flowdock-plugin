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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.Extension;

import com.gitblit.IStoredSettings;
import com.gitblit.extensions.TicketHook;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.utils.StringUtils;

/**
 * The ticket hook will post a message to a flow when a ticket is created or updated.
 *
 * @author James Moger
 *
 */
@Extension
public class FlowDockTicketHook extends TicketHook {

	final String name = getClass().getSimpleName();

	final Logger log = LoggerFactory.getLogger(getClass());

	final FlowDock flowdock;

	final IStoredSettings settings;

	public FlowDockTicketHook() {
		super();

		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		FlowDock.init(runtimeManager);
    	flowdock = FlowDock.instance();
    	settings = runtimeManager.getSettings();
	}

    @Override
    public void onNewTicket(TicketModel ticket) {
    	if (!shallPost(ticket, ticket.changes.get(0))) {
			return;
		}

    	TicketPayloadGenerator endpoint = getGenerator();
    	Payload payload = endpoint.generatePayload(ticket);

    	post(ticket, payload);
    }

    @Override
    public void onUpdateTicket(TicketModel ticket, Change change) {
    	if (!shallPost(ticket, change)) {
			return;
		}

    	TicketPayloadGenerator endpoint = getGenerator();
    	Payload payload = endpoint.generatePayload(ticket, change);

    	post(ticket, payload);
    }

    /**
     * Returns an instance of the payload generator.
     *
     * @return a payload generator
     */
    private TicketPayloadGenerator getGenerator() {
    	String defaultClazz = TicketEmailGenerator.class.getName();
    	String clazz = settings.getString(Plugin.SETTING_TICKET_PAYLOAD_GENERATOR, defaultClazz);
    	if (StringUtils.isEmpty(clazz)) {
    		clazz = defaultClazz;
    	}
    	try {
    		return (TicketPayloadGenerator) Class.forName(clazz).newInstance();
    	} catch (Throwable t) {
    		return new TicketEmailGenerator();
    	}
    }

    /**
     * Determine if a ticket should be posted to a FlowDock flow.
     *
     * @param ticket
     * @return true if the ticket should be posted to a FlowDock flow
     */
    protected boolean shallPost(TicketModel ticket, Change change) {
    	boolean shallPostTicket = settings.getBoolean(Plugin.SETTING_POST_TICKETS, true);

    	if (shallPostTicket) {
    		if (change.hasReview()) {
    			shallPostTicket = true;
    		} else if (change.hasPatchset()) {
    			shallPostTicket = true;
    		} else if (change.isMerge()) {
    			shallPostTicket = true;
    		} else if (change.isStatusChange()) {
    			shallPostTicket = true;
    		} else if (change.hasComment() && settings.getBoolean(Plugin.SETTING_POST_TICKET_COMMENTS, true)) {
    			shallPostTicket = true;
    		} else {
    			shallPostTicket = false;
    		}
    	}

    	if (!shallPostTicket) {
    		return false;
    	}

		IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
		RepositoryModel repository = repositoryManager.getRepositoryModel(ticket.repository);
		boolean shallPostRepo = flowdock.shallPost(repository);
		return shallPostRepo;
    }

    protected void post(TicketModel ticket, Payload payload) {

    	if (payload == null) {
    		return;
    	}

    	IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
		RepositoryModel repository = repositoryManager.getRepositoryModel(ticket.repository);

    	flowdock.setFlow(repository, payload);
   		flowdock.sendAsync(payload);
    }
}