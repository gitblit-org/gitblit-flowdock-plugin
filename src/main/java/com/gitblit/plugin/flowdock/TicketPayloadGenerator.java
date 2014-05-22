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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.utils.ArrayUtils;
import com.gitblit.utils.StringUtils;

/**
 * Parent class of ticket payload generators.
 *
 * @author James Moger
 *
 */
public abstract class TicketPayloadGenerator {

	final String name = getClass().getSimpleName();

	final Logger log = LoggerFactory.getLogger(getClass());

	final IStoredSettings settings;

	public TicketPayloadGenerator() {
		super();

		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
    	settings = runtimeManager.getSettings();
	}

	/**
	 * Generate a payload for a new ticket.
	 *
	 * @param ticket
	 * @return a payload
	 */
	public abstract Payload generatePayload(TicketModel ticket);

	/**
	 * Generate a payload for an updated ticket.
	 *
	 * @param ticket
	 * @param change
	 * @return a payload
	 */
	public abstract Payload generatePayload(TicketModel ticket, Change change);

	protected List<String> getTags(TicketModel ticket) {
    	List<String> tags = new ArrayList<String>();
    	tags.add(String.valueOf(ticket.number));
    	tags.add(ticket.type.name().toLowerCase());
    	tags.addAll(settings.getStrings(Plugin.SETTING_FIXED_TICKET_TAGS));

    	List<String> labels = ticket.getLabels();
    	if (!ArrayUtils.isEmpty(labels)) {
    		tags.addAll(labels);
    	}

    	if (!StringUtils.isEmpty(ticket.topic)) {
    		String [] values = ticket.topic.split(" ");
    		for (String value : values) {
    			if (!StringUtils.isEmpty(value)) {
    				tags.add(value);
    			}
    		}
    	}

    	if (!StringUtils.isEmpty(ticket.milestone)) {
    		tags.add(ticket.milestone);
    	}
    	return tags;
	}

	protected String getProject(TicketModel ticket) {
		// Flowdock supports very limited characters so we strip the repository name
		return StringUtils.stripDotGit(StringUtils.getLastPathElement(ticket.repository));
	}

	protected String getSource(TicketModel ticket) {
		return Constants.NAME;
	}

    protected String getUrl(TicketModel ticket) {
    	return GitblitContext.getManager(IGitblit.class).getTicketService().getTicketUrl(ticket);
    }

    /**
     * Returns a link appropriate for the push.
     *
     * If both new and old ids are null, the summary page link is returned.
     *
     * @param repo
     * @param oldId
     * @param newId
     * @return a link
     */
    protected String getUrl(String repo, String oldId, String newId) {
    	IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		String canonicalUrl = runtimeManager.getSettings().getString(Keys.web.canonicalUrl, "https://localhost:8443");

		if (oldId == null && newId != null) {
			// create
			final String hrefPattern = "{0}/commit?r={1}&h={2}";
			return MessageFormat.format(hrefPattern, canonicalUrl, repo, newId);
		} else if (oldId != null && newId == null) {
			// log
			final String hrefPattern = "{0}/log?r={1}&h={2}";
			return MessageFormat.format(hrefPattern, canonicalUrl, repo, oldId);
		} else if (oldId != null && newId != null) {
			// update/compare
			final String hrefPattern = "{0}/compare?r={1}&h={2}..{3}";
			return MessageFormat.format(hrefPattern, canonicalUrl, repo, oldId, newId);
		} else if (oldId == null && newId == null) {
			// summary page
			final String hrefPattern = "{0}/summary?r={1}";
			return MessageFormat.format(hrefPattern, canonicalUrl, repo);
		}

		return null;
    }
}