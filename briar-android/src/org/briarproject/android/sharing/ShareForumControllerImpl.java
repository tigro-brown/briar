package org.briarproject.android.sharing;

import org.briarproject.android.contactselection.ContactSelectorControllerImpl;
import org.briarproject.android.contactselection.SelectableContactItem;
import org.briarproject.android.controller.handler.ResultExceptionHandler;
import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.contact.ContactManager;
import org.briarproject.api.db.DatabaseExecutor;
import org.briarproject.api.db.DbException;
import org.briarproject.api.db.NoSuchContactException;
import org.briarproject.api.db.NoSuchGroupException;
import org.briarproject.api.forum.ForumSharingManager;
import org.briarproject.api.lifecycle.LifecycleManager;
import org.briarproject.api.nullsafety.NotNullByDefault;
import org.briarproject.api.sync.GroupId;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;

@Immutable
@NotNullByDefault
public class ShareForumControllerImpl
		extends ContactSelectorControllerImpl<SelectableContactItem>
		implements ShareForumController {

	private final static Logger LOG =
			Logger.getLogger(ShareForumControllerImpl.class.getName());

	private final ForumSharingManager forumSharingManager;

	@Inject
	public ShareForumControllerImpl(
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			ContactManager contactManager,
			ForumSharingManager forumSharingManager) {
		super(dbExecutor, lifecycleManager, contactManager);
		this.forumSharingManager = forumSharingManager;
	}

	@Override
	protected boolean isSelected(Contact c, boolean wasSelected)
			throws DbException {
		return wasSelected;
	}

	@Override
	protected boolean isDisabled(GroupId g, Contact c) throws DbException {
		return !forumSharingManager.canBeShared(g, c);
	}

	@Override
	protected SelectableContactItem getItem(Contact c, boolean selected,
			boolean disabled) {
		return new SelectableContactItem(c, selected, disabled);
	}

	@Override
	public void share(final GroupId g, final Collection<ContactId> contacts,
			final String msg,
			final ResultExceptionHandler<Void, DbException> handler) {
		runOnDbThread(new Runnable() {
			@Override
			public void run() {
				try {
					for (ContactId c : contacts) {
						try {
							forumSharingManager.sendInvitation(g, c, msg);
						} catch (NoSuchContactException | NoSuchGroupException e) {
							if (LOG.isLoggable(WARNING))
								LOG.log(WARNING, e.toString(), e);
						}
					}
				} catch (DbException e) {
					if (LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
					handler.onException(e);
				}
			}
		});
	}

}