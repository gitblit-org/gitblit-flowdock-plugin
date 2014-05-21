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

import com.gitblit.Constants;
import com.gitblit.models.UserModel;
import com.google.gson.annotations.SerializedName;

/**
 * General purpose message payload.
 *
 * @author James Moger
 *
 */
public class MessagePayload extends Payload  implements Serializable {

	private static final long serialVersionUID = 1L;

	private String source;

	private String subject;

	private String content;

	@SerializedName("from_name")
	private String fromName;

	@SerializedName("from_address")
	private String fromAddress;

	@SerializedName("reply_to")
	private String replyTo;

	private String project;

	private List<String> tags;

	private String link;

	public MessagePayload() {
		this.source = Constants.NAME;
	}

	@Override
	public String getEndPoint(String token) {
		return String.format("https://api.flowdock.com/v1/messages/team_inbox/%s", token);
	}

	public MessagePayload subject(String subject) {
		setSubject(subject);
		return this;
	}

	public MessagePayload content(String content) {
		setContent(content);
		return this;
	}

	public MessagePayload from(UserModel user) {
		return from(user.getDisplayName(), user.emailAddress);
	}

	public MessagePayload from(String name, String address) {
		setFromName(name);
		setFromAddress(address);
		return this;
	}

	public MessagePayload replyTo(String replyTo) {
		setReplyTo(replyTo);
		return this;
	}

	public MessagePayload project(String project) {
		setProject(project);
		return this;
	}

	public MessagePayload source(String source) {
		setSource(source);
		return this;
	}

	public MessagePayload tags(List<String> tags) {
		setTags(tags);
		return this;
	}

	public MessagePayload link(String link) {
		setLink(link);
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String name) {
		this.fromName = name;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String address) {
		this.fromAddress = address;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = sanitize(project);
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = sanitize(source);
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		if (tags != null) {
			this.tags = new ArrayList<String>();
			for (String tag : tags) {
				tags.add(sanitize(tag));
			}
		}
		this.tags = tags;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}
}
