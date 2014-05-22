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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.gitblit.models.UserModel;
import com.gitblit.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

/**
 * Payload for push notifications.
 *
 * @author James Moger
 *
 */
public class GitPayload extends Payload implements Serializable {

	private static final long serialVersionUID = 1L;

	private Ident sender;

	@SerializedName("ref_name")
	private String refName;

	private Repo repository;

	private List<Commit> commits;

	private int size;

	private String before;

	private String after;

	private String ref;

	@SerializedName("compare")
	private String compareUrl;

	public GitPayload() {
		super();
		this.sender = new Ident();
		this.repository = new Repo();
		this.commits = new ArrayList<>();
	}

	@Override
	public String getEndPoint(String token) {
		return String.format("https://api.flowdock.com/v1/git/%s", token);
	}

	@Override
	public boolean postForm() {
		return true;
	}

	public GitPayload repoUrl(String url) {
		this.repository.url = url;
		return this;
	}

	public GitPayload diffUrl(String url) {
		this.compareUrl = url;
		return this;
	}

	public GitPayload pusher(UserModel user) {
		return pusher(user.getDisplayName(), user.emailAddress);
	}

	public GitPayload pusher(String name, String email) {
		this.sender.name = name;
		this.sender.email = email;
		return this;
	}

	public GitPayload tags(List<String> tags) {
		setTags(tags);
		return this;
	}

	public GitPayload repository(String name) {
		this.repository.name = StringUtils.stripDotGit(name);
		return this;
	}

	public GitPayload ref(String ref) {
		this.ref = ref;
		return this;
	}

	public GitPayload refName(String refName) {
		this.refName = refName;
		return this;
	}

	public GitPayload before(String id) {
		this.before = id;
		return this;
	}

	public GitPayload after(String id) {
		this.after = id;
		return this;
	}

	public GitPayload add(Commit commit) {
		if (commits.size() < 20) {
			this.commits.add(commit);
		}
		this.size++;
		return this;
	}

	public String getPusherName() {
		return sender.name;
	}

	public void setPusherName(String name) {
		this.sender.name = name;
	}

	public String getPusherEmail() {
		return sender.email;
	}

	public void setPusherEmail(String email) {
		this.sender.email = email;
	}

	public static class Commit implements Serializable {

		private static final long serialVersionUID = 1L;

		String url;

		String id;

		Ident author;

		Date timestamp;

		String message;

		List<String> added;

		List<String> modified;

		List<String> removed;
	}

	public static class Repo implements Serializable {

		private static final long serialVersionUID = 1L;

		String url;

		String name;

	}

	public static class Ident implements Serializable {

		private static final long serialVersionUID = 1L;

		String name;

		String email;

		public Ident() {
		}

		public Ident(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}
}
