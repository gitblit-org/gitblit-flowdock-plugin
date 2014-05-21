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
package com.gitblit.plugin.flowdock.entity;

import java.util.ArrayList;
import java.util.List;

import com.gitblit.Constants;
import com.gitblit.models.UserModel;
import com.google.gson.annotations.SerializedName;


public class Payload {

	public static String sanitize(String value) {
		StringBuilder sb = new StringBuilder();
		for (char c : value.toCharArray()) {
			if (Character.isLetterOrDigit(c)) {
				sb.append(c);
			} else {
				switch (c) {
				case '-':
				case '_':
					sb.append(c);
					break;
				case '/':
					sb.append('_');
					break;
				default:
					continue;
				}
			}
		}
		return sb.toString();
	}

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

	private transient String flow;

	public Payload() {
		this.source = Constants.NAME;
	}

	public Payload subject(String subject) {
		setSubject(subject);
		return this;
	}

	public Payload content(String content) {
		setContent(content);
		return this;
	}

	public Payload from(UserModel user) {
		return from(user.getDisplayName(), user.emailAddress);
	}

	public Payload from(String name, String address) {
		setFromName(name);
		setFromAddress(address);
		return this;
	}

	public Payload replyTo(String replyTo) {
		setReplyTo(replyTo);
		return this;
	}

	public Payload project(String project) {
		setProject(project);
		return this;
	}

	public Payload tags(List<String> tags) {
		setTags(tags);
		return this;
	}

	public Payload link(String link) {
		setLink(link);
		return this;
	}

	public Payload flow(String flow) {
		setFlow(flow);
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

	public String getFlow() {
		return flow;
	}

	public void setFlow(String flow) {
		this.flow = flow;
	}
}
