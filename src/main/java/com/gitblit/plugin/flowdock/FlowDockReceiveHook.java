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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.Extension;

import com.gitblit.Constants;
import com.gitblit.Keys;
import com.gitblit.extensions.ReceiveHook;
import com.gitblit.git.GitblitReceivePack;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.models.PathModel.PathChangeModel;
import com.gitblit.models.RepositoryModel;
import com.gitblit.plugin.flowdock.GitPayload.Commit;
import com.gitblit.plugin.flowdock.GitPayload.Ident;
import com.gitblit.servlet.GitblitContext;
import com.gitblit.utils.JGitUtils;

/**
 * This hook will post a message to a room when a ref is updated.
 *
 * @author James Moger
 *
 */
@Extension
public class FlowDockReceiveHook extends ReceiveHook {

	final String name = getClass().getSimpleName();

	final Logger log = LoggerFactory.getLogger(getClass());

	final FlowDock flowdock;

	public FlowDockReceiveHook() {
		super();

		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		FlowDock.init(runtimeManager);
    	flowdock = FlowDock.instance();
	}

	@Override
	public void onPreReceive(GitblitReceivePack receivePack, Collection<ReceiveCommand> commands) {
		// NOOP
	}

	@Override
	public void onPostReceive(GitblitReceivePack receivePack, Collection<ReceiveCommand> commands) {
		if (!shallPost(receivePack, commands)) {
			return;
		}

    	IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		try {
			for (ReceiveCommand cmd : commands) {
				if (cmd.getRefName().startsWith(Constants.R_TAGS)) {
			    	boolean shallPostTag = runtimeManager.getSettings().getBoolean(Plugin.SETTING_POST_TAGS, true);
			    	if (!shallPostTag) {
			    		continue;
			    	}
				} else if (cmd.getRefName().startsWith(Constants.R_HEADS)) {
			    	boolean shallPostBranch = runtimeManager.getSettings().getBoolean(Plugin.SETTING_POST_BRANCHES, true);
			    	if (!shallPostBranch) {
			    		continue;
			    	}
				} else {
					// ignore other refs
					continue;
				}

				RepositoryModel repo = receivePack.getRepositoryModel();

				String repoUrl = getUrl(repo.name, null, null);
				String diffUrl = getUrl(repo.name, cmd.getOldId().getName(), cmd.getNewId().getName());

				GitPayload payload = new GitPayload()
				.pusher(receivePack.getUserModel())
				.repository(repo.name)
				.repoUrl(repoUrl)
				.tags(getTags(repo))
				.ref(cmd.getRefName())
				.refName(Repository.shortenRefName(cmd.getRefName()))
				.diffUrl(diffUrl)
				.before(cmd.getOldId().getName())
				.after(cmd.getNewId().getName());

				List<RevCommit> commits = getCommits(receivePack, cmd.getOldId().name(), cmd.getNewId().name());
				for (RevCommit commit : commits) {
					Commit c = new Commit();
					c.id = commit.getName();
					c.url = getUrl(repo.name, null, commit.getName());
					c.message = commit.getFullMessage().trim();

					PersonIdent author = commit.getAuthorIdent();
					c.author = new Ident(author.getName(), author.getEmailAddress());
					c.timestamp = author.getWhen();
					if (c.timestamp == null) {
						c.timestamp = commit.getCommitterIdent().getWhen();
					}

					List<PathChangeModel> paths = JGitUtils.getFilesInCommit(receivePack.getRepository(), commit);
					c.added = filter(paths, ChangeType.ADD);
					c.modified = filter(paths, ChangeType.MODIFY);
					c.removed = filter(paths, ChangeType.DELETE);

					payload.add(c);
				}

		    	flowdock.setFlow(repo, payload);
		    	flowdock.sendAsync(payload);
			}
		} catch (Exception e) {
			log.error("Failed to notify FlowDock!", e);
		}
	}

	/**
	 * Determine if the ref changes for this repository should be posted to FlowDock.
	 *
	 * @param receivePack
	 * @return true if the ref changes should be posted
	 */
	protected boolean shallPost(GitblitReceivePack receivePack, Collection<ReceiveCommand> commands) {
		boolean shallPostRepo = flowdock.shallPost(receivePack.getRepositoryModel());
		return shallPostRepo;
	}

	protected String getSource(RepositoryModel repository) {
		return Constants.NAME;
	}

	protected List<String> getTags(RepositoryModel repository) {
		IRuntimeManager runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
		List<String> tags = new ArrayList<String>();
		tags.addAll(runtimeManager.getSettings().getStrings(Plugin.SETTING_FIXED_COMMIT_TAGS));
		if (tags.isEmpty()) {
			return null;
		}
		return tags;
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

    private List<RevCommit> getCommits(GitblitReceivePack receivePack, String baseId, String tipId) {
    	List<RevCommit> list = new ArrayList<RevCommit>();
		RevWalk walk = receivePack.getRevWalk();
		walk.reset();
		walk.sort(RevSort.TOPO);
		try {
			RevCommit tip = walk.parseCommit(receivePack.getRepository().resolve(tipId));
			RevCommit base = walk.parseCommit(receivePack.getRepository().resolve(baseId));
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
		}
		return list;
	}

    private List<String> filter(List<PathChangeModel> paths, ChangeType ct) {
    	List<String> list = new ArrayList<String>();
		for (PathChangeModel path : paths) {
			switch (path.changeType) {
			case ADD:
			case COPY:
				if (ChangeType.ADD == ct) {
					list.add(path.path);
				}
				break;
			case DELETE:
				if (ChangeType.DELETE == ct) {
					list.add(path.path);
				}
				break;
			default:
				if (ChangeType.MODIFY == ct) {
					list.add(path.path);
				}
				break;
			}
		}
		return list;
    }
}