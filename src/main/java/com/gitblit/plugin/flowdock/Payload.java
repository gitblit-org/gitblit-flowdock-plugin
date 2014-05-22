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
import java.util.List;

import com.gitblit.Constants;
import com.google.gson.annotations.SerializedName;

/**
 * Parent class of payloads.
 *
 * @author James Moger
 *
 */
public abstract class Payload implements Serializable {

	private static final long serialVersionUID = 1L;

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

	private transient String flow;

	private String source;

	@SerializedName("from_name")
	private String fromName;

	@SerializedName("from_address")
	private String fromAddress;

	private List<String> tags;

	public Payload() {
		this.source = Constants.NAME;
	}

	public String getFlow() {
		return flow;
	}

	public void setFlow(String flow) {
		this.flow = flow;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = sanitize(source);
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

	public abstract String getEndPoint(String token);

	public abstract boolean postForm();
}
