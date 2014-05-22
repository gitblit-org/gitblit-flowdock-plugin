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
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;

/**
 * Generates a JIRA payload for a ticket.
 *
 * @author James Moger
 *
 */
public class TicketJiraGenerator extends TicketPayloadGenerator {

	public TicketJiraGenerator() {
		super();
	}

    @Override
    public Payload generatePayload(TicketModel ticket) {
    	return generate(ticket);
    }

    @Override
    public Payload generatePayload(TicketModel ticket, Change change) {
    	return generate(ticket);
    }

    private Payload generate(TicketModel ticket) {
    	String ticketUrl = getUrl(ticket);
    	String repoUrl = getUrl(ticket.repository, null, null);

    	JiraPayload payload = new JiraPayload()
		.ticket(ticket)
		.tags(getTags(ticket))
		.ticketUrl(ticketUrl)
		.projectUrl(repoUrl);

    	return payload;
    }
}