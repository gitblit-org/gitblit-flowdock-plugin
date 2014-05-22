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
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.Mailing;
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;
import com.gitblit.models.UserModel;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.tickets.TicketNotifier;

/**
 * Generates a standard Gitblit ticket email payload.
 *
 * @author James Moger
 *
 */
public class TicketEmailGenerator extends TicketPayloadGenerator {

	public TicketEmailGenerator() {
		super();
	}

    @Override
    public Payload generatePayload(TicketModel ticket) {
    	return generate(ticket, ticket.changes.get(0));
    }

    @Override
    public Payload generatePayload(TicketModel ticket, Change change) {
    	return generate(ticket, change);
    }

    private Payload generate(TicketModel ticket, Change change) {

    	IGitblit gitblit = GitblitContext.getManager(IGitblit.class);
    	TicketNotifier notifier = gitblit.getTicketService().createNotifier();
    	Mailing mailing = notifier.queueMailing(ticket);

    	String ticketUrl = getUrl(ticket);

    	IUserManager userManager = GitblitContext.getManager(IUserManager.class);
    	UserModel authorModel = userManager.getUserModel(change.author);

    	MessagePayload payload = new MessagePayload()
		.from(authorModel)
		.subject(mailing.subject)
		.content(mailing.content)
		.project(getProject(ticket))
		.source(getSource(ticket))
		.tags(getTags(ticket))
		.link(ticketUrl);

    	return payload;
    }
}