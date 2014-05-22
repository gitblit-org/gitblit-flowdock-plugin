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

	@SerializedName("compare")
	String compareUrl;

	String before;

	String after;

	String ref;

	@SerializedName("ref_name")
	String refName;

	Repo repository;

	List<Commit> commits;

	public GitPayload() {
		super();
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

	public GitPayload source(String source) {
		setSource(source);
		return this;
	}

	public GitPayload from(UserModel user) {
		return from(user.getDisplayName(), user.emailAddress);
	}

	public GitPayload from(String name, String address) {
		setFromName(name);
		setFromAddress(address);
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
		this.commits.add(commit);
		return this;
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

		public Ident(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}
}
