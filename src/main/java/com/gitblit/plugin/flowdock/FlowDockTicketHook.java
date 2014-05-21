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
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.Extension;

import com.gitblit.Constants;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.extensions.TicketHook;
import com.gitblit.manager.IGitblit;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.TicketModel;
import com.gitblit.models.TicketModel.Change;
import com.gitblit.models.TicketModel.Patchset;
import com.gitblit.models.TicketModel.Review;
import com.gitblit.models.UserModel;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.utils.ActivityUtils;
import com.gitblit.utils.ArrayUtils;
import com.gitblit.utils.BugtraqProcessor;
import com.gitblit.utils.MarkdownUtils;
import com.gitblit.utils.StringUtils;

/**
 * This hook will post a message to a flow when a ticket is created or updated.
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

	private final String addPattern = "<span style=\"color:darkgreen;\">+{0}</span>";

	private final String delPattern = "<span style=\"color:darkred;\">-{0}</span>";

	public FlowDockTicketHook() {
		super();

		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		FlowDock.init(runtimeManager);
    	flowdock = FlowDock.instance();
    	settings = runtimeManager.getSettings();
	}

    @Override
    public void onNewTicket(TicketModel ticket) {
    	if (!shallPost(ticket)) {
			return;
		}

    	String ticketUrl = getUrl(ticket);

		Set<TicketModel.Field> fieldExclusions = new HashSet<TicketModel.Field>();
		fieldExclusions.addAll(Arrays.asList(TicketModel.Field.watchers, TicketModel.Field.voters,
				TicketModel.Field.status, TicketModel.Field.mentions));

    	Change change = ticket.changes.get(0);
    	IUserManager userManager = GitblitContext.getManager(IUserManager.class);
    	UserModel authorModel = userManager.getUserModel(change.author);
    	String subject = getSubject(ticket, String.format("new %s ticket", ticket.type));

    	StringBuilder sb = new StringBuilder();
    	sb.append(String.format("<b>%s</b> has created <b>%s</b> <a href=\"%s\">ticket-%s</a>", authorModel.getDisplayName(),
    			StringUtils.stripDotGit(ticket.repository), ticketUrl, ticket.number));

    	fields(sb, ticket, ticket.changes.get(0), fieldExclusions);

    	MessagePayload payload = new MessagePayload()
    		.from(authorModel)
    		.subject(subject)
    		.content(sb.toString())
    		.project(getProject(ticket))
    		.source(getSource(ticket))
    		.tags(getTags(ticket))
    		.link(ticketUrl);

   		flowdock.sendAsync(payload);
    }

    @Override
    public void onUpdateTicket(TicketModel ticket, Change change) {
    	if (!shallPost(ticket)) {
			return;
		}
		Set<TicketModel.Field> fieldExclusions = new HashSet<TicketModel.Field>();
		fieldExclusions.addAll(Arrays.asList(TicketModel.Field.watchers, TicketModel.Field.voters,
				TicketModel.Field.mentions, TicketModel.Field.title, TicketModel.Field.body,
				TicketModel.Field.mergeSha));

		IUserManager userManager = GitblitContext.getManager(IUserManager.class);
		UserModel authorModel = userManager.getUserModel(change.author);
		String author = "<b>" + authorModel.getDisplayName() + "</b>";
		String url = String.format("<a href=\"%s\">ticket-%s</a>", getUrl(ticket), ticket.number);
		String repo = "<b>" + StringUtils.stripDotGit(ticket.repository) + "</b>";
		String subject = null;
		String msg = null;

		if (change.hasReview()) {
			/*
			 * Patchset review
			 */
			subject = getSubject(ticket, String.format("reviewed patchset %s-%s",
					change.patchset.number, change.patchset.rev));
			StringBuilder sb = new StringBuilder();
    		sb.append(String.format("%s has reviewed %s %s patchset %s-%s", author, repo, url,
    				change.patchset.number, change.patchset.rev));
    		sb.append("<p/>");

    		Review review = change.review;
    		String d = settings.getString(Keys.web.datestampShortFormat, "yyyy-MM-dd");
			String t = settings.getString(Keys.web.timeFormat, "HH:mm");
			DateFormat df = new SimpleDateFormat(d + " " + t);
			List<Change> reviews = ticket.getReviews(ticket.getPatchset(review.patchset, review.rev));
			sb.append("<table><thead<tr><th>Date</th><th>Reviewer</th><th>Score</th><th>Description</th></tr></thead><tbody>\n");
			for (Change c : reviews) {
				String name = c.author;
				UserModel u = userManager.getUserModel(change.author);
				if (u != null) {
					name = u.getDisplayName();
				}
				String score;
				switch (change.review.score) {
				case approved:
					score = MessageFormat.format(addPattern, c.review.score.getValue());
					break;
				case vetoed:
					score = MessageFormat.format(delPattern, Math.abs(c.review.score.getValue()));
					break;
				default:
					score = "" + c.review.score.getValue();
				}
				String date = df.format(c.date);
				sb.append(String.format("<tr><td>%1$s</td><td>%2$s</td><td>%3$s</td><td>%4$s</td></tr>\n",
						date, name, score, c.review.score.toString()));
			}
			sb.append("</tbody></table>");
			msg = sb.toString();

		} else if (change.hasPatchset()) {
			/*
			 * New Patchset
			 */
			Patchset ps = change.patchset;
			String tip = ps.tip;
			String base;
			String leadIn;
			if (change.patchset.rev == 1) {
				if (change.patchset.number == 1) {
					/*
					 * Initial proposal
					 */
					subject = getSubject(ticket, "proposal pushed");
					leadIn = String.format("%s has pushed a proposal for %s %s", author, repo, url);
				} else {
					/*
					 * Rewritten patchset
					 */
					subject = getSubject(ticket, String.format("patchset %s pushed (%s)", ps.number, ps.type));
					leadIn = String.format("%s has rewritten the patchset for %s %s (%s)",
							author, repo, url, ps.type);
				}
				base = change.patchset.base;
			} else {
				/*
				 * Fast-forward patchset update
				 */
				String noun = ps.added == 1 ? "commit" : "commits";
				subject = getSubject(ticket, String.format("added %s %s", ps.added, noun));
				leadIn = String.format("%s has added %s %s to %s %s", author, ps.added, noun, repo, url);
				Patchset prev = ticket.getPatchset(ps.number, ps.rev - 1);
				base = prev.tip;
			}

			StringBuilder sb = new StringBuilder();
			sb.append(leadIn);

			// show the fields above the commit list
			fields(sb, ticket, change, fieldExclusions);

			// abbreviated commit list
			List<RevCommit> commits = getCommits(ticket.repository, base, tip);
			sb.append("\n<table><tbody>\n");
			int shortIdLen = settings.getInteger(Keys.web.shortCommitIdLength, 6);
			int maxCommits = 5;
			for (int i = 0; i < Math.min(maxCommits, commits.size()); i++) {
				RevCommit commit = commits.get(i);
				String username = "";
				String email = "";
				if (commit.getAuthorIdent().getEmailAddress() != null) {
					username = commit.getAuthorIdent().getName();
					email = commit.getAuthorIdent().getEmailAddress().toLowerCase();
					if (StringUtils.isEmpty(username)) {
						username = email;
					}
				} else {
					username = commit.getAuthorIdent().getName();
					email = username.toLowerCase();
				}
				String gravatarUrl = ActivityUtils.getGravatarThumbnailUrl(email, 16);
				String commitUrl = getUrl(ticket.repository, null, commit.getName());
				String shortId = commit.getName().substring(0, shortIdLen);
				String shortMessage = StringUtils.trimString(commit.getShortMessage(), Constants.LEN_SHORTLOG);
				String row = String.format("<tr><td><img src=\"%s\"/></td><td><pre><a href=\"%s\">%s</a></pre></td><td>%s</td></tr>\n",
						gravatarUrl, commitUrl, shortId, shortMessage);
				sb.append(row);
			}
			sb.append("</tbody></table>\n");

			// compare link
			if (commits.size() > 1) {
				String compareUrl = getUrl(ticket.repository, base, tip);
				String compareText;
				if (commits.size() > maxCommits) {
					int diff = commits.size() - maxCommits;
					if (diff == 1) {
						compareText = "1 more commit";
					} else {
						compareText = String.format("%d more commits", diff);
					}
				} else {
					compareText = String.format("view comparison of these %s commits", commits.size());
				}
				sb.append(String.format("<a href=\"%s\">%s</a>\n", compareUrl, compareText));
			}

			msg = sb.toString();

		} else if (change.isMerge()) {
			/*
			 * Merged
			 */
			subject = getSubject(ticket, String.format("merged to %s", ticket.mergeTo));
			msg = String.format("%s has merged %s %s to <b>%s</b>", author, repo, url, ticket.mergeTo);
		} else if (change.isStatusChange()) {
			/*
			 * Status Change
			 */
			subject = getSubject(ticket, String.format("status changed to %s", ticket.status));
			msg = String.format("%s has changed the status of %s %s", author, repo, url);
		} else if (change.hasComment() && settings.getBoolean(Plugin.SETTING_POST_TICKET_COMMENTS, true)) {
			/*
			 * Comment
			 */
			subject = getSubject(ticket, "comment added");
			msg = String.format("%s has commented on %s %s", author, repo, url);
		}

		if (msg == null) {
			// not a change we are reporting
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(msg);

		// fields on patchset changes are output above this point
		if (!change.hasPatchset()) {
			fields(sb, ticket, change, fieldExclusions);
		}

    	IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
		RepositoryModel repository = repositoryManager.getRepositoryModel(ticket.repository);

    	String ticketUrl = getUrl(ticket);

    	MessagePayload payload = new MessagePayload()
    		.from(authorModel)
    		.subject(subject)
    		.content(sb.toString())
    		.project(getProject(ticket))
    		.source(getSource(ticket))
    		.tags(getTags(ticket))
    		.link(ticketUrl);

   		flowdock.setFlow(repository, payload);
   		flowdock.sendAsync(payload);
    }

	protected String getSubject(TicketModel ticket, String message) {
		return ticket.title;
	}

	protected String getProject(TicketModel ticket) {
		// Flowdock supports very limited characters so we strip the repository name
		return StringUtils.stripDotGit(StringUtils.getLastPathElement(ticket.repository));
	}

	protected String getSource(TicketModel ticket) {
		return Constants.NAME;
	}

	protected List<String> getTags(TicketModel ticket) {
    	List<String> tags = new ArrayList<String>();
    	tags.add("ticket-" + ticket.number);
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

    protected void fields(StringBuilder sb, TicketModel ticket, Change change, Set<TicketModel.Field> fieldExclusions) {
    	Map<TicketModel.Field, String> filtered = new HashMap<TicketModel.Field, String>();
    	if (change.hasFieldChanges()) {
    		for (Map.Entry<TicketModel.Field, String> fc : change.fields.entrySet()) {
    			if (!fieldExclusions.contains(fc.getKey())) {
    				// field is included
    				filtered.put(fc.getKey(), fc.getValue());
    			}
    		}
    	}

    	if (change.hasComment() && settings.getBoolean(Plugin.SETTING_POST_TICKET_COMMENTS, true)) {
    		// transform Markdown comment
    		sb.append("<br/>\n");
    		String comment = renderMarkdown(change.comment.text, ticket.repository);
    		sb.append(comment);
    	}

    	// ensure we have some basic context fields
    	if (!filtered.containsKey(TicketModel.Field.title)) {
    		filtered.put(TicketModel.Field.title, ticket.title);
    	}

    	// sort by field ordinal
    	List<TicketModel.Field> fields = new ArrayList<TicketModel.Field>(filtered.keySet());
    	Collections.sort(fields);

    	if (fields.size() > 0) {
			sb.append("\n<table><tbody>\n");
			for (TicketModel.Field field : fields) {
				String value;
				if (filtered.get(field) == null) {
					continue;
				} else {
					value = filtered.get(field);

					if (TicketModel.Field.body == field) {
						// transform the body to html
						value = renderMarkdown(value, ticket.repository);
					} else if (TicketModel.Field.topic == field) {
						// link bugtraq matches
						value = renderBugtraq(value, ticket.repository);
    				} else if (TicketModel.Field.responsible == field) {
    					// lookup display name of the user
    					value = getDisplayName(value);
    				}
				}
				sb.append(String.format("<tr><td><b>%1$s:<b/></td><td>%2$s</td></tr>\n", field.name(), value));
			}
			sb.append("</tbody></table>\n");
    	}
    }

    protected String renderMarkdown(String markdown, String repository) {
    	if (StringUtils.isEmpty(markdown)) {
    		return markdown;
    	}

		// transform the body to html
    	String bugtraq = renderBugtraq(markdown, repository);
		String html = MarkdownUtils.transformGFM(settings, bugtraq, repository);

		// strip paragraph tags
		html = html.replace("<p>", "");
		html = html.replace("</p>", "<br/><br/>");
		return html;
    }

    protected String renderBugtraq(String value, String repository) {
    	if (StringUtils.isEmpty(value)) {
    		return value;
    	}

    	IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
		Repository db = repositoryManager.getRepository(repository);
		try {
			BugtraqProcessor bugtraq = new BugtraqProcessor(settings);
			value = bugtraq.processText(db, repository, value);
		} finally {
			db.close();
		}
		return value;
    }

    protected String getDisplayName(String username) {
    	if (StringUtils.isEmpty(username)) {
    		return username;
    	}

		IUserManager userManager = GitblitContext.getManager(IUserManager.class);
		UserModel user = userManager.getUserModel(username);
		if (user != null) {
			String displayName = user.getDisplayName();
			if (!StringUtils.isEmpty(displayName) && !username.equals(displayName)) {
				return displayName;
			}
		}
		return username;
    }

    /**
     * Determine if a ticket should be posted to a FlowDock flow.
     *
     * @param ticket
     * @return true if the ticket should be posted to a FlowDock flow
     */
    protected boolean shallPost(TicketModel ticket) {
    	IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
    	boolean shallPostTicket = runtimeManager.getSettings().getBoolean(Plugin.SETTING_POST_TICKETS, true);
    	if (!shallPostTicket) {
    		return false;
    	}

		IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
		RepositoryModel repository = repositoryManager.getRepositoryModel(ticket.repository);
		boolean shallPostRepo = flowdock.shallPost(repository);
		return shallPostRepo;
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

    private List<RevCommit> getCommits(String repositoryName, String baseId, String tipId) {
    	IRepositoryManager repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
    	Repository db = repositoryManager.getRepository(repositoryName);
    	List<RevCommit> list = new ArrayList<RevCommit>();
		RevWalk walk = new RevWalk(db);
		walk.reset();
		walk.sort(RevSort.TOPO);
		walk.sort(RevSort.REVERSE, true);
		try {
			RevCommit tip = walk.parseCommit(db.resolve(tipId));
			RevCommit base = walk.parseCommit(db.resolve(baseId));
			walk.markStart(tip);
			walk.markUninteresting(base);
			for (;;) {
				RevCommit c = walk.next();
				if (c == null) {
					break;
				}
				list.add(c);
			}
		} catch (IOException e) {
			// Should never happen, the core receive process would have
			// identified the missing object earlier before we got control.
			log.error("failed to get commits", e);
		} finally {
			walk.release();
			db.close();
		}
		return list;
	}
}