/**
 * Copyright (C) 2014 gitblit.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitblit.plugin.flowdock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gitblit.manager.IUserManager;
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;
import com.gitblit.models.TicketModel.Field;
import com.gitblit.models.UserModel;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

/**
 * Pretend to be a JIRA issue.
 *
 * @author James Moger
 *
 */
public class JiraPayload extends Payload implements Serializable {

	private static final long serialVersionUID = 1L;

	@SerializedName("issue_key")
	String id;

	@SerializedName("issue_summary")
	String title;

	@SerializedName("issue_description")
	String description;

	@SerializedName("issue_status")
	String status;

	@SerializedName("issue_resolution")
	String resolution;

	@SerializedName("issue_type")
	String type;

	@SerializedName("project_name")
	String project;

	@SerializedName("issue_votes")
	String votes;

	@SerializedName("issue_reporter_name")
	String createdByName;

	@SerializedName("issue_reporter_email")
	String createdByEmail;

	@SerializedName("issue_assignee_name")
	String responsibleName;

	@SerializedName("issue_assignee_email")
	String responsibleEmail;

	@SerializedName("user_name")
	String updaterName;

	@SerializedName("user_email")
	String updaterEmail;

	@SerializedName("event_type")
	String updateType;

	@SerializedName("comment_body")
	String comment;

	@SerializedName("issue_url")
	String url;

	@SerializedName("project_url")
	String projectUrl;

	@SerializedName("issue_changelog")
	List<Changelog> changes;

	@Override
	public String getEndPoint(String token) {
		return String.format("https://api.flowdock.com/v1/jira/%s", token);
	}

	@Override
	public boolean postForm() {
		return false;
	}

	public JiraPayload ticket(TicketModel ticket) {
		id = String.valueOf(ticket.number);
		title = ticket.title;
		description = ticket.body;
		status = ticket.status.name();
		if (ticket.isClosed()) {
			resolution = ticket.status.name();
		}
		type = ticket.type.name();
		project = StringUtils.stripDotGit(ticket.repository);
		votes = String.valueOf(ticket.getVoters().size());

		IUserManager userManager = GitblitContext.getManager(IUserManager.class);

		UserModel createdBy = userManager.getUserModel(ticket.createdBy);
		if (createdBy != null) {
			createdByName = createdBy.getDisplayName();
			createdByEmail = createdBy.emailAddress;
		} else {
			createdByName = ticket.createdBy;
		}

		if (!StringUtils.isEmpty(ticket.responsible)) {
			UserModel responsible = userManager.getUserModel(ticket.responsible);
			if (responsible != null) {
				responsibleName = responsible.getDisplayName();
				responsibleEmail = responsible.emailAddress;
			} else {
				responsibleName = ticket.responsible;
			}
		}

		Change lastChange = ticket.changes.get(ticket.changes.size() - 1);
		UserModel changedBy = userManager.getUserModel(lastChange.author);
		updaterName = changedBy.getDisplayName();
		updaterEmail = changedBy.emailAddress;

		if (lastChange.hasComment()) {
			comment = lastChange.comment.text;
		}

		if (lastChange.hasFieldChanges()) {
			changes = new ArrayList<Changelog>();
			for (Field field :lastChange.fields.keySet()) {
				Changelog cl = new Changelog();
				cl.field = field.name();
				cl.newValue = lastChange.getField(field).toString();
				changes.add(cl);
			}
		}

		// set the event type
		if (ticket.changes.size() == 1) {
			// new ticket
			updateType = "create";
		} else if (lastChange.hasComment()) {
			// comment on ticket
			updateType = "comment";
		} else if (lastChange.hasField(Field.responsible) && lastChange.getField(Field.responsible) != null) {
			// assign ticket
			updateType = "assign";
		} else if (lastChange.isStatusChange()) {
			// status change
			switch (lastChange.getStatus()) {
			case Open:
				updateType = "reopen";
				break;
			case Resolved:
				updateType = "resolve";
				break;
			default:
				updateType = "close";
				break;
			}
		} else {
			updateType = "update";
		}

		return this;
	}

	public JiraPayload tags(List<String> tags) {
		setTags(tags);
		return this;
	}

	public JiraPayload ticketUrl(String url) {
		this.url = url;
		return this;
	}

	public JiraPayload projectUrl(String url) {
		this.projectUrl = url;
		return this;
	}

	public JiraPayload description(String description) {
		this.description = description;
		return this;
	}

	public JiraPayload comment(String comment) {
		this.comment = comment;
		return this;
	}

	public static class Changelog implements Serializable {

		private static final long serialVersionUID = 1L;

		String field;

		@SerializedName("old_value")
		String oldValue;

		@SerializedName("new_value")
		String newValue;
	}
}
